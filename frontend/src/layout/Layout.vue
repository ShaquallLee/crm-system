<script setup>
import { computed, reactive, ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { getReminders } from '@/api/activity'
import { changePassword } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const menuItems = computed(() => {
  const items = [
    { path: '/dashboard', title: '数据看板', icon: 'DataAnalysis' },
    { path: '/customers', title: '客户管理', icon: 'User' },
    { path: '/opportunities', title: '商机管理', icon: 'Money' },
    { path: '/activities', title: '跟进待办', icon: 'Clock' }
  ]
  if (userStore.isAdmin()) {
    items.push({ path: '/users', title: '用户管理', icon: 'Setting' })
    items.push({ path: '/audit', title: '审计日志', icon: 'Document' })
  }
  return items
})

const activeMenu = computed(() => '/' + route.path.split('/')[1])

const reminders = ref({ overdueCount: 0, todayCount: 0, upcomingCount: 0, items: [] })
const badgeCount = computed(() => reminders.value.overdueCount + reminders.value.todayCount)
let pollTimer = null

const pwdVisible = ref(false)
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

async function loadReminders() {
  try {
    const res = await getReminders()
    reminders.value = res.data || reminders.value
  } catch (e) {
    // 轮询失败不打扰用户
  }
}

async function handleCommand(command) {
  if (command === 'password') {
    Object.assign(pwdForm, { oldPassword: '', newPassword: '', confirmPassword: '' })
    pwdVisible.value = true
  } else if (command === 'logout') {
    await userStore.logout()
    router.push('/login')
  }
}

async function submitPassword() {
  if (!pwdForm.oldPassword) {
    ElMessage.warning('请输入原密码')
    return
  }
  if (!/^(?=.*[A-Za-z])(?=.*\d)[\x21-\x7E]{8,32}$/.test(pwdForm.newPassword)) {
    ElMessage.warning('新密码需为 8-32 位，且同时包含字母和数字')
    return
  }
  if (pwdForm.newPassword !== pwdForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  await changePassword(pwdForm.oldPassword, pwdForm.newPassword)
  pwdVisible.value = false
  ElMessage.success('密码修改成功，请重新登录')
  userStore.logoutLocal()
  router.push('/login')
}

function goActivities() {
  router.push('/activities')
}

function reminderTag(item) {
  const now = new Date()
  const time = new Date(String(item.nextFollowTime).replace(' ', 'T'))
  if (time < now) return { type: 'danger', text: '已逾期' }
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const tomorrow = new Date(today.getTime() + 86400000)
  if (time < tomorrow) return { type: 'warning', text: '今日' }
  return { type: 'info', text: '即将' }
}

onMounted(() => {
  loadReminders()
  pollTimer = setInterval(loadReminders, 5 * 60 * 1000)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<template>
  <el-container class="layout">
    <el-aside width="210px" class="aside">
      <div class="logo">
        <el-icon><DataLine /></el-icon>
        <span>CRM 管理系统</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="#001529"
        text-color="#ffffffb3"
        active-text-color="#fff"
      >
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-title">{{ route.meta.title || 'CRM' }}</div>
        <div class="header-right">
          <el-popover placement="bottom" :width="380" trigger="click">
            <template #reference>
              <el-badge :value="badgeCount" :hidden="badgeCount === 0" class="bell-badge">
                <el-icon class="bell"><Bell /></el-icon>
              </el-badge>
            </template>
            <div class="reminder-panel">
              <div class="reminder-summary">
                逾期 <b class="danger">{{ reminders.overdueCount }}</b>
                · 今日 <b class="warning">{{ reminders.todayCount }}</b>
                · 3 天内 <b>{{ reminders.upcomingCount }}</b>
              </div>
              <el-empty v-if="!reminders.items.length" description="暂无待办提醒" :image-size="56" />
              <div
                v-for="item in reminders.items"
                :key="item.id"
                class="reminder-item"
                @click="goActivities"
              >
                <el-tag size="small" :type="reminderTag(item).type">{{ reminderTag(item).text }}</el-tag>
                <div class="reminder-main">
                  <div class="reminder-content">
                    {{ item.customerName || '未关联客户' }} · {{ item.content }}
                  </div>
                  <div class="reminder-time">{{ item.nextFollowTime }}</div>
                </div>
              </div>
              <el-button link type="primary" style="margin-top: 8px" @click="goActivities">
                查看全部待办
              </el-button>
            </div>
          </el-popover>
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" style="vertical-align: middle">
                {{ userStore.userInfo?.realName?.charAt(0) || 'U' }}
              </el-avatar>
              <span class="username">{{ userStore.userInfo?.realName || userStore.userInfo?.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="password">修改密码</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>

    <el-dialog v-model="pwdVisible" title="修改密码" width="440px">
      <el-form label-width="90px">
        <el-form-item label="原密码" required>
          <el-input v-model="pwdForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" required>
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="8-32 位，含字母和数字" />
        </el-form-item>
        <el-form-item label="确认密码" required>
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPassword">确定</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<style scoped>
.layout { height: 100vh; }
.aside {
  background: #001529;
  overflow: hidden;
}
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  gap: 8px;
}
.logo .el-icon { font-size: 22px; }
:deep(.el-menu) { border-right: none; }
.header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #eee;
  padding: 0 24px;
}
.header-title { font-size: 18px; font-weight: 600; color: #333; }
.header-right { display: flex; align-items: center; gap: 22px; }
.bell-badge { cursor: pointer; line-height: 1; }
.bell { font-size: 20px; color: #606266; }
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: #666;
}
.username { font-size: 14px; }
.main { background: #f5f7fa; padding: 20px; }
.reminder-panel { max-height: 320px; overflow-y: auto; }
.reminder-summary { font-size: 13px; color: #666; padding-bottom: 8px; border-bottom: 1px solid #eee; }
.reminder-summary .danger { color: #f56c6c; }
.reminder-summary .warning { color: #e6a23c; }
.reminder-item {
  display: flex;
  gap: 8px;
  padding: 10px 0;
  border-bottom: 1px dashed #f0f0f0;
  cursor: pointer;
}
.reminder-item:hover { background: #fafafa; }
.reminder-main { flex: 1; min-width: 0; }
.reminder-content {
  font-size: 13px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.reminder-time { font-size: 12px; color: #999; margin-top: 2px; }
</style>
