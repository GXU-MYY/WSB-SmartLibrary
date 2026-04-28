<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { getReadingRecords, getRecentBooks } from '@/api/book'
import { getPersonalStats } from '@/api/community'
import { recommendBooks } from '@/api/rag'
import EmptyState from '@/components/EmptyState.vue'
import LoadingState from '@/components/LoadingState.vue'
import MetricCard from '@/components/MetricCard.vue'
import SectionPanel from '@/components/SectionPanel.vue'
import { useRegisterPageRefresh } from '@/composables/usePageRefresh'
import type { BookCardModel, PersonalStats, ReadingRecord, RecentBook } from '@/types/models'
import { formatDateTime, initialsFromName, readingStatusLabel, resolvePictureUrl } from '@/utils/format'

const router = useRouter()

const READING_PAGE_SIZE = 5
const AI_BOOK_ROW_SIZE = 8

const loading = ref(false)
const recommendationLoading = ref(false)
const readingPage = ref(1)

const personalStats = ref<PersonalStats | null>(null)
const recentBooks = ref<RecentBook[]>([])
const readingRecords = ref<ReadingRecord[]>([])
const recommendedBooks = ref<BookCardModel[]>([])

const recommendationForm = reactive({
  query: '',
})

const displayedRecentBooks = computed(() => recentBooks.value.slice(0, 4))
const displayedRecommendedBooks = computed(() => recommendedBooks.value.slice(0, 20))
const aiPlaceholderSlots = computed(() => Array.from({ length: AI_BOOK_ROW_SIZE }, (_, index) => index))

const metricCards = computed(() => {
  const stats = personalStats.value

  return [
    {
      label: '藏书总数',
      value: stats?.owned.totalBooks ?? 0,
      hint: '仅统计个人拥有的图书，借出的仍算在内，借入的不计入。',
    },
    {
      label: '借入 / 借出',
      value: `${stats?.borrowed.borrowedIn ?? 0} / ${stats?.borrowed.borrowedOut ?? 0}`,
      hint: '只统计当前仍在借入和借出的图书，已归还的不计入。',
    },
    {
      label: '我的收藏',
      value: stats?.collected.totalCollected ?? 0,
      hint: '只统计你收藏的图书数量。',
    },
    {
      label: '被收藏数',
      value: stats?.owned.booksBeingCollected ?? 0,
      hint: '显示别人收藏你的图书的数量。',
    },
  ]
})

const readingTotalPages = computed(() =>
  Math.max(1, Math.ceil(readingRecords.value.length / READING_PAGE_SIZE)),
)

const pagedReadingRecords = computed(() => {
  const startIndex = (readingPage.value - 1) * READING_PAGE_SIZE
  return readingRecords.value.slice(startIndex, startIndex + READING_PAGE_SIZE)
})

const readingPlaceholderRows = computed(() =>
  Math.max(0, READING_PAGE_SIZE - pagedReadingRecords.value.length),
)

const getReadingStatusClass = (status: number) => {
  if (status === 1) {
    return 'reading-status-chip--want'
  }

  if (status === 2) {
    return 'reading-status-chip--reading'
  }

  if (status === 3) {
    return 'reading-status-chip--done'
  }

  return 'reading-status-chip--default'
}

const loadDashboard = async () => {
  loading.value = true

  try {
    const [statsResult, recentBooksResult, readingResult] = await Promise.allSettled([
      getPersonalStats(),
      getRecentBooks(),
      getReadingRecords(),
    ])

    if (statsResult.status === 'fulfilled') {
      personalStats.value = statsResult.value
    }

    if (recentBooksResult.status === 'fulfilled') {
      recentBooks.value = recentBooksResult.value.slice(0, 4)
    }

    if (readingResult.status === 'fulfilled' && Array.isArray(readingResult.value)) {
      readingRecords.value = readingResult.value
      if (readingPage.value > readingTotalPages.value) {
        readingPage.value = readingTotalPages.value
      }
    }
  } finally {
    loading.value = false
  }
}

const handleRecommend = async () => {
  if (!recommendationForm.query.trim()) {
    return
  }

  recommendationLoading.value = true

  try {
    const result = await recommendBooks(recommendationForm.query.trim(), 20)
    recommendedBooks.value = result.slice(0, 20).map((book) => ({
      id: book.id,
      title: book.title,
      coverUrl: book.coverUrl,
    }))
  } finally {
    recommendationLoading.value = false
  }
}

const changeReadingPage = (nextPage: number) => {
  readingPage.value = Math.min(Math.max(1, nextPage), readingTotalPages.value)
}

useRegisterPageRefresh(loadDashboard)

onMounted(loadDashboard)
</script>

