package com.jenikmax.game.library.service.scaner;

import com.jenikmax.game.library.service.scaner.api.ScanerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GameScanerService implements ScanerService {

    static final Logger logger = LogManager.getLogger(GameScanerService.class.getName());


    private final String rootDirectory;

    public GameScanerService(@Value("${game-library.games.directory}") String rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    @Override
    public void scan() {
        logger.info("Start scan directory - {}",this.rootDirectory);
    }
}
