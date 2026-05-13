<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import BaseButton from '../components/common/BaseButton.vue'
import { setLocale } from '../i18n'

const auth = useAuthStore()
const router = useRouter()
const { t, locale } = useI18n()

const currentLocale = computed(() => locale.value)

const navItems = computed(() => {
  const role = auth.user?.role
  const items = []
  if (role === 'ATHLETE') {
    items.push({ to: '/dashboard', label: t('nav.dashboard'), icon: 'dashboard' })
    items.push({ to: '/recommendations', label: t('nav.recommendations'), icon: 'military_tech' })
    items.push({ to: '/my-trainings', label: t('nav.myTrainings'), icon: 'fitness_center' })
    items.push({ to: '/achievement-journal', label: t('nav.achievementJournal'), icon: 'emoji_events' })
    items.push({ to: '/my-achievements-media', label: t('nav.achievementGallery'), icon: 'perm_media' })
  } else if (role === 'COACH') {
    items.push({ to: '/coach/cabinet', label: t('nav.coachCabinet'), icon: 'dashboard' })
    items.push({ to: '/coach/workspace', label: t('nav.coachWorkspace'), icon: 'view_cozy' })
    items.push({ to: '/coach/library', label: t('nav.trainingLibrary'), icon: 'menu_book' })
  } else if (role === 'ADMIN') {
    items.push({ to: '/admin/users', label: t('admin.navUsers'), icon: 'group' })
    items.push({ to: '/admin/library', label: t('admin.navLibrary'), icon: 'menu_book' })
    items.push({ to: '/admin/ai', label: t('admin.navAi'), icon: 'smart_toy' })
  }
  items.push({ to: '/profile', label: t('nav.profile'), icon: 'person' })
  return items
})

const topQuickLinks = computed(() => {
  const role = auth.user?.role
  if (role === 'ATHLETE') {
    return [
      { to: '/recommendations', label: t('common.aiPlan') },
      { to: '/my-trainings', label: t('nav.myTrainings') },
      { to: '/achievement-journal', label: t('nav.achievementJournal') },
      { to: '/my-achievements-media', label: t('nav.achievementGallery') },
      { to: '/profile', label: t('nav.profile') },
    ]
  }
  if (role === 'COACH') {
    return [
      { to: '/coach/cabinet', label: t('nav.coachCabinet') },
      { to: '/coach/workspace', label: t('nav.coachWorkspace') },
      { to: '/coach/library', label: t('nav.trainingLibrary') },
      { to: '/profile', label: t('nav.profile') },
    ]
  }
  if (role === 'ADMIN') {
    return [
      { to: '/admin/users', label: t('admin.navUsers') },
      { to: '/admin/library', label: t('admin.navLibrary') },
      { to: '/admin/ai', label: t('admin.navAi') },
      { to: '/profile', label: t('nav.profile') },
    ]
  }
  return []
})

const newSessionTarget = computed(() => {
  const r = auth.user?.role
  if (r === 'ATHLETE') return '/my-trainings'
  if (r === 'COACH') return '/coach/cabinet'
  if (r === 'ADMIN') return '/admin'
  return '/dashboard'
})

function logout() {
  auth.logout()
  router.push('/login')
}

function switchLang(next) {
  setLocale({ global: { locale } }, next)
}
</script>

