<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'

import {
  createBook,
  createShelf,
  deleteBook,
  deleteShelf,
  getBookDetail,
  getBookList,
  getIsbnBook,
  getShelves,
  onShelf,
  updateShelf,
  updateBook,
} from '@/api/book'
import { uploadPicture } from '@/api/file'
import BookCard from '@/components/BookCard.vue'
import BookMetadataForm from '@/components/BookMetadataForm.vue'
import EmptyState from '@/components/EmptyState.vue'
import LoadingState from '@/components/LoadingState.vue'
import SectionPanel from '@/components/SectionPanel.vue'
import { useRegisterPageRefresh } from '@/composables/usePageRefresh'
import type {
  Book,
  BookFormPayload,
  BookUpdatePayload,
  Shelf,
  ShelfPayload,
} from '@/types/models'
import {
  buildBookCard,
  formatCurrency,
  formatDate,
  normalizePage,
  parseTagList,
  resolvePictureUrl,
} from '@/utils/format'
import { notifyError, notifySuccess } from '@/utils/notify'

const loading = ref(false)
const creatingBook = ref(false)
const editingBook = ref(false)
const creatingShelf = ref(false)
const attachingShelf = ref(false)
const isbnLoading = ref(false)
const uploadingCover = ref(false)
const detailLoading = ref(false)
const deletingBook = ref(false)
const deletingShelf = ref(false)
const editSummary = ref('')

const showCreateDialog = ref(false)
const showDetailDialog = ref(false)
const showDeleteDialog = ref(false)
const showShelfDeleteDialog = ref(false)
const showEditDialog = ref(false)
const showAttachDialog = ref(false)
const showShelfDialog = ref(false)

const books = ref<Book[]>([])
const shelves = ref<Shelf[]>([])
const detailBook = ref<Book | null>(null)
const deleteTarget = ref<Book | null>(null)
const shelfDeleteTarget = ref<Shelf | null>(null)

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
  publisher: '',
  publishDate: '',
  price: null,
  pageCount: null,
  binding: '',
  isbn: '',
  isbn10: '',
  coverUrl: '',
  classify: '',
  keyword: '',
  edition: '',
  impression: '',
  language: '',
  bookFormat: '',
  cip: '',
  clc: '',
  label: '',
  remark: '',
  shelfId: null,
  isOnShelf: false,
  isBorrowed: false,
})

