import request from '@/utils/request'
import type { BookRemote, RecommendBookPreview } from '@/types/models'

export const SIMILAR_BOOK_LIMIT = 4

export const getAiSummary = (bookId: number) =>
  request.get<string>(`/v1/rag/summary/${bookId}`)

export const getReviewDigest = (bookId: number) =>
  request.get<string>(`/v1/rag/reviews/${bookId}`)

export const generateAiSummary = (bookId: number) =>
  request.post<string>(`/v1/rag/summary/${bookId}`)

export const aggregateReviews = (bookId: number) =>
  request.post<string>(`/v1/rag/reviews/${bookId}`, null, {
    timeout: 60000,
  })

export const getSimilarBooks = (bookId: number, limit = SIMILAR_BOOK_LIMIT) =>
  request.get<BookRemote[]>(`/v1/rag/similar/${bookId}`, {
    params: { limit },
  })

export const getRecommendBookPreview = (bookId: number) =>
  request.get<RecommendBookPreview>(`/v1/rag/books/${bookId}/preview`)

export const recommendBooks = (query: string, limit = 6, mineOnly = false) =>
  request.get<BookRemote[]>('/v1/rag/recommend', {
    params: { query, limit, mineOnly },
  })
