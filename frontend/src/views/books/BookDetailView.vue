<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import {
  addReadingRecord,
  borrowBook,
  getBookDetail,
  getReadingRecords,
  getBookShelves,
  getShelves,
  offShelf,
  onShelf,
  updateReadingRecord,
} from '@/api/book'
import { aggregateReviews, generateAiSummary, getAiSummary, getSimilarBooks } from '@/api/rag'
import {
  addCollect,
  addComment,
  deleteCollect,
  deleteComment,
  getBookComments,
  getMyBookCollects,
} from '@/api/social'
import BookCard from '@/components/BookCard.vue'
import { useRegisterPageRefresh } from '@/composables/usePageRefresh'
import EmptyState from '@/components/EmptyState.vue'
import LoadingState from '@/components/LoadingState.vue'
import PageIntro from '@/components/PageIntro.vue'
import SectionPanel from '@/components/SectionPanel.vue'
import type { Book, CollectBook, CommentItem, ReadingRecord, Shelf } from '@/types/models'
import {
  buildBookCard,
  formatCurrency,
  formatDate,
  formatDateTime,
  parseTagList,
  readingStatusLabel,
  resolvePictureUrl,
} from '@/utils/format'
import { notifyError, notifySuccess } from '@/utils/notify'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const summaryLoading = ref(false)
const reviewLoading = ref(false)
const commentLoading = ref(false)
const collectLoading = ref(false)
const borrowLoading = ref(false)
const shelfLoading = ref(false)

const book = ref<Book | null>(null)
const comments = ref<CommentItem[]>([])
const averageScore = ref(0)
const readingRecord = ref<ReadingRecord | null>(null)
const collectRecord = ref<CollectBook | null>(null)
const shelves = ref<Shelf[]>([])
const bookShelf = ref<Shelf | null>(null)
const similarBooks = ref<Book[]>([])
const aiSummary = ref('')
const reviewDigest = ref('')

const commentForm = reactive({
  comment: '',
  starRating: 5,
})

const borrowForm = reactive({
  borrow_name: '',
  borrowing_time: new Date().toISOString().slice(0, 10),
  borrow_type: 2,
})

const attachShelfId = ref(0)

