export interface ApiEnvelope<T> {
  code: number
  msg?: string
  message?: string
  data: T
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface AuthSnapshot {
  tokenName: string
  tokenValue: string
  loginId: string
  loginType?: string
  userInfo?: UserInfo | null
}

export interface LoginPayload {
  username: string
  password: string
}

export interface LoginResult {
  tokenName?: string
  tokenValue: string
  loginId: string | number
  loginType?: string
}

export interface RegisterPayload {
  phone: string
  password: string
  captcha: string
}

export interface ResetPasswordPayload {
  phone: string
  passwd: string
  captcha: string
}

export interface UserInfo {
  id: number
  userName: string
  nickName?: string
  realName?: string
  phone?: string
  avatar?: string
  signature?: string
  email?: string
  isActive?: boolean
  isConfirmed?: boolean
  createTime?: string
  updateTime?: string
}

export interface UserUpdatePayload {
  nickName?: string
  realName?: string
  email?: string
  signature?: string
  avatar?: string
}

export interface Book {
  id: number
  title: string
  subtitle?: string
  coverUrl?: string
  author?: string
  summary?: string
  publisher?: string
  publishDate?: string
  pageCount?: number
  price?: number
  binding?: string
  isbn?: string
  isbn10?: string
  keyword?: string
  edition?: string
  impression?: string
  language?: string
  bookFormat?: string
  classify?: string
  cip?: string
  clc?: string
  label?: string
  remark?: string
  isOnShelf?: boolean
  isBorrowed?: boolean
  userId?: number
  createTime?: string
  updateTime?: string
}

export interface MyBookList {
  count: number
  books: Book[]
}

export interface RecentBook {
  id: number
  title: string
  coverUrl?: string
  updateTime?: string
}

export interface BookCardModel {
  id: number | string
  title: string
  author?: string
  coverUrl?: string
  summary?: string
  secondary?: string
  badge?: string
}

export interface BookMetadataPayload {
  title: string
  subtitle?: string
  author?: string
  publisher?: string
  publishDate?: string
  pageCount?: number | null
  price?: number | null
  binding?: string
  isbn?: string
  isbn10?: string
  keyword?: string
  edition?: string
  impression?: string
  language?: string
  bookFormat?: string
  classify?: string
  cip?: string
  clc?: string
  label?: string
  remark?: string
  coverUrl?: string
}

export interface BookFormPayload extends BookMetadataPayload {
  shelfId?: number | null
  isOnShelf?: boolean
  isBorrowed?: boolean
  owner?: string
  borrowTime?: string
}

export interface BookUpdatePayload extends Partial<BookMetadataPayload> {
  id: number
}

export interface Shelf {
  id: number
  shelfName: string
  address?: string
  isPublic?: boolean
  remark?: string
  userId?: number
  shelfType?: number
  createTime?: string
  updateTime?: string
}

export interface ShelfPayload {
  id?: number
  shelfName: string
  address?: string
  isPublic?: boolean
  remark?: string
  shelfType?: number
}

export interface IsbnBook {
  title?: string
  author?: string
  subtitle?: string
  publishDate?: string
  publisher?: string
  pageCount?: string
  price?: string
  summary?: string
  coverUrl?: string
  isbn?: string
  isbn10?: string
  binding?: string
  keyword?: string
  cip?: string
  edition?: string
  impression?: string
  language?: string
  bookFormat?: string
  clc?: string
}

export interface ReadingRecord {
  id: number
  bookId: number
  readingStatus: number
  bookName?: string
  coverUrl?: string
  createTime?: string
  updateTime?: string
}

export interface ReadingPayload {
  bookId: number
  readingStatus: number
}

export interface BorrowRecord {
  id: number
  title: string
  book_id: number
  user_id: number
  borrow_name: string
  borrowing_time: string
  due_time?: string
  return_time?: string
  borrow_type: number
  status: number
  pic?: string
}

export interface BorrowPayload {
  book_id: number
  borrow_name: string
  borrowing_time: string
  due_time?: string
  borrow_type: number
}

export interface BorrowUpdatePayload {
  borrow_id: number
  borrow_name: string
  borrowing_time?: string
  due_time?: string
}

export interface ReturnPayload {
  borrow_id: number
  return_time: string
}

export interface BorrowSummary {
  total: number
  borrowedIn: number
  borrowedOut: number
  active: number
  overdue: number
}

export interface Group {
  id: number
  groupName: string
  ownerId: number
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface GroupPayload {
  groupId?: number
  groupName: string
  remark?: string
  userIds?: number[]
}

export interface GroupUser {
  id: number
  userId: number
  nickname: string
  joinTime?: string
}

export interface GroupUserOperatePayload {
  groupId: number
  userIds: number[]
  type: 'add' | 'minus'
}

export interface SharePayload {
  groupId: number
  bookId?: number
  bookshelfId?: number
}

export interface ShareInfo {
  id: number
  groupId: number
  targetId: number
  sharePerson: number
  shareTime?: string
}

export interface ShareRecord {
  id: number
  groupId: number
  targetId: number
  shareType: string
  sharePerson: number
  shareTime?: string
  nickName?: string
  name?: string
}

export interface CommentItem {
  id: number
  bookId: number
  userId: number
  comment: string
  comTime?: string
  stars: number
}

export interface BookCommentList {
  starMean: number
  comments: CommentItem[]
}

export interface CommentPayload {
  bookId: number
  comment: string
  starRating: number
}

export interface CollectBook {
  id: number
  bookId: number
  title: string
  pic?: string
  collectTime?: string
}

export interface CollectShelf {
  id: number
  shelfId: number
  shelfName: string
  collectTime?: string
}

export interface CollectPayload {
  bookId?: number
  bookshelfId?: number
}

export interface TopRatedBook {
  id: number
  title: string
  stars: number
  pic?: string
}

export interface PersonalStats {
  owned: {
    totalBooks: number
    booksLentUnreturned: number
    booksBeingCollected: number
    booksByCategory: Array<{ category: string; count: number }>
  }
  borrowed: {
    totalBorrowed: number
    unreturned: number
  }
  collected: {
    totalCollected: number
  }
}

export interface BorrowStats {
  total: number
  reading: number
  read: number
  classifyList: Array<{
    category: string
    total: number
    reading: number
    read: number
  }>
}

export interface CollectStats {
  total: number
  collect: number
  classifyList: Array<{
    category: string
    total: number
    collect: number
  }>
}

export interface BookRank {
  bookId: number
  title: string
  pic?: string
  collectCount: number
  ranking: number
}

export interface UserRank {
  id: number
  userName?: string
  nickName?: string
  bookCount: number
  ranking: number
}

export interface BookRemote {
  id: number
  title: string
  subtitle?: string
  author?: string
  summary?: string
  keyword?: string
  label?: string
  coverUrl?: string
  embeddingStatus?: number
}

export interface UploadPictureResult {
  pic: string
}
