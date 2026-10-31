<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getOperationLogs, getLoginLogs } from '@/api/audit'

const activeTab = ref('operations')
const loading = ref(false)
const operationList = ref([])
const operationTotal = ref(0)
const loginList = ref([])
const loginTotal = ref(0)

const operationQuery = reactive({
  username: '', module: '', success: null, page: 1, size: 10, startDate: null, endDate: null
})
const loginQuery = reactive({
  username: '', success: null, page: 1, size: 10, startDate: null, endDate: null
})

const modules = ['认证', '客户', '商机', '联系人', '跟进', '用户']

async function loadOperations() {
  loading.value = true
  try {
    const res = await getOperationLogs(operationQuery)
    operationList.value = res.data.records
    operationTotal.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function loadLogins() {
  loading.value = true
  try {
    const res = await getLoginLogs(loginQuery)
    loginList.value = res.data.records
    loginTotal.value = res.data.total
  } finally {
    loading.value = false
  }
}

function searchOperations() {
  operationQuery.page = 1
  loadOperations()
}

function searchLogins() {
  loginQuery.page = 1
  loadLogins()
}

function resetOperations() {
  Object.assign(operationQuery, {
    username: '', module: '', success: null, page: 1, size: 10, startDate: null, endDate: null
  })
  loadOperations()
}

function resetLogins() {
  Object.assign(loginQuery, {
    username: '', success: null, page: 1, size: 10, startDate: null, endDate: null
  })
  loadLogins()
}

function onTabChange(name) {
  if (name === 'operations') loadOperations()
  else loadLogins()
}

onMounted(loadOperations)
</script>

<template>
  <el-card>
    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <el-tab-pane label="操作日志" name="operations">
        <el-form :inline="true" class="search-form">
          <el-form-item label="操作人">
            <el-input v-model="operationQuery.username" placeholder="用户名" clearable @keyup.enter="searchOperations" />
          </el-form-item>
          <el-form-item label="模块">
            <el-select v-model="operationQuery.module" placeholder="全部" clearable style="width: 120px">
              <el-option v-for="m in modules" :key="m" :label="m" :value="m" />
            </el-select>
          </el-form-item>
          <el-form-item label="结果">
            <el-select v-model="operationQuery.success" placeholder="全部" clearable style="width: 100px">
              <el-option label="成功" :value="1" />
              <el-option label="失败" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item label="时间">
            <el-date-picker
              v-model="operationQuery.startDate"
              type="date"
              placeholder="开始日期"
              value-format="YYYY-MM-DD"
              style="width: 140px"
            />
            <span style="margin: 0 6px">-</span>
            <el-date-picker
              v-model="operationQuery.endDate"
              type="date"
              placeholder="结束日期"
              value-format="YYYY-MM-DD"
              style="width: 140px"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="searchOperations">查询</el-button>
            <el-button @click="resetOperations">重置</el-button>
          </el-form-item>
        </el-form>

        <el-table :data="operationList" v-loading="loading" stripe>
          <el-table-column prop="createTime" label="时间" width="170" />
          <el-table-column prop="username" label="操作人" width="120" />
          <el-table-column prop="module" label="模块" width="100" />
          <el-table-column prop="action" label="动作" width="140" />
          <el-table-column prop="method" label="方法" width="80" />
          <el-table-column prop="uri" label="接口" min-width="200" show-overflow-tooltip />
          <el-table-column prop="ip" label="IP" width="140" />
          <el-table-column prop="params" label="参数" min-width="260" show-overflow-tooltip />
          <el-table-column label="结果" width="80">
            <template #default="{ row }">
              <el-tag :type="row.success === 1 ? 'success' : 'danger'">
                {{ row.success === 1 ? '成功' : '失败' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="costMs" label="耗时(ms)" width="100" />
          <el-table-column prop="errorMsg" label="错误信息" min-width="180" show-overflow-tooltip />
        </el-table>
        <el-pagination
          style="margin-top: 16px; justify-content: flex-end; display: flex"
          v-model:current-page="operationQuery.page"
          v-model:page-size="operationQuery.size"
          :total="operationTotal"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadOperations"
          @current-change="loadOperations"
        />
      </el-tab-pane>

      <el-tab-pane label="登录日志" name="logins">
        <el-form :inline="true" class="search-form">
          <el-form-item label="用户名">
            <el-input v-model="loginQuery.username" placeholder="用户名" clearable @keyup.enter="searchLogins" />
          </el-form-item>
          <el-form-item label="结果">
            <el-select v-model="loginQuery.success" placeholder="全部" clearable style="width: 100px">
              <el-option label="成功" :value="1" />
              <el-option label="失败" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item label="时间">
            <el-date-picker
              v-model="loginQuery.startDate"
              type="date"
              placeholder="开始日期"
              value-format="YYYY-MM-DD"
              style="width: 140px"
            />
            <span style="margin: 0 6px">-</span>
            <el-date-picker
              v-model="loginQuery.endDate"
              type="date"
              placeholder="结束日期"
              value-format="YYYY-MM-DD"
              style="width: 140px"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="searchLogins">查询</el-button>
            <el-button @click="resetLogins">重置</el-button>
          </el-form-item>
        </el-form>

        <el-table :data="loginList" v-loading="loading" stripe>
          <el-table-column prop="createTime" label="时间" width="170" />
          <el-table-column prop="username" label="用户名" width="140" />
          <el-table-column prop="ip" label="IP" width="150" />
          <el-table-column prop="userAgent" label="User-Agent" min-width="280" show-overflow-tooltip />
          <el-table-column label="结果" width="90">
            <template #default="{ row }">
              <el-tag :type="row.success === 1 ? 'success' : 'danger'">
                {{ row.success === 1 ? '成功' : '失败' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="message" label="说明" min-width="160" show-overflow-tooltip />
        </el-table>
        <el-pagination
          style="margin-top: 16px; justify-content: flex-end; display: flex"
          v-model:current-page="loginQuery.page"
          v-model:page-size="loginQuery.size"
          :total="loginTotal"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadLogins"
          @current-change="loadLogins"
        />
      </el-tab-pane>
    </el-tabs>
  </el-card>
</template>

<style scoped>
.search-form { margin-bottom: 0; }
</style>
