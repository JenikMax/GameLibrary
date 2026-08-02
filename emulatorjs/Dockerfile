# ============================================================
# emulatorjs — self-hosted EmulatorJS (RetroArch WASM-ядра)
# Скачивает собранные данные EmulatorJS с официального CDN
# (в GitHub-релизе только исходники, собранные ядра — на CDN),
# обрезает неиспользуемые ядра и раздаёт статику через nginx.
# Только внутренний доступ (через frontend Nginx на /game-library/emu/).
# ============================================================

# ─── Стадия 1: скачивание и обрезка ─────────────────────────
FROM alpine:3.20 AS download

ARG EJS_VERSION=4.2.3

RUN apk add --no-cache wget ca-certificates

RUN mkdir -p /out && cd /out \
    && wget -q -r -np -nH --cut-dirs=2 -R "*.map" -R "emulator.min.zip" \
       https://cdn.emulatorjs.org/${EJS_VERSION}/data/ \
    && mkdir -p data \
    && mv cores compression localization src emulator.css emulator.min.css \
       emulator.min.js loader.js version.json data/ 2>/dev/null || true

# Trim: оставляем только нужные ядра (fceumm/nestopia, snes9x, mupen64plus_next/parallel_n64,
# pcsx_rearmed/mednafen_psx_hw, ppsspp) + служебные файлы
RUN cd /out/data/cores \
    && find . -maxdepth 1 -type f \
       ! -name "fceumm*" ! -name "nestopia*" ! -name "snes9x*" \
       ! -name "mupen64plus_next*" ! -name "parallel_n64*" \
       ! -name "pcsx_rearmed*" ! -name "mednafen_psx_hw*" ! -name "ppsspp*" \
       ! -name "package.json" ! -name "cores.json" ! -name "README*" ! -name "core-README*" \
       -delete \
    && rm -rf reports

# ─── Стадия 2: Nginx ─────────────────────────────────────────
FROM nginx:1.30-alpine

COPY --from=download /out/data /usr/share/nginx/html/emu/data
COPY player.html /usr/share/nginx/html/emu/player.html

EXPOSE 80
