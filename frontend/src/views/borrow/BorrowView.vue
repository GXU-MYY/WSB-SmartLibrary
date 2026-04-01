<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

import {
  borrowBook,
  getBorrowRecords,
  getBorrowSummary,
  getMyBooks,
  returnBook,
  updateBorrowRecord,
} from '@/api/book'
import EmptyState from '@/components/EmptyState.vue'
import LoadingState from '@/components/LoadingState.vue'
import MetricCard from '@/components/MetricCard.vue'
import PageIntro from '@/components/PageIntro.vue'
import SectionPanel from '@/components/SectionPanel.vue'
import { useRegisterPageRefresh } from '@/composables/usePageRefresh'
import type { BorrowRecord, BorrowSummary, MyBookList, PageResult } from '@/types/models'
import { borrowStatusLabel, borrowTypeLabel, formatDate, resolvePictureUrl } from '@/utils/format'
import { notifyError, notifySuccess } from '@/utils/notify'

const today = () => new Date().toISOString().slice(0, 10)

const loading = ref(false)
const submitting = ref(false)
const updating = ref(false)
const returningId = ref(0)
const showEditDialog = ref(false)

const bookList = ref<MyBookList | null>(null)
const borrowSummary = ref<BorrowSummary | null>(null)
const records = ref<BorrowRecord[]>([])
const editingRecord = ref<BorrowRecord | null>(null)

const borrowTypeFilter = ref(0)
const statusFilter = ref(-1)

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0,
  pages: 1,
})

const borrowForm = reactive({
  book_id: 0,
  borrow_name: '',
  borrowing_time: today(),
  due_time: '',
  borrow_type: 2,
})

const updateForm = reactive({
  borrow_id: 0,
  borrow_name: '',
  borrowing_time: '',
  due_time: '',
})

const summaryCards = computed(() => {
  const summary = borrowSummary.value || {
    total: 0,
    borrowedIn: 0,
    borrowedOut: 0,
    active: 0,
    overdue: 0,
  }

  return [
    { label: '记录总数', value: summary.total, hint: '当前账号下所有借阅记录。', tone: 'brand' as const },
    { label: '借入', value: summary.borrowedIn, hint: '你从外部借来的书。', tone: 'plain' as const },
    { label: '借出', value: summary.borrowedOut, hint: '你借给别人的书。', tone: 'plain' as const },
    {
      label: '进行中 / 已逾期',
      value: `${summary.active} / ${summary.overdue}`,
      hint: '仍在流转中的记录，以及其中已经逾期的数量。',
      tone: 'accent' as const,
    },
  ]
})

const buildBorrowParams = (page = pagination.current) => ({
  page,
  page_size: pagination.size,
  borrow_type: borrowTypeFilter.value || undefined,
  status: statusFilter.value >= 0 ? statusFilter.value : undefined,
})

const applyBorrowPage = (pageData: PageResult<BorrowRecord>) => {
  records.value = pageData.records
  pagination.current = pageData.current
  pagination.size = pageData.size
  pagination.total = pageData.total
  pagination.pages = pageData.pages
}

const loadBorrowRecords = async (page = pagination.current) => {
  const pageData = await getBorrowRecords(buildBorrowParams(page))
  applyBorrowPage(pageData)
}

const loadBorrowSummary = async () => {
  borrowSummary.value = await getBorrowSummary()
}

const loadPage = async () => {
  loading.value = true

  try {
    const [pageData, booksResult, summaryResult] = await Promise.all([
      getBorrowRecords(buildBorrowParams(1)),
      getMyBooks(),
      getBorrowSummary(),
    ])

    applyBorrowPage(pageData)
    bookList.value = booksResult
    borrowSummary.value = summaryResult
  } finally {
    loading.value = false
  }
}

const resetBorrowForm = () => {
  borrowForm.book_id = 0
  borrowForm.borrow_name = ''
  borrowForm.borrowing_time = today()
  borrowForm.due_time = ''
  borrowForm.borrow_type = 2
}

