import { defineStore } from 'pinia'
import { achievementApi } from '../services/api/achievementApi'

export const useAchievementStore = defineStore('achievement', {
  state: () => ({
    achievements: [],
    loading: false,
    error: null,
  }),
  actions: {
    async fetchMyAchievements() {
      this.loading = true
      this.error = null
      try {
        this.achievements = await achievementApi.myAchievements()
      } catch (e) {
        this.error = e?.response?.data?.message || 'Не удалось загрузить достижения'
      } finally {
        this.loading = false
      }
    },
  },
})

