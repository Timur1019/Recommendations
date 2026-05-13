import { httpClient } from './httpClient'

export const trainingRequestApi = {
  async listMine() {
    const { data } = await httpClient.get('/training-requests/me')
    return data
  },
  async listCoach() {
    const { data } = await httpClient.get('/training-requests/coach')
    return data
  },
  async listAdmin() {
    const { data } = await httpClient.get('/training-requests/admin')
    return data
  },
  /**
   * @param {{ note?: string }} [body]
   */
  async submitMe(body) {
    const { data } = await httpClient.post('/training-requests/me', body ?? {})
    return data
  },
}
