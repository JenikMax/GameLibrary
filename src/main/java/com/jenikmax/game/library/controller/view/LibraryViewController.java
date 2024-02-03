package com.jenikmax.game.library.controller.view;

import com.jenikmax.game.library.service.api.LibraryService;
import com.jenikmax.game.library.service.scaner.api.ScanerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class LibraryViewController {

    static final Logger logger = LogManager.getLogger(LibraryViewController.class.getName());

    private final LibraryService libraryService;

    public LibraryViewController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @GetMapping("/library")
    public String main() {
        logger.info("Open library");
        return "libraryView";
    }

    //@PostMapping("/library")
    //public String scanLibrary(Map<String,Object> formData) {
    //    logger.info("Scan library");
    //    return "redirect:/library";
    //}


    @PostMapping("/scan")
    public String scanLibrary(Model model) {
        logger.info("Scan library");
        libraryService.scanLibrary();
        model.addAttribute("message","scan library in progress");
        return "redirect:/library";
    }


}
