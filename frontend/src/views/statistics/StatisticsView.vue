<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

import { getBookRank, getBorrowSummary, getCollectSummary, getPersonalStats, getUserRank } from '@/api/community'
import { useRegisterPageRefresh } from '@/composables/usePageRefresh'
import CategoryPieChart from '@/components/CategoryPieChart.vue'
import EmptyState from '@/components/EmptyState.vue'
import LoadingState from '@/components/LoadingState.vue'
import MetricCard from '@/components/MetricCard.vue'
import PageIntro from '@/components/PageIntro.vue'
import SectionPanel from '@/components/SectionPanel.vue'
import type { BookRank, BorrowStats, CollectStats, PersonalStats, UserRank } from '@/types/models'
import { normalizePage } from '@/utils/format'

const loading = ref(false)
const rankLoading = ref(false)
const personalStats = ref<PersonalStats | null>(null)
const borrowStats = ref<BorrowStats | null>(null)
const collectStats = ref<CollectStats | null>(null)
const bookRank = ref<BookRank[]>([])
const userRank = ref<UserRank[]>([])
const RANK_PAGE_SIZE = 5
const bookRankPagination = reactive({
  current: 1,
  size: RANK_PAGE_SIZE,
  total: 0,
  pages: 1,
})
const userRankPagination = reactive({
  current: 1,
  size: RANK_PAGE_SIZE,
  total: 0,
  pages: 1,
})

const overviewCards = computed(() => {
  const stats = personalStats.value
  return [
    { label: '总藏书', value: stats?.owned.totalBooks ?? 0, hint: '当前账号持有的图书总量。' },
    { label: '借阅总数', value: stats?.borrowed.totalBorrowed ?? 0, hint: '参与过的借阅总次数。' },
    { label: '未归还', value: stats?.borrowed.unreturned ?? 0, hint: '仍在流转中的借阅条目。' },
    { label: '收藏总数', value: stats?.collected.totalCollected ?? 0, hint: '主动收藏下来的内容数量。' },
  ]
})

const ownedDistributionItems = computed(() =>
  personalStats.value?.owned.booksByCategory?.map((item) => ({
    category: item.category,
    value: item.count ?? 0,
  })) ?? [],
)
const borrowDistributionItems = computed(() =>
  borrowStats.value?.classifyList?.map((item) => ({
    category: item.category,
    value: item.total ?? 0,
  })) ?? [],
)
const collectDistributionItems = computed(() =>
  collectStats.value?.classifyList?.map((item) => ({
    category: item.category,
    value: item.collect ?? 0,
  })) ?? [],
)

const updateBookRankPagination = (pageData: ReturnType<typeof normalizePage<BookRank>>) => {
  bookRankPagination.current = pageData.current
  bookRankPagination.size = pageData.size
  bookRankPagination.total = pageData.total
  bookRankPagination.pages = pageData.pages
}

const updateUserRankPagination = (pageData: ReturnType<typeof normalizePage<UserRank>>) => {
  userRankPagination.current = pageData.current
  userRankPagination.size = pageData.size
  userRankPagination.total = pageData.total
  userRankPagination.pages = pageData.pages
}

const loadBookRank = async (page = bookRankPagination.current) => {
  const result = await getBookRank({ page, page_size: RANK_PAGE_SIZE })
  const normalized = normalizePage(result)
  bookRank.value = normalized.records
  updateBookRankPagination(normalized)
}

const loadUserRank = async (page = userRankPagination.current) => {
  const result = await getUserRank({ page, page_size: RANK_PAGE_SIZE })
  const normalized = normalizePage(result)
  userRank.value = normalized.records
  updateUserRankPagination(normalized)
}

const loadStatistics = async () => {
  loading.value = true

  try {
    const [personal, borrow, collect] = await Promise.all([
      getPersonalStats(),
      getBorrowSummary('mine'),
      getCollectSummary('mine'),
    ])

    personalStats.value = personal
    borrowStats.value = borrow
    collectStats.value = collect
    await Promise.all([loadBookRank(bookRankPagination.current), loadUserRank(userRankPagination.current)])
  } finally {
    loading.value = false
  }
}

const handleBookRankPageChange = async (page: number) => {
  if (page < 1 || page > bookRankPagination.pages || page === bookRankPagination.current) {
    return
  }

  rankLoading.value = true
  try {
    await loadBookRank(page)
  } finally {
    rankLoading.value = false
  }
}

const handleUserRankPageChange = async (page: number) => {
  if (page < 1 || page > userRankPagination.pages || page === userRankPagination.current) {
    return
  }

  rankLoading.value = true
  try {
    await loadUserRank(page)
  } finally {
    rankLoading.value = false
  }
}

useRegisterPageRefresh(loadStatistics)

onMounted(loadStatistics)
</script>

