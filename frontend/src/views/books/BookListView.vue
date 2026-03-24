<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import {
  createBook,
  createShelf,
  deleteBook,
  deleteShelf,
  getBookList,
  getIsbnBook,
  getShelves,
  onShelf,
  updateBook,
} from '@/api/book'
import { uploadPicture } from '@/api/file'
import BookCard from '@/components/BookCard.vue'
import { useRegisterPageRefresh } from '@/composables/usePageRefresh'
import EmptyState from '@/components/EmptyState.vue'
import LoadingState from '@/components/LoadingState.vue'
import PageIntro from '@/components/PageIntro.vue'
import SectionPanel from '@/components/SectionPanel.vue'
import type { Book, BookFormPayload, BookUpdatePayload, Shelf, ShelfPayload } from '@/types/models'
import { buildBookCard, formatCurrency, normalizePage } from '@/utils/format'
import { notifyError, notifySuccess } from '@/utils/notify'

const router = useRouter()

const loading = ref(false)
const creatingBook = ref(false)
const editingBook = ref(false)
const creatingShelf = ref(false)
const attachingShelf = ref(false)
const isbnLoading = ref(false)
const uploadingCover = ref(false)

const books = ref<Book[]>([])
const shelves = ref<Shelf[]>([])

const pagination = reactive({
  current: 1,
  size: 8,
  total: 0,
})

const filters = reactive({
  keyword: '',
  classify: '',
  shelfId: '',
})

const createForm = reactive<BookFormPayload>({
  title: '',
  author: '',
  subtitle: '',
  summary: '',
  publisher: '',
  publishDate: '',
  price: null,
  pageCount: null,
  isbn: '',
  isbn10: '',
  coverUrl: '',
  classify: '',
  label: '',
  remark: '',
  shelfId: null,
  isOnShelf: false,
  isBorrowed: false,
})

const editForm = reactive<BookUpdatePayload>({
  id: 0,
  price: null,
  remark: '',
})

const shelfForm = reactive<ShelfPayload>({
  shelfName: '',
  address: '',
  isPublic: false,
  remark: '',
  shelfType: 1,
})

const attachForm = reactive({
  bookId: 0,
  shelfId: 0,
})

const classifyOptions = computed(() =>
  Array.from(new Set(books.value.map((item) => item.classify).filter(Boolean))),
)

const loadShelves = async () => {
  shelves.value = await getShelves()
}

const loadBooks = async () => {
  loading.value = true

  try {
    const result = await getBookList({
      page: pagination.current,
      page_size: pagination.size,
      bookshelf_id: filters.shelfId || undefined,
      book_name: filters.keyword || undefined,
      classify: filters.classify || undefined,
    })

    const normalized = normalizePage(result)
    books.value = normalized.records
    pagination.total = normalized.total
  } finally {
    loading.value = false
  }
}

const loadPage = async () => {
  await Promise.all([loadBooks(), loadShelves()])
}

const resetCreateForm = () => {
  createForm.title = ''
  createForm.author = ''
  createForm.subtitle = ''
  createForm.summary = ''
  createForm.publisher = ''
  createForm.publishDate = ''
  createForm.price = null
  createForm.pageCount = null
  createForm.isbn = ''
  createForm.isbn10 = ''
  createForm.coverUrl = ''
  createForm.classify = ''
  createForm.label = ''
  createForm.remark = ''
  createForm.shelfId = null
  createForm.isOnShelf = false
  createForm.isBorrowed = false
}

const startEdit = (book: Book) => {
  editForm.id = book.id
  editForm.price = book.price ?? null
  editForm.remark = book.remark || ''
}

const prefillAttach = (book: Book) => {
  attachForm.bookId = book.id
  attachForm.shelfId = Number(filters.shelfId || shelves.value[0]?.id || 0)
}

const normalizeIsbnInput = (value: string) => value.replace(/[^0-9Xx]/g, '').toUpperCase()

const normalizePublishDateInput = (value?: string | null) => {
  const rawValue = value?.trim()

  if (!rawValue) {
    return ''
  }

  const normalized = rawValue
    .replace(/[./]/g, '-')
    .replace(/年/g, '-')
    .replace(/月/g, '-')
    .replace(/日/g, '')
    .replace(/--+/g, '-')
    .replace(/^-|-$/g, '')

  const fullDateMatch = normalized.match(/^(\d{4})-(\d{1,2})-(\d{1,2})$/)
  if (fullDateMatch) {
    const [, year, month, day] = fullDateMatch
    return `${year}-${month.padStart(2, '0')}-${day.padStart(2, '0')}`
  }

  const yearMonthMatch = normalized.match(/^(\d{4})-(\d{1,2})$/)
  if (yearMonthMatch) {
    const [, year, month] = yearMonthMatch
    return `${year}-${month.padStart(2, '0')}-01`
  }

  const yearMatch = normalized.match(/^(\d{4})$/)
  if (yearMatch) {
    return `${yearMatch[1]}-01-01`
  }

  return rawValue
}