const editForm = reactive<BookUpdatePayload>({
  id: 0,
  title: '',
  author: '',
  subtitle: '',
  publisher: '',
  publishDate: '',
  pageCount: null,
  price: null,
  binding: '',
  isbn: '',
  isbn10: '',
  keyword: '',
  edition: '',
  impression: '',
  language: '',
  bookFormat: '',
  classify: '',
  cip: '',
  clc: '',
  label: '',
  remark: '',
  coverUrl: '',
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
const editingShelfId = ref<number | null>(null)

const classifyOptions = computed(() =>
  Array.from(new Set(books.value.map((item) => item.classify).filter(Boolean))),
)

const creatingBookTitle = computed(() => createForm.title || '新图书')
const createCoverPreviewUrl = computed(() =>
  resolvePictureUrl(createForm.coverUrl),
)
const editingBookTitle = computed(
  () =>
    books.value.find((item) => item.id === editForm.id)?.title || '当前图书',
)
const editCoverPreviewUrl = computed(() => resolvePictureUrl(editForm.coverUrl))
const attachBookTitle = computed(
  () =>
    books.value.find((item) => item.id === attachForm.bookId)?.title ||
    '当前图书',
)
const isEditingShelf = computed(() => editingShelfId.value !== null)
const detailTagItems = computed(() => parseTagList(detailBook.value?.label))
const detailKeywordItems = computed(() =>
  parseTagList(detailBook.value?.keyword),
)
const detailCoverUrl = computed(() =>
  resolvePictureUrl(detailBook.value?.coverUrl),
)
const detailMetaItems = computed(() => {
  const book = detailBook.value

  if (!book) {
    return []
  }

  const hasText = (value?: string | number | null) => {
    if (value === undefined || value === null) {
      return false
    }

    if (typeof value === 'number') {
      return !Number.isNaN(value)
    }

    return value.trim().length > 0
  }

  return [
    {
      label: '价格',
      value: hasText(book.price) ? formatCurrency(book.price) : '',
    },
    {
      label: '出版社',
      value: hasText(book.publisher) ? book.publisher : '',
    },
    {
      label: '出版时间',
      value: hasText(book.publishDate) ? formatDate(book.publishDate) : '',
    },
    {
      label: '页数',
      value: hasText(book.pageCount) ? String(book.pageCount) : '',
    },
    {
      label: 'ISBN',
      value: hasText(book.isbn) ? book.isbn : '',
    },
    {
      label: 'ISBN-10',
      value: hasText(book.isbn10) ? book.isbn10 : '',
    },
    {
      label: '装帧',
      value: hasText(book.binding) ? book.binding : '',
    },
    {
      label: '语言',
      value: hasText(book.language) ? book.language : '',
    },
    {
      label: '版次',
      value: hasText(book.edition) ? book.edition : '',
    },
    {
      label: '印次',
      value: hasText(book.impression) ? book.impression : '',
    },
    {
      label: '开本',
      value: hasText(book.bookFormat) ? book.bookFormat : '',
    },
    {
      label: 'CIP',
      value: hasText(book.cip) ? book.cip : '',
    },
    {
      label: '中图法分类',
      value: hasText(book.clc) ? book.clc : '',
    },
  ].map((item) => ({
    ...item,
    empty: !item.value,
  }))
})
const hasOpenDialog = computed(
  () =>
    showCreateDialog.value ||
    showDetailDialog.value ||
    showDeleteDialog.value ||
    showShelfDeleteDialog.value ||
    showEditDialog.value ||
    showAttachDialog.value ||
    showShelfDialog.value,
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

const handleApplyFilters = async () => {
  pagination.current = 1
  await loadBooks()
}

const handleResetFilters = async () => {
  filters.keyword = ''
  filters.classify = ''
  filters.shelfId = ''
  pagination.current = 1
  await loadBooks()
}

const resetCreateForm = () => {
  createForm.title = ''
  createForm.author = ''
  createForm.subtitle = ''
  createForm.publisher = ''
  createForm.publishDate = ''
  createForm.price = null
  createForm.pageCount = null
  createForm.binding = ''
  createForm.isbn = ''
  createForm.isbn10 = ''
  createForm.coverUrl = ''
  createForm.classify = ''
  createForm.keyword = ''
  createForm.edition = ''
  createForm.impression = ''
  createForm.language = ''
  createForm.bookFormat = ''
  createForm.cip = ''
  createForm.clc = ''
  createForm.label = ''
  createForm.remark = ''
  createForm.shelfId = null
  createForm.isOnShelf = false
  createForm.isBorrowed = false
}

const resetEditForm = () => {
  editForm.id = 0
  editForm.title = ''
  editForm.author = ''
  editForm.subtitle = ''
  editForm.publisher = ''
  editForm.publishDate = ''
  editForm.pageCount = null
  editForm.price = null
  editForm.binding = ''
  editForm.isbn = ''
  editForm.isbn10 = ''
  editForm.keyword = ''
  editForm.edition = ''
  editForm.impression = ''
  editForm.language = ''
  editForm.bookFormat = ''
  editForm.classify = ''
  editForm.cip = ''
  editForm.clc = ''
  editForm.label = ''
  editForm.remark = ''
  editForm.coverUrl = ''
  editSummary.value = ''
}

const resetShelfForm = () => {
  editingShelfId.value = null
  shelfForm.shelfName = ''
  shelfForm.address = ''
  shelfForm.isPublic = false
  shelfForm.remark = ''
  shelfForm.shelfType = 1
}

const normalizeIsbnInput = (value: string) =>
  value.replace(/[^0-9Xx]/g, '').toUpperCase()

const normalizeOptionalNumber = (value?: number | string | null) => {
  if (value === '' || value === null || value === undefined) {
    return null
  }

  const normalized = Number(value)
  return Number.isFinite(normalized) ? normalized : null
}

const normalizePublishDateInput = (value?: string | null) => {
  const rawValue = value?.trim()

  if (!rawValue) {
    return ''
  }

  const normalized = rawValue
    .replace(/[./,，]/g, '-')
    .replace(/[－–—]/g, '-')
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

const closeAllDialogs = () => {
  showCreateDialog.value = false
  showDetailDialog.value = false
  showDeleteDialog.value = false
  showShelfDeleteDialog.value = false
  showEditDialog.value = false
  showAttachDialog.value = false
  showShelfDialog.value = false
}

const openCreateDialog = () => {
  closeAllDialogs()
  showCreateDialog.value = true
}

const closeCreateDialog = () => {
  showCreateDialog.value = false
}

const openDetailDialog = async (book: Book) => {
  closeAllDialogs()
  detailBook.value = book
  detailLoading.value = true
  showDetailDialog.value = true

  try {
    detailBook.value = await getBookDetail(book.id)
  } catch {
    notifyError(
      '详情加载失败',
      '先展示当前列表中的基础信息，你可以稍后再试一次。',
    )
  } finally {
    detailLoading.value = false
  }
}

const closeDetailDialog = () => {
  showDetailDialog.value = false
}

const openDeleteDialog = (book: Book) => {
  closeAllDialogs()
  deleteTarget.value = book
  showDeleteDialog.value = true
}

const closeDeleteDialog = () => {
  showDeleteDialog.value = false
}

const openEditDialog = (book: Book) => {
  closeAllDialogs()
  editForm.id = book.id
  editForm.title = book.title || ''
  editForm.author = book.author || ''
  editForm.subtitle = book.subtitle || ''
  editForm.publisher = book.publisher || ''
  editForm.publishDate = normalizePublishDateInput(book.publishDate) || ''
  editForm.pageCount = book.pageCount ?? null
  editForm.price = book.price ?? null
  editForm.binding = book.binding || ''
  editForm.isbn = normalizeIsbnInput(book.isbn || '')
  editForm.isbn10 = normalizeIsbnInput(book.isbn10 || '')
  editForm.keyword = book.keyword || ''
  editForm.edition = book.edition || ''
  editForm.impression = book.impression || ''
  editForm.language = book.language || ''
  editForm.bookFormat = book.bookFormat || ''
  editForm.classify = book.classify || ''
  editForm.cip = book.cip || ''
  editForm.clc = book.clc || ''
  editForm.label = book.label || ''
  editForm.remark = book.remark || ''
  editForm.coverUrl = book.coverUrl || ''
  editSummary.value = (book.summary || '').trim()
  showEditDialog.value = true
}

const closeEditDialog = () => {
  showEditDialog.value = false
  resetEditForm()
}

const openAttachDialog = (book: Book) => {
  closeAllDialogs()
  attachForm.bookId = book.id
  attachForm.shelfId = Number(filters.shelfId || shelves.value[0]?.id || 0)
  showAttachDialog.value = true
}

const closeAttachDialog = () => {
  showAttachDialog.value = false
}

const openShelfDialog = () => {
  closeAllDialogs()
  resetShelfForm()
  showShelfDialog.value = true
}

const closeShelfDialog = () => {
  showShelfDialog.value = false
  resetShelfForm()
}

const openShelfEdit = (shelf: Shelf) => {
  editingShelfId.value = shelf.id
  shelfForm.shelfName = shelf.shelfName || ''
  shelfForm.address = shelf.address || ''
  shelfForm.isPublic = Boolean(shelf.isPublic)
  shelfForm.remark = shelf.remark || ''
  shelfForm.shelfType = Number(shelf.shelfType || 1)
}

const openShelfDeleteDialog = (shelf: Shelf) => {
  shelfDeleteTarget.value = shelf
  showShelfDeleteDialog.value = true
}

const closeShelfDeleteDialog = () => {
  showShelfDeleteDialog.value = false
  shelfDeleteTarget.value = null
}

const handleAutofillByIsbn = async () => {
  const normalizedIsbn = normalizeIsbnInput(createForm.isbn || '')

  if (!normalizedIsbn) {
    notifyError('请输入 ISBN', 'ISBN 是自动补全图书信息的触发条件。')
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
    createForm.coverUrl = result.coverUrl || createForm.coverUrl
    createForm.publishDate =
      normalizePublishDateInput(result.publishDate) || createForm.publishDate
    createForm.keyword = result.keyword || createForm.keyword
    createForm.binding = result.binding || createForm.binding
    createForm.language = result.language || createForm.language
    createForm.edition = result.edition || createForm.edition
    createForm.impression = result.impression || createForm.impression
    createForm.bookFormat = result.bookFormat || createForm.bookFormat
    createForm.cip = result.cip || createForm.cip
    createForm.clc = result.clc || createForm.clc
    createForm.isbn = result.isbn || createForm.isbn
    createForm.isbn10 = result.isbn10 || createForm.isbn10
    createForm.pageCount = result.pageCount
      ? Number(result.pageCount)
      : createForm.pageCount
    createForm.price = result.price
      ? Number(String(result.price).replace(/[^\d.]/g, ''))
      : createForm.price
    notifySuccess('图书信息已补全', '请校对关键字段后再保存。')
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

const handleEditCoverUpload = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]

  if (!file) {
    return
  }

  uploadingCover.value = true

  try {
    const result = await uploadPicture(file)
    editForm.coverUrl = result.pic
    notifySuccess('封面已上传', '保存修改后会同步更新图书卡片和详情。')
  } finally {
    uploadingCover.value = false
    input.value = ''
  }
}

const handleCreateBook = async () => {
  if (!createForm.title.trim()) {
    notifyError('图书标题不能为空', '请至少填写图书标题后再保存。')
    return
  }

  creatingBook.value = true

  try {
    const normalizedPublishDate = normalizePublishDateInput(
      createForm.publishDate,
    )
    createForm.publishDate = normalizedPublishDate

    await createBook({
      title: createForm.title.trim(),
      author: (createForm.author || '').trim(),
      subtitle: (createForm.subtitle || '').trim(),
      publisher: (createForm.publisher || '').trim(),
      publishDate: normalizedPublishDate || undefined,
      pageCount: normalizeOptionalNumber(createForm.pageCount),
      price: normalizeOptionalNumber(createForm.price),
      binding: (createForm.binding || '').trim(),
      isbn: normalizeIsbnInput(createForm.isbn || ''),
      isbn10: normalizeIsbnInput(createForm.isbn10 || ''),
      keyword: (createForm.keyword || '').trim(),
      edition: (createForm.edition || '').trim(),
      impression: (createForm.impression || '').trim(),
      language: (createForm.language || '').trim(),
      bookFormat: (createForm.bookFormat || '').trim(),
      classify: (createForm.classify || '').trim(),
      cip: (createForm.cip || '').trim(),
      clc: (createForm.clc || '').trim(),
      label: (createForm.label || '').trim(),
      remark: (createForm.remark || '').trim(),
      coverUrl: (createForm.coverUrl || '').trim(),
      shelfId: createForm.shelfId,
      isOnShelf: createForm.isOnShelf || Boolean(createForm.shelfId),
      isBorrowed: createForm.isBorrowed,
    })
    notifySuccess('图书已加入书库', '新的藏书已经出现在列表中。')
    resetCreateForm()
    closeCreateDialog()
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

  if (!(editForm.title || '').trim()) {
    notifyError('图书标题不能为空', '请至少保留书名后再保存。')
    return
  }

  editingBook.value = true

  try {
    const normalizedPublishDate = normalizePublishDateInput(
      editForm.publishDate,
    )
    const updatedBook = await updateBook({
      ...editForm,
      title: (editForm.title || '').trim(),
      author: (editForm.author || '').trim(),
      subtitle: (editForm.subtitle || '').trim(),
      publisher: (editForm.publisher || '').trim(),
      publishDate: normalizedPublishDate || undefined,
      pageCount: normalizeOptionalNumber(editForm.pageCount),
      price: normalizeOptionalNumber(editForm.price),
      binding: (editForm.binding || '').trim(),
      isbn: normalizeIsbnInput(editForm.isbn || ''),
      isbn10: normalizeIsbnInput(editForm.isbn10 || ''),
      keyword: (editForm.keyword || '').trim(),
      edition: (editForm.edition || '').trim(),
      impression: (editForm.impression || '').trim(),
      language: (editForm.language || '').trim(),
      bookFormat: (editForm.bookFormat || '').trim(),
      classify: (editForm.classify || '').trim(),
      cip: (editForm.cip || '').trim(),
      clc: (editForm.clc || '').trim(),
      label: (editForm.label || '').trim(),
      remark: (editForm.remark || '').trim(),
      coverUrl: (editForm.coverUrl || '').trim(),
    })
    notifySuccess('图书信息已更新', '图书元数据已经同步回图书列表。')
    if (detailBook.value?.id === updatedBook.id) {
      detailBook.value = updatedBook
    }
    closeEditDialog()
    await loadBooks()
  } finally {
    editingBook.value = false
  }
}

const handleDeleteBook = async () => {
  if (!deleteTarget.value) {
    return
  }

  deletingBook.value = true

  try {
    await deleteBook(deleteTarget.value.id)
    notifySuccess(
      '图书已删除',
      `《${deleteTarget.value.title}》已经从当前列表移除。`,
    )
    closeDeleteDialog()
    await loadBooks()
  } finally {
    deletingBook.value = false
  }
}

const handleCreateShelf = async () => {
  if (!shelfForm.shelfName?.trim()) {
    notifyError('请先填写书架名称')
    return
  }

  creatingShelf.value = true

  const payload: ShelfPayload = {
    id: editingShelfId.value ?? undefined,
    shelfName: shelfForm.shelfName.trim(),
    address: (shelfForm.address || '').trim(),
    isPublic: Boolean(shelfForm.isPublic),
    remark: (shelfForm.remark || '').trim(),
    shelfType: Number(shelfForm.shelfType || 1),
  }

  try {
    if (editingShelfId.value) {
      await updateShelf(payload)
      notifySuccess('书架已更新', `${payload.shelfName} 的信息已保存。`)
    } else {
      await createShelf(payload)
      notifySuccess('书架已创建', '新的收纳位置已经加入可选书架。')
    }

    resetShelfForm()
    await loadShelves()
  } finally {
    creatingShelf.value = false
  }
}

const handleDeleteShelf = async () => {
  if (!shelfDeleteTarget.value) {
    return
  }

  deletingShelf.value = true

  try {
    const target = shelfDeleteTarget.value
    await deleteShelf(target.id)
    notifySuccess('书架已删除', `${target.shelfName} 已从当前列表移除。`)

    if (editingShelfId.value === target.id) {
      resetShelfForm()
    }

    if (createForm.shelfId === target.id) {
      createForm.shelfId = null
    }

    if (attachForm.shelfId === target.id) {
      attachForm.shelfId = 0
    }

    closeShelfDeleteDialog()

    if (String(filters.shelfId) === String(target.id)) {
      filters.shelfId = ''
      await Promise.all([loadShelves(), loadBooks()])
    } else {
      await loadShelves()
    }
  } finally {
    deletingShelf.value = false
  }
}

const handleAttachToShelf = async () => {
  if (!attachForm.bookId || !attachForm.shelfId) {
    notifyError('请选择图书和书架', '入架前需要同时确认目标图书和目标书架。')
    return
  }

  attachingShelf.value = true

  try {
    await onShelf({
      book_id: attachForm.bookId,
      shelf_id: attachForm.shelfId,
    })
    notifySuccess('图书已入架', '这本书已经关联到你选择的书架。')
    closeAttachDialog()
    await loadPage()
  } finally {
    attachingShelf.value = false
  }
}

watch(hasOpenDialog, (open) => {
  document.body.style.overflow = open ? 'hidden' : ''
})

onUnmounted(() => {
  document.body.style.overflow = ''
})

useRegisterPageRefresh(loadPage)

onMounted(() => {
  loadPage()
})
</script>

<template>
  <div class="page-shell page-stack">
    <SectionPanel class="books-filter-panel">
      <div class="books-filter-toolbar">
        <div class="field books-filter-toolbar__search">
          <input
            v-model="filters.keyword"
            type="text"
            placeholder="关键词：标题 / 作者 / 出版社"
            aria-label="关键词筛选"
          />
        </div>
        <div class="books-filter-toolbar__group">
          <div class="field">
            <select v-model="filters.classify" aria-label="分类筛选">
              <option value="">全部分类</option>
              <option v-for="item in classifyOptions" :key="item" :value="item">
                {{ item }}
              </option>
            </select>
          </div>
          <div class="field">
            <select v-model="filters.shelfId" aria-label="书架筛选">
              <option value="">全部书架</option>
              <option v-for="item in shelves" :key="item.id" :value="item.id">
                {{ item.shelfName }}
              </option>
            </select>
          </div>
        </div>
        <div class="books-filter-toolbar__actions">
          <button
            class="button button--secondary books-filter-toolbar__button"
            type="button"
            @click="handleApplyFilters"
          >
            筛选
          </button>
          <button
            class="button button--ghost books-filter-toolbar__button"
            type="button"
            @click="handleResetFilters"
          >
            清空
          </button>
        </div>
      </div>
    </SectionPanel>

    <SectionPanel class="books-list-panel" title="图书清单">
      <template #actions>
        <button
          class="button button--ghost"
          type="button"
          @click="openShelfDialog"
        >
          管理书架
        </button>
        <button
          class="button button--primary"
          type="button"
          @click="openCreateDialog"
        >
          新增图书
        </button>
      </template>

      <LoadingState v-if="loading && books.length === 0" />
      <div v-else class="books-grid">
        <BookCard
          v-for="book in books"
          :key="book.id"
          interactive
          mobile-minimal
          @open="openDetailDialog(book)"
          :book="
            buildBookCard({
              id: book.id,
              title: book.title,
              author: book.author,
              coverUrl: book.coverUrl,
              summary: book.summary,
              secondary: `${formatCurrency(book.price)} · ${book.publisher || '出版社待补充'}`,
              badge: book.isBorrowed
                ? '借阅中'
                : book.isOnShelf
                  ? '已上架'
                  : '',
            })
          "
        >
          <template #actions>
            <button
              class="button button--ghost book-card-action"
              type="button"
              @click="openDetailDialog(book)"
            >
              详情
            </button>
            <button
              class="button button--secondary book-card-action"
              type="button"
              @click="openEditDialog(book)"
            >
              编辑
            </button>
            <button
              class="button button--ghost book-card-action"
              type="button"
              @click="openAttachDialog(book)"
            >
              入架
            </button>
            <button
              class="button button--danger book-card-action"
              type="button"
              @click="openDeleteDialog(book)"
            >
              删除
            </button>
          </template>
        </BookCard>

        <EmptyState
          v-if="!loading && books.length === 0"
          title="当前筛选下没有图书"
        />
      </div>
    </SectionPanel>

    <Teleport to="body">
      <div
        v-if="showDetailDialog"
        class="dialog-scrim"
        @click.self="closeDetailDialog"
      >
        <section class="surface-card desk-dialog desk-dialog--detail">
          <header class="desk-dialog__head">
            <div>
              <span class="eyebrow">Detail Card</span>
              <h2>{{ detailBook?.title || '图书详情' }}</h2>
              <p>{{ detailBook?.author || '作者待补充' }}</p>
            </div>

            <button
              class="button button--ghost desk-dialog__close"
              type="button"
              @click="closeDetailDialog"
            >
              关闭
            </button>
          </header>

          <LoadingState
            v-if="detailLoading && !detailBook"
            title="正在载入图书详情"
          />

          <div v-else-if="detailBook" class="detail-card">
            <div class="detail-card__cover">
              <img
                v-if="detailCoverUrl"
                :src="detailCoverUrl"
                :alt="detailBook.title"
                loading="lazy"
              />
              <div v-else class="detail-card__placeholder serif-title">
                BOOK
              </div>
            </div>

            <div class="detail-card__copy">
              <div class="detail-card__badges inline-actions">
                <span v-if="detailBook.classify" class="badge">{{
                  detailBook.classify
                }}</span>
                <span v-if="detailBook.isBorrowed" class="badge badge--accent"
                  >借阅中</span
                >
                <span v-else-if="detailBook.isOnShelf" class="badge"
                  >已上架</span
                >
              </div>

              <section v-if="detailBook.subtitle" class="detail-card__section">
                <h3>副标题</h3>
                <p>{{ detailBook.subtitle }}</p>
              </section>

              <div class="detail-card__meta">
                <article
                  v-for="item in detailMetaItems"
                  :key="item.label"
                  :class="{ 'detail-card__meta-item--empty': item.empty }"
                >
                  <span>{{ item.label }}</span>
                  <strong>{{ item.value }}</strong>
                </article>
              </div>

              <div v-if="detailTagItems.length" class="inline-actions">
                <span
                  v-for="item in detailTagItems"
                  :key="item"
                  class="badge"
                  >{{ item }}</span
                >
              </div>

              <section
                v-if="detailKeywordItems.length"
                class="detail-card__section"
              >
                <h3>关键词</h3>
                <div class="detail-card__chips">
                  <span
                    v-for="item in detailKeywordItems"
                    :key="item"
                    class="badge badge--soft"
                    >{{ item }}</span
                  >
                </div>
              </section>

              <section class="detail-card__section">
                <h3>图书简介</h3>
                <p>{{ detailBook.summary || '这本书暂时还没有补充简介。' }}</p>
              </section>

              <section class="detail-card__section">
                <h3>录入备注</h3>
                <p>{{ detailBook.remark || '当前还没有录入备注。' }}</p>
              </section>
            </div>
          </div>

          <footer class="desk-dialog__foot desk-dialog__foot--align-end">
            <button
              v-if="detailBook"
              class="button button--secondary"
              type="button"
              @click="openEditDialog(detailBook)"
            >
              编辑
            </button>
          </footer>
        </section>
      </div>

      <div
        v-if="showDeleteDialog"
        class="dialog-scrim"
        @click.self="closeDeleteDialog"
      >
        <section class="surface-card desk-dialog desk-dialog--compact">
          <header class="desk-dialog__head">
            <div>
              <span class="eyebrow">Delete Book</span>
              <h2>确认删除</h2>
              <p>
                {{
                  deleteTarget
                    ? `《${deleteTarget.title}》删除后将从当前列表移除。`
                    : '确认是否删除这本书。'
                }}
              </p>
            </div>

            <button
              class="button button--ghost desk-dialog__close"
              type="button"
              @click="closeDeleteDialog"
            >
              关闭
            </button>
          </header>

          <footer class="desk-dialog__foot desk-dialog__foot--danger-only">
            <button
              class="button button--danger"
              type="button"
              :disabled="deletingBook"
              @click="handleDeleteBook"
            >
              {{ deletingBook ? '删除中...' : '确认删除' }}
            </button>
          </footer>
        </section>
      </div>

      <div
        v-if="showCreateDialog"
        class="dialog-scrim"
        @click.self="closeCreateDialog"
      >
        <section class="surface-card desk-dialog desk-dialog--wide">
          <header class="desk-dialog__head">
            <div>
              <span class="eyebrow">New Book</span>
              <h2>录入新书</h2>
            </div>

            <button
              class="button button--ghost desk-dialog__close"
              type="button"
              @click="closeCreateDialog"
            >
              关闭
            </button>
          </header>

          <div class="desk-dialog__body">
            <BookMetadataForm
              :form="createForm"
              :cover-preview-url="createCoverPreviewUrl"
              :cover-alt="creatingBookTitle"
              :uploading-cover="uploadingCover"
              @cover-upload="handleCoverUpload"
            >
              <template #before-grid>
                <div class="field">
                  <label>ISBN 自动补全</label>
                  <div class="inline-composer">
                    <input
                      v-model="createForm.isbn"
                      type="text"
                      placeholder="输入 ISBN"
                    />
                    <button
                      class="button button--secondary"
                      type="button"
                      :disabled="isbnLoading"
                      @click="handleAutofillByIsbn"
                    >
                      {{ isbnLoading ? '检索中...' : '自动补全' }}
                    </button>
                  </div>
                </div>
              </template>

              <template #after-grid>
                <div class="field-grid field-grid--single">
                  <div class="field">
                    <label>目标书架</label>
                    <select v-model="createForm.shelfId">
                      <option :value="null">暂不入架</option>
                      <option
                        v-for="shelf in shelves"
                        :key="shelf.id"
                        :value="shelf.id"
                      >
                        {{ shelf.shelfName }}
                      </option>
                    </select>
                  </div>
                </div>

                <div class="toggle-row">
                  <label
                    ><input v-model="createForm.isOnShelf" type="checkbox" />
                    已上架</label
                  >
                  <label
                    ><input v-model="createForm.isBorrowed" type="checkbox" />
                    借阅中</label
                  >
                </div>
              </template>

              <template #aside-extra>
                <article
                  class="dialog-note summary-lock-note summary-lock-note--create"
                >
                  <strong>AI 摘要</strong>
                  <p>摘要将在新增图书后由 AI 自动生成</p>
                </article>
              </template>
            </BookMetadataForm>
          </div>

          <footer class="desk-dialog__foot">
            <button
              class="button button--ghost"
              type="button"
              @click="resetCreateForm"
            >
              清空表单
            </button>
            <button
              class="button button--primary"
              type="button"
              :disabled="creatingBook"
              @click="handleCreateBook"
            >
              {{ creatingBook ? '保存中...' : '保存图书' }}
            </button>
          </footer>
        </section>
      </div>

      <div
        v-if="showEditDialog"
        class="dialog-scrim"
        @click.self="closeEditDialog"
      >
        <section class="surface-card desk-dialog desk-dialog--wide">
          <header class="desk-dialog__head">
            <div>
              <span class="eyebrow">Book Metadata</span>
              <h2>编辑《{{ editingBookTitle }}》</h2>
            </div>

            <button
              class="button button--ghost desk-dialog__close"
              type="button"
              @click="closeEditDialog"
            >
              关闭
            </button>
          </header>

          <div class="desk-dialog__body">
            <BookMetadataForm
              :form="editForm"
              :cover-preview-url="editCoverPreviewUrl"
              :cover-alt="editingBookTitle"
              :uploading-cover="uploadingCover"
              @cover-upload="handleEditCoverUpload"
            >
              <template #summary>
                <article
                  class="dialog-note summary-lock-note summary-lock-note--readonly"
                >
                  <strong>AI 摘要</strong>
                  <p
                    class="summary-lock-note__content"
                    :class="{
                      'summary-lock-note__content--pending': !editSummary,
                    }"
                  >
                    {{ editSummary || '正在生成AI摘要中......' }}
                  </p>
                  <p class="summary-lock-note__hint">
                    AI 自动生成，当前不支持手动修改
                  </p>
                </article>
              </template>
            </BookMetadataForm>
          </div>

          <footer class="desk-dialog__foot desk-dialog__foot--align-end">
            <button
              class="button button--ghost"
              type="button"
              @click="closeEditDialog"
            >
              取消
            </button>
            <button
              class="button button--secondary"
              type="button"
              :disabled="editingBook"
              @click="handleUpdateBook"
            >
              {{ editingBook ? '更新中...' : '保存修改' }}
            </button>
          </footer>
        </section>
      </div>

      <div
        v-if="showAttachDialog"
        class="dialog-scrim"
        @click.self="closeAttachDialog"
      >
        <section class="surface-card desk-dialog desk-dialog--compact">
          <header class="desk-dialog__head">
            <div>
              <span class="eyebrow">On Shelf</span>
              <h2>把《{{ attachBookTitle }}》放进书架</h2>
            </div>

            <button
              class="button button--ghost desk-dialog__close"
              type="button"
              @click="closeAttachDialog"
            >
              关闭
            </button>
          </header>

          <div class="desk-dialog__body">
            <div class="field-grid field-grid--single">
              <div class="field">
                <label>目标书架</label>
                <select v-model.number="attachForm.shelfId">
                  <option :value="0">选择书架</option>
                  <option
                    v-for="shelf in shelves"
                    :key="shelf.id"
                    :value="shelf.id"
                  >
                    {{ shelf.shelfName }}
                  </option>
                </select>
              </div>
            </div>
          </div>

          <footer class="desk-dialog__foot">
            <button
              class="button button--ghost"
              type="button"
              @click="openShelfDialog"
            >
              管理书架
            </button>
            <button
              class="button button--primary"
              type="button"
              :disabled="attachingShelf"
              @click="handleAttachToShelf"
            >
              {{ attachingShelf ? '入架中...' : '确认入架' }}
            </button>
          </footer>
        </section>
      </div>

      <div
        v-if="showShelfDialog"
        class="dialog-scrim"
        @click.self="closeShelfDialog"
      >
        <section class="surface-card desk-dialog desk-dialog--compact">
          <header class="desk-dialog__head">
            <div>
              <span class="eyebrow">Shelf Desk</span>
              <h2>书架管理</h2>
            </div>

            <button
              class="button button--ghost desk-dialog__close"
              type="button"
              @click="closeShelfDialog"
            >
              关闭
            </button>
          </header>

          <div class="desk-dialog__body">
            <div class="field-grid">
              <div class="field">
                <label>{{ isEditingShelf ? '书架名称' : '新书架名称' }}</label>
                <input
                  v-model="shelfForm.shelfName"
                  type="text"
                  placeholder="例如：客厅主书架"
                />
              </div>
              <div class="field">
                <label>位置</label>
                <input
                  v-model="shelfForm.address"
                  type="text"
                  placeholder="物理位置或用途说明"
                />
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
              <textarea
                v-model="shelfForm.remark"
                placeholder="介绍这个书架收纳的主题或用途"
              />
            </div>

            <div class="shelf-form-actions">
              <button
                class="button button--secondary"
                type="button"
                :disabled="creatingShelf"
                @click="handleCreateShelf"
              >
                {{
                  creatingShelf
                    ? isEditingShelf
                      ? '保存中...'
                      : '创建中...'
                    : isEditingShelf
                      ? '保存'
                      : '创建书架'
                }}
              </button>
              <button
                v-if="isEditingShelf"
                class="button button--ghost"
                type="button"
                :disabled="creatingShelf"
                @click="resetShelfForm"
              >
                取消
              </button>
            </div>

            <ul class="shelf-list list-reset">
              <li v-for="shelf in shelves" :key="shelf.id">
                <div>
                  <strong>{{ shelf.shelfName }}</strong>
                  <p>{{ shelf.shelfType === 2 ? '虚拟书单' : '实体书架' }}</p>
                </div>
                <div class="shelf-list__actions">
                  <button
                    class="button button--ghost"
                    type="button"
                    @click="openShelfEdit(shelf)"
                  >
                    编辑
                  </button>
                  <button
                    class="button button--danger"
                    type="button"
                    @click="openShelfDeleteDialog(shelf)"
                  >
                    删除
                  </button>
                </div>
              </li>
            </ul>
          </div>
        </section>
      </div>

      <div
        v-if="showShelfDeleteDialog"
        class="dialog-scrim"
        @click.self="closeShelfDeleteDialog"
      >
        <section class="surface-card desk-dialog desk-dialog--compact">
          <header class="desk-dialog__head">
            <div>
              <span class="eyebrow">Delete Shelf</span>
              <h2>确认删除</h2>
              <p>
                {{
                  shelfDeleteTarget
                    ? `${shelfDeleteTarget.shelfName} 删除后将从当前书架列表移除。`
                    : '确认是否删除这个书架。'
                }}
              </p>
            </div>

            <button
              class="button button--ghost desk-dialog__close"
              type="button"
              @click="closeShelfDeleteDialog"
            >
              关闭
            </button>
          </header>

          <footer class="desk-dialog__foot desk-dialog__foot--danger-only">
            <button
              class="button button--danger"
              type="button"
              :disabled="deletingShelf"
              @click="handleDeleteShelf"
            >
              {{ deletingShelf ? '删除中...' : '确认删除' }}
            </button>
          </footer>
        </section>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.books-filter-panel {
  gap: 10px;
  padding: 16px 18px;
}

.books-filter-toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1.8fr) minmax(320px, 1.1fr) auto;
  gap: 14px;
  align-items: end;
}

.books-filter-toolbar__search {
  min-width: 0;
}

.books-filter-toolbar__search :deep(input),
.books-filter-toolbar__group :deep(select) {
  min-height: 42px;
}

.books-filter-toolbar__group {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.books-filter-toolbar__actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, auto));
  gap: 10px;
  align-items: end;
}