<template>
  <div class="page-shell page-stack">
    <PageIntro
      eyebrow="Statistics"
      title="把阅读、借阅和收藏转成能被观察的趋势"
      description="统计页不是为了装点后台，而是帮你判断哪些分类增长快、哪些内容更有公共吸引力。"
    />

    <section class="summary-metric-grid">
      <MetricCard
        v-for="item in overviewCards"
        :key="item.label"
        :label="item.label"
        :value="item.value"
        :hint="item.hint"
      />
    </section>

    <section class="page-grid statistics-layout">
      <SectionPanel
        title="分布统计"
        hint="按中图分类号首字母聚合，用三个饼图对比个人藏书、借阅和收藏的分类结构。"
      >
        <LoadingState v-if="loading && !personalStats" />
        <div v-else class="distribution-pie-grid">
          <CategoryPieChart title="我的图书分布" :items="ownedDistributionItems" />
          <CategoryPieChart title="借阅分布" :items="borrowDistributionItems" />
          <CategoryPieChart title="收藏分布" :items="collectDistributionItems" />
        </div>
      </SectionPanel>

      <SectionPanel
        title="热门图书排行"
        hint="哪些书最容易被收藏，也最值得在社区里二次扩散。"
      >
        <div v-if="bookRank.length" class="rank-table-stack">
          <div class="table-shell">
            <table class="table rank-table">
              <thead>
                <tr>
                  <th>排名</th>
                  <th>图书</th>
                  <th>收藏数</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in bookRank" :key="item.bookId">
                  <td>
                    <span class="rank-badge">#{{ item.ranking }}</span>
                  </td>
                  <td>
                    <strong>{{ item.title }}</strong>
                  </td>
                  <td>{{ item.collectCount }}</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="pagination-bar">
            <p class="pagination-bar__info">
              第 {{ bookRankPagination.current }} / {{ Math.max(bookRankPagination.pages, 1) }} 页 · 共
              {{ bookRankPagination.total }} 条
            </p>
            <div class="inline-actions">
              <button
                class="button button--ghost"
                type="button"
                :disabled="bookRankPagination.current <= 1 || rankLoading"
                @click="handleBookRankPageChange(bookRankPagination.current - 1)"
              >
                上一页
              </button>
              <button
                class="button button--secondary"
                type="button"
                :disabled="bookRankPagination.current >= bookRankPagination.pages || rankLoading"
                @click="handleBookRankPageChange(bookRankPagination.current + 1)"
              >
                下一页
              </button>
            </div>
          </div>
        </div>
        <EmptyState v-else title="图书排行还未生成" />
      </SectionPanel>

      <SectionPanel
        title="用户排行"
        hint="从用户维度看，谁的书库已经形成了更明显的规模。"
      >
        <div v-if="userRank.length" class="rank-table-stack">
          <div class="table-shell">
            <table class="table rank-table">
              <thead>
                <tr>
                  <th>排名</th>
                  <th>用户</th>
                  <th>书库规模</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in userRank" :key="item.id">
                  <td>
                    <span class="rank-badge">#{{ item.ranking }}</span>
                  </td>
                  <td>
                    <strong>{{ item.nickName || item.userName || `用户 ${item.id}` }}</strong>
                  </td>
                  <td>{{ item.bookCount }} 本</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="pagination-bar">
            <p class="pagination-bar__info">
              第 {{ userRankPagination.current }} / {{ Math.max(userRankPagination.pages, 1) }} 页 · 共
              {{ userRankPagination.total }} 条
            </p>
            <div class="inline-actions">
              <button
                class="button button--ghost"
                type="button"
                :disabled="userRankPagination.current <= 1 || rankLoading"
                @click="handleUserRankPageChange(userRankPagination.current - 1)"
              >
                上一页
              </button>
              <button
                class="button button--secondary"
                type="button"
                :disabled="userRankPagination.current >= userRankPagination.pages || rankLoading"
                @click="handleUserRankPageChange(userRankPagination.current + 1)"
              >
                下一页
              </button>
            </div>
          </div>
        </div>
        <EmptyState v-else title="用户排行还未生成" />
      </SectionPanel>
    </section>
  </div>
</template>

<style scoped>
.statistics-layout {
  align-items: start;
}

.statistics-layout > *:nth-child(1),
.statistics-layout > *:nth-child(2),
.statistics-layout > *:nth-child(3) {
  grid-column: span 12;
}

.statistics-layout > *:nth-child(2),
.statistics-layout > *:nth-child(3) {
  grid-column: span 6;
}

.distribution-pie-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  min-width: 0;
}

.distribution-pie-grid > * {
  min-width: 0;
}

.rank-table-stack {
  display: grid;
  gap: 14px;
}

.rank-table th:first-child,
.rank-table td:first-child {
  width: 86px;
}

.rank-table th:last-child,
.rank-table td:last-child {
  width: 110px;
  text-align: right;
}

.rank-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 44px;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(31, 95, 107, 0.1);
  color: var(--sl-brand-strong);
  font-weight: 800;
}

.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.pagination-bar__info {
  margin: 0;
  color: var(--sl-ink-soft);
}

@media (max-width: 1200px) {
  .statistics-layout > * {
    grid-column: span 12 !important;
  }
}

@media (max-width: 720px) {
  .distribution-pie-grid {
    grid-template-columns: 1fr;
  }

  .pagination-bar {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
