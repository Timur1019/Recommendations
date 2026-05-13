<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import AppShell from '../../layouts/AppShell.vue'
import { useI18n } from 'vue-i18n'
import { useCoachStore } from '../../stores/coach'
import { useRoute, useRouter } from 'vue-router'
import { achievementApi } from '../../services/api/achievementApi'
import { trainingRequestApi } from '../../services/api/trainingRequestApi'
import CoachTrainingJournalSection from '../../components/coach/CoachTrainingJournalSection.vue'

const { t } = useI18n()
const coachStore = useCoachStore()
const route = useRoute()
const router = useRouter()

const journalRef = ref(null)
const cohortLoading = ref(false)
const cohortError = ref('')
const cohortInsight = ref(null)
const focusAthleteId = ref(null)
const trainingRequests = ref([])
const trLoading = ref(false)
const trError = ref(null)

onMounted(() => {
  coachStore.fetchMe()
  loadTrainingRequests()
})

async function loadTrainingRequests() {
  trLoading.value = true
  trError.value = null
  try {
    trainingRequests.value = await trainingRequestApi.listCoach()
  } catch (e) {
    trError.value = e?.response?.data?.message || t('coach.trainingRequestsLoadFailed')
    trainingRequests.value = []
  } finally {
    trLoading.value = false
  }
}

function formatReqDate(iso) {
  if (!iso) return '—'
  try {
    return new Date(iso).toLocaleString()
  } catch {
    return String(iso)
  }
}

function requestStatusBadgeClass(status) {
  const s = String(status || '').toUpperCase()
  if (s === 'APPROVED') return 'kn-badge-status--approved'
  if (s === 'REJECTED') return 'kn-badge-status--rejected'
  return 'kn-badge-status--pending'
}

const athletes = computed(() => coachStore.athletes)

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

const focusAthleteName = computed(() => {
  if (focusAthleteId.value == null) return ''
  const a = athletes.value.find((x) => x.id === focusAthleteId.value)
  return a?.user?.fullName || ''
})

function setFocusAthleteFromRoute() {
  const raw = route.query?.focusAthleteId
  if (raw == null || raw === '') {
    focusAthleteId.value = null
    return
  }
  const n = Number(raw)
  focusAthleteId.value = Number.isFinite(n) ? n : null
}

async function maybeOpenTrainingFromRoute() {
  const raw = route.query?.openTrainingFor
  if (raw == null || raw === '') return
  const id = Number(raw)
  if (!Number.isFinite(id)) return

  if (coachStore.loading) return
  await nextTick()
  journalRef.value?.scrollToJournalAndOpen(id)

  const nextQuery = { ...route.query }
  delete nextQuery.openTrainingFor
  router.replace({ query: nextQuery })
}

async function maybeRunCohortFromRoute() {
  if (route.query?.runCohort !== '1') return
  if (coachStore.loading) return
  await loadCohortInsight()
  const nextQuery = { ...route.query }
  delete nextQuery.runCohort
  router.replace({ query: nextQuery })
}

async function loadCohortInsight() {
  cohortLoading.value = true
  cohortError.value = ''
  try {
    cohortInsight.value = await achievementApi.cohortInsight(null)
  } catch (e) {
    cohortError.value = e?.response?.data?.message || t('journal.cohortFailed')
    cohortInsight.value = null
  } finally {
    cohortLoading.value = false
  }
}

