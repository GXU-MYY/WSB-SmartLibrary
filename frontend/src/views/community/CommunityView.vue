<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'

import { getMyBooks, getShelves } from '@/api/book'
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
  getShareRecords,
  operateGroupUsers,
  rejectGroupBorrowRequest,
  shareToGroup,
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
  MyBookList,
  ShareRecord,
  Shelf,
  UserInfo,
} from '@/types/models'
import { formatDateTime, normalizePage } from '@/utils/format'
import { notifyError, notifySuccess } from '@/utils/notify'

const PENDING_STATUS = 0
const APPROVED_STATUS = 1
const REJECTED_STATUS = 2

const userStore = useUserStore()

const loading = ref(false)
const savingGroup = ref(false)
const savingMembers = ref(false)
const searchingCreateMember = ref(false)
const searchingInviteMember = ref(false)
const sharing = ref(false)
const requestingBookId = ref<number | null>(null)
const handlingRequestId = ref<number | null>(null)

const groups = ref<Group[]>([])
const selectedGroupId = ref(0)
const members = ref<GroupUser[]>([])
const shareRecords = ref<ShareRecord[]>([])
const publicShelves = ref<GroupPublicShelf[]>([])
const publicBooks = ref<GroupPublicBook[]>([])
const borrowRequests = ref<GroupBorrowRequest[]>([])
const myBooks = ref<MyBookList | null>(null)
const myShelves = ref<Shelf[]>([])

const showCreateDialog = ref(false)
const showInviteDialog = ref(false)
const memberDetailUserId = ref<number | null>(null)

const selectedCreateMembers = ref<UserInfo[]>([])
const selectedInviteMembers = ref<UserInfo[]>([])
const createMemberPhone = ref('')
const inviteMemberPhone = ref('')

const groupForm = reactive({
  groupName: '',
  remark: '',
})

const shareType = ref<'book' | 'bookshelf'>('book')
const shareTargetId = ref(0)

const selectedGroup = computed(
  () => groups.value.find((item) => item.id === selectedGroupId.value) || null,
)
const currentUserId = computed(() => Number(userStore.userInfo?.id || 0))
const isOwner = computed(() => selectedGroup.value?.ownerId === currentUserId.value)
const selectedMember = computed(
  () => members.value.find((item) => item.userId === memberDetailUserId.value) || null,
)

const shareTargetOptions = computed(() => {
  if (shareType.value === 'book') {
    return (myBooks.value?.books || []).map((book) => ({
      id: book.id,
      label: book.title,
    }))
  }

  return myShelves.value.map((shelf) => ({
    id: shelf.id,
    label: shelf.shelfName,
  }))
})

const incomingRequests = computed(() =>
  borrowRequests.value.filter((item) => item.ownerUserId === currentUserId.value),
)

const outgoingRequests = computed(() =>
  borrowRequests.value.filter((item) => item.borrowerUserId === currentUserId.value),
)

const selectedMemberShelves = computed(() =>
  memberDetailUserId.value == null
    ? []
    : publicShelves.value.filter((item) => item.ownerUserId === memberDetailUserId.value),
)

const selectedMemberBooks = computed(() =>
  memberDetailUserId.value == null
    ? []
    : publicBooks.value.filter((item) => item.ownerUserId === memberDetailUserId.value),
)

const loadBaseResources = async () => {
  const [booksResult, shelvesResult] = await Promise.all([getMyBooks(), getShelves()])
  myBooks.value = booksResult
  myShelves.value = shelvesResult
}

