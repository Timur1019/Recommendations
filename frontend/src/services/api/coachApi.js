import { httpClient } from './httpClient'

export const coachApi = {
  async me() {
    const { data } = await httpClient.get('/coaches/me')
    return data
  },

  /** Список спортсменов, закреплённых за тренером (тот же coachId, что в /coaches/me). */
  async myAthletes(coachId) {
    const { data } = await httpClient.get(`/athletes/coach/${coachId}`)
    return data
  },
}
