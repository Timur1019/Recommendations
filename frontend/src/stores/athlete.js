import { defineStore } from 'pinia'
import { athleteApi } from '../services/api/athleteApi'

export const useAthleteStore = defineStore('athlete', {
  state: () => ({
    profile: null,
    loading: false,
    error: null,
  }),
  actions: {
    async fetchMyProfile() {
      this.loading = true
      this.error = null
      try {
        this.profile = await athleteApi.me()
      } catch (e) {
        this.error = e?.response?.data?.message || 'Не удалось загрузить профиль'
      } finally {
        this.loading = false
      }
    },
    async patchDisplayName(firstName, lastName) {
      this.profile = await athleteApi.patchDisplayName({ firstName, lastName })
      return this.profile
    },
  },
})

