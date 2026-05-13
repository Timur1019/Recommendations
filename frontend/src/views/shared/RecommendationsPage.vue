<script setup>
import { computed, onMounted } from 'vue'
import AppShell from '../../layouts/AppShell.vue'
import BaseButton from '../../components/common/BaseButton.vue'
import { useI18n } from 'vue-i18n'
import { useRecommendationStore } from '../../stores/recommendation'
import { useAuthStore } from '../../stores/auth'

const { t } = useI18n()
const rec = useRecommendationStore()
const auth = useAuthStore()

onMounted(() => {
  if (auth.user?.role === 'ATHLETE') {
    rec.fetchAll()
  }
})

const deficits = computed(() => rec.comparison?.deficits || [])
const weekPlan = computed(() => rec.weekPlan || rec.latest?.weekPlan || {})
const tipOfDay = computed(() => rec.latest?.tipOfTheDay || rec.comparison?.tipOfTheDay || null)

function fmtNum(v) {
  if (v == null || Number.isNaN(Number(v))) return '—'
  const n = Number(v)
  return Number.isInteger(n) ? String(n) : n.toFixed(1)
}
</script>

<template>
  <AppShell>
    <header class="kn-page-header">
      <h1 class="kn-page-header__title">{{ t('recommendations.title') }}</h1>
      <p class="kn-page-header__desc">{{ t('recommendations.subtitle') }}</p>
    </header>
    <div v-if="rec.error" class="alert alert-warning py-2 mb-3">
      {{ rec.error }}
    </div>
    <div class="row g-3">
      <div class="col-12">
        <div class="app-card p-4 d-flex justify-content-between align-items-start flex-wrap gap-3">
          <div>
            <div class="text-uppercase small fw-bold kn-text-muted kn-kicker">
              {{ t('common.aiPlan') }}
            </div>
            <div class="app-muted small mt-1">
              <template v-if="rec.latest">
                {{ t('recommendations.generated') }}: {{ rec.latest.generatedDate }} ·
                {{ t('recommendations.progress') }}: {{ rec.latest.progressPercent }}%
              </template>
              <template v-else>{{ t('recommendations.hintNoLatest') }}</template>
            </div>
          </div>
          <div class="d-flex flex-wrap gap-2">
            <BaseButton class="rounded-pill px-3" variant="primary" :loading="rec.loading" @click="rec.generateMe()">
              {{ t('recommendations.generateNew') }}
            </BaseButton>
            <BaseButton
              class="rounded-pill px-3"
              variant="outline-primary"
              :loading="rec.exporting"
              :disabled="rec.loading"
              @click="rec.exportPdf()"
            >
              {{ t('recommendations.exportPdf') }}
            </BaseButton>
          </div>
        </div>
      </div>

      <div class="col-12 col-lg-6">
        <div class="app-card bg-white p-4 h-100">
          <div class="fw-bold mb-3">{{ t('recommendations.comparisonTitle') }}</div>

          <div v-if="rec.loading" class="d-flex align-items-center gap-2 app-muted">
            <div class="spinner-border spinner-border-sm" aria-hidden="true"></div>
            {{ t('ui.loading') }}
          </div>

          <div class="table-responsive">
            <table class="table table-sm align-middle">
              <thead>
                <tr class="text-nowrap">
                  <th>{{ t('recommendations.parameter') }}</th>
                  <th class="text-end">{{ t('recommendations.current') }}</th>
                  <th class="text-end">{{ t('recommendations.target') }}</th>
                  <th class="text-end">{{ t('recommendations.diff') }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="d in deficits" :key="d.parameter">
                  <td class="fw-semibold">{{ d.parameter }}</td>
                  <td class="text-end">{{ fmtNum(d.current) }}</td>
                  <td class="text-end">{{ fmtNum(d.target) }}</td>
                  <td class="text-end">
                    <span class="badge" :class="d.difference < 0 ? 'text-bg-danger' : 'text-bg-success'">
                      {{ fmtNum(d.difference) }}
                    </span>
                  </td>
                </tr>
                <tr v-if="!rec.loading && deficits.length === 0">
                  <td colspan="4" class="text-center app-muted py-4">{{ t('ui.empty') }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <div class="col-12 col-lg-6">
        <div class="app-card bg-white p-4 h-100">
          <div class="fw-bold mb-3">{{ t('recommendations.weekPlanTitle') }}</div>
          <div class="row g-2">
            <div v-for="(v, k) in weekPlan" :key="k" class="col-12 col-md-6">
              <div class="border rounded-3 p-3 h-100">
                <div class="small text-uppercase app-muted fw-semibold" style="letter-spacing: 0.12em">
                  {{ k }}
                </div>
                <div class="fw-semibold mt-1">{{ v }}</div>
              </div>
            </div>
          </div>
          <div class="alert alert-warning mt-3 mb-0">
            <span class="fw-semibold">{{ t('recommendations.tipOfDay') }}:</span>
            {{ tipOfDay || '—' }}
          </div>
        </div>
      </div>
    </div>
  </AppShell>
</template>

