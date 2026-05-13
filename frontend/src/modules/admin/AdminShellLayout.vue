<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import AppShell from '../../layouts/AppShell.vue'

const { t } = useI18n()
const route = useRoute()

const lead = computed(() => {
  const p = route.path
  if (p.includes('/admin/library')) return t('admin.moduleLibraryLead')
  if (p.includes('/admin/ai')) return t('admin.moduleAiLead')
  return t('admin.moduleUsersLead')
})
</script>

<template>
  <AppShell>
    <header class="kn-page-header mb-0">
      <h1 class="kn-page-header__title">{{ t('admin.panelTitle') }}</h1>
      <p class="kn-page-header__desc mb-3 mb-md-4">{{ lead }}</p>

      <ul class="nav nav-pills flex-wrap gap-1 gap-md-2 mb-3 admin-shell-nav">
        <li class="nav-item">
          <RouterLink class="nav-link rounded-pill px-3" to="/admin/users" active-class="active">
            {{ t('admin.navUsers') }}
          </RouterLink>
        </li>
        <li class="nav-item">
          <RouterLink class="nav-link rounded-pill px-3" to="/admin/library" active-class="active">
            {{ t('admin.navLibrary') }}
          </RouterLink>
        </li>
        <li class="nav-item">
          <RouterLink class="nav-link rounded-pill px-3" to="/admin/ai" active-class="active">
            {{ t('admin.navAi') }}
          </RouterLink>
        </li>
      </ul>
    </header>

    <router-view />
  </AppShell>
</template>

<style src="./admin-ui.css"></style>

<style scoped>
.admin-shell-nav .nav-link {
  color: var(--kn-primary, #003461);
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(0, 52, 97, 0.12);
  font-weight: 600;
  transition:
    background 0.15s ease,
    color 0.15s ease,
    box-shadow 0.15s ease,
    border-color 0.15s ease;
}

.admin-shell-nav .nav-link:hover {
  border-color: rgba(0, 52, 97, 0.22);
  box-shadow: var(--kn-shadow-xs);
}

.admin-shell-nav .nav-link.active {
  background: linear-gradient(180deg, var(--kn-primary-container, #004b87) 0%, var(--kn-primary, #003461) 100%);
  color: #fff;
  border-color: transparent;
  box-shadow: var(--kn-shadow-sm), 0 4px 14px rgba(0, 52, 97, 0.22);
  font-weight: 700;
}
</style>
