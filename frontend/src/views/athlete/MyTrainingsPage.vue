<script setup>
import { Modal } from 'bootstrap'
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import AppShell from '../../layouts/AppShell.vue'
import BaseButton from '../../components/common/BaseButton.vue'
import { trainingRequestApi } from '../../services/api/trainingRequestApi'
import { useTrainingStore } from '../../stores/training'
import './my-trainings.css'

const { t } = useI18n()
const trainingStore = useTrainingStore()

const q = ref('')
const type = ref('')
const requestModalEl = ref(null)
const requestNote = ref('')
const submittingRequest = ref(false)
const banner = ref(null)
const bannerError = ref(null)
const myRequests = ref([])
const requestsLoading = ref(false)
const requestsError = ref(null)

onMounted(() => {
  trainingStore.fetchMyTrainings()
  loadMyRequests()
})

const filtered = computed(() => {
  const query = q.value.trim().toLowerCase()
  return trainingStore.trainings.filter((tr) => {
    if (type.value && tr.workoutType !== type.value) return false
    if (!query) return true
    return (
      String(tr.id).includes(query) ||
      String(tr.trainingDate).includes(query) ||
      String(tr.workoutType).toLowerCase().includes(query) ||
      String(tr.notes || '').toLowerCase().includes(query)
    )
  })
})

function openRequestModal() {
  banner.value = null
  bannerError.value = null
  const el = requestModalEl.value
  if (el) Modal.getOrCreateInstance(el).show()
}

function hideRequestModal() {
  const el = requestModalEl.value
  if (el) Modal.getInstance(el)?.hide()
}

