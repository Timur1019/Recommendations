import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import App from './App.vue'
import { i18n, setLocale } from './i18n'

import 'bootstrap/dist/css/bootstrap.min.css'
import 'bootstrap'

import './styles/kurash-theme.css'
import './styles/kn-buttons.css'
import './styles/kn-data-tables.css'
import './styles/coach-ui.css'
import './styles/app.css'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(i18n)
app.config.errorHandler = (err, instance, info) => {
  console.error('[Vue]', err, info, instance)
}
app.mount('#app')

try {
  setLocale(i18n, i18n.global.locale.value)
} catch (e) {
  console.warn('[i18n] setLocale', e)
}
