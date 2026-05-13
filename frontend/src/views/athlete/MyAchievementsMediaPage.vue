<script setup>
import { onMounted, ref } from 'vue'
import AppShell from '../../layouts/AppShell.vue'
import BaseButton from '../../components/common/BaseButton.vue'
import AchievementMediaTile from '../../components/athlete/AchievementMediaTile.vue'
import { useI18n } from 'vue-i18n'
import { achievementApi } from '../../services/api/achievementApi'
import './my-achievements-media.css'

const { t } = useI18n()

const loading = ref(true)
const error = ref('')
const list = ref([])
const uploading = ref({})
const fileInputRefs = ref({})

function setFileInput(id, el) {
  if (el) fileInputRefs.value[id] = el
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    list.value = await achievementApi.myAchievements()
  } catch (e) {
    error.value = e?.response?.data?.message || t('media.loadFailed')
    list.value = []
  } finally {
    loading.value = false
  }
}

async function onPickFile(achievementId, event) {
  const input = event.target
  const file = input.files?.[0]
  if (!file) return
  uploading.value = { ...uploading.value, [achievementId]: true }
  error.value = ''
  try {
    await achievementApi.uploadAchievementMedia(achievementId, file)
    await load()
  } catch (e) {
    error.value = e?.response?.data?.message || t('media.uploadFailed')
  } finally {
    uploading.value = { ...uploading.value, [achievementId]: false }
    input.value = ''
  }
}

function triggerFile(achievementId) {
  fileInputRefs.value[achievementId]?.click()
}

async function onDelete(achievementId, mediaId) {
  if (!window.confirm(t('media.confirmDelete'))) return
  try {
    await achievementApi.deleteAchievementMedia(achievementId, mediaId)
    await load()
  } catch (e) {
    error.value = e?.response?.data?.message || t('media.deleteFailed')
  }
}

function formatDate(d) {
  if (!d) return '—'
  try {
    return new Date(d).toLocaleDateString()
  } catch {
    return String(d)
  }
}

onMounted(load)
</script>

<template>
  <AppShell>
    <div class="am-page container py-4">
      <div class="d-flex justify-content-between align-items-start flex-wrap gap-3 mb-4">
        <div>
          <h1 class="h4 fw-bold font-headline mb-1">{{ t('media.title') }}</h1>
          <p class="text-muted small mb-0">{{ t('media.subtitle') }}</p>
        </div>
        <BaseButton variant="outline-secondary" :loading="loading" @click="load">{{ t('ui.refresh') }}</BaseButton>
      </div>

      <div v-if="error" class="alert alert-danger py-2">{{ error }}</div>

      <div v-if="loading" class="am-page__loading text-muted">{{ t('ui.loading') }}</div>

      <div v-else-if="!list.length" class="am-page__empty app-card p-4 text-center text-muted">
        {{ t('media.emptyAchievements') }}
      </div>

      <div v-else class="row g-3">
        <div v-for="a in list" :key="a.id" class="col-12">
          <article class="am-card app-card p-4">
            <div class="d-flex justify-content-between align-items-start flex-wrap gap-2 mb-3">
              <div>
                <h2 class="h6 fw-bold mb-1">{{ a.competitionName }}</h2>
                <div class="small text-muted">
                  {{ formatDate(a.competitionDate) }} · {{ a.competitionLevel }} · {{ a.medalType }}
                </div>
              </div>
              <div>
                <input
                  :ref="(el) => setFileInput(a.id, el)"
                  type="file"
                  class="d-none"
                  accept="image/jpeg,image/png,image/webp,image/gif,video/mp4,video/webm,video/quicktime"
                  @change="onPickFile(a.id, $event)"
                />
                <BaseButton
                  variant="primary"
                  size="sm"
                  :loading="uploading[a.id]"
                  @click="triggerFile(a.id)"
                >
                  {{ t('media.addFile') }}
                </BaseButton>
              </div>
            </div>

            <div v-if="!(a.media && a.media.length)" class="small text-muted mb-0">
              {{ t('media.noMediaYet') }}
            </div>
            <div v-else class="am-grid">
              <AchievementMediaTile
                v-for="m in a.media"
                :key="m.id"
                :achievement-id="a.id"
                :media="m"
                @delete="onDelete(a.id, m.id)"
              />
            </div>
          </article>
        </div>
      </div>
    </div>
  </AppShell>
</template>
