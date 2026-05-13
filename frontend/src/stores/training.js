import { defineStore } from 'pinia'
import { trainingApi } from '../services/api/trainingApi'

export const useTrainingStore = defineStore('training', {
  state: () => ({
    trainings: [],
    weekly: null,
    loading: false,
    error: null,
  }),
  actions: {
    async fetchMyTrainings() {
      this.loading = true
      this.error = null
      try {
        this.trainings = await trainingApi.myTrainings()
      } catch (e) {
        this.trainings = []
        this.error = e?.response?.data?.message || 'Не удалось загрузить тренировки'
      } finally {
        this.loading = false
      }
    },
    async fetchWeeklyStats() {
      try {
        this.weekly = await trainingApi.weeklyStats()
      } catch {
        this.weekly = null
      }
    },
  },
})

