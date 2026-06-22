package com.jenikmax.game.library.controller.api;

import com.jenikmax.game.library.model.dto.api.ApiResponse;
import com.jenikmax.game.library.model.dto.api.ScraperConfigResponse;
import com.jenikmax.game.library.service.scraper.ConfigEncryptionService;
import com.jenikmax.game.library.service.scraper.ScraperFactory;
import com.jenikmax.game.library.service.scraper.ScraperConfigService;
import com.jenikmax.game.library.service.scraper.model.ScraperConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/scraper-config")
@PreAuthorize("hasRole('ADMIN')")
public class ScraperConfigController {

    private static final Logger log = LoggerFactory.getLogger(ScraperConfigController.class);

    private final ScraperConfigService configService;
    private final ScraperFactory scraperFactory;
    private final ConfigEncryptionService encryptionService;

    public ScraperConfigController(ScraperConfigService configService,
                                    ScraperFactory scraperFactory,
                                    ConfigEncryptionService encryptionService) {
        this.configService = configService;
        this.scraperFactory = scraperFactory;
        this.encryptionService = encryptionService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ScraperConfigResponse>>> getAll() {
        List<ScraperConfigResponse> items = configService.getAllConfigs().values().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(items));
    }

    @GetMapping("/{type}")
    public ResponseEntity<ApiResponse<ScraperConfigResponse>> getOne(@PathVariable String type) {
        ScraperConfig cfg = configService.getConfig(type);
        if (cfg == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Scraper not found: " + type));
        }
        return ResponseEntity.ok(ApiResponse.ok(toResponse(cfg)));
    }

    @PutMapping("/{type}")
    public ResponseEntity<ApiResponse<ScraperConfigResponse>> update(
            @PathVariable String type,
            @RequestBody ScraperConfig config) {
        log.info("Updating scraper config: {}", type);
        try {
            configService.updateConfig(type, config);
            scraperFactory.invalidateCache();
            ScraperConfig updated = configService.getConfig(type);
            return ResponseEntity.ok(ApiResponse.ok("Scraper config updated", toResponse(updated)));
        } catch (Exception e) {
            log.error("Failed to update scraper config: {}", type, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to update: " + e.getMessage()));
        }
    }

    @PostMapping("/reload")
    public ResponseEntity<ApiResponse<Void>> reload() {
        log.info("Reloading scraper config from disk");
        try {
            configService.reload();
            scraperFactory.invalidateCache();
            return ResponseEntity.ok(ApiResponse.ok("Scraper config reloaded", null));
        } catch (Exception e) {
            log.error("Failed to reload scraper config", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to reload: " + e.getMessage()));
        }
    }

    private ScraperConfigResponse toResponse(ScraperConfig cfg) {
        boolean hasKey = cfg.getEncryptedApiKey() != null && !cfg.getEncryptedApiKey().isEmpty();
        String maskedKey = hasKey ? encryptionService.mask(encryptionService.decrypt(cfg.getEncryptedApiKey())) : "";
        return ScraperConfigResponse.from(cfg, maskedKey, hasKey);
    }
}
