import request from '@/utils/request'
import type {
  Book,
  BookFormPayload,
  BookUpdatePayload,
  BorrowPayload,
  BorrowRecord,
  BorrowUpdatePayload,
  IsbnBook,
  MyBookList,
  PageResult,
  RecentBook,
  ReadingPayload,
  ReadingRecord,
  ReturnPayload,
  Shelf,
  ShelfPayload,
} from '@/types/models'

export const getMyBooks = () => request.get<MyBookList>('/v1/book/my')

export const getRecentBooks = () => request.get<RecentBook[]>('/v1/book/recent')

export const getBookList = (params: {
  page?: number
  page_size?: number
  bookshelf_id?: string | number
  keyword?: string
  classify?: string
}) => request.get<PageResult<Book>>('/v1/book', { params })

export const getBookDetail = (bookId: number) =>
  request.get<Book>('/v1/book/detail', { params: { book_id: bookId } })

export const getBookShelves = (bookId: number) =>
  request.get<Shelf | null>('/v1/book/shelf', { params: { book_id: bookId } })

export const createBook = (payload: BookFormPayload) =>
  request.post<void>('/v1/book', payload)

export const updateBook = (payload: BookUpdatePayload) =>
  request.put<Book>('/v1/book', payload)

export const deleteBook = (id: number) =>
  request.delete<void>('/v1/book', { params: { id } })

export const getShelves = (params?: { user_id?: number; id?: number }) =>
  request.get<Shelf[]>('/v1/bookshelf', { params })

export const listShelves = (params: {
  page?: number
  page_size?: number
  shelf_type?: string
  shelf_name?: string
  user_id?: number
}) => request.get<PageResult<Shelf>>('/v1/bookshelf/list', { params })

export const createShelf = (payload: ShelfPayload) =>
  request.post<Shelf>('/v1/bookshelf', payload)

export const updateShelf = (payload: ShelfPayload) =>
  request.put<Shelf>('/v1/bookshelf', payload)

export const deleteShelf = (shelfId: number) =>
  request.delete<void>('/v1/bookshelf', { data: { shelf_id: shelfId } })

export const getIsbnBook = (isbn: string) =>
  request.get<IsbnBook>('/v1/isbn', { params: { isbn } })

export const getReadingRecords = (bookId?: number) =>
  request.get<ReadingRecord | ReadingRecord[]>('/v1/book/reading', {
    params: bookId ? { book_id: bookId } : undefined,
  })

export const addReadingRecord = (payload: ReadingPayload) =>
  request.post<ReadingRecord>('/v1/book/reading', payload)

export const updateReadingRecord = (payload: ReadingPayload) =>
  request.put<ReadingRecord>('/v1/book/reading', payload)

export const getBorrowRecords = (borrowType?: number) =>
  request.get<BorrowRecord[]>('/v1/book/borrow', {
    params: borrowType ? { borrow_type: borrowType } : undefined,
  })

export const borrowBook = (payload: BorrowPayload) =>
  request.post<void>('/v1/book/borrow', payload)

export const updateBorrowRecord = (payload: BorrowUpdatePayload) =>
  request.put<void>('/v1/book/borrow', payload)

export const returnBook = (payload: ReturnPayload) =>
  request.post<void>('/v1/book/returning', payload)

export const onShelf = (payload: { book_id: number; shelf_id: number }) =>
  request.post<void>('/v1/book/shelf', payload)

export const offShelf = (payload: { book_id: number; shelf_id?: number }) =>
  request.delete<void>('/v1/book/shelf', { data: payload })
