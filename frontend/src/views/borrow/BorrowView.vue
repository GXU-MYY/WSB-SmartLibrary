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
import {
  borrowStatusLabel,
  borrowTypeLabel,
  formatDate,
  resolvePictureUrl,
} from '@/utils/format'
import { notifyError, notifySuccess } from '@/utils/notify'

const today = () => new Date().toISOString().slice(0, 10)

const loading = ref(false)
const submitting = ref(false)
const updating = ref(false)
const returningId = ref(0)

const bookList = ref<MyBookList | null>(null)
const borrowSummary = ref<BorrowSummary | null>(null)
const records = ref<BorrowRecord[]>([])

const selectedRecordId = ref(0)
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

const selectedRecord = computed(() =>
  records.value.find((item) => item.id === selectedRecordId.value) || null,
)

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
    { label: '进行中 / 逾期', value: `${summary.active} / ${summary.overdue}`, hint: '仍在流转中的记录和其中已逾期的数量。', tone: 'accent' as const },
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

  if (selectedRecordId.value && !records.value.some((item) => item.id === selectedRecordId.value)) {
    selectedRecordId.value = 0
  }
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
  selectedRecordId.value = 0
  updateForm.borrow_id = 0
  updateForm.borrow_name = ''
  updateForm.borrowing_time = ''
  updateForm.due_time = ''
}

const prefillUpdate = (record: BorrowRecord) => {
  selectedRecordId.value = record.id
  updateForm.borrow_id = record.id
  updateForm.borrow_name = record.borrow_name
  updateForm.borrowing_time = record.borrowing_time
  updateForm.due_time = record.due_time || ''
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
    notifyError('请先从记录列表中选择一条借阅记录')
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
      title="让借入与借出都落成可追踪的记录"
      description="这里会集中处理借阅登记、归还、逾期状态与历史记录，让每一本书的流转都有清晰时间线。"
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
      <div class="borrow-layout__side">
        <SectionPanel
          title="登记借阅"
          hint="登记时可以补上预计归还日期，后续逾期统计会基于它自动判断。"
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

          <button
            class="button button--primary"
            type="button"
            :disabled="submitting"
            @click="handleCreateBorrow"
          >
            {{ submitting ? '登记中...' : '保存借阅记录' }}
          </button>
        </SectionPanel>

        <SectionPanel
          title="编辑记录"
          hint="从右侧记录表中点“编辑”后，会把当前记录回填到这里。"
        >
          <div class="field-grid">
            <div class="field">
              <label>记录 ID</label>
              <input v-model.number="updateForm.borrow_id" type="number" min="0" placeholder="先从表格中选择记录" />
            </div>
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

          <div class="inline-actions">
            <button
              class="button button--secondary"
              type="button"
              :disabled="updating"
              @click="handleUpdateRecord"
            >
              {{ updating ? '保存中...' : '保存修改' }}
            </button>
            <button class="button button--ghost" type="button" @click="resetUpdateForm">
              清空
            </button>
          </div>

          <div v-if="selectedRecord" class="record-hint surface-card">
            <strong>当前编辑：{{ selectedRecord.title }}</strong>
            <p>
              {{ selectedRecord.borrow_name }} ·
              {{ borrowTypeLabel(selectedRecord.borrow_type) }} ·
              {{ borrowStatusLabel(selectedRecord.status) }}
            </p>
          </div>
        </SectionPanel>
      </div>

      <SectionPanel
        class="borrow-layout__main"
        title="借阅记录"
        hint="还书现在会直接按借阅记录 ID 定位，不再靠图书 ID 猜测最后一条未归还记录。"
      >
        <template #actions>
          <div class="inline-actions borrow-layout__filters">
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

        <div v-else-if="records.length" class="table-shell">
          <table class="table">
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
                <td>
                  <div class="record-book">
                    <img
                      v-if="resolvePictureUrl(record.pic)"
                      :src="resolvePictureUrl(record.pic)"
                      :alt="record.title"
                      class="record-book__cover"
                      loading="lazy"
                    />
                    <div v-else class="record-book__cover record-book__cover--placeholder">BOOK</div>
                    <div class="record-book__meta">
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
                  <div class="inline-actions">
                    <button class="button button--ghost" type="button" @click="prefillUpdate(record)">
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

        <EmptyState v-else title="还没有借阅记录" description="先登记一条借阅记录，这里就会出现完整的流转历史。" />
      </SectionPanel>
    </section>
  </div>
</template>

<style scoped>
.metrics-grid > * {
  grid-column: span 3;
}

.borrow-layout__side {
  grid-column: span 4;
  display: grid;
  gap: 20px;
}

.borrow-layout__main {
  grid-column: span 8;
}

.borrow-layout__filters {
  justify-content: flex-end;
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.record-book {
  display: grid;
  grid-template-columns: 42px 1fr;
  gap: 12px;
  align-items: center;
  min-width: 220px;
}

.record-book__cover {
  width: 42px;
  aspect-ratio: 3 / 4;
  border-radius: 10px;
  object-fit: cover;
  background: var(--sl-soft-panel-bg);
}

.record-book__cover--placeholder {
  display: grid;
  place-items: center;
  font-size: 0.68rem;
  letter-spacing: 0.08em;
  color: var(--sl-ink-soft);
}

.record-book__meta {
  display: grid;
  gap: 4px;
}

.record-book__meta strong {
  line-height: 1.4;
}

.record-book__meta span {
  color: var(--sl-ink-soft);
  font-size: 0.82rem;
}

.status-chip {
  display: inline-flex;
  align-items: center;
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

.record-hint {
  display: grid;
  gap: 6px;
  padding: 16px;
}

.record-hint p {
  margin: 0;
  color: var(--sl-ink-soft);
}

.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-top: 18px;
}

.pagination-bar__info {
  margin: 0;
  color: var(--sl-ink-soft);
}

@media (max-width: 1200px) {
  .metrics-grid > *,
  .borrow-layout__side,
  .borrow-layout__main {
    grid-column: span 12;
  }
}

@media (max-width: 720px) {
  .field-grid {
    grid-template-columns: 1fr;
  }

  .pagination-bar {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
