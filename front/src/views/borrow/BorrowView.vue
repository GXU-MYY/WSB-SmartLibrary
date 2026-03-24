<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

import {
  borrowBook,
  getBorrowRecords,
  getMyBooks,
  returnBook,
  updateBorrowRecord,
} from '@/api/book'
import { useRegisterPageRefresh } from '@/composables/usePageRefresh'
import EmptyState from '@/components/EmptyState.vue'
import LoadingState from '@/components/LoadingState.vue'
import MetricCard from '@/components/MetricCard.vue'
import PageIntro from '@/components/PageIntro.vue'
import SectionPanel from '@/components/SectionPanel.vue'
import type { BorrowRecord, MyBookList } from '@/types/models'
import { borrowTypeLabel, formatDate, normalizePage } from '@/utils/format'
import { notifyError, notifySuccess } from '@/utils/notify'

const loading = ref(false)
const submitting = ref(false)
const updating = ref(false)

const bookList = ref<MyBookList | null>(null)
const records = ref<BorrowRecord[]>([])
const selectedRecordId = ref(0)
const borrowTypeFilter = ref(0)

const borrowForm = reactive({
  book_id: 0,
  borrow_name: '',
  borrowing_time: new Date().toISOString().slice(0, 10),
  borrow_type: 2,
})

const updateForm = reactive({
  borrow_id: 0,
  borrow_name: '',
  borrowing_time: '',
})

const summaryCards = computed(() => {
  const borrowedIn = records.value.filter((item) => item.borrow_type === 1)
  const borrowedOut = records.value.filter((item) => item.borrow_type === 2)
  const unreturned = records.value.filter((item) => !item.return_time)

  return [
    { label: '记录总数', value: records.value.length, hint: '当前视图下的借阅记录总量。', tone: 'brand' as const },
    { label: '借入', value: borrowedIn.length, hint: '你从外部借入并仍在跟踪的图书。', tone: 'plain' as const },
    { label: '借出', value: borrowedOut.length, hint: '你已借出的书以及对应对象。', tone: 'plain' as const },
    { label: '未归还', value: unreturned.length, hint: '仍在流转中的记录，需要持续关注。', tone: 'accent' as const },
  ]
})

const selectedRecord = computed(() =>
  records.value.find((item) => item.id === selectedRecordId.value) || null,
)

const loadRecords = async () => {
  loading.value = true

  try {
    records.value = await getBorrowRecords(borrowTypeFilter.value || undefined)
  } finally {
    loading.value = false
  }
}

const loadPage = async () => {
  loading.value = true

  try {
    const [recordsResult, booksResult] = await Promise.all([
      getBorrowRecords(borrowTypeFilter.value || undefined),
      getMyBooks(),
    ])

    records.value = recordsResult
    bookList.value = booksResult
  } finally {
    loading.value = false
  }
}

const prefillUpdate = (record: BorrowRecord) => {
  selectedRecordId.value = record.id
  updateForm.borrow_id = record.id
  updateForm.borrow_name = record.borrow_name
  updateForm.borrowing_time = record.borrowing_time
}

const handleCreateBorrow = async () => {
  if (!borrowForm.book_id || !borrowForm.borrow_name.trim()) {
    notifyError('请先选择图书并填写借阅对象')
    return
  }

  submitting.value = true

  try {
    await borrowBook(borrowForm)
    notifySuccess('借阅记录已登记')
    borrowForm.book_id = 0
    borrowForm.borrow_name = ''
    borrowForm.borrowing_time = new Date().toISOString().slice(0, 10)
    borrowForm.borrow_type = 2
    await loadRecords()
  } finally {
    submitting.value = false
  }
}

const handleUpdateRecord = async () => {
  if (!updateForm.borrow_id || !updateForm.borrow_name.trim()) {
    notifyError('请先从表格中选择一条记录')
    return
  }

  updating.value = true

  try {
    await updateBorrowRecord(updateForm)
    notifySuccess('借阅记录已更新')
    await loadRecords()
  } finally {
    updating.value = false
  }
}

const handleReturn = async (record: BorrowRecord) => {
  await returnBook({
    book_id: String(record.book_id),
    borrow_type: String(record.borrow_type),
    return_time: new Date().toISOString().slice(0, 10),
  })
  notifySuccess('归还已登记', `《${record.title}》的归还时间已写入。`)
  await loadRecords()
}

useRegisterPageRefresh(loadPage)

onMounted(loadPage)
</script>

