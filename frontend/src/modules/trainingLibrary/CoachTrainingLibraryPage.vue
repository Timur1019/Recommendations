<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { Modal } from 'bootstrap'
import { useI18n } from 'vue-i18n'
import AppShell from '../../layouts/AppShell.vue'
import { trainingLibraryApi } from '../../services/api/trainingLibraryApi'
import { useCoachStore } from '../../stores/coach'

const { t } = useI18n()
const coachStore = useCoachStore()

const API_DAY_TO_I18N = {
  monday: 'MONDAY',
  tuesday: 'TUESDAY',
  wednesday: 'WEDNESDAY',
  thursday: 'THURSDAY',
  friday: 'FRIDAY',
  saturday: 'SATURDAY',
  sunday: 'SUNDAY',
}

const q = ref('')
const rows = ref([])
const loading = ref(false)
const error = ref('')

const readerEl = ref(null)
const active = ref(null)
const readerLoading = ref(false)
const readerError = ref('')
const pdfObjectUrl = ref('')
const textPreview = ref('')
const textTruncated = ref(false)

const aiModalEl = ref(null)
const aiFile = ref(null)
const aiLoading = ref(false)
const aiError = ref('')
const aiResult = ref(null)
const coachNotes = ref('')
const selectedAthleteId = ref(null)
const applyLoading = ref(false)
const applyDone = ref(false)

const filtered = computed(() => {
  const query = q.value.trim().toLowerCase()
  if (!query) return rows.value
  return rows.value.filter((x) => {
    return (
      (x.title || '').toLowerCase().includes(query) ||
      (x.originalFilename || '').toLowerCase().includes(query)
    )
  })
})

const orderedWeekDays = ['monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday', 'sunday']

function isPdf(r) {
  return String(r?.contentType || '')
    .toLowerCase()
    .split(';')[0]
    .trim() === 'application/pdf'
}

function dayLabel(key) {
  const code = API_DAY_TO_I18N[String(key || '').toLowerCase()]
  return code ? t(`achievements.days.${code}`) : key
}

function revokePdf() {
  if (pdfObjectUrl.value) {
    URL.revokeObjectURL(pdfObjectUrl.value)
    pdfObjectUrl.value = ''
  }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    rows.value = await trainingLibraryApi.listCoach()
  } catch (e) {
    error.value = e?.response?.data?.message || t('ui.loadFailed')
    rows.value = []
  } finally {
    loading.value = false
  }
}

async function downloadFile(r) {
  try {
    const blob = await trainingLibraryApi.getFileBlob(r.id, true)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = r.originalFilename || 'document'
    a.rel = 'noopener'
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    error.value = e?.response?.data?.message || t('library.downloadError')
  }
}

async function openReader(r) {
  active.value = r
  readerError.value = ''
  textPreview.value = ''
  textTruncated.value = false
  revokePdf()
  const el = readerEl.value
  if (!el) return
  readerLoading.value = true
  Modal.getOrCreateInstance(el).show()
  try {
    if (isPdf(r)) {
      const blob = await trainingLibraryApi.getFileBlob(r.id, false)
      pdfObjectUrl.value = URL.createObjectURL(blob)
    } else {
      const prev = await trainingLibraryApi.getPreviewText(r.id)
      textPreview.value = prev.text || ''
      textTruncated.value = Boolean(prev.truncated)
    }
  } catch (e) {
    readerError.value = e?.response?.data?.message || t('library.readError')
  } finally {
    readerLoading.value = false
  }
}

function openAiModal(r) {
  aiFile.value = r
  aiResult.value = null
  aiError.value = ''
  applyDone.value = false
  coachNotes.value = ''
  const first = coachStore.athletes?.[0]
  selectedAthleteId.value = first?.id ?? null
  const m = aiModalEl.value
  if (m) Modal.getOrCreateInstance(m).show()
}

async function runAnalyze() {
  if (!aiFile.value) return
  aiLoading.value = true
  aiError.value = ''
  try {
    const payload = {}
    if (selectedAthleteId.value) payload.athleteId = selectedAthleteId.value
    if (coachNotes.value.trim()) payload.coachNotes = coachNotes.value.trim()
    aiResult.value = await trainingLibraryApi.analyzeHandbook(aiFile.value.id, payload)
  } catch (e) {
    aiError.value = e?.response?.data?.message || t('ui.loadFailed')
  } finally {
    aiLoading.value = false
  }
}

async function runApply() {
  if (!aiFile.value || !aiResult.value) return
  if (!selectedAthleteId.value) {
    aiError.value = t('library.applyNeedAthlete')
    return
  }
  applyLoading.value = true
  aiError.value = ''
  try {
    await trainingLibraryApi.applyHandbook(aiFile.value.id, {
      athleteId: selectedAthleteId.value,
      summary: aiResult.value.summary,
      weekPlan: aiResult.value.weekPlan,
    })
    applyDone.value = true
  } catch (e) {
    aiError.value = e?.response?.data?.message || t('ui.loadFailed')
  } finally {
    applyLoading.value = false
  }
}

