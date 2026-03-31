<script setup lang="ts">
import axios from 'axios'
import { computed, onMounted, reactive, ref, watch } from 'vue'

import { uploadPicture } from '@/api/file'
import PageIntro from '@/components/PageIntro.vue'
import SectionPanel from '@/components/SectionPanel.vue'
import UserAvatar from '@/components/UserAvatar.vue'
import { useRegisterPageRefresh } from '@/composables/usePageRefresh'
import { useUserStore } from '@/stores/user'
import { formatDateTime } from '@/utils/format'
import { notifyError, notifySuccess } from '@/utils/notify'

const userStore = useUserStore()

const editorVisible = ref(false)
const saving = ref(false)
const uploading = ref(false)

const form = reactive({
  nickName: '',
  realName: '',
  email: '',
  signature: '',
  avatar: '',
})

const syncForm = () => {
  form.nickName = userStore.userInfo?.nickName || ''
  form.realName = userStore.userInfo?.realName || ''
  form.email = userStore.userInfo?.email || ''
  form.signature = userStore.userInfo?.signature || ''
  form.avatar = userStore.userInfo?.avatar || ''
}

const displayName = computed(() => userStore.displayName)

const displayText = (value?: string | null) => {
  const normalized = value?.trim()
  return normalized ? normalized : '未填写'
}

const formatProfileDate = (value?: string) => (value ? formatDateTime(value) : '未填写')

const resolveUploadErrorMessage = (error: unknown) => {
  const rawMessage = axios.isAxiosError(error)
    ? ((error.response?.data as { message?: string; msg?: string } | undefined)?.message ||
        (error.response?.data as { message?: string; msg?: string } | undefined)?.msg ||
        error.message)
    : error instanceof Error
      ? error.message
      : ''

  if (/Bucket name only should contain lowercase characters, num and -/i.test(rawMessage)) {
    return '头像上传暂时不可用，文件服务的存储桶配置不符合规范。你可以先直接填写头像 URL。'
  }

  return rawMessage || '头像上传失败，请稍后重试。'
}

const openEditor = () => {
  syncForm()
  editorVisible.value = true
}

const closeEditor = () => {
  if (saving.value) {
    return
  }

  editorVisible.value = false
  syncForm()
}

watch(
  () => userStore.userInfo,
  () => {
    if (!editorVisible.value) {
      syncForm()
    }
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
    notifySuccess('头像已上传', '保存资料后就会同步到身份卡片。')
  } catch (error) {
    notifyError('头像上传失败', resolveUploadErrorMessage(error))
  } finally {
    uploading.value = false
    input.value = ''
  }
}

