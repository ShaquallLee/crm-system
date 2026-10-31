<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCustomer } from '@/api/customer'
import { getContactsByCustomer, createContact, deleteContact } from '@/api/contact'
import { getOpportunities } from '@/api/opportunity'
import { getActivitiesByCustomer, createActivity } from '@/api/activity'

const route = useRoute()
const router = useRouter()
const customerId = route.params.id

const customer = ref(null)
const contacts = ref([])
const opportunities = ref([])
const activities = ref([])
const activeTab = ref('contacts')

const contactDialog = ref(false)
const contactForm = ref({ name: '', position: '', phone: '', email: '', isPrimary: 0, remark: '' })

const activityDialog = ref(false)
const activityForm = ref({ type: 'CALL', content: '', activityTime: '', nextFollowTime: '' })

const stageMap = { LEAD: '线索确认', REQUIREMENT: '需求分析', QUOTATION: '方案报价', NEGOTIATION: '商务谈判', WON: '赢单', LOST: '输单' }
const activityTypeMap = { CALL: '电话', MEETING: '会议', EMAIL: '邮件', VISIT: '拜访', OTHER: '其他' }

async function loadAll() {
  const [cRes, ctRes, opRes, acRes] = await Promise.all([
    getCustomer(customerId),
    getContactsByCustomer(customerId),
    getOpportunities({ customerId, size: 100 }),
    getActivitiesByCustomer(customerId)
  ])
  customer.value = cRes.data
  contacts.value = ctRes.data
  opportunities.value = opRes.data.records
  activities.value = acRes.data
}

async function handleSaveContact() {
  if (!contactForm.value.name) { ElMessage.warning('请输入联系人姓名'); return }
  await createContact({ ...contactForm.value, customerId })
  ElMessage.success('添加成功')
  contactDialog.value = false
  contactForm.value = { name: '', position: '', phone: '', email: '', isPrimary: 0, remark: '' }
  loadAll()
}

async function handleSaveActivity() {
  if (!activityForm.value.content) { ElMessage.warning('请输入跟进内容'); return }
  await createActivity({ ...activityForm.value, customerId })
  ElMessage.success('记录成功')
  activityDialog.value = false
  activityForm.value = { type: 'CALL', content: '', activityTime: '', nextFollowTime: '' }
  loadAll()
}

onMounted(loadAll)
</script>

<template>
  <div v-loading="!customer">
    <el-page-header @back="router.back()" :content="customer?.name" style="margin-bottom: 16px" />

    <el-card v-if="customer" style="margin-bottom: 16px">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="客户名称">{{ customer.name }}</el-descriptions-item>
        <el-descriptions-item label="行业">{{ customer.industry }}</el-descriptions-item>
        <el-descriptions-item label="级别">{{ customer.level }}</el-descriptions-item>
        <el-descriptions-item label="负责人">{{ customer.ownerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="来源">{{ customer.source }}</el-descriptions-item>
        <el-descriptions-item label="地址">{{ customer.address }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ customer.remark }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="联系人" name="contacts">
          <div style="margin-bottom: 12px">
            <el-button type="primary" size="small" @click="contactDialog = true">添加联系人</el-button>
          </div>
          <el-table :data="contacts" stripe>
            <el-table-column prop="name" label="姓名" width="120" />
            <el-table-column prop="position" label="职位" width="120" />
            <el-table-column prop="phone" label="电话" width="140" />
            <el-table-column prop="email" label="邮箱" min-width="180" />
            <el-table-column label="主联系人" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.isPrimary" type="success">是</el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" />
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="商机" name="opportunities">
          <el-table :data="opportunities" stripe>
            <el-table-column prop="name" label="商机名称" min-width="160" />
            <el-table-column label="阶段" width="120">
              <template #default="{ row }">
                <el-tag :type="row.stage === 'WON' ? 'success' : row.stage === 'LOST' ? 'danger' : 'warning'">
                  {{ stageMap[row.stage] }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="预计金额" width="140">
              <template #default="{ row }">¥{{ Number(row.amount || 0).toLocaleString() }}</template>
            </el-table-column>
            <el-table-column prop="expectedDate" label="预计成交" width="130" />
            <el-table-column label="赢单概率" width="100">
              <template #default="{ row }">{{ row.probability }}%</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="跟进记录" name="activities">
          <div style="margin-bottom: 12px">
            <el-button type="primary" size="small" @click="activityDialog = true">添加跟进</el-button>
          </div>
          <el-timeline v-if="activities.length">
            <el-timeline-item v-for="a in activities" :key="a.id" :timestamp="a.activityTime" placement="top">
              <el-card shadow="never">
                <div style="font-weight: 600; margin-bottom: 6px">
                  <el-tag size="small">{{ activityTypeMap[a.type] }}</el-tag>
                </div>
                <div>{{ a.content }}</div>
                <div v-if="a.nextFollowTime" style="margin-top: 6px; color: #E6A23C; font-size: 12px">
                  下次跟进：{{ a.nextFollowTime }}
                </div>
              </el-card>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无跟进记录" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="contactDialog" title="添加联系人" width="500px">
      <el-form :model="contactForm" label-width="80px">
        <el-form-item label="姓名" required><el-input v-model="contactForm.name" /></el-form-item>
        <el-form-item label="职位"><el-input v-model="contactForm.position" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="contactForm.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="contactForm.email" /></el-form-item>
        <el-form-item label="主联系人">
          <el-switch v-model="contactForm.isPrimary" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="contactForm.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="contactDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSaveContact">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="activityDialog" title="添加跟进记录" width="500px">
      <el-form :model="activityForm" label-width="90px">
        <el-form-item label="类型">
          <el-select v-model="activityForm.type">
            <el-option label="电话" value="CALL" />
            <el-option label="会议" value="MEETING" />
            <el-option label="邮件" value="EMAIL" />
            <el-option label="拜访" value="VISIT" />
          </el-select>
        </el-form-item>
        <el-form-item label="跟进内容" required>
          <el-input v-model="activityForm.content" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="活动时间">
          <el-date-picker v-model="activityForm.activityTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="下次跟进">
          <el-date-picker v-model="activityForm.nextFollowTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="activityDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSaveActivity">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>
