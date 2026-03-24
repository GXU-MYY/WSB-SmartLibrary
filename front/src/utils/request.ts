import axios, {
  AxiosError,
  type AxiosRequestConfig,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios'

import type { ApiEnvelope } from '@/types/models'
import { clearAuthSnapshot, DEFAULT_TOKEN_NAME, getAuthSnapshot } from './auth'
import { notifyError } from './notify'

interface RetryableRequestConfig extends InternalAxiosRequestConfig {
  _retryCount?: number
}

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 12000,
})

const MAX_GET_RETRY = 1

const wait = (time: number) => new Promise((resolve) => setTimeout(resolve, time))

const resolveMessage = (payload?: Partial<ApiEnvelope<unknown>>) =>
  payload?.message || payload?.msg || 'Request failed, please try again later.'

const isAuthMessage = (message: string) =>
  /login|token|auth|unauthorized|401/i.test(message)

const shouldRetry = (
  error: AxiosError<{ message?: string; msg?: string }>,
  config?: RetryableRequestConfig,
) => {
  if (!config) {
    return false
  }

  const method = (config.method || 'get').toLowerCase()
  const retryCount = config._retryCount || 0
  return method === 'get' && !error.response && retryCount < MAX_GET_RETRY
}

service.interceptors.request.use((config) => {
  const auth = getAuthSnapshot()

  if (auth?.tokenValue) {
    // Sa-Token can read both the custom header and a Bearer token.
    config.headers.set(auth.tokenName || DEFAULT_TOKEN_NAME, auth.tokenValue)
    config.headers.set('Authorization', `Bearer ${auth.tokenValue}`)
  }

  return config
})

service.interceptors.response.use(
  (response) => {
    const payload = response.data as ApiEnvelope<unknown> | Blob | ArrayBuffer | string

    if (payload && typeof payload === 'object' && 'code' in payload) {
      const message = resolveMessage(payload)

      if (Number(payload.code) !== 200) {
        if (isAuthMessage(message)) {
          clearAuthSnapshot()
        }

        notifyError('Request was rejected', message)
        return Promise.reject(new Error(message))
      }

      response.data = payload.data
    }

    return response
  },
  async (error: AxiosError<{ message?: string; msg?: string }>) => {
    const originalRequest = error.config as RetryableRequestConfig | undefined

    if (originalRequest && shouldRetry(error, originalRequest)) {
      originalRequest._retryCount = (originalRequest._retryCount || 0) + 1
      await wait(400 * originalRequest._retryCount)
      return service(originalRequest)
    }

    const message =
      error.response?.data?.message ||
      error.response?.data?.msg ||
      error.message ||
      'Network error, please try again later.'

    if (isAuthMessage(message)) {
      clearAuthSnapshot()
    }

    notifyError('Network issue detected', message)
    return Promise.reject(error)
  },
)

const unwrap = <T>(promise: Promise<AxiosResponse<T>>) => promise.then((response) => response.data)

const request = {
  get<T>(url: string, config?: AxiosRequestConfig) {
    return unwrap(service.get<unknown, AxiosResponse<T>>(url, config))
  },
  post<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
    return unwrap(service.post<unknown, AxiosResponse<T>>(url, data, config))
  },
  put<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
    return unwrap(service.put<unknown, AxiosResponse<T>>(url, data, config))
  },
  delete<T>(url: string, config?: AxiosRequestConfig) {
    return unwrap(service.delete<unknown, AxiosResponse<T>>(url, config))
  },
}

export default request