.books-filter-toolbar__button {
  min-width: 112px;
}

.books-list-panel :deep(.section-panel__head) {
  align-items: center;
}

.books-list-panel :deep(.section-panel__actions) {
  justify-content: flex-end;
}

.books-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.books-grid :deep(.book-card__actions) {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.books-grid :deep(.book-card__summary) {
  -webkit-line-clamp: 1;
}

.books-grid :deep(.book-card__actions .book-card-action) {
  min-height: 36px;
  padding: 0 8px;
  font-size: 0.88rem;
  white-space: nowrap;
}

.summary-lock-note {
  margin-top: 4px;
  gap: 12px;
}

.summary-lock-note--readonly {
  border-color: rgba(201, 119, 46, 0.28);
  background:
    linear-gradient(
      180deg,
      rgba(255, 248, 236, 0.92),
      rgba(255, 243, 224, 0.78)
    ),
    radial-gradient(
      circle at top right,
      rgba(201, 119, 46, 0.18),
      transparent 48%
    );
}

[data-theme='dark'] .summary-lock-note--readonly {
  border-color: rgba(255, 190, 116, 0.28);
  background:
    linear-gradient(180deg, rgba(48, 34, 18, 0.92), rgba(31, 24, 16, 0.88)),
    radial-gradient(
      circle at top right,
      rgba(255, 190, 116, 0.18),
      transparent 50%
    );
}

.summary-lock-note--create {
  gap: 8px;
}

.summary-lock-note__content {
  white-space: pre-wrap;
  color: var(--sl-ink);
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.56);
  border: 1px solid rgba(201, 119, 46, 0.14);
  line-height: 1.75;
}

