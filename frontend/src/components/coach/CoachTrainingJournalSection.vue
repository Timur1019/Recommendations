<script setup>
import { Modal } from 'bootstrap'
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import BaseButton from '../common/BaseButton.vue'
import { trainingApi } from '../../services/api/trainingApi'

const props = defineProps({
  athletes: { type: Array, default: () => [] },
})

const { t } = useI18n()

const rows = ref([])
const loading = ref(false)
const error = ref(null)
const saving = ref(false)
const formError = ref(null)
const modalEl = ref(null)

const form = reactive({
  athleteId: '',
  trainingDate: new Date().toISOString().slice(0, 10),
  workoutType: 'OFP',
  durationMinutes: 60,
  intensity: '',
  notes: '',
})

const workoutTypes = ['OFP', 'TECHNICAL', 'TACTICAL', 'SPARRING', 'RECOVERY']

const athleteOptions = computed(() =>
  props.athletes.map((a) => ({ id: a.id, name: a.user?.fullName || '—' })),
)

function athleteName(id) {
  const a = props.athletes.find((x) => x.id === id)
  return a?.user?.fullName || '—'
}

async function load() {
  loading.value = true
  error.value = null
  try {
    rows.value = await trainingApi.myTrainings()
  } catch (e) {
    error.value = e?.response?.data?.message || t('coach.trainingsLoadFailed')
    rows.value = []
  } finally {
    loading.value = false
  }
}

function openModal() {
  formError.value = null
  if (athleteOptions.value.length && !form.athleteId) {
    form.athleteId = String(athleteOptions.value[0].id)
  }
  showModalEl()
}

function openModalForAthlete(athleteId) {
  formError.value = null
  if (athleteId != null && props.athletes.some((a) => a.id === athleteId)) {
    form.athleteId = String(athleteId)
  } else if (athleteOptions.value.length && !form.athleteId) {
    form.athleteId = String(athleteOptions.value[0].id)
  }
  showModalEl()
}

function showModalEl() {
  const el = modalEl.value
  if (el) Modal.getOrCreateInstance(el).show()
}

async function scrollToJournalAndOpen(athleteId) {
  const root = document.getElementById('coach-training-journal')
  root?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  await nextTick()
  openModalForAthlete(athleteId)
}

function hideModal() {
  const el = modalEl.value
  if (el) Modal.getInstance(el)?.hide()
}

async function submit() {
  formError.value = null
  if (!form.athleteId) {
    formError.value = t('coach.selectAthleteRequired')
    return
  }
  saving.value = true
  try {
    const intensity =
      form.intensity === '' || form.intensity == null ? null : Number(form.intensity)
    await trainingApi.create({
      athleteId: Number(form.athleteId),
      trainingDate: form.trainingDate,
      workoutType: form.workoutType,
      durationMinutes: Number(form.durationMinutes),
      intensity: intensity != null && Number.isFinite(intensity) ? intensity : null,
      technicalActions: null,
      notes: form.notes.trim() || null,
    })
    hideModal()
    await load()
  } catch (e) {
    formError.value = e?.response?.data?.message || t('coach.trainingSaveFailed')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  load()
})

defineExpose({
  openModalForAthlete,
  scrollToJournalAndOpen,
  focusJournal: () => {
    const root = document.getElementById('coach-training-journal')
    root?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  },
})
</script>

