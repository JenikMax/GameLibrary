package com.jenikmax.game.library.service.data.api;

import com.jenikmax.game.library.model.entity.Game;

import java.util.List;

public interface GameService {

    List<Game> getGameList();

    void storeGame(Game game);

    void storeNewGameInLibrary(List<Game> games);

}
