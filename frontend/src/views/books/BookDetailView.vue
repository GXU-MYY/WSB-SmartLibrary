<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import {
  addCollect,
  addReadingRecord,
  deleteCollect,
  getBookDetail,
  getBookShelves,
  getMyBookCollects,
  getReadingRecords,
  getShelves,
  offShelf,
  onShelf,
  updateReadingRecord,
} from '@/api/book'
import { aggregateReviews, getAiSummary, getReviewDigest, getSimilarBooks } from '@/api/rag'
import BookCard from '@/components/BookCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import LoadingState from '@/components/LoadingState.vue'
import PageIntro from '@/components/PageIntro.vue'
import SectionPanel from '@/components/SectionPanel.vue'
import { useRegisterPageRefresh } from '@/composables/usePageRefresh'
import type { Book, CollectBook, ReadingRecord, Shelf } from '@/types/models'
import {
  buildBookCard,
  formatCurrency,
  formatDate,
  parseTagList,
  readingStatusLabel,
  resolvePictureUrl,
} from '@/utils/format'
import { notifyError, notifySuccess } from '@/utils/notify'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const reviewLoading = ref(false)
const collectLoading = ref(false)
const shelfLoading = ref(false)
const activeAiPane = ref<'summary' | 'reviews'>('summary')

const book = ref<Book | null>(null)
const readingRecord = ref<ReadingRecord | null>(null)
const collectRecord = ref<CollectBook | null>(null)
const shelves = ref<Shelf[]>([])
const bookShelf = ref<Shelf | null>(null)
const similarBooks = ref<Book[]>([])
const aiSummary = ref('')
const reviewDigest = ref('')

const attachShelfId = ref(0)

const bookId = computed(() => Number(route.params.id))
const coverUrl = computed(() => resolvePictureUrl(book.value?.coverUrl))
const tagItems = computed(() => parseTagList(book.value?.label))
const attachedShelfIds = computed(() => (bookShelf.value?.id ? [bookShelf.value.id] : []))
const attachedShelfNames = computed(() => (bookShelf.value?.shelfName ? [bookShelf.value.shelfName] : []))
const isBorrowedBook = computed(() => Boolean(book.value?.isBorrowed))
const isOnShelf = computed(() => Boolean(bookShelf.value) || Boolean(book.value?.isOnShelf))
const isSelectedShelfAttached = computed(
  () => attachShelfId.value > 0 && attachedShelfIds.value.includes(attachShelfId.value),
)
const canManageShelf = computed(() => !isBorrowedBook.value || isOnShelf.value)
const shelfStatusText = computed(() =>
  attachedShelfNames.value.length > 0 ? attachedShelfNames.value.join('、') : '当前未上架',
)
const shelfFieldLabel = computed(() =>
  isOnShelf.value ? `是否上架（当前在架：${shelfStatusText.value}）` : '是否上架（当前未上架）',
)
const shelfActionText = computed(() => {
  if (shelfLoading.value) {
    return isSelectedShelfAttached.value ? '下架中...' : '上架中...'
  }

  return isSelectedShelfAttached.value ? '下架' : '上架'
})
const collectButtonLabel = computed(() => (collectRecord.value ? '取消收藏' : '加入收藏'))

const isSameId = (left: number | string | null | undefined, right: number | string | null | undefined) =>
  Number(left || 0) === Number(right || 0)
const findBookCollectRecord = (collects: CollectBook[]) =>
  collects.find((item) => isSameId(item.bookId, bookId.value)) || null

const syncAttachShelfSelection = () => {
  if (bookShelf.value?.id) {
    attachShelfId.value = bookShelf.value.id
    return
  }

  attachShelfId.value = 0
}

