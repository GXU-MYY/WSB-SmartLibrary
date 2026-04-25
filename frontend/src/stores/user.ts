import { defineStore } from 'pinia'

import { getUserProfile, login as loginApi, logout as logoutApi, updateUserProfile } from '@/api/user'
import type { LoginPayload, UserUpdatePayload } from '@/types/models'
import {
  clearAuthSnapshot,
  DEFAULT_TOKEN_NAME,
  getAuthSnapshot,
  setAuthSnapshot,
} from '@/utils/auth'
import { notifyInfo, notifySuccess } from '@/utils/notify'

const cachedAuth = getAuthSnapshot()

export const useUserStore = defineStore('user', {
  state: () => ({
    tokenName: cachedAuth?.tokenName || DEFAULT_TOKEN_NAME,
    tokenValue: cachedAuth?.tokenValue || '',
    loginId: cachedAuth?.loginId || '',
    loginType: cachedAuth?.loginType || '',
    userInfo: cachedAuth?.userInfo || null,
    profileLoading: false,
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.tokenValue),
    displayName: (state) => state.userInfo?.nickName || state.userInfo?.userName || '书友',
  },
  actions: {
    persist() {
      if (!this.tokenValue) {
        clearAuthSnapshot()
        return
      }

      setAuthSnapshot({
        tokenName: this.tokenName,
        tokenValue: this.tokenValue,
        loginId: this.loginId,
        loginType: this.loginType,
        userInfo: this.userInfo,
      })
    },
    async login(payload: LoginPayload) {
      const result = await loginApi(payload)
      this.tokenName = result.tokenName || DEFAULT_TOKEN_NAME
      this.tokenValue = result.tokenValue
      this.loginId = String(result.loginId)
      this.loginType = result.loginType || ''
      this.persist()
      notifySuccess('欢迎回来', '你的书库工作台已经准备就绪。')
      return this.fetchProfile(true)
    },
    async fetchProfile(force = false) {
      if (!this.loginId) {
        return null
      }

      if (this.userInfo && !force) {
        return this.userInfo
      }

      this.profileLoading = true

      try {
        const profile = await getUserProfile(Number(this.loginId))
        this.userInfo = profile
        this.persist()
        return profile
      } finally {
        this.profileLoading = false
      }
    },
    async updateProfile(payload: UserUpdatePayload) {
      const result = await updateUserProfile(payload)
      this.userInfo = result
      this.persist()
      notifySuccess('资料已更新', '新的个人信息已经同步到当前会话。')
      return result
    },
    async logout() {
      try {
        if (this.tokenValue) {
          await logoutApi()
        }
      } finally {
        this.tokenName = DEFAULT_TOKEN_NAME
        this.tokenValue = ''
        this.loginId = ''
        this.loginType = ''
        this.userInfo = null
        clearAuthSnapshot()
        notifyInfo('已退出登录', '期待你下次回来继续整理书库。')
      }
    },
  },
})
