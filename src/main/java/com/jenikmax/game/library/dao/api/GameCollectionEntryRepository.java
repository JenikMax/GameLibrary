package com.jenikmax.game.library.dao.api;

import com.jenikmax.game.library.model.entity.GameCollectionEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GameCollectionEntryRepository extends JpaRepository<GameCollectionEntry, Long> {

    List<GameCollectionEntry> findByCollectionIdOrderBySortOrderAsc(Long collectionId);

    Optional<GameCollectionEntry> findByCollectionIdAndGameId(Long collectionId, Long gameId);

    void deleteByCollectionIdAndGameId(Long collectionId, Long gameId);

    long countByCollectionId(Long collectionId);

    List<Long> findGameIdByCollectionId(Long collectionId);
}
