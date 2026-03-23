import request from '@/utils/request'
import type {
  BookRank,
  BorrowStats,
  CollectStats,
  Group,
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

export const getGroups = (params: { page?: number; page_size?: number; name?: string }) =>
  request.get<PageResult<Group>>('/v1/group', { params })

export const createGroup = (payload: GroupPayload) =>
  request.post<Group>('/v1/group', payload)

export const updateGroup = (payload: GroupPayload) =>
  request.put<Group>('/v1/group', payload)

export const deleteGroup = (groupId: number) =>
  request.delete<void>('/v1/group', { params: { group_id: groupId } })

export const getGroupUsers = (groupId: number, type: 'in' | 'out') =>
  request.get<GroupUser[]>('/v1/group/user', {
    params: {
      group_id: groupId,
      type,
    },
  })

export const operateGroupUsers = (payload: GroupUserOperatePayload) =>
  request.post<void>('/v1/group/user', payload)

export const shareToGroup = (payload: SharePayload) =>
  request.post<ShareInfo>('/v1/group/share', payload)

export const getShareRecords = (groupId: number, shareType?: string) =>
  request.get<ShareRecord[]>('/v1/group/share', {
    params: {
      group_id: groupId,
      share_type: shareType,
    },
  })

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
  request.get<PersonalStats>('/v1/community/statistics/personal')

export const getBookRank = (params: { page?: number; page_size?: number }) =>
  request.get<PageResult<BookRank>>('/v1/community/statistics/rank', {
    params: {
      ...params,
      type: 'book',
    },
  })

export const getUserRank = (params: { page?: number; page_size?: number }) =>
  request.get<PageResult<UserRank>>('/v1/community/statistics/rank', {
    params: {
      ...params,
      type: 'user',
    },
  })
