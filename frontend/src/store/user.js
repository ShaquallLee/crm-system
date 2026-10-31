import { defineStore } from 'pinia'
import { login, getUserInfo, logout as logoutApi } from '@/api/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('crm_token') || '',
    userInfo: JSON.parse(localStorage.getItem('crm_user') || 'null')
  }),
  actions: {
    async login(loginForm) {
      const res = await login(loginForm)
      this.token = res.data.token
      this.userInfo = res.data
      localStorage.setItem('crm_token', this.token)
      localStorage.setItem('crm_user', JSON.stringify(this.userInfo))
      return res.data
    },
    async fetchUserInfo() {
      const res = await getUserInfo()
      this.userInfo = { ...this.userInfo, ...res.data }
      localStorage.setItem('crm_user', JSON.stringify(this.userInfo))
    },
    logoutLocal() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('crm_token')
      localStorage.removeItem('crm_user')
    },
    async logout() {
      try {
        await logoutApi()
      } catch (e) {
        // 网络异常时也要清理本地登录态
      } finally {
        this.logoutLocal()
      }
    },
    isAdmin() {
      return this.userInfo?.roleId === 1
    },
    isManager() {
      return this.userInfo?.roleId === 2
    }
  }
})
