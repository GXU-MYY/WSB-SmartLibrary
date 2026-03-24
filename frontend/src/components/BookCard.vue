<script setup lang="ts">
import { computed } from 'vue'

import type { BookCardModel } from '@/types/models'
import { initialsFromName, resolvePictureUrl } from '@/utils/format'

const props = defineProps<{
  book: BookCardModel
  compact?: boolean
  interactive?: boolean
  mobileMinimal?: boolean
}>()

const emit = defineEmits<{
  (e: 'open'): void
}>()

const coverUrl = computed(() => resolvePictureUrl(props.book.coverUrl))
const placeholder = computed(() => initialsFromName(props.book.title))

const handleOpen = () => {
  if (!props.interactive) {
    return
  }

  emit('open')
}
</script>

<template>
  <article
    class="surface-card book-card"
    :class="{
      'book-card--compact': compact,
      'book-card--interactive': interactive,
      'book-card--mobile-minimal': mobileMinimal,
    }"
  >
    <button
      class="book-card__body"
      :class="{ 'book-card__body--interactive': interactive }"
      type="button"
      :disabled="!interactive"
      @click="handleOpen"
    >
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
    </button>

    <div v-if="$slots.actions" class="book-card__actions">
      <slot name="actions" />
    </div>
  </article>
</template>

<style scoped>
.book-card {
  display: grid;
  gap: 14px;
  padding: 16px;
}

.book-card__body {
  display: grid;
  gap: 16px;
  padding: 0;
  border: 0;
  background: transparent;
  color: inherit;
  text-align: left;
}

.book-card__body:disabled {
  cursor: default;
  opacity: 1;
}

.book-card__body--interactive {
  cursor: pointer;
}

.book-card__body--interactive:hover .book-card__cover,
.book-card__body--interactive:focus-visible .book-card__cover {
  transform: translateY(-2px);
  box-shadow: 0 18px 34px rgba(31, 95, 107, 0.16);
}

.book-card__body--interactive:hover .book-card__copy h3,
.book-card__body--interactive:focus-visible .book-card__copy h3 {
  color: var(--sl-brand-strong);
}

.book-card__body--interactive:focus-visible {
  outline: 3px solid rgba(31, 95, 107, 0.2);
  outline-offset: 4px;
  border-radius: 22px;
}

.book-card__cover {
  position: relative;
  overflow: hidden;
  aspect-ratio: 5 / 6;
  border-radius: 20px;
  background: linear-gradient(180deg, rgba(31, 95, 107, 0.18), rgba(201, 119, 46, 0.24));
  transition: transform 180ms ease, box-shadow 180ms ease;
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
  transition: color 180ms ease;
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

.book-card--compact {
  gap: 12px;
  padding: 14px;
}

.book-card--compact .book-card__body {
  gap: 12px;
}

.book-card--compact .book-card__cover {
  aspect-ratio: 4 / 4.8;
  border-radius: 18px;
}

.book-card--compact .book-card__copy {
  gap: 6px;
}

.book-card--compact .book-card__copy h3 {
  font-size: 1.12rem;
}

.book-card--compact .book-card__author,
.book-card--compact .book-card__secondary {
  font-size: 0.92rem;
}

.book-card--compact .book-card__summary {
  font-size: 0.92rem;
  line-height: 1.55;
  -webkit-line-clamp: 2;
}

.book-card--compact .book-card__actions {
  gap: 8px;
}

@media (max-width: 760px) {
  .book-card--mobile-minimal {
    gap: 10px;
    padding: 12px;
  }

  .book-card--mobile-minimal .book-card__body {
    gap: 10px;
  }

  .book-card--mobile-minimal .book-card__cover {
    border-radius: 16px;
    aspect-ratio: 4 / 5;
  }

  .book-card--mobile-minimal .book-card__badge,
  .book-card--mobile-minimal .book-card__summary,
  .book-card--mobile-minimal .book-card__secondary {
    display: none;
  }

  .book-card--mobile-minimal .book-card__copy {
    gap: 4px;
  }

  .book-card--mobile-minimal .book-card__copy h3 {
    font-size: 1rem;
    line-height: 1.2;
  }

  .book-card--mobile-minimal .book-card__author {
    font-size: 0.86rem;
    line-height: 1.35;
  }

  .book-card--mobile-minimal .book-card__actions {
    gap: 8px;
  }
}
</style>
