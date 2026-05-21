<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { TableColumnsType } from 'ant-design-vue'
import { downloadSalesReport, getSalesReport, type CategoryRankItem, type ProductRankItem, type SalesReport } from '@/api/report'
import {
  formatReportPeriod,
  normalizeReportGranularity,
  reportGranularityName,
  reportGranularityOptions,
  type ReportGranularity,
} from '@/utils/report-granularity'

const loading = ref(false)
const report = ref<SalesReport>(createEmptyReport())
const query = reactive({
  startDate: formatDate(offsetDate(-29)),
  endDate: formatDate(new Date()),
  granularity: 'day' as ReportGranularity,
})

const productColumns: TableColumnsType<ProductRankItem> = [
  { title: '商品', dataIndex: 'productName', key: 'productName' },
  { title: '销量', dataIndex: 'saleQuantity', key: 'saleQuantity', width: 100 },
  { title: '订单数', dataIndex: 'orderCount', key: 'orderCount', width: 100 },
  { title: '销售额', dataIndex: 'salesAmount', key: 'salesAmount', width: 140 },
  { title: '均价', dataIndex: 'averagePrice', key: 'averagePrice', width: 120 },
]

const categoryColumns: TableColumnsType<CategoryRankItem> = [
  { title: '分类', dataIndex: 'categoryName', key: 'categoryName' },
  { title: '销量', dataIndex: 'saleQuantity', key: 'saleQuantity', width: 110 },
  { title: '订单数', dataIndex: 'orderCount', key: 'orderCount', width: 110 },
  { title: '销售额', dataIndex: 'salesAmount', key: 'salesAmount', width: 140 },
]

const overviewCards = computed(() => {
  const overview = report.value.overview
  return [
    { label: '实收金额', value: formatMoney(overview.paidAmount), sub: `净收入 ${formatMoney(overview.netAmount)}`, tone: 'green' },
    { label: '订单数', value: formatNumber(overview.orderCount), sub: `支付订单 ${formatNumber(overview.paidOrderCount)}`, tone: 'blue' },
    { label: '客单价', value: formatMoney(overview.averageOrderAmount), sub: `转化率 ${formatPercent(overview.paidConversionRate)}`, tone: 'purple' },
    { label: '退款金额', value: formatMoney(overview.refundAmount), sub: `退款率 ${formatPercent(overview.refundRate)}`, tone: 'orange' },
    { label: '新增会员', value: formatNumber(overview.newMemberCount), sub: `${report.value.startDate} 至 ${report.value.endDate}`, tone: 'cyan' },
  ]
})

const maxPaidAmount = computed(() => {
  return Math.max(...report.value.salesTrend.map((item) => Number(item.paidAmount || 0)), 1)
})

const maxRefundAmount = computed(() => {
  return Math.max(...report.value.refundTrend.map((item) => Number(item.refundAmount || 0)), 1)
})

const maxMemberCount = computed(() => {
  return Math.max(...report.value.memberGrowth.map((item) => Number(item.newMemberCount || 0)), 1)
})

const trendSummary = computed(() => `${reportGranularityName(report.value.granularity)}统计，共 ${report.value.salesTrend.length} 个周期`)

onMounted(fetchReport)

async function fetchReport() {
  loading.value = true
  try {
    query.granularity = normalizeReportGranularity(query.granularity)
    const res = await getSalesReport(query)
    report.value = {
      ...createEmptyReport(),
      ...(res.data || {}),
      granularity: normalizeReportGranularity(res.data?.granularity),
      overview: {
        ...createEmptyReport().overview,
        ...(res.data?.overview || {}),
      },
      salesTrend: res.data?.salesTrend || [],
      productRanks: res.data?.productRanks || [],
      categoryRanks: res.data?.categoryRanks || [],
      refundTrend: res.data?.refundTrend || [],
      memberGrowth: res.data?.memberGrowth || [],
    }
  } finally {
    loading.value = false
  }
}

async function exportReport() {
  query.granularity = normalizeReportGranularity(query.granularity)
  await downloadSalesReport(query)
}

function useQuickRange(days: number) {
  query.endDate = formatDate(new Date())
  query.startDate = formatDate(offsetDate(-(days - 1)))
  fetchReport()
}

function handleGranularityChange() {
  fetchReport()
}

function createEmptyReport(): SalesReport {
  return {
    startDate: '',
    endDate: '',
    granularity: 'day',
    overview: {
      orderCount: 0,
      paidOrderCount: 0,
      refundOrderCount: 0,
      newMemberCount: 0,
      grossAmount: 0,
      paidAmount: 0,
      refundAmount: 0,
      netAmount: 0,
      averageOrderAmount: 0,
      paidConversionRate: 0,
      refundRate: 0,
    },
    salesTrend: [],
    productRanks: [],
    categoryRanks: [],
    refundTrend: [],
    memberGrowth: [],
  }
}

