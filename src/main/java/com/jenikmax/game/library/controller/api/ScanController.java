package com.jenikmax.game.library.controller.api;

import com.jenikmax.game.library.model.dto.api.ApiResponse;
import com.jenikmax.game.library.service.api.LibraryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ScanController {

    private static final Logger logger = LoggerFactory.getLogger(ScanController.class);

    private final LibraryService libraryService;

    public ScanController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @PostMapping("/scan")
    public ResponseEntity<ApiResponse<Void>> scanLibrary() {
        logger.info("REST scan library");
        try {
            libraryService.scanLibrary();
            return ResponseEntity.ok(ApiResponse.ok("Library scan completed", null));
        } catch (Exception e) {
            logger.error("Scan error", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Scan failed: " + e.getMessage()));
        }
    }
}
