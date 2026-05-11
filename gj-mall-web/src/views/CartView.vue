<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import ShopHeader from '@/components/ShopHeader.vue'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'
import type { CartItem } from '@/api/cart'

const auth = useAuthStore()
const cart = useCartStore()
const router = useRouter()

const allSelected = computed({
  get: () => cart.hasItems && cart.items.every((item) => item.selected === 1 || item.invalid),
  set: (value: boolean) => cart.selectAll(value),
})

function formatPrice(value?: number) {
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    maximumFractionDigits: 2,
  }).format(Number(value || 0))
}

function normalizeImage(url?: string, seed = 'cart') {
  if (!url || url.includes('x.com/')) {
    return `https://picsum.photos/seed/${encodeURIComponent(seed)}/420/420`
  }
  return url
}

function handleImageError(event: Event, seed: string) {
  const target = event.target as HTMLImageElement
  target.src = `https://picsum.photos/seed/${encodeURIComponent(seed)}/420/420`
}

function specText(item: CartItem) {
  const values = item.specData ? Object.values(item.specData).filter(Boolean) : []
  return values.length ? values.join(' / ') : item.skuName || '默认规格'
}

async function toggleItem(item: CartItem, checked: string | number | boolean) {
  if (item.invalid) {
    return
  }
  await cart.updateItem(item.skuId, { selected: checked === true ? 1 : 0 })
}

async function updateQuantity(item: CartItem, quantity: number) {
  await cart.updateItem(item.skuId, { quantity })
}

async function removeItem(item: CartItem) {
  await cart.remove(item.skuId)
}