const loadPage = async () => {
  loading.value = true

  try {
    const [bookResult, readingResult, similarResult, collectResult, shelfResult, bookShelfResult] =
      await Promise.allSettled([
        getBookDetail(bookId.value),
        getReadingRecords(bookId.value),
        getSimilarBooks(bookId.value, 4),
        getMyBookCollects(),
        getShelves(),
        getBookShelves(bookId.value),
      ])

    if (bookResult.status === 'fulfilled') {
      book.value = bookResult.value
    }

    if (readingResult.status === 'fulfilled' && !Array.isArray(readingResult.value)) {
      readingRecord.value = readingResult.value
    } else {
      readingRecord.value = null
    }

    if (similarResult.status === 'fulfilled') {
      similarBooks.value = similarResult.value as unknown as Book[]
    }

    if (collectResult.status === 'fulfilled') {
      collectRecord.value = findBookCollectRecord(collectResult.value)
    }

    if (shelfResult.status === 'fulfilled') {
      shelves.value = shelfResult.value
    }

    if (bookShelfResult.status === 'fulfilled') {
      bookShelf.value = bookShelfResult.value
    } else {
      bookShelf.value = null
    }

    syncAttachShelfSelection()

    const [summaryResult, reviewResult] = await Promise.allSettled([
      getAiSummary(bookId.value),
      getReviewDigest(bookId.value),
    ])

    aiSummary.value = summaryResult.status === 'fulfilled' ? summaryResult.value : ''
    reviewDigest.value = reviewResult.status === 'fulfilled' ? reviewResult.value || '' : ''
  } finally {
    loading.value = false
  }
}

const handleReadingStatusChange = async (event: Event) => {
  if (!book.value) {
    return
  }

  const target = event.target as HTMLSelectElement
  const status = Number(target.value)
  const payload = {
    bookId: book.value.id,
    readingStatus: status,
  }

  if (readingRecord.value?.id) {
    readingRecord.value = await updateReadingRecord(payload)
  } else {
    readingRecord.value = await addReadingRecord(payload)
  }

  notifySuccess('阅读状态已更新', `当前状态：${readingStatusLabel(status)}`)
}

const toggleCollect = async () => {
  collectLoading.value = true

  try {
    if (collectRecord.value) {
      await deleteCollect(collectRecord.value.id)
      collectRecord.value = null
      notifySuccess('已取消收藏')
    } else {
      await addCollect({ bookId: bookId.value })
      const collects = await getMyBookCollects()
      collectRecord.value = findBookCollectRecord(collects)
      notifySuccess('已加入收藏')
    }
  } finally {
    collectLoading.value = false
  }
}

const handleGoBack = () => {
  if (window.history.length > 1) {
    router.back()
    return
  }

  router.push('/books')
}

const handleAggregateReviews = async () => {
  activeAiPane.value = 'reviews'
  reviewLoading.value = true

  try {
    reviewDigest.value = await aggregateReviews(bookId.value)
    notifySuccess('网络书评已聚合')
  } finally {
    reviewLoading.value = false
  }
}

const handleShelfAction = async () => {
  if (!book.value || !attachShelfId.value) {
    notifyError('请选择目标书架')
    return
  }

  if (isBorrowedBook.value && !isSelectedShelfAttached.value) {
    notifyError('借入图书不能上架')
    return
  }

  shelfLoading.value = true

  try {
    if (isSelectedShelfAttached.value) {
      await offShelf({
        book_id: book.value.id,
        shelf_id: attachShelfId.value,
      })
      notifySuccess('图书已下架')
    } else {
      await onShelf({
        book_id: book.value.id,
        shelf_id: attachShelfId.value,
      })
      notifySuccess('图书已上架')
    }
    await loadPage()
  } finally {
    shelfLoading.value = false
  }
}

useRegisterPageRefresh(loadPage)

watch(
  () => route.params.id,
  () => {
    if (route.params.id) {
      loadPage()
    }
  },
)

onMounted(loadPage)
</script>

