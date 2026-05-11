<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { login } from '@/api/auth'

const router = useRouter()
const route = useRoute()

const form = reactive({ username: 'admin', password: '123456' })
const loading = ref(false)

async function onSubmit() {
  if (!form.username || !form.password) {
    message.error('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const res = await login(form)
    localStorage.setItem('admin_token', res.data.accessToken)
    localStorage.setItem('admin_refresh_token', res.data.refreshToken)
    localStorage.setItem('admin_user', JSON.stringify(res.data.user))
    localStorage.removeItem('admin_permission_codes')
    message.success('登录成功')
    const redirect = (route.query.redirect as string) || '/dashboard'
    router.replace(redirect)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div style="min-height: 100vh; display: flex; align-items: center; justify-content: center; background: #f0f2f5">
    <a-card title="GJ Mall 管理后台登录" style="width: 360px">
      <a-form :model="form" layout="vertical" @finish="onSubmit">
        <a-form-item label="用户名" name="username">
          <a-input v-model:value="form.username" placeholder="请输入用户名" />
        </a-form-item>
        <a-form-item label="密码" name="password">
          <a-input-password v-model:value="form.password" placeholder="请输入密码" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit" block :loading="loading">登录</a-button>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>
