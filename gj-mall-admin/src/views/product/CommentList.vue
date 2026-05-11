<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { CloseOutlined } from '@ant-design/icons-vue'
import type { TableColumnsType } from 'ant-design-vue'
import {
  approveComment,
  deleteComment,
  getCommentDetail,
  getCommentPage,
  hideComment,
  rejectComment,
  replyComment,
  showComment,
  type ApiId,
  type CommentQuery,
  type CommentActionPayload,
  type CommentRecord,
} from '@/api/comment'

type ActionMode = 'approve' | 'reject' | 'hide' | 'show'

interface CommentFilterState extends Omit<CommentQuery, 'hasImage' | 'hasReply'> {
  hasImage?: number
  hasReply?: number
}

const loading = ref(false)
const detailLoading = ref(false)
const actionLoading = ref(false)
const replyLoading = ref(false)
const comments = ref<CommentRecord[]>([])
const currentComment = ref<CommentRecord>()
const actionTarget = ref<CommentRecord>()
const detailOpen = ref(false)
const actionOpen = ref(false)
const replyOpen = ref(false)
const actionMode = ref<ActionMode>('approve')

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
})

const filters = reactive<CommentFilterState>({
  keyword: '',
  userId: undefined,
  spuId: undefined,
  score: undefined,
  status: undefined,
  hasImage: undefined,
  hasReply: undefined,
})

const actionForm = reactive({
  auditRemark: '',
})

const replyForm = reactive({
  replyContent: '',
})

const columns: TableColumnsType<CommentRecord> = [
  { title: '评价内容', dataIndex: 'content', key: 'content', width: 340 },
  { title: '商品 / SKU', dataIndex: 'spuName', key: 'product', width: 330 },
  { title: '会员', dataIndex: 'userId', key: 'user', width: 180 },
  { title: '评分', dataIndex: 'score', key: 'score', width: 150 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 140 },
  { title: '商家回复', dataIndex: 'replyContent', key: 'reply', width: 240 },
  { title: '时间', dataIndex: 'createTime', key: 'time', width: 180 },
  { title: '操作', key: 'action', fixed: 'right', width: 260 },
]

const statusOptions = [
  { label: '待审核', value: 0 },
  { label: '已通过', value: 1 },
  { label: '已驳回', value: 2 },
  { label: '已隐藏', value: 3 },
]

onMounted(fetchComments)