<template>
  <div class="page-shell page-stack">
    <PageIntro
      eyebrow="Book Detail"
      :title="book?.title || '图书详情'"
      :description="book?.summary || '查看图书元数据、AI 摘要与相似图书推荐。'"
    >
      <template #actions>
        <button
          class="favorite-icon-button favorite-icon-button--large"
          type="button"
          :class="{ 'is-collected': collectRecord }"
          :disabled="collectLoading"
          :aria-label="collectButtonLabel"
          :title="collectButtonLabel"
          @click="toggleCollect"
        >
          <span class="favorite-icon-button__icon" aria-hidden="true">
            {{ collectRecord ? '★' : '☆' }}
          </span>
        </button>
        <button
          class="button button--ghost detail-page-action"
          type="button"
          aria-label="返回上一页"
          title="返回上一页"
          @click="handleGoBack"
        >
          <span aria-hidden="true" class="detail-page-action__icon">←</span>
        </button>
      </template>
    </PageIntro>

    <LoadingState v-if="loading && !book" title="正在加载图书详情" />

    <EmptyState
      v-else-if="!loading && !book"
      title="没有找到这本图书"
      description="它可能已被删除，或者当前账号还没有访问权限。"
    >
      <button class="button button--primary" type="button" @click="router.push('/books')">返回图书列表</button>
    </EmptyState>

    <template v-else>
      <section class="surface-card detail-hero">
        <div class="detail-hero__cover">
          <img v-if="coverUrl" :src="coverUrl" :alt="book?.title" loading="lazy" />
          <div v-else class="detail-hero__placeholder serif-title">BOOK</div>
        </div>

        <div class="detail-hero__copy">
          <div class="detail-hero__head">
            <div>
              <h2>{{ book?.title }}</h2>
              <p>{{ book?.author || '作者待补充' }}</p>
            </div>
            <div class="inline-actions">
              <span v-if="book?.classify" class="badge">{{ book.classify }}</span>
              <span v-if="book?.isBorrowed" class="badge badge--accent">借入图书</span>
            </div>
          </div>

          <div class="detail-hero__meta">
            <article>
              <span>价格</span>
              <strong>{{ formatCurrency(book?.price) }}</strong>
            </article>
            <article>
              <span>出版社</span>
              <strong>{{ book?.publisher || '未记录' }}</strong>
            </article>
            <article>
              <span>出版时间</span>
              <strong>{{ formatDate(book?.publishDate) }}</strong>
            </article>
          </div>

          <div v-if="tagItems.length" class="inline-actions">
            <span v-for="item in tagItems" :key="item" class="badge">{{ item }}</span>
          </div>

          <div class="detail-hero__tools">
            <div class="field">
              <label>阅读状态</label>
              <select :value="readingRecord?.readingStatus || 0" @change="handleReadingStatusChange">
                <option :value="0">未设置</option>
                <option :value="1">想读</option>
                <option :value="2">在读</option>
                <option :value="3">已读</option>
              </select>
            </div>
            <div class="field">
              <label>{{ shelfFieldLabel }}</label>
              <div v-if="canManageShelf" class="detail-hero__attach">
                <select v-model.number="attachShelfId" :disabled="isBorrowedBook">
                  <option :value="0" disabled hidden>选择书架</option>
                  <option v-for="item in shelves" :key="item.id" :value="item.id">{{ item.shelfName }}</option>
                </select>
                <button class="button button--ghost" type="button" :disabled="shelfLoading" @click="handleShelfAction">
                  {{ shelfActionText }}
                </button>
              </div>
              <p v-else class="detail-hero__shelf-note">借入的图书不能上架到个人书架。</p>
            </div>
          </div>
        </div>
      </section>

      <section class="page-grid detail-grid">
        <SectionPanel title="AI 阅读助手" hint="这里集中展示摘要、聚合书评与延展阅读结果。">
          <div class="ai-reader-switch">
            <button
              class="button ai-reader-switch__item"
              :class="activeAiPane === 'summary' ? 'button--secondary ai-reader-switch__item--active' : 'button--ghost'"
              type="button"
              @click="activeAiPane = 'summary'"
            >
              AI 摘要
            </button>
            <button
              class="button ai-reader-switch__item"
              :class="activeAiPane === 'reviews' ? 'button--secondary ai-reader-switch__item--active' : 'button--ghost'"
              type="button"
              @click="activeAiPane = 'reviews'"
            >
              网络书评
            </button>
          </div>

          <div v-if="activeAiPane === 'summary'" class="copy-block">
            <p class="copy-block__body">{{ aiSummary || 'AI 摘要正在生成中，请稍后回来查看。' }}</p>
          </div>

          <div v-else class="copy-block">
            <p class="copy-block__body copy-block__body--preserve">{{ reviewDigest || '暂未聚合。' }}</p>
            <div class="copy-block__footer">
              <button
                class="button button--ghost copy-block__trigger"
                type="button"
                :disabled="reviewLoading"
                @click="handleAggregateReviews"
              >
                {{ reviewLoading ? '聚合中...' : '聚合网络书评' }}
              </button>
            </div>
          </div>
        </SectionPanel>

        <SectionPanel title="相似图书" hint="来自 RAG 相似检索结果，适合继续扩展阅读链路。">
          <div class="similar-grid">
            <BookCard
              v-for="item in similarBooks"
              :key="item.id"
              mobile-minimal
              :book="
                buildBookCard({
                  id: item.id,
                  title: item.title,
                  author: item.author,
                  coverUrl: item.coverUrl,
                  summary: item.summary,
                  badge: '相似',
                })
              "
            >
              <template #actions>
                <button class="button button--ghost" type="button" @click="router.push(`/books/${item.id}`)">
                  打开
                </button>
              </template>
            </BookCard>

            <EmptyState v-if="similarBooks.length === 0" title="暂时还没有相似图书" />
          </div>
        </SectionPanel>
      </section>
    </template>
  </div>
