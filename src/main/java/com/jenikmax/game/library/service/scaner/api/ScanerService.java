package com.jenikmax.game.library.service.scaner.api;

import com.jenikmax.game.library.model.entity.Game;

import java.util.List;

public interface ScanerService {

    List<Game> scanDirectory(String path);

    Game getAdditinalGameInfo(Game game);

    void storeGame(Game game);
}
