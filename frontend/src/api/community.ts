import request from '@/utils/request'
import { cachedRequest, invalidateRequestCache } from '@/utils/requestCache'
import type {
  BookRank,
  BorrowStats,
  CollectStats,
  Group,
  GroupBorrowRequest,
  GroupBorrowRequestPayload,
  GroupPublicBook,
  GroupPublicShelf,
  GroupPayload,
  GroupUser,
  GroupUserOperatePayload,
  PageResult,
  PersonalStats,
  ShareInfo,
  SharePayload,
  ShareRecord,
  UserRank,
} from '@/types/models'

const PERSONAL_STATS_CACHE_KEY = 'community:personal-stats'
const BOOK_RANK_CACHE_PREFIX = 'community:book-rank:'
const USER_RANK_CACHE_PREFIX = 'community:user-rank:'
const GROUP_PUBLIC_SHELVES_CACHE_PREFIX = 'community:group-public-shelves:'
const GROUP_PUBLIC_BOOKS_CACHE_PREFIX = 'community:group-public-books:'

const PERSONAL_STATS_CACHE_TTL = 15_000
const RANK_CACHE_TTL = 20_000
const GROUP_PUBLIC_CACHE_TTL = 20_000

const buildRankKey = (prefix: string, params: { page?: number; page_size?: number }) =>
  `${prefix}${params.page || 1}:${params.page_size || 10}`

export const getGroups = (params: { page?: number; page_size?: number; name?: string }) =>
  request.get<PageResult<Group>>('/v1/group', { params })

export const createGroup = async (payload: GroupPayload) => {
  const result = await request.post<Group>('/v1/group', payload)
  invalidateRequestCache(GROUP_PUBLIC_SHELVES_CACHE_PREFIX)
  invalidateRequestCache(GROUP_PUBLIC_BOOKS_CACHE_PREFIX)
  return result
}

export const updateGroup = async (payload: GroupPayload) => {
  const result = await request.put<Group>('/v1/group', payload)
  invalidateRequestCache(GROUP_PUBLIC_SHELVES_CACHE_PREFIX)
  invalidateRequestCache(GROUP_PUBLIC_BOOKS_CACHE_PREFIX)
  return result
}

export const deleteGroup = async (groupId: number) => {
  const result = await request.delete<void>('/v1/group', { params: { group_id: groupId } })
  invalidateRequestCache(`${GROUP_PUBLIC_SHELVES_CACHE_PREFIX}${groupId}`)
  invalidateRequestCache(`${GROUP_PUBLIC_BOOKS_CACHE_PREFIX}${groupId}`)
  return result
}

export const exitGroup = async (groupId: number) => {
  const result = await request.delete<void>('/v1/group/user/exit', {
    params: {
      group_id: groupId,
    },
  })
  invalidateRequestCache(`${GROUP_PUBLIC_SHELVES_CACHE_PREFIX}${groupId}`)
  invalidateRequestCache(`${GROUP_PUBLIC_BOOKS_CACHE_PREFIX}${groupId}`)
  return result
}

export const getGroupUsers = (groupId: number, type: 'in' | 'out') =>
  request.get<GroupUser[]>('/v1/group/user', {
    params: {
      group_id: groupId,
      type,
    },
  })

export const operateGroupUsers = async (payload: GroupUserOperatePayload) => {
  const result = await request.post<void>('/v1/group/user', payload)
  invalidateRequestCache(`${GROUP_PUBLIC_SHELVES_CACHE_PREFIX}${payload.groupId}`)
  invalidateRequestCache(`${GROUP_PUBLIC_BOOKS_CACHE_PREFIX}${payload.groupId}`)
  return result
}

export const removeGroupMember = async (groupId: number, userId: number) => {
  const result = await request.delete<void>('/v1/group/user', {
    params: {
      group_id: groupId,
      user_id: userId,
    },
  })
  invalidateRequestCache(`${GROUP_PUBLIC_SHELVES_CACHE_PREFIX}${groupId}`)
  invalidateRequestCache(`${GROUP_PUBLIC_BOOKS_CACHE_PREFIX}${groupId}`)
  return result
}

export const shareToGroup = (payload: SharePayload) =>
  request.post<ShareInfo>('/v1/group/share', payload)

export const getShareRecords = (groupId: number, shareType?: string) =>
  request.get<ShareRecord[]>('/v1/group/share', {
    params: {
      group_id: groupId,
      share_type: shareType,
    },
  })

export const getGroupPublicShelves = (groupId: number) =>
  cachedRequest(`${GROUP_PUBLIC_SHELVES_CACHE_PREFIX}${groupId}`, GROUP_PUBLIC_CACHE_TTL, () =>
    request.get<GroupPublicShelf[]>('/v1/group/borrow/public/shelves', {
      params: {
        group_id: groupId,
      },
    }),
  )

export const getGroupPublicBooks = (groupId: number) =>
  cachedRequest(`${GROUP_PUBLIC_BOOKS_CACHE_PREFIX}${groupId}`, GROUP_PUBLIC_CACHE_TTL, () =>
    request.get<GroupPublicBook[]>('/v1/group/borrow/public/books', {
      params: {
        group_id: groupId,
      },
    }),
  )

export const createGroupBorrowRequest = async (payload: GroupBorrowRequestPayload) => {
  const result = await request.post<GroupBorrowRequest>('/v1/group/borrow/request', payload)
  invalidateRequestCache(`${GROUP_PUBLIC_BOOKS_CACHE_PREFIX}${payload.groupId}`)
  return result
}

export const getGroupBorrowRequests = (groupId: number) =>
  request.get<GroupBorrowRequest[]>('/v1/group/borrow/request', {
    params: {
      group_id: groupId,
    },
  })

export const approveGroupBorrowRequest = async (requestId: number) => {
  const result = await request.post<GroupBorrowRequest>(`/v1/group/borrow/request/${requestId}/approve`)
  invalidateRequestCache(GROUP_PUBLIC_BOOKS_CACHE_PREFIX)
  return result
}

export const rejectGroupBorrowRequest = async (requestId: number) => {
  const result = await request.post<GroupBorrowRequest>(`/v1/group/borrow/request/${requestId}/reject`)
  return result
}

export const getBorrowSummary = (scope: 'all' | 'mine') =>
  request.get<BorrowStats>('/v1/community/statistics/summary', {
    params: {
      scope,
      type: 'borrow',
    },
  })

export const getCollectSummary = (scope: 'all' | 'mine') =>
  request.get<CollectStats>('/v1/community/statistics/summary', {
    params: {
      scope,
      type: 'collect',
    },
  })

export const getPersonalStats = () =>
  cachedRequest(PERSONAL_STATS_CACHE_KEY, PERSONAL_STATS_CACHE_TTL, () =>
    request.get<PersonalStats>('/v1/community/statistics/personal'),
  )

export const getBookRank = (params: { page?: number; page_size?: number }) =>
  cachedRequest(buildRankKey(BOOK_RANK_CACHE_PREFIX, params), RANK_CACHE_TTL, () =>
    request.get<PageResult<BookRank>>('/v1/community/statistics/rank', {
      params: {
        ...params,
        type: 'book',
      },
    }),
  )

export const getUserRank = (params: { page?: number; page_size?: number }) =>
  cachedRequest(buildRankKey(USER_RANK_CACHE_PREFIX, params), RANK_CACHE_TTL, () =>
    request.get<PageResult<UserRank>>('/v1/community/statistics/rank', {
      params: {
        ...params,
        type: 'user',
      },
    }),
  )
