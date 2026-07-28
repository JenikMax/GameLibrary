<!-- SVG-компонент ретро-монитора (CRT-дисплей) для страниц аутентификации. Отображает настраиваемые строки текста, сканлайны, виньетку, мерцающий курсор и кастомную цветовую палитру. -->
<template>
  <div class="retro-crt-wrapper" :style="cssVars">
    <svg viewBox="0 0 340 200" xmlns="http://www.w3.org/2000/svg" class="retro-crt-svg" stroke-linecap="round" stroke-linejoin="round">
      <!-- Корпус монитора -->
      <rect x="85" y="15" width="170" height="170" rx="8" :stroke="primary" :fill="bg" />
      <!-- Экран -->
      <rect x="100" y="28" width="140" height="120" rx="4" :stroke="primary" :fill="bgInner" />
      <rect x="105" y="33" width="130" height="110" rx="3" stroke="none" :fill="primary" opacity="0.06" />
      <!-- Строки текста на экране -->
      <text v-for="(line, i) in screenLines" :key="i"
        :x="line.x || 115" :y="line.y || 50 + i * 14"
        :font-size="line.size || 6"
        :stroke="line.color || primary"
        :fill="line.color || primary"
        :opacity="line.opacity ?? (line.isCursor ? 0.9 : 0.6)"
        font-family="monospace">{{ line.text }}</text>
      <!-- Мерцающий курсор SVG -->
      <text x="115" y="144" font-size="7" :stroke="muted" :fill="muted" opacity="0.9" font-family="monospace" class="crt-cursor-text">_</text>
      <!-- Нижние декоративные элементы управления -->
      <rect x="100" y="158" width="15" height="8" rx="2" :stroke="primary" fill="none" />
      <rect x="120" y="158" width="15" height="8" rx="2" :stroke="primary" fill="none" />
      <circle cx="230" cy="162" r="5" :stroke="primary" fill="none" />
      <line x1="230" y1="160" x2="230" y2="164" :stroke="primary" opacity="0.5" />
    </svg>
    <!-- Наложение сканлайнов -->
    <div class="crt-overlay"></div>
    <!-- Виньетка по краям -->
    <div class="vignette-overlay"></div>
    <div class="corner-overlay"></div>
    <!-- Приглашение под монитором -->
    <div class="retro-crt-prompt">{{ prompt }}<span class="t-cursor"></span></div>
  </div>
</template>

<script setup>
// SVG-компонент ретро-монитора: строки текста, кастомные цвета primary/muted/bg, сканлайны, виньетка, мерцающий курсор
import { computed } from 'vue'

const props = defineProps({
  screenLines: {
    type: Array,
    default: () => [
      { text: 'GAME-LIBRARY', y: 50, size: 7, opacity: 0.8 },
      { text: 'AUTH v2.4.1', y: 64, size: 6, opacity: 0.6 },
      { text: '', y: 78, size: 6 },
      { text: 'login --user', y: 92, size: 6, opacity: 0.5 },
      { text: '', y: 106, size: 6 },
      { text: 'enter username...', y: 120, size: 6, opacity: 0.9 },
    ]
  },
  prompt: {
    type: String,
    default: 'root@glib:~$ login --user'
  },
  primary: { type: String, default: '#00ff88' },
  muted: { type: String, default: '#00cc6a' },
  bg: { type: String, default: '#003d22' },
  bgInner: { type: String, default: '#002a18' },
})

// Конвертация hex → RGB для CSS-переменной свечения
function hexToRgb(hex) {
  const r = parseInt(hex.slice(1, 3), 16)
  const g = parseInt(hex.slice(3, 5), 16)
  const b = parseInt(hex.slice(5, 7), 16)
  return { r, g, b }
}

// CSS-переменные для эффекта свечения
const cssVars = computed(() => {
  const c = hexToRgb(props.primary)
  return {
    '--crt-glow': `rgba(${c.r},${c.g},${c.b},0.08)`,
    '--crt-muted': props.muted,
  }
})
</script>

<style scoped>
.retro-crt-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  max-width: 340px;
  margin: 0 auto 1rem;
  position: relative;
}
.retro-crt-svg {
  width: 100%;
  height: auto;
  display: block;
  filter: drop-shadow(0 0 12px var(--crt-glow));
  position: relative;
  z-index: 2;
}
.crt-overlay {
  position: absolute;
  inset: 0;
  border-radius: 8px;
  background: repeating-linear-gradient(
    0deg,
    transparent 0px,
    transparent 2px,
    rgba(0,0,0,0.1) 2px,
    rgba(0,0,0,0.1) 4px
  );
  pointer-events: none;
  z-index: 3;
}
.vignette-overlay {
  position: absolute;
  inset: 0;
  border-radius: 8px;
  background: radial-gradient(ellipse at center, transparent 50%, rgba(0,0,0,0.6) 100%);
  pointer-events: none;
  z-index: 3;
}
.corner-overlay {
  position: absolute;
  inset: -1px;
  border-radius: 9px;
  border: 1px solid var(--crt-glow);
  pointer-events: none;
  z-index: 3;
}
.retro-crt-prompt {
  font-size: 0.75rem;
  color: var(--crt-muted);
  margin-top: 0.3rem;
  z-index: 2;
}
.t-cursor {
  display: inline-block;
  width: 0.45em;
  height: 0.8em;
  background: var(--crt-muted);
  margin-left: 0.1em;
  animation: blink 1s step-end infinite;
  vertical-align: text-bottom;
}
@keyframes blink {
  0%, 100% { opacity: 1 }
  50% { opacity: 0 }
}
.crt-cursor-text {
  animation: blink 1s step-end infinite;
}
</style>
