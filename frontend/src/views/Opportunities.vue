<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOpportunities, createOpportunity, updateOpportunity, deleteOpportunity } from '@/api/opportunity'
import { getCustomers } from '@/api/customer'
import { getOwnerOptions } from '@/api/user'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const editing = ref(null)
const customerOptions = ref([])
const ownerOptions = ref([])

const query = reactive({ name: '', stage: '', page: 1, size: 10 })
const form = reactive({
  id: null, customerId: null, name: '', stage: 'LEAD', amount: 0,
  expectedDate: '', probability: 0, ownerId: null, remark: ''
})

const stages = [
  { value: 'LEAD', label: '线索确认' },
  { value: 'REQUIREMENT', label: '需求分析' },
  { value: 'QUOTATION', label: '方案报价' },
  { value: 'NEGOTIATION', label: '商务谈判' },
  { value: 'WON', label: '赢单' },
  { value: 'LOST', label: '输单' }
]

async function loadData() {
  loading.value = true
  try {
    const res = await getOpportunities(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.page = 1; loadData() }

function handleAdd() {
  Object.assign(form, { id: null, customerId: null, name: '', stage: 'LEAD', amount: 0, expectedDate: '', probability: 0, ownerId: null, remark: '' })
  editing.value = null
  dialogVisible.value = true
}

function handleEdit(row) {
  Object.assign(form, row)
  editing.value = row
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.name) { ElMessage.warning('请输入商机名称'); return }
  if (!form.customerId) { ElMessage.warning('请选择客户'); return }
  if (editing.value) {
    await updateOpportunity(form)
    ElMessage.success('更新成功')
  } else {
    await createOpportunity(form)
    ElMessage.success('创建成功')
  }
  dialogVisible.value = false
  loadData()
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除商机「${row.name}」吗？`, '提示', { type: 'warning' })
  await deleteOpportunity(row.id)
  ElMessage.success('删除成功')
  loadData()
}

function stageLabel(s) { return stages.find(x => x.value === s)?.label || s }
function stageType(s) { return s === 'WON' ? 'success' : s === 'LOST' ? 'danger' : 'warning' }

async function loadOptions() {
  const [cRes, oRes] = await Promise.all([getCustomers({ size: 100 }), getOwnerOptions()])
  customerOptions.value = cRes.data.records || []
  ownerOptions.value = oRes.data || []
}

onMounted(() => {
  loadData()
  loadOptions()
})
</script>

<template>
  <div>
    <el-card>
      <el-form :inline="true">
        <el-form-item label="商机名称">
          <el-input v-model="query.name" placeholder="请输入" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="阶段">
          <el-select v-model="query.stage" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="s in stages" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="query.name=''; query.stage=''; handleSearch()">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top: 16px">
      <div style="margin-bottom: 16px">
        <el-button type="primary" @click="handleAdd">新增商机</el-button>
      </div>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="name" label="商机名称" min-width="160" />
        <el-table-column label="阶段" width="110">
          <template #default="{ row }">
            <el-tag :type="stageType(row.stage)">{{ stageLabel(row.stage) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="预计金额" width="140">
          <template #default="{ row }">¥{{ Number(row.amount || 0).toLocaleString() }}</template>
        </el-table-column>
        <el-table-column prop="expectedDate" label="预计成交" width="120" />
        <el-table-column label="赢单概率" width="100">
          <template #default="{ row }">{{ row.probability }}%</template>
        </el-table-column>
        <el-table-column label="客户" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.customerName || '-' }}</template>
        </el-table-column>
        <el-table-column label="负责人" width="110">
          <template #default="{ row }">{{ row.ownerName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑商机' : '新增商机'" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="商机名称" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="客户" required>
          <el-select v-model="form.customerId" filterable placeholder="请选择客户" style="width: 100%">
            <el-option
              v-for="c in customerOptions"
              :key="c.id"
              :label="c.name"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="负责人">
          <el-select v-model="form.ownerId" clearable placeholder="默认当前用户" style="width: 100%">
            <el-option
              v-for="u in ownerOptions"
              :key="u.id"
              :label="u.realName || u.username"
              :value="u.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="阶段">
          <el-select v-model="form.stage">
            <el-option v-for="s in stages" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="预计金额"><el-input-number v-model="form.amount" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="预计成交">
          <el-date-picker v-model="form.expectedDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="赢单概率"><el-slider v-model="form.probability" :max="100" show-input /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>
