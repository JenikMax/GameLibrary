package com.jenikmax.game.library.dao.api;

import com.jenikmax.game.library.model.entity.GameTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface GameTagRepository extends JpaRepository<GameTag, Long> {

    List<GameTag> findByGameId(Long gameId);

    @Query("SELECT DISTINCT gt.tagCode FROM GameTag gt ORDER BY gt.tagCode")
    List<String> findAllTagCodes();

    void deleteByGameId(Long gameId);
}
