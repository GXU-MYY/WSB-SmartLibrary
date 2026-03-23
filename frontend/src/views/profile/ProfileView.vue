<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'

import { uploadPicture } from '@/api/file'
import { useRegisterPageRefresh } from '@/composables/usePageRefresh'
import PageIntro from '@/components/PageIntro.vue'
import SectionPanel from '@/components/SectionPanel.vue'
import UserAvatar from '@/components/UserAvatar.vue'
import { useUserStore } from '@/stores/user'
import { formatDateTime } from '@/utils/format'
import { notifySuccess } from '@/utils/notify'

const userStore = useUserStore()

const saving = ref(false)
const uploading = ref(false)

const form = reactive({
  nickName: '',
  signature: '',
  avatar: '',
})

watch(
  () => userStore.userInfo,
  (profile) => {
    form.nickName = profile?.nickName || ''
    form.signature = profile?.signature || ''
    form.avatar = profile?.avatar || ''
  },
  { immediate: true },
)

const handleAvatarUpload = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]

  if (!file) {
    return
  }

  uploading.value = true

  try {
    const result = await uploadPicture(file)
    form.avatar = result.pic
    notifySuccess('头像已上传', '保存资料后即可正式生效。')
  } finally {
    uploading.value = false
    input.value = ''
  }
}

const handleSave = async () => {
  saving.value = true

  try {
    await userStore.updateProfile(form)
  } finally {
    saving.value = false
  }
}

const loadProfile = async () => {
  await userStore.fetchProfile(true)
}

useRegisterPageRefresh(loadProfile)

onMounted(loadProfile)
</script>

<template>
  <div class="page-shell page-stack">
    <PageIntro
      eyebrow="Profile"
      title="把你的身份信息也整理成一张清晰的卡片"
      description="个人中心不只是编辑昵称，更是你在社区和书库中的公开身份层。"
    />

    <section class="page-grid profile-layout">
      <SectionPanel
        title="身份卡片"
        hint="当前会话信息、头像和公开展示资料都会在这里集中呈现。"
      >
        <div class="profile-card surface-card">
          <UserAvatar
            :src="form.avatar || userStore.userInfo?.avatar"
            :name="form.nickName || userStore.displayName"
            :size="88"
          />
          <div>
            <h3>{{ form.nickName || userStore.displayName }}</h3>
            <p>{{ userStore.userInfo?.userName || '未命名用户' }}</p>
            <span class="badge">{{ userStore.userInfo?.phone || '手机号未记录' }}</span>
          </div>
        </div>

        <dl class="detail-list">
          <div>
            <dt>创建时间</dt>
            <dd>{{ formatDateTime(userStore.userInfo?.createTime) }}</dd>
          </div>
          <div>
            <dt>最近更新</dt>
            <dd>{{ formatDateTime(userStore.userInfo?.updateTime) }}</dd>
          </div>
          <div>
            <dt>邮箱</dt>
            <dd>{{ userStore.userInfo?.email || '未填写' }}</dd>
          </div>
        </dl>
      </SectionPanel>

      <SectionPanel
        title="编辑公开资料"
        hint="这些信息会影响你在群组、评论和个人主页中的展示方式。"
      >
        <div class="field">
          <label>昵称</label>
          <input v-model="form.nickName" type="text" placeholder="输入你希望展示的名字" />
        </div>

        <div class="field">
          <label>签名</label>
          <textarea v-model="form.signature" placeholder="用一句话介绍你的阅读兴趣或擅长方向" />
        </div>

        <div class="field">
          <label>头像地址</label>
          <div class="avatar-row">
            <input v-model="form.avatar" type="text" placeholder="可填图片 URL 或上传后的 key" />
            <label class="button button--ghost upload-button">
              {{ uploading ? '上传中...' : '上传头像' }}
              <input type="file" accept="image/*" :disabled="uploading" @change="handleAvatarUpload" />
            </label>
          </div>
        </div>

        <button class="button button--primary" type="button" :disabled="saving" @click="handleSave">
          {{ saving ? '保存中...' : '保存资料' }}
        </button>
      </SectionPanel>
    </section>
  </div>
</template>

<style scoped>
.profile-layout > * {
  grid-column: span 6;
}

.profile-card {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 18px;
}

.profile-card h3,
.profile-card p {
  margin: 0;
}

.profile-card p {
  margin-top: 6px;
  color: var(--sl-ink-soft);
}

.detail-list {
  display: grid;
  gap: 12px;
}

.detail-list div {
  padding: 14px 16px;
  border-radius: 18px;
  border: 1px solid var(--sl-line);
  background: var(--sl-soft-panel-bg);
}

.detail-list dt {
  color: var(--sl-ink-soft);
  font-size: 0.82rem;
}

.detail-list dd {
  margin: 8px 0 0;
}

.avatar-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
}

.upload-button {
  position: relative;
  overflow: hidden;
}

.upload-button input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

@media (max-width: 960px) {
  .profile-layout > * {
    grid-column: span 12;
  }

  .avatar-row {
    grid-template-columns: 1fr;
  }
}
</style>