<template>
  <div class="page-shell page-stack">
    <section class="summary-metric-grid">
      <MetricCard
        v-for="item in metricCards"
        :key="item.label"
        :label="item.label"
        :value="item.value"
        :hint="item.hint"
      />
    </section>

    <section class="page-grid home-grid">
      <SectionPanel title="最近整理的藏书" hint="最近录入或修改过的图书，方便继续补全信息。">
        <LoadingState v-if="loading && recentBooks.length === 0" />

        <div v-else class="compact-book-grid">
          <button
            v-for="book in displayedRecentBooks"
            :key="book.id"
            class="surface-card compact-book-card"
            type="button"
            @click="router.push(`/books/${book.id}`)"
          >
            <div class="compact-book-card__cover">
              <img
                v-if="resolvePictureUrl(book.coverUrl)"
                :src="resolvePictureUrl(book.coverUrl)"
                :alt="book.title"
                loading="lazy"
              />
              <div v-else class="compact-book-card__placeholder serif-title">
                {{ initialsFromName(book.title) }}
              </div>
            </div>
            <p class="compact-book-card__title">{{ book.title }}</p>
          </button>

          <EmptyState v-if="!loading && recentBooks.length === 0" title="你的书架还是空的">
            <button class="button button--primary" type="button" @click="router.push('/books')">
              去添加图书
            </button>
          </EmptyState>
        </div>
      </SectionPanel>

      <SectionPanel title="阅读轨迹" hint="最近调整过的阅读状态会显示在这里，帮助保持阅读节奏。">
        <LoadingState v-if="loading && readingRecords.length === 0" />

        <template v-else-if="readingRecords.length">
          <div class="reading-table-wrapper">
            <table class="reading-table">
              <thead>
                <tr>
                  <th>图书</th>
                  <th>状态</th>
                  <th>更新时间</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in pagedReadingRecords" :key="item.id">
                  <td class="reading-table__book">
                    <strong>{{ item.bookName || `图书 #${item.bookId}` }}</strong>
                  </td>
                  <td class="reading-table__status">
                    <span class="reading-status-chip" :class="getReadingStatusClass(item.readingStatus)">
                      {{ readingStatusLabel(item.readingStatus) }}
                    </span>
                  </td>
                  <td class="reading-table__time">{{ formatDateTime(item.updateTime || item.createTime) }}</td>
                </tr>
                <tr
                  v-for="index in readingPlaceholderRows"
                  :key="`placeholder-${index}`"
                  class="reading-table__placeholder"
                  aria-hidden="true"
                >
                  <td colspan="3">占位</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="reading-pagination">
            <p class="reading-pagination__info">
              第 {{ readingPage }} / {{ readingTotalPages }} 页 · 共 {{ readingRecords.length }} 条
            </p>
            <div class="inline-actions">
              <button
                class="button button--ghost"
                type="button"
                :disabled="readingPage <= 1"
                @click="changeReadingPage(readingPage - 1)"
              >
                上一页
              </button>
              <button
                class="button button--ghost"
                type="button"
                :disabled="readingPage >= readingTotalPages"
                @click="changeReadingPage(readingPage + 1)"
              >
                下一页
              </button>
            </div>
          </div>
        </template>

        <EmptyState v-else title="阅读轨迹还没开始" />
      </SectionPanel>

      <SectionPanel
        title="AI推荐"
        hint="输入主题、场景或读者画像，让 RAG 服务帮你找到方向接近的图书。"
      >
        <div class="field">
          <div class="recommend-row">
            <input
              id="recommend-query"
              v-model="recommendationForm.query"
              type="text"
              aria-label="AI推荐输入框"
              placeholder="试试：系统性思维、历史传记"
              @keyup.enter="handleRecommend"
            />
            <button
              class="button button--primary"
              type="button"
              :disabled="recommendationLoading"
              @click="handleRecommend"
            >
              {{ recommendationLoading ? '生成中...' : '生成推荐' }}
            </button>
          </div>
        </div>

        <div class="compact-book-grid ai-book-grid">
          <template v-if="displayedRecommendedBooks.length">
            <button
              v-for="book in displayedRecommendedBooks"
              :key="book.id"
              class="surface-card compact-book-card"
              type="button"
              @click="router.push({ name: 'book-preview', params: { id: book.id } })"
            >
              <div class="compact-book-card__cover">
                <img
                  v-if="resolvePictureUrl(book.coverUrl)"
                  :src="resolvePictureUrl(book.coverUrl)"
                  :alt="book.title"
                  loading="lazy"
                />
                <div v-else class="compact-book-card__placeholder serif-title">
                  {{ initialsFromName(book.title) }}
                </div>
              </div>
              <p class="compact-book-card__title">{{ book.title }}</p>
            </button>
          </template>

          <div
            v-else
            v-for="slot in aiPlaceholderSlots"
            :key="`ai-placeholder-${slot}`"
            class="surface-card compact-book-card compact-book-card--ghost"
            aria-hidden="true"
          />
        </div>
      </SectionPanel>
    </section>
  </div>
</template>

<style scoped>
.home-grid > *:nth-child(1),
.home-grid > *:nth-child(2) {
  grid-column: span 6;
}

