package com.jenikmax.game.library.controller.view;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainViewController {

    static final Logger logger = LogManager.getLogger(MainViewController.class.getName());

    @GetMapping("/")
    public String main() {
        logger.info("Open app");
        //model.addAttribute("message", "Hello, World!");
        return "indexView";
    }
}
