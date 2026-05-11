<template>
  <view class="container">
    <view class="profile">
      <text class="name">游客</text>
      <button size="mini" type="primary">登录</button>
    </view>
  </view>
</template>

<style lang="scss">
.container { padding: 30rpx; }
.profile {
  background: #fff;
  padding: 40rpx;
  border-radius: 20rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.name { font-size: 32rpx; font-weight: bold; }
</style>
