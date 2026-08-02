package com.jenikmax.game.library.controller.api;

import com.jenikmax.game.library.model.dto.GameDto;
import com.jenikmax.game.library.model.dto.api.ApiResponse;
import com.jenikmax.game.library.model.entity.EmulatorSave;
import com.jenikmax.game.library.service.api.LibraryService;
import com.jenikmax.game.library.service.data.api.UserService;
import com.jenikmax.game.library.service.emulator.EmulatorCatalog;
import com.jenikmax.game.library.service.emulator.EmulatorCatalog.EmulatorDef;
import com.jenikmax.game.library.service.emulator.EmulatorSaveService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Контроллер браузерной эмуляции (EmulatorJS).
 * Список ROM-файлов игры (рекурсивно, включая вложенные папки и
 * мультифайловые образы cue+bin), стриминг ROM-файлов с поддержкой
 * HTTP Range (206), серверное хранение сейвов (srm/state) per-user.
 * Эмуляция выполняется на клиенте — сервер только раздаёт файлы.
 */
@RestController
@RequestMapping("/api/games")
@Tag(name = "Emulation", description = "Browser emulation: ROM listing, streaming, saves")
public class EmulationController {

    private static final Logger logger = LoggerFactory.getLogger(EmulationController.class);

    private static final String ROM_PREFIX = "/rom/";
    private static final Pattern CUE_FILE_PATTERN = Pattern.compile(
            "(?i)^\\s*FILE\\s+(?:\"([^\"]+)\"|(\\S+))");

    private final LibraryService libraryService;
    private final EmulatorSaveService saveService;
    private final UserService userService;

    public EmulationController(LibraryService libraryService,
                               EmulatorSaveService saveService,
                               UserService userService) {
        this.libraryService = libraryService;
        this.saveService = saveService;
        this.userService = userService;
    }

