<script setup>
import { Modal } from 'bootstrap'
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import BaseButton from '../common/BaseButton.vue'
import { trainingApi } from '../../services/api/trainingApi'

const props = defineProps({
  athletes: { type: Array, default: () => [] },
})

const emit = defineEmits(['saved'])

const { t } = useI18n()

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

function show() {
  formError.value = null
  if (athleteOptions.value.length && !form.athleteId) {
    form.athleteId = String(athleteOptions.value[0].id)
  }
  const el = modalEl.value
  if (el) Modal.getOrCreateInstance(el).show()
}

function showForAthlete(athleteId) {
  formError.value = null
  if (athleteId != null && props.athletes.some((a) => a.id === athleteId)) {
    form.athleteId = String(athleteId)
  } else if (athleteOptions.value.length && !form.athleteId) {
    form.athleteId = String(athleteOptions.value[0].id)
  }
  const el = modalEl.value
  if (el) Modal.getOrCreateInstance(el).show()
}

function hide() {
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
    const intensity = form.intensity === '' || form.intensity == null ? null : Number(form.intensity)
    await trainingApi.create({
      athleteId: Number(form.athleteId),
      trainingDate: form.trainingDate,
      workoutType: form.workoutType,
      durationMinutes: Number(form.durationMinutes),
      intensity: intensity != null && Number.isFinite(intensity) ? intensity : null,
      technicalActions: null,
      notes: form.notes.trim() || null,
    })
    hide()
    emit('saved')
  } catch (e) {
    formError.value = e?.response?.data?.message || t('coach.trainingSaveFailed')
  } finally {
    saving.value = false
  }
}

defineExpose({ show, showForAthlete })
</script>

<template>
  <div
    id="knCoachAddTrainingModal"
    ref="modalEl"
    class="modal fade"
    tabindex="-1"
    aria-labelledby="knCoachAddTrainingModalLabel"
    aria-hidden="true"
  >
    <div class="modal-dialog modal-dialog-centered modal-lg">
      <div class="modal-content kn-modal-sheet">
        <div class="modal-header border-bottom-0 pb-0">
          <h2 id="knCoachAddTrainingModalLabel" class="modal-title fs-6 fw-bold">
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
</template>

