<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import { getBookRank, getBorrowSummary, getCollectSummary, getPersonalStats, getUserRank } from '@/api/community'
import { useRegisterPageRefresh } from '@/composables/usePageRefresh'
import EmptyState from '@/components/EmptyState.vue'
import LoadingState from '@/components/LoadingState.vue'
import MetricCard from '@/components/MetricCard.vue'
import PageIntro from '@/components/PageIntro.vue'
import SectionPanel from '@/components/SectionPanel.vue'
import type { BookRank, BorrowStats, CollectStats, PersonalStats, UserRank } from '@/types/models'
import { normalizePage } from '@/utils/format'

const loading = ref(false)
const personalStats = ref<PersonalStats | null>(null)
const borrowStats = ref<BorrowStats | null>(null)
const collectStats = ref<CollectStats | null>(null)
const bookRank = ref<BookRank[]>([])
const userRank = ref<UserRank[]>([])

const overviewCards = computed(() => {
  const stats = personalStats.value
  return [
    { label: '总藏书', value: stats?.owned.totalBooks ?? 0, hint: '当前账号持有的图书总量。', tone: 'brand' as const },
    { label: '借阅总数', value: stats?.borrowed.totalBorrowed ?? 0, hint: '参与过的借阅总次数。', tone: 'plain' as const },
    { label: '未归还', value: stats?.borrowed.unreturned ?? 0, hint: '仍在流转中的借阅条目。', tone: 'accent' as const },
    { label: '收藏总数', value: stats?.collected.totalCollected ?? 0, hint: '主动收藏下来的内容数量。', tone: 'plain' as const },
  ]
})

const loadStatistics = async () => {
  loading.value = true

  try {
    const [personal, borrow, collect, books, users] = await Promise.all([
      getPersonalStats(),
      getBorrowSummary('mine'),
      getCollectSummary('mine'),
      getBookRank({ page: 1, page_size: 8 }),
      getUserRank({ page: 1, page_size: 8 }),
    ])

    personalStats.value = personal
    borrowStats.value = borrow
    collectStats.value = collect
    bookRank.value = normalizePage(books).records
    userRank.value = normalizePage(users).records
  } finally {
    loading.value = false
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

    <section class="page-grid metrics-grid">
      <MetricCard
        v-for="item in overviewCards"
        :key="item.label"
        :label="item.label"
        :value="item.value"
        :hint="item.hint"
        :tone="item.tone"
      />
    </section>

    <section class="page-grid statistics-layout">
      <SectionPanel
        title="我的分类分布"
        hint="从个人书库视角观察，哪些类型已经累积得比较厚。"
      >
        <LoadingState v-if="loading && !personalStats" />
        <div v-else-if="personalStats?.owned.booksByCategory?.length" class="bar-list">
          <article v-for="item in personalStats.owned.booksByCategory" :key="item.category" class="bar-list__item">
            <div class="split-actions">
              <strong>{{ item.category }}</strong>
              <span>{{ item.count }}</span>
            </div>
            <div class="bar-track">
              <div class="bar-track__fill" :style="{ width: `${Math.min(item.count * 14, 100)}%` }" />
            </div>
          </article>
        </div>
        <EmptyState v-else title="还没有形成分类分布" />
      </SectionPanel>

      <SectionPanel
        title="借阅与收藏结构"
        hint="两个视角一起看，更容易判断哪些书在流动，哪些书在沉淀。"
      >
        <div class="mini-panels">
          <article class="surface-card mini-panel">
            <h3>借阅分类</h3>
            <div v-if="borrowStats?.classifyList?.length" class="bar-list">
              <article v-for="item in borrowStats.classifyList" :key="item.category" class="bar-list__item">
                <div class="split-actions">
                  <strong>{{ item.category }}</strong>
                  <span>{{ item.total }}</span>
                </div>
                <div class="bar-track">
                  <div class="bar-track__fill bar-track__fill--warm" :style="{ width: `${Math.min(item.total * 18, 100)}%` }" />
                </div>
              </article>
            </div>
          </article>

          <article class="surface-card mini-panel">
            <h3>收藏分类</h3>
            <div v-if="collectStats?.classifyList?.length" class="bar-list">
              <article v-for="item in collectStats.classifyList" :key="item.category" class="bar-list__item">
                <div class="split-actions">
                  <strong>{{ item.category }}</strong>
                  <span>{{ item.collect }}</span>
                </div>
                <div class="bar-track">
                  <div class="bar-track__fill" :style="{ width: `${Math.min(item.collect * 22, 100)}%` }" />
                </div>
              </article>
            </div>
          </article>
        </div>
      </SectionPanel>

      <SectionPanel
        title="热门图书排行"
        hint="哪些书最容易被收藏，也最值得在社区里二次扩散。"
      >
        <div v-if="bookRank.length" class="rank-list">
          <article v-for="item in bookRank" :key="item.bookId" class="rank-list__item">
            <strong>#{{ item.ranking }} {{ item.title }}</strong>
            <p>收藏 {{ item.collectCount }} 次</p>
          </article>
        </div>
        <EmptyState v-else title="图书排行还未生成" />
      </SectionPanel>

      <SectionPanel
        title="用户排行"
        hint="从用户维度看，谁的书库已经形成了更明显的规模。"
      >
        <div v-if="userRank.length" class="rank-list">
          <article v-for="item in userRank" :key="item.id" class="rank-list__item">
            <strong>#{{ item.ranking }} {{ item.nickName || item.userName || `用户 ${item.id}` }}</strong>
            <p>书库规模 {{ item.bookCount }} 本</p>
          </article>
        </div>
        <EmptyState v-else title="用户排行还未生成" />
      </SectionPanel>
    </section>
  </div>
</template>

<style scoped>
.metrics-grid > * {
  grid-column: span 3;
}

.statistics-layout > *:nth-child(1),
.statistics-layout > *:nth-child(2) {
  grid-column: span 6;
}

.statistics-layout > *:nth-child(3),
.statistics-layout > *:nth-child(4) {
  grid-column: span 6;
}

.bar-list {
  display: grid;
  gap: 12px;
}

.bar-list__item {
  display: grid;
  gap: 10px;
}

.bar-track {
  height: 12px;
  border-radius: 999px;
  background: rgba(34, 48, 67, 0.08);
  overflow: hidden;
}

.bar-track__fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--sl-brand), var(--sl-brand-strong));
}

.bar-track__fill--warm {
  background: linear-gradient(90deg, var(--sl-accent), #a45f20);
}

.mini-panels {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.mini-panel {
  display: grid;
  gap: 14px;
  padding: 16px;
}

.mini-panel h3 {
  margin: 0;
}

.rank-list {
  display: grid;
  gap: 12px;
}

.rank-list__item {
  padding: 16px;
  border-radius: 18px;
  border: 1px solid var(--sl-line);
  background: var(--sl-soft-panel-bg);
}

.rank-list__item p {
  margin: 6px 0 0;
  color: var(--sl-ink-soft);
}

@media (max-width: 1200px) {
  .metrics-grid > *,
  .statistics-layout > * {
    grid-column: span 12 !important;
  }
}

@media (max-width: 720px) {
  .mini-panels {
    grid-template-columns: 1fr;
  }
}
</style>
