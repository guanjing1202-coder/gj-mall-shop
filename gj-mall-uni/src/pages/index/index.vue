<template>
  <view class="container">
    <view class="banner">
      <text class="title">GJ Mall</text>
      <text class="subtitle">多端电商商城</text>
    </view>
    <view class="card">
      <text class="card-title">后端连通性</text>
      <text selectable>{{ pingResult }}</text>
      <button type="primary" @tap="checkPing">重新检查</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { request } from '@/utils/request'

const pingResult = ref<string>('checking...')

async function checkPing() {
  try {
    const res = await request<any>({ url: '/api/v1/ping', method: 'GET' })
    pingResult.value = JSON.stringify(res, null, 2)
  } catch (e: any) {
    pingResult.value = 'ERROR: ' + (e?.message || JSON.stringify(e))
  }
}

onMounted(checkPing)
</script>

<style lang="scss">
.container {
  min-height: 100vh;
  padding: 20rpx;
  background: #f5f5f5;
}
.banner {
  background: linear-gradient(135deg, #2979ff, #6699ff);
  padding: 60rpx 30rpx;
  border-radius: 20rpx;
  color: white;
  display: flex;
  flex-direction: column;
  margin-bottom: 20rpx;
}
.title { font-size: 48rpx; font-weight: bold; }
.subtitle { font-size: 28rpx; margin-top: 10rpx; opacity: 0.9; }
.card {
  background: #fff;
  padding: 30rpx;
  border-radius: 20rpx;
  display: flex;
  flex-direction: column;
}
.card-title { font-size: 32rpx; font-weight: bold; margin-bottom: 20rpx; }
</style>
