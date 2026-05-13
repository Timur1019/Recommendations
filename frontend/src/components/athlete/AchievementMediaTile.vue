<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { achievementApi } from '../../services/api/achievementApi'

const props = defineProps({
  achievementId: { type: Number, required: true },
  media: { type: Object, required: true },
})

const emit = defineEmits(['delete'])

const url = ref('')
const error = ref('')
const loading = ref(true)

async function load() {
  loading.value = true
  error.value = ''
  if (url.value) {
    URL.revokeObjectURL(url.value)
    url.value = ''
  }
  try {
    url.value = await achievementApi.getMediaBlobUrl(props.achievementId, props.media.id)
  } catch (e) {
    error.value = e?.response?.status === 413 ? 'Файл слишком большой' : 'Не удалось загрузить'
  } finally {
    loading.value = false
  }
}

onMounted(load)

watch(
  () => [props.achievementId, props.media.id],
  () => load(),
)

onBeforeUnmount(() => {
  if (url.value) URL.revokeObjectURL(url.value)
})
</script>

<template>
  <div class="am-tile">
    <div v-if="loading" class="am-tile__placeholder">…</div>
    <div v-else-if="error" class="am-tile__err small">{{ error }}</div>
    <template v-else>
      <img v-if="media.mediaKind === 'IMAGE'" :src="url" class="am-tile__img" alt="" />
      <video v-else-if="media.mediaKind === 'VIDEO'" :src="url" class="am-tile__video" controls playsinline />
    </template>
    <div class="am-tile__meta small text-muted text-truncate" :title="media.originalFilename">
      {{ media.originalFilename }}
    </div>
    <button type="button" class="btn btn-sm btn-outline-danger am-tile__del" @click="emit('delete')">
      ×
    </button>
  </div>
</template>