const handleAutofillByIsbn = async () => {
  const normalizedIsbn = normalizeIsbnInput(createForm.isbn || '')

  if (!normalizedIsbn) {
    notifyError('请输入 ISBN', 'ISBN 是自动补全图书元数据的触发条件。')
    return
  }

  createForm.isbn = normalizedIsbn
  isbnLoading.value = true

  try {
    const result = await getIsbnBook(normalizedIsbn)
    createForm.title = result.title || createForm.title
    createForm.author = result.author || createForm.author
    createForm.subtitle = result.subtitle || createForm.subtitle
    createForm.publisher = result.publisher || createForm.publisher
    createForm.summary = result.summary || createForm.summary
    createForm.coverUrl = result.coverUrl || createForm.coverUrl
    createForm.publishDate = normalizePublishDateInput(result.publishDate) || createForm.publishDate
    createForm.isbn10 = result.isbn10 || createForm.isbn10
    createForm.classify = result.clc || createForm.classify
    createForm.pageCount = result.pageCount ? Number(result.pageCount) : createForm.pageCount
    createForm.price = result.price ? Number(String(result.price).replace(/[^\d.]/g, '')) : createForm.price
    notifySuccess('图书信息已补全', '请检查后再保存到个人书库。')
  } finally {
    isbnLoading.value = false
  }
}

const handleCoverUpload = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]

  if (!file) {
    return
  }

  uploadingCover.value = true

  try {
    const result = await uploadPicture(file)
    createForm.coverUrl = result.pic
    notifySuccess('封面已上传', '保存图书时会一并写入封面地址。')
  } finally {
    uploadingCover.value = false
    input.value = ''
  }
}

const handleCreateBook = async () => {
  if (!createForm.title.trim()) {
    notifyError('图书标题不能为空', '请先填写至少一个标题字段。')
    return
  }

  creatingBook.value = true

  try {
    const normalizedPublishDate = normalizePublishDateInput(createForm.publishDate)
    createForm.publishDate = normalizedPublishDate

    await createBook({
      ...createForm,
      publishDate: normalizedPublishDate || undefined,
    })
    notifySuccess('图书已加入书库', '新的藏书已经出现在列表中。')
    resetCreateForm()
    await loadPage()
  } finally {
    creatingBook.value = false
  }
}

const handleUpdateBook = async () => {
  if (!editForm.id) {
    notifyError('还没有选择要编辑的图书')
    return
  }

  editingBook.value = true

  try {
    await updateBook(editForm)
    notifySuccess('图书备注已更新', '价格和备注已经写回。')
    await loadBooks()
  } finally {
    editingBook.value = false
  }
}

const handleDeleteBook = async (book: Book) => {
  await deleteBook(book.id)
  notifySuccess('图书已删除', `《${book.title}》已从当前书库移除。`)
  await loadBooks()
}

const handleCreateShelf = async () => {
  if (!shelfForm.shelfName?.trim()) {
    notifyError('请先填写书架名称')
    return
  }

  creatingShelf.value = true

  try {
    await createShelf(shelfForm)
    notifySuccess('书架已创建', '新的收纳位已经加入书架列表。')
    shelfForm.shelfName = ''
    shelfForm.address = ''
    shelfForm.isPublic = false
    shelfForm.remark = ''
    shelfForm.shelfType = 1
    await loadShelves()
  } finally {
    creatingShelf.value = false
  }
}

const handleDeleteShelf = async (shelf: Shelf) => {
  await deleteShelf(shelf.id)
  notifySuccess('书架已删除', `${shelf.shelfName} 已从当前列表移除。`)
  await loadShelves()
}

const handleAttachToShelf = async () => {
  if (!attachForm.bookId || !attachForm.shelfId) {
    notifyError('请选择图书和书架', '入架工作台需要同时知道目标图书和目标书架。')
    return
  }

  attachingShelf.value = true

  try {
    await onShelf({
      book_id: attachForm.bookId,
      shelf_id: attachForm.shelfId,
    })
    notifySuccess('图书已入架', '这本书现在已经与选定书架关联。')
    await loadPage()
  } finally {
    attachingShelf.value = false
  }
}

useRegisterPageRefresh(loadPage)

onMounted(loadPage)
</script>

