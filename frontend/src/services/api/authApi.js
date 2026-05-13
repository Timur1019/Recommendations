import { httpClient } from './httpClient'

export const authApi = {
  async login(payload) {
    const { data } = await httpClient.post('/auth/login', payload)
    return data
  },
  async registerAthlete(payload) {
    const { data } = await httpClient.post('/auth/register/athlete', payload)
    return data
  },
  async registerCoach(payload) {
    const { data } = await httpClient.post('/auth/register/coach', payload)
    return data
  },
  async me() {
    const { data } = await httpClient.get('/auth/me')
    return data
  },
  async patchMyProfile(payload) {
    const { data } = await httpClient.patch('/auth/me/profile', payload)
    return data
  },
}