<template>
  <div class="page-shell page-stack">
    <PageIntro
      eyebrow="Borrow Flow"
      title="把借入借出这件事留成可追踪的流转记录"
      description="这里关注的是流转过程，而不是单纯列表。你可以登记、修订和归还借阅，让书不会消失在口头约定里。"
    />

    <section class="page-grid metrics-grid">
      <MetricCard
        v-for="item in summaryCards"
        :key="item.label"
        :label="item.label"
        :value="item.value"
        :hint="item.hint"
        :tone="item.tone"
      />
    </section>

    <section class="page-grid borrow-layout">
      <SectionPanel
        title="登记新借阅"
        hint="录入一本图书的借出或借入动作，并立即把它纳入后续跟踪。"
      >
        <div class="field">
          <label>图书</label>
          <select v-model.number="borrowForm.book_id">
            <option :value="0">选择图书</option>
            <option v-for="book in bookList?.books || []" :key="book.id" :value="book.id">
              {{ book.title }}
            </option>
          </select>
        </div>

        <div class="field-grid">
          <div class="field">
            <label>借阅对象</label>
            <input v-model="borrowForm.borrow_name" type="text" placeholder="填写对象姓名" />
          </div>
          <div class="field">
            <label>类型</label>
            <select v-model.number="borrowForm.borrow_type">
              <option :value="1">借入</option>
              <option :value="2">借出</option>
            </select>
          </div>
          <div class="field">
            <label>日期</label>
            <input v-model="borrowForm.borrowing_time" type="date" />
          </div>
        </div>

        <button class="button button--primary" type="button" :disabled="submitting" @click="handleCreateBorrow">
          {{ submitting ? '保存中...' : '保存借阅记录' }}
        </button>
      </SectionPanel>

      <SectionPanel
        title="借阅记录"
        hint="点击“修订”可以把该条记录送到左侧编辑区，点击“归还”会直接写入当天日期。"
      >
        <div class="inline-actions">
          <button class="button button--secondary" type="button" @click="borrowTypeFilter = 0; loadRecords()">全部</button>
          <button class="button button--ghost" type="button" @click="borrowTypeFilter = 1; loadRecords()">只看借入</button>
          <button class="button button--ghost" type="button" @click="borrowTypeFilter = 2; loadRecords()">只看借出</button>
        </div>

        <LoadingState v-if="loading && records.length === 0" />
        <div v-else-if="records.length" class="table-shell">
          <table class="table">
            <thead>
              <tr>
                <th>图书</th>
                <th>对象</th>
                <th>类型</th>
                <th>借阅日期</th>
                <th>归还日期</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="record in records" :key="record.id">
                <td>{{ record.title }}</td>
                <td>{{ record.borrow_name }}</td>
                <td>{{ borrowTypeLabel(record.borrow_type) }}</td>
                <td>{{ formatDate(record.borrowing_time) }}</td>
                <td>{{ formatDate(record.return_time) }}</td>
                <td>
                  <div class="inline-actions">
                    <button class="button button--ghost" type="button" @click="prefillUpdate(record)">修订</button>
                    <button class="button button--secondary" type="button" @click="handleReturn(record)">归还</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <EmptyState v-else title="还没有借阅记录" />
      </SectionPanel>

      <SectionPanel
        title="记录修订"
        hint="如果借阅对象写错了，或借出日期需要校正，可以在这里快速调整。"
      >
        <div class="field-grid">
          <div class="field">
            <label>记录 ID</label>
            <input v-model.number="updateForm.borrow_id" type="number" min="0" placeholder="先从表格点修订" />
          </div>
          <div class="field">
            <label>借阅对象</label>
            <input v-model="updateForm.borrow_name" type="text" placeholder="对象姓名" />
          </div>
          <div class="field">
            <label>借阅日期</label>
            <input v-model="updateForm.borrowing_time" type="date" />
          </div>
        </div>

        <button class="button button--secondary" type="button" :disabled="updating" @click="handleUpdateRecord">
          {{ updating ? '更新中...' : '保存修订' }}
        </button>

        <div v-if="selectedRecord" class="record-hint surface-card">
          <strong>当前修订对象：{{ selectedRecord.title }}</strong>
          <p>{{ selectedRecord.borrow_name }} · {{ borrowTypeLabel(selectedRecord.borrow_type) }}</p>
        </div>
      </SectionPanel>
    </section>
  </div>
</template>

<style scoped>
.metrics-grid > * {
  grid-column: span 3;
}

.borrow-layout > *:nth-child(1),
.borrow-layout > *:nth-child(3) {
  grid-column: span 4;
}

.borrow-layout > *:nth-child(2) {
  grid-column: span 8;
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.record-hint {
  display: grid;
  gap: 6px;
  padding: 16px;
}

.record-hint p {
  margin: 0;
  color: var(--sl-ink-soft);
}

@media (max-width: 1200px) {
  .metrics-grid > *,
  .borrow-layout > * {
    grid-column: span 12 !important;
  }
}

@media (max-width: 720px) {
  .field-grid {
    grid-template-columns: 1fr;
  }
}
</style>
