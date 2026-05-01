<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    current: number
    pageSize: number
    pageSizes: number[]
    total: number
    disabled?: boolean
    hidePageSize?: boolean
    unit?: string
  }>(),
  { disabled: false, hidePageSize: false, unit: '本' },
)

const emit = defineEmits<{
  (e: 'update:current', page: number): void
  (e: 'update:pageSize', size: number): void
  (e: 'current-change', page: number): void
  (e: 'size-change', size: number): void
}>()

const totalPages = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)))

const pages = computed(() => {
  const total = totalPages.value
  const cur = props.current

  if (total <= 7) {
    return Array.from({ length: total }, (_, i) => i + 1)
  }

  const result: (number | '...')[] = [1]

  const left = Math.max(2, cur - 1)
  const right = Math.min(total - 1, cur + 1)

  if (left > 2) result.push('...')
  for (let i = left; i <= right; i++) result.push(i)
  if (right < total - 1) result.push('...')

  result.push(total)
  return result
})

const canPrev = computed(() => props.current > 1)
const canNext = computed(() => props.current < totalPages.value)

const go = (page: number) => {
  if (props.disabled) return
  if (page < 1 || page > totalPages.value || page === props.current) return
  emit('update:current', page)
  emit('current-change', page)
}

const prev = () => go(props.current - 1)
const next = () => go(props.current + 1)

const onSizeChange = (e: Event) => {
  const val = Number((e.target as HTMLSelectElement).value)
  if (props.disabled || val === props.pageSize) return
  emit('update:pageSize', val)
  emit('update:current', 1)
  emit('size-change', val)
}

const onJump = (e: Event) => {
  const val = Number((e.target as HTMLInputElement).value)
  if (val) go(val)
}
</script>

<template>
  <div class="pagination-bar" :class="{ 'pagination-bar--disabled': disabled }">
    <span class="pagination-bar__total">共 {{ total }} {{ unit }}</span>

    <select v-if="!hidePageSize" class="pagination-bar__sizes" :value="pageSize" :disabled="disabled" @change="onSizeChange">
      <option v-for="s in pageSizes" :key="s" :value="s">{{ s }}{{ unit }}/页</option>
    </select>

    <div class="pagination-bar__nav">
      <button
        type="button"
        class="pagination-bar__btn pagination-bar__btn--nav"
        :disabled="!canPrev || disabled"
        @click="prev"
      >
        ‹
      </button>

      <ul class="pagination-bar__pager">
        <li
          v-for="(p, i) in pages"
          :key="i"
          class="pagination-bar__page"
          :class="{
            'pagination-bar__page--active': p === current,
            'pagination-bar__page--ellipsis': p === '...',
          }"
          @click="p !== '...' && go(p as number)"
        >
          {{ p }}
        </li>
      </ul>

      <button
        type="button"
        class="pagination-bar__btn pagination-bar__btn--nav"
        :disabled="!canNext || disabled"
        @click="next"
      >
        ›
      </button>
    </div>

    <div class="pagination-bar__jumper">
      <span>前往</span>
      <input
        type="number"
        class="pagination-bar__jump-input"
        :min="1"
        :max="totalPages"
        :disabled="disabled"
        @keydown.enter="onJump"
      />
      <span>页</span>
    </div>
  </div>
</template>

<style scoped>
.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 14px;
  font-weight: 600;
  color: var(--sl-ink);
  margin-top: 18px;
}

.pagination-bar--disabled {
  opacity: 0.5;
  pointer-events: none;
}

.pagination-bar__total,
.pagination-bar__jumper span {
  color: var(--sl-ink);
}

/* sizes select */
.pagination-bar__sizes {
  border: 1px solid var(--sl-line);
  border-radius: var(--sl-radius-sm);
  background: var(--sl-input-bg);
  padding: 4px 8px;
  font: inherit;
  font-size: 14px;
  font-weight: 600;
  color: var(--sl-ink);
  cursor: pointer;
}

/* nav group */
.pagination-bar__nav {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* prev / next buttons */
.pagination-bar__btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 30px;
  height: 30px;
  border: none;
  border-radius: var(--sl-radius-sm);
  background: transparent;
  font: inherit;
  font-size: 16px;
  font-weight: 700;
  color: var(--sl-ink);
  cursor: pointer;
  transition: color 0.15s;
}

.pagination-bar__btn:hover:not(:disabled) {
  color: var(--sl-accent);
}

.pagination-bar__btn:disabled {
  color: var(--sl-ink-soft);
  cursor: default;
}

/* pager */
.pagination-bar__pager {
  display: flex;
  align-items: center;
  gap: 2px;
  list-style: none;
  margin: 0;
  padding: 0;
}

.pagination-bar__page {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 30px;
  height: 30px;
  border-radius: var(--sl-radius-sm);
  font: inherit;
  font-size: 14px;
  font-weight: 600;
  color: var(--sl-ink);
  cursor: pointer;
  user-select: none;
  transition: color 0.15s;
}

.pagination-bar__page:hover:not(.pagination-bar__page--active):not(.pagination-bar__page--ellipsis) {
  color: var(--sl-accent);
}

.pagination-bar__page--active {
  color: var(--sl-accent);
  font-weight: 700;
}

.pagination-bar__page--ellipsis {
  cursor: default;
  color: var(--sl-ink-soft);
}

/* jumper */
.pagination-bar__jumper {
  display: flex;
  align-items: center;
  gap: 6px;
}

.pagination-bar__jump-input {
  width: 44px;
  height: 30px;
  border: 1px solid var(--sl-line);
  border-radius: var(--sl-radius-sm);
  background: var(--sl-input-bg);
  padding: 0 6px;
  font: inherit;
  font-size: 14px;
  font-weight: 600;
  color: var(--sl-ink);
  text-align: center;
  -moz-appearance: textfield;
}

.pagination-bar__jump-input::-webkit-inner-spin-button,
.pagination-bar__jump-input::-webkit-outer-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

.pagination-bar__jump-input:focus {
  outline: none;
  border-color: rgba(31, 95, 107, 0.44);
  box-shadow: 0 0 0 3px rgba(31, 95, 107, 0.08);
}
</style>
