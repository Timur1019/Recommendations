import { defineStore } from 'pinia'
import { authApi } from '../services/api/authApi'
import { useTrainingStore } from './training'
import { useCoachStore } from './coach'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null,
    token: localStorage.getItem('kn_token') || null,
    loading: false,
    error: null,
  }),
  getters: {
    isAuthenticated: (s) => Boolean(s.token),
    homeRoute: (s) => {
      const role = s.user?.role
      if (role === 'ADMIN') return { path: '/admin/users' }
      if (role === 'COACH') return { path: '/coach/cabinet' }
      if (role === 'ATHLETE') return { path: '/dashboard' }
      return { path: '/login' }
    },
  },
  actions: {
    async login(email, password) {
      this.loading = true
      this.error = null
      try {
        const res = await authApi.login({ email, password })
        this.token = res.token
        this.user = res.user
        localStorage.setItem('kn_token', this.token)
        return true
      } catch (e) {
        this.error = e?.response?.data?.message || 'Login failed'
        return false
      } finally {
        this.loading = false
      }
    },
    async fetchMe() {
      if (!this.token) return null
      try {
        this.user = await authApi.me()
        return this.user
      } catch {
        this.logout()
        return null
      }
    },
    logout() {
      try {
        useTrainingStore().$reset()
        useCoachStore().$reset()
      } catch {
        /* pinia не инициализирован */
      }
      this.user = null
      this.token = null
      localStorage.removeItem('kn_token')
    },
  },
})

