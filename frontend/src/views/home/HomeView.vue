<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { getReadingRecords, getRecentBooks } from '@/api/book'
import { getPersonalStats } from '@/api/community'
import { recommendBooks } from '@/api/rag'
import { getTopRatedBooks } from '@/api/social'
import BookCard from '@/components/BookCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import LoadingState from '@/components/LoadingState.vue'
import MetricCard from '@/components/MetricCard.vue'
import SectionPanel from '@/components/SectionPanel.vue'
import { useRegisterPageRefresh } from '@/composables/usePageRefresh'
import type { BookCardModel, PersonalStats, ReadingRecord, RecentBook, TopRatedBook } from '@/types/models'
import { buildBookCard, formatDateTime, initialsFromName, readingStatusLabel, resolvePictureUrl } from '@/utils/format'

const router = useRouter()

const loading = ref(false)
const recommendationLoading = ref(false)

const personalStats = ref<PersonalStats | null>(null)
const recentBooks = ref<RecentBook[]>([])
const topRatedBooks = ref<TopRatedBook[]>([])
const readingRecords = ref<ReadingRecord[]>([])
const recommendedBooks = ref<BookCardModel[]>([])

const recommendationForm = reactive({
  query: '',
})

const metricCards = computed(() => {
  const stats = personalStats.value

  return [
    {
      label: '藏书总数',
      value: stats?.owned.totalBooks ?? 0,
      hint: '你已经录入到私人书库中的图书数量。',
      tone: 'brand' as const,
    },
    {
      label: '借阅未归还',
      value: stats?.borrowed.unreturned ?? 0,
      hint: '需要继续跟进回收的借阅记录。',
      tone: 'accent' as const,
    },
    {
      label: '被收藏次数',
      value: stats?.owned.booksBeingCollected ?? 0,
      hint: '社区对你书库内容的关注热度。',
      tone: 'plain' as const,
    },
    {
      label: '我的收藏',
      value: stats?.collected.totalCollected ?? 0,
      hint: '你主动收藏的图书与书架内容。',
      tone: 'plain' as const,
    },
  ]
})

const loadDashboard = async () => {
  loading.value = true

  try {
    const [statsResult, recentBooksResult, topRatedResult, readingResult] = await Promise.allSettled([
      getPersonalStats(),
      getRecentBooks(),
      getTopRatedBooks(4),
      getReadingRecords(),
    ])

    if (statsResult.status === 'fulfilled') {
      personalStats.value = statsResult.value
    }

    if (recentBooksResult.status === 'fulfilled') {
      recentBooks.value = recentBooksResult.value
    }

    if (topRatedResult.status === 'fulfilled') {
      topRatedBooks.value = topRatedResult.value
    }

    if (readingResult.status === 'fulfilled' && Array.isArray(readingResult.value)) {
      readingRecords.value = readingResult.value.slice(0, 5)
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
    const result = await recommendBooks(recommendationForm.query, 4)
    recommendedBooks.value = result.map((book) =>
      buildBookCard({
        id: book.id,
        title: book.title,
        author: book.author,
        coverUrl: book.coverUrl,
        summary: book.summary,
        secondary: book.keyword || 'AI 推荐结果',
        badge: 'AI 推荐',
      }),
    )
  } finally {
    recommendationLoading.value = false
  }
}

useRegisterPageRefresh(loadDashboard)

onMounted(loadDashboard)
</script>

<template>
  <div class="page-shell page-stack">
    <section class="page-grid metrics-grid">
      <MetricCard
        v-for="item in metricCards"
        :key="item.label"
        :label="item.label"
        :value="item.value"
        :hint="item.hint"
        :tone="item.tone"
      />
    </section>

    <section class="page-grid home-grid">
      <SectionPanel title="最近整理的藏书" hint="最近录入或修改过的图书，方便你继续补全信息。">
        <LoadingState v-if="loading && recentBooks.length === 0" />
        <div v-else class="recent-books-grid">
          <button
            v-for="book in recentBooks"
            :key="book.id"
            class="surface-card recent-book-card"
            type="button"
            @click="router.push(`/books/${book.id}`)"
          >
            <div class="recent-book-card__cover">
              <img v-if="resolvePictureUrl(book.coverUrl)" :src="resolvePictureUrl(book.coverUrl)" :alt="book.title" loading="lazy" />
              <div v-else class="recent-book-card__placeholder serif-title">{{ initialsFromName(book.title) }}</div>
            </div>
            <p class="recent-book-card__title">{{ book.title }}</p>
          </button>

          <EmptyState v-if="!loading && recentBooks.length === 0" title="你的书架还是空的">
            <button class="button button--primary" type="button" @click="router.push('/books')">
              去添加图书
            </button>
          </EmptyState>
        </div>
      </SectionPanel>

      <SectionPanel
        title="AI 选书助手"
        hint="输入主题、使用场景或读者画像，让 RAG 服务帮你找方向接近的图书。"
      >
        <div class="field">
          <label for="recommend-query">推荐语句</label>
          <div class="recommend-row">
            <input
              id="recommend-query"
              v-model="recommendationForm.query"
              type="text"
              placeholder="例如：适合建立系统性思维的非虚构图书"
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

        <div class="books-grid">
          <BookCard v-for="book in recommendedBooks" :key="book.id" :book="book">
            <template #actions>
              <button class="button button--ghost" type="button" @click="router.push(`/books/${book.id}`)">
                打开图书
              </button>
            </template>
          </BookCard>

          <EmptyState v-if="!recommendationLoading && recommendedBooks.length === 0" title="先告诉我你想读什么" />
        </div>
      </SectionPanel>

      <SectionPanel title="社区高分书目" hint="来自评论模块的高分榜，适合快速发现值得跟进的新书。">
        <div class="books-grid books-grid--compact">
          <BookCard
            v-for="item in topRatedBooks"
            :key="item.id"
            :book="{
              id: item.id,
              title: item.title,
              coverUrl: item.pic,
              secondary: `社区均分 ${item.stars}/5`,
              badge: '高分',
            }"
          >
            <template #actions>
              <button class="button button--ghost" type="button" @click="router.push(`/books/${item.id}`)">
                去看看
              </button>
            </template>
          </BookCard>
        </div>
      </SectionPanel>

      <SectionPanel title="阅读轨迹" hint="最近调整过的阅读状态会显示在这里，帮助你保持阅读节奏。">
        <LoadingState v-if="loading && readingRecords.length === 0" />
        <ul v-else-if="readingRecords.length" class="timeline list-reset">
          <li v-for="item in readingRecords" :key="item.id" class="timeline__item">
            <span class="status-dot" :class="{ 'status-dot--warm': item.readingStatus !== 3 }" />
            <div>
              <strong>{{ item.bookName || `图书 #${item.bookId}` }}</strong>
              <p>{{ readingStatusLabel(item.readingStatus) }} · {{ formatDateTime(item.updateTime || item.createTime) }}</p>
            </div>
          </li>
        </ul>
        <EmptyState v-else title="阅读轨迹还没开始" />
      </SectionPanel>
    </section>
  </div>
