<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { aggregateReviews, getRecommendBookPreview } from '@/api/rag'
import EmptyState from '@/components/EmptyState.vue'
import LoadingState from '@/components/LoadingState.vue'
import PageIntro from '@/components/PageIntro.vue'
import SectionPanel from '@/components/SectionPanel.vue'
import type { RecommendBookPreview } from '@/types/models'
import { resolvePictureUrl } from '@/utils/format'
import { notifySuccess } from '@/utils/notify'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const reviewLoading = ref(false)
const preview = ref<RecommendBookPreview | null>(null)

const bookId = computed(() => Number(route.params.id))
const coverUrl = computed(() => resolvePictureUrl(preview.value?.coverUrl))

const loadPage = async () => {
  loading.value = true

  try {
    preview.value = await getRecommendBookPreview(bookId.value)
  } finally {
    loading.value = false
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
  reviewLoading.value = true

  try {
    const reviewDigest = await aggregateReviews(bookId.value)
    preview.value = preview.value ? { ...preview.value, reviewDigest } : preview.value
    notifySuccess('网络书评已更新')
  } finally {
    reviewLoading.value = false
  }
}

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
      eyebrow="Book Preview"
      :title="preview?.title || '图书预览'"
      description="来自推荐或相似图书入口的受限详情，只保留快速判断是否值得继续阅读的关键信息。"
    >
      <template #actions>
        <button
          class="button button--ghost preview-back-button"
          type="button"
          aria-label="返回上一页"
          title="返回上一页"
          @click="handleGoBack"
        >
          返回
        </button>
      </template>
    </PageIntro>

    <LoadingState v-if="loading && !preview" title="正在加载图书预览" />

    <EmptyState
      v-else-if="!loading && !preview"
      title="没有找到这本图书"
      description="它可能已被删除，或当前账号暂时没有访问权限。"
    >
      <button class="button button--primary" type="button" @click="router.push('/books')">返回图书列表</button>
    </EmptyState>

    <template v-else>
      <section class="surface-card preview-hero">
        <div class="preview-hero__cover">
          <img v-if="coverUrl" :src="coverUrl" :alt="preview?.title" loading="lazy" />
          <div v-else class="preview-hero__placeholder serif-title">BOOK</div>
        </div>

        <div class="preview-hero__copy">
          <div class="preview-hero__head">
            <h2>{{ preview?.title }}</h2>
            <p>{{ preview?.author || '作者待补充' }}</p>
          </div>

          <div class="preview-meta-grid">
            <article>
              <span>作者</span>
              <strong>{{ preview?.author || '未记录' }}</strong>
            </article>
            <article>
              <span>出版社</span>
              <strong>{{ preview?.publisher || '未记录' }}</strong>
            </article>
            <article>
              <span>ISBN</span>
              <strong>{{ preview?.isbn || '未记录' }}</strong>
            </article>
          </div>
        </div>
      </section>

      <section class="page-grid preview-grid">
        <SectionPanel title="AI 摘要" hint="只展示已入库的 AI 摘要，帮助你快速判断这本书是否值得继续深入阅读。">
          <p class="copy-block__body">{{ preview?.summary || '暂未生成 AI 摘要。' }}</p>
        </SectionPanel>

        <SectionPanel title="书评聚合" hint="优先展示已缓存的聚合书评；如为空，可以手动触发一次聚合。">
          <div class="copy-block">
            <p class="copy-block__body copy-block__body--preserve">
              {{ preview?.reviewDigest || '暂未聚合网络书评。' }}
            </p>
            <div class="copy-block__footer">
              <button
                v-if="!preview?.reviewDigest || preview?.reviewDigest.length === 0"
                class="button button--ghost"
                type="button"
                :disabled="reviewLoading"
                @click="handleAggregateReviews"
              >
                {{ reviewLoading ? '聚合中...' : '聚合网络书评' }}
              </button>
            </div>
          </div>
        </SectionPanel>
      </section>
    </template>
  </div>
</template>

<style scoped>
.preview-back-button {
  min-width: 88px;
}

.preview-hero {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 22px;
  padding: 22px;
}

.preview-hero__cover {
  overflow: hidden;
  min-height: 340px;
  border-radius: 24px;
  background: linear-gradient(180deg, rgba(31, 95, 107, 0.18), rgba(201, 119, 46, 0.24));
}

.preview-hero__cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-hero__placeholder {
  display: grid;
  place-items: center;
  width: 100%;
  height: 100%;
  color: rgba(255, 255, 255, 0.84);
  font-size: 2rem;
}

.preview-hero__copy {
  display: grid;
  gap: 18px;
  align-content: start;
}

.preview-hero__head h2,
.preview-hero__head p {
  margin: 0;
}

.preview-hero__head h2 {
  font-size: 2.3rem;
}

.preview-hero__head p {
  margin-top: 10px;
  color: var(--sl-ink-soft);
}

.preview-meta-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.preview-meta-grid article {
  padding: 14px;
  border-radius: 18px;
  border: 1px solid var(--sl-detail-meta-border);
  background: var(--sl-detail-meta-bg);
  box-shadow: var(--sl-detail-meta-shadow);
}

.preview-meta-grid span {
  display: block;
  color: var(--sl-detail-meta-label);
  font-size: 0.82rem;
  line-height: 1.2;
}

.preview-meta-grid strong {
  display: block;
  margin-top: 6px;
  color: var(--sl-detail-meta-value);
  line-height: 1.35;
  word-break: break-word;
}

.preview-grid > * {
  grid-column: span 6;
}

.copy-block {
  display: grid;
  gap: 10px;
}

.copy-block__body {
  color: var(--sl-ink-soft);
  line-height: 1.8;
}

.copy-block__body--preserve {
  white-space: pre-line;
}

.copy-block__footer {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 1200px) {
  .preview-grid > * {
    grid-column: span 12;
  }
}

@media (max-width: 900px) {
  .preview-hero {
    grid-template-columns: 1fr;
  }

  .preview-meta-grid {
    grid-template-columns: 1fr;
  }
}
</style>
