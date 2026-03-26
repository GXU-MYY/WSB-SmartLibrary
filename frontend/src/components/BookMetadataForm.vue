<script setup lang="ts">
interface BookMetadataFormModel {
  title?: string
  subtitle?: string
  author?: string
  publisher?: string
  publishDate?: string
  pageCount?: number | null
  price?: number | null
  binding?: string
  isbn?: string
  isbn10?: string
  keyword?: string
  edition?: string
  impression?: string
  language?: string
  bookFormat?: string
  classify?: string
  cip?: string
  clc?: string
  label?: string
  remark?: string
  coverUrl?: string
  shelfId?: number | null
  isOnShelf?: boolean
  isBorrowed?: boolean
}

const props = withDefaults(
  defineProps<{
    form: BookMetadataFormModel
    coverPreviewUrl?: string
    coverAlt: string
    uploadLabel?: string
    uploadingCover?: boolean
    coverPlaceholder?: string
  }>(),
  {
    coverPreviewUrl: '',
    uploadLabel: '上传封面',
    uploadingCover: false,
    coverPlaceholder: '封面待补充',
  },
)

defineEmits<{
  (event: 'cover-upload', payload: Event): void
}>()
</script>

<template>
  <div class="book-metadata-form">
    <div class="book-metadata-form__main">
      <slot name="before-grid" />

      <div class="field-grid">
        <div class="field">
          <label>书名</label>
          <input v-model="props.form.title" type="text" placeholder="请输入书名" />
        </div>
        <div class="field">
          <label>作者</label>
          <input v-model="props.form.author" type="text" placeholder="作者姓名" />
        </div>
        <div class="field">
          <label>副标题</label>
          <input v-model="props.form.subtitle" type="text" placeholder="可选" />
        </div>
        <div class="field">
          <label>出版社</label>
          <input v-model="props.form.publisher" type="text" placeholder="出版社" />
        </div>
        <div class="field">
          <label>出版日期</label>
          <input v-model="props.form.publishDate" type="date" />
        </div>
        <div class="field">
          <label>页数</label>
          <input v-model.number="props.form.pageCount" type="number" min="1" step="1" placeholder="页数" />
        </div>
        <div class="field">
          <label>价格</label>
          <input v-model.number="props.form.price" type="number" min="0" step="0.01" placeholder="价格" />
        </div>
        <div class="field">
          <label>分类</label>
          <input v-model="props.form.classify" type="text" placeholder="如：文学 / 商业 / 设计" />
        </div>
        <div class="field">
          <label>标签</label>
          <input v-model="props.form.label" type="text" placeholder="多个标签用逗号分隔" />
        </div>
        <div class="field">
          <label>关键词</label>
          <input v-model="props.form.keyword" type="text" placeholder="多个关键词用逗号分隔" />
        </div>
        <div class="field">
          <label>ISBN-13</label>
          <input v-model="props.form.isbn" type="text" placeholder="13 位 ISBN" />
        </div>
        <div class="field">
          <label>ISBN-10</label>
          <input v-model="props.form.isbn10" type="text" placeholder="10 位 ISBN" />
        </div>
        <div class="field">
          <label>装帧</label>
          <input v-model="props.form.binding" type="text" placeholder="如：平装 / 精装" />
        </div>
        <div class="field">
          <label>语言</label>
          <input v-model="props.form.language" type="text" placeholder="如：中文 / 英文" />
        </div>
        <div class="field">
          <label>版次</label>
          <input v-model="props.form.edition" type="text" placeholder="如：第 2 版" />
        </div>
        <div class="field">
          <label>印次</label>
          <input v-model="props.form.impression" type="text" placeholder="如：2025 年第 3 次印刷" />
        </div>
        <div class="field">
          <label>开本</label>
          <input v-model="props.form.bookFormat" type="text" placeholder="如：16 开 / 32 开" />
        </div>
        <div class="field">
          <label>CIP</label>
          <input v-model="props.form.cip" type="text" placeholder="CIP 核字号" />
        </div>
        <div class="field">
          <label>中图法分类</label>
          <input v-model="props.form.clc" type="text" placeholder="如：I247.5" />
        </div>
      </div>

      <slot name="after-grid" />
      <slot name="summary" />

      <div class="field">
        <label>备注</label>
        <textarea v-model="props.form.remark" placeholder="例如版本信息、购买备注或你想补充的说明" />
      </div>
    </div>

    <aside class="book-metadata-form__aside">
      <article class="dialog-note">
        <strong>封面</strong>
        <div class="edit-cover-preview">
          <img v-if="props.coverPreviewUrl" :src="props.coverPreviewUrl" :alt="props.coverAlt" />
          <div v-else class="edit-cover-preview__placeholder">{{ props.coverPlaceholder }}</div>
        </div>
        <div class="edit-cover-controls">
          <div class="field edit-cover-field">
            <label>封面链接</label>
            <input v-model="props.form.coverUrl" type="text" placeholder="粘贴图片 URL 或上传返回的 key" />
          </div>
          <label class="button button--ghost upload-button edit-cover-controls__upload">
            {{ props.uploadingCover ? '上传中...' : props.uploadLabel }}
            <input type="file" accept="image/*" :disabled="props.uploadingCover" @change="$emit('cover-upload', $event)" />
          </label>
        </div>
      </article>

      <slot name="aside-extra" />
    </aside>
  </div>