async function fetchComments() {
  loading.value = true
  try {
    const res = await getCommentPage({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      keyword: filters.keyword?.trim() || undefined,
      userId: normalizeId(filters.userId),
      spuId: normalizeId(filters.spuId),
      score: filters.score,
      status: filters.status,
      hasImage: toBooleanFilter(filters.hasImage),
      hasReply: toBooleanFilter(filters.hasReply),
    })
    comments.value = res.data.list
    pagination.total = Number(res.data.total || 0)
  } catch {
    comments.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

async function handleSearch() {
  pagination.current = 1
  await fetchComments()
}

async function handleReset() {
  filters.keyword = ''
  filters.userId = undefined
  filters.spuId = undefined
  filters.score = undefined
  filters.status = undefined
  filters.hasImage = undefined
  filters.hasReply = undefined
  pagination.current = 1
  await fetchComments()
}

async function handleTableChange(page: { current?: number; pageSize?: number }) {
  pagination.current = page.current || 1
  pagination.pageSize = page.pageSize || 10
  await fetchComments()
}

async function handleView(record: CommentRecord) {
  detailOpen.value = true
  detailLoading.value = true
  currentComment.value = record
  try {
    const res = await getCommentDetail(record.id)
    currentComment.value = res.data
  } catch {
    detailOpen.value = false
    currentComment.value = undefined
  } finally {
    detailLoading.value = false
  }
}

function closeDetail() {
  detailOpen.value = false
}

function openAction(mode: ActionMode, record: CommentRecord) {
  actionMode.value = mode
  actionTarget.value = record
  actionForm.auditRemark = ''
  actionOpen.value = true
}

async function submitAction() {
  if (!actionTarget.value) {
    return
  }
  const payload: CommentActionPayload = {
    auditRemark: normalizeOptional(actionForm.auditRemark),
  }
  actionLoading.value = true
  try {
    const id = actionTarget.value.id
    if (actionMode.value === 'approve') {
      await approveComment(id, payload)
      message.success('评价已通过')
    } else if (actionMode.value === 'reject') {
      await rejectComment(id, payload)
      message.success('评价已驳回')
    } else if (actionMode.value === 'hide') {
      await hideComment(id, payload)
      message.success('评价已隐藏')
    } else {
      await showComment(id, payload)
      message.success('评价已展示')
    }
    actionOpen.value = false
    await fetchComments()
    await refreshCurrent(id)
  } catch {
    // Error feedback is already handled by the shared request interceptor.
  } finally {
    actionLoading.value = false
  }
}

function openReply(record: CommentRecord) {
  actionTarget.value = record
  replyForm.replyContent = record.replyContent || ''
  replyOpen.value = true
}

async function submitReply() {
  if (!actionTarget.value) {
    return
  }
  const replyContent = replyForm.replyContent.trim()
  if (!replyContent) {
    message.warning('请输入回复内容')
    return
  }
  replyLoading.value = true
  try {
    await replyComment(actionTarget.value.id, { replyContent })
    message.success('回复已保存')
    replyOpen.value = false
    await fetchComments()
    await refreshCurrent(actionTarget.value.id)
  } catch {
    // Error feedback is already handled by the shared request interceptor.
  } finally {
    replyLoading.value = false
  }
}

function handleDelete(record: CommentRecord) {
  Modal.confirm({
    title: '确认删除这条评价吗？',
    content: record.content || '删除后评价会从后台列表中移除。',
    okButtonProps: { danger: true },
    async onOk() {
      try {
        await deleteComment(record.id)
        message.success('评价已删除')
        if (comments.value.length === 1 && pagination.current > 1) {
          pagination.current -= 1
        }
        await fetchComments()
        if (currentComment.value?.id === record.id) {
          detailOpen.value = false
          currentComment.value = undefined
        }
      } catch {
        // Error feedback is already handled by the shared request interceptor.
      }
    },
  })
}

async function refreshCurrent(id: ApiId) {
  if (currentComment.value?.id !== id) {
    return
  }
  try {
    const res = await getCommentDetail(id)
    currentComment.value = res.data
  } catch {
    detailOpen.value = false
    currentComment.value = undefined
  }
}

function actionTitle() {
  const map: Record<ActionMode, string> = {
    approve: '审核通过',
    reject: '驳回评价',
    hide: '隐藏评价',
    show: '展示评价',
  }
  return map[actionMode.value]
}

function statusColor(status?: number) {
  if (status === 0) {
    return 'processing'
  }
  if (status === 1) {
    return 'success'
  }
  if (status === 2) {
    return 'error'
  }
  return 'default'
}

function userText(record?: CommentRecord) {
  if (!record) {
    return '--'
  }
  return record.nickname || record.username || `会员 ${record.userId}`
}

function productImage(record?: CommentRecord) {
  return record?.skuImage || record?.mainImage || ''
}

function formatMoney(value?: number) {
  if (value === undefined || value === null) {
    return '--'
  }
  return `¥${Number(value).toFixed(2)}`
}

function formatSpecs(specs?: Record<string, string>) {
  if (!specs || !Object.keys(specs).length) {
    return '--'
  }
  return Object.entries(specs)
    .map(([key, value]) => `${key}: ${value}`)
    .join(' / ')
}

function normalizeId(value?: ApiId) {
  const text = String(value ?? '').trim()
  return text ? text : undefined
}

function normalizeOptional(value: string) {
  const text = value.trim()
  return text ? text : undefined
}

function toBooleanFilter(value?: number) {
  if (value === undefined || value === null) {
    return undefined
  }
  return value === 1
}

function toComment(record: Record<string, any>) {
  return record as CommentRecord
}
</script>

<template>
  <a-card title="评价/评论管理" :bordered="false">
    <a-form layout="inline" style="margin-bottom: 16px; row-gap: 12px">
      <a-form-item>
        <a-input
          v-model:value="filters.keyword"
          allow-clear
          placeholder="搜索评价、商品、会员、订单"
          style="width: 260px"
          @pressEnter="handleSearch"
        />
      </a-form-item>
      <a-form-item>
        <a-input v-model:value="filters.userId" allow-clear placeholder="会员 ID" style="width: 130px" />
      </a-form-item>
      <a-form-item>
        <a-input v-model:value="filters.spuId" allow-clear placeholder="SPU ID" style="width: 160px" />
      </a-form-item>
      <a-form-item>
        <a-select v-model:value="filters.score" allow-clear placeholder="评分" style="width: 120px">
          <a-select-option v-for="score in [5, 4, 3, 2, 1]" :key="score" :value="score">
            {{ score }} 星
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-select v-model:value="filters.status" allow-clear placeholder="状态" style="width: 130px">
          <a-select-option v-for="item in statusOptions" :key="item.value" :value="item.value">
            {{ item.label }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-select v-model:value="filters.hasImage" allow-clear placeholder="图片" style="width: 120px">
          <a-select-option :value="1">有图</a-select-option>
          <a-select-option :value="0">无图</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-select v-model:value="filters.hasReply" allow-clear placeholder="回复" style="width: 120px">
          <a-select-option :value="1">已回复</a-select-option>
          <a-select-option :value="0">未回复</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-space>
          <a-button type="primary" @click="handleSearch">查询</a-button>
          <a-button @click="handleReset">重置</a-button>
        </a-space>
      </a-form-item>
    </a-form>

    <a-table
      row-key="id"
      :columns="columns"
      :data-source="comments"
      :loading="loading"
      :pagination="pagination"
      :scroll="{ x: 1780 }"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'content'">
          <div class="comment-content">{{ record.content || '--' }}</div>
          <div class="comment-muted">
            {{ record.images?.length ? `${record.images.length} 张图片` : '无图评价' }}
          </div>
          <div v-if="record.auditRemark" class="comment-muted">备注：{{ record.auditRemark }}</div>
        </template>

        <template v-else-if="column.key === 'product'">
          <div class="comment-product">
            <img v-if="productImage(toComment(record))" :src="productImage(toComment(record))" alt="商品图" class="comment-thumb" />
            <div v-else class="comment-thumb comment-thumb-empty">无图</div>
            <div class="comment-product-text">
              <div class="comment-title">{{ record.spuName || '--' }}</div>
              <div class="comment-muted">{{ record.skuName || '--' }}</div>
              <div class="comment-muted">{{ formatSpecs(record.specData) }}</div>
              <div class="comment-id">SPU {{ record.spuId }} / SKU {{ record.skuId }}</div>
            </div>
          </div>
        </template>

        <template v-else-if="column.key === 'user'">
          <div>{{ userText(toComment(record)) }}</div>
          <div class="comment-muted">ID {{ record.userId }}</div>
          <div class="comment-muted">{{ record.phone || '--' }}</div>
        </template>

        <template v-else-if="column.key === 'score'">
          <a-rate :value="record.score || 0" disabled />
          <div class="comment-muted">{{ record.score || 0 }} 星</div>
        </template>

        <template v-else-if="column.key === 'status'">
          <a-tag :color="statusColor(record.status)">
            {{ record.statusDesc || '--' }}
          </a-tag>
          <div class="comment-muted" style="margin-top: 6px">
            {{ record.orderNo || '无订单号' }}
          </div>
        </template>

        <template v-else-if="column.key === 'reply'">
          <div class="comment-content">{{ record.replyContent || '--' }}</div>
          <div v-if="record.replyTime" class="comment-muted">{{ record.replyTime }}</div>
        </template>

        <template v-else-if="column.key === 'time'">
          <div>{{ record.createTime || '--' }}</div>
          <div class="comment-muted">更新 {{ record.updateTime || '--' }}</div>
        </template>

        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="handleView(toComment(record))">查看</a-button>
            <a-button v-if="record.status === 0" type="link" size="small" @click="openAction('approve', toComment(record))">通过</a-button>
            <a-button v-if="record.status === 0" type="link" danger size="small" @click="openAction('reject', toComment(record))">驳回</a-button>
            <a-button v-if="record.status === 1" type="link" size="small" @click="openAction('hide', toComment(record))">隐藏</a-button>
            <a-button v-if="record.status === 3" type="link" size="small" @click="openAction('show', toComment(record))">展示</a-button>
            <a-button type="link" size="small" @click="openReply(toComment(record))">回复</a-button>
            <a-button type="link" danger size="small" @click="handleDelete(toComment(record))">删除</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
  </a-card>

  <a-drawer v-model:open="detailOpen" width="860" destroy-on-close :closable="false">
    <template #title>
      <div class="comment-drawer-title">
        <a-button class="comment-drawer-close" type="text" size="small" aria-label="关闭评价详情" @click="closeDetail">
          <template #icon><CloseOutlined /></template>
        </a-button>
        <span>评价详情</span>
      </div>
    </template>
    <a-spin :spinning="detailLoading">
      <template v-if="currentComment">
        <a-descriptions :column="2" bordered size="small">
          <a-descriptions-item label="评价ID">{{ currentComment.id }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="statusColor(currentComment.status)">
              {{ currentComment.statusDesc || '--' }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="会员">{{ userText(currentComment) }}</a-descriptions-item>
          <a-descriptions-item label="手机号">{{ currentComment.phone || '--' }}</a-descriptions-item>
          <a-descriptions-item label="商品" :span="2">{{ currentComment.spuName || '--' }}</a-descriptions-item>
          <a-descriptions-item label="SKU">{{ currentComment.skuName || '--' }}</a-descriptions-item>
          <a-descriptions-item label="售价">{{ formatMoney(currentComment.skuPrice) }}</a-descriptions-item>
          <a-descriptions-item label="品牌">{{ currentComment.brandName || '--' }}</a-descriptions-item>
          <a-descriptions-item label="分类">{{ currentComment.categoryName || '--' }}</a-descriptions-item>
          <a-descriptions-item label="订单号">{{ currentComment.orderNo || '--' }}</a-descriptions-item>
          <a-descriptions-item label="订单项ID">{{ currentComment.orderItemId || '--' }}</a-descriptions-item>
          <a-descriptions-item label="评分">
            <a-rate :value="currentComment.score || 0" disabled />
          </a-descriptions-item>
          <a-descriptions-item label="规格">{{ formatSpecs(currentComment.specData) }}</a-descriptions-item>
          <a-descriptions-item label="评价内容" :span="2">{{ currentComment.content || '--' }}</a-descriptions-item>
          <a-descriptions-item label="审核备注" :span="2">{{ currentComment.auditRemark || '--' }}</a-descriptions-item>
          <a-descriptions-item label="商家回复" :span="2">{{ currentComment.replyContent || '--' }}</a-descriptions-item>
          <a-descriptions-item label="评价时间">{{ currentComment.createTime || '--' }}</a-descriptions-item>
          <a-descriptions-item label="回复时间">{{ currentComment.replyTime || '--' }}</a-descriptions-item>
        </a-descriptions>

        <div v-if="currentComment.images?.length" style="margin-top: 20px">
          <h4 style="margin-bottom: 12px">评价图片</h4>
          <a-space wrap>
            <img
              v-for="image in currentComment.images"
              :key="image"
              :src="image"
              alt="评价图片"
              style="width: 92px; height: 92px; border-radius: 6px; object-fit: cover; border: 1px solid #f0f0f0"
            />
          </a-space>
        </div>

        <div style="margin-top: 20px; display: flex; justify-content: flex-end">
          <a-space>
            <a-button v-if="currentComment.status === 0" @click="openAction('approve', currentComment)">审核通过</a-button>
            <a-button v-if="currentComment.status === 0" danger @click="openAction('reject', currentComment)">驳回</a-button>
            <a-button v-if="currentComment.status === 1" @click="openAction('hide', currentComment)">隐藏</a-button>
            <a-button v-if="currentComment.status === 3" @click="openAction('show', currentComment)">展示</a-button>
            <a-button type="primary" @click="openReply(currentComment)">回复</a-button>
          </a-space>
        </div>
      </template>
    </a-spin>
  </a-drawer>

  <a-modal
    v-model:open="actionOpen"
    :title="actionTitle()"
    :confirm-loading="actionLoading"
    @ok="submitAction"
  >
    <a-form layout="vertical">
      <a-form-item label="评价内容">
        <a-textarea :value="actionTarget?.content" disabled :rows="3" />
      </a-form-item>
      <a-form-item label="操作备注">
        <a-textarea
          v-model:value="actionForm.auditRemark"
          :maxlength="500"
          show-count
          :rows="4"
          placeholder="请输入审核或隐藏原因"
        />
      </a-form-item>
    </a-form>
  </a-modal>

  <a-modal
    v-model:open="replyOpen"
    title="商家回复"
    :confirm-loading="replyLoading"
    width="640px"
    @ok="submitReply"
  >
    <a-form layout="vertical">
      <a-form-item label="评价内容">
        <a-textarea :value="actionTarget?.content" disabled :rows="3" />
      </a-form-item>
      <a-form-item label="回复内容">
        <a-textarea
          v-model:value="replyForm.replyContent"
          :maxlength="1000"
          show-count
          :rows="5"
          placeholder="请输入商家回复"
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<style scoped>
.comment-product {
  display: flex;
  gap: 12px;
  align-items: center;
  min-width: 0;
}

.comment-thumb {
  width: 52px;
  height: 52px;
  flex: 0 0 52px;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  object-fit: cover;
}

.comment-thumb-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  color: #999;
  font-size: 12px;
}

.comment-product-text {
  min-width: 0;
}

.comment-title {
  color: #1f1f1f;
  font-weight: 600;
}

.comment-content {
  max-width: 320px;
  color: #1f1f1f;
  white-space: normal;
  word-break: break-word;
}

.comment-muted {
  margin-top: 4px;
  color: #8c8c8c;
  font-size: 12px;
}

.comment-id {
  margin-top: 3px;
  color: #bfbfbf;
  font-size: 12px;
  word-break: break-all;
}

.comment-drawer-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.comment-drawer-close {
  margin-left: -8px;
}
</style>
