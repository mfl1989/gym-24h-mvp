import axios from 'axios'

const configuredBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim()

const api = axios.create({
  baseURL: configuredBaseUrl ? configuredBaseUrl.replace(/\/$/, '') : undefined,
})

api.interceptors.request.use((config) => {
  const authToken = localStorage.getItem('authToken')

  if (authToken) {
    config.headers.Authorization = `Bearer ${authToken}`
  }

  return config
})

export default api