function offsetDate(offset: number) {
  const date = new Date()
  date.setDate(date.getDate() + offset)
  return date
}

function formatDate(date: Date) {
  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}

function formatShortDate(value?: string) {
  return formatReportPeriod(value, report.value.granularity)
}

function formatMoney(value?: number) {
  return `¥${Number(value || 0).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`
}

function formatNumber(value?: number) {
  return Number(value || 0).toLocaleString('zh-CN')
}

function formatPercent(value?: number) {
  return `${Number(value || 0).toFixed(2)}%`
}

function barStyle(value?: number, max = 1) {
  const numberValue = Number(value || 0)
  const width = numberValue <= 0 ? 0 : Math.max((numberValue / max) * 100, 6)
  return { height: `${width}%` }
}
</script>

<template>
  <a-spin :spinning="loading">
    <div class="report-page">
      <section class="report-toolbar">
        <div class="toolbar-title">
          <h2>经营报表</h2>
          <span>销售趋势、商品排行、退款分析和会员增长</span>
        </div>
        <div class="toolbar-actions">
          <a-space wrap>
            <a-button @click="useQuickRange(7)">近 7 天</a-button>
            <a-button @click="useQuickRange(30)">近 30 天</a-button>
            <a-button @click="useQuickRange(90)">近 90 天</a-button>
            <a-segmented
              v-model:value="query.granularity"
              :options="reportGranularityOptions"
              @change="handleGranularityChange"
            />
            <a-input v-model:value="query.startDate" class="date-input" />
            <span class="date-separator">至</span>
            <a-input v-model:value="query.endDate" class="date-input" />
            <a-button type="primary" @click="fetchReport">查询</a-button>
            <a-button @click="exportReport">导出 CSV</a-button>
          </a-space>
        </div>
      </section>

      <div class="overview-grid">
        <div v-for="item in overviewCards" :key="item.label" class="overview-card" :class="`tone-${item.tone}`">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <em>{{ item.sub }}</em>
        </div>
      </div>

      <section class="report-panel">
        <div class="panel-header">
          <h3>销售趋势</h3>
          <span>{{ report.startDate }} 至 {{ report.endDate }} · {{ trendSummary }}</span>
        </div>
        <div class="chart-bars">
          <div v-for="item in report.salesTrend" :key="item.date" class="chart-column">
            <div class="bar-track">
              <div class="bar-fill paid" :style="barStyle(item.paidAmount, maxPaidAmount)" />
            </div>
            <strong>{{ formatMoney(item.paidAmount) }}</strong>
            <span>{{ formatShortDate(item.date) }}</span>
          </div>
        </div>
      </section>

      <div class="report-grid">
        <section class="report-panel">
          <div class="panel-header">
            <h3>退款分析</h3>
            <span>退款金额与退款笔数</span>
          </div>
          <div class="mini-bars">
            <div v-for="item in report.refundTrend" :key="item.date" class="mini-row">
              <span>{{ formatShortDate(item.date) }}</span>
              <div class="mini-track">
                <div class="mini-fill refund" :style="{ width: `${Math.max((Number(item.refundAmount || 0) / maxRefundAmount) * 100, Number(item.refundAmount || 0) > 0 ? 6 : 0)}%` }" />
              </div>
              <strong>{{ formatMoney(item.refundAmount) }}</strong>
              <em>{{ formatNumber(item.refundCount) }} 笔</em>
            </div>
          </div>
        </section>

        <section class="report-panel">
          <div class="panel-header">
            <h3>会员增长</h3>
            <span>新增会员趋势</span>
          </div>
          <div class="mini-bars">
            <div v-for="item in report.memberGrowth" :key="item.date" class="mini-row">
              <span>{{ formatShortDate(item.date) }}</span>
              <div class="mini-track">
                <div class="mini-fill member" :style="{ width: `${Math.max((Number(item.newMemberCount || 0) / maxMemberCount) * 100, Number(item.newMemberCount || 0) > 0 ? 6 : 0)}%` }" />
              </div>
              <strong>{{ formatNumber(item.newMemberCount) }}</strong>
              <em>人</em>
            </div>
          </div>
        </section>
      </div>

      <div class="report-grid table-grid">
        <section class="report-panel">
          <div class="panel-header">
            <h3>商品销售排行</h3>
            <span>按销量排序</span>
          </div>
          <a-table row-key="spuId" size="small" :columns="productColumns" :data-source="report.productRanks" :pagination="false">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'productName'">
                <strong class="table-title">{{ record.productName || '--' }}</strong>
                <span class="table-muted">ID {{ record.spuId }}</span>
              </template>
              <template v-else-if="column.key === 'saleQuantity'">
                {{ formatNumber(record.saleQuantity) }}
              </template>
              <template v-else-if="column.key === 'orderCount'">
                {{ formatNumber(record.orderCount) }}
              </template>
              <template v-else-if="column.key === 'salesAmount'">
                {{ formatMoney(record.salesAmount) }}
              </template>
              <template v-else-if="column.key === 'averagePrice'">
                {{ formatMoney(record.averagePrice) }}
              </template>
            </template>
          </a-table>
        </section>

        <section class="report-panel">
          <div class="panel-header">
            <h3>分类销售排行</h3>
            <span>按销售额排序</span>
          </div>
          <a-table row-key="categoryName" size="small" :columns="categoryColumns" :data-source="report.categoryRanks" :pagination="false">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'categoryName'">
                <strong class="table-title">{{ record.categoryName || '未分类' }}</strong>
              </template>
              <template v-else-if="column.key === 'saleQuantity'">
                {{ formatNumber(record.saleQuantity) }}
              </template>
              <template v-else-if="column.key === 'orderCount'">
                {{ formatNumber(record.orderCount) }}
              </template>
              <template v-else-if="column.key === 'salesAmount'">
                {{ formatMoney(record.salesAmount) }}
              </template>
            </template>
          </a-table>
        </section>
      </div>
    </div>
  </a-spin>
