package com.jenikmax.game.library.service.scaner.api;

import com.jenikmax.game.library.model.entity.Game;
import com.jenikmax.game.library.model.entity.Screenshot;

import java.util.List;

/**
 * Интерфейс сервиса сканирования файловой системы игр.
 * Определяет операции обхода директорий, извлечения метаданных,
 * логотипов, скриншотов и вычисления размера директории.
 */
public interface ScanerService {

    List<Game> scanDirectory(String path);

    Game getAdditinalGameInfo(Game game);

    Game getBasicGameInfo(Game game);

    byte[] getLogo(Game game);

    List<Screenshot> getScreenshots(Game game);

    long calculateGameDirSize(String directoryPath);

    void storeGame(Game game);
}
