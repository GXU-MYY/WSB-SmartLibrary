<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { deleteComment, getMyBookCollects, getMyComments, getMyShelfCollects, getTopRatedBooks } from '@/api/social'
import BookCard from '@/components/BookCard.vue'
import { useRegisterPageRefresh } from '@/composables/usePageRefresh'
import EmptyState from '@/components/EmptyState.vue'
import LoadingState from '@/components/LoadingState.vue'
import PageIntro from '@/components/PageIntro.vue'
import SectionPanel from '@/components/SectionPanel.vue'
import type { CollectBook, CollectShelf, CommentItem, TopRatedBook } from '@/types/models'
import { buildBookCard, formatDateTime } from '@/utils/format'
import { notifySuccess } from '@/utils/notify'

const router = useRouter()

const loading = ref(false)
const topRatedBooks = ref<TopRatedBook[]>([])
const myComments = ref<CommentItem[]>([])
const myBookCollects = ref<CollectBook[]>([])
const myShelfCollects = ref<CollectShelf[]>([])

const loadPage = async () => {
  loading.value = true

  try {
    const [rated, comments, books, shelves] = await Promise.all([
      getTopRatedBooks(6),
      getMyComments(),
      getMyBookCollects(),
      getMyShelfCollects(),
    ])

    topRatedBooks.value = rated
    myComments.value = comments
    myBookCollects.value = books
    myShelfCollects.value = shelves
  } finally {
    loading.value = false
  }
}

const handleDeleteComment = async (commentId: number) => {
  await deleteComment(commentId)
  notifySuccess('评论已删除')
  await loadPage()
}

useRegisterPageRefresh(loadPage)

onMounted(loadPage)
</script>

<template>
  <div class="page-shell page-stack">
    <PageIntro
      eyebrow="Social Pulse"
      title="把评论、收藏和社区热度放到同一块反馈面板"
      description="社交页不是单独的一条时间线，而是你与社区之间所有互动信号的聚合结果。"
    />

    <section class="page-grid social-layout">
      <SectionPanel
        title="社区高分图书"
        hint="最容易被讨论和认可的书，通常也是你可以优先跟进的内容。"
      >
        <LoadingState v-if="loading && topRatedBooks.length === 0" />
        <div v-else class="books-grid">
          <BookCard
            v-for="item in topRatedBooks"
            :key="item.id"
            :book="
              buildBookCard({
                id: item.id,
                title: item.title,
                coverUrl: item.pic,
                secondary: `社区均分 ${item.stars}/5`,
                badge: '热议'
              })
            "
          >
            <template #actions>
              <button class="button button--ghost" type="button" @click="router.push(`/books/${item.id}`)">
                打开图书
              </button>
            </template>
          </BookCard>
        </div>
      </SectionPanel>

      <SectionPanel
        title="我的评论"
        hint="这些是你在平台上留下的阅读判断，它们也是个人阅读轨迹的一部分。"
      >
        <div v-if="myComments.length" class="comment-list">
          <article v-for="item in myComments" :key="item.id" class="comment-list__item">
            <div class="split-actions">
              <div>
                <strong>图书 #{{ item.bookId }}</strong>
                <p>{{ formatDateTime(item.comTime) }} · {{ item.stars }} 分</p>
              </div>
              <div class="inline-actions">
                <button class="button button--ghost" type="button" @click="router.push(`/books/${item.bookId}`)">查看图书</button>
                <button class="button button--danger" type="button" @click="handleDeleteComment(item.id)">删除</button>
              </div>
            </div>
            <p class="comment-list__body">{{ item.comment }}</p>
          </article>
        </div>
        <EmptyState v-else title="你还没有发布评论" />
      </SectionPanel>

      <SectionPanel
        title="我的收藏"
        hint="你主动保存下来的图书与书架，通常就是下一阶段值得继续扩展的方向。"
      >
        <div class="collect-grid">
          <article v-for="item in myBookCollects" :key="item.id" class="collect-card">
            <strong>{{ item.title }}</strong>
            <p>图书收藏 · {{ formatDateTime(item.collectTime) }}</p>
          </article>

          <article v-for="item in myShelfCollects" :key="item.id" class="collect-card">
            <strong>{{ item.shelfName }}</strong>
            <p>书架收藏 · {{ formatDateTime(item.collectTime) }}</p>
          </article>
        </div>

        <EmptyState v-if="!myBookCollects.length && !myShelfCollects.length" title="你还没有收藏任何内容" />
      </SectionPanel>
    </section>
  </div>
</template>

<style scoped>
.social-layout > * {
  grid-column: span 12;
}

.books-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.comment-list,
.collect-grid {
  display: grid;
  gap: 14px;
}

.comment-list__item,
.collect-card {
  padding: 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.56);
  border: 1px solid var(--sl-line);
}

.comment-list__item p,
.collect-card p {
  margin: 6px 0 0;
  color: var(--sl-ink-soft);
}

.comment-list__body {
  margin-top: 12px !important;
  line-height: 1.7;
}

@media (max-width: 960px) {
  .books-grid {
    grid-template-columns: 1fr;
  }
}
</style>