<template>
  <div id="coach-training-journal" class="kn-coach-stat-card p-4 mb-4">
    <div class="kn-coach-toolbar">
      <div>
        <h2 class="kn-coach-card-title">{{ t('coach.trainingsJournalTitle') }}</h2>
        <div class="small kn-text-muted">{{ t('coach.trainingsJournalHint') }}</div>
      </div>
      <div class="d-flex flex-wrap gap-2">
        <button
          type="button"
          class="btn btn-outline-secondary btn-sm rounded-pill px-3"
          :disabled="loading"
          @click="load"
        >
          {{ t('ui.refresh') }}
        </button>
        <BaseButton
          class="rounded-pill px-3"
          variant="primary"
          size="sm"
          :disabled="!athleteOptions.length"
          @click="openModal"
        >
          {{ t('coach.addTrainingForAthlete') }}
        </BaseButton>
      </div>
    </div>

    <div v-if="!athleteOptions.length" class="alert alert-warning py-2 small mb-0">
      {{ t('coach.noAthletesForTraining') }}
    </div>
    <div v-else-if="error" class="alert alert-danger py-2">{{ error }}</div>
    <div v-else-if="loading" class="d-flex align-items-center gap-2 kn-text-muted small py-2">
      <div class="spinner-border spinner-border-sm" aria-hidden="true" />
      {{ t('ui.loading') }}
    </div>
    <div v-else class="kn-data-table-wrap">
      <div class="kn-data-table-responsive kn-data-table-responsive--auto">
        <table class="table table-sm align-middle mb-0 kn-data-table kn-data-table--coach-journal">
          <thead>
            <tr>
              <th class="kn-th--date">{{ t('athlete.colDate') }}</th>
              <th class="kn-th--athlete">{{ t('coach.colAthlete') }}</th>
              <th class="kn-th--type">{{ t('athlete.colType') }}</th>
              <th class="kn-th--duration text-end">{{ t('athlete.colDuration') }}</th>
              <th class="kn-th--intensity text-end">{{ t('athlete.colIntensity') }}</th>
              <th class="kn-th--notes">{{ t('athlete.colNotes') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="tr in rows" :key="tr.id">
              <td class="kn-td--date text-nowrap small">{{ tr.trainingDate }}</td>
              <td class="kn-td--athlete fw-semibold">{{ athleteName(tr.athleteId) }}</td>
              <td class="kn-td--type">
                <span class="kn-type-pill" :title="tr.workoutType">{{ tr.workoutType }}</span>
              </td>
              <td class="kn-td--duration text-end">{{ tr.durationMinutes }} {{ t('athlete.minutesSuffix') }}</td>
              <td class="kn-td--intensity text-end">{{ tr.intensity ?? '—' }}</td>
              <td class="kn-td--notes">{{ tr.notes || '—' }}</td>
            </tr>
            <tr v-if="rows.length === 0">
              <td colspan="6" class="text-center kn-text-muted py-4">{{ t('ui.empty') }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div
      id="knCoachTrainingModal"
      ref="modalEl"
      class="modal fade"
      tabindex="-1"
      aria-labelledby="knCoachTrainingModalLabel"
      aria-hidden="true"
    >
      <div class="modal-dialog modal-dialog-centered modal-lg">
        <div class="modal-content kn-modal-sheet">
          <div class="modal-header border-bottom-0 pb-0">
            <h2 id="knCoachTrainingModalLabel" class="modal-title fs-6 fw-bold">
              {{ t('coach.addTrainingModalTitle') }}
            </h2>
            <button type="button" class="btn-close" data-bs-dismiss="modal" :aria-label="t('common.close')" />
          </div>
          <div class="modal-body pt-2">
            <div v-if="formError" class="alert alert-danger py-2 small">{{ formError }}</div>
            <div class="row g-2">
              <div class="col-12 col-md-6">
                <label class="form-label fw-semibold">{{ t('coach.colAthlete') }}</label>
                <select v-model="form.athleteId" class="form-select" required>
                  <option disabled value="">{{ t('coach.selectAthletePlaceholder') }}</option>
                  <option v-for="o in athleteOptions" :key="o.id" :value="String(o.id)">{{ o.name }}</option>
                </select>
              </div>
              <div class="col-12 col-md-6">
                <label class="form-label fw-semibold">{{ t('athlete.colDate') }}</label>
                <input v-model="form.trainingDate" class="form-control" type="date" />
              </div>
              <div class="col-12 col-md-6">
                <label class="form-label fw-semibold">{{ t('athlete.colType') }}</label>
                <select v-model="form.workoutType" class="form-select">
                  <option v-for="w in workoutTypes" :key="w" :value="w">{{ w }}</option>
                </select>
              </div>
              <div class="col-6 col-md-3">
                <label class="form-label fw-semibold">{{ t('athlete.colDuration') }}</label>
                <input v-model.number="form.durationMinutes" class="form-control" type="number" min="1" max="600" />
              </div>
              <div class="col-6 col-md-3">
                <label class="form-label fw-semibold">{{ t('athlete.colIntensity') }} (1–10)</label>
                <input v-model="form.intensity" class="form-control" type="number" min="1" max="10" placeholder="—" />
              </div>
              <div class="col-12">
                <label class="form-label fw-semibold">{{ t('athlete.colNotes') }}</label>
                <textarea v-model="form.notes" class="form-control" rows="2" />
              </div>
            </div>
          </div>
          <div class="modal-footer border-top-0 pt-0 gap-2">
            <button type="button" class="btn btn-outline-secondary rounded-pill px-3" data-bs-dismiss="modal">
              {{ t('common.cancel') }}
            </button>
            <BaseButton class="rounded-pill px-3" variant="primary" :loading="saving" @click="submit">{{
              t('common.save')
            }}</BaseButton>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
