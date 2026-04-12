import type { BookCardModel, PageResult } from '@/types/models'

const pictureEndpoint = `${import.meta.env.VITE_API_BASE_URL || '/api'}/v1/picture?pic=`

export const formatDate = (value?: string) => {
  if (!value) {
    return '未记录'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }

  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(date)
}

export const formatDateTime = (value?: string) => {
  if (!value) {
    return '刚刚'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }

  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}

export const formatCurrency = (value?: number | null) => {
  if (value === undefined || value === null || Number.isNaN(value)) {
    return '未定价'
  }

  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    maximumFractionDigits: 2,
  }).format(value)
}

export const readingStatusLabel = (status?: number) => {
  switch (status) {
    case 1:
      return '想读'
    case 2:
      return '在读'
    case 3:
      return '已读'
    default:
      return '未设置'
  }
}

export const borrowTypeLabel = (type?: number) => {
  return type === 1 ? '借入' : '借出'
}

export const borrowStatusLabel = (status?: number) => {
  switch (status) {
    case 1:
      return '已归还'
    case 2:
      return '已逾期'
    default:
      return '借阅中'
  }
}

export const initialsFromName = (value?: string) => {
  if (!value) {
    return 'WS'
  }

  return value
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((item) => item.charAt(0).toUpperCase())
    .join('')
}

export const resolvePictureUrl = (value?: string | null) => {
  if (!value) {
    return ''
  }

  if (/^https?:\/\//.test(value)) {
    return value
  }

  return `${pictureEndpoint}${encodeURIComponent(value)}`
}

export const normalizePage = <T>(value: PageResult<T> | T[]) => {
  if (Array.isArray(value)) {
    return {
      records: value,
      total: value.length,
      size: value.length,
      current: 1,
      pages: 1,
    }
  }

  return value
}

export const buildBookCard = (book: Partial<BookCardModel> & Pick<BookCardModel, 'id' | 'title'>) => ({
  id: book.id,
  title: book.title,
  author: book.author,
  coverUrl: book.coverUrl,
  summary: book.summary,
  secondary: book.secondary,
  badge: book.badge,
})

export const parseTagList = (label?: string) =>
  label
    ?.split(/[，,]/)
    .map((item) => item.trim())
    .filter(Boolean) ?? []
