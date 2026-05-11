<script setup lang="ts">
import ShopHeader from '@/components/ShopHeader.vue'
</script>

<template>
  <div class="about-page">
    <ShopHeader />
    <main class="about-shell">
      <section class="about-hero">
        <span>GJ Mall Shop</span>
        <h1>多端电商商城项目</h1>
        <p>PC 商城、移动端、管理后台和 Spring Boot 后端已经在同一套商品、订单、营销数据上逐步打通。</p>
      </section>

      <section class="about-grid">
        <article>
          <strong>PC 商城</strong>
          <p>首页商品浏览、分类筛选、品牌筛选和商品详情已经接入公开商品接口。</p>
        </article>
        <article>
          <strong>管理后台</strong>
          <p>商品、库存、评价、订单、营销、售后、支付、物流等模块正在持续完善。</p>
        </article>
        <article>
          <strong>服务端</strong>
          <p>后端采用多模块单体结构，当前重点是把业务闭环和自检流程做扎实。</p>
        </article>
      </section>
    </main>
  </div>
</template>

<style scoped>
.about-page {
  min-height: 100vh;
  background: #f7f8f5;
  color: #111827;
}

.about-shell {
  max-width: 1040px;
  margin: 0 auto;
  padding: 56px 24px;
}

.about-hero {
  max-width: 720px;
}

.about-hero span {
  color: #2f8f67;
  font-size: 13px;
  font-weight: 900;
  text-transform: uppercase;
}

.about-hero h1 {
  margin: 14px 0 0;
  font-size: 48px;
  line-height: 1.1;
  letter-spacing: 0;
}

.about-hero p {
  margin: 18px 0 0;
  color: #4b5563;
  font-size: 17px;
  line-height: 1.8;
}

.about-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
  margin-top: 34px;
}

.about-grid article {
  min-height: 180px;
  padding: 24px;
  border: 1px solid rgba(17, 24, 39, 0.08);
  border-radius: 8px;
  background: #fff;
}

.about-grid strong {
  font-size: 20px;
}

.about-grid p {
  color: #6b7280;
  line-height: 1.8;
}

@media (max-width: 760px) {
  .about-shell {
    padding: 36px 16px;
  }

  .about-hero h1 {
    font-size: 34px;
  }

  .about-grid {
    grid-template-columns: 1fr;
  }
}
</style>