.home-grid > *:nth-child(1) {
  grid-column: span 7;
}

.home-grid > *:nth-child(2) {
  grid-column: span 5;
}

.home-grid > *:nth-child(3) {
  grid-column: span 12;
}

.compact-book-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  align-items: stretch;
}

.compact-book-card {
  display: grid;
  grid-template-rows: auto minmax(2.7em, auto);
  gap: 10px;
  height: 100%;
  padding: 12px;
  border: 0;
  text-align: left;
  background: var(--sl-surface);
  color: inherit;
  cursor: pointer;
  transition: transform 180ms ease, box-shadow 180ms ease;
}

.compact-book-card:hover,
.compact-book-card:focus-visible {
  transform: translateY(-2px);
  box-shadow: 0 18px 34px rgba(31, 95, 107, 0.16);
}

.compact-book-card:focus-visible {
  outline: 3px solid rgba(31, 95, 107, 0.2);
  outline-offset: 4px;
}

.compact-book-card--ghost {
  visibility: hidden;
  pointer-events: none;
}

.compact-book-card__cover {
  overflow: hidden;
  aspect-ratio: 5 / 6;
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(31, 95, 107, 0.18), rgba(201, 119, 46, 0.24));
}

.compact-book-card__cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.compact-book-card__placeholder {
  display: grid;
  place-items: end start;
  width: 100%;
  height: 100%;
  padding: 16px;
  color: rgba(255, 255, 255, 0.92);
  font-size: 1.6rem;
}

.compact-book-card__title {
  margin: 0;
  min-height: 2.7em;
  font-size: 0.98rem;
  line-height: 1.35;
  color: var(--sl-ink);
  text-align: center;
  font-family: 'STSong', 'SimSun', serif;
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.field {
  display: grid;
  gap: 8px;
  margin-bottom: 16px;
}

.field label {
  font-weight: 600;
}

.field input[type='text'] {
  width: 100%;
  border: 1px solid var(--sl-line);
  border-radius: var(--sl-radius-md);
  padding: 12px 14px;
  background: var(--sl-input-bg);
  color: var(--sl-ink);
  transition: border-color 180ms ease, box-shadow 180ms ease;
}

.field input[type='text']:focus {
  border-color: rgba(31, 95, 107, 0.44);
  box-shadow: 0 0 0 4px rgba(31, 95, 107, 0.08);
  outline: none;
}

.recommend-row {
  display: grid;
  width: min(100%, 560px);
  grid-template-columns: minmax(0, 1fr) 120px;
  gap: 12px;
}

.ai-book-grid {
  grid-template-columns: repeat(8, minmax(0, 1fr));
}

.reading-table-wrapper {
  width: 100%;
  overflow-x: auto;
  border: 1px solid var(--sl-border-color);
  border-radius: 18px;
  background: var(--sl-soft-panel-bg);
}

.reading-table {
  width: 100%;
  min-width: 0;
  border-collapse: collapse;
  table-layout: fixed;
}

.reading-table th,
.reading-table td {
  padding: 8px 12px;
  text-align: left;
  border-bottom: 1px solid var(--sl-border-color);
  vertical-align: middle;
}

.reading-table th {
  color: var(--sl-brand-strong);
  font-size: 0.82rem;
  font-weight: 700;
  white-space: nowrap;
}

.reading-table td {
  color: var(--sl-ink-soft);
  line-height: 1.45;
}

.reading-table__book {
  width: 42%;
}

.reading-table__status {
  width: 18%;
}

.reading-table__time {
  width: 40%;
}

.reading-table td strong {
  display: block;
  color: var(--sl-ink);
  word-break: break-word;
}

.reading-table tbody tr:last-child td {
  border-bottom: none;
}

.reading-table__placeholder td {
  height: 36px;
  color: transparent;
}

.reading-status-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  background: rgba(31, 95, 107, 0.12);
  color: var(--sl-brand-strong);
  font-size: 0.84rem;
  font-weight: 600;
  white-space: nowrap;
}

.reading-status-chip--want {
  background: rgba(201, 119, 46, 0.16);
  color: #b46819;
}

.reading-status-chip--reading {
  background: rgba(31, 95, 107, 0.16);
  color: #1f5f6b;
}

.reading-status-chip--done {
  background: rgba(36, 117, 56, 0.16);
  color: #2f7a3a;
}

.reading-status-chip--default {
  background: rgba(98, 109, 127, 0.14);
  color: var(--sl-ink-soft);
}

.reading-pagination {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
}

.reading-pagination__info {
  margin: 0;
  color: var(--sl-ink-soft);
}

@media (max-width: 1080px) {
  .home-grid > * {
    grid-column: span 12 !important;
  }

  .ai-book-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .compact-book-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .recommend-row {
    grid-template-columns: 1fr;
  }

  .reading-pagination {
    flex-direction: column;
    align-items: stretch;
  }

  .reading-table__placeholder {
    display: none;
  }
}
</style>