function bindReaderCleanup() {
  const el = readerEl.value
  if (!el) return
  el.addEventListener('hidden.bs.modal', () => {
    revokePdf()
    textPreview.value = ''
    readerError.value = ''
    active.value = null
  })
}

onMounted(async () => {
  await load()
  await coachStore.fetchMe()
  bindReaderCleanup()
})

onBeforeUnmount(() => {
  revokePdf()
})

function athleteLabel(a) {
  const name = (a?.user?.fullName || '').trim()
  return name || `id ${a?.id}`
}
</script>

<template>
  <AppShell>
    <header class="kn-page-header d-flex flex-wrap justify-content-between align-items-start gap-3 mb-3">
      <div>
        <h1 class="kn-page-header__title mb-1">{{ t('library.title') }}</h1>
        <p class="kn-page-header__desc mb-0">{{ t('library.subtitleCoach') }}</p>
      </div>
      <button
        type="button"
        class="btn btn-outline-primary btn-sm rounded-pill px-3"
        :disabled="loading"
        @click="load"
      >
        <span class="material-symbols-outlined kn-library-refresh-icon" aria-hidden="true">refresh</span>
        {{ t('ui.refresh') }}
      </button>
    </header>

    <div class="row g-2 mb-3">
      <div class="col-12 col-md-6">
        <input v-model="q" class="form-control" :placeholder="t('library.searchPlaceholder')" />
      </div>
    </div>

    <div v-if="error" class="alert alert-danger py-2">{{ error }}</div>
    <div v-else-if="loading" class="d-flex align-items-center gap-2 kn-text-muted">
      <div class="spinner-border spinner-border-sm" aria-hidden="true" />
      {{ t('ui.loading') }}
    </div>

    <div v-else class="row g-3">
      <div v-for="r in filtered" :key="r.id" class="col-12 col-lg-6">
        <div class="kn-coach-stat-card kn-library-card p-4 h-100 d-flex flex-column gap-3">
          <div class="d-flex align-items-start justify-content-between gap-3">
            <div class="min-w-0">
              <div class="fw-bold text-truncate font-headline">{{ r.title }}</div>
              <div class="small kn-text-muted text-truncate">{{ r.originalFilename }}</div>
            </div>
            <span class="kn-library-file-badge text-uppercase">{{ isPdf(r) ? 'PDF' : 'FILE' }}</span>
          </div>

          <div v-if="r.description" class="small kn-text-muted">{{ r.description }}</div>

          <div class="kn-library-actions mt-auto">
            <button type="button" class="btn btn-primary btn-sm rounded-pill px-3" @click="openReader(r)">
              <span class="material-symbols-outlined kn-library-action-icon" aria-hidden="true">menu_book</span>
              {{ t('library.read') }}
            </button>
            <button type="button" class="btn btn-outline-primary btn-sm rounded-pill px-3" @click="downloadFile(r)">
              <span class="material-symbols-outlined kn-library-action-icon" aria-hidden="true">download</span>
              {{ t('library.download') }}
            </button>
            <button type="button" class="btn btn-outline-secondary btn-sm rounded-pill px-3" @click="openAiModal(r)">
              <span class="material-symbols-outlined kn-library-action-icon" aria-hidden="true">smart_toy</span>
              {{ t('library.aiPlan') }}
            </button>
          </div>
        </div>
      </div>

      <div v-if="filtered.length === 0" class="col-12">
        <div class="kn-coach-stat-card kn-empty-panel p-5 text-center kn-text-muted">{{ t('ui.empty') }}</div>
      </div>
    </div>

    <div
      id="knLibraryReader"
      ref="readerEl"
      class="modal fade"
      tabindex="-1"
      aria-labelledby="knLibraryReaderLabel"
      aria-hidden="true"
    >
      <div class="modal-dialog modal-dialog-centered modal-xl">
        <div class="modal-content kn-modal-sheet">
          <div class="modal-header border-bottom-0 pb-0">
            <div class="min-w-0">
              <h2 id="knLibraryReaderLabel" class="modal-title fs-6 fw-bold text-truncate">
                {{ active?.title || t('library.readerTitle') }}
              </h2>
              <div class="small kn-text-muted text-truncate">{{ active?.originalFilename || '' }}</div>
            </div>
            <button type="button" class="btn-close" data-bs-dismiss="modal" :aria-label="t('common.close')" />
          </div>
          <div class="modal-body pt-2">
            <div v-if="readerLoading" class="d-flex align-items-center gap-2 kn-text-muted py-5 justify-content-center">
              <div class="spinner-border spinner-border-sm" aria-hidden="true" />
              {{ t('ui.loading') }}
            </div>
            <div v-else-if="readerError" class="alert alert-warning mb-0">{{ readerError }}</div>
            <div v-else-if="active && isPdf(active) && pdfObjectUrl" class="ratio ratio-16x9">
              <iframe :src="pdfObjectUrl" title="Reader" />
            </div>
            <div v-else-if="textPreview" class="kn-library-text-preview p-3 small">
              <pre class="kn-library-text-preview__pre">{{ textPreview }}</pre>
              <div v-if="textTruncated" class="text-muted mt-2 mb-0 small">{{ t('library.textTruncated') }}</div>
            </div>
            <div v-else class="alert alert-warning mb-0">{{ t('library.previewNotSupportedHint') }}</div>
          </div>
          <div class="modal-footer border-top-0 pt-0 gap-2">
            <button
              v-if="active"
              type="button"
              class="btn btn-outline-primary rounded-pill px-3"
              :disabled="readerLoading"
              @click="downloadFile(active)"
            >
              <span class="material-symbols-outlined kn-library-action-icon" aria-hidden="true">download</span>
              {{ t('library.download') }}
            </button>
            <button type="button" class="btn btn-outline-secondary rounded-pill px-3" data-bs-dismiss="modal">
              {{ t('common.close') }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <div
      id="knLibraryAi"
      ref="aiModalEl"
      class="modal fade"
      tabindex="-1"
      aria-labelledby="knLibraryAiLabel"
      aria-hidden="true"
    >
      <div class="modal-dialog modal-dialog-centered modal-lg modal-dialog-scrollable">
        <div class="modal-content kn-modal-sheet">
          <div class="modal-header border-bottom-0 pb-0">
            <div class="min-w-0">
              <h2 id="knLibraryAiLabel" class="modal-title fs-6 fw-bold text-truncate">{{ t('library.aiModalTitle') }}</h2>
              <div class="small kn-text-muted text-truncate">{{ aiFile?.title }} — {{ aiFile?.originalFilename }}</div>
            </div>
            <button type="button" class="btn-close" data-bs-dismiss="modal" :aria-label="t('common.close')" />
          </div>
          <div class="modal-body pt-2">
            <div class="mb-3">
              <label class="form-label small mb-1">{{ t('library.selectAthlete') }}</label>
              <select v-model.number="selectedAthleteId" class="form-select form-select-sm">
                <option v-if="!coachStore.athletes?.length" disabled :value="null">{{ t('library.noAthletes') }}</option>
                <option v-for="a in coachStore.athletes" :key="a.id" :value="a.id">{{ athleteLabel(a) }}</option>
              </select>
            </div>
            <div class="mb-3">
              <label class="form-label small mb-1">{{ t('library.coachNotesPlaceholder') }}</label>
              <textarea v-model="coachNotes" class="form-control form-control-sm" rows="2" />
            </div>
            <div class="d-flex flex-wrap gap-2 mb-3">
              <button
                type="button"
                class="btn btn-primary btn-sm rounded-pill px-3"
                :disabled="aiLoading"
                @click="runAnalyze"
              >
                <span v-if="aiLoading" class="spinner-border spinner-border-sm" aria-hidden="true" />
                {{ aiLoading ? t('library.aiAnalyzing') : t('library.aiRun') }}
              </button>
            </div>
            <div v-if="aiError" class="alert alert-danger py-2 small">{{ aiError }}</div>
            <div v-if="applyDone" class="alert alert-success py-2 small">{{ t('library.applySuccess') }}</div>
            <template v-if="aiResult">
              <h3 class="fs-6 fw-bold">{{ t('library.aiResultTitle') }}</h3>
              <p class="small">{{ aiResult.summary }}</p>
              <h3 class="fs-6 fw-bold mt-3">{{ t('library.aiWeekPlan') }}</h3>
              <ul class="list-unstyled small mb-0">
                <li v-for="d in orderedWeekDays" :key="d" class="mb-2">
                  <span class="fw-semibold">{{ dayLabel(d) }}:</span>
                  {{ aiResult.weekPlan?.[d] || '—' }}
                </li>
              </ul>
              <button
                type="button"
                class="btn btn-success btn-sm rounded-pill px-3 mt-3"
                :disabled="applyLoading || !selectedAthleteId"
                @click="runApply"
              >
                <span v-if="applyLoading" class="spinner-border spinner-border-sm" aria-hidden="true" />
                {{ t('library.aiAssignCourse') }}
              </button>
            </template>
          </div>
        </div>
      </div>
    </div>
  </AppShell>
</template>

<style src="./training-library.css"></style>
