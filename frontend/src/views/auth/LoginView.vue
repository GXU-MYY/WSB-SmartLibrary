<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useUserStore } from '@/stores/user'
import { notifyError } from '@/utils/notify'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const form = reactive({
  username: '',
  password: '',
})

const validate = () => {
  if (!form.username.trim()) {
    notifyError('请输入用户名', '用户名或手机号不能为空。')
    return false
  }

  if (!form.password.trim()) {
    notifyError('请输入密码', '登录密码不能为空。')
    return false
  }

  return true
}

const handleSubmit = async () => {
  if (!validate()) {
    return
  }

  loading.value = true

  try {
    await userStore.login(form)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/home'
    router.replace(redirect)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="auth-view">
    <div class="auth-view__copy">
      <span class="eyebrow">登录</span>
      <h2>登录</h2>
      <p>使用用户名或手机号继续。</p>
    </div>

    <form class="auth-view__form" @submit.prevent="handleSubmit">
      <div class="field">
        <label for="username">用户名</label>
        <input id="username" v-model="form.username" type="text" placeholder="输入用户名或手机号" />
      </div>

      <div class="field">
        <label for="password">密码</label>
        <input id="password" v-model="form.password" type="password" placeholder="输入登录密码" />
      </div>

      <div class="inline-actions">
        <button class="button button--primary" type="submit" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
        <RouterLink class="button button--ghost" to="/register">创建账号</RouterLink>
      </div>
    </form>
  </section>
</template>

<style scoped>
.auth-view {
  display: grid;
  gap: 24px;
}

.auth-view__copy {
  display: grid;
  gap: 10px;
}

.auth-view__copy h2,
.auth-view__copy p {
  margin: 0;
}

.auth-view__copy h2 {
  font-size: clamp(2.2rem, 5vw, 3rem);
}

.auth-view__copy p {
  color: var(--sl-ink-soft);
  line-height: 1.7;
}

.auth-view__form {
  display: grid;
  gap: 16px;
}
</style>
