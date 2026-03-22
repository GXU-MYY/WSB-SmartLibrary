<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { register, sendCaptcha } from '@/api/user'
import { notifyError, notifySuccess } from '@/utils/notify'

const router = useRouter()

const loading = ref(false)
const sending = ref(false)
const countdown = ref(0)

const form = reactive({
  phone: '',
  password: '',
  captcha: '',
})

let timer: number | null = null

const validate = () => {
  if (!/^1\d{10}$/.test(form.phone)) {
    notifyError('手机号格式不正确', '请输入 11 位中国大陆手机号。')
    return false
  }

  if (form.password.trim().length < 6) {
    notifyError('密码太短', '请至少输入 6 位密码。')
    return false
  }

  if (!form.captcha.trim()) {
    notifyError('请输入验证码', '短信验证码不能为空。')
    return false
  }

  return true
}

const startCountdown = () => {
  countdown.value = 60
  timer = window.setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0 && timer) {
      window.clearInterval(timer)
      timer = null
    }
  }, 1000)
}

const handleSendCaptcha = async () => {
  if (!/^1\d{10}$/.test(form.phone)) {
    notifyError('请先输入手机号', '验证码需要发送到有效手机号。')
    return
  }

  sending.value = true

  try {
    await sendCaptcha(form.phone)
    notifySuccess('验证码已发送', '请注意查收短信。')
    startCountdown()
  } finally {
    sending.value = false
  }
}

const handleSubmit = async () => {
  if (!validate()) {
    return
  }

  loading.value = true

  try {
    await register(form)
    notifySuccess('注册成功', '现在可以使用新账号登录。')
    router.replace('/login')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="auth-view">
    <div class="auth-view__copy">
      <span class="eyebrow">Register</span>
      <h2>为你的数字书房开一个新入口</h2>
      <p>注册完成后，你就可以录入藏书、加入群组，并逐步沉淀自己的阅读网络。</p>
    </div>

    <form class="auth-view__form" @submit.prevent="handleSubmit">
      <div class="field">
        <label for="phone">手机号</label>
        <input id="phone" v-model="form.phone" type="tel" placeholder="输入 11 位手机号" />
      </div>

      <div class="field">
        <label for="password">密码</label>
        <input id="password" v-model="form.password" type="password" placeholder="至少 6 位密码" />
      </div>

      <div class="field">
        <label for="captcha">验证码</label>
        <div class="auth-view__captcha">
          <input id="captcha" v-model="form.captcha" type="text" placeholder="输入短信验证码" />
          <button
            class="button button--secondary"
            type="button"
            :disabled="sending || countdown > 0"
            @click="handleSendCaptcha"
          >
            {{ countdown > 0 ? `${countdown}s` : sending ? '发送中...' : '获取验证码' }}
          </button>
        </div>
      </div>

      <div class="inline-actions">
        <button class="button button--primary" type="submit" :disabled="loading">
          {{ loading ? '注册中...' : '完成注册' }}
        </button>
        <RouterLink class="button button--ghost" to="/login">返回登录</RouterLink>
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
  font-size: 2.2rem;
}

.auth-view__copy p {
  color: var(--sl-ink-soft);
  line-height: 1.8;
}

.auth-view__form {
  display: grid;
  gap: 16px;
}

.auth-view__captcha {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 150px;
  gap: 10px;
}

@media (max-width: 560px) {
  .auth-view__captcha {
    grid-template-columns: 1fr;
  }
}
</style>