async function loadMyRequests() {
  requestsLoading.value = true
  requestsError.value = null
  try {
    myRequests.value = await trainingRequestApi.listMine()
  } catch (e) {
    requestsError.value = e?.response?.data?.message || t('athlete.requestsLoadFailed')
    myRequests.value = []
  } finally {
    requestsLoading.value = false
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

function requestStatusClass(status) {
  const s = String(status || '').toUpperCase()
  if (s === 'APPROVED') return 'kn-badge-status--approved'
  if (s === 'REJECTED') return 'kn-badge-status--rejected'
  return 'kn-badge-status--pending'
}

async function submitTrainingRequest() {
  submittingRequest.value = true
  bannerError.value = null
  try {
    const note = requestNote.value.trim()
    await trainingRequestApi.submitMe(note ? { note } : {})
    banner.value = t('athlete.requestSent')
    requestNote.value = ''
    hideRequestModal()
    await loadMyRequests()
  } catch (e) {
    bannerError.value = e?.response?.data?.message || t('athlete.requestFailed')
  } finally {
    submittingRequest.value = false
  }
}
</script>

<template>
  <AppShell>
    <div class="kn-my-trainings">
      <div v-if="banner" class="alert alert-success py-2 mb-0">{{ banner }}</div>
      <div v-if="bannerError" class="alert alert-danger py-2 mb-0">{{ bannerError }}</div>

      <section class="app-card kn-my-trainings__card p-4 p-md-4">
        <div class="kn-my-trainings__toolbar">
          <div>
            <h1 class="kn-page-header__title mb-1">{{ t('athlete.myTrainingsTitle') }}</h1>
            <p class="kn-page-header__desc mb-0">{{ t('athlete.myTrainingsSubtitle') }}</p>
          </div>
          <div class="d-flex flex-wrap gap-2">
            <BaseButton class="rounded-pill px-3" variant="outline-secondary" @click="trainingStore.fetchMyTrainings()">
              {{ t('ui.refresh') }}
            </BaseButton>
            <BaseButton class="rounded-pill px-3" variant="primary" @click="openRequestModal">
              {{ t('athlete.requestTrainingAdd') }}
            </BaseButton>
          </div>
        </div>

        <div class="row g-2 kn-my-trainings__filters">
          <div class="col-12 col-md-7">
            <input v-model="q" class="form-control" :placeholder="t('athlete.trainingsSearchPlaceholder')" />
          </div>
          <div class="col-12 col-md-5">
            <select v-model="type" class="form-select">
              <option value="">{{ t('athlete.allWorkoutTypes') }}</option>
              <option value="OFP">OFP</option>
              <option value="TECHNICAL">TECHNICAL</option>
              <option value="TACTICAL">TACTICAL</option>
              <option value="SPARRING">SPARRING</option>
              <option value="RECOVERY">RECOVERY</option>
            </select>
          </div>
        </div>

        <div v-if="trainingStore.error" class="alert alert-danger py-2">{{ trainingStore.error }}</div>

        <div v-if="trainingStore.loading" class="d-flex align-items-center gap-2 app-muted py-4 justify-content-center">
          <div class="spinner-border spinner-border-sm" aria-hidden="true"></div>
          {{ t('ui.loading') }}
        </div>

        <div v-else class="kn-data-table-wrap">
          <div class="table-responsive kn-data-table-responsive">
            <table class="table kn-data-table kn-data-table--trainings border-0">
              <thead>
                <tr>
                  <th class="kn-th--id text-nowrap">{{ t('athlete.colId') }}</th>
                  <th class="kn-th--date text-nowrap">{{ t('athlete.colDate') }}</th>
                  <th class="kn-th--type text-nowrap">{{ t('athlete.colType') }}</th>
                  <th class="kn-th--num text-end text-nowrap">{{ t('athlete.colDuration') }}</th>
                  <th class="kn-th--num text-end text-nowrap">{{ t('athlete.colIntensity') }}</th>
                  <th class="kn-th--notes">{{ t('athlete.colNotes') }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="tr in filtered" :key="tr.id">
                  <td class="kn-td--id text-muted small">{{ tr.id }}</td>
                  <td class="kn-td--date fw-semibold">{{ tr.trainingDate }}</td>
                  <td class="kn-td--type">
                    <span class="kn-type-pill" :title="tr.workoutType">{{ tr.workoutType }}</span>
                  </td>
                  <td class="kn-td--num text-end">{{ tr.durationMinutes }} {{ t('athlete.minutesSuffix') }}</td>
                  <td class="kn-td--num text-end">{{ tr.intensity ?? '—' }}</td>
                  <td class="kn-td--notes">{{ tr.notes || '—' }}</td>
                </tr>
                <tr v-if="filtered.length === 0">
                  <td colspan="6" class="text-center app-muted py-5">{{ t('ui.empty') }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </section>

      <section class="app-card kn-my-trainings__card p-4 p-md-4">
        <div class="kn-my-trainings__section-head">
          <div>
            <h2 class="kn-my-trainings__section-title">{{ t('athlete.myRequestsTitle') }}</h2>
            <p class="app-muted small mb-0">{{ t('athlete.myRequestsHint') }}</p>
          </div>
          <BaseButton class="rounded-pill px-3" variant="outline-secondary" size="sm" @click="loadMyRequests">
            {{ t('ui.refresh') }}
          </BaseButton>
        </div>

        <div v-if="requestsError" class="alert alert-danger py-2">{{ requestsError }}</div>
        <div v-if="requestsLoading" class="d-flex align-items-center gap-2 app-muted small py-4 justify-content-center">
          <div class="spinner-border spinner-border-sm" aria-hidden="true"></div>
          {{ t('ui.loading') }}
        </div>
        <div v-else class="kn-data-table-wrap">
          <div class="table-responsive kn-data-table-responsive kn-data-table-responsive--short">
            <table class="table kn-data-table kn-data-table--requests border-0 mb-0">
              <thead>
                <tr>
                  <th class="kn-th--id">{{ t('athlete.colId') }}</th>
                  <th class="kn-th--created text-nowrap">{{ t('athlete.requestColCreated') }}</th>
                  <th class="kn-th--status">{{ t('athlete.requestColStatus') }}</th>
                  <th class="kn-th--note">{{ t('athlete.requestColNote') }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="r in myRequests" :key="r.id">
                  <td class="kn-td--id text-muted small">{{ r.id }}</td>
                  <td class="kn-td--created small">{{ formatReqDate(r.createdAt) }}</td>
                  <td class="kn-td--status">
                    <span class="kn-badge-status" :class="requestStatusClass(r.status)">{{
                      t('trainingRequestStatus.' + (r.status || 'PENDING'))
                    }}</span>
                  </td>
                  <td class="kn-td--note">{{ r.note || '—' }}</td>
                </tr>
                <tr v-if="myRequests.length === 0">
                  <td colspan="4" class="text-center app-muted py-4">{{ t('athlete.requestListEmpty') }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </section>
    </div>

    <div
      id="knTrainingRequestModal"
      ref="requestModalEl"
      class="modal fade"
      tabindex="-1"
      aria-labelledby="knTrainingRequestModalLabel"
      aria-hidden="true"
    >
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content kn-modal-training-req">
          <div class="modal-header border-bottom-0 pb-0">
            <h2 id="knTrainingRequestModalLabel" class="modal-title fs-6 fw-bold">
              {{ t('athlete.requestModalTitle') }}
            </h2>
            <button type="button" class="btn-close" data-bs-dismiss="modal" :aria-label="t('common.close')" />
          </div>
          <div class="modal-body pt-2">
            <p class="small app-muted mb-0">{{ t('athlete.requestModalHint') }}</p>
            <label class="form-label fw-semibold" for="knTrainingRequestNote">{{ t('athlete.requestNoteLabel') }}</label>
            <textarea
              id="knTrainingRequestNote"
              v-model="requestNote"
              class="form-control"
              rows="4"
              :placeholder="t('athlete.requestNotePlaceholder')"
            />
          </div>
          <div class="modal-footer border-top-0 pt-0 gap-2">
            <BaseButton class="rounded-pill px-3" variant="outline-secondary" data-bs-dismiss="modal">{{
              t('common.cancel')
            }}</BaseButton>
            <BaseButton class="rounded-pill px-3" variant="primary" :loading="submittingRequest" @click="submitTrainingRequest">
              {{ t('athlete.requestSubmit') }}
            </BaseButton>
          </div>
        </div>
      </div>
    </div>
  </AppShell>
</template>
