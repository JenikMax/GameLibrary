package com.jenikmax.game.library.controller.view;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TestViewController {

    static final Logger logger = LogManager.getLogger(TestViewController.class.getName());

    @GetMapping("/message")
    public String displayMessage(Model model) {
        logger.info("Start displayMessage!!!");
        model.addAttribute("message", "Hello, World!");
        return "messageView";
    }

}