</template>

<style scoped>
.report-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.report-toolbar,
.report-panel,
.overview-card {
  border: 1px solid #edf0f5;
  border-radius: 6px;
  background: #ffffff;
}

.report-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px;
}

.toolbar-title h2 {
  margin: 0 0 5px;
  color: #142033;
  font-size: 20px;
  font-weight: 800;
}

.toolbar-title span,
.panel-header span,
.overview-card span,
.overview-card em,
.table-muted {
  color: #7b8794;
  font-size: 12px;
}

.toolbar-actions {
  display: flex;
  justify-content: flex-end;
}

.date-input {
  width: 118px;
}

.date-separator {
  color: #7b8794;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
}

.overview-card {
  min-width: 0;
  padding: 15px;
  border-top: 4px solid #1677ff;
}

.overview-card strong {
  display: block;
  overflow: hidden;
  margin: 9px 0 7px;
  color: #142033;
  font-size: 24px;
  line-height: 1.1;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.overview-card em {
  display: block;
  overflow: hidden;
  font-style: normal;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tone-green {
  border-top-color: #389e0d;
}

.tone-blue {
  border-top-color: #1677ff;
}

.tone-purple {
  border-top-color: #722ed1;
}

.tone-orange {
  border-top-color: #d48806;
}

.tone-cyan {
  border-top-color: #08979c;
}

.report-panel {
  min-width: 0;
  padding: 16px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.panel-header h3 {
  margin: 0;
  color: #142033;
  font-size: 15px;
  font-weight: 700;
}

.chart-bars {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(24px, 1fr));
  align-items: end;
  gap: 8px;
  min-height: 260px;
}

.chart-column {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.bar-track {
  display: flex;
  align-items: flex-end;
  width: 100%;
  height: 180px;
  overflow: hidden;
  border-radius: 6px;
  background: #f2f5f8;
}

.bar-fill {
  width: 100%;
  min-height: 0;
  border-radius: 6px 6px 0 0;
  transition: height 0.2s ease;
}

.bar-fill.paid {
  background: #0f766e;
}

.chart-column strong {
  overflow: hidden;
  width: 100%;
  color: #364152;
  font-size: 11px;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chart-column span {
  color: #7b8794;
  font-size: 11px;
}

.report-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.table-grid {
  align-items: start;
}

.mini-bars {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 300px;
  overflow: auto;
  padding-right: 4px;
}

.mini-row {
  display: grid;
  grid-template-columns: 48px minmax(100px, 1fr) 108px 42px;
  align-items: center;
  gap: 10px;
  color: #5d6b7a;
  font-size: 12px;
}

.mini-track {
  height: 8px;
  overflow: hidden;
  border-radius: 999px;
  background: #f2f5f8;
}

.mini-fill {
  height: 100%;
  border-radius: 999px;
}

.mini-fill.refund {
  background: #d48806;
}

.mini-fill.member {
  background: #1677ff;
}

.mini-row strong {
  overflow: hidden;
  color: #142033;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mini-row em {
  color: #7b8794;
  font-style: normal;
}

.table-title {
  display: block;
  overflow: hidden;
  color: #142033;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.table-muted {
  display: block;
  margin-top: 4px;
}

@media (max-width: 1200px) {
  .report-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .report-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .overview-grid {
    grid-template-columns: 1fr;
  }

  .mini-row {
    grid-template-columns: 44px minmax(80px, 1fr) 86px;
  }

  .mini-row em {
    display: none;
  }
}
</style>
