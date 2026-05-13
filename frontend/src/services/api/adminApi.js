import { httpClient } from './httpClient'

export const adminApi = {
  async listUsers() {
    const { data } = await httpClient.get('/admin/users')
    return data
  },
  async updateUserRole(userId, role) {
    const { data } = await httpClient.put(`/admin/users/${userId}/role`, { role })
    return data
  },
  async setUserActive(userId, active) {
    const { data } = await httpClient.put(`/admin/users/${userId}/activate`, { active })
    return data
  },
  async recalculateGoldStandards() {
    await httpClient.post('/admin/gold-standard/recalculate')
  },
  async listCoaches() {
    const { data } = await httpClient.get('/admin/coaches')
    return data
  },
  async assignAthleteCoach(athleteId, coachId) {
    const { data } = await httpClient.patch(`/admin/athletes/${athleteId}/coach`, { coachId })
    return data
  },
}