[data-theme='dark'] .summary-lock-note__content {
  background: rgba(255, 248, 236, 0.06);
  border-color: rgba(255, 190, 116, 0.14);
}

.summary-lock-note__content--pending {
  color: var(--sl-ink-soft);
  font-style: italic;
}

.summary-lock-note__hint {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 40px;
  padding: 0 14px;
  border-radius: 999px;
  font-size: 0.92rem;
  font-weight: 700;
  letter-spacing: 0.01em;
  color: #a44b12;
  background: rgba(255, 214, 170, 0.72);
  border: 1px solid rgba(201, 119, 46, 0.2);
}

[data-theme='dark'] .summary-lock-note__hint {
  color: #ffd6ad;
  background: rgba(255, 190, 116, 0.12);
  border-color: rgba(255, 190, 116, 0.22);
}

.dialog-scrim {
  position: fixed;
  inset: 0;
  z-index: 60;
  display: grid;
  place-items: center;
  padding: 24px;
  background:
    radial-gradient(circle at top, rgba(31, 95, 107, 0.2), transparent 28%),
    rgba(17, 27, 36, 0.48);
  backdrop-filter: blur(12px);
}

.desk-dialog {
  width: min(100%, 1100px);
  max-height: min(88vh, 920px);
  display: grid;
  gap: 18px;
  padding: 24px;
  overflow: auto;
  border-radius: 30px;
}

