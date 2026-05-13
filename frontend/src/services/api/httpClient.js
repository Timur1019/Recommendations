import axios from 'axios'

export const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

export const httpClient = axios.create({
  baseURL: apiBaseUrl,
  timeout: 20000,
})

httpClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('kn_token')
  if (token) {
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

httpClient.interceptors.response.use(
  (res) => res,
  (err) => {
    // Keep it simple: pages/stores handle messaging.
    return Promise.reject(err)
  },
)

