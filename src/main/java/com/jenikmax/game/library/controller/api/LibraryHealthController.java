package com.jenikmax.game.library.controller.api;

import com.jenikmax.game.library.model.dto.api.ApiResponse;
import com.jenikmax.game.library.model.dto.api.LibraryHealthReport;
import com.jenikmax.game.library.service.LibraryHealthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/library/health")
@Tag(name = "Library Health", description = "Library health check and auto-fix for missing metadata")
public class LibraryHealthController {

    private static final Logger logger = LoggerFactory.getLogger(LibraryHealthController.class);

    private final LibraryHealthService healthService;

    public LibraryHealthController(LibraryHealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<LibraryHealthReport>> getHealthReport() {
        return ResponseEntity.ok(ApiResponse.ok(healthService.getHealthReport()));
    }

    @GetMapping("/issues/{issueType}")
    public ResponseEntity<ApiResponse<List<LibraryHealthReport.GameIssue>>> getIssuesByType(
            @PathVariable String issueType,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit) {
        List<LibraryHealthReport.GameIssue> issues = healthService.getIssuesByType(issueType.toUpperCase(), offset, limit);
        return ResponseEntity.ok(ApiResponse.ok(issues));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/fix/{issueType}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> fixIssueType(@PathVariable String issueType) {
        logger.info("Starting auto-fix for issue type: {}", issueType);
        int fixed = healthService.fixIssueType(issueType.toUpperCase());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("issueType", issueType.toUpperCase(), "fixedCount", fixed)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/fix/{issueType}/{gameId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> fixSingleGame(
            @PathVariable String issueType,
            @PathVariable Long gameId) {
        int fixed = healthService.fixSingleGame(gameId, issueType.toUpperCase());
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "issueType", issueType.toUpperCase(), "gameId", gameId, "fixed", fixed > 0)));
    }
}
