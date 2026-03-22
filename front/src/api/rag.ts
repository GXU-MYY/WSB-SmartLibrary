import request from '@/utils/request'
import type { BookRemote } from '@/types/models'

export const getAiSummary = (bookId: number) =>
  request.get<string>(`/v1/rag/summary/${bookId}`)

export const generateAiSummary = (bookId: number) =>
  request.post<string>(`/v1/rag/summary/${bookId}`)

export const aggregateReviews = (bookId: number) =>
  request.post<string>(`/v1/rag/reviews/${bookId}`)

export const getSimilarBooks = (bookId: number, limit = 6) =>
  request.get<BookRemote[]>(`/v1/rag/similar/${bookId}`, {
    params: { limit },
  })

export const recommendBooks = (query: string, limit = 6) =>
  request.post<BookRemote[]>('/v1/rag/recommend', null, {
    params: { query, limit },
  })
