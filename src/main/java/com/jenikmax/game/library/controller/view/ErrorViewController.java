package com.jenikmax.game.library.controller.view;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ErrorViewController {

    @GetMapping("/error403")
    public String error403(Model model) {
        model.addAttribute("title","Error 403");
        model.addAttribute("h1","Error 403 - Forbidden");
        model.addAttribute("p1","Access denied. You don't have permission to view this page.");
        model.addAttribute("p2","Please contact the administrator for assistance.");
        return "errorView";
    }

}