.desk-dialog--wide {
  background:
    linear-gradient(
      180deg,
      rgba(255, 252, 246, 0.98),
      rgba(245, 238, 228, 0.96)
    ),
    radial-gradient(
      circle at top right,
      rgba(31, 95, 107, 0.12),
      transparent 36%
    );
}

[data-theme='dark'] .desk-dialog--wide {
  background:
    linear-gradient(180deg, rgba(18, 28, 37, 0.98), rgba(15, 23, 32, 0.96)),
    radial-gradient(
      circle at top right,
      rgba(108, 185, 199, 0.14),
      transparent 36%
    );
}

.desk-dialog--compact {
  width: min(100%, 720px);
}

.desk-dialog--detail {
  width: min(100%, 1120px);
}

.desk-dialog__head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.desk-dialog__head h2,
.desk-dialog__head p {
  margin: 0;
}

.desk-dialog__head p {
  margin-top: 8px;
  color: var(--sl-ink-soft);
}

.desk-dialog__close {
  flex: 0 0 auto;
}

.desk-dialog__body {
  display: grid;
  gap: 18px;
}

.desk-dialog__body--split {
  grid-template-columns: minmax(0, 1.6fr) minmax(260px, 0.8fr);
  align-items: start;
}

.detail-card {
  display: grid;
  grid-template-columns: minmax(240px, 300px) minmax(0, 1fr);
  gap: 22px;
  align-items: start;
}

