package com.jenikmax.game.library.service.emulator;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Каталог браузерных эмуляторов (EmulatorJS).
 * Маппит платформы библиотеки (свободная строка из имени папки) на
 * системные имена EmulatorJS, ядра и расширения ROM-файлов.
 * Эмуляция выполняется на клиенте (WASM в браузере) — сервер только
 * раздаёт ROM-файлы и хранит сейвы.
 */
public final class EmulatorCatalog {

    /** Описание платформы: системное имя EmulatorJS, ядра, расширения файлов. */
    public static final class EmulatorDef {
        private final String key;
        private final String system;
        private final String defaultCore;
        private final List<String> alternateCores;
        private final Set<String> extensions;
        private final Set<String> companionExtensions;
        private final Set<String> saveExtensions;

        public EmulatorDef(String key, String system, String defaultCore, List<String> alternateCores,
                           Set<String> extensions, Set<String> companionExtensions, Set<String> saveExtensions) {
            this.key = key;
            this.system = system;
            this.defaultCore = defaultCore;
            this.alternateCores = alternateCores;
            this.extensions = extensions;
            this.companionExtensions = companionExtensions;
            this.saveExtensions = saveExtensions;
        }

        public String getKey() { return key; }
        public String getSystem() { return system; }
        public String getDefaultCore() { return defaultCore; }
        public List<String> getAlternateCores() { return alternateCores; }
        public Set<String> getExtensions() { return extensions; }
        public Set<String> getCompanionExtensions() { return companionExtensions; }
        public Set<String> getSaveExtensions() { return saveExtensions; }
    }

    private static final Map<String, EmulatorDef> DEFS = new LinkedHashMap<>();
    private static final Map<String, String> ALIASES = new HashMap<>();

    static {
        // Расширения соответствуют cores.json EmulatorJS 4.2.x (pcsx_rearmed: cue/bin/img/mdf/pbp/toc/cbn/ccd).
        // chd поддерживается только mednafen_psx_hw — клиент при выборе .chd переключается на него.
        DEFS.put("ps1", new EmulatorDef("ps1", "psx", "pcsx_rearmed", List.of("mednafen_psx_hw"),
                Set.of("cue", "bin", "img", "mdf", "pbp", "toc", "cbn", "ccd", "chd"),
                Set.of("bin"), Set.of("srm", "mcr", "mcd", "mem")));
        DEFS.put("psp", new EmulatorDef("psp", "psp", "ppsspp", List.of(),
                Set.of("iso", "cso", "pbp", "elf"), Set.of(), Set.of()));
        DEFS.put("nes", new EmulatorDef("nes", "nes", "fceumm", List.of("nestopia"),
                Set.of("nes", "fds", "unf", "zip"), Set.of(), Set.of("srm", "sav")));
        DEFS.put("n64", new EmulatorDef("n64", "n64", "mupen64plus_next", List.of("parallel_n64"),
                Set.of("n64", "z64", "v64", "zip"), Set.of(), Set.of("srm", "sav")));
        DEFS.put("snes", new EmulatorDef("snes", "snes", "snes9x", List.of(),
                Set.of("smc", "sfc", "swc", "fig", "zip"), Set.of(), Set.of("srm", "sav")));

        registerAliases("ps1", "ps1", "psx", "playstation", "playstation1", "playstationone",
                "psone", "sonyplaystation", "sonyplaystation1");
        registerAliases("psp", "psp", "playstationportable", "sonypsp", "pspgo");
        registerAliases("nes", "nes", "dendy", "nintendoentertainmentsystem", "famicom",
                "familycomputer", "nintendo8bit", "fc", "nesfamicom");
        registerAliases("n64", "n64", "nintendo64", "ultra64");
        registerAliases("snes", "snes", "supernintendo", "supernintendoentertainmentsystem",
                "sfc", "superfamicom", "supernes");
    }

    /**
     * Нормализует имя платформы: нижний регистр, удаление всех не-буквенно-цифровых символов.
     * «Nintendo64», «Nintendo 64», «nintendo-64» → «nintendo64».
     */
    public static String normalize(String platform) {
        return platform.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
    }

    /**
     * Определяет эмулятор для платформы игры.
     * @param platform значение колонки platform (имя папки платформы)
     * @return описание эмулятора или пусто, если платформа не поддерживается
     */
    public static Optional<EmulatorDef> resolve(String platform) {
        if (platform == null || platform.isBlank()) return Optional.empty();
        String key = ALIASES.get(normalize(platform));
        return key == null ? Optional.empty() : Optional.of(DEFS.get(key));
    }

    private static void registerAliases(String key, String... aliases) {
        for (String alias : aliases) ALIASES.put(alias, key);
    }

    private EmulatorCatalog() {
    }
}
