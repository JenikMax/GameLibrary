import { createApp } from 'vue'
import { createPinia } from 'pinia'
import PrimeVue from 'primevue/config'
import Aura from '@primevue/themes/aura'
import { definePreset } from '@primevue/themes'

const WarmAura = definePreset(Aura, {
  semantic: {
    colorScheme: {
      light: {
        surface: {
          0:   "#f5f3f0",
          50:  "#edeae6",
          100: "#e3e0db",
          200: "#d5d1cb",
          300: "#b0aba3",
          400: "#8c877e",
          500: "#6e6a63",
          600: "#534f49",
          700: "#3d3a36",
          800: "#282623",
          900: "#181716",
          950: "#0c0b0a",
        },
        text: {
          color: "{surface.600}",
          hoverColor: "{surface.700}",
          mutedColor: "{surface.500}",
          hoverMutedColor: "{surface.600}",
        },
      }
    }
  }
})

import Tooltip from 'primevue/tooltip'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import router from './router'

import App from './App.vue'
import './assets/styles/main.css'
import './assets/styles/primeflex.css'
import 'primeicons/primeicons.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(PrimeVue, {
  theme: {
    preset: WarmAura,
    options: {
      darkModeSelector: '.app-dark'
    }
  }
})
app.use(ToastService)
app.use(ConfirmationService)
app.directive('tooltip', Tooltip)

app.config.errorHandler = (err, instance, info) => {
  console.error('[Vue Error]', err)
  console.error('Component:', instance?.$options?.name || instance?.$options?.__name || 'unknown')
  console.error('Info:', info)
}

window.addEventListener('unhandledrejection', (event) => {
  console.error('[Unhandled Promise Rejection]', event.reason)
})

app.mount('#app')
