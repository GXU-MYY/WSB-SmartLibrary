<script setup lang="ts">
import { computed } from 'vue'

import type { BookCardModel } from '@/types/models'
import { initialsFromName, resolvePictureUrl } from '@/utils/format'

const props = defineProps<{
  book: BookCardModel
}>()

const coverUrl = computed(() => resolvePictureUrl(props.book.coverUrl))
const placeholder = computed(() => initialsFromName(props.book.title))
</script>

<template>
  <article class="surface-card book-card">
    <div class="book-card__cover">
      <img
        v-if="coverUrl"
        :src="coverUrl"
        :alt="book.title"
        class="book-card__image"
        loading="lazy"
      />
      <div v-else class="book-card__placeholder">
        <span>{{ placeholder }}</span>
      </div>

      <span v-if="book.badge" class="book-card__badge">{{ book.badge }}</span>
    </div>

    <div class="book-card__copy">
      <h3>{{ book.title }}</h3>
      <p class="book-card__author">{{ book.author || '作者待补充' }}</p>
      <p v-if="book.summary" class="book-card__summary">{{ book.summary }}</p>
      <p v-if="book.secondary" class="book-card__secondary">{{ book.secondary }}</p>
    </div>

    <div v-if="$slots.actions" class="book-card__actions">
      <slot name="actions" />
    </div>
  </article>
</template>

<style scoped>
.book-card {
  display: grid;
  gap: 16px;
  padding: 16px;
}

.book-card__cover {
  position: relative;
  overflow: hidden;
  aspect-ratio: 5 / 6;
  border-radius: 20px;
  background: linear-gradient(180deg, rgba(31, 95, 107, 0.18), rgba(201, 119, 46, 0.24));
}

.book-card__image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.book-card__placeholder {
  display: grid;
  place-items: end start;
  width: 100%;
  height: 100%;
  padding: 18px;
  color: rgba(255, 255, 255, 0.9);
  font-family: 'Newsreader', serif;
  font-size: 2rem;
}

.book-card__badge {
  position: absolute;
  top: 14px;
  left: 14px;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255, 253, 248, 0.92);
  color: var(--sl-brand-strong);
  font-size: 0.8rem;
}

.book-card__copy {
  display: grid;
  gap: 8px;
}

.book-card__copy h3,
.book-card__copy p {
  margin: 0;
}

.book-card__copy h3 {
  font-size: 1.3rem;
  line-height: 1.1;
}

.book-card__author,
.book-card__secondary {
  color: var(--sl-ink-soft);
}

.book-card__summary {
  display: -webkit-box;
  overflow: hidden;
  color: var(--sl-ink-soft);
  line-height: 1.7;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

.book-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
</style>
