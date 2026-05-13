import { createI18n } from 'vue-i18n'
import ru from './ru.json'
import uz from './uz.json'

const STORAGE_KEY = 'kn_lang'

export function getInitialLocale() {
  const saved = localStorage.getItem(STORAGE_KEY)
  return saved === 'uz' || saved === 'ru' ? saved : 'ru'
}

export function setLocale(i18n, locale) {
  if (i18n?.global?.locale?.value != null) {
    i18n.global.locale.value = locale
  } else if (i18n?.locale?.value != null) {
    i18n.locale.value = locale
  }
  localStorage.setItem(STORAGE_KEY, locale)
  document.documentElement.lang = locale
}

export const i18n = createI18n({
  legacy: false,
  locale: getInitialLocale(),
  fallbackLocale: 'ru',
  messages: { ru, uz },
})

