import { httpClient } from './httpClient'

export const trainingApi = {
  async myTrainings() {
    const { data } = await httpClient.get('/trainings/me')
    return data
  },
  /**
   * @param {{
   *   athleteId: number,
   *   trainingDate: string,
   *   workoutType: string,
   *   durationMinutes: number,
   *   intensity?: number | null,
   *   technicalActions?: unknown | null,
   *   notes?: string | null
   * }} payload
   */
  async create(payload) {
    const { data } = await httpClient.post('/trainings', payload)
    return data
  },
  async weeklyStats() {
    const { data } = await httpClient.get('/trainings/statistics/weekly')
    return data
  },
}

