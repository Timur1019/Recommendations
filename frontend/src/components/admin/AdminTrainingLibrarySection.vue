<script setup>
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { trainingLibraryApi } from '../../services/api/trainingLibraryApi'

const { t } = useI18n()

const rows = ref([])
const loading = ref(false)
const error = ref('')

const title = ref('')
const description = ref('')
const file = ref(null)
const uploading = ref(false)
const uploadError = ref('')
const q = ref('')
const downloadingId = ref(null)

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

async function downloadFile(r) {
  if (downloadingId.value != null) return
  downloadingId.value = r.id
  try {
    const blob = await trainingLibraryApi.getFileBlob(r.id, true)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = r.originalFilename || `file-${r.id}`
    a.rel = 'noopener'
    document.body.appendChild(a)
    a.click()
    a.remove()
    URL.revokeObjectURL(url)
  } catch (e) {
    window.alert(e?.response?.data?.message || t('ui.loadFailed'))
  } finally {
    downloadingId.value = null
  }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    rows.value = await trainingLibraryApi.listAdmin()
  } catch (e) {
    error.value = e?.response?.data?.message || t('ui.loadFailed')
    rows.value = []
  } finally {
    loading.value = false
  }
}

function onFileChange(e) {
  const f = e.target.files?.[0]
  file.value = f || null
}

async function upload() {
  uploadError.value = ''
  if (!title.value.trim()) {
    uploadError.value = t('library.titleRequired')
    return
  }
  if (!file.value) {
    uploadError.value = t('library.fileRequired')
    return
  }
  uploading.value = true
  try {
    await trainingLibraryApi.uploadAdmin({
      title: title.value.trim(),
      description: description.value.trim() || null,
      file: file.value,
    })
    title.value = ''
    description.value = ''
    file.value = null
    const input = document.getElementById('knAdminLibraryFileInput')
    if (input) input.value = ''
    await load()
  } catch (e) {
    uploadError.value = e?.response?.data?.message || t('ui.saveFailed')
  } finally {
    uploading.value = false
  }
}

async function remove(r) {
  if (!window.confirm(t('library.deleteConfirm'))) return
  try {
    await trainingLibraryApi.deleteAdmin(r.id)
    await load()
  } catch (e) {
    window.alert(e?.response?.data?.message || t('ui.deleteFailed'))
  }
}

onMounted(() => {
  load()
})

defineExpose({ refresh: load })
</script>

<template>
  <div id="admin-training-library" class="app-card kn-admin-card p-4">
    <div class="kn-admin-toolbar mb-3">
      <div>
        <h2 class="kn-admin-card__title">{{ t('library.blockTitleInAdmin') }}</h2>
        <div class="app-muted small">{{ t('library.subtitleAdmin') }}</div>
      </div>
      <button
        type="button"
        class="btn btn-sm btn-outline-secondary rounded-pill px-3"
        :disabled="loading"
        @click="load"
      >
        {{ t('ui.refresh') }}
      </button>
    </div>

    <div class="kn-admin-upload-panel">
      <div class="kn-admin-upload-panel__title">{{ t('library.uploadBlockTitle') }}</div>
      <div v-if="uploadError" class="alert alert-danger py-2 small">{{ uploadError }}</div>
      <div class="row g-2 align-items-end">
        <div class="col-12 col-md-4">
          <label class="form-label small fw-semibold mb-1">{{ t('library.colTitle') }}</label>
          <input v-model="title" class="form-control" :placeholder="t('library.titlePlaceholder')" />
        </div>
        <div class="col-12 col-md-4">
          <label class="form-label small fw-semibold mb-1">{{ t('library.colDescription') }}</label>
          <input v-model="description" class="form-control" :placeholder="t('library.descPlaceholder')" />
        </div>
        <div class="col-12 col-md-4">
          <label class="form-label small fw-semibold mb-1">{{ t('library.colFile') }}</label>
          <input
            id="knAdminLibraryFileInput"
            class="form-control"
            type="file"
            accept=".pdf,.doc,.docx,.xls,.xlsx,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            @change="onFileChange"
          />
        </div>
        <div class="col-12">
          <button type="button" class="btn btn-primary rounded-pill px-4" :disabled="uploading" @click="upload">
            {{ uploading ? t('ui.loading') : t('library.upload') }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="error" class="alert alert-danger py-2">{{ error }}</div>
    <div v-else-if="loading" class="d-flex align-items-center gap-2 app-muted">
      <div class="spinner-border spinner-border-sm" aria-hidden="true" />
      {{ t('ui.loading') }}
    </div>

    <div v-else>
      <div class="row g-2 mb-3">
        <div class="col-12 col-md-6">
          <input v-model="q" class="form-control" :placeholder="t('library.searchPlaceholder')" />
        </div>
      </div>
      <div class="kn-data-table-wrap">
        <div class="kn-data-table-responsive kn-data-table-responsive--auto">
          <table class="table table-sm align-middle mb-0 kn-data-table kn-data-table--admin-library">
            <thead>
              <tr>
                <th class="kn-th--id">ID</th>
                <th class="kn-th--title">{{ t('library.colTitle') }}</th>
                <th class="kn-th--file">{{ t('library.colFile') }}</th>
                <th class="kn-th--type">{{ t('library.colType') }}</th>
                <th class="kn-th--actions text-end">{{ t('ui.actions') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="r in filtered" :key="r.id">
                <td class="kn-td--id text-muted">{{ r.id }}</td>
                <td class="kn-td--title fw-semibold">{{ r.title }}</td>
                <td class="kn-td--file text-muted">{{ r.originalFilename }}</td>
                <td class="kn-td--type">
                  <span class="kn-type-pill" :title="r.contentType">{{ r.contentType }}</span>
                </td>
                <td class="kn-td--actions text-end">
                  <div class="d-flex flex-wrap gap-1 justify-content-end">
                    <button
                      type="button"
                      class="btn btn-outline-primary btn-sm rounded-pill px-3"
                      :disabled="downloadingId === r.id"
                      @click="downloadFile(r)"
                    >
                      {{ downloadingId === r.id ? t('ui.loading') : t('library.download') }}
                    </button>
                    <button type="button" class="btn btn-outline-danger btn-sm rounded-pill px-3" @click="remove(r)">
                      {{ t('common.delete') }}
                    </button>
                  </div>
                </td>
              </tr>
              <tr v-if="filtered.length === 0">
                <td colspan="5" class="text-center app-muted py-4">{{ t('ui.empty') }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>
