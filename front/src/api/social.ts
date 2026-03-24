import request from '@/utils/request'
import type {
  BookCommentList,
  CollectBook,
  CollectPayload,
  CollectShelf,
  CommentItem,
  CommentPayload,
  TopRatedBook,
} from '@/types/models'

export const getBookComments = (bookId: number) =>
  request.get<BookCommentList>('/v1/social/book/comment', {
    params: { book_id: bookId },
  })

export const getMyComments = () =>
  request.get<CommentItem[]>('/v1/social/book/comment')

export const addComment = (payload: CommentPayload) =>
  request.post<CommentItem>('/v1/social/book/comment', payload)

export const deleteComment = (commentId: number) =>
  request.delete<void>('/v1/social/book/comment', { data: { commentId } })

export const getTopRatedBooks = (top = 6) =>
  request.get<TopRatedBook[]>('/v1/stars', { params: { top } })

export const getMyBookCollects = () =>
  request.get<CollectBook[]>('/v1/collect', { params: { type: 'book' } })

export const getMyShelfCollects = () =>
  request.get<CollectShelf[]>('/v1/collect', { params: { type: 'bookshelf' } })

export const addCollect = (payload: CollectPayload) =>
  request.post<void>('/v1/collect', payload)

export const deleteCollect = (collectId: number) =>
  request.delete<void>('/v1/collect', { data: { collectId } })
