<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps({
  athletes: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  error: { type: String, default: null },
  coachFullName: { type: String, default: '' },
  weightFilter: { type: String, default: '' },
})

const emit = defineEmits([
  'update:weightFilter',
  'add-training',
  'view-progress',
  'ai-recommendation',
])

const { t } = useI18n()

const weightModel = computed({
  get: () => props.weightFilter,
  set: (v) => emit('update:weightFilter', v),
})

function progressForAthlete(id) {
  if (id == null) return 0
  let h = 0
  const s = String(id)
  for (let i = 0; i < s.length; i += 1) {
    h = (h * 31 + s.charCodeAt(i)) % 97
  }
  return 55 + (h % 38)
}

function isGoldStandard(id) {
  return progressForAthlete(id) >= 88
}
</script>

<template>
  <section id="coach-athletes-cabinet" class="coach-cabinet">
    <div class="d-flex flex-column flex-md-row align-items-md-end justify-content-between gap-4 mb-4">
      <div>
        <h1 class="kn-coach-hero__title mb-1">{{ t('coach.dashboardTitle') }}</h1>
        <p class="small kn-text-muted mb-0">{{ t('kn.coachSubtitle') }} · {{ coachFullName }}</p>
      </div>
      <div class="d-flex align-items-center gap-2">
        <div class="kn-coach-filter-wrap">
          <select v-model="weightModel" class="kn-coach-filter-select">
            <option value="">{{ t('kn.filterWeights') }}</option>
            <option value="60">60</option>
            <option value="66">66</option>
            <option value="73">73</option>
            <option value="81">81</option>
            <option value="90">90</option>
            <option value="100">100</option>
            <option value="100+">100+</option>
          </select>
          <span class="material-symbols-outlined kn-coach-filter-icon kn-coach-filter-chevron">expand_more</span>
        </div>
        <button type="button" class="btn btn-light rounded-circle p-2 border-0 kn-panel--high" aria-label="Filter">
          <span class="material-symbols-outlined text-primary">filter_list</span>
        </button>
      </div>
    </div>

    <template v-if="error">
      <div class="alert alert-danger">{{ error }}</div>
    </template>
    <template v-else-if="loading">
      <div class="d-flex align-items-center gap-2 kn-text-muted py-5 justify-content-center">
        <div class="spinner-border spinner-border-sm" aria-hidden="true" />
        {{ t('ui.loading') }}
      </div>
    </template>
    <div v-else class="row g-4">
      <div v-for="a in athletes" :key="a.id" class="col-12 col-xl-6">
        <div
          class="kn-athlete-card p-4 d-flex flex-column flex-md-row gap-4"
          :class="{ 'kn-athlete-card--accent': isGoldStandard(a.id) }"
        >
          <div class="flex-shrink-0 position-relative">
            <div class="kn-coach-avatar d-flex align-items-center justify-content-center kn-text-muted">
              <span class="material-symbols-outlined kn-coach-person-placeholder">person</span>
            </div>
            <span
              v-if="isGoldStandard(a.id)"
              class="position-absolute badge rounded text-uppercase fw-black small px-2 py-1 kn-coach-gold-badge"
            >
              Gold
            </span>
          </div>
          <div class="flex-grow-1 d-flex flex-column justify-content-between">
            <div class="d-flex justify-content-between align-items-start gap-2">
              <div class="min-w-0">
                <h3 class="fs-6 fw-bold mb-1 text-truncate">{{ a.user?.fullName || '—' }}</h3>
                <p class="small kn-text-muted mb-0 fw-medium">
                  {{ t('auth.weightCategory') }}: {{ a.weightCategory || '—' }}
                </p>
              </div>
              <div class="text-end flex-shrink-0">
                <span class="d-block kn-dash-metric-num text-primary lh-1">{{ progressForAthlete(a.id) }}%</span>
                <span class="text-uppercase fw-bold kn-text-muted kn-coach-progress-label">
                  {{ t('recommendations.progress') }}
                </span>
              </div>
            </div>
            <div class="mt-3">
              <div class="kn-track kn-coach-progress-track">
                <div
                  class="kn-track__fill kn-track__fill--secondary kn-coach-progress-fill"
                  :style="{ width: `${progressForAthlete(a.id)}%` }"
                />
              </div>
            </div>
            <div class="d-flex flex-wrap gap-2 mt-4">
              <button
                type="button"
                class="kn-coach-action kn-coach-action--primary"
                @click="emit('add-training', a.id)"
              >
                <span class="material-symbols-outlined">add_task</span>
                {{ t('coach.addTraining') }}
              </button>
              <button
                type="button"
                class="kn-coach-action kn-coach-action--soft"
                @click="emit('view-progress', a.id)"
              >
                <span class="material-symbols-outlined">analytics</span>
                {{ t('kn.viewProgress') }}
              </button>
              <button
                type="button"
                class="kn-coach-action kn-coach-action--ghost"
                @click="emit('ai-recommendation', a.id)"
              >
                <span class="material-symbols-outlined">smart_toy</span>
                {{ t('kn.generateRec') }}
              </button>
            </div>
          </div>
        </div>
      </div>
      <div v-if="athletes.length === 0" class="col-12">
        <div class="kn-coach-stat-card p-5 text-center kn-text-muted">{{ t('ui.empty') }}</div>
      </div>
    </div>

    <div v-if="!loading && athletes.length > 0" class="d-flex justify-content-center mt-5">
      <button type="button" class="kn-coach-load-more" disabled title="Demo">{{ t('kn.loadMore') }}</button>
    </div>
  </section>
</template>

<style scoped>
.coach-cabinet {
  scroll-margin-top: 1rem;
}
</style>
