<script setup>
import { ref, onMounted } from 'vue'
import { getTodoActivities } from '@/api/activity'

const list = ref([])
const loading = ref(false)

const activityTypeMap = { CALL: '电话', MEETING: '会议', EMAIL: '邮件', VISIT: '拜访', OTHER: '其他' }

async function loadData() {
  loading.value = true
  try {
    const res = await getTodoActivities()
    list.value = res.data
  } finally {
    loading.value = false
  }
}

function followTag(value) {
  const now = new Date()
  const time = new Date(String(value).replace(' ', 'T'))
  if (time < now) return { type: 'danger', text: '已逾期' }
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  if (time < new Date(today.getTime() + 86400000)) return { type: 'warning', text: '今日' }
  return { type: 'info', text: '即将' }
}

onMounted(loadData)
</script>

<template>
  <el-card>
    <template #header><span>待办跟进事项</span></template>
    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column label="类型" width="100">
        <template #default="{ row }">
          <el-tag>{{ activityTypeMap[row.type] || row.type }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="customerName" label="客户" width="160" show-overflow-tooltip>
        <template #default="{ row }">{{ row.customerName || '-' }}</template>
      </el-table-column>
      <el-table-column prop="content" label="跟进内容" min-width="300" show-overflow-tooltip />
      <el-table-column prop="activityTime" label="活动时间" width="170" />
      <el-table-column label="下次跟进时间" width="200">
        <template #default="{ row }">
          <span style="margin-right: 8px">{{ row.nextFollowTime }}</span>
          <el-tag v-if="row.nextFollowTime" size="small" :type="followTag(row.nextFollowTime).type">
            {{ followTag(row.nextFollowTime).text }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && list.length === 0" description="暂无待办跟进" />
  </el-card>
</template>
