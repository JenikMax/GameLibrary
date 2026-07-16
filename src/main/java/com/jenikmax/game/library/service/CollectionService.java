package com.jenikmax.game.library.service;

import com.jenikmax.game.library.dao.api.GameCollectionEntryRepository;
import com.jenikmax.game.library.dao.api.GameCollectionRepository;
import com.jenikmax.game.library.dao.api.UserRepository;
import com.jenikmax.game.library.model.entity.GameCollection;
import com.jenikmax.game.library.model.entity.GameCollectionEntry;
import com.jenikmax.game.library.model.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class CollectionService {

    private final GameCollectionRepository collectionRepository;
    private final GameCollectionEntryRepository entryRepository;
    private final UserRepository userRepository;

    public CollectionService(GameCollectionRepository collectionRepository,
                              GameCollectionEntryRepository entryRepository,
                              UserRepository userRepository) {
        this.collectionRepository = collectionRepository;
        this.entryRepository = entryRepository;
        this.userRepository = userRepository;
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
}
