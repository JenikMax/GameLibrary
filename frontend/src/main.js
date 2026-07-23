import { createApp } from 'vue'
import { createPinia } from 'pinia'
import PrimeVue from 'primevue/config'
import Aura from '@primevue/themes/aura'

import Tooltip from 'primevue/tooltip'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import router from './router'

import App from './App.vue'
import './assets/styles/main.css'
import './assets/styles/primeflex.css'
import 'primeicons/primeicons.css'
import './assets/styles/themes/default-light.css'
import './assets/styles/themes/default-dark.css'
import './assets/styles/themes/retro-terminal.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(PrimeVue, {
  theme: {
    preset: Aura,
    options: {
      darkModeSelector: '[data-color-scheme="dark"]'
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
