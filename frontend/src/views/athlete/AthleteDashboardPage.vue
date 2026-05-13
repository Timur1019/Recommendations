<script setup>
import { computed, onMounted, ref } from 'vue'
import AppShell from '../../layouts/AppShell.vue'
import { useAuthStore } from '../../stores/auth'
import { useAthleteStore } from '../../stores/athlete'
import { useTrainingStore } from '../../stores/training'
import { useAchievementStore } from '../../stores/achievement'
import { useI18n } from 'vue-i18n'
import { recommendationApi } from '../../services/api/recommendationApi'

const auth = useAuthStore()
const athleteStore = useAthleteStore()
const trainingStore = useTrainingStore()
const achievementStore = useAchievementStore()
const { t, locale } = useI18n()

const viewTab = ref('daily')
const latestRec = ref(null)

const RING_R = 80
const RING_C = 2 * Math.PI * RING_R

onMounted(() => {
  athleteStore.fetchMyProfile()
  trainingStore.fetchMyTrainings()
  trainingStore.fetchWeeklyStats()
  achievementStore.fetchMyAchievements()
  recommendationApi
    .latest()
    .then((data) => {
      latestRec.value = data
    })
    .catch(() => {
      latestRec.value = null
    })
})

const goldPct = computed(() => {
  const p = latestRec.value?.progressPercent
  if (typeof p === 'number' && !Number.isNaN(p)) {
    return Math.min(100, Math.max(0, p))
  }
  return 0
})

const ringOffset = computed(() => RING_C * (1 - goldPct.value / 100))

const techPct = computed(() => Math.min(100, goldPct.value + 12))
const staminaPct = computed(() => Math.max(0, goldPct.value - 10))

const recentTrainings = computed(() => trainingStore.trainings.slice(0, 5))

const medals = computed(() => ({
  gold: athleteStore.profile?.currentMedalCountGold ?? 0,
  silver: athleteStore.profile?.currentMedalCountSilver ?? 0,
  bronze: athleteStore.profile?.currentMedalCountBronze ?? 0,
}))

const regionLabel = computed(() => athleteStore.profile?.region || '—')
const weightLabel = computed(() => athleteStore.profile?.weightCategory || '—')
const rankLabel = computed(() => athleteStore.profile?.rank || '—')

const weeklyCount = computed(() => trainingStore.weekly?.count ?? 0)
const weeklyAvg = computed(() => {
  const w = trainingStore.weekly
  if (!w) return null
  const avg = typeof w.avgIntensity === 'number' ? w.avgIntensity : Number(w.avgIntensity || 0)
  return Number.isFinite(avg) ? avg : null
})

const recoveryLabel = computed(() => {
  const c = weeklyCount.value
  if (c >= 3) return t('kn.recoveryOptimal')
  if (c >= 1) return t('kn.recoveryGood')
  return t('kn.recoveryLow')
})

const recoveryWidthPct = computed(() => {
  const c = weeklyCount.value
  if (c >= 4) return 100
  if (c >= 2) return 75
  if (c >= 1) return 50
  return 25
})

function intensityMeta(tr) {
  const n = Number(tr.intensity)
  if (Number.isNaN(n)) {
    return { label: '—', dots: [false, false, false], dotClass: 'kn-intensity-dot--high' }
  }
  if (n >= 8) {
    return {
      label: t('kn.intensityHigh'),
      dots: [true, true, true],
      dotClass: 'kn-intensity-dot--high',
    }
  }
  if (n >= 5) {
    return {
      label: t('kn.intensityModerate'),
      dots: [true, true, false],
      dotClass: 'kn-intensity-dot--mid',
    }
  }
  return {
    label: t('kn.intensityLow'),
    dots: [true, false, false],
    dotClass: 'kn-intensity-dot--low',
  }
}

const trainingRows = computed(() => {
  void locale.value
  return recentTrainings.value.map((tr) => ({ tr, meta: intensityMeta(tr) }))
})
</script>

