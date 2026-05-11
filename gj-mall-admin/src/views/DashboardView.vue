<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { TableColumnsType } from 'ant-design-vue'
import {
  getDashboardBusiness,
  type DashboardBusiness,
  type DashboardLatestOrderItem,
  type DashboardLowStockItem,
} from '@/api/dashboard'

const router = useRouter()
const loading = ref(false)

const dashboard = ref<DashboardBusiness>(createEmptyDashboard())

const lowStockColumns: TableColumnsType<DashboardLowStockItem> = [
  { title: '商品 / SKU', dataIndex: 'spuName', key: 'product' },
  { title: '库存', dataIndex: 'stock', key: 'stock', width: 150 },
  { title: '预警差额', dataIndex: 'alertGap', key: 'alertGap', width: 110 },
]

const latestOrderColumns: TableColumnsType<DashboardLatestOrderItem> = [
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo' },
  { title: '用户', dataIndex: 'userId', key: 'userId', width: 110 },
  { title: '金额', dataIndex: 'payAmount', key: 'payAmount', width: 130 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 110 },
  { title: '下单时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
]

const metricCards = computed(() => [
  {
    label: '今日实收',
    value: formatMoney(dashboard.value.todayPaidAmount),
    sub: `累计 ${formatMoney(dashboard.value.paidAmountTotal)}`,
    tone: 'revenue',
  },
  {
    label: '今日订单',
    value: formatNumber(dashboard.value.todayOrderCount),
    sub: `总订单 ${formatNumber(dashboard.value.orderTotal)}`,
    tone: 'order',
  },
  {
    label: '待发货',
    value: formatNumber(dashboard.value.pendingDeliveryCount),
    sub: `金额 ${formatMoney(dashboard.value.pendingDeliveryAmount)}`,
    tone: 'delivery',
  },
  {
    label: '库存预警',
    value: formatNumber(dashboard.value.lowStockSkuCount),
    sub: `无库存 ${formatNumber(dashboard.value.emptyStockSkuCount)}`,
    tone: 'stock',
  },
])

const pendingCards = computed(() => [
  { label: '待付款', value: dashboard.value.pendingPayCount, route: 'Order' },
  { label: '待收货', value: dashboard.value.pendingReceiveCount, route: 'Order' },
  { label: '售后待处理', value: dashboard.value.pendingAfterSaleCount, route: 'AfterSale' },
  { label: '待审核评价', value: dashboard.value.pendingCommentCount, route: 'ProductComment' },
  { label: '有效优惠券', value: dashboard.value.activeCouponCount, route: 'MarketingCoupon' },
  { label: '进行中秒杀', value: dashboard.value.activeSeckillCount, route: 'MarketingSeckill' },
])

const maxTrendOrders = computed(() => {
  return Math.max(...dashboard.value.orderTrend.map((item) => Number(item.orderCount || 0)), 1)
})

onMounted(fetchDashboard)

async function fetchDashboard() {
  loading.value = true
  try {
    const res = await getDashboardBusiness()
    dashboard.value = {
      ...createEmptyDashboard(),
      ...(res.data || {}),
      orderTrend: res.data?.orderTrend || [],
      hotProducts: res.data?.hotProducts || [],
      lowStockSkus: res.data?.lowStockSkus || [],
      latestOrders: res.data?.latestOrders || [],
    }
  } finally {
    loading.value = false
  }
}

function createEmptyDashboard(): DashboardBusiness {
  return {
    productTotal: 0,
    productOnSale: 0,
    lowStockSkuCount: 0,
    emptyStockSkuCount: 0,
    brandTotal: 0,
    categoryTotal: 0,
    userTotal: 0,
    todayNewUsers: 0,
    orderTotal: 0,
    todayOrderCount: 0,
    pendingPayCount: 0,
    pendingDeliveryCount: 0,
    pendingReceiveCount: 0,
    completedCount: 0,
    canceledCount: 0,
    refundingCount: 0,
    refundedCount: 0,
    paidAmountTotal: 0,
    todayPaidAmount: 0,
    pendingDeliveryAmount: 0,
    pendingAfterSaleCount: 0,
    pendingCommentCount: 0,
    activeCouponCount: 0,
    activeSeckillCount: 0,
    orderTrend: [],
    hotProducts: [],
    lowStockSkus: [],
    latestOrders: [],
  }
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

function trendBarStyle(value?: number) {
  const percent = Math.max((Number(value || 0) / maxTrendOrders.value) * 100, Number(value || 0) > 0 ? 8 : 0)
  return { width: `${percent}%` }
}

function formatDate(date?: string) {
  if (!date) {
    return '--'
  }
  return date.slice(5)
}

function statusColor(status?: number) {
  const map: Record<number, string> = {
    0: 'warning',
    1: 'processing',
    2: 'blue',
    3: 'success',
    4: 'default',
    5: 'purple',
    6: 'default',
  }
  return status === undefined || status === null ? 'default' : map[status] || 'default'
}

function openModule(name: string) {
  router.push({ name })
}
</script>

<template>
  <a-spin :spinning="loading">
    <div class="dashboard-page">
      <div class="metric-grid">
        <div v-for="item in metricCards" :key="item.label" class="metric-card" :class="`tone-${item.tone}`">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <em>{{ item.sub }}</em>
        </div>
      </div>

      <div class="dashboard-grid">
        <section class="dashboard-panel">
          <div class="panel-header">
            <h3>7 日订单趋势</h3>
            <a-button type="link" @click="openModule('Order')">订单管理</a-button>
          </div>
          <div class="trend-list">
            <div v-for="item in dashboard.orderTrend" :key="item.date" class="trend-row">
              <span>{{ formatDate(item.date) }}</span>
              <div class="trend-track">
                <div class="trend-bar" :style="trendBarStyle(item.orderCount)" />
              </div>
              <strong>{{ formatNumber(item.orderCount) }}</strong>
              <em>{{ formatMoney(item.paidAmount) }}</em>
            </div>
          </div>
        </section>

        <section class="dashboard-panel">
          <div class="panel-header">
            <h3>待处理事项</h3>
          </div>
          <div class="pending-grid">
            <button
              v-for="item in pendingCards"
              :key="item.label"
              type="button"
              class="pending-item"
              @click="openModule(item.route)"
            >
              <span>{{ item.label }}</span>
              <strong>{{ formatNumber(item.value) }}</strong>
            </button>
          </div>
          <div class="product-strip">
            <span>商品 {{ formatNumber(dashboard.productOnSale) }} / {{ formatNumber(dashboard.productTotal) }}</span>
            <span>品牌 {{ formatNumber(dashboard.brandTotal) }}</span>
            <span>分类 {{ formatNumber(dashboard.categoryTotal) }}</span>
            <span>用户 {{ formatNumber(dashboard.userTotal) }}</span>
          </div>
        </section>
      </div>

      <div class="dashboard-grid lower-grid">
        <section class="dashboard-panel">
          <div class="panel-header">
            <h3>热销商品</h3>
            <a-button type="link" @click="openModule('ProductOperation')">商品运营</a-button>
          </div>
          <div class="hot-list">
            <div v-for="item in dashboard.hotProducts" :key="item.id" class="hot-item">
              <img v-if="item.mainImage" :src="item.mainImage" alt="商品图" />
              <div v-else class="hot-image-empty">无图</div>
              <div class="hot-content">
                <strong>{{ item.name }}</strong>
                <span>{{ formatMoney(item.price) }} / 销量 {{ formatNumber(item.saleCount) }}</span>
              </div>
            </div>
          </div>
        </section>

        <section class="dashboard-panel">
          <div class="panel-header">
            <h3>库存预警</h3>
            <a-button type="link" @click="openModule('ProductInventory')">库存管理</a-button>
          </div>
          <a-table
            row-key="skuId"
            size="small"
            :columns="lowStockColumns"
            :data-source="dashboard.lowStockSkus"
            :pagination="false"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'product'">
                <div class="table-title">{{ record.spuName || '--' }}</div>
                <div class="table-muted">{{ record.skuName || record.skuCode || '--' }}</div>
              </template>
              <template v-else-if="column.key === 'stock'">
                <a-tag color="warning">可用 {{ record.stock || 0 }}</a-tag>
                <div class="table-muted">预警 {{ record.warnStock || 0 }}</div>
              </template>
              <template v-else-if="column.key === 'alertGap'">
                <strong class="warning-text">{{ record.alertGap || 0 }}</strong>
              </template>
            </template>
          </a-table>
        </section>
      </div>

      <section class="dashboard-panel">
        <div class="panel-header">
          <h3>最近订单</h3>
          <a-button type="link" @click="openModule('Order')">查看全部</a-button>
        </div>
        <a-table
          row-key="id"
          size="small"
          :columns="latestOrderColumns"
          :data-source="dashboard.latestOrders"
          :pagination="false"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'orderNo'">
              <span class="table-title">{{ record.orderNo }}</span>
            </template>
            <template v-else-if="column.key === 'userId'">
              ID {{ record.userId }}
            </template>
            <template v-else-if="column.key === 'payAmount'">
              {{ formatMoney(record.payAmount) }}
            </template>
            <template v-else-if="column.key === 'status'">
              <a-tag :color="statusColor(record.status)">
                {{ record.statusDesc || '未知' }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'createTime'">
              {{ record.createTime || '--' }}
            </template>
          </template>
        </a-table>
      </section>
    </div>
  </a-spin>
</template>

<style scoped>
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.metric-grid,
.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.dashboard-grid {
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 1fr);
}

.metric-card,
.dashboard-panel {
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  background: #fff;
}

.metric-card {
  min-height: 112px;
  padding: 16px;
  border-left: 4px solid #1677ff;
}

.metric-card span,
.metric-card em {
  display: block;
  color: #8c8c8c;
  font-style: normal;
  font-size: 12px;
}

.metric-card strong {
  display: block;
  margin: 10px 0 8px;
  color: #1f1f1f;
  font-size: 26px;
  line-height: 1.1;
}

.tone-revenue {
  border-left-color: #389e0d;
}

.tone-order {
  border-left-color: #0958d9;
}

.tone-delivery {
  border-left-color: #d48806;
}

.tone-stock {
  border-left-color: #cf1322;
}

.dashboard-panel {
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
  color: #1f1f1f;
  font-size: 15px;
  font-weight: 600;
}

.trend-list {
  display: flex;
  flex-direction: column;
  gap: 11px;
}

.trend-row {
  display: grid;
  grid-template-columns: 54px minmax(120px, 1fr) 48px 100px;
  align-items: center;
  gap: 10px;
  color: #595959;
  font-size: 12px;
}

.trend-track {
  height: 8px;
  overflow: hidden;
  border-radius: 999px;
  background: #f0f0f0;
}

.trend-bar {
  height: 100%;
  border-radius: 999px;
  background: #1677ff;
}

.trend-row strong {
  color: #1f1f1f;
}

.trend-row em {
  color: #8c8c8c;
  font-style: normal;
  text-align: right;
}

.pending-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.pending-item {
  min-height: 78px;
  padding: 12px;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  background: #fafafa;
  text-align: left;
  cursor: pointer;
}

.pending-item:hover {
  border-color: #1677ff;
  background: #f5f9ff;
}

.pending-item span {
  display: block;
  color: #8c8c8c;
  font-size: 12px;
}

.pending-item strong {
  display: block;
  margin-top: 8px;
  color: #1f1f1f;
  font-size: 22px;
}

.product-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.product-strip span {
  padding: 4px 9px;
  border-radius: 999px;
  background: #f5f5f5;
  color: #595959;
  font-size: 12px;
}

.lower-grid {
  grid-template-columns: minmax(0, 0.9fr) minmax(0, 1.1fr);
}

.hot-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.hot-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hot-item img,
.hot-image-empty {
  width: 48px;
  height: 48px;
  flex: 0 0 48px;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  object-fit: cover;
}

.hot-image-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  color: #999;
  font-size: 12px;
}

.hot-content {
  min-width: 0;
}

.hot-content strong,
.table-title {
  display: block;
  overflow: hidden;
  color: #1f1f1f;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hot-content span,
.table-muted {
  display: block;
  margin-top: 4px;
  color: #8c8c8c;
  font-size: 12px;
}

.warning-text {
  color: #d48806;
}

@media (max-width: 1200px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .dashboard-grid,
  .lower-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .metric-grid,
  .pending-grid {
    grid-template-columns: 1fr;
  }

  .trend-row {
    grid-template-columns: 48px minmax(80px, 1fr) 36px;
  }

  .trend-row em {
    display: none;
  }
}
</style>
