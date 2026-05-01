<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import { deleteCollect, getMyBookCollects } from '@/api/book'
import BookCard from '@/components/BookCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import LoadingState from '@/components/LoadingState.vue'
import PageIntro from '@/components/PageIntro.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import SectionPanel from '@/components/SectionPanel.vue'
import { useRegisterPageRefresh } from '@/composables/usePageRefresh'
import type { CollectBook } from '@/types/models'
import { buildBookCard, formatDateTime } from '@/utils/format'
import { notifySuccess } from '@/utils/notify'

const router = useRouter()

const loading = ref(false)
const removingCollectId = ref<number | null>(null)
const keyword = ref('')
const bookCollects = ref<CollectBook[]>([])
const COLLECTION_PAGE_SIZE = 10
const collectionPage = ref(1)

const normalizedKeyword = computed(() => keyword.value.trim().toLowerCase())
const filteredBookCollects = computed(() => {
  if (!normalizedKeyword.value) {
    return bookCollects.value
  }

  return bookCollects.value.filter((item) =>
    [item.title, item.author].some((value) => value?.toLowerCase().includes(normalizedKeyword.value)),
  )
})
const currentTotal = computed(() => filteredBookCollects.value.length)
const pagedCollections = computed(() => {
  const start = (collectionPage.value - 1) * COLLECTION_PAGE_SIZE
  return filteredBookCollects.value.slice(start, start + COLLECTION_PAGE_SIZE)
})

const loadCollections = async () => {
  loading.value = true

  try {
    bookCollects.value = await getMyBookCollects()
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  keyword.value = ''
  collectionPage.value = 1
}

const handleCollectionPageChange = (page: number) => {
  collectionPage.value = page
}

const openBookDetail = (bookId: number) => {
  router.push(`/books/${bookId}`)
}

const handleRemoveBookCollect = async (collect: CollectBook) => {
  removingCollectId.value = collect.id

  try {
    await deleteCollect(collect.id)
    bookCollects.value = bookCollects.value.filter((item) => item.id !== collect.id)
    notifySuccess(`已取消收藏《${collect.title}》`)
  } finally {
    removingCollectId.value = null
  }
}

watch(keyword, () => {
  collectionPage.value = 1
})

useRegisterPageRefresh(loadCollections)

onMounted(loadCollections)
</script>

<template>
  <div class="page-shell page-stack">
    <PageIntro
      eyebrow="My Collections"
      title="我的收藏"
      description="集中管理你收藏的图书，把想回访的阅读线索留在一处。"
    />

    <SectionPanel
      class="collection-list-panel"
      title="收藏图书"
      :hint="`当前筛选结果 ${currentTotal} 项`"
    >
      <template #actions>
        <div class="collection-panel-actions">
          <div class="field collection-panel-actions__search">
            <input
              v-model="keyword"
              type="text"
              placeholder="搜索收藏的图书"
              aria-label="搜索收藏"
            />
          </div>
          <button class="button button--secondary" type="button" @click="loadCollections">刷新</button>
          <button class="button button--ghost" type="button" @click="handleReset">清空</button>
        </div>
      </template>

      <LoadingState v-if="loading" />

      <template v-else>
        <div v-if="filteredBookCollects.length" class="collection-book-grid">
          <BookCard
            v-for="book in pagedCollections"
            :key="book.id"
            compact
            interactive
            mobile-minimal
            :book="
              buildBookCard({
                id: book.bookId,
                title: book.title,
                author: book.author,
                coverUrl: book.pic,
                secondary: book.collectTime ? `收藏于 ${formatDateTime(book.collectTime)}` : '收藏时间未记录',
                badge: '收藏',
              })
            "
            @open="openBookDetail(book.bookId)"
          >
            <template #actions>
              <button class="button button--ghost" type="button" @click="openBookDetail(book.bookId)">
                详情
              </button>
              <button
                class="button button--secondary"
                type="button"
                :disabled="removingCollectId === book.id"
                @click="handleRemoveBookCollect(book)"
              >
                {{ removingCollectId === book.id ? '取消中...' : '取消收藏' }}
              </button>
            </template>
          </BookCard>
        </div>

        <PaginationBar
          v-if="currentTotal > 0"
          :current="collectionPage"
          :page-size="COLLECTION_PAGE_SIZE"
          :page-sizes="[]"
          :total="currentTotal"
          hide-page-size
          unit="本"
          @update:current="handleCollectionPageChange"
        />

        <EmptyState
          v-if="!loading && filteredBookCollects.length === 0"
          title="还没有收藏图书"
          description="在图书详情页点亮星标后，收藏的图书会出现在这里。"
        />
      </template>
    </SectionPanel>
  </div>
</template>

<style scoped>
.collection-panel-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.collection-panel-actions__search {
  min-width: min(320px, 42vw);
}

.collection-panel-actions__search input {
  min-height: 48px;
}

.collection-book-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 18px;
}

@media (max-width: 960px) {
  .collection-panel-actions {
    width: 100%;
    flex-wrap: wrap;
  }

  .collection-panel-actions__search {
    min-width: 100%;
  }

  .collection-panel-actions .button {
    flex: 1;
    justify-content: center;
  }
}

@media (max-width: 640px) {
  .collection-book-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
  }

  .collection-book-grid :deep(.book-card__actions) {
    flex-wrap: nowrap;
    gap: 6px;
  }

  .collection-book-grid :deep(.book-card__actions .button) {
    flex: 1 1 0;
    min-width: 0;
    min-height: 36px;
    padding: 0 8px;
    overflow: hidden;
    font-size: 0.78rem;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
</style>
