<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getCustomers,
  createCustomer,
  updateCustomer,
  deleteCustomer,
  exportCustomers,
  downloadImportTemplate,
  importCustomers
} from '@/api/customer'
import { getOwnerOptions } from '@/api/user'

const router = useRouter()
const list = ref([])
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const editing = ref(null)
const saving = ref(false)
const ownerOptions = ref([])

const exporting = ref(false)
const importVisible = ref(false)
const importing = ref(false)
const importFile = ref(null)
const importResult = ref(null)
const uploadRef = ref(null)

const query = reactive({ name: '', industry: '', level: '', ownerId: null, page: 1, size: 10 })
const form = reactive({
  id: null, name: '', industry: '', level: 'C', source: '',
  ownerId: null, address: '', remark: ''
})

const levels = ['A', 'B', 'C', 'D']

async function loadData() {
  loading.value = true
  try {
    const res = await getCustomers(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function loadOwnerOptions() {
  const res = await getOwnerOptions()
  ownerOptions.value = res.data
}

function handleSearch() {
  query.page = 1
  loadData()
}

function resetQuery() {
  Object.assign(query, { name: '', industry: '', level: '', ownerId: null, page: 1 })
  loadData()
}

function handleAdd() {
  Object.assign(form, {
    id: null, name: '', industry: '', level: 'C', source: '',
    ownerId: null, address: '', remark: ''
  })
  editing.value = null
  dialogVisible.value = true
}

function handleEdit(row) {
  Object.assign(form, row)
  editing.value = row
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.name) {
    ElMessage.warning('请输入客户名称')
    return
  }
  saving.value = true
  try {
    if (editing.value) {
      await updateCustomer(form)
      ElMessage.success('更新成功')
    } else {
      await createCustomer(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除客户「${row.name}」吗？`, '提示', { type: 'warning' })
  await deleteCustomer(row.id)
  ElMessage.success('删除成功')
  loadData()
}

function goDetail(row) {
  router.push(`/customers/${row.id}`)
}

function downloadBlob(response, fallbackName) {
  const disposition = response.headers?.['content-disposition'] || ''
  let filename = fallbackName
  const match = disposition.match(/filename\*=UTF-8''([^;]+)/)
  if (match) {
    filename = decodeURIComponent(match[1])
  }
  const url = URL.createObjectURL(new Blob([response.data]))
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

async function handleExport() {
  exporting.value = true
  try {
    const res = await exportCustomers({
      name: query.name,
      industry: query.industry,
      level: query.level,
      ownerId: query.ownerId
    })
    downloadBlob(res, '客户数据.xlsx')
  } finally {
    exporting.value = false
  }
}

async function handleTemplate() {
  const res = await downloadImportTemplate()
  downloadBlob(res, '客户导入模板.xlsx')
}

function openImport() {
  importFile.value = null
  importResult.value = null
  importVisible.value = true
  uploadRef.value?.clearFiles()
}

function onFileChange(uploadFile) {
  importFile.value = uploadFile.raw
}

function onFileRemove() {
  importFile.value = null
}

async function submitImport() {
  if (!importFile.value) {
    ElMessage.warning('请先选择 Excel 文件')
    return
  }
  importing.value = true
  try {
    const res = await importCustomers(importFile.value)
    importResult.value = res.data
    ElMessage.success(`导入完成：成功 ${res.data.success} 条，失败 ${res.data.failed} 条`)
    if (res.data.success > 0) {
      loadData()
    }
  } finally {
    importing.value = false
  }
}

onMounted(() => {
  loadOwnerOptions()
  loadData()
})
</script>

<template>
  <div>
    <el-card>
      <el-form :inline="true" class="search-form">
        <el-form-item label="客户名称">
          <el-input v-model="query.name" placeholder="请输入" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="行业">
          <el-input v-model="query.industry" placeholder="请输入" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="级别">
          <el-select v-model="query.level" placeholder="全部" clearable style="width: 100px">
            <el-option v-for="l in levels" :key="l" :label="l" :value="l" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="ownerOptions.length > 1" label="负责人">
          <el-select v-model="query.ownerId" placeholder="全部" clearable style="width: 140px">
            <el-option
              v-for="u in ownerOptions"
              :key="u.id"
              :label="u.realName || u.username"
              :value="u.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top: 16px">
      <div class="toolbar">
        <div>
          <el-button type="primary" @click="handleAdd">新增客户</el-button>
          <el-button :loading="exporting" @click="handleExport">导出 Excel</el-button>
          <el-button @click="openImport">导入 Excel</el-button>
          <el-button link type="primary" @click="handleTemplate">下载导入模板</el-button>
        </div>
      </div>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="name" label="客户名称" min-width="160">
          <template #default="{ row }">
            <el-link type="primary" @click="goDetail(row)">{{ row.name }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="industry" label="行业" width="120" />
        <el-table-column prop="level" label="级别" width="80">
          <template #default="{ row }">
            <el-tag :type="row.level === 'A' ? 'danger' : row.level === 'B' ? 'warning' : 'info'">{{ row.level }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="source" label="来源" width="120" />
        <el-table-column prop="ownerName" label="负责人" width="110" />
        <el-table-column prop="address" label="地址" min-width="150" show-overflow-tooltip />
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

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑客户' : '新增客户'" width="600px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="客户名称" required>
          <el-input v-model="form.name" maxlength="128" />
        </el-form-item>
        <el-form-item label="行业">
          <el-input v-model="form.industry" maxlength="64" />
        </el-form-item>
        <el-form-item label="客户级别">
          <el-select v-model="form.level">
            <el-option v-for="l in levels" :key="l" :label="l" :value="l" />
          </el-select>
        </el-form-item>
        <el-form-item label="客户来源">
          <el-input v-model="form.source" maxlength="64" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-select v-model="form.ownerId" clearable placeholder="默认本人">
            <el-option
              v-for="u in ownerOptions"
              :key="u.id"
              :label="u.realName || u.username"
              :value="u.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.address" maxlength="255" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="importVisible" title="导入客户" width="560px">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="请使用下载的模板填写；客户名称必填，级别仅支持 A/B/C/D，单次最多 1000 条。"
        style="margin-bottom: 16px"
      />
      <el-upload
        ref="uploadRef"
        drag
        :auto-upload="false"
        :limit="1"
        accept=".xlsx,.xls"
        :on-change="onFileChange"
        :on-remove="onFileRemove"
        :on-exceed="() => ElMessage.warning('一次只能选择一个文件')"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">将 Excel 文件拖到此处，或<em>点击选择</em></div>
      </el-upload>

      <div v-if="importResult" class="import-result">
        <div>共 {{ importResult.total }} 条，成功 {{ importResult.success }} 条，失败 {{ importResult.failed }} 条。</div>
        <el-scrollbar v-if="importResult.errors.length" max-height="180px" style="margin-top: 8px">
          <div v-for="(err, idx) in importResult.errors" :key="idx" class="import-error">{{ err }}</div>
        </el-scrollbar>
      </div>

      <template #footer>
        <el-button @click="importVisible = false">关闭</el-button>
        <el-button type="primary" :loading="importing" @click="submitImport">开始导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.search-form { margin-bottom: 0; }
.toolbar { margin-bottom: 16px; }
.import-result { margin-top: 16px; font-size: 13px; color: #606266; }
.import-error { color: #f56c6c; line-height: 22px; }
</style>
