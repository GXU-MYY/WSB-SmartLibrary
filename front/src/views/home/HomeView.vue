<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { getMyBooks, getReadingRecords } from '@/api/book'
import { getPersonalStats } from '@/api/community'
import { recommendBooks } from '@/api/rag'
import { getTopRatedBooks } from '@/api/social'
import BookCard from '@/components/BookCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import LoadingState from '@/components/LoadingState.vue'
import MetricCard from '@/components/MetricCard.vue'
import PageIntro from '@/components/PageIntro.vue'
import SectionPanel from '@/components/SectionPanel.vue'
import type { BookCardModel, PersonalStats, ReadingRecord, TopRatedBook } from '@/types/models'
import { buildBookCard, formatDateTime, readingStatusLabel } from '@/utils/format'

const router = useRouter()

const loading = ref(false)
const recommendationLoading = ref(false)

const personalStats = ref<PersonalStats | null>(null)
const myBooks = ref<BookCardModel[]>([])
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
      detail: '你已经录入到私人书库中的图书数量。',
      tone: 'brand' as const,
    },
    {
      label: '借阅未归还',
      value: stats?.borrowed.unreturned ?? 0,
      detail: '需要继续跟进回收的借阅记录。',
      tone: 'accent' as const,
    },
    {
      label: '被收藏次数',
      value: stats?.owned.booksBeingCollected ?? 0,
      detail: '社区对你书库内容的关注热度。',
      tone: 'plain' as const,
    },
    {
      label: '我的收藏',
      value: stats?.collected.totalCollected ?? 0,
      detail: '你主动收藏的图书与书架内容。',
      tone: 'plain' as const,
    },
  ]
})

const loadDashboard = async () => {
  loading.value = true

  try {
    const [statsResult, booksResult, topRatedResult, readingResult] = await Promise.allSettled([
      getPersonalStats(),
      getMyBooks(),
      getTopRatedBooks(4),
      getReadingRecords(),
    ])

    if (statsResult.status === 'fulfilled') {
      personalStats.value = statsResult.value
    }

    if (booksResult.status === 'fulfilled') {
      myBooks.value = booksResult.value.books.slice(0, 4).map((book) =>
        buildBookCard({
          id: book.id,
          title: book.title,
          author: book.author,
          coverUrl: book.coverUrl,
          summary: book.summary,
          secondary: `${book.publisher || '出版社待补充'} · ${book.publishDate || '时间待补充'}`,
        }),
      )
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

onMounted(loadDashboard)
</script>

<template>
  <div class="page-shell page-stack">
    <PageIntro
      eyebrow="Desk Overview"
      title="你的纸感书库工作台"
      description="从藏书、借阅、社区反馈到 AI 推荐，把所有与阅读相关的动作收束成一张持续更新的桌面。"
    >
      <template #actions>
        <button class="button button--ghost" type="button" @click="loadDashboard">刷新数据</button>
        <button class="button button--primary" type="button" @click="router.push('/books')">
          去整理图书
        </button>
      </template>
    </PageIntro>

    <section class="page-grid metrics-grid">
      <MetricCard
        v-for="item in metricCards"
        :key="item.label"
        :label="item.label"
        :value="item.value"
        :detail="item.detail"
        :tone="item.tone"
      />
    </section>

    <section class="page-grid home-grid">
      <SectionPanel
        title="最近整理的藏书"
        description="最近录入或修改过的图书，方便你继续补全信息。"
      >
        <LoadingState v-if="loading && myBooks.length === 0" />
        <div v-else class="books-grid">
          <BookCard
            v-for="book in myBooks"
            :key="book.id"
            :book="book"
          >
            <template #actions>
              <button class="button button--ghost" type="button" @click="router.push(`/books/${book.id}`)">
                查看详情
              </button>
            </template>
          </BookCard>

          <EmptyState
            v-if="!loading && myBooks.length === 0"
            title="你的书架还是空的"
            description="先从 ISBN 自动补全或手动录入开始，把第一批藏书放上工作台。"
          >
            <button class="button button--primary" type="button" @click="router.push('/books')">
              去添加图书
            </button>
          </EmptyState>
        </div>
      </SectionPanel>

      <SectionPanel
        title="AI 选书助手"
        description="输入主题、使用场景或读者画像，让 RAG 服务帮你找方向接近的图书。"
      >
        <div class="field">
          <label for="recommend-query">推荐语句</label>
          <div class="recommend-row">
            <input
              id="recommend-query"
              v-model="recommendationForm.query"
              type="text"
              placeholder="例如：适合建立系统思维的非虚构图书"
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
          <BookCard
            v-for="book in recommendedBooks"
            :key="book.id"
            :book="book"
          >
            <template #actions>
              <button class="button button--ghost" type="button" @click="router.push(`/books/${book.id}`)">
                打开图书
              </button>
            </template>
          </BookCard>

          <EmptyState
            v-if="!recommendationLoading && recommendedBooks.length === 0"
            title="先告诉我你想读什么"
            description="推荐支持主题、用途、气质和读者层级，输入越具体越容易得到贴合结果。"
          />
        </div>
      </SectionPanel>

      <SectionPanel
        title="社区高分书目"
        description="来自评论模块的高分榜，适合快速发现值得跟进的新书。"
      >
        <div class="books-grid books-grid--compact">
          <BookCard
            v-for="item in topRatedBooks"
            :key="item.id"
            :book="{
              id: item.id,
              title: item.title,
              coverUrl: item.pic,
              secondary: `社区均分 ${item.stars}/5`,
              badge: '高分'
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

      <SectionPanel
        title="阅读轨迹"
        description="最近调整过的阅读状态会显示在这里，帮助你保持阅读节奏。"
      >
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
        <EmptyState
          v-else
          title="阅读轨迹还没有开始"
          description="去图书详情页设置想读、在读或已读，系统就会开始记录你的阅读推进。"
        />
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
  .books-grid,
  .books-grid--compact,
  .recommend-row {
    grid-template-columns: 1fr;
  }
}
</style>
