package com.jenikmax.game.library.service.scaner;

import com.jenikmax.game.library.model.entity.Game;
import com.jenikmax.game.library.model.entity.Screenshot;
import com.jenikmax.game.library.service.ai.EmbeddingService;
import com.jenikmax.game.library.service.data.api.GameService;
import com.jenikmax.game.library.service.scaner.api.ScanerService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class ScanTaskService implements DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(ScanTaskService.class);
    private static final String LIBRARY_PREFIX = "/games";

    private final Map<String, ScanTask> tasks = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "library-scanner");
        t.setDaemon(true);
        return t;
    });

    private final ScanerService scanerService;
    private final GameService gameService;
    private final EmbeddingService embeddingService;
    private final String rootDirectory;

    @PersistenceUnit
    private EntityManagerFactory emf;

    public ScanTaskService(ScanerService scanerService,
                           GameService gameService,
                           EmbeddingService embeddingService,
                           @Value("${game-library.games.directory}") String rootDirectory) {
        this.scanerService = scanerService;
        this.gameService = gameService;
        this.embeddingService = embeddingService;
        this.rootDirectory = rootDirectory;
    }

    public String submitScanTask() {
        pruneOldTasks();
        String taskId = UUID.randomUUID().toString();
        ScanTask task = new ScanTask(taskId);
        tasks.put(taskId, task);

        executor.submit(() -> {
            try {
                task.setStatus(ScanTask.Status.SCANNING_DIRS);
                task.setProgress(5);
                List<Game> findGames = scanerService.scanDirectory(rootDirectory + LIBRARY_PREFIX);

                List<Object[]> storedPaths = gameService.getGameDirectoryPaths();
                Map<String, Long> storedMap = new HashMap<>();
                for (Object[] row : storedPaths) {
                    storedMap.put((String) row[1], (Long) row[0]);
                }

                List<Game> newGames = storedMap.isEmpty() ?
                        findGames :
                        findGames.stream()
                                .filter(newGame -> !storedMap.containsKey(newGame.getDirectoryPath()))
                                .collect(Collectors.toList());

                List<Game> gamesToDelete = storedMap.isEmpty() ?
                        new ArrayList<>() :
                        storedMap.entrySet().stream()
                                .filter(entry -> findGames.stream()
                                        .noneMatch(findGame -> findGame.getDirectoryPath().equals(entry.getKey())))
                                .map(entry -> { Game g = new Game(); g.setId(entry.getValue()); return g; })
                                .collect(Collectors.toList());

                int newTotal = newGames.size();
                int deleteTotal = gamesToDelete.size();
                task.setNewGamesCount(newTotal);
                task.setDeletedGamesCount(deleteTotal);
                task.setTotalCount(newTotal + deleteTotal);
                task.setProgress(10);

                task.setStatus(ScanTask.Status.STORING_METADATA);
                List<Long> newGameIds = new ArrayList<>();
                int metaDone = 0;
                for (Game gameShort : newGames) {
                    task.setCurrentGame(gameShort.getName());
                    Game game = scanerService.getBasicGameInfo(gameShort);
                    game = gameService.storeGameMetadata(game);
                    newGameIds.add(game.getId());
                    metaDone++;
                    task.setProgress(10 + metaDone * 40 / Math.max(newTotal, 1));
                }

                task.setStatus(ScanTask.Status.LOADING_IMAGES);
                int imgDone = 0;
                for (Long gameId : newGameIds) {
                    EntityManager em = emf.createEntityManager();
                    try {
                        em.getTransaction().begin();
                        Game game = em.find(Game.class, gameId);
                        if (game != null) {
                            task.setCurrentGame(game.getName());
                            byte[] logo = scanerService.getLogo(game);
                            game.setLogo(logo);
                            List<Screenshot> screenshots = scanerService.getScreenshots(game);
                            game.getScreenshots().clear();
                            for (Screenshot s : screenshots) {
                                s.setGame(game);
                                em.persist(s);
                            }
                            game.setTotalSizeBytes(scanerService.calculateGameDirSize(game.getDirectoryPath()));
                        }
                        em.getTransaction().commit();
                    } catch (Exception e) {
                        if (em.getTransaction().isActive()) {
                            em.getTransaction().rollback();
                        }
                        throw e;
                    } finally {
                        em.close();
                    }
                    imgDone++;
                    task.setProgress(40 + imgDone * 25 / Math.max(newTotal, 1));
                }

                task.setStatus(ScanTask.Status.GENERATING_EMBEDDINGS);
                if (embeddingService.isAvailable()) {
                    int embDone = 0;
                    for (Long gameId : newGameIds) {
                        embeddingService.generateAndStore(gameId);
                        embDone++;
                        task.setProgress(65 + embDone * 15 / Math.max(newTotal, 1));
                    }
                }

                task.setStatus(ScanTask.Status.REFRESHING_SIZES);
                Map<String, Long> existingMap = new LinkedHashMap<>();
                for (Game fg : findGames) {
                    Long id = storedMap.get(fg.getDirectoryPath());
                    if (id != null) {
                        existingMap.put(fg.getDirectoryPath(), id);
                    }
                }
                int existTotal = existingMap.size();
                int existDone = 0;
                for (Map.Entry<String, Long> entry : existingMap.entrySet()) {
                    String dirPath = entry.getKey();
                    String displayName = dirPath.substring(dirPath.lastIndexOf('/') + 1);
                    task.setCurrentGame("refreshing size: " + displayName);
                    long size = scanerService.calculateGameDirSize(dirPath);
                    EntityManager em = emf.createEntityManager();
                    try {
                        em.getTransaction().begin();
                        Game game = em.find(Game.class, entry.getValue());
                        if (game != null) {
                            game.setTotalSizeBytes(size);
                        }
                        em.getTransaction().commit();
                    } catch (Exception e) {
                        if (em.getTransaction().isActive()) {
                            em.getTransaction().rollback();
                        }
                        logger.warn("Failed to refresh size for game {}", entry.getValue(), e);
                    } finally {
                        em.close();
                    }
                    existDone++;
                    task.setProgress(80 + existDone * 15 / Math.max(existTotal, 1));
                }

                for (Game gameShort : gamesToDelete) {
                    task.setCurrentGame("deleting game #" + gameShort.getId());
                    gameService.deleteGameInfo(gameShort.getId());
                }

                task.setProgress(100);
                task.setStatus(ScanTask.Status.COMPLETED);
                logger.info("Scan completed: {} new games, {} deleted", newTotal, deleteTotal);

            } catch (Exception e) {
                logger.error("Scan failed", e);
                task.setStatus(ScanTask.Status.FAILED);
                task.setErrorMessage(e.getMessage() != null ? e.getMessage() : "Unknown error");
            }
        });

        return taskId;
    }

    public ScanTask getTask(String taskId) {
        return tasks.get(taskId);
    }

    @Scheduled(fixedRate = 60000)
    public void pruneOldTasks() {
        long now = System.currentTimeMillis();
        long keepMillis = 300000;
        Iterator<Map.Entry<String, ScanTask>> it = tasks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, ScanTask> entry = it.next();
            ScanTask task = entry.getValue();
            if (task.getStatus() == ScanTask.Status.COMPLETED
                    || task.getStatus() == ScanTask.Status.FAILED) {
                if (now - task.getCreatedAt() > keepMillis) {
                    it.remove();
                }
            }
        }
    }

    @Override
    public void destroy() {
        executor.shutdown();
    }
}
