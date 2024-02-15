package com.jenikmax.game.library.controller.view;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class MainViewController {

    static final Logger logger = LogManager.getLogger(MainViewController.class.getName());

    @GetMapping("/")
    public String main() {
        logger.info("Open app");
        //model.addAttribute("message", "Hello, World!");
        return "indexView";
    }

    //@PostMapping("/login")
    //public String login(){
    //    logger.info("login in app");
    //    return "redirect:/library";
    //}
}