</template>

<style scoped>
.metrics-grid > * {
  grid-column: span 3;
}

.home-grid > *:nth-child(1),
.home-grid > *:nth-child(2) {
  grid-column: span 6;
}

.home-grid > *:nth-child(3),
.home-grid > *:nth-child(4) {
  grid-column: span 6;
}

.books-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.recent-books-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.recent-book-card {
  display: grid;
  gap: 10px;
  padding: 12px;
  border: 0;
  text-align: left;
  background: var(--sl-surface);
  color: inherit;
  cursor: pointer;
  transition: transform 180ms ease, box-shadow 180ms ease;
}

.recent-book-card:hover,
.recent-book-card:focus-visible {
  transform: translateY(-2px);
  box-shadow: 0 18px 34px rgba(31, 95, 107, 0.16);
}

.recent-book-card:focus-visible {
  outline: 3px solid rgba(31, 95, 107, 0.2);
  outline-offset: 4px;
}

.recent-book-card__cover {
  overflow: hidden;
  aspect-ratio: 5 / 6;
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(31, 95, 107, 0.18), rgba(201, 119, 46, 0.24));
}

.recent-book-card__cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.recent-book-card__placeholder {
  display: grid;
  place-items: end start;
  width: 100%;
  height: 100%;
  padding: 16px;
  color: rgba(255, 255, 255, 0.92);
  font-size: 1.6rem;
}

.recent-book-card__title {
  margin: 0;
  font-size: 0.98rem;
  line-height: 1.35;
  color: var(--sl-ink);
  text-align: center;
  font-family: "STSong", "SimSun", serif;
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.books-grid--compact {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.recommend-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 150px;
  gap: 12px;
}

.timeline {
  display: grid;
  gap: 14px;
}

.timeline__item {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 12px;
  align-items: start;
}

.timeline__item p {
  margin: 6px 0 0;
  color: var(--sl-ink-soft);
}

@media (max-width: 1080px) {
  .metrics-grid > *,
  .home-grid > * {
    grid-column: span 12 !important;
  }
}

@media (max-width: 720px) {
  .recent-books-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .books-grid,
  .books-grid--compact,
  .recommend-row {
    grid-template-columns: 1fr;
  }
}
</style>