const handleSave = async () => {
  saving.value = true

  try {
    await userStore.updateProfile({
      nickName: form.nickName,
      realName: form.realName,
      email: form.email,
      signature: form.signature,
      avatar: form.avatar,
    })
    editorVisible.value = false
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
      title="把你的阅读身份整理成一张清晰卡片"
      description="个人中心会集中展示你的账号资料和公开信息，方便你在微书包里维护一份稳定的个人形象。"
    />

    <section class="page-grid profile-layout">
      <SectionPanel
        title="身份卡片"
        hint="这里会展示你当前的身份信息，编辑时会以弹窗的方式打开。"
      >
        <article class="profile-card surface-card">
          <div class="profile-card__hero">
            <UserAvatar :src="userStore.userInfo?.avatar" :name="displayName" :size="96" />
            <div class="profile-card__identity">
              <h3>{{ displayName }}</h3>
              <p class="profile-card__phone">{{ displayText(userStore.userInfo?.phone) }}</p>
            </div>
          </div>

          <dl class="profile-card__details">
            <div class="profile-info-card">
              <dt>邮箱</dt>
              <dd>{{ displayText(userStore.userInfo?.email) }}</dd>
            </div>
            <div class="profile-info-card">
              <dt>真实姓名</dt>
              <dd>{{ displayText(userStore.userInfo?.realName) }}</dd>
            </div>
            <div class="profile-info-card">
              <dt>注册时间</dt>
              <dd>{{ formatProfileDate(userStore.userInfo?.createTime) }}</dd>
            </div>
            <div class="profile-info-card profile-info-card--wide">
              <dt>个性签名</dt>
              <dd>{{ displayText(userStore.userInfo?.signature) }}</dd>
            </div>
          </dl>

          <div class="profile-card__actions">
            <button class="button button--primary" type="button" @click="openEditor">
              编辑个人信息
            </button>
          </div>
        </article>
      </SectionPanel>
    </section>

    <Teleport to="body">
      <div v-if="editorVisible" class="dialog-overlay" @click.self="closeEditor">
        <section class="surface-card desk-dialog desk-dialog--wide profile-dialog">
          <header class="desk-dialog__head">
            <div>
              <h2>编辑个人信息</h2>
            </div>
            <button class="button button--ghost desk-dialog__close" type="button" @click="closeEditor">
              关闭
            </button>
          </header>

          <div class="desk-dialog__body">
            <div class="profile-editor">
              <div class="profile-editor__avatar surface-card">
                <UserAvatar :src="form.avatar || userStore.userInfo?.avatar" :name="form.nickName || displayName" :size="84" />
                <div class="profile-editor__avatar-copy">
                  <h3>{{ form.nickName || displayName }}</h3>
                  <p>{{ displayText(form.email) }}</p>
                </div>
              </div>

              <div class="profile-form">
                <div class="field">
                  <label>昵称</label>
                  <input v-model="form.nickName" type="text" maxlength="50" placeholder="输入你希望展示的昵称" />
                </div>

                <div class="profile-form__row">
                  <div class="field">
                    <label>真实姓名</label>
                    <input v-model="form.realName" type="text" maxlength="50" placeholder="输入真实姓名" />
                  </div>
                  <div class="field">
                    <label>邮箱</label>
                    <input v-model="form.email" type="email" maxlength="100" placeholder="输入常用邮箱" />
                  </div>
                </div>

                <div class="field">
                  <label>个性签名</label>
                  <textarea
                    v-model="form.signature"
                    maxlength="255"
                    placeholder="用一句话介绍你的阅读兴趣或最近关注的方向"
                  />
                </div>

                <div class="field">
                  <label>头像地址</label>
                  <div class="avatar-row">
                    <input v-model="form.avatar" type="text" maxlength="255" placeholder="可填写图片 URL 或上传后的 key" />
                    <label class="button button--ghost upload-button">
                      {{ uploading ? '上传中...' : '上传头像' }}
                      <input type="file" accept="image/*" :disabled="uploading" @change="handleAvatarUpload" />
                    </label>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <footer class="desk-dialog__foot desk-dialog__foot--align-end">
            <button class="button button--ghost" type="button" :disabled="saving" @click="closeEditor">
              取消
            </button>
            <button class="button button--primary" type="button" :disabled="saving" @click="handleSave">
              {{ saving ? '保存中...' : '保存资料' }}
            </button>
          </footer>
        </section>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.profile-layout > * {
  grid-column: span 12;
}

.profile-card {
  display: grid;
  gap: 22px;
  padding: 24px;
}

.profile-card__hero {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 18px;
  align-items: center;
}

.profile-card__identity {
  min-width: 0;
}

.profile-card__identity h3,
.profile-card__phone {
  margin: 0;
}

.profile-card__identity h3 {
  font-size: 1.52rem;
  line-height: 1.15;
}

.profile-card__phone {
  margin-top: 10px;
  color: var(--sl-ink-soft);
  font-size: 1rem;
}

.profile-card__details {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin: 0;
}

.profile-info-card {
  display: grid;
  align-content: start;
  min-width: 0;
  min-height: 108px;
  padding: 16px 18px;
  border-radius: 20px;
  border: 1px solid var(--sl-line);
  background: var(--sl-soft-panel-bg);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.25);
}

.profile-info-card--wide {
  grid-column: 1 / -1;
  min-height: 120px;
}

.profile-card__details dt {
  color: var(--sl-ink-soft);
  font-size: 0.82rem;
  letter-spacing: 0.04em;
}

.profile-card__details dd {
  margin: 8px 0 0;
  line-height: 1.7;
  word-break: break-word;
  font-size: 0.98rem;
}

.profile-card__actions {
  display: flex;
  justify-content: flex-end;
}

.dialog-overlay {
  position: fixed;
  inset: 0;
  z-index: 110;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(15, 23, 42, 0.46);
  backdrop-filter: blur(14px);
}

.desk-dialog {
  width: min(100%, 840px);
  max-height: min(90vh, 880px);
  overflow: auto;
  border: 1px solid var(--sl-line);
  box-shadow: 0 30px 70px rgba(15, 23, 42, 0.18);
}

.desk-dialog--wide {
  border-radius: 28px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.96)),
    var(--sl-panel-bg);
}

[data-theme='dark'] .desk-dialog--wide {
  background:
    linear-gradient(180deg, rgba(17, 24, 39, 0.98), rgba(15, 23, 42, 0.96)),
    var(--sl-panel-bg);
  box-shadow: 0 28px 70px rgba(2, 6, 23, 0.52);
}

.desk-dialog__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  padding: 24px 28px 18px;
}

.desk-dialog__head h2,
.desk-dialog__head p {
  margin: 0;
}

.desk-dialog__head p {
  margin-top: 8px;
  color: var(--sl-ink-soft);
  line-height: 1.6;
}

.desk-dialog__close {
  flex-shrink: 0;
}

.desk-dialog__body {
  padding: 0 28px 24px;
}

.desk-dialog__foot {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 0 28px 28px;
}

.profile-editor {
  display: grid;
  gap: 18px;
}

.profile-editor__avatar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px;
}

.profile-editor__avatar-copy h3,
.profile-editor__avatar-copy p {
  margin: 0;
}

.profile-editor__avatar-copy p {
  margin-top: 8px;
  color: var(--sl-ink-soft);
}

.profile-form {
  display: grid;
  gap: 16px;
}

.profile-form__row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
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
  .dialog-overlay {
    padding: 16px;
    align-items: flex-end;
  }

  .desk-dialog {
    max-height: 92vh;
  }

  .desk-dialog--wide {
    border-radius: 24px 24px 0 0;
  }

  .desk-dialog__head,
  .desk-dialog__body,
  .desk-dialog__foot {
    padding-left: 20px;
    padding-right: 20px;
  }

  .profile-card__hero,
  .profile-card__details,
  .profile-form__row,
  .avatar-row {
    grid-template-columns: 1fr;
  }

  .profile-info-card--wide {
    grid-column: span 1;
  }

  .profile-editor__avatar {
    align-items: flex-start;
  }
}
</style>
