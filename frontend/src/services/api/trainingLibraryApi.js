import { httpClient } from './httpClient'

export const trainingLibraryApi = {
  async listCoach() {
    const { data } = await httpClient.get('/training-library')
    return data
  },

  async listAdmin() {
    const { data } = await httpClient.get('/admin/training-library')
    return data
  },

  async uploadAdmin({ title, description, file }) {
    const form = new FormData()
    form.append('title', title)
    if (description != null) form.append('description', description)
    form.append('file', file)
    const { data } = await httpClient.post('/admin/training-library', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 180000,
    })
    return data
  },

  async deleteAdmin(id) {
    await httpClient.delete(`/admin/training-library/${id}`)
  },

  async getFileBlob(id, download = false) {
    const { data } = await httpClient.get(`/training-library/files/${id}`, {
      params: { download: download ? 1 : 0 },
      responseType: 'blob',
      timeout: 120000,
    })
    return data
  },

  async getPreviewText(id) {
    const { data } = await httpClient.get(`/training-library/files/${id}/preview-text`, { timeout: 120000 })
    return data
  },

  async analyzeHandbook(id, payload) {
    const { data } = await httpClient.post(`/training-library/files/${id}/analyze`, payload ?? {}, {
      timeout: 180000,
    })
    return data
  },

  async applyHandbook(id, payload) {
    const { data } = await httpClient.post(`/training-library/files/${id}/apply-handbook`, payload, { timeout: 60000 })
    return data
  },
}

