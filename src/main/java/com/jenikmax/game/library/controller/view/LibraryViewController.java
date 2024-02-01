package com.jenikmax.game.library.controller.view;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Map;

@Controller
public class LibraryViewController {

    static final Logger logger = LogManager.getLogger(LibraryViewController.class.getName());

    @GetMapping("/library")
    public String main() {
        logger.info("Open library");
        return "libraryView";
    }

    @PostMapping("/library")
    public String scanLibrary(Map<String,Object> formData) {
        logger.info("Scan library");
        return "redirect:/library";
    }


}