.detail-card__cover {
  overflow: hidden;
  aspect-ratio: 5 / 6;
  border-radius: 26px;
  background: linear-gradient(
    180deg,
    rgba(31, 95, 107, 0.18),
    rgba(201, 119, 46, 0.24)
  );
}

.detail-card__cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.detail-card__placeholder {
  display: grid;
  place-items: center;
  width: 100%;
  height: 100%;
  color: rgba(255, 255, 255, 0.9);
  font-size: 2rem;
}

.detail-card__copy {
  display: grid;
  gap: 18px;
}

.detail-card__badges {
  align-items: center;
}

.detail-card__meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.detail-card__meta article {
  padding: 14px;
  border-radius: 18px;
  border: 1px solid var(--sl-detail-meta-border);
  background: var(--sl-detail-meta-bg);
  box-shadow: var(--sl-detail-meta-shadow);
}

.detail-card__meta article.detail-card__meta-item--empty {
  opacity: 0.42;
  filter: saturate(0.65);
  box-shadow: none;
}

.detail-card__meta span {
  display: block;
  color: var(--sl-detail-meta-label);
  font-size: 0.82rem;
}

.detail-card__meta strong {
  display: block;
  margin-top: 6px;
  min-height: 1.35em;
  color: var(--sl-detail-meta-value);
  line-height: 1.35;
}