const resetUpdateForm = () => {
  updateForm.borrow_id = 0
  updateForm.borrow_name = ''
  updateForm.borrowing_time = ''
  updateForm.due_time = ''
  editingRecord.value = null
}

const openEditDialog = (record: BorrowRecord) => {
  editingRecord.value = record
  updateForm.borrow_id = record.id
  updateForm.borrow_name = record.borrow_name
  updateForm.borrowing_time = record.borrowing_time
  updateForm.due_time = record.due_time || ''
  showEditDialog.value = true
}

const closeEditDialog = () => {
  showEditDialog.value = false
  resetUpdateForm()
}

const handleCreateBorrow = async () => {
  if (!borrowForm.book_id || !borrowForm.borrow_name.trim()) {
    notifyError('请先选择图书并填写借阅对象')
    return
  }

  submitting.value = true

  try {
    await borrowBook({
      ...borrowForm,
      due_time: borrowForm.due_time || undefined,
    })
    notifySuccess('借阅记录已登记')
    resetBorrowForm()
    await Promise.all([loadBorrowRecords(1), loadBorrowSummary()])
  } finally {
    submitting.value = false
  }
}

const handleUpdateRecord = async () => {
  if (!updateForm.borrow_id || !updateForm.borrow_name.trim()) {
    notifyError('请先填写完整的编辑信息')
    return
  }

  updating.value = true

  try {
    await updateBorrowRecord({
      ...updateForm,
      borrowing_time: updateForm.borrowing_time || undefined,
      due_time: updateForm.due_time || undefined,
    })
    notifySuccess('借阅记录已更新')
    closeEditDialog()
    await Promise.all([loadBorrowRecords(pagination.current), loadBorrowSummary()])
  } finally {
    updating.value = false
  }
}

const handleReturn = async (record: BorrowRecord) => {
  returningId.value = record.id

  try {
    await returnBook({
      borrow_id: record.id,
      return_time: today(),
    })
    notifySuccess('还书已登记', `《${record.title}》的归还时间已写入。`)
    await Promise.all([loadBorrowRecords(pagination.current), loadBorrowSummary()])
  } finally {
    returningId.value = 0
  }
}

const handleFilterChange = async () => {
  loading.value = true
  try {
    await loadBorrowRecords(1)
  } finally {
    loading.value = false
  }
}

const handlePageChange = async (page: number) => {
  if (page < 1 || page > pagination.pages || page === pagination.current) {
    return
  }

  loading.value = true
  try {
    await loadBorrowRecords(page)
  } finally {
    loading.value = false
  }
}

useRegisterPageRefresh(loadPage)

onMounted(loadPage)
</script>

