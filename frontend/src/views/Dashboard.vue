<script setup>
import { ref, onMounted, shallowRef } from 'vue'
import * as echarts from 'echarts'
import { getDashboardStats } from '@/api/dashboard'

const stats = ref(null)
const funnelChart = shallowRef(null)
const trendChart = shallowRef(null)
const rankingChart = shallowRef(null)

const stageMap = {
  LEAD: '线索确认', REQUIREMENT: '需求分析', QUOTATION: '方案报价',
  NEGOTIATION: '商务谈判', WON: '赢单', LOST: '输单'
}

async function loadStats() {
  const res = await getDashboardStats()
  stats.value = res.data
  renderCharts()
}

function renderCharts() {
  const data = stats.value
  if (!data) return

  // 阶段分布漏斗图
  const funnelData = data.stageDistribution.map(s => ({
    name: stageMap[s.stage] || s.stage,
    value: s.count
  }))
  if (funnelChart.value) funnelChart.value.dispose()
  funnelChart.value = echarts.init(document.getElementById('funnelChart'))
  funnelChart.value.setOption({
    tooltip: { trigger: 'item' },
    series: [{
      type: 'funnel',
      left: '10%',
      width: '80%',
      data: funnelData
    }]
  })

  // 活动趋势折线图
  if (trendChart.value) trendChart.value.dispose()
  trendChart.value = echarts.init(document.getElementById('trendChart'))
  trendChart.value.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: data.activityTrend.map(t => t.date) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{
      data: data.activityTrend.map(t => t.count),
      type: 'line',
      smooth: true,
      areaStyle: {}
    }]
  })

  // 负责人排行柱状图
  if (rankingChart.value) rankingChart.value.dispose()
  rankingChart.value = echarts.init(document.getElementById('rankingChart'))
  rankingChart.value.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: data.ownerRanking.map(r => r.ownerName || '用户' + r.owner_id) },
    series: [{
      type: 'bar',
      data: data.ownerRanking.map(r => r.total_amount)
    }]
  })
}

onMounted(loadStats)
</script>

<template>
  <div class="dashboard" v-loading="!stats">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon" style="background:#409EFF"><el-icon><User /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats?.customerTotal || 0 }}</div>
            <div class="stat-label">客户总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon" style="background:#67C23A"><el-icon><UserFilled /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats?.customerNewThisMonth || 0 }}</div>
            <div class="stat-label">本月新增客户</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon" style="background:#E6A23C"><el-icon><Money /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats?.opportunityTotal || 0 }}</div>
            <div class="stat-label">商机总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon" style="background:#F56C6C"><el-icon><Wallet /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">¥{{ Number(stats?.amountThisMonth || 0).toLocaleString() }}</div>
            <div class="stat-label">本月预计成交</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card>
          <template #header><span>商机阶段分布</span></template>
          <div id="funnelChart" style="height: 320px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><span>近 7 天跟进活动趋势</span></template>
          <div id="trendChart" style="height: 320px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="24">
        <el-card>
          <template #header><span>负责人商机金额排行</span></template>
          <div id="rankingChart" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.stat-card { display: flex; align-items: center; }
.stat-icon {
  width: 56px; height: 56px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 28px; margin-right: 16px;
}
.stat-value { font-size: 24px; font-weight: 700; color: #333; }
.stat-label { font-size: 14px; color: #999; margin-top: 4px; }
</style>
