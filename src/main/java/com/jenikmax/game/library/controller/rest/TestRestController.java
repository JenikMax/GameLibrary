package com.jenikmax.game.library.controller.rest;

import com.jenikmax.game.library.service.api.ScanerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestRestController {

    static final Logger logger = LogManager.getLogger(TestRestController.class.getName());

    private final ScanerService service;

    public TestRestController(ScanerService service) {
        this.service = service;
    }

    @GetMapping("/")
    public String test(){
        logger.info("Start test!!!");
        service.scan();
        logger.info("End test!!!");
        return "Hello World!";
    }


}