<template>
  <div class="page-shell page-stack">
    <PageIntro
      eyebrow="Borrow Flow"
      title="把借入与借出落成清晰记录"
      description="左侧快速登记，右侧集中浏览和处理记录，让每一本书的借阅状态都能一眼看清。"
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
      <SectionPanel title="登记借阅" class="borrow-layout__form">
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
            <input v-model="borrowForm.borrow_name" type="text" placeholder="填写对方姓名" />
          </div>
          <div class="field">
            <label>借阅类型</label>
            <select v-model.number="borrowForm.borrow_type">
              <option :value="1">借入</option>
              <option :value="2">借出</option>
            </select>
          </div>
          <div class="field">
            <label>借阅日期</label>
            <input v-model="borrowForm.borrowing_time" type="date" />
          </div>
          <div class="field">
            <label>预计归还日期</label>
            <input v-model="borrowForm.due_time" type="date" />
          </div>
        </div>

        <button class="button button--primary borrow-submit" type="button" :disabled="submitting" @click="handleCreateBorrow">
          {{ submitting ? '登记中...' : '保存借阅记录' }}
        </button>
      </SectionPanel>

      <SectionPanel title="借阅记录" class="borrow-layout__records">
        <template #actions>
          <div class="record-toolbar">
            <select v-model.number="borrowTypeFilter" @change="handleFilterChange">
              <option :value="0">全部类型</option>
              <option :value="1">只看借入</option>
              <option :value="2">只看借出</option>
            </select>
            <select v-model.number="statusFilter" @change="handleFilterChange">
              <option :value="-1">全部状态</option>
              <option :value="0">借阅中</option>
              <option :value="1">已归还</option>
              <option :value="2">已逾期</option>
            </select>
          </div>
        </template>

        <LoadingState v-if="loading && records.length === 0" />

        <div v-else-if="records.length" class="borrow-records-shell">
          <div class="table-shell borrow-table-desktop">
            <table class="table borrow-table">
            <thead>
              <tr>
                <th>图书</th>
                <th>对象</th>
                <th>类型</th>
                <th>借阅日期</th>
                <th>预计归还</th>
                <th>状态</th>
                <th>归还日期</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="record in records" :key="record.id">
                <td class="borrow-table__book">
                  <div class="book-cell">
                    <img
                      v-if="resolvePictureUrl(record.pic)"
                      :src="resolvePictureUrl(record.pic)"
                      :alt="record.title"
                      class="book-cell__cover"
                      loading="lazy"
                    />
                    <div v-else class="book-cell__cover book-cell__cover--placeholder">BOOK</div>
                    <div class="book-cell__meta">
                      <strong>{{ record.title }}</strong>
                      <span>#{{ record.id }}</span>
                    </div>
                  </div>
                </td>
                <td>{{ record.borrow_name }}</td>
                <td>{{ borrowTypeLabel(record.borrow_type) }}</td>
                <td>{{ formatDate(record.borrowing_time) }}</td>
                <td>{{ record.due_time ? formatDate(record.due_time) : '未设置' }}</td>
                <td>
                  <span class="status-chip" :class="`status-chip--${record.status}`">
                    {{ borrowStatusLabel(record.status) }}
                  </span>
                </td>
                <td>{{ record.return_time ? formatDate(record.return_time) : '未归还' }}</td>
                <td>
                  <div class="inline-actions borrow-table__actions">
                    <button class="button button--ghost" type="button" @click="openEditDialog(record)">
                      编辑
                    </button>
                    <button
                      v-if="record.status !== 1"
                      class="button button--secondary"
                      type="button"
                      :disabled="returningId === record.id"
                      @click="handleReturn(record)"
                    >
                      {{ returningId === record.id ? '还书中...' : '还书' }}
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
            </table>
          </div>

          <div class="borrow-card-list">
            <article v-for="record in records" :key="`mobile-${record.id}`" class="borrow-record-card surface-card">
              <div class="borrow-record-card__head">
                <div class="book-cell">
                  <img
                    v-if="resolvePictureUrl(record.pic)"
                    :src="resolvePictureUrl(record.pic)"
                    :alt="record.title"
                    class="book-cell__cover"
                    loading="lazy"
                  />
                  <div v-else class="book-cell__cover book-cell__cover--placeholder">BOOK</div>
                  <div class="book-cell__meta">
                    <strong>{{ record.title }}</strong>
                    <span>#{{ record.id }}</span>
                  </div>
                </div>
                <span class="status-chip" :class="`status-chip--${record.status}`">
                  {{ borrowStatusLabel(record.status) }}
                </span>
              </div>

              <div class="borrow-record-card__meta">
                <div class="borrow-record-card__pair">
                  <span>对象</span>
                  <strong>{{ record.borrow_name }}</strong>
                </div>
                <div class="borrow-record-card__pair">
                  <span>类型</span>
                  <strong>{{ borrowTypeLabel(record.borrow_type) }}</strong>
                </div>
                <div class="borrow-record-card__pair">
                  <span>借阅</span>
                  <strong>{{ formatDate(record.borrowing_time) }}</strong>
                </div>
                <div class="borrow-record-card__pair">
                  <span>预计</span>
                  <strong>{{ record.due_time ? formatDate(record.due_time) : '未设置' }}</strong>
                </div>
                <div v-if="record.return_time" class="borrow-record-card__pair">
                  <span>归还</span>
                  <strong>{{ formatDate(record.return_time) }}</strong>
                </div>
              </div>

              <div class="inline-actions borrow-record-card__actions">
                <button class="button button--ghost" type="button" @click="openEditDialog(record)">
                  编辑
                </button>
                <button
                  v-if="record.status !== 1"
                  class="button button--secondary"
                  type="button"
                  :disabled="returningId === record.id"
                  @click="handleReturn(record)"
                >
                  {{ returningId === record.id ? '还书中...' : '还书' }}
                </button>
              </div>
            </article>
          </div>

          <div class="pagination-bar">
            <p class="pagination-bar__info">
              第 {{ pagination.current }} / {{ Math.max(pagination.pages, 1) }} 页 · 共 {{ pagination.total }} 条
            </p>
            <div class="inline-actions">
              <button
                class="button button--ghost"
                type="button"
                :disabled="pagination.current <= 1 || loading"
                @click="handlePageChange(pagination.current - 1)"
              >
                上一页
              </button>
              <button
                class="button button--ghost"
                type="button"
                :disabled="pagination.current >= pagination.pages || loading"
                @click="handlePageChange(pagination.current + 1)"
              >
                下一页
              </button>
            </div>
          </div>
        </div>

        <EmptyState
          v-else
          title="还没有借阅记录"
          description="先登记一条借阅记录，这里就会出现完整的流转历史。"
        />
      </SectionPanel>
    </section>

    <div v-if="showEditDialog" class="dialog-backdrop" @click.self="closeEditDialog">
      <section class="dialog-card dialog-card--borrow-edit">
        <button class="dialog-close" type="button" @click="closeEditDialog">×</button>

        <header class="dialog-head">
          <div>
            <p class="dialog-eyebrow">Borrow Edit</p>
            <h2>编辑借阅记录</h2>
          </div>
        </header>

        <div v-if="editingRecord" class="dialog-book-summary surface-card">
          <strong>{{ editingRecord.title }}</strong>
          <p>
            {{ editingRecord.borrow_name }} ·
            {{ borrowTypeLabel(editingRecord.borrow_type) }} ·
            {{ borrowStatusLabel(editingRecord.status) }}
          </p>
        </div>

        <div class="field-grid">
          <div class="field">
            <label>借阅对象</label>
            <input v-model="updateForm.borrow_name" type="text" placeholder="填写对方姓名" />
          </div>
          <div class="field">
            <label>借阅日期</label>
            <input v-model="updateForm.borrowing_time" type="date" />
          </div>
          <div class="field">
            <label>预计归还日期</label>
            <input v-model="updateForm.due_time" type="date" />
          </div>
        </div>

        <div class="dialog-actions">
          <button class="button button--ghost" type="button" @click="closeEditDialog">
            取消
          </button>
          <button class="button button--primary" type="button" :disabled="updating" @click="handleUpdateRecord">
            {{ updating ? '保存中...' : '保存修改' }}
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.metrics-grid > * {
  grid-column: span 3;
}