</template>

<style scoped>
.detail-page-action {
  width: 46px;
  height: 46px;
  min-width: 46px;
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  border: 1px solid var(--sl-line);
  background: var(--sl-ghost-bg);
  color: var(--sl-ink);
  font-size: 1.18rem;
  line-height: 1;
  cursor: pointer;
  transition:
    transform 0.18s ease,
    border-color 0.18s ease,
    background 0.18s ease,
    color 0.18s ease,
    box-shadow 0.18s ease;
}

.detail-page-action:hover:not(:disabled) {
  transform: translateY(-1px);
}

.detail-page-action:disabled {
  cursor: wait;
  opacity: 0.72;
}

.detail-page-action__icon {
  font-family: 'Segoe UI Symbol', 'Apple Symbols', 'Noto Sans Symbols 2', sans-serif;
  font-size: 1.26rem;
  line-height: 1;
}

.detail-hero {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 22px;
  padding: 22px;
}

.detail-hero__cover {
  overflow: hidden;
  min-height: 360px;
  border-radius: 24px;
  background: linear-gradient(180deg, rgba(31, 95, 107, 0.18), rgba(201, 119, 46, 0.24));
}

.detail-hero__cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.detail-hero__placeholder {
  display: grid;
  place-items: center;
  width: 100%;
  height: 100%;
  color: rgba(255, 255, 255, 0.84);
  font-size: 2rem;
}

.detail-hero__copy {
  display: grid;
  gap: 18px;
}

.detail-hero__head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
}

.detail-hero__head h2,
.detail-hero__head p {
  margin: 0;
}

.detail-hero__head h2 {
  font-size: 2.4rem;
}

.detail-hero__head p {
  margin-top: 8px;
  color: var(--sl-ink-soft);
}

.detail-hero__meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.detail-hero__meta article {
  padding: 14px;
  border-radius: 18px;
  border: 1px solid var(--sl-detail-meta-border);
  background: var(--sl-detail-meta-bg);
  box-shadow: var(--sl-detail-meta-shadow);
}

.detail-hero__meta span {
  display: block;
  color: var(--sl-detail-meta-label);
  font-size: 0.82rem;
  line-height: 1.2;
}

.detail-hero__meta strong {
  display: block;
  margin-top: 6px;
  color: var(--sl-detail-meta-value);
  line-height: 1.25;
}

.detail-hero__tools {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.detail-hero__attach {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
}

.detail-hero__shelf-note {
  margin: 8px 0 0;
  color: var(--sl-ink-soft);
  line-height: 1.6;
}

.detail-grid {
  align-items: start;
}

.detail-grid > * {
  grid-column: span 6;
}

.copy-block {
  display: grid;
  gap: 10px;
}

.copy-block__footer {
  display: flex;
  justify-content: flex-end;
}

.copy-block__trigger {
  padding-inline: 14px;
  font-size: 0.92rem;
}

.copy-block__body {
  color: var(--sl-ink-soft);
  line-height: 1.8;
}

.copy-block__body--preserve {
  white-space: pre-line;
}

.ai-reader-switch {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 14px;
}

.ai-reader-switch__item {
  min-width: 110px;
}

.ai-reader-switch__item--active {
  box-shadow: 0 10px 24px rgba(22, 40, 28, 0.14);
}

.similar-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

@media (max-width: 1200px) {
  .detail-grid > * {
    grid-column: span 12;
  }
}

@media (max-width: 900px) {
  .detail-hero {
    grid-template-columns: 1fr;
  }

  .detail-hero__meta,
  .detail-hero__tools {
    grid-template-columns: 1fr;
  }

  .detail-hero__attach {
    grid-template-columns: 1fr;
  }
}
</style>