async function clearAll() {
  try {
    await ElMessageBox.confirm('清空后无法恢复，确认清空购物车？', '清空购物车', {
      confirmButtonText: '清空',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  await cart.clear()
}

function goCheckout() {
  if (!auth.isLoggedIn) {
    auth.openLoginDialog()
    return
  }
  if (cart.selectedCount > 0) {
    router.push('/checkout')
  }
}

onMounted(() => {
  if (auth.isLoggedIn) {
    cart.fetchCart()
  }
})
</script>

<template>
  <div class="cart-page">
    <ShopHeader />

    <main class="cart-shell">
      <section class="cart-hero">
        <div>
          <span>Cart</span>
          <h1>购物车</h1>
          <p>已选商品会进入结算汇总，失效商品不会计入金额。</p>
        </div>
        <button v-if="cart.hasItems" class="clear-button" type="button" @click="clearAll">清空购物车</button>
      </section>

      <section v-if="!auth.isLoggedIn" class="login-needed">
        <h2>登录后查看购物车</h2>
        <p>购物车数据保存在登录账户下，登录后可继续加购和结算。</p>
        <el-button type="primary" size="large" @click="auth.openLoginDialog()">立即登录</el-button>
      </section>

      <section v-else class="cart-layout">
        <div v-loading="cart.loading" class="cart-list">
          <div v-if="cart.hasItems" class="cart-tools">
            <el-checkbox v-model="allSelected">全选</el-checkbox>
            <span>{{ cart.selectedCount }} 件已选</span>
          </div>

          <article
            v-for="item in cart.items"
            :key="item.skuId"
            class="cart-item"
            :class="{ invalid: item.invalid }"
          >
            <el-checkbox
              :model-value="item.selected === 1"
              :disabled="item.invalid"
              @change="(checked) => toggleItem(item, checked)"
            />
            <img
              :src="normalizeImage(item.image, String(item.skuId))"
              :alt="item.spuName || item.skuName"
              @error="handleImageError($event, String(item.skuId))"
            />
            <div class="item-info">
              <strong>{{ item.spuName || item.skuName || '商品' }}</strong>
              <small>{{ specText(item) }}</small>
              <em v-if="item.invalid">商品失效或库存不足</em>
              <span v-else>库存 {{ item.stock ?? 0 }}</span>
            </div>
            <div class="item-price">{{ formatPrice(item.price) }}</div>
            <el-input-number
              :model-value="item.quantity"
              :min="1"
              :max="Math.max(1, item.stock || 1)"
              :disabled="item.invalid"
              controls-position="right"
              @change="(value: number | undefined) => updateQuantity(item, Number(value || 1))"
            />
            <div class="item-total">{{ formatPrice(item.totalAmount) }}</div>
            <button class="remove-button" type="button" @click="removeItem(item)">删除</button>
          </article>

          <el-empty v-if="!cart.loading && !cart.hasItems" description="购物车还没有商品">
            <el-button type="primary" @click="$router.push('/')">去逛逛</el-button>
          </el-empty>
        </div>

        <aside class="summary-panel">
          <span>Summary</span>
          <h2>结算汇总</h2>
          <dl>
            <div>
              <dt>商品总数</dt>
              <dd>{{ cart.totalCount }} 件</dd>
            </div>
            <div>
              <dt>已选商品</dt>
              <dd>{{ cart.selectedCount }} 件</dd>
            </div>
            <div>
              <dt>应付金额</dt>
              <dd class="amount">{{ formatPrice(cart.selectedAmount) }}</dd>
            </div>
          </dl>
          <el-button size="large" type="primary" :disabled="cart.selectedCount === 0" @click="goCheckout">
            去结算
          </el-button>
        </aside>
      </section>
    </main>
  </div>
</template>

<style scoped>
.cart-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at 12% 5%, rgba(47, 143, 103, 0.12), transparent 26%),
    linear-gradient(180deg, #fbfcf8 0%, #f4f7f2 100%);
  color: #111827;
}

.cart-shell {
  max-width: 1240px;
  margin: 0 auto;
  padding: 36px 24px 64px;
}

.cart-hero {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 24px;
}

.cart-hero span,
.summary-panel > span {
  display: inline-flex;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(47, 143, 103, 0.1);
  color: #2f8f67;
  font-size: 13px;
  font-weight: 900;
  text-transform: uppercase;
}

.cart-hero h1 {
  margin: 12px 0 0;
  font-size: 42px;
  line-height: 1.12;
}

.cart-hero p {
  margin: 12px 0 0;
  color: #6b7280;
}

.clear-button,
.remove-button {
  border: 0;
  background: transparent;
  cursor: pointer;
  font: inherit;
}

.clear-button {
  color: #e5484d;
  font-weight: 900;
}

.login-needed,
.cart-list,
.summary-panel {
  border: 1px solid rgba(17, 24, 39, 0.06);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 22px 48px rgba(15, 23, 42, 0.07);
}

.login-needed {
  padding: 44px;
}

.login-needed h2 {
  margin: 0;
  font-size: 28px;
}

.login-needed p {
  margin: 12px 0 24px;
  color: #6b7280;
}

.cart-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 24px;
  align-items: start;
}

.cart-list {
  min-height: 360px;
  padding: 18px;
}

.cart-tools {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 4px 14px;
  color: #6b7280;
  font-weight: 800;
}

.cart-item {
  display: grid;
  grid-template-columns: auto 96px minmax(0, 1fr) 100px 136px 112px auto;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border-top: 1px solid rgba(17, 24, 39, 0.06);
  border-radius: 16px;
  transition: background 0.18s ease, box-shadow 0.18s ease;
}

.cart-item:hover {
  background: #fbfcf8;
  box-shadow: inset 0 0 0 1px rgba(17, 24, 39, 0.03);
}

.cart-item.invalid {
  opacity: 0.58;
}

.cart-item img {
  width: 96px;
  height: 96px;
  border-radius: 14px;
  object-fit: cover;
  background: #f3f4f6;
}

.item-info {
  min-width: 0;
}

.item-info strong,
.item-info small,
.item-info span,
.item-info em {
  display: block;
}

.item-info strong {
  overflow: hidden;
  font-size: 17px;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-info small {
  margin-top: 8px;
  color: #6b7280;
  line-height: 1.5;
}

.item-info span,
.item-info em {
  margin-top: 8px;
  color: #2f8f67;
  font-size: 12px;
  font-style: normal;
  font-weight: 900;
}

.item-info em {
  color: #e5484d;
}

.item-price,
.item-total {
  font-weight: 900;
}

.item-total {
  color: #e5484d;
}

.remove-button {
  color: #6b7280;
  font-weight: 800;
}

.remove-button:hover {
  color: #e5484d;
}

.summary-panel {
  position: sticky;
  top: 96px;
  padding: 24px;
}

.summary-panel h2 {
  margin: 10px 0 20px;
  font-size: 26px;
}

.summary-panel dl,
.summary-panel dd {
  margin: 0;
}

.summary-panel div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 14px 0;
  border-top: 1px solid rgba(17, 24, 39, 0.08);
}

.summary-panel dt {
  color: #6b7280;
}

.summary-panel dd {
  font-weight: 900;
}

.summary-panel .amount {
  color: #e5484d;
  font-size: 24px;
}

.summary-panel :deep(.el-button) {
  width: 100%;
  height: 46px;
  margin-top: 20px;
  border-radius: 12px;
  font-weight: 900;
}

.summary-panel :deep(.el-button--primary),
.login-needed :deep(.el-button--primary) {
  background: linear-gradient(135deg, #e5484d, #c92432);
  border-color: transparent;
  box-shadow: 0 12px 24px rgba(229, 72, 77, 0.22);
}

@media (max-width: 1080px) {
  .cart-layout {
    grid-template-columns: 1fr;
  }

  .summary-panel {
    position: static;
  }
}

@media (max-width: 820px) {
  .cart-shell {
    padding: 28px 16px 44px;
  }

  .cart-hero {
    align-items: start;
    flex-direction: column;
  }

  .cart-item {
    grid-template-columns: auto 82px minmax(0, 1fr);
  }

  .cart-item img {
    width: 82px;
    height: 82px;
  }

  .item-price,
  .item-total,
  .cart-item :deep(.el-input-number),
  .remove-button {
    grid-column: 3;
    justify-self: start;
  }
}
</style>