.borrow-layout__form {
  grid-column: span 4;
}

.borrow-layout__records {
  grid-column: span 8;
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.borrow-submit {
  width: 100%;
}

.record-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
}

.borrow-records-shell {
  display: grid;
  gap: 16px;
}

.table-shell {
  overflow-x: visible;
  padding-bottom: 4px;
}

.borrow-table {
  width: 100%;
  min-width: 0;
  table-layout: fixed;
}

.borrow-table th,
.borrow-table td {
  vertical-align: middle;
}

.borrow-table th {
  white-space: nowrap;
}

.borrow-table td {
  line-height: 1.5;
}

.borrow-table__book {
  width: 32%;
}

.borrow-table__person {
  width: 14%;
  word-break: break-all;
}

.borrow-table__status {
  width: 13%;
}

.borrow-info-cell {
  display: grid;
  gap: 4px;
  color: var(--sl-ink-soft);
}

.borrow-info-cell strong {
  color: var(--sl-ink);
}

.borrow-table th:last-child,
.borrow-table td:last-child {
  width: 150px;
}

.borrow-table__actions {
  justify-content: flex-start;
  flex-wrap: nowrap;
}

.borrow-card-list {
  display: none;
}

.borrow-record-card {
  display: grid;
  gap: 14px;
  padding: 16px;
}

