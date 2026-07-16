package com.jenikmax.game.library.service;

import com.jenikmax.game.library.dao.api.GameCollectionEntryRepository;
import com.jenikmax.game.library.dao.api.GameCollectionRepository;
import com.jenikmax.game.library.dao.api.UserRepository;
import com.jenikmax.game.library.model.entity.GameCollection;
import com.jenikmax.game.library.model.entity.GameCollectionEntry;
import com.jenikmax.game.library.model.entity.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CollectionService {

    private final GameCollectionRepository collectionRepository;
    private final GameCollectionEntryRepository entryRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    public CollectionService(GameCollectionRepository collectionRepository,
                              GameCollectionEntryRepository entryRepository,
                              UserRepository userRepository,
                              JdbcTemplate jdbcTemplate) {
        this.collectionRepository = collectionRepository;
        this.entryRepository = entryRepository;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<GameCollection> getUserCollections(Long userId) {
        return collectionRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    public List<GameCollection> getPublicCollections() {
        return collectionRepository.findByIsPublicTrueOrderByUpdatedAtDesc();
    }

    public Optional<GameCollection> getById(Long id) {
        return collectionRepository.findById(id);
    }

    @Transactional
    public GameCollection create(String name, String description, boolean isPublic, Long userId) {
        GameCollection c = new GameCollection();
        c.setName(name);
        c.setDescription(description);
        c.setIsPublic(isPublic);
        c.setUser(userRepository.getReferenceById(userId));
        c.setCreatedAt(Timestamp.from(Instant.now()));
        c.setUpdatedAt(Timestamp.from(Instant.now()));
        return collectionRepository.save(c);
    }

    @Transactional
    public GameCollection update(Long id, String name, String description, boolean isPublic) {
        GameCollection c = collectionRepository.findById(id).orElseThrow();
        c.setName(name);
        c.setDescription(description);
        c.setIsPublic(isPublic);
        c.setUpdatedAt(Timestamp.from(Instant.now()));
        return collectionRepository.save(c);
    }

    @Transactional
    public void delete(Long id) {
        collectionRepository.deleteById(id);
    }

    public List<GameCollectionEntry> getEntries(Long collectionId) {
        return entryRepository.findByCollectionIdOrderBySortOrderAsc(collectionId);
    }

    @Transactional
    public GameCollectionEntry addGame(Long collectionId, Long gameId) {
        Optional<GameCollectionEntry> existing = entryRepository.findByCollectionIdAndGameId(collectionId, gameId);
        if (existing.isPresent()) return existing.get();

        long count = entryRepository.countByCollectionId(collectionId);
        GameCollectionEntry e = new GameCollectionEntry();
        e.setCollection(collectionRepository.getReferenceById(collectionId));
        e.setGameId(gameId);
        e.setSortOrder((int) count);
        e.setAddedAt(Timestamp.from(Instant.now()));
        return entryRepository.save(e);
    }

    @Transactional
    public void removeGame(Long collectionId, Long gameId) {
        entryRepository.deleteByCollectionIdAndGameId(collectionId, gameId);
    }

    @Transactional
    public void reorder(Long collectionId, List<Long> gameIds) {
        for (int i = 0; i < gameIds.size(); i++) {
            final int order = i;
            entryRepository.findByCollectionIdAndGameId(collectionId, gameIds.get(i))
                    .ifPresent(e -> {
                        e.setSortOrder(order);
                        entryRepository.save(e);
                    });
        }
    }

    public long getEntryCount(Long collectionId) {
        return entryRepository.countByCollectionId(collectionId);
    }

    public boolean isOwner(Long collectionId, Long userId) {
        return collectionRepository.findById(collectionId)
                .map(c -> c.getUser().getId().equals(userId))
                .orElse(false);
    }

    public List<Map<String, Object>> getCollectionsWithHeroData(Long currentUserId) {
        List<GameCollection> mine = getUserCollections(currentUserId);
        List<GameCollection> shared = getPublicCollections();

        Set<Long> seen = new HashSet<>();
        List<GameCollection> all = new ArrayList<>();
        for (GameCollection c : mine) {
            all.add(c);
            seen.add(c.getId());
        }
        for (GameCollection c : shared) {
            if (!seen.contains(c.getId())) {
                all.add(c);
            }
        }

        if (all.isEmpty()) return List.of();

        List<Long> collectionIds = all.stream().map(GameCollection::getId).toList();

        String inClause = collectionIds.stream().map(String::valueOf).collect(Collectors.joining(","));

        Map<Long, Integer> countMap = jdbcTemplate.query(
                "SELECT collection_id, COUNT(*) FROM library.game_collection_entry WHERE collection_id IN (" + inClause + ") GROUP BY collection_id",
                (rs, rn) -> Map.entry(rs.getLong(1), rs.getInt(2))
        ).stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        String sql = """
                SELECT gce.collection_id, gce.game_id, g.name,
                       COALESCE(AVG(gr.rating), 0) AS avg_rating
                FROM library.game_collection_entry gce
                JOIN library.game_data g ON gce.game_id = g.id
                LEFT JOIN library.game_rating gr ON g.id = gr.game_id
                WHERE gce.collection_id IN (%s)
                GROUP BY gce.collection_id, gce.game_id, g.name, gce.sort_order
                ORDER BY gce.collection_id, avg_rating DESC, g.name ASC""".formatted(inClause);

        List<Object[]> rows = jdbcTemplate.query(sql, (rs, rn) -> new Object[]{
                rs.getLong("collection_id"),
                rs.getLong("game_id"),
                rs.getString("name"),
                rs.getDouble("avg_rating")
        });

        Map<Long, List<Object[]>> grouped = rows.stream()
                .collect(Collectors.groupingBy(r -> (Long) r[0]));

        return all.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("name", c.getName());
            m.put("description", c.getDescription());
            m.put("isPublic", c.getIsPublic());
            m.put("userId", c.getUser().getId());
            m.put("username", c.getUser().getUsername());
            m.put("gameCount", countMap.getOrDefault(c.getId(), 0));
            m.put("updatedAt", c.getUpdatedAt() != null ? c.getUpdatedAt().toString() : null);

            List<Object[]> games = grouped.getOrDefault(c.getId(), List.of());

            if (games.isEmpty()) {
                m.put("heroGameId", null);
                m.put("heroGameName", null);
                m.put("previewGames", List.of());
                m.put("overflow", 0);
            } else {
                Object[] hero = games.get(0);
                m.put("heroGameId", hero[1]);
                m.put("heroGameName", hero[2]);

                List<Map<String, Object>> previews = new ArrayList<>();
                int total = games.size();
                int maxPreviews = Math.min(total - 1, 2);
                for (int i = 1; i <= maxPreviews; i++) {
                    Object[] g = games.get(i);
                    Map<String, Object> pg = new LinkedHashMap<>();
                    pg.put("gameId", g[1]);
                    pg.put("name", g[2]);
                    previews.add(pg);
                }
                m.put("previewGames", previews);
                int overflow = total - 1 - maxPreviews;
                m.put("overflow", Math.max(overflow, 0));
            }

            return m;
        }).collect(Collectors.toList());
    }
}