<template>
  <div class="page-shell page-stack">
    <PageIntro
      eyebrow="Library Desk"
      title="把图书、书架和补录动作集中在一张桌面"
      description="这里不是传统表格后台，而是一块围绕录书和整理过程设计的纸感操作台。"
    />

    <section class="page-grid books-layout">
      <SectionPanel
        title="筛选与录入"
        hint="先用筛选看清现有藏书，再通过 ISBN 或手动表单新增。"
      >
        <div class="field-grid">
          <div class="field">
            <label>关键词</label>
            <input v-model="filters.keyword" type="text" placeholder="标题 / 作者 / 出版社" />
          </div>
          <div class="field">
            <label>分类</label>
            <select v-model="filters.classify">
              <option value="">全部分类</option>
              <option v-for="item in classifyOptions" :key="item" :value="item">{{ item }}</option>
            </select>
          </div>
          <div class="field">
            <label>书架</label>
            <select v-model="filters.shelfId">
              <option value="">全部书架</option>
              <option v-for="item in shelves" :key="item.id" :value="item.id">{{ item.shelfName }}</option>
            </select>
          </div>
        </div>

        <div class="inline-actions">
          <button class="button button--secondary" type="button" @click="loadBooks">应用筛选</button>
          <button
            class="button button--ghost"
            type="button"
            @click="
              filters.keyword = '';
              filters.classify = '';
              filters.shelfId = '';
              loadBooks();
            "
          >
            清空
          </button>
        </div>

        <div class="split-line" />

        <div class="field">
          <label>ISBN 自动补全</label>
          <div class="inline-composer">
            <input v-model="createForm.isbn" type="text" placeholder="输入 ISBN" />
            <button class="button button--secondary" type="button" :disabled="isbnLoading" @click="handleAutofillByIsbn">
              {{ isbnLoading ? '检索中...' : '自动补全' }}
            </button>
          </div>
        </div>

        <div class="field-grid">
          <div class="field">
            <label>书名</label>
            <input v-model="createForm.title" type="text" placeholder="请输入书名" />
          </div>
          <div class="field">
            <label>作者</label>
            <input v-model="createForm.author" type="text" placeholder="作者姓名" />
          </div>
          <div class="field">
            <label>出版社</label>
            <input v-model="createForm.publisher" type="text" placeholder="出版社" />
          </div>
          <div class="field">
            <label>出版日期</label>
            <input v-model="createForm.publishDate" type="date" />
          </div>
          <div class="field">
            <label>价格</label>
            <input v-model.number="createForm.price" type="number" min="0" step="0.01" placeholder="价格" />
          </div>
          <div class="field">
            <label>页数</label>
            <input v-model.number="createForm.pageCount" type="number" min="1" step="1" placeholder="页数" />
          </div>
          <div class="field">
            <label>分类</label>
            <input v-model="createForm.classify" type="text" placeholder="如：文学 / 商业 / 设计" />
          </div>
          <div class="field">
            <label>标签</label>
            <input v-model="createForm.label" type="text" placeholder="多个标签用逗号分隔" />
          </div>
        </div>

        <div class="field">
          <label>图书简介</label>
          <textarea v-model="createForm.summary" placeholder="输入简介、摘录或录入备注" />
        </div>

        <div class="field">
          <label>封面</label>
          <div class="inline-composer">
            <input v-model="createForm.coverUrl" type="text" placeholder="可填图片 URL 或上传返回的 key" />
            <label class="button button--ghost upload-button">
              {{ uploadingCover ? '上传中...' : '上传封面' }}
              <input type="file" accept="image/*" :disabled="uploadingCover" @change="handleCoverUpload" />
            </label>
          </div>
        </div>

        <div class="toggle-row">
          <label><input v-model="createForm.isOnShelf" type="checkbox" /> 已上架</label>
          <label><input v-model="createForm.isBorrowed" type="checkbox" /> 借阅中</label>
        </div>

        <button class="button button--primary" type="button" :disabled="creatingBook" @click="handleCreateBook">
          {{ creatingBook ? '保存中...' : '保存到书库' }}
        </button>
      </SectionPanel>

      <SectionPanel
        title="图书清单"
        hint="点击卡片可以去详情页，或把某一本拉到快速编辑与入架工作台。"
      >
        <LoadingState v-if="loading && books.length === 0" />
        <div v-else class="books-grid">
          <BookCard
            v-for="book in books"
            :key="book.id"
            :book="
              buildBookCard({
                id: book.id,
                title: book.title,
                author: book.author,
                coverUrl: book.coverUrl,
                summary: book.summary,
                secondary: `${formatCurrency(book.price)} · ${book.publisher || '出版社待补充'}`,
                badge: book.isBorrowed ? '借阅中' : book.isOnShelf ? '已上架' : '',
              })
            "
          >
            <template #actions>
              <button class="button button--ghost" type="button" @click="router.push(`/books/${book.id}`)">详情</button>
              <button class="button button--secondary" type="button" @click="startEdit(book)">快改</button>
              <button class="button button--ghost" type="button" @click="prefillAttach(book)">入架</button>
              <button class="button button--danger" type="button" @click="handleDeleteBook(book)">删除</button>
            </template>
          </BookCard>

          <EmptyState v-if="!loading && books.length === 0" title="当前筛选下没有图书" />
        </div>
      </SectionPanel>

      <SectionPanel
        title="快速编辑与入架"
        hint="后端目前开放的是轻量编辑接口，所以这里专注改价格、备注和书架关联。"
      >
        <div class="field-grid">
          <div class="field">
            <label>图书 ID</label>
            <input v-model.number="editForm.id" type="number" min="0" placeholder="先从图书卡片点“快改”" />
          </div>
          <div class="field">
            <label>价格</label>
            <input v-model.number="editForm.price" type="number" min="0" step="0.01" placeholder="价格" />
          </div>
        </div>

        <div class="field">
          <label>备注</label>
          <textarea v-model="editForm.remark" placeholder="如：版本信息、购入备注、阅读计划" />
        </div>

        <button class="button button--secondary" type="button" :disabled="editingBook" @click="handleUpdateBook">
          {{ editingBook ? '更新中...' : '保存轻量修改' }}
        </button>

        <div class="split-line" />

        <div class="field-grid">
          <div class="field">
            <label>待入架图书</label>
            <select v-model.number="attachForm.bookId">
              <option :value="0">选择图书</option>
              <option v-for="book in books" :key="book.id" :value="book.id">{{ book.title }}</option>
            </select>
          </div>
          <div class="field">
            <label>目标书架</label>
            <select v-model.number="attachForm.shelfId">
              <option :value="0">选择书架</option>
              <option v-for="shelf in shelves" :key="shelf.id" :value="shelf.id">{{ shelf.shelfName }}</option>
            </select>
          </div>
        </div>

        <button class="button button--primary" type="button" :disabled="attachingShelf" @click="handleAttachToShelf">
          {{ attachingShelf ? '入架中...' : '确认入架' }}
        </button>

        <div class="split-line" />

        <div class="field-grid">
          <div class="field">
            <label>新书架名称</label>
            <input v-model="shelfForm.shelfName" type="text" placeholder="如：客厅主书架" />
          </div>
          <div class="field">
            <label>位置</label>
            <input v-model="shelfForm.address" type="text" placeholder="物理位置或用途说明" />
          </div>
          <div class="field">
            <label>书架类型</label>
            <select v-model.number="shelfForm.shelfType">
              <option :value="1">实体书架</option>
              <option :value="2">虚拟书单</option>
            </select>
          </div>
          <div class="field">
            <label>公开可见</label>
            <select v-model="shelfForm.isPublic">
              <option :value="false">仅自己可见</option>
              <option :value="true">公开</option>
            </select>
          </div>
        </div>

        <div class="field">
          <label>书架备注</label>
          <textarea v-model="shelfForm.remark" placeholder="介绍这个书架收纳的主题或用途" />
        </div>

        <button class="button button--secondary" type="button" :disabled="creatingShelf" @click="handleCreateShelf">
          {{ creatingShelf ? '创建中...' : '创建书架' }}
        </button>

        <ul class="shelf-list list-reset">
          <li v-for="shelf in shelves" :key="shelf.id">
            <div>
              <strong>{{ shelf.shelfName }}</strong>
              <p>{{ shelf.address || '位置待补充' }} · {{ shelf.shelfType === 2 ? '虚拟书单' : '实体书架' }}</p>
            </div>
            <button class="button button--ghost" type="button" @click="handleDeleteShelf(shelf)">删除</button>
          </li>
        </ul>
      </SectionPanel>
    </section>
  </div>
</template>

<style scoped>
.books-layout > *:nth-child(1),
.books-layout > *:nth-child(3) {
  grid-column: span 4;
}

.books-layout > *:nth-child(2) {
  grid-column: span 8;
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.inline-composer {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
}

.toggle-row {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  color: var(--sl-ink-soft);
}

.toggle-row label {
  display: inline-flex;
  gap: 8px;
  align-items: center;
}

.upload-button {
  position: relative;
  overflow: hidden;
}

.upload-button input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

.split-line {
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(34, 48, 67, 0.18), transparent);
}

.books-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.shelf-list {
  display: grid;
  gap: 12px;
}

.shelf-list li {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.54);
}

.shelf-list p {
  margin: 6px 0 0;
  color: var(--sl-ink-soft);
}

@media (max-width: 1200px) {
  .books-layout > * {
    grid-column: span 12 !important;
  }
}

@media (max-width: 720px) {
  .field-grid,
  .inline-composer,
  .books-grid {
    grid-template-columns: 1fr;
  }
}
</style>