.borrow-record-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.borrow-record-card__meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.borrow-record-card__pair {
  display: grid;
  gap: 4px;
}

.borrow-record-card__pair span {
  font-size: 0.82rem;
  color: var(--sl-ink-soft);
}

.borrow-record-card__pair strong {
  line-height: 1.5;
  word-break: break-word;
}

.borrow-record-card__actions {
  justify-content: flex-end;
}

.book-cell {
  display: grid;
  grid-template-columns: 46px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  min-width: 0;
}

.book-cell__cover {
  width: 46px;
  aspect-ratio: 3 / 4;
  border-radius: 10px;
  object-fit: cover;
  background: var(--sl-soft-panel-bg);
}

.book-cell__cover--placeholder {
  display: grid;
  place-items: center;
  font-size: 0.66rem;
  letter-spacing: 0.08em;
  color: var(--sl-ink-soft);
}

.book-cell__meta {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.book-cell__meta strong {
  line-height: 1.45;
}

.book-cell__meta span {
  font-size: 0.82rem;
  color: var(--sl-ink-soft);
}

.status-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 0.82rem;
  font-weight: 600;
  white-space: nowrap;
}

.status-chip--0 {
  background: rgba(38, 114, 97, 0.14);
  color: #1f6a59;
}

.status-chip--1 {
  background: rgba(98, 109, 127, 0.14);
  color: var(--sl-ink-soft);
}

.status-chip--2 {
  background: rgba(180, 88, 62, 0.16);
  color: #b4583e;
}

.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-top: 16px;
}

.pagination-bar__info {
  margin: 0;
  color: var(--sl-ink-soft);
}

.dialog-backdrop {
  position: fixed;
  inset: 0;
  z-index: 90;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(18, 22, 32, 0.46);
  backdrop-filter: blur(12px);
}

.dialog-card {
  position: relative;
  width: min(720px, 100%);
  max-height: calc(100vh - 48px);
  overflow: auto;
  background: var(--sl-panel-bg);
  border: 1px solid var(--sl-border-color);
  border-radius: 26px;
  box-shadow: 0 26px 60px rgba(11, 18, 32, 0.2);
  padding: 24px;
  display: grid;
  gap: 18px;
}

.dialog-close {
  position: absolute;
  top: 16px;
  right: 16px;
  border: none;
  background: transparent;
  color: var(--sl-ink-soft);
  font-size: 1.8rem;
  line-height: 1;
  cursor: pointer;
}

.dialog-head h2,
.dialog-head p {
  margin: 0;
}

.dialog-eyebrow {
  margin-bottom: 6px;
  font-size: 0.8rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--sl-ink-soft);
}

.dialog-book-summary {
  display: grid;
  gap: 6px;
  padding: 16px;
}

.dialog-book-summary p {
  margin: 0;
  color: var(--sl-ink-soft);
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 1200px) {
  .borrow-layout__form,
  .borrow-layout__records {
    grid-column: span 12;
  }
}

@media (max-width: 960px) {
  .metrics-grid > * {
    grid-column: span 6;
  }
}

@media (max-width: 720px) {
  .metrics-grid > * {
    grid-column: span 12;
  }

  .field-grid {
    grid-template-columns: 1fr;
  }

  .record-toolbar {
    justify-content: flex-start;
  }

  .borrow-table-desktop {
    display: none;
  }

  .borrow-card-list {
    display: grid;
    gap: 12px;
  }

  .borrow-record-card__meta {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .borrow-record-card__actions {
    justify-content: stretch;
  }

  .borrow-record-card__actions :deep(.button),
  .borrow-record-card__actions .button {
    flex: 1;
  }

  .pagination-bar,
  .dialog-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .dialog-card {
    padding: 20px;
    border-radius: 22px;
  }
}
</style>