function scrollToCohort() {
  document.getElementById('coach-cohort-block')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function scrollToJournal() {
  const root = document.getElementById('coach-training-journal')
  root?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

async function maybeScrollFromRoute() {
  if (coachStore.loading) return
  const target = route.query?.scrollTo
  if (target === 'cohort') {
    scrollToCohort()
    const nextQuery = { ...route.query }
    delete nextQuery.scrollTo
    router.replace({ query: nextQuery })
    return
  }
  if (target === 'journal') {
    await nextTick()
    scrollToJournal()
    const nextQuery = { ...route.query }
    delete nextQuery.scrollTo
    router.replace({ query: nextQuery })
  }
}

watch(
  () => route.query,
  async () => {
    setFocusAthleteFromRoute()
    await maybeRunCohortFromRoute()
    await maybeOpenTrainingFromRoute()
    await maybeScrollFromRoute()
  },
  { immediate: true },
)

watch(
  () => coachStore.loading,
  async (loading) => {
    if (loading) return
    await maybeRunCohortFromRoute()
    await maybeOpenTrainingFromRoute()
    await maybeScrollFromRoute()
  },
)
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
            <p class="kn-dash-metric-num text-primary mb-0">{{ avgProgress != null ? `${avgProgress}%` : '—' }}</p>
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

    <div id="coach-cohort-block" class="kn-coach-stat-card p-4 mb-4">
      <div class="kn-coach-toolbar mb-0">
        <div>
          <h2 class="kn-coach-card-title">{{ t('journal.cohortTitle') }}</h2>
          <div class="small kn-text-muted">{{ t('journal.cohortSubtitle') }}</div>
        </div>
        <button
          type="button"
          class="btn btn-primary rounded-pill px-4"
          :disabled="cohortLoading"
          @click="loadCohortInsight"
        >
          {{ cohortLoading ? t('ui.loading') : t('journal.runCohortInsight') }}
        </button>
      </div>
      <div v-if="focusAthleteName" class="alert alert-info py-2 small mt-3 mb-0">
        {{ t('coach.cohortFocusHint', { name: focusAthleteName }) }}
      </div>
      <div v-if="cohortError" class="alert alert-danger mt-3 mb-0 py-2">{{ cohortError }}</div>
      <div v-if="cohortInsight" class="mt-3 small">
        <p class="mb-3">{{ cohortInsight.analysisText }}</p>
        <div class="kn-data-table-wrap">
          <div class="kn-data-table-responsive kn-data-table-responsive--short">
            <table class="table table-sm align-middle mb-0 kn-data-table kn-data-table--coach-cohort">
              <thead>
                <tr>
                  <th class="kn-th--day">{{ t('journal.colDay') }}</th>
                  <th class="kn-th--count">{{ t('journal.activityCount') }}</th>
                  <th class="kn-th--patterns">{{ t('journal.topPatterns') }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in cohortInsight.chartByWeekday" :key="row.dayOfWeek">
                  <td class="kn-td--day fw-semibold">{{ row.dayOfWeek }}</td>
                  <td class="kn-td--count">{{ row.activityCount }}</td>
                  <td class="kn-td--patterns">{{ (row.sampleActivities || []).join('; ') }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <CoachTrainingJournalSection
      v-if="!coachStore.loading"
      ref="journalRef"
      :athletes="coachStore.athletes"
    />

    <div class="kn-coach-stat-card p-4 mb-4">
      <div class="kn-coach-toolbar">
        <div>
          <h2 class="kn-coach-card-title">{{ t('coach.trainingRequestsTitle') }}</h2>
          <div class="small kn-text-muted">{{ t('coach.trainingRequestsHint') }}</div>
        </div>
        <button
          type="button"
          class="btn btn-outline-secondary btn-sm rounded-pill px-3"
          :disabled="trLoading"
          @click="loadTrainingRequests"
        >
          {{ t('ui.refresh') }}
        </button>
      </div>
      <div v-if="trError" class="alert alert-danger py-2 mb-0">{{ trError }}</div>
      <template v-else-if="trLoading">
        <div class="d-flex align-items-center gap-2 kn-text-muted small py-2">
          <div class="spinner-border spinner-border-sm" aria-hidden="true" />
          {{ t('ui.loading') }}
        </div>
      </template>
      <div v-else class="kn-data-table-wrap">
        <div class="kn-data-table-responsive kn-data-table-responsive--auto">
          <table class="table table-sm align-middle mb-0 kn-data-table kn-data-table--coach-requests">
            <thead>
              <tr>
                <th class="kn-th--id">{{ t('athlete.colId') }}</th>
                <th class="kn-th--created">{{ t('athlete.requestColCreated') }}</th>
                <th class="kn-th--athlete">{{ t('coach.colAthlete') }}</th>
                <th class="kn-th--status">{{ t('athlete.requestColStatus') }}</th>
                <th class="kn-th--note">{{ t('athlete.requestColNote') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="r in trainingRequests" :key="r.id">
                <td class="kn-td--id text-muted">{{ r.id }}</td>
                <td class="kn-td--created text-nowrap small">{{ formatReqDate(r.createdAt) }}</td>
                <td class="kn-td--athlete fw-semibold">{{ r.athleteFullName || '—' }}</td>
                <td class="kn-td--status">
                  <span class="kn-badge-status" :class="requestStatusBadgeClass(r.status)">
                    {{ t('trainingRequestStatus.' + r.status) }}
                  </span>
                </td>
                <td class="kn-td--note">{{ r.note || '—' }}</td>
              </tr>
              <tr v-if="trainingRequests.length === 0">
                <td colspan="5" class="text-center kn-text-muted py-4">{{ t('coach.trainingRequestsEmpty') }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </AppShell>
</template>
