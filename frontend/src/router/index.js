import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    component: () => import('@/layout/Layout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '数据看板', icon: 'DataAnalysis' }
      },
      {
        path: 'customers',
        name: 'Customers',
        component: () => import('@/views/Customers.vue'),
        meta: { title: '客户管理', icon: 'User' }
      },
      {
        path: 'customers/:id',
        name: 'CustomerDetail',
        component: () => import('@/views/CustomerDetail.vue'),
        meta: { title: '客户详情', hidden: true }
      },
      {
        path: 'opportunities',
        name: 'Opportunities',
        component: () => import('@/views/Opportunities.vue'),
        meta: { title: '商机管理', icon: 'Money' }
      },
      {
        path: 'activities',
        name: 'Activities',
        component: () => import('@/views/Activities.vue'),
        meta: { title: '跟进待办', icon: 'Clock' }
      },
      {
        path: 'users',
        name: 'Users',
        component: () => import('@/views/Users.vue'),
        meta: { title: '用户管理', icon: 'Setting', admin: true }
      },
      {
        path: 'audit',
        name: 'AuditLogs',
        component: () => import('@/views/AuditLogs.vue'),
        meta: { title: '审计日志', icon: 'Document', admin: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  if (to.meta.public) {
    next()
  } else if (!userStore.token) {
    next('/login')
  } else if (to.meta.admin && !userStore.isAdmin()) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router