<template>
  <div class="kn-shell">
    <aside class="kn-shell__sidebar kn-glass d-none d-lg-flex">
      <div class="kn-shell__sidebar-inner">
        <div>
          <div class="kn-shell__brand-title">{{ t('app.name') }}</div>
          <div class="kn-shell__brand-tag">{{ t('app.tagline') }}</div>
        </div>

        <nav class="kn-shell__nav" aria-label="Primary">
          <RouterLink
            v-for="item in navItems"
            :key="item.to"
            class="kn-nav-link"
            :to="item.to"
          >
            <span class="material-symbols-outlined" aria-hidden="true">{{ item.icon }}</span>
            <span class="font-headline">{{ item.label }}</span>
          </RouterLink>
        </nav>

        <div class="kn-shell__sidebar-footer">
          <RouterLink class="text-decoration-none" :to="newSessionTarget">
            <span class="kn-btn-cta w-100 d-flex align-items-center justify-content-center gap-2">
              <span class="material-symbols-outlined kn-shell__cta-icon" aria-hidden="true">add</span>
              {{ t('kn.newSession') }}
            </span>
          </RouterLink>

          <div class="kn-shell__lang-row mt-2">
            <button
              type="button"
              class="btn btn-sm"
              :class="currentLocale === 'ru' ? 'btn-primary' : 'btn-outline-primary'"
              @click="switchLang('ru')"
            >
              {{ t('lang.ru') }}
            </button>
            <button
              type="button"
              class="btn btn-sm"
              :class="currentLocale === 'uz' ? 'btn-primary' : 'btn-outline-primary'"
              @click="switchLang('uz')"
            >
              {{ t('lang.uz') }}
            </button>
          </div>

          <BaseButton variant="outline-secondary" block class="mt-2" @click="logout">
            {{ t('common.logout') }}
          </BaseButton>
        </div>
      </div>
    </aside>

    <div class="kn-shell__column">
      <header class="kn-shell__topbar kn-glass">
        <div class="d-flex align-items-center gap-2 min-w-0 flex-grow-1">
          <button
            class="btn btn-outline-primary btn-sm d-lg-none flex-shrink-0 kn-shell__menu-btn"
            type="button"
            data-bs-toggle="offcanvas"
            data-bs-target="#appShellNav"
            aria-controls="appShellNav"
          >
            <span class="material-symbols-outlined kn-shell__menu-icon" aria-hidden="true">menu</span>
            <span>{{ t('nav.menu') }}</span>
          </button>
          <div class="kn-search d-none d-md-block">
            <span class="material-symbols-outlined kn-search__icon" aria-hidden="true">search</span>
            <input class="kn-search__input" type="search" :placeholder="t('kn.searchPlaceholder')" readonly />
          </div>
        </div>

        <nav class="kn-shell__topbar-links" aria-label="Quick">
          <RouterLink v-for="l in topQuickLinks" :key="l.to" class="kn-top-link" :to="l.to">
            {{ l.label }}
          </RouterLink>
        </nav>

        <div class="d-flex align-items-center gap-2 flex-shrink-0">
          <RouterLink
            v-if="auth.user?.role === 'ATHLETE'"
            class="btn btn-sm btn-primary d-md-none rounded-pill px-3"
            to="/recommendations"
          >
            {{ t('common.aiPlan') }}
          </RouterLink>
          <button
            type="button"
            class="btn kn-shell__notify-btn d-none d-sm-inline-flex"
            aria-label="Notifications"
          >
            <span class="material-symbols-outlined kn-shell__notify-icon">notifications</span>
          </button>
          <div class="kn-shell__avatar d-none d-sm-flex" aria-hidden="true">
            <span class="material-symbols-outlined">account_circle</span>
          </div>
        </div>
      </header>

      <main class="kn-shell__main">
        <slot />
      </main>
    </div>

    <div
      id="appShellNav"
      class="offcanvas offcanvas-start"
      tabindex="-1"
      aria-labelledby="appShellNavLabel"
    >
      <div class="offcanvas-header border-bottom">
        <div id="appShellNavLabel" class="offcanvas-title fw-bold font-headline">{{ t('app.name') }}</div>
        <button type="button" class="btn-close" data-bs-dismiss="offcanvas" :aria-label="t('common.close')" />
      </div>
      <div class="offcanvas-body d-flex flex-column">
        <nav class="d-flex flex-column gap-1">
          <RouterLink
            v-for="item in navItems"
            :key="`m-${item.to}`"
            class="kn-nav-link"
            :to="item.to"
            data-bs-dismiss="offcanvas"
          >
            <span class="material-symbols-outlined" aria-hidden="true">{{ item.icon }}</span>
            <span class="font-headline">{{ item.label }}</span>
          </RouterLink>
        </nav>
        <div class="mt-auto pt-4 border-top">
          <RouterLink
            class="text-decoration-none d-block mb-3"
            :to="newSessionTarget"
            data-bs-dismiss="offcanvas"
          >
            <span class="kn-btn-cta w-100 d-flex align-items-center justify-content-center gap-2">
              <span class="material-symbols-outlined kn-shell__cta-icon" aria-hidden="true">add</span>
              {{ t('kn.newSession') }}
            </span>
          </RouterLink>
          <div class="d-flex gap-2 mb-3">
            <button
              type="button"
              class="btn btn-sm flex-grow-1"
              :class="currentLocale === 'ru' ? 'btn-primary' : 'btn-outline-primary'"
              data-bs-dismiss="offcanvas"
              @click="switchLang('ru')"
            >
              {{ t('lang.ru') }}
            </button>
            <button
              type="button"
              class="btn btn-sm flex-grow-1"
              :class="currentLocale === 'uz' ? 'btn-primary' : 'btn-outline-primary'"
              data-bs-dismiss="offcanvas"
              @click="switchLang('uz')"
            >
              {{ t('lang.uz') }}
            </button>
          </div>
          <BaseButton variant="outline-secondary" block data-bs-dismiss="offcanvas" @click="logout">
            {{ t('common.logout') }}
          </BaseButton>
        </div>
      </div>
    </div>
  </div>
</template>

<style src="./AppShell.css"></style>
