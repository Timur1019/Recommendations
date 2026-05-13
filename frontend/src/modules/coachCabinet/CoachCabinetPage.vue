<script setup>
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import AppShell from '../../layouts/AppShell.vue'
import { useAuthStore } from '../../stores/auth'
import { useCoachStore } from '../../stores/coach'
import CoachCabinetSection from './CoachCabinetSection.vue'
import CoachAddTrainingModal from '../../components/coach/CoachAddTrainingModal.vue'
import CoachCohortInsightModal from '../../components/coach/CoachCohortInsightModal.vue'

const auth = useAuthStore()
const coachStore = useCoachStore()
const { t } = useI18n()

const weightFilter = ref('')
const addTrainingModalRef = ref(null)
const cohortModalRef = ref(null)

onMounted(() => {
  coachStore.fetchMe()
})

const athletes = computed(() => coachStore.athletes)

const filteredAthletes = computed(() => {
  const w = weightFilter.value
  if (!w) return athletes.value
  return athletes.value.filter((a) => String(a.weightCategory) === w)
})

const totalAthletes = computed(() => athletes.value.length)

function progressForAthlete(id) {
  if (id == null) return 0
  let h = 0
  const s = String(id)
  for (let i = 0; i < s.length; i += 1) {
    h = (h * 31 + s.charCodeAt(i)) % 97
  }
  return 55 + (h % 38)
}

const avgProgress = computed(() => {
  if (!athletes.value.length) return null
  const sum = athletes.value.reduce((acc, a) => acc + progressForAthlete(a.id), 0)
  return Math.round(sum / athletes.value.length)
})

function onCabinetAddTraining(athleteId) {
  addTrainingModalRef.value?.showForAthlete(athleteId)
}

function onCabinetViewProgress(athleteId) {
  cohortModalRef.value?.show({ athleteId, runCohort: false })
}

function onCabinetAiRecommendation(athleteId) {
  cohortModalRef.value?.show({ athleteId, runCohort: true })
}
</script>

<template>
  <AppShell>
    <p class="small text-uppercase fw-bold kn-text-muted mb-2">{{ t('coach.moduleWorkspaceKicker') }}</p>

    <section class="row g-4 mb-4 mb-md-5">
      <div class="col-md-6 col-xl-3">
        <div class="kn-coach-stat-card p-4 h-100 d-flex flex-column justify-content-between">
          <div>
            <p class="text-uppercase small fw-bold kn-text-muted mb-1 kn-coach-stat-kicker">
              {{ t('kn.totalAthletes') }}
            </p>
            <p class="kn-dash-metric-num text-primary mb-0">{{ totalAthletes }}</p>
          </div>
          <div class="d-flex align-items-center gap-2 text-success fw-semibold small mt-3">
            <span class="material-symbols-outlined kn-coach-trend-icon">trending_up</span>
            {{ t('kn.coachTrendUp') }}
          </div>
        </div>
      </div>
      <div class="col-md-6 col-xl-3">
        <div class="kn-coach-stat-card p-4 h-100 d-flex flex-column justify-content-between">
          <div>
            <p class="text-uppercase small fw-bold kn-text-muted mb-1 kn-coach-stat-kicker">
              {{ t('kn.avgProgress') }}
            </p>
            <p class="kn-dash-metric-num text-primary mb-0">
              {{ avgProgress != null ? `${avgProgress}%` : '—' }}
            </p>
          </div>
          <div class="d-flex align-items-center gap-2 text-success fw-semibold small mt-3">
            <span class="material-symbols-outlined kn-coach-trend-icon">check_circle</span>
            {{ t('kn.coachTrendOk') }}
          </div>
        </div>
      </div>
      <div class="col-xl-6">
        <div class="kn-coach-banner p-4 d-flex flex-column flex-sm-row align-items-start align-items-sm-center justify-content-between gap-3">
          <div class="position-relative z-1">
            <p class="text-uppercase small fw-bold mb-1 opacity-90 kn-coach-banner-kicker">
              {{ t('kn.coachBannerTitle') }}
            </p>
            <h2 class="fw-bold fs-5 text-white mb-2">{{ t('athlete.medals') }} · Kurash Nation</h2>
            <p class="small mb-0 text-white text-opacity-75 kn-coach-banner-text">
              {{ t('kn.coachBannerText') }}
            </p>
          </div>
          <a
            href="#coach-athletes-cabinet"
            class="btn btn-light btn-sm rounded-pill fw-bold position-relative z-1 text-primary"
          >
            {{ t('kn.viewAwards') }}
          </a>
          <span class="material-symbols-outlined text-white kn-coach-banner__deco material-symbols-outlined--fill">
            military_tech
          </span>
        </div>
      </div>
    </section>

    <CoachCabinetSection
      v-model:weight-filter="weightFilter"
      :athletes="filteredAthletes"
      :loading="coachStore.loading"
      :error="coachStore.error"
      :coach-full-name="auth.user?.fullName || ''"
      @add-training="onCabinetAddTraining"
      @view-progress="onCabinetViewProgress"
      @ai-recommendation="onCabinetAiRecommendation"
    />

    <CoachAddTrainingModal ref="addTrainingModalRef" :athletes="coachStore.athletes" />
    <CoachCohortInsightModal ref="cohortModalRef" :athletes="coachStore.athletes" />
  </AppShell>
</template>

