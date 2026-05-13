import { httpClient } from './httpClient'

export const athleteApi = {
  async me() {
    const { data } = await httpClient.get('/athletes/me')
    return data
  },
  async patchDisplayName(payload) {
    const { data } = await httpClient.patch('/athletes/me/name', payload)
    return data
  },
  async updateMe(payload) {
    const { data } = await httpClient.put('/athletes/me', payload)
    return data
  },
}

