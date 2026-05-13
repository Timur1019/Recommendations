import { httpClient } from './httpClient'

export const adminAiApi = {
  async integration() {
    const { data } = await httpClient.get('/admin/ai/integration')
    return data
  },

  async listPrompts() {
    const { data } = await httpClient.get('/admin/ai/prompts')
    return data
  },

  async updatePrompt(settingKey, payload) {
    const { data } = await httpClient.put(
      `/admin/ai/prompts/${encodeURIComponent(settingKey)}`,
      payload,
    )
    return data
  },
}
