package com.jenikmax.game.library.controller.rest;

import com.jenikmax.game.library.model.dto.GameReadDto;
import com.jenikmax.game.library.model.dto.GameShortDto;
import com.jenikmax.game.library.service.api.LibraryService;
import com.jenikmax.game.library.service.data.api.GameService;
import com.jenikmax.game.library.service.scaner.api.ScanerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/test")
public class TestRestController {

    static final Logger logger = LogManager.getLogger(TestRestController.class.getName());

    private final GameService service;
    private final ScanerService scanerService;
    private final LibraryService libraryService;

    public TestRestController(GameService service, ScanerService scanerService, LibraryService libraryService) {
        this.service = service;
        this.scanerService = scanerService;
        this.libraryService = libraryService;
    }

    @GetMapping("/")
    public List<GameReadDto> test(){
        logger.info("Start test!!!");
        return service.getGameShortList();
    }


    @GetMapping("/scan")
    public void testScan(){
        logger.info("Start scan!!!");
        libraryService.scanLibrary();
    }

}
