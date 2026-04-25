type CacheEntry<T> = {
  data: T
  expiresAt: number
}

const responseCache = new Map<string, CacheEntry<unknown>>()
const pendingRequests = new Map<string, Promise<unknown>>()

export const cachedRequest = <T>(key: string, ttlMs: number, loader: () => Promise<T>) => {
  const now = Date.now()
  const cached = responseCache.get(key)
  if (cached && cached.expiresAt > now) {
    return Promise.resolve(cached.data as T)
  }

  const pending = pendingRequests.get(key)
  if (pending) {
    return pending as Promise<T>
  }

  const request = loader()
    .then((data) => {
      responseCache.set(key, {
        data,
        expiresAt: Date.now() + ttlMs,
      })
      pendingRequests.delete(key)
      return data
    })
    .catch((error) => {
      pendingRequests.delete(key)
      throw error
    })

  pendingRequests.set(key, request as Promise<unknown>)
  return request
}

export const invalidateRequestCache = (prefix: string) => {
  Array.from(responseCache.keys())
    .filter((key) => key.startsWith(prefix))
    .forEach((key) => responseCache.delete(key))

  Array.from(pendingRequests.keys())
    .filter((key) => key.startsWith(prefix))
    .forEach((key) => pendingRequests.delete(key))
}
