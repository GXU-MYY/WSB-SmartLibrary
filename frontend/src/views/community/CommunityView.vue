<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'

import {
  approveGroupBorrowRequest,
  createGroup,
  createGroupBorrowRequest,
  deleteGroup,
  exitGroup,
  getGroupBorrowRequests,
  getGroupPublicBooks,
  getGroupPublicShelves,
  getGroupUsers,
  getGroups,
  operateGroupUsers,
  removeGroupMember,
  rejectGroupBorrowRequest,
} from '@/api/community'
import { getUsers } from '@/api/user'
import EmptyState from '@/components/EmptyState.vue'
import LoadingState from '@/components/LoadingState.vue'
import PageIntro from '@/components/PageIntro.vue'
import SectionPanel from '@/components/SectionPanel.vue'
import UserAvatar from '@/components/UserAvatar.vue'
import { useRegisterPageRefresh } from '@/composables/usePageRefresh'
import { useUserStore } from '@/stores/user'
import type {
  Group,
  GroupBorrowRequest,
  GroupPublicBook,
  GroupPublicShelf,
  GroupUser,
  UserInfo,
} from '@/types/models'
import { formatDateTime, normalizePage } from '@/utils/format'
import { notifyError, notifySuccess } from '@/utils/notify'

const PENDING_STATUS = 0
const APPROVED_STATUS = 1
const REJECTED_STATUS = 2
const REQUESTS_PAGE_SIZE = 5

const userStore = useUserStore()

const loading = ref(false)
const savingGroup = ref(false)
const savingMembers = ref(false)
const searchingCreateMember = ref(false)
const searchingInviteMember = ref(false)
const requestingBookId = ref<number | null>(null)
const handlingRequestId = ref<number | null>(null)
const incomingRequestPage = ref(1)
const outgoingRequestPage = ref(1)

const groups = ref<Group[]>([])
const members = ref<GroupUser[]>([])
const publicShelves = ref<GroupPublicShelf[]>([])
const publicBooks = ref<GroupPublicBook[]>([])
const borrowRequests = ref<GroupBorrowRequest[]>([])

const selectedGroupId = ref(0)
const showCreateDialog = ref(false)
const showInviteDialog = ref(false)
const showBorrowRequestDialog = ref(false)
const memberDetailUserId = ref<number | null>(null)
const selectedShelfId = ref<number | null>(null)
const borrowRequestTarget = ref<GroupPublicBook | null>(null)

const createMemberPhone = ref('')
const inviteMemberPhone = ref('')
const selectedCreateMembers = ref<UserInfo[]>([])
const selectedInviteMembers = ref<UserInfo[]>([])

const groupForm = reactive({
  groupName: '',
  remark: '',
})

const borrowRequestForm = reactive({
  dueTime: '',
  requestRemark: '',
})

const toId = (value: number | string | null | undefined) => Number(value || 0)
const isSameId = (left: number | string | null | undefined, right: number | string | null | undefined) =>
  toId(left) === toId(right)
