import { defineStore } from 'pinia'
import { adminApi } from '../services/api/adminApi'

export const useAdminStore = defineStore('admin', {
  state: () => ({
    users: [],
    loading: false,
    error: null,
    busyByUserId: {},
    recalcLoading: false,
    recalcMessage: null,
    recalcError: null,
  }),
  actions: {
    async fetchUsers() {
      this.loading = true
      this.error = null
      try {
        this.users = await adminApi.listUsers()
      } catch (e) {
        this.error = e?.response?.data?.message || 'Не удалось загрузить пользователей'
      } finally {
        this.loading = false
      }
    },
    async changeRole(userId, role) {
      this.error = null
      this.busyByUserId = { ...this.busyByUserId, [userId]: true }
      try {
        const updated = await adminApi.updateUserRole(userId, role)
        this.users = this.users.map((u) => (u.id === userId ? updated : u))
      } catch (e) {
        this.error = e?.response?.data?.message || 'Не удалось сменить роль'
        throw e
      } finally {
        const next = { ...this.busyByUserId }
        delete next[userId]
        this.busyByUserId = next
      }
    },
    async toggleActive(userId, active) {
      this.error = null
      this.busyByUserId = { ...this.busyByUserId, [userId]: true }
      try {
        const updated = await adminApi.setUserActive(userId, active)
        this.users = this.users.map((u) => (u.id === userId ? updated : u))
      } catch (e) {
        this.error = e?.response?.data?.message || 'Не удалось изменить статус'
        throw e
      } finally {
        const next = { ...this.busyByUserId }
        delete next[userId]
        this.busyByUserId = next
      }
    },
    async recalculateGoldStandards() {
      this.recalcLoading = true
      this.recalcMessage = null
      this.recalcError = null
      try {
        await adminApi.recalculateGoldStandards()
        this.recalcMessage = 'ok'
      } catch (e) {
        this.recalcError = e?.response?.data?.message || 'Не удалось запустить пересчёт'
      } finally {
        this.recalcLoading = false
      }
    },
    clearRecalcFeedback() {
      this.recalcMessage = null
      this.recalcError = null
    },
    async assignAthleteCoach(athleteId, coachId) {
      this.error = null
      try {
        await adminApi.assignAthleteCoach(athleteId, coachId)
        await this.fetchUsers()
      } catch (e) {
        this.error = e?.response?.data?.message || 'Не удалось назначить тренера'
        throw e
      }
    },
  },
})
