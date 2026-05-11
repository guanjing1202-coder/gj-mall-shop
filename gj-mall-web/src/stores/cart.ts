import { defineStore } from 'pinia'
import { ElMessage } from 'element-plus'
import {
  addCart,
  clearCart,
  getCart,
  removeCartItem,
  selectAllCart,
  updateCart,
  type CartInfo,
  type CartItem,
} from '@/api/cart'
import type { ApiId } from '@/api/product'
import { useAuthStore } from './auth'

const emptyCart = (): CartInfo => ({
  items: [],
  totalCount: 0,
  selectedCount: 0,
  selectedAmount: 0,
})

export const useCartStore = defineStore('mall-cart', {
  state: () => ({
    cart: emptyCart(),
    loading: false,
  }),
  getters: {
    items: (state): CartItem[] => state.cart.items || [],
    totalCount: (state): number => Number(state.cart.totalCount || 0),
    selectedCount: (state): number => Number(state.cart.selectedCount || 0),
    selectedAmount: (state): number => Number(state.cart.selectedAmount || 0),
    hasItems: (state): boolean => Boolean(state.cart.items?.length),
  },
  actions: {
    reset() {
      this.cart = emptyCart()
    },
    async fetchCart() {
      const auth = useAuthStore()
      if (!auth.isLoggedIn) {
        this.reset()
        return this.cart
      }
      this.loading = true
      try {
        const res = await getCart()
        this.cart = res.data || emptyCart()
        return this.cart
      } finally {
        this.loading = false
      }
    },
    async add(skuId: ApiId, quantity: number) {
      await addCart({ skuId, quantity })
      await this.fetchCart()
      ElMessage.success('已加入购物车')
    },
    async updateItem(skuId: ApiId, payload: { quantity?: number; selected?: number }) {
      await updateCart({ skuId, ...payload })
      await this.fetchCart()
    },
    async remove(skuId: ApiId) {
      await removeCartItem(skuId)
      await this.fetchCart()
    },
    async clear() {
      await clearCart()
      this.reset()
    },
    async selectAll(selected: boolean) {
      await selectAllCart(selected)
      await this.fetchCart()
    },
  },
})