const loadWorkspace = async (groupId: number) => {
  const [membersResult, recordsResult, shelvesResult, booksResult, requestsResult] =
    await Promise.allSettled([
      getGroupUsers(groupId, 'in'),
      getShareRecords(groupId),
      getGroupPublicShelves(groupId),
      getGroupPublicBooks(groupId),
      getGroupBorrowRequests(groupId),
    ])

  members.value = membersResult.status === 'fulfilled' ? membersResult.value : []
  shareRecords.value = recordsResult.status === 'fulfilled' ? recordsResult.value : []
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
    await Promise.all([loadBaseResources(), loadGroups()])
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
    notifyError('未找到该手机号对应的用户')
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

const openCreateDialog = () => {
  resetCreateDialog()
  showCreateDialog.value = true
}

const closeCreateDialog = () => {
  showCreateDialog.value = false
  resetCreateDialog()
}

const resetInviteDialog = () => {
  inviteMemberPhone.value = ''
  selectedInviteMembers.value = []
}

const openInviteDialog = () => {
  if (!selectedGroup.value) {
    notifyError('请先选择群组')
    return
  }
  if (!isOwner.value) {
    notifyError('只有群主可以邀请新成员')
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
}

const closeMemberDetail = () => {
  memberDetailUserId.value = null
}

const getMemberRoleText = (member: GroupUser) =>
  selectedGroup.value?.ownerId === member.userId ? '群主' : '群成员'

const getMemberPublicShelfCount = (userId: number) =>
  publicShelves.value.filter((item) => item.ownerUserId === userId).length

const getMemberPublicBookCount = (userId: number) =>
  publicBooks.value.filter((item) => item.ownerUserId === userId).length

const handleAddCreateMember = async () => {
  searchingCreateMember.value = true
  try {
    const user = await findUserByPhone(createMemberPhone.value)
    if (!user) {
      return
    }
    if (user.id === currentUserId.value) {
      notifyError('只需要添加其他初始成员')
      return
    }
    if (selectedCreateMembers.value.some((item) => item.id === user.id)) {
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
    if (user.id === currentUserId.value) {
      notifyError('你已经在该群组中')
      return
    }
    if (members.value.some((member) => member.userId === user.id)) {
      notifyError('该用户已经是群成员')
      return
    }
    if (selectedInviteMembers.value.some((item) => item.id === user.id)) {
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
  if (!groupForm.groupName.trim()) {
    notifyError('请先填写群组名称')
    return
  }

  savingGroup.value = true
  try {
    const result = await createGroup({
      groupName: groupForm.groupName.trim(),
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
  if (!selectedGroup.value || !isOwner.value) {
    return
  }

  const ok = window.confirm(`确认将 ${member.nickname} 移出群聊吗？`)
  if (!ok) {
    return
  }

  await operateGroupUsers({
    groupId: selectedGroup.value.id,
    userIds: [member.userId],
    type: 'minus',
  })
  notifySuccess('成员已移出群聊')

  if (member.userId === memberDetailUserId.value) {
    closeMemberDetail()
  }
  await loadWorkspace(selectedGroup.value.id)
}

const handleExitGroup = async () => {
  if (!selectedGroup.value || isOwner.value) {
    return
  }

  const groupName = selectedGroup.value.groupName
  const ok = window.confirm(`确认退出群聊「${groupName}」吗？`)
  if (!ok) {
    return
  }

  await exitGroup(selectedGroup.value.id)
  notifySuccess(`你已退出群聊「${groupName}」`)
  closeMemberDetail()
  await loadGroups()
}

const handleDeleteGroup = async () => {
  if (!selectedGroup.value || !isOwner.value) {
    return
  }

  const groupName = selectedGroup.value.groupName
  const ok = window.confirm(`确认解散群组「${groupName}」吗？此操作不可撤销。`)
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

const handleShare = async () => {
  if (!selectedGroup.value || !shareTargetId.value) {
    notifyError('请选择要分享的内容')
    return
  }

  sharing.value = true
  try {
    await shareToGroup({
      groupId: selectedGroup.value.id,
      bookId: shareType.value === 'book' ? shareTargetId.value : undefined,
      bookshelfId: shareType.value === 'bookshelf' ? shareTargetId.value : undefined,
    })
    notifySuccess('内容已分享至群组')
    await loadWorkspace(selectedGroup.value.id)
  } finally {
    sharing.value = false
  }
}

const hasPendingRequest = (bookId: number) =>
  borrowRequests.value.some(
    (item) =>
      item.bookId === bookId &&
      item.borrowerUserId === currentUserId.value &&
      item.status === PENDING_STATUS,
  )

const canRequestBorrow = (book: GroupPublicBook) =>
  Boolean(book.borrowable) &&
  book.ownerUserId !== currentUserId.value &&
  !hasPendingRequest(book.bookId)

const getBorrowButtonLabel = (book: GroupPublicBook) => {
  if (book.ownerUserId === currentUserId.value) {
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

const handleBorrowRequest = async (book: GroupPublicBook) => {
  if (!selectedGroup.value || !canRequestBorrow(book)) {
    return
  }

  requestingBookId.value = book.bookId
  try {
    await createGroupBorrowRequest({
      groupId: selectedGroup.value.id,
      bookId: book.bookId,
    })
    notifySuccess(`已向 ${book.ownerNickname || '书主'} 发起借阅申请`)
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
    notifySuccess('借阅申请已同意')
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
    notifySuccess('借阅申请已拒绝')
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

watch(selectedGroupId, async (groupId) => {
  closeInviteDialog()
  closeMemberDetail()
  shareTargetId.value = 0

  if (groupId) {
    await loadWorkspace(groupId)
  } else {
    members.value = []
    shareRecords.value = []
    publicShelves.value = []
    publicBooks.value = []
    borrowRequests.value = []
  }
})

watch(shareType, () => {
  shareTargetId.value = 0
})

useRegisterPageRefresh(loadPage)
onMounted(loadPage)
</script>

<template>
  <div class="page-shell page-stack">
    <PageIntro
      eyebrow="Community Desk"
      title="把群组协作放进同一个阅读工作区"
      description="在这里选择群组、邀请成员、处理借阅申请，也可以把自己的图书和书架分享给群成员。"
    />

    <section class="community-layout">
      <div class="community-sidebar">
        <SectionPanel title="选择群组" hint="切换群组后，中间工作区会同步更新。">
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
              :class="{ 'is-active': selectedGroupId === group.id }"
              type="button"
              @click="selectedGroupId = group.id"
            >
              <strong>{{ group.groupName }}</strong>
              <p>{{ group.remark || '暂无群组说明' }}</p>
            </button>
            <p v-if="!groups.length" class="muted">还没有可用群组，先创建一个吧。</p>
          </div>
        </SectionPanel>

        <SectionPanel
          title="分享工作台"
          hint="把自己的图书或书架分享给当前群组，让协作从内容流动开始。"
        >
          <div v-if="selectedGroup" class="section-stack">
            <div class="field">
              <label>分享类型</label>
              <select v-model="shareType">
                <option value="book">图书</option>
                <option value="bookshelf">书架</option>
              </select>
            </div>

            <div class="field">
              <label>目标内容</label>
              <select v-model.number="shareTargetId">
                <option :value="0" disabled>选择内容</option>
                <option v-for="item in shareTargetOptions" :key="item.id" :value="item.id">
                  {{ item.label }}
                </option>
              </select>
            </div>

            <button class="button button--primary" type="button" :disabled="sharing" @click="handleShare">
              {{ sharing ? '分享中...' : `分享至 ${selectedGroup.groupName}` }}
            </button>
          </div>
          <EmptyState v-else title="先选择群组" />
        </SectionPanel>
      </div>

      <SectionPanel
        class="community-workspace"
        title="群组工作区"
        hint="成员、借阅申请和分享记录集中在这里处理。"
      >
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
                  class="button button--secondary"
                  type="button"
                  @click="openInviteDialog"
                >
                  邀请成员
                </button>
                <button
                  v-if="isOwner"
                  class="button button--danger"
                  type="button"
                  @click="handleDeleteGroup"
                >
                  解散群组
                </button>
                <button
                  v-else
                  class="button button--ghost"
                  type="button"
                  @click="handleExitGroup"
                >
                  退出群聊
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
                <p>点击成员卡片查看对方公开书架和可借图书。仅群主可以移除成员。</p>
              </div>

              <div v-if="members.length" class="member-grid">
                <article
                  v-for="member in members"
                  :key="member.id"
                  class="panel-card member-profile-card focus-ring"
                  @click="openMemberDetail(member)"
                >
                  <UserAvatar :name="member.nickname" :size="68" />
                  <strong>{{ member.nickname }}</strong>
                  <span class="member-profile-card__role">{{ getMemberRoleText(member) }}</span>
                  <p class="member-profile-card__meta">
                    {{ member.joinTime ? formatDateTime(member.joinTime) : '刚加入' }}
                  </p>
                  <p class="member-profile-card__meta">
                    {{ getMemberPublicShelfCount(member.userId) }} 个公开书架 ·
                    {{ getMemberPublicBookCount(member.userId) }} 本公开图书
                  </p>

                  <button
                    v-if="isOwner && member.userId !== currentUserId"
                    class="button button--ghost member-profile-card__remove"
                    type="button"
                    @click.stop="handleRemoveMember(member)"
                  >
                    踢出群聊
                  </button>
                </article>
              </div>
              <EmptyState v-else title="当前群组还没有成员" />
            </section>

            <section class="section-block">
              <div class="section-title">
                <h4>借阅申请</h4>
                <p>先申请，后借阅。出借方同意后，系统才会创建正式借阅记录。</p>
              </div>

              <div class="request-columns">
                <div class="request-column">
                  <div class="request-column__head">
                    <h5>收到的申请</h5>
                    <span>{{ incomingRequests.length }}</span>
                  </div>

                  <div v-if="incomingRequests.length" class="request-list">
                    <article
                      v-for="request in incomingRequests"
                      :key="request.id"
                      class="panel-card request-card"
                    >
                      <div class="request-card__head">
                        <strong>{{ request.bookName }}</strong>
                        <span :class="requestStatusClass(request.status)">{{ requestStatusText(request.status) }}</span>
                      </div>
                      <p>
                        {{ request.borrowerNickname || `用户 ${request.borrowerUserId}` }}
                        想借阅这本书
                      </p>
                      <span>来源书架：{{ request.shelfName || '公开书架' }}</span>
                      <span>申请时间：{{ request.createTime ? formatDateTime(request.createTime) : '未知' }}</span>
                      <span>预计归还：{{ request.dueTime || '未填写' }}</span>
                      <span v-if="request.requestRemark">备注：{{ request.requestRemark }}</span>

                      <div v-if="request.status === PENDING_STATUS" class="request-card__actions">
                        <button
                          class="button button--primary"
                          type="button"
                          :disabled="handlingRequestId === request.id"
                          @click="handleApproveRequest(request)"
                        >
                          {{ handlingRequestId === request.id ? '处理中...' : '同意借阅' }}
                        </button>
                        <button
                          class="button button--ghost"
                          type="button"
                          :disabled="handlingRequestId === request.id"
                          @click="handleRejectRequest(request)"
                        >
                          拒绝
                        </button>
                      </div>
                    </article>
                  </div>
                  <EmptyState v-else title="还没有收到借阅申请" />
                </div>

                <div class="request-column">
                  <div class="request-column__head">
                    <h5>我发出的申请</h5>
                    <span>{{ outgoingRequests.length }}</span>
                  </div>

                  <div v-if="outgoingRequests.length" class="request-list">
                    <article
                      v-for="request in outgoingRequests"
                      :key="request.id"
                      class="panel-card request-card"
                    >
                      <div class="request-card__head">
                        <strong>{{ request.bookName }}</strong>
                        <span :class="requestStatusClass(request.status)">{{ requestStatusText(request.status) }}</span>
                      </div>
                      <p>向 {{ request.ownerNickname || `用户 ${request.ownerUserId}` }} 发起了借阅申请</p>
                      <span>来源书架：{{ request.shelfName || '公开书架' }}</span>
                      <span>申请时间：{{ request.createTime ? formatDateTime(request.createTime) : '未知' }}</span>
                      <span>预计归还：{{ request.dueTime || '未填写' }}</span>
                    </article>
                  </div>
                  <EmptyState v-else title="你还没有发起借阅申请" />
                </div>
              </div>
            </section>

            <section class="section-block">
              <div class="section-title">
                <h4>群组分享记录</h4>
                <p>记录图书和书架的分享轨迹，方便群内成员快速回顾内容流转。</p>
              </div>

              <div v-if="shareRecords.length" class="share-log">
                <article v-for="record in shareRecords" :key="record.id" class="panel-card share-log__item">
                  <strong>{{ record.name || `目标 #${record.targetId}` }}</strong>
                  <p>
                    {{ record.nickName || `用户 ${record.sharePerson}` }}
                    · {{ record.shareType }}
                    · {{ record.shareTime ? formatDateTime(record.shareTime) : '未知时间' }}
                  </p>
                </article>
              </div>
              <EmptyState v-else title="当前群组还没有分享记录" />
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
              <p>可以按手机号补充初始成员，也可以先创建群组后再邀请。</p>
            </div>
            <button class="button button--ghost desk-dialog__close" type="button" @click="closeCreateDialog">
              关闭
            </button>
          </header>

          <div class="desk-dialog__body">
            <div class="section-stack">
              <div class="field">
                <label>群组名称</label>
                <input v-model="groupForm.groupName" type="text" placeholder="例如：设计书共读组" />
              </div>

              <div class="field">
                <label>群组说明</label>
                <textarea v-model="groupForm.remark" placeholder="描述这个群组的阅读主题和协作方式。" />
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
                <article v-for="user in selectedCreateMembers" :key="user.id" class="selected-user-card">
                  <div>
                    <strong>{{ user.nickName || user.userName }}</strong>
                    <p>{{ user.phone || '未绑定手机号' }}</p>
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
        <section class="surface-card desk-dialog desk-dialog--wide">
          <header class="desk-dialog__head">
            <div>
              <span class="eyebrow">Invite Members</span>
              <h2>邀请成员</h2>
              <p>按手机号查找用户，然后加入待邀请列表。</p>
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
                <article v-for="user in selectedInviteMembers" :key="user.id" class="selected-user-card">
                  <div>
                    <strong>{{ user.nickName || user.userName }}</strong>
                    <p>{{ user.phone || '未绑定手机号' }}</p>
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
      <div v-if="selectedMember" class="dialog-scrim" @click.self="closeMemberDetail">
        <section class="surface-card desk-dialog desk-dialog--wide member-detail-dialog">
          <header class="desk-dialog__head">
            <div>
              <span class="eyebrow">Member Profile</span>
              <h2>{{ selectedMember.nickname }}</h2>
              <p>查看这位成员公开出来的书架与可借图书。</p>
            </div>
            <button class="button button--ghost desk-dialog__close" type="button" @click="closeMemberDetail">
              关闭
            </button>
          </header>

          <div class="desk-dialog__body">
            <div class="member-detail-head panel-card">
              <UserAvatar :name="selectedMember.nickname" :size="72" />
              <div>
                <strong>{{ selectedMember.nickname }}</strong>
                <p>{{ getMemberRoleText(selectedMember) }}</p>
                <span>{{ selectedMember.joinTime ? formatDateTime(selectedMember.joinTime) : '加入时间未知' }}</span>
              </div>
            </div>

            <div class="section-stack">
              <section class="section-block">
                <div class="section-title">
                  <h4>公开书架</h4>
                  <p>只有群成员主动公开的书架会显示在这里。</p>
                </div>

                <div v-if="selectedMemberShelves.length" class="public-shelf-grid">
                  <article v-for="shelf in selectedMemberShelves" :key="shelf.id" class="panel-card public-shelf-card">
                    <strong>{{ shelf.shelfName }}</strong>
                    <p>{{ shelf.remark || '这个书架还没有补充说明。' }}</p>
                  </article>
                </div>
                <EmptyState v-else title="这个成员还没有公开书架" />
              </section>

              <section class="section-block">
                <div class="section-title">
                  <h4>可借公开图书</h4>
                  <p>如果图书当前可借，你可以在这里直接发起借阅申请。</p>
                </div>

                <div v-if="selectedMemberBooks.length" class="public-book-list">
                  <article
                    v-for="book in selectedMemberBooks"
                    :key="`${book.shelfId}-${book.bookId}`"
                    class="panel-card public-book-card"
                  >
                    <img v-if="book.coverUrl" :src="book.coverUrl" :alt="book.title" class="public-book-card__cover" />
                    <div v-else class="public-book-card__cover is-placeholder">书</div>

                    <div class="public-book-card__body">
                      <strong>{{ book.title }}</strong>
                      <p>{{ book.author || '作者未填写' }}</p>
                      <span>{{ book.shelfName }}</span>
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
                <EmptyState v-else title="这个成员还没有公开图书" />
              </section>
            </div>
          </div>
        </section>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.community-layout {
  display: grid;
  grid-template-columns: minmax(300px, 340px) minmax(0, 1fr);
  gap: 18px;
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
.share-log,
.group-switcher {
  display: grid;
  gap: 14px;
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
  gap: 8px;
  padding: 16px;
  text-align: left;
  transition:
    transform 0.18s ease,
    border-color 0.18s ease,
    background 0.18s ease;
}

.group-switcher__item strong,
.group-switcher__item p {
  margin: 0;
}

.group-switcher__item p {
  color: var(--sl-ink-soft);
  line-height: 1.6;
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
  border: 1px solid var(--sl-border-color);
  border-radius: 14px;
  padding: 12px 14px;
  background: var(--sl-card-bg);
  color: var(--sl-ink);
}

.field textarea {
  min-height: 108px;
  resize: vertical;
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
  grid-template-columns: repeat(auto-fill, minmax(148px, 1fr));
  gap: 14px;
}

.member-profile-card {
  position: relative;
  display: grid;
  justify-items: center;
  gap: 8px;
  padding: 18px 14px 16px;
  text-align: center;
  cursor: pointer;
}

.member-profile-card strong,
.member-profile-card p {
  margin: 0;
}

.member-profile-card__role {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(31, 95, 107, 0.12);
  color: var(--sl-brand-strong);
  font-size: 0.88rem;
  font-weight: 600;
}

.member-profile-card__meta {
  color: var(--sl-ink-soft);
  font-size: 0.92rem;
  line-height: 1.5;
}

.member-profile-card__remove {
  width: 100%;
}

.request-columns {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.request-column,
.request-card {
  display: grid;
  gap: 12px;
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

.request-card {
  padding: 16px;
}

.request-card span,
.request-card p,
.share-log__item p {
  color: var(--sl-ink-soft);
}

.request-card p,
.share-log__item p {
  margin: 0;
  line-height: 1.6;
}

.request-card__head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.request-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.share-log__item {
  padding: 16px;
  display: grid;
  gap: 8px;
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

.desk-dialog {
  width: min(100%, 720px);
  max-height: min(88vh, 920px);
  overflow: auto;
  border-radius: 28px;
  background: var(--sl-card-bg);
  border: 1px solid var(--sl-border-color);
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

.desk-dialog__body {
  display: grid;
  gap: 18px;
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
.member-detail-head p,
.member-detail-head span {
  display: block;
}

.member-detail-head p,
.member-detail-head span {
  color: var(--sl-ink-soft);
}

.public-shelf-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 14px;
}

.public-shelf-card,
.public-book-card {
  padding: 16px;
}

.public-shelf-card {
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
  .request-columns {
    grid-template-columns: 1fr;
  }

  .workspace-head,
  .desk-dialog__head {
    display: grid;
  }

  .workspace-head__actions {
    justify-content: flex-start;
  }

  .field-inline {
    grid-template-columns: 1fr;
  }

  .public-book-card {
    grid-template-columns: 1fr;
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

  .workspace-overview {
    grid-template-columns: 1fr;
  }

  .member-grid,
  .public-shelf-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
