import Aura from '@primevue/themes/aura'
import { definePreset } from '@primevue/themes'

const RetroTerminal = definePreset(Aura, {
  semantic: {
    colorScheme: {
      light: {
        surface: {
          0:   "#f5f0e8",
          50:  "#ede7db",
          100: "#e5dece",
          200: "#d5ccb8",
          300: "#b8a98a",
          400: "#9a8a6a",
          500: "#7a6b4e",
          600: "#5c4a00",
          700: "#4a3c00",
          800: "#3a2f00",
          900: "#2a2200",
          950: "#1a1500",
        },
        primary: {
          50:  "#fef9eb",
          100: "#fcf0c8",
          200: "#f9e096",
          300: "#f5ca5c",
          400: "#c99b1a",
          500: "#8b6914",
          600: "#7a5b10",
          700: "#5c4a00",
          800: "#4a3c00",
          900: "#3a2f00",
          950: "#2a2200",
        },
        text: {
          color: "{surface.600}",
          hoverColor: "{surface.700}",
          mutedColor: "{surface.400}",
          hoverMutedColor: "{surface.500}",
        },
        content: {
          background: "{surface.0}",
          hoverBackground: "{surface.100}",
          borderColor: "rgba(92,74,0,0.2)",
          hoverBorderColor: "rgba(92,74,0,0.4)",
        },
      },
      dark: {
        surface: {
          0:   "#0a0a0a",
          50:  "#001a0d",
          100: "#00220f",
          200: "#003316",
          300: "#004d22",
          400: "#006630",
          500: "#00803d",
          600: "#00aa52",
          700: "#00cc6a",
          800: "#00e67a",
          900: "#00ff88",
          950: "#33ff9f",
        },
        primary: {
          50:  "#00220f",
          100: "#003316",
          200: "#004d22",
          300: "#006630",
          400: "#00994a",
          500: "#00cc6a",
          600: "#00e67a",
          700: "#00ff88",
          800: "#33ff9f",
          900: "#66ffb3",
          950: "#99ffcc",
        },
        text: {
          color: "#00ff88",
          hoverColor: "#33ff9f",
          mutedColor: "rgba(0,255,136,0.5)",
          hoverMutedColor: "rgba(0,255,136,0.7)",
        },
        content: {
          background: "rgba(0,26,13,0.4)",
          hoverBackground: "rgba(0,255,136,0.06)",
          borderColor: "rgba(0,255,136,0.2)",
          hoverBorderColor: "rgba(0,255,136,0.5)",
        },
      }
    }
  },
  components: {
    button: {
      colorScheme: {
        light: {
          root: {
            borderRadius: "2px",
          }
        },
        dark: {
          root: {
            borderRadius: "2px",
          }
        }
      }
    },
    inputtext: {
      colorScheme: {
        light: {
          root: {
            borderRadius: "2px",
          }
        },
        dark: {
          root: {
            borderRadius: "2px",
          }
        }
      }
    },
    card: {
      colorScheme: {
        light: {
          root: {
            borderRadius: "2px",
          }
        },
        dark: {
          root: {
            borderRadius: "2px",
          }
        }
      }
    }
  }
})

export default RetroTerminal
