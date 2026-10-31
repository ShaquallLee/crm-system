import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import router from '@/router'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 20000
})

let handlingUnauthorized = false

function handleUnauthorized(message) {
  if (handlingUnauthorized) {
    return
  }
  handlingUnauthorized = true
  const userStore = useUserStore()
  userStore.logoutLocal()
  ElMessage.warning(message || '登录已过期，请重新登录')
  router.push('/login').finally(() => {
    setTimeout(() => {
      handlingUnauthorized = false
    }, 800)
  })
}

request.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers['Authorization'] = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response) => {
    // 文件下载直接返回完整响应
    if (response.config.responseType === 'blob' || response.config.responseType === 'arraybuffer') {
      return response
    }
    const res = response.data
    if (res.code !== 200) {
      if (res.code === 401) {
        handleUnauthorized(res.message)
      } else {
        ElMessage.error(res.message || '请求失败')
      }
      return Promise.reject(new Error(res.message || 'Error'))
    }
    return res
  },
  (error) => {
    const status = error.response?.status
    const message = error.response?.data?.message
    if (status === 401) {
      handleUnauthorized(message)
    } else if (status === 403) {
      ElMessage.error(message || '无权限访问该资源')
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请稍后重试')
    } else {
      ElMessage.error(message || '网络错误，请稍后重试')
    }
    return Promise.reject(error)
  }
)

export default request