const bookId = computed(() => Number(route.params.id))
const coverUrl = computed(() => resolvePictureUrl(book.value?.coverUrl))
const tagItems = computed(() => parseTagList(book.value?.label))
const attachedShelfIds = computed(() => (bookShelf.value?.id ? [bookShelf.value.id] : []))
const attachedShelfNames = computed(() => (bookShelf.value?.shelfName ? [bookShelf.value.shelfName] : []))
const isOnShelf = computed(() => Boolean(bookShelf.value) || Boolean(book.value?.isOnShelf))
const isSelectedShelfAttached = computed(
  () => attachShelfId.value > 0 && attachedShelfIds.value.includes(attachShelfId.value),
)
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
const collectButtonLabel = computed(() =>
  collectRecord.value ? '取消收藏' : '加入收藏',
)
const collectButtonIcon = computed(() => (collectRecord.value ? '★' : '☆'))

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
    const [bookResult, commentResult, readingResult, similarResult, collectResult, shelfResult, bookShelfResult] =
      await Promise.allSettled([
        getBookDetail(bookId.value),
        getBookComments(bookId.value),
        getReadingRecords(bookId.value),
        getSimilarBooks(bookId.value, 4),
        getMyBookCollects(),
        getShelves(),
        getBookShelves(bookId.value),
      ])

    if (bookResult.status === 'fulfilled') {
      book.value = bookResult.value
    }

    if (commentResult.status === 'fulfilled') {
      comments.value = commentResult.value.comments
      averageScore.value = commentResult.value.starMean
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
      collectRecord.value = collectResult.value.find((item) => item.bookId === bookId.value) || null
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

    try {
      aiSummary.value = await getAiSummary(bookId.value)
    } catch {
      aiSummary.value = ''
    }
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

const handleSubmitComment = async () => {
  if (!commentForm.comment.trim()) {
    notifyError('评论内容不能为空')
    return
  }

  commentLoading.value = true

  try {
    await addComment({
      bookId: bookId.value,
      comment: commentForm.comment,
      starRating: commentForm.starRating,
    })
    commentForm.comment = ''
    commentForm.starRating = 5
    const result = await getBookComments(bookId.value)
    comments.value = result.comments
    averageScore.value = result.starMean
    notifySuccess('评论已发布')
  } finally {
    commentLoading.value = false
  }
}

const handleDeleteComment = async (commentId: number) => {
  await deleteComment(commentId)
  const result = await getBookComments(bookId.value)
  comments.value = result.comments
  averageScore.value = result.starMean
  notifySuccess('评论已删除')
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
      collectRecord.value = collects.find((item) => item.bookId === bookId.value) || null
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

const handleGenerateSummary = async () => {
  summaryLoading.value = true

  try {
    aiSummary.value = await generateAiSummary(bookId.value)
    notifySuccess('AI 摘要已生成')
  } finally {
    summaryLoading.value = false
  }
}

const handleAggregateReviews = async () => {
  reviewLoading.value = true

  try {
    reviewDigest.value = await aggregateReviews(bookId.value)
    notifySuccess('聚合书评已生成')
  } finally {
    reviewLoading.value = false
  }
}

const handleBorrow = async () => {
  if (!book.value) {
    return
  }

  if (!borrowForm.borrow_name.trim()) {
    notifyError('请填写借阅对象')
    return
  }

  borrowLoading.value = true

  try {
    await borrowBook({
      book_id: book.value.id,
      borrow_name: borrowForm.borrow_name,
      borrowing_time: borrowForm.borrowing_time,
      borrow_type: borrowForm.borrow_type,
    })
    notifySuccess('借阅记录已创建')
    router.push('/borrow')
  } finally {
    borrowLoading.value = false
  }
}

const handleAttachShelf = async () => {
  if (!book.value || !attachShelfId.value) {
    notifyError('请先选择目标书架')
    return
  }

  shelfLoading.value = true

  try {
    await onShelf({
      book_id: book.value.id,
      shelf_id: attachShelfId.value,
    })
    notifySuccess('图书已加入书架')
    await loadPage()
  } finally {
    shelfLoading.value = false
  }
}

const handleShelfAction = async () => {
  if (!book.value || !attachShelfId.value) {
    notifyError('请选择目标书架')
    return
  }

  shelfLoading.value = true

  try {
    if (isSelectedShelfAttached.value) {
      await offShelf({
        book_id: book.value.id,
        shelf_id: attachShelfId.value,
      })
      notifySuccess('图书已下架', '这本书已从当前书架移出。')
    } else {
      await onShelf({
        book_id: book.value.id,
        shelf_id: attachShelfId.value,
      })
      notifySuccess('图书已上架', '这本书已经放入选定书架。')
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
      :description="book?.summary || '查看图书元数据、评论反馈、阅读状态、AI 摘要和相似书籍。'"
    >
      <template #actions>
        <button
          class="button button--secondary detail-page-action detail-page-action--favorite"
          type="button"
          :class="{ 'detail-page-action--active': collectRecord }"
          :disabled="collectLoading"
          :aria-label="collectButtonLabel"
          :title="collectButtonLabel"
          @click="toggleCollect"
        >
          {{ collectRecord ? '取消收藏' : '加入收藏' }}
        </button>
        <button
          class="button button--ghost detail-page-action"
          type="button"
          aria-label="返回上一页"
          title="返回上一页"
          @click="handleGoBack"
        >
          <span aria-hidden="true" class="detail-page-action__icon">↩</span>
        </button>
      </template>
    </PageIntro>

    <LoadingState v-if="loading && !book" title="正在装载图书详情" />

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
              <span v-if="book?.isBorrowed" class="badge badge--accent">借阅中</span>
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
            <article>
              <span>评分</span>
              <strong>{{ averageScore }}/5</strong>
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
              <div class="detail-hero__attach">
                <select v-model.number="attachShelfId">
                  <option :value="0" disabled hidden>选择书架</option>
                  <option v-for="item in shelves" :key="item.id" :value="item.id">{{ item.shelfName }}</option>
                </select>
                <button class="button button--ghost" type="button" :disabled="shelfLoading" @click="handleShelfAction">
                  {{ shelfActionText }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section class="page-grid detail-grid">
        <SectionPanel
          title="AI 阅读助手"
          hint="这里集中展示摘要、聚合书评与延展阅读结果。"
        >
          <div class="inline-actions">
            <button class="button button--secondary" type="button" :disabled="summaryLoading" @click="handleGenerateSummary">
              {{ summaryLoading ? '生成中...' : '生成 AI 摘要' }}
            </button>
            <button class="button button--ghost" type="button" :disabled="reviewLoading" @click="handleAggregateReviews">
              {{ reviewLoading ? '聚合中...' : '聚合网络书评' }}
            </button>
          </div>

          <div class="copy-block">
            <h3>摘要</h3>
            <p>{{ aiSummary || '还没有生成摘要，你可以手动触发一次。' }}</p>
          </div>

          <div class="copy-block">
            <h3>聚合书评</h3>
            <p>{{ reviewDigest || '这里会收集面向这本书的外部评论概览。' }}</p>
          </div>
        </SectionPanel>

        <SectionPanel
          title="借阅登记"
          hint="当你要把这本书借出或借入时，可以在这里直接登记。"
        >
          <div class="field">
            <label>借阅对象</label>
            <input v-model="borrowForm.borrow_name" type="text" placeholder="填写借阅对象姓名" />
          </div>
          <div class="field-grid">
            <div class="field">
              <label>借阅类型</label>
              <select v-model.number="borrowForm.borrow_type">
                <option :value="1">借入</option>
                <option :value="2">借出</option>
              </select>
            </div>
            <div class="field">
              <label>借阅日期</label>
              <input v-model="borrowForm.borrowing_time" type="date" />
            </div>
          </div>
          <button class="button button--primary" type="button" :disabled="borrowLoading" @click="handleBorrow">
            {{ borrowLoading ? '登记中...' : '保存借阅记录' }}
          </button>
        </SectionPanel>

        <SectionPanel
          title="评论与评分"
          hint="书评既是你的阅读回声，也是社区关系的一部分。"
        >
          <div class="field">
            <label>评分</label>
            <select v-model.number="commentForm.starRating">
              <option :value="5">5 分</option>
              <option :value="4">4 分</option>
              <option :value="3">3 分</option>
              <option :value="2">2 分</option>
              <option :value="1">1 分</option>
            </select>
          </div>

          <div class="field">
            <label>评论内容</label>
            <textarea v-model="commentForm.comment" placeholder="写下你对这本书的感受、摘录或判断。" />
          </div>

          <button class="button button--secondary" type="button" :disabled="commentLoading" @click="handleSubmitComment">
            {{ commentLoading ? '提交中...' : '发布评论' }}
          </button>

          <ul v-if="comments.length" class="comment-list list-reset">
            <li v-for="item in comments" :key="item.id" class="comment-list__item">
              <div class="split-actions">
                <div>
                  <strong>用户 {{ item.userId }}</strong>
                  <p>{{ formatDateTime(item.comTime) }} · {{ item.stars }} 分</p>
                </div>
                <button class="button button--ghost" type="button" @click="handleDeleteComment(item.id)">删除</button>
              </div>
              <p class="comment-list__body">{{ item.comment }}</p>
            </li>
          </ul>
          <EmptyState v-else title="还没有评论" />
        </SectionPanel>

        <SectionPanel
          title="相似图书"
          hint="来自 RAG 相似检索结果，适合继续扩展阅读链路。"
        >
          <div class="similar-grid">
            <BookCard
              v-for="item in similarBooks"
              :key="item.id"
              :book="
                buildBookCard({
                  id: item.id,
                  title: item.title,
                  author: item.author,
                  coverUrl: item.coverUrl,
                  summary: item.summary,
                  badge: '相似'
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
  min-width: 46px;
  min-height: 46px;
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 1.18rem;
  line-height: 1;
}

.detail-page-action__icon {
  font-family: 'Segoe UI Symbol', 'Apple Symbols', 'Noto Sans Symbols 2', sans-serif;
  font-size: 1.26rem;
  line-height: 1;
}

.detail-page-action--favorite {
  position: relative;
  font-size: 0;
}

.detail-page-action--favorite::before {
  content: '☆';
  font-size: 1.32rem;
  line-height: 1;
  color: currentColor;
}

.detail-page-action--favorite.detail-page-action--active::before {
  content: '★';
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
  grid-template-columns: repeat(4, minmax(0, 1fr));
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

.detail-hero__shelf-status {
  margin: 0 0 10px;
  color: var(--sl-ink-soft);
  font-size: 0.9rem;
  line-height: 1.5;
}

.detail-grid > * {
  grid-column: span 6;
}

.copy-block {
  display: grid;
  gap: 10px;
}

.copy-block h3,
.copy-block p {
  margin: 0;
}

.copy-block p {
  color: var(--sl-ink-soft);
  line-height: 1.8;
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.comment-list {
  display: grid;
  gap: 14px;
}

.comment-list__item {
  padding: 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.58);
}

.comment-list__item p {
  margin: 0;
}

.comment-list__item .split-actions p {
  margin-top: 6px;
  color: var(--sl-ink-soft);
}

.comment-list__body {
  margin-top: 12px !important;
  line-height: 1.8;
  color: var(--sl-ink-soft);
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
  .detail-hero__tools,
  .field-grid,
  .similar-grid {
    grid-template-columns: 1fr;
  }

  .detail-hero__attach {
    grid-template-columns: 1fr;
  }
}

@media (min-width: 901px) {
  .detail-hero__meta article {
    padding: 11px 14px;
  }

  .detail-hero__meta strong {
    margin-top: 4px;
    font-size: 0.98rem;
  }
}
</style>
