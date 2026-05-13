import { httpClient } from './httpClient'

export const recommendationApi = {
  /** @returns {Promise<object|null>} null если сохранённой рекомендации ещё нет (HTTP 204) */
  async latest() {
    const res = await httpClient.get('/recommendations/me/latest')
    if (res.status === 204) return null
    return res.data ?? null
  },
  async generateMe() {
    const { data } = await httpClient.post('/recommendations/me/generate')
    return data
  },
  async compare() {
    const { data } = await httpClient.get('/recommendations/me/compare')
    return data
  },
  async weekPlan() {
    const { data } = await httpClient.get('/recommendations/me/week-plan')
    return data
  },
  async exportPdf() {
    const res = await httpClient.get('/recommendations/me/export-pdf', { responseType: 'blob' })
    return res.data
  },
}

