<script setup>
import { Modal } from 'bootstrap'
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { achievementApi } from '../../services/api/achievementApi'

const props = defineProps({
  athletes: { type: Array, default: () => [] },
})

const { t } = useI18n()

const modalEl = ref(null)
const loading = ref(false)
const error = ref('')
const insight = ref(null)
const focusAthleteId = ref(null)
const autoRun = ref(false)

const focusAthleteName = computed(() => {
  if (focusAthleteId.value == null) return ''
  const a = props.athletes.find((x) => x.id === focusAthleteId.value)
  return a?.user?.fullName || ''
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    insight.value = await achievementApi.cohortInsight(null)
  } catch (e) {
    error.value = e?.response?.data?.message || t('journal.cohortFailed')
    insight.value = null
  } finally {
    loading.value = false
  }
}

async function show({ athleteId, runCohort } = {}) {
  focusAthleteId.value = athleteId ?? null
  autoRun.value = Boolean(runCohort)

  const el = modalEl.value
  if (el) Modal.getOrCreateInstance(el).show()

  if (autoRun.value) {
    await load()
  }
}

defineExpose({ show })
</script>

<template>
  <div
    id="knCoachCohortModal"
    ref="modalEl"
    class="modal fade"
    tabindex="-1"
    aria-labelledby="knCoachCohortModalLabel"
    aria-hidden="true"
  >
    <div class="modal-dialog modal-dialog-centered modal-xl">
      <div class="modal-content kn-modal-sheet">
        <div class="modal-header border-bottom-0 pb-0">
          <div class="min-w-0">
            <h2 id="knCoachCohortModalLabel" class="modal-title fs-6 fw-bold">
              {{ t('journal.cohortTitle') }}
            </h2>
            <div class="small kn-text-muted">{{ t('journal.cohortSubtitle') }}</div>
          </div>
          <button type="button" class="btn-close" data-bs-dismiss="modal" :aria-label="t('common.close')" />
        </div>

        <div class="modal-body pt-2">
          <div class="d-flex flex-wrap justify-content-between align-items-center gap-2">
            <div v-if="focusAthleteName" class="alert alert-info py-2 small mb-0 flex-grow-1">
              {{ t('coach.cohortFocusHint', { name: focusAthleteName }) }}
            </div>
            <button type="button" class="btn btn-primary rounded-pill px-4" :disabled="loading" @click="load">
              {{ loading ? t('ui.loading') : t('journal.runCohortInsight') }}
            </button>
          </div>

          <div v-if="error" class="alert alert-danger mt-3 mb-0 py-2">{{ error }}</div>
          <div v-else-if="loading" class="d-flex align-items-center gap-2 kn-text-muted small py-3">
            <div class="spinner-border spinner-border-sm" aria-hidden="true" />
            {{ t('ui.loading') }}
          </div>

          <div v-if="insight" class="mt-3 small">
            <p class="mb-3">{{ insight.analysisText }}</p>
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
                    <tr v-for="row in insight.chartByWeekday" :key="row.dayOfWeek">
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

        <div class="modal-footer border-top-0 pt-0">
          <button type="button" class="btn btn-outline-secondary rounded-pill px-3" data-bs-dismiss="modal">
            {{ t('common.close') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

