<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getUsers, getRoles, getOwnerOptions, createUser, updateUser, deleteUser, resetPassword
} from '@/api/user'

const list = ref([])
const total = ref(0)
const roles = ref([])
const userOptions = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const pwdVisible = ref(false)
const editing = ref(null)
const resetUserId = ref(null)
const resetUserLabel = ref('')
const newPassword = ref('')

const query = reactive({ username: '', roleId: null, page: 1, size: 10 })
const form = reactive({
  id: null, username: '', password: '', realName: '', phone: '', email: '',
  roleId: 2, managerId: null, status: 1
})

const managerOptions = computed(() =>
  userOptions.value.filter(u => [1, 2].includes(u.roleId) && u.id !== form.id)
)

function validPassword(password) {
  return /^(?=.*[A-Za-z])(?=.*\d)[\x21-\x7E]{8,32}$/.test(password)
}

async function loadData() {
  loading.value = true
  try {
    const res = await getUsers(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function loadRoles() {
  const res = await getRoles()
  roles.value = res.data
}

async function loadUserOptions() {
  const res = await getOwnerOptions()
  userOptions.value = res.data
}

function handleSearch() {
  query.page = 1
  loadData()
}

function handleAdd() {
  Object.assign(form, {
    id: null, username: '', password: '', realName: '', phone: '', email: '',
    roleId: 2, managerId: null, status: 1
  })
  editing.value = null
  dialogVisible.value = true
}

function handleEdit(row) {
  Object.assign(form, {
    id: row.id,
    username: row.username,
    password: '',
    realName: row.realName,
    phone: row.phone,
    email: row.email,
    roleId: row.roleId,
    managerId: row.managerId,
    status: row.status
  })
  editing.value = row
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.username) {
    ElMessage.warning('请输入用户名')
    return
  }
  if (!editing.value && !validPassword(form.password)) {
    ElMessage.warning('初始密码需为 8-32 位，且同时包含字母和数字')
    return
  }
  saving.value = true
  try {
    if (editing.value) {
      await updateUser(form)
      ElMessage.success('更新成功')
    } else {
      await createUser(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await Promise.all([loadData(), loadUserOptions()])
  } finally {
    saving.value = false
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除用户「${row.username}」吗？`, '提示', { type: 'warning' })
  await deleteUser(row.id)
  ElMessage.success('删除成功')
  await Promise.all([loadData(), loadUserOptions()])
}

function handleResetPwd(row) {
  resetUserId.value = row.id
  resetUserLabel.value = row.realName || row.username
  newPassword.value = ''
  pwdVisible.value = true
}

async function confirmResetPwd() {
  if (!validPassword(newPassword.value)) {
    ElMessage.warning('新密码需为 8-32 位，且同时包含字母和数字')
    return
  }
  await resetPassword(resetUserId.value, newPassword.value)
  ElMessage.success('密码重置成功，该用户需重新登录')
  pwdVisible.value = false
}

function roleName(id) {
  return roles.value.find(r => r.id === id)?.roleName || '-'
}

onMounted(() => {
  loadRoles()
  loadUserOptions()
  loadData()
})
</script>

<template>
  <div>
    <el-card>
      <el-form :inline="true">
        <el-form-item label="用户名">
          <el-input v-model="query.username" placeholder="请输入" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="query.roleId" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="r in roles" :key="r.id" :label="r.roleName" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="query.username=''; query.roleId=null; handleSearch()">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top: 16px">
      <div style="margin-bottom: 16px">
        <el-button type="primary" @click="handleAdd">新增用户</el-button>
      </div>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="realName" label="真实姓名" width="120" />
        <el-table-column label="角色" width="120">
          <template #default="{ row }">{{ roleName(row.roleId) }}</template>
        </el-table-column>
        <el-table-column prop="managerName" label="直属上级" width="120">
          <template #default="{ row }">{{ row.managerName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" @click="handleResetPwd(row)">重置密码</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        style="margin-top: 16px; justify-content: flex-end; display: flex"
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑用户' : '新增用户'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="用户名" required>
          <el-input v-model="form.username" :disabled="!!editing" maxlength="64" />
        </el-form-item>
        <el-form-item v-if="!editing" label="初始密码" required>
          <el-input v-model="form.password" type="password" show-password placeholder="8-32 位，含字母和数字" />
        </el-form-item>
        <el-form-item label="真实姓名"><el-input v-model="form.realName" maxlength="64" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" maxlength="20" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" maxlength="128" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleId">
            <el-option v-for="r in roles" :key="r.id" :label="r.roleName" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="直属上级">
          <el-select v-model="form.managerId" clearable placeholder="无">
            <el-option
              v-for="u in managerOptions"
              :key="u.id"
              :label="u.realName || u.username"
              :value="u.id"
            />
          </el-select>
          <div class="form-tip">仅管理员或销售经理可作为直属上级</div>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="pwdVisible" title="重置密码" width="440px">
      <el-form label-width="90px">
        <el-form-item label="用户">
          <span>{{ resetUserLabel }}</span>
        </el-form-item>
        <el-form-item label="新密码" required>
          <el-input v-model="newPassword" type="password" show-password placeholder="8-32 位，含字母和数字" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmResetPwd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.form-tip { font-size: 12px; color: #999; line-height: 18px; margin-top: 4px; }
</style>
