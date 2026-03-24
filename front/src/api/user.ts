import request from '@/utils/request'
import type {
  LoginPayload,
  LoginResult,
  PageResult,
  RegisterPayload,
  ResetPasswordPayload,
  UserInfo,
  UserUpdatePayload,
} from '@/types/models'

/**
 * 登录并获取 Sa-Token 会话信息。
 */
export const login = (payload: LoginPayload) =>
  request.post<LoginResult>('/v1/admin/login', payload)

/**
 * 退出当前登录态。
 */
export const logout = () => request.post<void>('/v1/admin/logout')

/**
 * 注册新用户。
 */
export const register = (payload: RegisterPayload) =>
  request.post<UserInfo>('/v1/admin/register', payload)

/**
 * 获取用户详情。
 */
export const getUserProfile = (userId: number) =>
  request.get<UserInfo>('/v1/admin/user', { params: { user_id: userId } })

/**
 * 获取用户列表，用于群组邀请等场景。
 */
export const getUsers = (params: { page?: number; page_size?: number; user_name?: string }) =>
  request.get<PageResult<UserInfo>>('/v1/admin/user', { params })

/**
 * 更新当前用户资料。
 */
export const updateUserProfile = (payload: UserUpdatePayload) =>
  request.put<UserInfo>('/v1/admin/user', payload)

export const resetPassword = (payload: ResetPasswordPayload) =>
  request.post<void>('/v1/admin/passwd', payload)

export const sendCaptcha = (phone: string) =>
  request.get<void>('/v1/admin/phone/captcha', { params: { phone } })