.detail-card__section {
  display: grid;
  gap: 8px;
  padding: 16px 18px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.48);
  border: 1px solid rgba(34, 48, 67, 0.08);
}

[data-theme='dark'] .detail-card__section {
  background: rgba(18, 28, 37, 0.76);
  border-color: rgba(159, 217, 228, 0.12);
}

.detail-card__section h3,
.detail-card__section p {
  margin: 0;
}

.detail-card__section p {
  color: var(--sl-ink-soft);
  line-height: 1.8;
}

.detail-card__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.desk-dialog__foot {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.desk-dialog__foot--align-end {
  justify-content: flex-end;
}

.desk-dialog__foot--danger-only {
  justify-content: flex-end;
}

.dialog-form,
.dialog-aside {
  display: grid;
  gap: 16px;
}

.dialog-aside {
  align-content: start;
}

.dialog-note {
  display: grid;
  gap: 8px;
  padding: 16px 18px;
  border-radius: 20px;
  border: 1px solid rgba(34, 48, 67, 0.08);
  background:
    linear-gradient(
      180deg,
      rgba(255, 255, 255, 0.64),
      rgba(255, 251, 244, 0.48)
    ),
    radial-gradient(
      circle at top right,
      rgba(201, 119, 46, 0.12),
      transparent 45%
    );
}

[data-theme='dark'] .dialog-note {
  border-color: rgba(159, 217, 228, 0.14);
  background:
    linear-gradient(180deg, rgba(25, 38, 49, 0.96), rgba(16, 25, 33, 0.92)),
    radial-gradient(
      circle at top right,
      rgba(108, 185, 199, 0.14),
      transparent 48%
    );
}

.dialog-note strong,
.dialog-note p {
  margin: 0;
}

.dialog-note p {
  color: var(--sl-ink-soft);
  line-height: 1.7;
}

.dialog-note__list {
  display: grid;
  gap: 10px;
}

.dialog-note__list li {
  display: grid;
  gap: 4px;
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(255, 253, 248, 0.82);
}

[data-theme='dark'] .dialog-note__list li {
  background: rgba(18, 28, 37, 0.8);
}

.dialog-note__list small {
  color: var(--sl-ink-soft);
}

.edit-cover-preview {
  overflow: hidden;
  aspect-ratio: 5 / 6;
  border-radius: 20px;
  background: linear-gradient(
    180deg,
    rgba(31, 95, 107, 0.18),
    rgba(201, 119, 46, 0.24)
  );
}

.edit-cover-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.edit-cover-preview__placeholder {
  display: grid;
  place-items: center;
  width: 100%;
  height: 100%;
  color: rgba(255, 255, 255, 0.92);
  font-size: 1rem;
}

.edit-cover-controls {
  display: grid;
  gap: 12px;
}

.edit-cover-field input {
  width: 100%;
}

.edit-cover-controls__upload {
  justify-content: center;
  width: 100%;
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.field-grid--single {
  grid-template-columns: 1fr;
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

.shelf-form-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
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

[data-theme='dark'] .shelf-list li {
  background: rgba(18, 28, 37, 0.72);
}

.shelf-list__actions {
  display: inline-flex;
  gap: 10px;
  flex-wrap: wrap;
}

.shelf-list__actions .button {
  min-width: 78px;
}

.shelf-list p {
  margin: 6px 0 0;
  color: var(--sl-ink-soft);
}

@media (max-width: 1100px) {
  .books-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .books-filter-toolbar {
    grid-template-columns: minmax(0, 1fr);
  }

  .desk-dialog__body--split {
    grid-template-columns: 1fr;
  }

  .detail-card {
    grid-template-columns: 1fr;
  }

  .detail-card__cover {
    max-width: 320px;
  }
}

@media (max-width: 760px) {
  .books-filter-panel {
    gap: 6px;
    padding: 10px 12px;
  }

  .books-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .detail-card__meta,
  .field-grid,
  .inline-composer {
    grid-template-columns: 1fr;
  }

  .books-filter-toolbar,
  .books-filter-toolbar__group {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
  }

  .books-filter-toolbar__search {
    grid-column: 1 / -1;
  }

  .books-filter-toolbar__group {
    grid-column: 1 / -1;
  }

  .books-filter-toolbar__actions {
    grid-column: 1 / -1;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
    align-items: stretch;
  }

  .books-filter-toolbar__actions {
    margin-top: 2px;
  }

  .books-filter-toolbar__button {
    min-width: 0;
  }

  .books-list-panel :deep(.section-panel__head) {
    flex-direction: row;
    align-items: center;
    gap: 10px;
  }

  .books-list-panel :deep(.section-panel__head h2) {
    font-size: 1.12rem;
  }

  .books-list-panel :deep(.section-panel__actions) {
    flex-wrap: nowrap;
    gap: 8px;
  }

  .books-list-panel :deep(.section-panel__actions .button) {
    min-width: 0;
    min-height: 34px;
    padding-inline: 10px;
    font-size: 0.84rem;
  }

  .books-grid :deep(.book-card__actions) {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .books-grid :deep(.book-card__actions .book-card-action) {
    min-height: 34px;
    padding: 0 6px;
    font-size: 0.82rem;
  }

  .dialog-scrim {
    padding: 14px;
  }

  .desk-dialog {
    padding: 18px;
    border-radius: 24px;
  }

  .desk-dialog__head,
  .desk-dialog__foot {
    flex-direction: column;
  }

  .desk-dialog__close,
  .desk-dialog__foot .button {
    width: 100%;
  }

  .shelf-list li {
    align-items: stretch;
    flex-direction: column;
  }
}

@media (max-width: 520px) {
  .books-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
  }

  .books-filter-toolbar,
  .books-filter-toolbar__group,
  .books-filter-toolbar__actions {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
