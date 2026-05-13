<script setup>
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import BaseButton from '../../components/common/BaseButton.vue'
import { adminAiApi } from '../../services/api/adminAiApi'

const { t } = useI18n()

const integration = ref(null)
const prompts = ref([])
const loading = ref(true)
const error = ref('')
const savingKey = ref(null)
const editDraft = ref({})

async function loadAll() {
  loading.value = true
  error.value = ''
  try {
    const [integ, plist] = await Promise.all([adminAiApi.integration(), adminAiApi.listPrompts()])
    integration.value = integ
    prompts.value = plist
    const draft = {}
    for (const p of plist) {
      draft[p.settingKey] = { label: p.label, body: p.body }
    }
    editDraft.value = draft
  } catch (e) {
    error.value = e?.response?.data?.message || t('ui.loadFailed')
    integration.value = null
    prompts.value = []
  } finally {
    loading.value = false
  }
}

async function savePrompt(key) {
  const d = editDraft.value[key]
  if (!d) return
  savingKey.value = key
  error.value = ''
  try {
    await adminAiApi.updatePrompt(key, { label: d.label, body: d.body })
    await loadAll()
  } catch (e) {
    error.value = e?.response?.data?.message || t('ui.saveFailed')
  } finally {
    savingKey.value = null
  }
}

onMounted(() => {
  loadAll()
})
</script>

<template>
  <div class="row g-3 kn-admin-stack">
    <div class="col-12">
      <div class="app-card kn-admin-card p-4">
        <h2 class="kn-admin-card__title mb-3">{{ t('admin.aiIntegrationTitle') }}</h2>
        <div v-if="loading" class="app-muted small">{{ t('ui.loading') }}</div>
        <template v-else-if="integration">
          <div class="d-flex flex-wrap gap-2 align-items-center mb-2">
            <span
              class="badge rounded-pill px-3 kn-admin-status-badge--ok"
              :class="integration.llmConfigured ? 'text-bg-success' : 'text-bg-warning'"
            >
              {{ integration.llmConfigured ? t('admin.llmOn') : t('admin.llmOff') }}
            </span>
            <span class="small text-muted"> {{ integration.baseUrl }} · {{ integration.model }} </span>
          </div>
          <p class="small text-muted mb-0">{{ t('admin.aiIntegrationHint') }}</p>
        </template>
      </div>
    </div>

    <div class="col-12">
      <div class="app-card kn-admin-card p-4">
        <div class="kn-admin-toolbar">
          <h2 class="kn-admin-card__title mb-0">{{ t('admin.aiPromptsTitle') }}</h2>
          <button
            type="button"
            class="btn btn-sm btn-outline-secondary rounded-pill px-3"
            :disabled="loading"
            @click="loadAll"
          >
            {{ t('ui.refresh') }}
          </button>
        </div>

        <div v-if="error" class="alert alert-danger py-2 mt-3">{{ error }}</div>

        <div v-if="loading" class="d-flex align-items-center gap-2 app-muted py-3">
          <div class="spinner-border spinner-border-sm" aria-hidden="true" />
          {{ t('ui.loading') }}
        </div>

        <div v-else-if="prompts.length === 0" class="text-muted small mt-2">{{ t('ui.empty') }}</div>

        <div v-else class="d-flex flex-column gap-3 mt-3">
          <div v-for="p in prompts" :key="p.settingKey" class="kn-admin-prompt-panel">
            <template v-if="editDraft[p.settingKey]">
              <div class="kn-admin-prompt-panel__key">{{ p.settingKey }}</div>
              <div class="mb-2">
                <label class="form-label small fw-semibold mb-1">{{ t('library.colTitle') }}</label>
                <input v-model="editDraft[p.settingKey].label" type="text" class="form-control form-control-sm" />
              </div>
              <div class="mb-3">
                <label class="form-label small fw-semibold mb-1">{{ t('admin.aiPromptBody') }}</label>
                <textarea v-model="editDraft[p.settingKey].body" class="form-control font-monospace small" rows="12" />
              </div>
              <div class="d-flex justify-content-between align-items-center flex-wrap gap-2">
                <span class="small text-muted">
                  {{ t('admin.updatedAt') }}: {{ p.updatedAt ? new Date(p.updatedAt).toLocaleString() : '—' }}
                </span>
                <BaseButton
                  class="rounded-pill px-3"
                  variant="primary"
                  size="sm"
                  :loading="savingKey === p.settingKey"
                  @click="savePrompt(p.settingKey)"
                >
                  {{ t('common.save') }}
                </BaseButton>
              </div>
            </template>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