const formatDateInputValue = (value: Date) => {
  const year = value.getFullYear()
  const month = `${value.getMonth() + 1}`.padStart(2, '0')
  const day = `${value.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}
const getDefaultDueDate = () => {
  const nextMonth = new Date()
  nextMonth.setMonth(nextMonth.getMonth() + 1)
  return formatDateInputValue(nextMonth)
}
const paginateRequests = (records: GroupBorrowRequest[], page: number) => {
  const startIndex = (page - 1) * REQUESTS_PAGE_SIZE
  return records.slice(startIndex, startIndex + REQUESTS_PAGE_SIZE)
}

const currentUserId = computed(() => toId(userStore.userInfo?.id || userStore.loginId))
const todayDate = computed(() => {
  const now = new Date()
  const timezoneOffset = now.getTimezoneOffset() * 60_000
  return new Date(now.getTime() - timezoneOffset).toISOString().slice(0, 10)
})
const selectedGroup = computed(
  () => groups.value.find((item) => isSameId(item.id, selectedGroupId.value)) || null,
)
const isOwner = computed(() => isSameId(selectedGroup.value?.ownerId, currentUserId.value))
const selectedMember = computed(
  () => members.value.find((item) => isSameId(item.userId, memberDetailUserId.value)) || null,
)
const selectedMemberShelves = computed(() =>
  memberDetailUserId.value == null
    ? []
    : publicShelves.value.filter((item) => isSameId(item.ownerUserId, memberDetailUserId.value)),
)
const selectedShelf = computed(
  () => selectedMemberShelves.value.find((item) => isSameId(item.id, selectedShelfId.value)) || null,
)
const selectedShelfBooks = computed(() => {
  if (memberDetailUserId.value == null || selectedShelfId.value == null) {
    return []
  }

  return publicBooks.value.filter(
    (item) =>
      isSameId(item.ownerUserId, memberDetailUserId.value) &&
      isSameId(item.shelfId, selectedShelfId.value),
  )
})
const incomingRequests = computed(() =>
  borrowRequests.value.filter((item) => isSameId(item.ownerUserId, currentUserId.value)),
)
const outgoingRequests = computed(() =>
  borrowRequests.value.filter((item) => isSameId(item.borrowerUserId, currentUserId.value)),
)
const incomingRequestTotalPages = computed(() =>
  Math.max(1, Math.ceil(incomingRequests.value.length / REQUESTS_PAGE_SIZE)),
)
const outgoingRequestTotalPages = computed(() =>
  Math.max(1, Math.ceil(outgoingRequests.value.length / REQUESTS_PAGE_SIZE)),
)
const pagedIncomingRequests = computed(() =>
  paginateRequests(incomingRequests.value, incomingRequestPage.value),
)
const pagedOutgoingRequests = computed(() =>
  paginateRequests(outgoingRequests.value, outgoingRequestPage.value),
)
const incomingRequestPlaceholderRows = computed(() =>
  Math.max(0, REQUESTS_PAGE_SIZE - pagedIncomingRequests.value.length),
)
const outgoingRequestPlaceholderRows = computed(() =>
  Math.max(0, REQUESTS_PAGE_SIZE - pagedOutgoingRequests.value.length),
)

const loadWorkspace = async (groupId: number) => {
  const [membersResult, shelvesResult, booksResult, requestsResult] = await Promise.allSettled([
    getGroupUsers(groupId, 'in'),
    getGroupPublicShelves(groupId),
    getGroupPublicBooks(groupId),
    getGroupBorrowRequests(groupId),
  ])

  members.value = membersResult.status === 'fulfilled' ? membersResult.value : []
  publicShelves.value = shelvesResult.status === 'fulfilled' ? shelvesResult.value : []
  publicBooks.value = booksResult.status === 'fulfilled' ? booksResult.value : []
  borrowRequests.value = requestsResult.status === 'fulfilled' ? requestsResult.value : []
}

const loadGroups = async () => {
  const page = await getGroups({ page: 1, page_size: 50 })
  const normalized = normalizePage(page)
  groups.value = normalized.records

  if (!groups.value.length) {
    selectedGroupId.value = 0
    return
  }

  if (!selectedGroupId.value || !groups.value.some((item) => item.id === selectedGroupId.value)) {
    selectedGroupId.value = groups.value[0].id
    return
  }

  await loadWorkspace(selectedGroupId.value)
}

const loadPage = async () => {
  loading.value = true
  try {
    await loadGroups()
  } finally {
    loading.value = false
  }
}

const findUserByPhone = async (phone: string) => {
  const normalized = phone.trim()
  if (!normalized) {
    notifyError('请输入手机号')
    return null
  }

  const result = await getUsers({
    page: 1,
    page_size: 10,
    phone: normalized,
  })

  const records = normalizePage(result).records
  const exact = records.find((user) => user.phone === normalized) || records[0]
  if (!exact) {
    notifyError('没有找到该手机号对应的用户')
    return null
  }

  return exact
}

const resetCreateDialog = () => {
  groupForm.groupName = ''
  groupForm.remark = ''
  createMemberPhone.value = ''
  selectedCreateMembers.value = []
}

const resetInviteDialog = () => {
  inviteMemberPhone.value = ''
  selectedInviteMembers.value = []
}

const openCreateDialog = () => {
  resetCreateDialog()
  showCreateDialog.value = true
}

const closeCreateDialog = () => {
  showCreateDialog.value = false
  resetCreateDialog()
}

const openInviteDialog = () => {
  if (!selectedGroup.value) {
    notifyError('请先选择群组')
    return
  }
  if (!isOwner.value) {
    notifyError('只有群主可以邀请成员')
    return
  }

  resetInviteDialog()
  showInviteDialog.value = true
}

const closeInviteDialog = () => {
  showInviteDialog.value = false
  resetInviteDialog()
}

const openMemberDetail = (member: GroupUser) => {
  memberDetailUserId.value = member.userId
  selectedShelfId.value = null
}

const closeMemberDetail = () => {
  memberDetailUserId.value = null
  selectedShelfId.value = null
}

const resetBorrowRequestDialog = () => {
  borrowRequestTarget.value = null
  borrowRequestForm.dueTime = getDefaultDueDate()
  borrowRequestForm.requestRemark = ''
}

const openBorrowRequestDialog = (book: GroupPublicBook) => {
  if (!selectedGroup.value || !canRequestBorrow(book)) {
    return
  }

  borrowRequestTarget.value = book
  borrowRequestForm.dueTime = getDefaultDueDate()
  borrowRequestForm.requestRemark = ''
  showBorrowRequestDialog.value = true
}

const closeBorrowRequestDialog = () => {
  showBorrowRequestDialog.value = false
  resetBorrowRequestDialog()
}

const selectShelf = (shelfId: number) => {
  selectedShelfId.value = shelfId
}

const isCurrentMember = (member: GroupUser) => isSameId(member.userId, currentUserId.value)

const getMemberRoleText = (member: GroupUser) =>
  isSameId(selectedGroup.value?.ownerId, member.userId) ? '群主' : '成员'

const getMemberPublicShelfCount = (userId: number) =>
  publicShelves.value.filter((item) => isSameId(item.ownerUserId, userId)).length

const getMemberBorrowableBookCount = (userId: number) =>
  publicBooks.value.filter((item) => isSameId(item.ownerUserId, userId) && item.borrowable).length

const handleAddCreateMember = async () => {
  searchingCreateMember.value = true
  try {
    const user = await findUserByPhone(createMemberPhone.value)
    if (!user) {
      return
    }
    if (isSameId(user.id, currentUserId.value)) {
      notifyError('只需要添加其他初始成员')
      return
    }
    if (selectedCreateMembers.value.some((item) => isSameId(item.id, user.id))) {
      notifyError('该用户已经在初始成员列表中')
      return
    }

    selectedCreateMembers.value = [...selectedCreateMembers.value, user]
    createMemberPhone.value = ''
    notifySuccess(`已添加 ${user.nickName || user.userName}`)
  } finally {
    searchingCreateMember.value = false
  }
}

const handleAddInviteMember = async () => {
  if (!selectedGroup.value) {
    notifyError('请先选择群组')
    return
  }

  searchingInviteMember.value = true
  try {
    const user = await findUserByPhone(inviteMemberPhone.value)
    if (!user) {
      return
    }
    if (isSameId(user.id, currentUserId.value)) {
      notifyError('你已经在这个群组里了')
      return
    }
    if (members.value.some((member) => isSameId(member.userId, user.id))) {
      notifyError('该用户已经是群成员')
      return
    }
    if (selectedInviteMembers.value.some((item) => isSameId(item.id, user.id))) {
      notifyError('该用户已经在待邀请列表中')
      return
    }

    selectedInviteMembers.value = [...selectedInviteMembers.value, user]
    inviteMemberPhone.value = ''
    notifySuccess(`已加入待邀请列表：${user.nickName || user.userName}`)
  } finally {
    searchingInviteMember.value = false
  }
}

const removeSelectedCreateMember = (userId: number) => {
  selectedCreateMembers.value = selectedCreateMembers.value.filter((user) => user.id !== userId)
}

const removeSelectedInviteMember = (userId: number) => {
  selectedInviteMembers.value = selectedInviteMembers.value.filter((user) => user.id !== userId)
}

const handleCreateGroup = async () => {
  const groupName = groupForm.groupName.trim()
  if (!groupName) {
    notifyError('请先填写群组名称')
    return
  }

  savingGroup.value = true
  try {
    const result = await createGroup({
      groupName,
      remark: groupForm.remark.trim() || undefined,
      userIds: selectedCreateMembers.value.map((user) => user.id),
    })
    notifySuccess(`群组 ${result.groupName} 已创建`)
    closeCreateDialog()
    await loadGroups()
    selectedGroupId.value = result.id
  } finally {
    savingGroup.value = false
  }
}

const handleInviteMembers = async () => {
  if (!selectedGroup.value || !selectedInviteMembers.value.length) {
    notifyError('请先添加待邀请成员')
    return
  }

  savingMembers.value = true
  try {
    await operateGroupUsers({
      groupId: selectedGroup.value.id,
      userIds: selectedInviteMembers.value.map((user) => user.id),
      type: 'add',
    })
    notifySuccess('成员已加入群组')
    closeInviteDialog()
    await loadWorkspace(selectedGroup.value.id)
  } finally {
    savingMembers.value = false
  }
}

const handleRemoveMember = async (member: GroupUser) => {
  if (!selectedGroup.value || !isOwner.value || isCurrentMember(member)) {
    return
  }

  const ok = window.confirm(`确认将 ${member.nickname} 移出群组吗？`)
  if (!ok) {
    return
  }

  await removeGroupMember(selectedGroup.value.id, member.userId)
  notifySuccess('成员已被移出群组')

  if (isSameId(member.userId, memberDetailUserId.value)) {
    closeMemberDetail()
  }
  await loadWorkspace(selectedGroup.value.id)
}

const handleExitGroup = async () => {
  if (!selectedGroup.value || isOwner.value) {
    return
  }

  const groupName = selectedGroup.value.groupName
  const ok = window.confirm(`确认退出群组“${groupName}”吗？`)
  if (!ok) {
    return
  }

  await exitGroup(selectedGroup.value.id)
  notifySuccess(`你已退出群组“${groupName}”`)
  closeMemberDetail()
  await loadGroups()
}

const handleDeleteGroup = async () => {
  if (!selectedGroup.value || !isOwner.value) {
    return
  }

  const groupName = selectedGroup.value.groupName
  const ok = window.confirm(`确认解散群组“${groupName}”吗？此操作不可撤销。`)
  if (!ok) {
    return
  }

  await deleteGroup(selectedGroup.value.id)
  notifySuccess(`群组 ${groupName} 已解散`)
  closeInviteDialog()
  closeMemberDetail()
  selectedGroupId.value = 0
  await loadGroups()
}

const hasPendingRequest = (bookId: number) =>
  borrowRequests.value.some(
    (item) =>
      isSameId(item.bookId, bookId) &&
      isSameId(item.borrowerUserId, currentUserId.value) &&
      item.status === PENDING_STATUS,
  )

const canRequestBorrow = (book: GroupPublicBook) =>
  Boolean(book.borrowable) &&
  !isSameId(book.ownerUserId, currentUserId.value) &&
  !hasPendingRequest(book.bookId)

const getBorrowButtonLabel = (book: GroupPublicBook) => {
  if (isSameId(book.ownerUserId, currentUserId.value)) {
    return '我的图书'
  }
  if (hasPendingRequest(book.bookId)) {
    return '已申请'
  }
  if (!book.borrowable) {
    return '借出中'
  }
  return '请求借入'
}

const handleBorrowRequest = (book: GroupPublicBook) => {
  openBorrowRequestDialog(book)
}

const handleSubmitBorrowRequest = async () => {
  if (!selectedGroup.value || !borrowRequestTarget.value || !canRequestBorrow(borrowRequestTarget.value)) {
    return
  }

  const book = borrowRequestTarget.value
  requestingBookId.value = book.bookId
  try {
    await createGroupBorrowRequest({
      groupId: selectedGroup.value.id,
      bookId: book.bookId,
      dueTime: borrowRequestForm.dueTime || undefined,
      requestRemark: borrowRequestForm.requestRemark.trim() || undefined,
    })
    notifySuccess(`已向 ${book.ownerNickname || '书主'} 发起借阅请求`)
    closeBorrowRequestDialog()
    await loadWorkspace(selectedGroup.value.id)
  } finally {
    requestingBookId.value = null
  }
}

const handleApproveRequest = async (request: GroupBorrowRequest) => {
  if (!selectedGroup.value) {
    return
  }

  handlingRequestId.value = request.id
  try {
    await approveGroupBorrowRequest(request.id)
    notifySuccess('借阅请求已同意')
    await loadWorkspace(selectedGroup.value.id)
  } finally {
    handlingRequestId.value = null
  }
}

const handleRejectRequest = async (request: GroupBorrowRequest) => {
  if (!selectedGroup.value) {
    return
  }

  handlingRequestId.value = request.id
  try {
    await rejectGroupBorrowRequest(request.id)
    notifySuccess('借阅请求已拒绝')
    await loadWorkspace(selectedGroup.value.id)
  } finally {
    handlingRequestId.value = null
  }
}

const requestStatusText = (status: number) => {
  if (status === APPROVED_STATUS) {
    return '已同意'
  }
  if (status === REJECTED_STATUS) {
    return '已拒绝'
  }
  return '待处理'
}

const requestStatusClass = (status: number) => ({
  'status-badge': true,
  'is-pending': status === PENDING_STATUS,
  'is-approved': status === APPROVED_STATUS,
  'is-rejected': status === REJECTED_STATUS,
})
const changeIncomingRequestPage = (nextPage: number) => {
  incomingRequestPage.value = Math.min(Math.max(1, nextPage), incomingRequestTotalPages.value)
}
const changeOutgoingRequestPage = (nextPage: number) => {
  outgoingRequestPage.value = Math.min(Math.max(1, nextPage), outgoingRequestTotalPages.value)
}

watch(selectedGroupId, async (groupId) => {
  closeInviteDialog()
  closeBorrowRequestDialog()
  closeMemberDetail()
  incomingRequestPage.value = 1
  outgoingRequestPage.value = 1

  if (groupId) {
    await loadWorkspace(groupId)
  } else {
    members.value = []
    publicShelves.value = []
    publicBooks.value = []
    borrowRequests.value = []
  }
})

watch(memberDetailUserId, () => {
  selectedShelfId.value = null
})

watch(incomingRequests, () => {
  if (incomingRequestPage.value > incomingRequestTotalPages.value) {
    incomingRequestPage.value = incomingRequestTotalPages.value
  }
})

watch(outgoingRequests, () => {
  if (outgoingRequestPage.value > outgoingRequestTotalPages.value) {
    outgoingRequestPage.value = outgoingRequestTotalPages.value
  }
})

useRegisterPageRefresh(loadPage)
onMounted(loadPage)
</script>

<template>
  <div class="page-shell page-stack">
    <PageIntro
      eyebrow="Community Desk"
      title="把群组协作收进同一个阅读工作区"
      description="在这里选择群组、邀请成员、处理借阅申请，并通过成员卡片查看对方公开出来的书架与可借图书。"
    />

    <section class="community-layout">
      <div class="community-sidebar">
        <SectionPanel title="选择群组" hint="切换群组后，中间工作区会同步刷新。">
          <template #actions>
            <button class="button button--primary" type="button" @click="openCreateDialog">
              创建群组
            </button>
          </template>

          <div class="group-switcher">
            <button
              v-for="group in groups"
              :key="group.id"
              class="group-switcher__item focus-ring"
              :class="{ 'is-active': isSameId(selectedGroupId, group.id) }"
              type="button"
              @click="selectedGroupId = group.id"
            >
              <div class="group-switcher__head">
                <strong>{{ group.groupName }}</strong>
                <span class="group-switcher__badge">
                  {{ isSameId(group.ownerId, currentUserId) ? '我是群主' : '群成员' }}
                </span>
              </div>
              <p>{{ group.remark || '这个群组还没有补充说明。' }}</p>
            </button>

            <p v-if="!groups.length" class="muted">还没有可用群组，先创建一个吧。</p>
          </div>
        </SectionPanel>
      </div>

      <SectionPanel
        class="community-workspace"
        title="群组工作区"
        hint="成员、书架和借阅申请都集中在这里处理。"
      >
        <template v-if="selectedGroup" #actions>
          <button
            v-if="isOwner"
            class="button button--secondary workspace-head__action"
            type="button"
            @click="openInviteDialog"
          >
            邀请成员
          </button>
          <button
            v-if="isOwner"
            class="button button--danger workspace-head__action"
            type="button"
            @click="handleDeleteGroup"
          >
            解散群组
          </button>
          <button
            v-else
            class="button button--ghost workspace-head__action"
            type="button"
            @click="handleExitGroup"
          >
            退出群组
          </button>
        </template>

        <LoadingState v-if="loading && !selectedGroup" />

        <template v-else-if="selectedGroup">
          <div class="section-stack">
            <article class="panel-card workspace-head">
              <div>
                <span class="eyebrow">Current Group</span>
                <h3>{{ selectedGroup.groupName }}</h3>
                <p>{{ selectedGroup.remark || '这个群组还没有补充说明。' }}</p>
              </div>

              <div class="workspace-head__actions">
                <button
                  v-if="isOwner"
                  class="button button--secondary workspace-head__action"
                  type="button"
                  @click="openInviteDialog"
                >
                  邀请成员
                </button>
                <button
                  v-if="isOwner"
                  class="button button--danger workspace-head__action"
                  type="button"
                  @click="handleDeleteGroup"
                >
                  解散群组
                </button>
                <button
                  v-else
                  class="button button--ghost workspace-head__action"
                  type="button"
                  @click="handleExitGroup"
                >
                  退出群组
                </button>
              </div>
            </article>

            <div class="workspace-overview">
              <article class="panel-card overview-card">
                <span>群成员</span>
                <strong>{{ members.length }}</strong>
              </article>
              <article class="panel-card overview-card">
                <span>公开书架</span>
                <strong>{{ publicShelves.length }}</strong>
              </article>
              <article class="panel-card overview-card">
                <span>可借图书</span>
                <strong>{{ publicBooks.filter((item) => item.borrowable).length }}</strong>
              </article>
              <article class="panel-card overview-card">
                <span>待处理申请</span>
                <strong>{{ incomingRequests.filter((item) => item.status === PENDING_STATUS).length }}</strong>
              </article>
            </div>

            <section class="section-block">
              <div class="section-title">
                <h4>群成员</h4>
                <p>点击成员卡片先查看对方公开书架，再进入书架详情浏览图书并发起借阅。</p>
              </div>

              <div v-if="members.length" class="member-grid">
                <article
                  v-for="member in members"
                  :key="member.userId"
                  class="panel-card member-profile-card focus-ring"
                  :class="{ 'is-self': isCurrentMember(member) }"
                  @click="openMemberDetail(member)"
                >
                  <UserAvatar :src="member.avatar" :name="member.nickname" :size="72" />

                  <div class="member-profile-card__tags">
                    <span class="member-profile-card__role">{{ getMemberRoleText(member) }}</span>
                    <span v-if="isCurrentMember(member)" class="member-profile-card__self-badge">本人</span>
                  </div>

                  <strong>{{ member.nickname }}</strong>
                  <p class="member-profile-card__meta">
                    {{ getMemberPublicShelfCount(member.userId) }} 个公开书架
                  </p>
                  <p class="member-profile-card__meta">
                    {{ getMemberBorrowableBookCount(member.userId) }} 本可借图书
                  </p>

                </article>
              </div>

              <EmptyState v-else title="当前群组还没有成员" />
            </section>

            <section class="section-block">
              <div class="section-title">
                <h4>借阅申请</h4>
              </div>

              <div class="request-stack">
                <div class="request-column">
                  <div class="request-column__head">
                    <h5>收到的申请</h5>
                    <span>{{ incomingRequests.length }}</span>
                  </div>

                  <div v-if="incomingRequests.length" class="request-list">
                    <div class="request-table-wrapper">
                      <table class="request-table">
                        <thead>
                          <tr>
                            <th>图书</th>
                            <th>申请人</th>
                            <th>来源书架</th>
                            <th>申请时间</th>
                            <th>预计归还</th>
                            <th>状态</th>
                            <th>备注</th>
                            <th>操作</th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr v-for="request in pagedIncomingRequests" :key="request.id">
                            <td>{{ request.bookName }}</td>
                            <td>{{ request.borrowerNickname || `用户 ${request.borrowerUserId}` }}</td>
                            <td>{{ request.shelfName || '公开书架' }}</td>
                            <td>{{ request.createTime ? formatDateTime(request.createTime) : '未知' }}</td>
                            <td>{{ request.dueTime || '未填写' }}</td>
                            <td>
                              <span :class="requestStatusClass(request.status)">
                                {{ requestStatusText(request.status) }}
                              </span>
                            </td>
                            <td>{{ request.requestRemark || '无' }}</td>
                            <td>
                              <div v-if="request.status === PENDING_STATUS" class="request-table__actions">
                                <button
                                  class="button button--primary request-table__action-btn"
                                  type="button"
                                  :disabled="handlingRequestId === request.id"
                                  @click="handleApproveRequest(request)"
                                >
                                  {{ handlingRequestId === request.id ? '处理中...' : '同意' }}
                                </button>
                                <button
                                  class="button button--ghost request-table__action-btn"
                                  type="button"
                                  :disabled="handlingRequestId === request.id"
                                  @click="handleRejectRequest(request)"
                                >
                                  拒绝
                                </button>
                              </div>
                              <span v-else class="muted">-</span>
                            </td>
                          </tr>
                          <tr
                            v-for="placeholderIndex in incomingRequestPlaceholderRows"
                            :key="`incoming-placeholder-${placeholderIndex}`"
                            class="request-table__placeholder"
                          >
                            <td colspan="8">&nbsp;</td>
                          </tr>
                        </tbody>
                      </table>
                    </div>

                    <div class="request-pagination">
                      <button
                        class="button button--ghost"
                        type="button"
                        :disabled="incomingRequestPage === 1"
                        @click="changeIncomingRequestPage(incomingRequestPage - 1)"
                      >
                        上一页
                      </button>
                      <span>第 {{ incomingRequestPage }} / {{ incomingRequestTotalPages }} 页</span>
                      <button
                        class="button button--ghost"
                        type="button"
                        :disabled="incomingRequestPage === incomingRequestTotalPages"
                        @click="changeIncomingRequestPage(incomingRequestPage + 1)"
                      >
                        下一页
                      </button>
                    </div>
                  </div>

                  <EmptyState v-else title="还没有收到借阅申请" />
                </div>

                <div class="request-column">
                  <div class="request-column__head">
                    <h5>发出的申请</h5>
                    <span>{{ outgoingRequests.length }}</span>
                  </div>

                  <div v-if="outgoingRequests.length" class="request-list">
                    <div class="request-table-wrapper">
                      <table class="request-table">
                        <thead>
                          <tr>
                            <th>图书</th>
                            <th>出借人</th>
                            <th>来源书架</th>
                            <th>申请时间</th>
                            <th>预计归还</th>
                            <th>状态</th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr v-for="request in pagedOutgoingRequests" :key="request.id">
                            <td>{{ request.bookName }}</td>
                            <td>{{ request.ownerNickname || `用户 ${request.ownerUserId}` }}</td>
                            <td>{{ request.shelfName || '公开书架' }}</td>
                            <td>{{ request.createTime ? formatDateTime(request.createTime) : '未知' }}</td>
                            <td>{{ request.dueTime || '未填写' }}</td>
                            <td>
                              <span :class="requestStatusClass(request.status)">
                                {{ requestStatusText(request.status) }}
                              </span>
                            </td>
                          </tr>
                          <tr
                            v-for="placeholderIndex in outgoingRequestPlaceholderRows"
                            :key="`outgoing-placeholder-${placeholderIndex}`"
                            class="request-table__placeholder"
                          >
                            <td colspan="6">&nbsp;</td>
                          </tr>
                        </tbody>
                      </table>
                    </div>

                    <div class="request-pagination">
                      <button
                        class="button button--ghost"
                        type="button"
                        :disabled="outgoingRequestPage === 1"
                        @click="changeOutgoingRequestPage(outgoingRequestPage - 1)"
                      >
                        上一页
                      </button>
                      <span>第 {{ outgoingRequestPage }} / {{ outgoingRequestTotalPages }} 页</span>
                      <button
                        class="button button--ghost"
                        type="button"
                        :disabled="outgoingRequestPage === outgoingRequestTotalPages"
                        @click="changeOutgoingRequestPage(outgoingRequestPage + 1)"
                      >
                        下一页
                      </button>
                    </div>
                  </div>

                  <EmptyState v-else title="你还没有发起借阅申请" />
                </div>
              </div>
            </section>
          </div>
        </template>

        <EmptyState v-else title="先在左侧选择一个群组" />
      </SectionPanel>
    </section>

    <Teleport to="body">
      <div v-if="showCreateDialog" class="dialog-scrim" @click.self="closeCreateDialog">
        <section class="surface-card desk-dialog desk-dialog--wide">
          <header class="desk-dialog__head">
            <div>
              <span class="eyebrow">Create Group</span>
              <h2>创建群组</h2>
              <p>先完成群名称和说明，再按手机号补充初始成员。</p>
            </div>
            <button class="button button--ghost desk-dialog__close" type="button" @click="closeCreateDialog">
              关闭
            </button>
          </header>

          <div class="desk-dialog__body">
            <div class="section-stack">
              <div class="field">
                <label>群组名称</label>
                <input v-model="groupForm.groupName" type="text" placeholder="例如：设计阅读共学组" />
              </div>

              <div class="field">
                <label>群组说明</label>
                <textarea
                  v-model="groupForm.remark"
                  placeholder="描述这个群组的阅读主题、协作方式或适合加入的人。"
                />
              </div>

              <div class="field">
                <label>添加初始成员</label>
                <div class="field-inline">
                  <input
                    v-model="createMemberPhone"
                    type="text"
                    placeholder="输入手机号后添加"
                    @keyup.enter="handleAddCreateMember"
                  />
                  <button
                    class="button button--secondary"
                    type="button"
                    :disabled="searchingCreateMember"
                    @click="handleAddCreateMember"
                  >
                    {{ searchingCreateMember ? '查找中...' : '添加' }}
                  </button>
                </div>
              </div>

              <div class="selected-user-list">
                <article
                  v-for="user in selectedCreateMembers"
                  :key="user.id"
                  class="selected-user-card"
                >
                  <div class="selected-user-card__info">
                    <UserAvatar :src="user.avatar" :name="user.nickName || user.userName" :size="44" />
                    <div>
                      <strong>{{ user.nickName || user.userName }}</strong>
                      <p>{{ user.phone || '未绑定手机号' }}</p>
                    </div>
                  </div>
                  <button class="button button--ghost" type="button" @click="removeSelectedCreateMember(user.id)">
                    移除
                  </button>
                </article>
                <p v-if="!selectedCreateMembers.length" class="muted">当前还没有添加初始成员。</p>
              </div>
            </div>
          </div>

          <footer class="desk-dialog__foot desk-dialog__foot--align-end">
            <button class="button button--ghost" type="button" @click="closeCreateDialog">取消</button>
            <button class="button button--primary" type="button" :disabled="savingGroup" @click="handleCreateGroup">
              {{ savingGroup ? '创建中...' : '创建群组' }}
            </button>
          </footer>
        </section>
      </div>
    </Teleport>

    <Teleport to="body">
      <div v-if="showInviteDialog" class="dialog-scrim" @click.self="closeInviteDialog">
        <section class="surface-card desk-dialog desk-dialog--wide invite-dialog">
          <header class="desk-dialog__head">
            <div>
              <span class="eyebrow">Invite Members</span>
              <h2>邀请成员</h2>
              <p>通过手机号查找用户，加入待邀请列表后统一邀请入群。</p>
            </div>
            <button class="button button--ghost desk-dialog__close" type="button" @click="closeInviteDialog">
              关闭
            </button>
          </header>

          <div class="desk-dialog__body">
            <div class="section-stack">
              <div class="field">
                <label>通过手机号邀请成员</label>
                <div class="field-inline">
                  <input
                    v-model="inviteMemberPhone"
                    type="text"
                    placeholder="输入手机号后加入待邀请列表"
                    @keyup.enter="handleAddInviteMember"
                  />
                  <button
                    class="button button--secondary"
                    type="button"
                    :disabled="searchingInviteMember"
                    @click="handleAddInviteMember"
                  >
                    {{ searchingInviteMember ? '查找中...' : '加入待邀请' }}
                  </button>
                </div>
              </div>

              <div class="selected-user-list">
                <article
                  v-for="user in selectedInviteMembers"
                  :key="user.id"
                  class="selected-user-card"
                >
                  <div class="selected-user-card__info">
                    <UserAvatar :src="user.avatar" :name="user.nickName || user.userName" :size="44" />
                    <div>
                      <strong>{{ user.nickName || user.userName }}</strong>
                      <p>{{ user.phone || '未绑定手机号' }}</p>
                    </div>
                  </div>
                  <button class="button button--ghost" type="button" @click="removeSelectedInviteMember(user.id)">
                    移除
                  </button>
                </article>
                <p v-if="!selectedInviteMembers.length" class="muted">还没有待邀请成员。</p>
              </div>
            </div>
          </div>

          <footer class="desk-dialog__foot desk-dialog__foot--align-end">
            <button class="button button--ghost" type="button" @click="closeInviteDialog">取消</button>
            <button
              class="button button--primary"
              type="button"
              :disabled="savingMembers || !selectedInviteMembers.length"
              @click="handleInviteMembers"
            >
              {{ savingMembers ? '邀请中...' : '确认邀请' }}
            </button>
          </footer>
        </section>
      </div>
    </Teleport>

    <Teleport to="body">
      <div
        v-if="showBorrowRequestDialog && borrowRequestTarget"
        class="dialog-scrim dialog-scrim--front"
        @click.self="closeBorrowRequestDialog"
      >
        <section class="surface-card desk-dialog desk-dialog--compact">
          <header class="desk-dialog__head">
            <div>
              <span class="eyebrow">Borrow Request</span>
              <h2>申请借入图书</h2>
              <p>向 {{ borrowRequestTarget.ownerNickname || '书主' }} 发起借阅申请，并可选填写预计归还时间。</p>
            </div>
            <button class="button button--ghost desk-dialog__close" type="button" @click="closeBorrowRequestDialog">
              关闭
            </button>
          </header>

          <div class="desk-dialog__body">
            <div class="section-stack">
              <article class="panel-card borrow-request-preview">
                <div class="borrow-request-preview__copy">
                  <strong>{{ borrowRequestTarget.title }}</strong>
                  <p>{{ borrowRequestTarget.author || '作者未填写' }}</p>
                  <span>{{ borrowRequestTarget.shelfName || '公开书架' }}</span>
                </div>
              </article>

              <div class="field">
                <label>预计归还时间</label>
                <input
                  v-model="borrowRequestForm.dueTime"
                  :min="todayDate"
                  type="date"
                />
              </div>

              <div class="field">
                <label>申请备注</label>
                <textarea
                  v-model="borrowRequestForm.requestRemark"
                  placeholder="例如：我计划阅读两周左右，读完会及时归还。"
                />
              </div>
            </div>
          </div>

          <footer class="desk-dialog__foot desk-dialog__foot--align-end">
            <button class="button button--ghost" type="button" @click="closeBorrowRequestDialog">取消</button>
            <button
              class="button button--primary"
              type="button"
              :disabled="requestingBookId === borrowRequestTarget.bookId"
              @click="handleSubmitBorrowRequest"
            >
              {{ requestingBookId === borrowRequestTarget.bookId ? '申请中...' : '提交申请' }}
            </button>
          </footer>
        </section>
      </div>
    </Teleport>

    <Teleport to="body">
      <div v-if="selectedMember" class="dialog-scrim" @click.self="closeMemberDetail">
        <section class="surface-card desk-dialog desk-dialog--wide member-detail-dialog">
          <header class="desk-dialog__head">
            <div>
              <span class="eyebrow">Member Shelves</span>
              <h2>{{ selectedMember.nickname }}</h2>
              <p>先查看对方公开书架，再进入书架详情浏览图书并发起借阅。</p>
            </div>

            <div class="desk-dialog__head-actions">
              <button
                v-if="isOwner && !isCurrentMember(selectedMember)"
                class="button button--ghost"
                type="button"
                @click="handleRemoveMember(selectedMember)"
              >
                踢出群组
              </button>
              <button class="button button--ghost desk-dialog__close" type="button" @click="closeMemberDetail">
                关闭
              </button>
            </div>
          </header>

          <div class="desk-dialog__body">
            <div class="member-detail-head panel-card">
              <UserAvatar :src="selectedMember.avatar" :name="selectedMember.nickname" :size="76" />
              <div>
                <strong>{{ selectedMember.nickname }}</strong>
                <div class="member-detail-head__tags">
                  <span class="member-profile-card__role">{{ getMemberRoleText(selectedMember) }}</span>
                  <span v-if="isCurrentMember(selectedMember)" class="member-profile-card__self-badge">本人</span>
                </div>
                <p>
                  {{ getMemberPublicShelfCount(selectedMember.userId) }} 个公开书架 ·
                  {{ getMemberBorrowableBookCount(selectedMember.userId) }} 本可借图书
                </p>
              </div>
            </div>

            <section class="section-block">
              <div class="section-title">
                <h4>公开书架</h4>
                <p>点击“查看书架详情”后，再显示这个书架下可供借阅的图书。</p>
              </div>

              <div v-if="selectedMemberShelves.length" class="public-shelf-grid">
                <article
                  v-for="shelf in selectedMemberShelves"
                  :key="shelf.id"
                  class="panel-card public-shelf-card"
                  :class="{ 'is-active': selectedShelfId === shelf.id }"
                >
                  <div class="public-shelf-card__content">
                    <strong>{{ shelf.shelfName }}</strong>
                    <p>{{ shelf.remark || '这个书架还没有补充说明。' }}</p>
                  </div>
                  <button
                    class="button"
                    :class="selectedShelfId === shelf.id ? 'button--primary' : 'button--ghost'"
                    type="button"
                    @click="selectShelf(shelf.id)"
                  >
                    {{ selectedShelfId === shelf.id ? '正在查看' : '查看书架详情' }}
                  </button>
                </article>
              </div>
              <EmptyState v-else title="这个成员还没有公开书架" />
            </section>

            <section v-if="selectedShelf" class="section-block">
              <div class="section-title">
                <h4>书架详情</h4>
                <p>当前书架：{{ selectedShelf.shelfName }}</p>
              </div>

              <div v-if="selectedShelfBooks.length" class="public-book-list">
                <article
                  v-for="book in selectedShelfBooks"
                  :key="`${book.shelfId}-${book.bookId}`"
                  class="panel-card public-book-card"
                >
                  <img
                    v-if="book.coverUrl"
                    :src="book.coverUrl"
                    :alt="book.title"
                    class="public-book-card__cover"
                  />
                  <div v-else class="public-book-card__cover is-placeholder">书</div>

                  <div class="public-book-card__body">
                    <strong>{{ book.title }}</strong>
                    <p>{{ book.author || '作者未填写' }}</p>
                    <span>{{ selectedShelf.shelfName }}</span>
                  </div>

                  <button
                    class="button public-book-card__action"
                    :class="canRequestBorrow(book) ? 'button--primary' : 'button--ghost'"
                    type="button"
                    :disabled="!canRequestBorrow(book) || requestingBookId === book.bookId"
                    @click="handleBorrowRequest(book)"
                  >
                    {{ requestingBookId === book.bookId ? '申请中...' : getBorrowButtonLabel(book) }}
                  </button>
                </article>
              </div>
              <EmptyState v-else title="这个书架下还没有公开图书" />
            </section>
          </div>
        </section>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.community-layout {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 20px;
  align-items: start;
}

.community-sidebar {
  display: grid;
  gap: 18px;
  align-self: start;
}

.community-workspace {
  min-height: 100%;
}

.section-stack,
.request-list,
.group-switcher {
  display: grid;
  gap: 14px;
}

.request-list {
  align-content: start;
}

.request-table-wrapper {
  overflow-x: auto;
  border: 1px solid var(--sl-border-color);
  border-radius: 18px;
  background: var(--sl-soft-panel-bg);
}

.panel-card,
.group-switcher__item,
.selected-user-card,
.member-detail-head {
  border: 1px solid var(--sl-border-color);
  background: var(--sl-soft-panel-bg);
  border-radius: 20px;
}

.group-switcher__item {
  display: grid;
  gap: 10px;
  padding: 16px;
  text-align: left;
  transition:
    transform 0.18s ease,
    border-color 0.18s ease,
    background 0.18s ease;
}

.group-switcher__head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: flex-start;
}

.group-switcher__item strong,
.group-switcher__item p {
  margin: 0;
}

.group-switcher__item p {
  color: var(--sl-ink-soft);
  line-height: 1.6;
}

.group-switcher__badge {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(31, 95, 107, 0.12);
  color: var(--sl-brand-strong);
  font-size: 0.82rem;
  font-weight: 600;
  white-space: nowrap;
}

.group-switcher__item.is-active {
  border-color: rgba(31, 95, 107, 0.42);
  background:
    linear-gradient(180deg, rgba(31, 95, 107, 0.12), rgba(31, 95, 107, 0.04)),
    var(--sl-soft-panel-bg);
}

.workspace-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 20px;
  display: none;
}

.workspace-head h3,
.workspace-head p {
  margin: 0;
}

.workspace-head p {
  margin-top: 8px;
  color: var(--sl-ink-soft);
  line-height: 1.7;
}

.workspace-head__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
}

.workspace-head__action {
  min-height: 36px;
  padding-inline: 12px;
  font-size: 0.88rem;
  line-height: 1.2;
  white-space: nowrap;
}

.workspace-overview {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.overview-card {
  display: grid;
  gap: 6px;
  padding: 16px 18px;
}

.overview-card span {
  color: var(--sl-ink-soft);
}

.overview-card strong {
  font-size: 1.8rem;
}

.section-block {
  display: grid;
  gap: 14px;
}

.section-title {
  display: grid;
  gap: 6px;
}

.section-title h4,
.section-title h5,
.section-title p {
  margin: 0;
}

.section-title p {
  color: var(--sl-ink-soft);
  line-height: 1.6;
}

.field {
  display: grid;
  gap: 8px;
}

.field label {
  font-weight: 600;
}

.field input,
.field textarea,
.field select {
  width: 100%;
  border: 1px solid var(--sl-line);
  border-radius: var(--sl-radius-md);
  padding: 12px 14px;
  background: var(--sl-input-bg);
  color: var(--sl-ink);
  transition: border-color 180ms ease, box-shadow 180ms ease;
}

.field textarea {
  min-height: 108px;
  resize: vertical;
}

.field input:focus,
.field textarea:focus,
.field select:focus {
  border-color: rgba(31, 95, 107, 0.44);
  box-shadow: 0 0 0 4px rgba(31, 95, 107, 0.08);
  outline: none;
}

.field-inline {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
}

.selected-user-list {
  display: grid;
  gap: 10px;
}

.selected-user-card {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  padding: 14px 16px;
}

.selected-user-card__info {
  display: flex;
  gap: 12px;
  align-items: center;
  min-width: 0;
}

.selected-user-card strong,
.selected-user-card p {
  margin: 0;
}

.selected-user-card p {
  margin-top: 4px;
  color: var(--sl-ink-soft);
}

.member-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 14px;
}

.member-profile-card {
  position: relative;
  display: grid;
  justify-items: center;
  gap: 10px;
  padding: 18px 14px 16px;
  text-align: center;
  cursor: pointer;
}

.member-profile-card.is-self {
  border-color: rgba(31, 95, 107, 0.42);
  box-shadow: 0 12px 28px rgba(31, 95, 107, 0.12);
}

.member-profile-card strong,
.member-profile-card p {
  margin: 0;
}

.member-profile-card__tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
}

.member-profile-card__role,
.member-profile-card__self-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 0.88rem;
  font-weight: 600;
}

.member-profile-card__role {
  background: rgba(31, 95, 107, 0.12);
  color: var(--sl-brand-strong);
}

.member-profile-card__self-badge {
  background: rgba(201, 119, 46, 0.18);
  color: var(--sl-accent);
}

.member-profile-card__meta {
  color: var(--sl-ink-soft);
  font-size: 0.92rem;
  line-height: 1.5;
}

.request-stack {
  display: grid;
  gap: 20px;
  align-items: start;
}

.request-column {
  display: grid;
  gap: 12px;
}

.request-column {
  align-self: start;
  align-content: start;
}

.request-column__head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.request-column__head h5,
.request-column__head span {
  margin: 0;
}

.request-column__head span {
  color: var(--sl-brand-strong);
  font-weight: 700;
}

.request-pagination {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 10px;
  color: var(--sl-ink-soft);
}

.request-table {
  width: 100%;
  min-width: 780px;
  border-collapse: collapse;
}

.request-table th,
.request-table td {
  padding: 14px 16px;
  text-align: left;
  border-bottom: 1px solid var(--sl-border-color);
  vertical-align: top;
}

.request-table th {
  color: var(--sl-brand-strong);
  font-size: 0.86rem;
  font-weight: 700;
  white-space: nowrap;
}

.request-table td {
  color: var(--sl-ink-soft);
  line-height: 1.55;
}

.request-table tbody tr:last-child td {
  border-bottom: none;
}

.request-table__actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.request-table__action-btn {
  min-height: 28px;
  padding: 0 10px;
  font-size: 0.88rem;
  line-height: 1;
}

.request-table__placeholder td {
  height: 57px;
  color: transparent;
}

.borrow-request-preview {
  display: grid;
  gap: 10px;
  padding: 16px;
}

.borrow-request-preview__copy {
  display: grid;
  gap: 6px;
}

.borrow-request-preview__copy strong,
.borrow-request-preview__copy p,
.borrow-request-preview__copy span {
  margin: 0;
}

.borrow-request-preview__copy p,
.borrow-request-preview__copy span {
  color: var(--sl-ink-soft);
}

.status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 0.88rem;
  font-weight: 600;
}

.status-badge.is-pending {
  background: rgba(201, 119, 46, 0.15);
  color: #b46819;
}

.status-badge.is-approved {
  background: rgba(36, 117, 56, 0.16);
  color: #2f7a3a;
}

.status-badge.is-rejected {
  background: rgba(183, 54, 54, 0.14);
  color: #b73636;
}

.dialog-scrim {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(14, 23, 28, 0.46);
  backdrop-filter: blur(8px);
}

.dialog-scrim--front {
  z-index: 70;
}

.desk-dialog {
  width: min(100%, 760px);
  max-height: min(88vh, 920px);
  overflow: auto;
  border-radius: 28px;
  background: var(--sl-paper-strong);
  border: 1px solid var(--sl-border-color);
}

.invite-dialog {
  width: min(100%, 840px);
}

.desk-dialog__head,
.desk-dialog__body,
.desk-dialog__foot {
  padding: 22px 24px;
}

.desk-dialog__head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  border-bottom: 1px solid var(--sl-border-color);
}

.desk-dialog__head h2,
.desk-dialog__head p {
  margin: 0;
}

.desk-dialog__head p {
  margin-top: 8px;
  color: var(--sl-ink-soft);
  line-height: 1.6;
}

.desk-dialog__head-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.desk-dialog__body {
  display: grid;
  gap: 18px;
}

.invite-dialog .desk-dialog__body {
  max-height: min(62vh, 560px);
  overflow-y: auto;
}

.invite-dialog .selected-user-list {
  max-height: min(34vh, 300px);
  overflow-y: auto;
  padding-right: 4px;
}

.desk-dialog__foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  border-top: 1px solid var(--sl-border-color);
}

.member-detail-dialog .desk-dialog__body {
  gap: 20px;
}

.member-detail-head {
  display: flex;
  gap: 16px;
  align-items: center;
  padding: 18px;
}

.member-detail-head strong,
.member-detail-head p {
  display: block;
  margin: 0;
}

.member-detail-head p {
  margin-top: 8px;
  color: var(--sl-ink-soft);
}

.member-detail-head__tags {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

.public-shelf-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 14px;
}

.public-shelf-card,
.public-book-card {
  padding: 16px;
}

.public-shelf-card {
  display: grid;
  gap: 14px;
}

.public-shelf-card.is-active {
  border-color: rgba(31, 95, 107, 0.42);
  box-shadow: 0 14px 28px rgba(31, 95, 107, 0.12);
}

.public-shelf-card__content {
  display: grid;
  gap: 8px;
}

.public-shelf-card p {
  margin: 0;
  color: var(--sl-ink-soft);
  line-height: 1.6;
}

.public-book-list {
  display: grid;
  gap: 14px;
}

.public-book-card {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
}

.public-book-card__cover {
  width: 72px;
  height: 96px;
  border-radius: 16px;
  object-fit: cover;
  background: rgba(31, 95, 107, 0.12);
}

.public-book-card__cover.is-placeholder {
  display: grid;
  place-items: center;
  color: var(--sl-brand-strong);
  font-weight: 700;
}

.public-book-card__body {
  display: grid;
  gap: 6px;
}

.public-book-card__body strong,
.public-book-card__body p,
.public-book-card__body span {
  margin: 0;
}

.public-book-card__body p,
.public-book-card__body span {
  color: var(--sl-ink-soft);
}

.public-book-card__action {
  justify-self: end;
}

[data-theme='dark'] .group-switcher__item.is-active {
  border-color: rgba(96, 197, 220, 0.34);
  background:
    linear-gradient(180deg, rgba(31, 95, 107, 0.28), rgba(31, 95, 107, 0.12)),
    var(--sl-soft-panel-bg);
}

[data-theme='dark'] .member-profile-card.is-self,
[data-theme='dark'] .public-shelf-card.is-active {
  box-shadow: 0 16px 32px rgba(0, 0, 0, 0.28);
}

[data-theme='dark'] .status-badge.is-pending {
  color: #f2c287;
}

[data-theme='dark'] .status-badge.is-approved {
  color: #8dd89d;
}

[data-theme='dark'] .status-badge.is-rejected {
  color: #f1a0a0;
}

@media (max-width: 1180px) {
  .community-layout {
    grid-template-columns: 1fr;
  }

  .workspace-overview {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 860px) {
  .workspace-head,
  .desk-dialog__head {
    display: grid;
  }

  .workspace-head__actions,
  .desk-dialog__head-actions {
    justify-content: flex-start;
  }

  .request-pagination {
    justify-content: space-between;
    flex-wrap: wrap;
  }

  .invite-dialog {
    width: 100%;
  }

  .field-inline {
    grid-template-columns: 1fr;
  }

  .public-book-card {
    grid-template-columns: 1fr;
  }

  .public-book-card__action {
    justify-self: stretch;
  }
}

@media (max-width: 640px) {
  .dialog-scrim {
    padding: 12px;
  }

  .desk-dialog {
    width: 100%;
    border-radius: 22px;
  }

  .desk-dialog__head,
  .desk-dialog__body,
  .desk-dialog__foot {
    padding: 18px;
  }

  .workspace-overview,
  .member-grid,
  .public-shelf-grid {
    grid-template-columns: 1fr;
  }

  .selected-user-card,
  .member-detail-head {
    align-items: flex-start;
  }
}
</style>