</template>

<style scoped>
.book-metadata-form {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(260px, 0.8fr);
  gap: 18px;
  align-items: start;
}

.book-metadata-form__main,
.book-metadata-form__aside {
  display: grid;
  gap: 16px;
}

.book-metadata-form__aside {
  align-content: start;
}

.dialog-note {
  display: grid;
  gap: 8px;
  padding: 16px 18px;
  border-radius: 20px;
  border: 1px solid rgba(34, 48, 67, 0.08);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.64), rgba(255, 251, 244, 0.48)),
    radial-gradient(circle at top right, rgba(201, 119, 46, 0.12), transparent 45%);
}

[data-theme='dark'] .dialog-note {
  border-color: rgba(159, 217, 228, 0.14);
  background:
    linear-gradient(180deg, rgba(25, 38, 49, 0.96), rgba(16, 25, 33, 0.92)),
    radial-gradient(circle at top right, rgba(108, 185, 199, 0.14), transparent 48%);
}

.dialog-note strong,
.dialog-note p {
  margin: 0;
}

.dialog-note p {
  color: var(--sl-ink-soft);
  line-height: 1.7;
}

:deep(.field) {
  display: grid;
  gap: 8px;
}

:deep(.field label) {
  color: var(--sl-ink-soft);
  font-size: 0.92rem;
}

:deep(.field input),
:deep(.field select),
:deep(.field textarea) {
  width: 100%;
}

:deep(.field textarea) {
  min-height: 132px;
  resize: vertical;
}

.edit-cover-preview {
  overflow: hidden;
  aspect-ratio: 5 / 6;
  border-radius: 20px;
  background: linear-gradient(180deg, rgba(31, 95, 107, 0.18), rgba(201, 119, 46, 0.24));
}

.edit-cover-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.edit-cover-preview__placeholder {
  display: grid;
  place-items: center;
  width: 100%;
  height: 100%;
  color: rgba(255, 255, 255, 0.92);
  font-size: 1rem;
}

.edit-cover-controls {
  display: grid;
  gap: 12px;
}

.edit-cover-field input {
  width: 100%;
}

.edit-cover-controls__upload {
  justify-content: center;
  width: 100%;
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

:deep(.field-grid) {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

:deep(.field-grid--single) {
  grid-template-columns: 1fr;
}

:deep(.inline-composer) {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
}

:deep(.toggle-row) {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  color: var(--sl-ink-soft);
}

:deep(.toggle-row label) {
  display: inline-flex;
  gap: 8px;
  align-items: center;
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

@media (max-width: 900px) {
  .book-metadata-form {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .field-grid {
    grid-template-columns: 1fr;
  }

  :deep(.field-grid) {
    grid-template-columns: 1fr;
  }
}
</style>
