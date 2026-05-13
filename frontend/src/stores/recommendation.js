import { defineStore } from 'pinia'
import { recommendationApi } from '../services/api/recommendationApi'

export const useRecommendationStore = defineStore('recommendation', {
  state: () => ({
    latest: null,
    comparison: null,
    weekPlan: null,
    loading: false,
    error: null,
    exporting: false,
  }),
  actions: {
    async fetchAll() {
      this.loading = true
      this.error = null
      try {
        const results = await Promise.allSettled([
          recommendationApi.latest(),
          recommendationApi.compare(),
          recommendationApi.weekPlan(),
        ])
        const errs = []
        if (results[0].status === 'fulfilled') {
          this.latest = results[0].value
        } else {
          this.latest = null
          errs.push(results[0].reason)
        }
        if (results[1].status === 'fulfilled') {
          this.comparison = results[1].value
        } else {
          this.comparison = null
          errs.push(results[1].reason)
        }
        if (results[2].status === 'fulfilled') {
          this.weekPlan = results[2].value
        } else {
          this.weekPlan = null
          errs.push(results[2].reason)
        }
        if (errs.length) {
          const first = errs[0]
          this.error =
            first?.response?.data?.message ||
            first?.message ||
            'Не удалось загрузить часть данных. Проверьте, что бэкенд запущен и вы вошли как спортсмен.'
        }
      } catch (e) {
        this.error = e?.response?.data?.message || 'Не удалось загрузить рекомендации'
      } finally {
        this.loading = false
      }
    },
    async generateMe() {
      this.error = null
      try {
        await recommendationApi.generateMe()
        await this.fetchAll()
      } catch (e) {
        this.error = e?.response?.data?.message || 'Не удалось сгенерировать рекомендацию'
      }
    },
    async exportPdf() {
      this.exporting = true
      try {
        const blob = await recommendationApi.exportPdf()
        const url = window.URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = 'recommendation.pdf'
        a.click()
        window.URL.revokeObjectURL(url)
      } finally {
        this.exporting = false
      }
    },
  },
})