<template>
  <AppShell>
    <section class="d-flex flex-column flex-md-row justify-content-between align-items-md-end gap-4 mb-4 mb-md-5">
      <div>
        <h1 class="kn-dash-hero__title mb-2">
          {{ t('kn.greeting') }}, {{ auth.user?.fullName || t('auth.roleAthlete') }}!
        </h1>
        <div class="kn-dash-hero__meta">
          <span class="d-inline-flex align-items-center gap-1 fw-semibold kn-text-primary-deep">
            <span class="material-symbols-outlined kn-dash-icon-sm">location_on</span>
            {{ regionLabel }}
          </span>
          <span class="rounded-circle bg-secondary-subtle d-inline-block kn-dash-dot" aria-hidden="true" />
          <span class="badge rounded-pill px-3 py-2 fw-bold text-uppercase small kn-panel--high border-0">
            {{ t('kn.weightBadge') }}: {{ weightLabel }}
          </span>
        </div>
      </div>
      <div class="kn-dash-pill-toggle">
        <button
          type="button"
          class="kn-dash-pill-toggle__btn"
          :class="{ 'kn-dash-pill-toggle__btn--active': viewTab === 'daily' }"
          @click="viewTab = 'daily'"
        >
          {{ t('kn.dailyView') }}
        </button>
        <button
          type="button"
          class="kn-dash-pill-toggle__btn"
          :class="{ 'kn-dash-pill-toggle__btn--active': viewTab === 'history' }"
          @click="viewTab = 'history'"
        >
          {{ t('kn.performanceHistory') }}
        </button>
      </div>
    </section>

    <p v-if="viewTab === 'history'" class="small kn-text-muted mb-4">
      {{ t('kn.historyHint') }}
    </p>

    <div class="row g-4">
      <div class="col-12 col-lg-8">
        <div class="kn-panel kn-panel--white p-4 p-lg-5 kn-dash-feature min-vh-25">
          <div class="position-relative z-1">
            <p class="small fw-bold text-uppercase mb-1 kn-text-primary-deep kn-dash-kicker">
              {{ t('kn.statusLine') }}: {{ rankLabel }}
            </p>
            <h2 class="font-headline fw-black fs-3 mb-4">{{ t('athlete.progressToGold') }}</h2>
            <div class="d-flex flex-column flex-lg-row align-items-center align-items-lg-start gap-4 gap-lg-5">
              <div class="kn-dash-ring-wrap">
                <svg class="kn-dash-ring" viewBox="0 0 200 200" aria-hidden="true">
                  <circle class="kn-dash-ring__track" cx="100" cy="100" :r="RING_R" />
                  <circle
                    class="kn-dash-ring__value"
                    cx="100"
                    cy="100"
                    :r="RING_R"
                    :stroke-dasharray="RING_C"
                    :stroke-dashoffset="ringOffset"
                  />
                </svg>
                <div class="kn-dash-ring__label">
                  <span class="kn-dash-ring__pct">{{ goldPct }}%</span>
                  <span class="kn-dash-ring__sub">{{ t('kn.efficiency') }}</span>
                </div>
              </div>
              <div class="flex-grow-1 w-100">
                <div class="mb-4">
                  <div class="d-flex justify-content-between text-uppercase small fw-bold kn-text-muted mb-2">
                    <span>{{ t('kn.technicalMastery') }}</span>
                    <span>{{ techPct }}%</span>
                  </div>
                  <div class="kn-track">
                    <div class="kn-track__fill kn-track__fill--primary" :style="{ width: `${techPct}%` }" />
                  </div>
                </div>
                <div class="mb-4">
                  <div class="d-flex justify-content-between text-uppercase small fw-bold kn-text-muted mb-2">
                    <span>{{ t('kn.stamina') }}</span>
                    <span>{{ staminaPct }}%</span>
                  </div>
                  <div class="kn-track">
                    <div class="kn-track__fill kn-track__fill--secondary" :style="{ width: `${staminaPct}%` }" />
                  </div>
                </div>
                <RouterLink class="kn-dash-link-arrow" to="/recommendations">
                  {{ t('kn.viewRecommendations') }}
                  <span class="material-symbols-outlined kn-dash-icon-sm">arrow_forward</span>
                </RouterLink>
              </div>
            </div>
          </div>
          <span
            class="material-symbols-outlined kn-dash-feature__bg-icon material-symbols-outlined--fill"
            aria-hidden="true"
          >
            military_tech
          </span>
        </div>
      </div>

      <div class="col-12 col-lg-4 d-flex flex-column gap-4">
        <div class="kn-panel kn-panel--high p-4 p-lg-5 flex-grow-1 border-start border-4 border-warning">
          <h3 class="text-uppercase small fw-black mb-4 kn-dash-medals-title">
            {{ t('athlete.medals') }}
          </h3>
          <div class="d-flex flex-column gap-4">
            <div class="kn-dash-medal-row">
              <div class="d-flex align-items-center gap-3">
                <div class="kn-dash-medal-row__icon kn-dash-medal-row__icon--gold">
                  <span class="material-symbols-outlined material-symbols-outlined--fill">military_tech</span>
                </div>
                <span class="fw-bold">{{ t('athlete.gold') }}</span>
              </div>
              <span class="kn-dash-metric-num">{{ medals.gold }}</span>
            </div>
            <div class="kn-dash-medal-row">
              <div class="d-flex align-items-center gap-3">
                <div class="kn-dash-medal-row__icon bg-secondary bg-opacity-10 text-secondary">
                  <span class="material-symbols-outlined material-symbols-outlined--fill">military_tech</span>
                </div>
                <span class="fw-bold">{{ t('athlete.silver') }}</span>
              </div>
              <span class="kn-dash-metric-num">{{ medals.silver }}</span>
            </div>
            <div class="kn-dash-medal-row">
              <div class="d-flex align-items-center gap-3">
                <div class="kn-dash-medal-row__icon kn-dash-medal-row__icon--bronze">
                  <span class="material-symbols-outlined material-symbols-outlined--fill">military_tech</span>
                </div>
                <span class="fw-bold">{{ t('athlete.bronze') }}</span>
              </div>
              <span class="kn-dash-metric-num">{{ medals.bronze }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="col-12 col-lg-7">
        <div class="kn-panel kn-panel--low p-4 p-lg-5">
          <div class="d-flex justify-content-between align-items-center mb-4">
            <h3 class="font-headline fw-extrabold fs-5 mb-0">{{ t('athleteUi.recentTrainings') }}</h3>
            <RouterLink
              class="text-uppercase small fw-bold kn-dash-link-arrow border-bottom border-primary border-opacity-25 pb-1"
              to="/my-trainings"
            >
              {{ t('kn.fullLog') }}
            </RouterLink>
          </div>

          <div v-if="trainingStore.error" class="alert alert-danger py-2">{{ trainingStore.error }}</div>
          <div v-else-if="trainingStore.loading" class="d-flex align-items-center gap-2 kn-text-muted">
            <div class="spinner-border spinner-border-sm" aria-hidden="true" />
            {{ t('ui.loading') }}
          </div>
          <div v-else-if="recentTrainings.length === 0" class="text-center kn-text-muted py-5">
            {{ t('athleteUi.noTrainings') }}
          </div>
          <div v-else class="d-flex flex-column gap-3">
            <div
              v-for="{ tr, meta } in trainingRows"
              :key="tr.id"
              class="kn-training-row p-3 d-flex align-items-center justify-content-between gap-3"
            >
              <div class="d-flex align-items-center gap-3 min-w-0">
                <div class="rounded-circle d-flex align-items-center justify-content-center text-white kn-gradient flex-shrink-0 kn-dash-training-icon">
                  <span class="material-symbols-outlined kn-dash-training-glyph">fitness_center</span>
                </div>
                <div class="min-w-0">
                  <p class="fw-bold small mb-0 text-truncate">{{ tr.workoutType }}</p>
                  <p class="mb-0 kn-text-muted fw-medium kn-dash-training-meta">
                    {{ tr.trainingDate }} · {{ tr.durationMinutes }} {{ t('kn.min') }} · intensity {{ tr.intensity ?? '—' }}
                  </p>
                </div>
              </div>
              <div class="d-flex align-items-center gap-3 flex-shrink-0">
                <div class="text-end">
                  <span class="d-block text-uppercase fw-black small kn-dash-intensity-label">
                    {{ meta.label }}
                  </span>
                  <div class="kn-intensity-dots mt-1">
                    <span
                      v-for="(on, i) in meta.dots"
                      :key="i"
                      class="kn-dot-intensity"
                      :class="on ? meta.dotClass : 'bg-secondary-subtle'"
                    />
                  </div>
                </div>
                <span class="material-symbols-outlined kn-text-muted">chevron_right</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="col-12 col-lg-5 d-flex flex-column gap-4">
        <div class="kn-dash-tip kn-gradient p-4 p-lg-5 flex-grow-1">
          <div class="position-relative z-1">
            <div class="d-flex align-items-center gap-2 mb-3">
              <span class="material-symbols-outlined">lightbulb</span>
              <span class="text-uppercase fw-black small kn-dash-tip-kicker">
                {{ t('recommendations.tipOfDay') }}
              </span>
            </div>
            <p class="font-headline fw-bold fs-5 mb-3 lh-sm">
              {{ latestRec?.tipOfTheDay || t('kn.tipFallback') }}
            </p>
            <RouterLink to="/recommendations" class="btn btn-sm rounded-pill fw-bold kn-dash-tip-btn">
              {{ t('kn.learnMore') }}
            </RouterLink>
          </div>
          <span class="material-symbols-outlined position-absolute opacity-10 kn-dash-tip-deco">sports_kabaddi</span>
        </div>

        <div class="kn-dash-ai-card bg-success bg-opacity-10 p-4 d-flex align-items-center justify-content-between">
          <div class="d-flex align-items-center gap-3">
            <div class="bg-white rounded-circle d-flex align-items-center justify-content-center text-success flex-shrink-0 kn-dash-ai-icon">
              <span class="material-symbols-outlined">psychology</span>
            </div>
            <div>
              <h4 class="fw-bold small mb-1 text-success-emphasis">{{ t('kn.performanceAi') }}</h4>
              <p class="mb-0 small text-success text-opacity-75">{{ t('kn.aiSubtitle') }}</p>
            </div>
          </div>
          <RouterLink to="/recommendations" class="kn-btn-icon text-decoration-none" aria-label="Recommendations">
            <span class="material-symbols-outlined text-white">trending_up</span>
          </RouterLink>
        </div>

        <div class="kn-panel kn-panel--white p-4">
          <div class="fw-bold mb-2">{{ t('athleteUi.achievements') }}</div>
          <div v-if="achievementStore.error" class="alert alert-danger py-2">{{ achievementStore.error }}</div>
          <div v-else class="d-flex justify-content-between align-items-center">
            <span class="kn-text-muted">{{ t('athleteUi.total') }}</span>
            <span class="kn-dash-metric-num">{{ achievementStore.achievements.length }}</span>
          </div>
        </div>
      </div>
    </div>

    <section class="row g-4 mt-2">
      <div class="col-md-4">
        <div class="kn-panel kn-panel--muted p-4">
          <span class="d-block text-uppercase small fw-bold kn-text-muted mb-2 kn-dash-metric-kicker">
            {{ t('kn.metricTrainingsWeek') }}
          </span>
          <div class="d-flex align-items-end gap-2">
            <span class="kn-dash-metric-num">{{ weeklyCount }}</span>
          </div>
          <div class="kn-track mt-2">
            <div class="kn-track__fill bg-danger" :style="{ width: `${Math.min(100, weeklyCount * 25)}%` }" />
          </div>
        </div>
      </div>
      <div class="col-md-4">
        <div class="kn-panel kn-panel--muted p-4">
          <span class="d-block text-uppercase small fw-bold kn-text-muted mb-2 kn-dash-metric-kicker">
            {{ t('kn.metricAvgIntensity') }}
          </span>
          <div class="d-flex align-items-end gap-2">
            <span class="kn-dash-metric-num">{{ weeklyAvg != null ? weeklyAvg.toFixed(1) : '—' }}</span>
          </div>
          <div class="kn-track mt-2">
            <div
              class="kn-track__fill bg-primary"
              :style="{ width: weeklyAvg != null ? `${Math.min(100, weeklyAvg * 10)}%` : '0%' }"
            />
          </div>
        </div>
      </div>
      <div class="col-md-4">
        <div class="kn-panel kn-panel--muted p-4">
          <span class="d-block text-uppercase small fw-bold kn-text-muted mb-2 kn-dash-metric-kicker">
            {{ t('kn.metricRecovery') }}
          </span>
          <div class="d-flex align-items-end gap-2">
            <span class="kn-dash-metric-num fs-5">{{ recoveryLabel }}</span>
            <span class="material-symbols-outlined text-success material-symbols-outlined--fill mb-1">check_circle</span>
          </div>
          <div class="kn-track mt-2">
            <div class="kn-track__fill kn-track__fill--secondary" :style="{ width: `${recoveryWidthPct}%` }" />
          </div>
        </div>
      </div>
    </section>
  </AppShell>
</template>

<style src="./AthleteDashboardPage.css"></style>
<style scoped>
.kn-intensity-dot--high {
  background-color: var(--kn-error);
}
.kn-intensity-dot--mid {
  background-color: var(--kn-secondary);
}
.kn-intensity-dot--low {
  background-color: var(--kn-on-surface-variant);
}
</style>
