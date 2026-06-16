package com.jenikmax.game.library.controller.api;

import com.jenikmax.game.library.model.dto.api.ApiResponse;
import com.jenikmax.game.library.service.utils.ImageMigrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class MigrationController {

    private static final Logger logger = LoggerFactory.getLogger(MigrationController.class);

    private final ImageMigrationService imageMigrationService;

    public MigrationController(ImageMigrationService imageMigrationService) {
        this.imageMigrationService = imageMigrationService;
    }

    @PostMapping("/migrate-images")
    public ResponseEntity<ApiResponse<String>> migrateImages() {
        logger.info("Starting image migration...");
        try {
            int count = imageMigrationService.migrateAll();
            String msg = "Migration complete. Migrated " + count + " images to " + "/gameLibrary/images";
            logger.info(msg);
            return ResponseEntity.ok(ApiResponse.ok(msg, null));
        } catch (Exception e) {
            logger.error("Migration failed", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Migration failed: " + e.getMessage()));
        }
    }
}