    /**
     * Информация об эмуляции игры: платформа, ядро EmulatorJS, список ROM-файлов.
     * Файлы ищутся рекурсивно по всей директории игры. Для cue+bin бины
     * помечаются как companion (загружаются эмулятором автоматически по cue).
     * @param id идентификатор игры
     * @return supported=false для неподдерживаемых платформ или игр без файлов
     */
    @GetMapping("/{id}/emulation")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEmulation(@PathVariable Long id) {
        GameDto gameDto = libraryService.getGameInfo(id);
        if (gameDto == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("Game not found"));
        }
        Optional<EmulatorDef> def = EmulatorCatalog.resolve(gameDto.getPlatform());
        if (def.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.ok(Map.of(
                    "supported", false,
                    "platform", gameDto.getPlatform() != null ? gameDto.getPlatform() : "")));
        }

        EmulatorDef emulatorDef = def.get();
        List<Map<String, Object>> files = scanRomFiles(gameDto.getDirectoryPath(), emulatorDef);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("supported", true);
        result.put("platform", gameDto.getPlatform());
        result.put("system", emulatorDef.getSystem());
        result.put("core", emulatorDef.getDefaultCore());
        result.put("alternateCores", emulatorDef.getAlternateCores());
        result.put("files", files);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * Стриминг ROM-файла с поддержкой HTTP Range (206 Partial Content).
     * Путь к файлу — относительный, в path URL (не query): это критично,
     * т.к. EmulatorJS резолвит .bin из .cue через url.resolve() относительно
     * URL игры. Защита от path traversal: относительный путь разрешается
     * только внутри директории игры.
     * @param id       идентификатор игры
     * @param request  HTTP-запрос (путь после /rom/ и заголовок Range)
     * @param response HTTP-ответ (200/206/416, потоковый вывод)
     */
    @GetMapping("/{id}/rom/**")
    public void serveRom(@PathVariable Long id,
                         HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        GameDto gameDto = libraryService.getGameInfo(id);
        if (gameDto == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Path baseDir = Paths.get(gameDto.getDirectoryPath()).normalize();
        Path target = resolveSafe(baseDir, extractRomPath(request));
        if (target == null || !Files.isRegularFile(target)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        long size = Files.size(target);
        response.setHeader("Accept-Ranges", "bytes");
        response.setContentType(contentTypeFor(target));

        long start = 0;
        long end = size - 1;
        boolean partial = false;

        String range = request.getHeader("Range");
        if (range != null && range.startsWith("bytes=")) {
            long[] bounds = parseRange(range, size);
            if (bounds == null) {
                response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                response.setHeader("Content-Range", "bytes */" + size);
                return;
            }
            start = bounds[0];
            end = bounds[1];
            partial = true;
        }

        if (partial) {
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + size);
            response.setHeader("Content-Length", String.valueOf(end - start + 1));
        } else {
            response.setHeader("Content-Length", String.valueOf(size));
        }

        if ("HEAD".equalsIgnoreCase(request.getMethod())) {
            return;
        }

        long position = start;
        long remaining = end - start + 1;
        try (FileChannel channel = FileChannel.open(target, StandardOpenOption.READ)) {
            while (remaining > 0) {
                long transferred = channel.transferTo(position, remaining,
                        Channels.newChannel(response.getOutputStream()));
                if (transferred <= 0) {
                    break;
                }
                position += transferred;
                remaining -= transferred;
            }
        }
    }

    /**
     * Список серверных сейвов пользователя для игры.
     */
    @GetMapping("/{id}/save-list")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSaveList(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }
        List<Map<String, Object>> items = saveService.list(id, userId).stream()
                .map(s -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("kind", s.getKind());
                    item.put("slot", s.getSlot());
                    item.put("name", s.getName() != null ? s.getName() : "");
                    item.put("sizeBytes", s.getSizeBytes());
                    item.put("updatedAt", s.getUpdatedAt() != null ? s.getUpdatedAt().getTime() : null);
                    return item;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(items));
    }

    /**
     * Скачать серверный сейв пользователя (srm/state, слот).
     * 404, если сейва нет.
     */
    @GetMapping("/{id}/save")
    public void getSave(@PathVariable Long id,
                        @RequestParam(defaultValue = "srm") String kind,
                        @RequestParam(defaultValue = "0") int slot,
                        HttpServletResponse response) throws IOException {
        Long userId = getCurrentUserId();
        if (userId == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        Optional<EmulatorSave> save = saveService.find(id, userId, kind, slot);
        if (save.isEmpty() || save.get().getData() == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"save." + kind + "\"");
        response.setContentLength(save.get().getData().length);
        response.getOutputStream().write(save.get().getData());
    }

    /**
     * Сохранить сейв пользователя на сервер (upsert в слот).
     * Тело запроса — бинарные данные сейва.
     */
    @PutMapping("/{id}/save")
    public ResponseEntity<ApiResponse<Map<String, Object>>> putSave(
            @PathVariable Long id,
            @RequestParam(defaultValue = "srm") String kind,
            @RequestParam(defaultValue = "0") int slot,
            @RequestParam(required = false) String name,
            @RequestBody byte[] data) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }
        try {
            EmulatorSave saved = saveService.save(id, userId, kind, slot, name, data);
            return ResponseEntity.ok(ApiResponse.ok(Map.of(
                    "saved", true,
                    "sizeBytes", saved.getSizeBytes(),
                    "updatedAt", saved.getUpdatedAt() != null ? saved.getUpdatedAt().getTime() : null)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─── Внутренние хелперы ─────────────────────────────────────────────

    /**
     * Рекурсивно сканирует директорию игры на ROM-файлы.
     * Файлы информации (information/, img/) исключаются. Каждый файл
     * получает kind: primary (загружаемый), companion (bin из cue+bin) или
     * save (сейв-файлы, не ромы).
     */
    private List<Map<String, Object>> scanRomFiles(String directoryPath, EmulatorDef def) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (directoryPath == null || directoryPath.isBlank()) {
            return result;
        }
        Path baseDir = Paths.get(directoryPath).normalize();
        if (!Files.isDirectory(baseDir)) {
            return result;
        }

        List<Path> paths;
        try (Stream<Path> stream = Files.walk(baseDir)) {
            paths = stream.filter(Files::isRegularFile).collect(Collectors.toList());
        } catch (IOException e) {
            logger.warn("Failed to scan game directory {}: {}", baseDir, e.getMessage());
            return result;
        }

        Set<String> cueBins = collectCueBins(paths, baseDir, def);

        for (Path path : paths) {
            String relPath = toRelativePath(baseDir, path);
            String ext = extensionOf(path);
            if (isInfoPath(relPath)) {
                continue;
            }
            String kind;
            if (def.getSaveExtensions().contains(ext)) {
                kind = "save";
            } else if (def.getExtensions().contains(ext)) {
                kind = "primary";
            } else if (def.getCompanionExtensions().contains(ext) && cueBins.contains(relPath.toLowerCase(Locale.ROOT))) {
                kind = "companion";
            } else {
                continue;
            }

            long size;
            try {
                size = Files.size(path);
            } catch (IOException e) {
                size = 0;
            }

            Map<String, Object> fileInfo = new LinkedHashMap<>();
            fileInfo.put("path", relPath);
            fileInfo.put("name", path.getFileName().toString());
            fileInfo.put("size", size);
            fileInfo.put("kind", kind);
            result.add(fileInfo);
        }

        result.sort(Comparator.comparing(m -> ((String) m.get("path")).toLowerCase(Locale.ROOT)));
        return result;
    }

    /**
     * Собирает множество относительных путей .bin-файлов, упомянутых в cue-файлах.
     * Имена bin резолвятся относительно директории cue-файла.
     */
    private Set<String> collectCueBins(List<Path> paths, Path baseDir, EmulatorDef def) {
        Set<String> bins = new HashSet<>();
        for (Path path : paths) {
            if (!"cue".equals(extensionOf(path))) {
                continue;
            }
            Path cueDir = path.getParent() != null ? path.getParent() : baseDir;
            try {
                List<String> lines = Files.readAllLines(path, StandardCharsets.ISO_8859_1);
                for (String line : lines) {
                    Matcher matcher = CUE_FILE_PATTERN.matcher(line);
                    if (matcher.find()) {
                        String fileName = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
                        Path binPath = cueDir.resolve(fileName).normalize();
                        if (binPath.startsWith(baseDir)) {
                            bins.add(toRelativePath(baseDir, binPath).toLowerCase(Locale.ROOT));
                        }
                    }
                }
            } catch (IOException e) {
                logger.warn("Failed to parse cue file {}: {}", path, e.getMessage());
            }
        }
        return bins;
    }

    /**
     * Резолвит относительный путь внутри базовой директории.
     * Возвращает null при path traversal.
     */
    private Path resolveSafe(Path baseDir, String relPath) {
        if (relPath == null || relPath.isBlank()) {
            return null;
        }
        Path target = baseDir.resolve(relPath).normalize();
        return target.startsWith(baseDir) ? target : null;
    }

    /**
     * Извлекает относительный путь ROM из path-части URL (после /rom/).
     * getRequestURI() не декодируется Tomcat, поэтому декодируем вручную
     * (UTF-8, с защитой имени от «+» как пробела — в path «+» валидный символ).
     */
    private String extractRomPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        int idx = uri.indexOf(ROM_PREFIX);
        if (idx < 0) {
            return null;
        }
        String raw = uri.substring(idx + ROM_PREFIX.length());
        return URLDecoder.decode(raw.replace("+", "%2B"), StandardCharsets.UTF_8);
    }

    private String toRelativePath(Path baseDir, Path path) {
        return baseDir.relativize(path).toString().replace(File.separatorChar, '/');
    }

    private String extensionOf(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    }

    /** Исключает служебные папки информации и скриншотов из поиска ромов. */
    private boolean isInfoPath(String relPath) {
        String normalized = relPath.toLowerCase(Locale.ROOT);
        return normalized.startsWith("information/")
                || normalized.startsWith("img/")
                || normalized.contains("/information/")
                || normalized.contains("/img/");
    }

    /**
     * Парсит одиночный диапазон Range: bytes=start-end | bytes=start- | bytes=-suffix.
     * @return [start, end] включительно или null при невалидном/множественном диапазоне
     */
    private long[] parseRange(String range, long size) {
        String spec = range.substring("bytes=".length()).trim();
        if (spec.contains(",")) {
            return null;
        }
        try {
            int dash = spec.indexOf('-');
            if (dash < 0) {
                return null;
            }
            if (dash == 0) {
                long suffix = Long.parseLong(spec.substring(1).trim());
                if (suffix <= 0) {
                    return null;
                }
                long start = Math.max(0, size - suffix);
                return size == 0 ? null : new long[]{start, size - 1};
            }
            long start = Long.parseLong(spec.substring(0, dash).trim());
            String endPart = spec.substring(dash + 1).trim();
            long end = endPart.isEmpty() ? size - 1 : Long.parseLong(endPart);
            if (start < 0 || start >= size || end < start) {
                return null;
            }
            return new long[]{start, Math.min(end, size - 1)};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String contentTypeFor(Path path) {
        switch (extensionOf(path)) {
            case "cue": return "text/plain; charset=utf-8";
            case "iso": return "application/x-iso9660-image";
            case "zip": return "application/zip";
            default: return "application/octet-stream";
        }
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        var userDto = userService.getUserInfoByName(auth.getName());
        return userDto != null ? userDto.getId() : null;
    }
}
