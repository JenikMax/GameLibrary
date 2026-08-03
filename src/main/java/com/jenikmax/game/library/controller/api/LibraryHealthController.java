package com.jenikmax.game.library.controller.api;

import com.jenikmax.game.library.model.dto.api.ApiResponse;
import com.jenikmax.game.library.model.dto.api.LibraryHealthReport;
import com.jenikmax.game.library.service.LibraryHealthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/library/health")
@Tag(name = "Library Health", description = "Library health check and diagnostics")
public class LibraryHealthController {

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
}
