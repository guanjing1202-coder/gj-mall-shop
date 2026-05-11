<template>
  <view class="container">
    <text>商品详情（开发中，spuId: {{ spuId }}）</text>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'

const spuId = ref<string>('')

onLoad((options: any) => {
  spuId.value = options?.id ?? ''
})
</script>

<style lang="scss">
.container { padding: 30rpx; }
</style>
