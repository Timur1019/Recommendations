import { httpClient } from './httpClient'

export const achievementApi = {
  async myAchievements() {
    const { data } = await httpClient.get('/achievements/me')
    return data
  },
  async requestBatch(payload) {
    const { data } = await httpClient.post('/achievements/request/batch', payload)
    return data
  },
  async myInsight() {
    const { data } = await httpClient.get('/achievements/me/insight')
    return data
  },
  async cohortInsight(athleteIds) {
    const { data } = await httpClient.post('/achievements/coach/cohort-insight', {
      athleteIds: athleteIds?.length ? athleteIds : null,
    })
    return data
  },
  async aiStatus() {
    const { data } = await httpClient.get('/achievements/ai/status')
    return data
  },

  async uploadAchievementMedia(achievementId, file) {
    const form = new FormData()
    form.append('file', file)
    const { data } = await httpClient.post(`/achievements/${achievementId}/media`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 180000,
    })
    return data
  },

  async deleteAchievementMedia(achievementId, mediaId) {
    await httpClient.delete(`/achievements/${achievementId}/media/${mediaId}`)
  },

  /** object URL — вызвать URL.revokeObjectURL после использования */
  async getMediaBlobUrl(achievementId, mediaId) {
    const { data } = await httpClient.get(`/achievements/${achievementId}/media/${mediaId}/file`, {
      responseType: 'blob',
      timeout: 180000,
    })
    return URL.createObjectURL(data)
  },
}

