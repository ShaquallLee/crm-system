<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)

const form = reactive({
  username: 'admin',
  password: 'admin123'
})

async function handleLogin() {
  loading.value = true
  try {
    await userStore.login(form)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (e) {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-box">
      <div class="login-header">
        <h2>CRM 客户管理系统</h2>
        <p>欢迎登录</p>
      </div>
      <el-form :model="form" @submit.prevent="handleLogin" class="login-form">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" size="large" :prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" size="large" :prefix-icon="Lock" show-password @keyup.enter="handleLogin" />
        </el-form-item>
        <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="handleLogin">
          登录
        </el-button>
      </el-form>
      <div class="login-tip">默认账号: admin / admin123</div>
    </div>
  </div>
</template>

<script>
import { User, Lock } from '@element-plus/icons-vue'
export default { components: { User, Lock } }
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-box {
  width: 380px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0,0,0,0.15);
}
.login-header {
  text-align: center;
  margin-bottom: 30px;
}
.login-header h2 { margin: 0 0 8px; color: #333; }
.login-header p { margin: 0; color: #999; font-size: 14px; }
.login-form { margin-bottom: 16px; }
.login-tip {
  text-align: center;
  color: #aaa;
  font-size: 12px;
}
</style>
