import { defineStore } from 'pinia'
import { coachApi } from '../services/api/coachApi'

export const useCoachStore = defineStore('coach', {
  state: () => ({
    profile: null,
    athletes: [],
    loading: false,
    error: null,
  }),
  actions: {
    async fetchMe() {
      this.loading = true
      this.error = null
      try {
        this.profile = await coachApi.me()
        this.athletes = await coachApi.myAthletes(this.profile.id)
      } catch (e) {
        this.error = e?.response?.data?.message || 'Не удалось загрузить кабинет тренера'
      } finally {
        this.loading = false
      }
    },
  },
})

