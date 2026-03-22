<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'

import { getMyBooks, getShelves } from '@/api/book'
import {
  createGroup,
  deleteGroup,
  getGroupUsers,
  getGroups,
  getShareRecords,
  operateGroupUsers,
  shareToGroup,
} from '@/api/community'
import { useRegisterPageRefresh } from '@/composables/usePageRefresh'
import { getUsers } from '@/api/user'
import EmptyState from '@/components/EmptyState.vue'
import LoadingState from '@/components/LoadingState.vue'
import PageIntro from '@/components/PageIntro.vue'
import SectionPanel from '@/components/SectionPanel.vue'
import { useUserStore } from '@/stores/user'
import type { Group, GroupUser, MyBookList, ShareRecord, Shelf, UserInfo } from '@/types/models'
import { formatDateTime, normalizePage } from '@/utils/format'
import { notifyError, notifySuccess } from '@/utils/notify'

const userStore = useUserStore()

const loading = ref(false)
const savingGroup = ref(false)
const savingMembers = ref(false)
const sharing = ref(false)

const groups = ref<Group[]>([])
const selectedGroupId = ref(0)
const members = ref<GroupUser[]>([])
const outsiders = ref<GroupUser[]>([])
const shareRecords = ref<ShareRecord[]>([])
const userOptions = ref<UserInfo[]>([])
const myBooks = ref<MyBookList | null>(null)
const myShelves = ref<Shelf[]>([])

const groupForm = reactive({
  groupName: '',
  remark: '',
  userIds: [] as number[],
})

const inviteIds = ref<number[]>([])
const shareType = ref<'book' | 'bookshelf'>('book')
const shareTargetId = ref(0)

const selectedGroup = computed(() => groups.value.find((item) => item.id === selectedGroupId.value) || null)
const isOwner = computed(() => selectedGroup.value?.ownerId === userStore.userInfo?.id)
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

const loadBaseResources = async () => {
  const [usersResult, booksResult, shelvesResult] = await Promise.all([
    getUsers({ page: 1, page_size: 100 }),
    getMyBooks(),
    getShelves(),
  ])

  userOptions.value = normalizePage(usersResult).records
  myBooks.value = booksResult
  myShelves.value = shelvesResult
}

const loadWorkspace = async (groupId: number) => {
  const [membersResult, outsidersResult, recordsResult] = await Promise.allSettled([
    getGroupUsers(groupId, 'in'),
    getGroupUsers(groupId, 'out'),
    getShareRecords(groupId),
  ])

  members.value = membersResult.status === 'fulfilled' ? membersResult.value : []
  outsiders.value = outsidersResult.status === 'fulfilled' ? outsidersResult.value : []
  shareRecords.value = recordsResult.status === 'fulfilled' ? recordsResult.value : []
}

const loadGroups = async () => {
  loading.value = true

  try {
    const page = await getGroups({ page: 1, page_size: 50 })
    const normalized = normalizePage(page)
    groups.value = normalized.records

    if (!selectedGroupId.value && groups.value.length) {
      selectedGroupId.value = groups.value[0].id
    }
  } finally {
    loading.value = false
  }
}

const loadPage = async () => {
  await Promise.all([loadBaseResources(), loadGroups()])

  if (selectedGroupId.value) {
    await loadWorkspace(selectedGroupId.value)
  }
}

const handleCreateGroup = async () => {
  if (!groupForm.groupName.trim()) {
    notifyError('请先填写群组名称')
    return
  }

  if (!groupForm.userIds.length) {
    notifyError('请至少选择一位初始成员')
    return
  }

  savingGroup.value = true

  try {
    const result = await createGroup(groupForm)
    notifySuccess('群组已创建', `${result.groupName} 已进入你的社区工作区。`)
    groupForm.groupName = ''
    groupForm.remark = ''
    groupForm.userIds = []
    await loadGroups()
    selectedGroupId.value = result.id
  } finally {
    savingGroup.value = false
  }
}

const handleInviteMembers = async () => {
  if (!selectedGroup.value || !inviteIds.value.length) {
    notifyError('请选择要邀请的成员')
    return
  }

  savingMembers.value = true

  try {
    await operateGroupUsers({
      groupId: selectedGroup.value.id,
      userIds: inviteIds.value,
      type: 'add',
    })
    inviteIds.value = []
    notifySuccess('成员已加入群组')
    await loadWorkspace(selectedGroup.value.id)
  } finally {
    savingMembers.value = false
  }
}

const handleRemoveMember = async (member: GroupUser) => {
  if (!selectedGroup.value) {
    return
  }

  await operateGroupUsers({
    groupId: selectedGroup.value.id,
    userIds: [member.userId],
    type: 'minus',
  })
  notifySuccess('成员已移出群组')
  await loadWorkspace(selectedGroup.value.id)
}

const handleDeleteGroup = async () => {
  if (!selectedGroup.value) {
    return
  }

  await deleteGroup(selectedGroup.value.id)
  notifySuccess('群组已删除', `${selectedGroup.value.groupName} 不再出现在列表中。`)
  selectedGroupId.value = 0
  await loadGroups()
}

const handleShare = async () => {
  if (!selectedGroup.value || !shareTargetId.value) {
    notifyError('请选择分享目标')
    return
  }

  sharing.value = true

  try {
    await shareToGroup({
      groupId: selectedGroup.value.id,
      bookId: shareType.value === 'book' ? shareTargetId.value : undefined,
      bookshelfId: shareType.value === 'bookshelf' ? shareTargetId.value : undefined,
    })
    notifySuccess('内容已分享进群组')
    await loadWorkspace(selectedGroup.value.id)
  } finally {
    sharing.value = false
  }
}

watch(selectedGroupId, (groupId) => {
  if (groupId) {
    loadWorkspace(groupId)
  } else {
    members.value = []
    outsiders.value = []
    shareRecords.value = []
  }
})

useRegisterPageRefresh(loadPage)

onMounted(loadPage)
</script>

<template>
  <div class="page-shell page-stack">
    <PageIntro
      eyebrow="Community Desk"
      title="把群组协作和内容流转放进同一个社区工作区"
      description="这里关注的是谁在一起、分享了什么、内容如何在小群里流动，而不是单纯的聊天入口。"
    />

    <section class="page-grid community-layout">
      <SectionPanel
        title="创建与选择群组"
        hint="先把一组读者和书架关系组织起来，再逐步往群里放内容。"
      >
        <div class="field">
          <label>群组名称</label>
          <input v-model="groupForm.groupName" type="text" placeholder="例如：设计书共读组" />
        </div>

        <div class="field">
          <label>群组说明</label>
          <textarea v-model="groupForm.remark" placeholder="说明群组的主题、使用方式和氛围。" />
        </div>

        <div class="field">
          <label>初始成员</label>
          <select v-model="groupForm.userIds" multiple size="6">
            <option v-for="user in userOptions" :key="user.id" :value="user.id">
              {{ user.nickName || user.userName }}
            </option>
          </select>
          <span class="field__hint">按住 Ctrl 或 Command 可多选。</span>
        </div>

        <button class="button button--primary" type="button" :disabled="savingGroup" @click="handleCreateGroup">
          {{ savingGroup ? '创建中...' : '创建群组' }}
        </button>

        <div class="group-list">
          <button
            v-for="group in groups"
            :key="group.id"
            class="group-list__item focus-ring"
            :class="{ 'is-active': selectedGroupId === group.id }"
            type="button"
            @click="selectedGroupId = group.id"
          >
            <strong>{{ group.groupName }}</strong>
            <p>{{ group.remark || '暂无群组说明' }}</p>
          </button>
        </div>
      </SectionPanel>

      <SectionPanel
        title="群组工作区"
        hint="这里会显示当前群组的成员和最新分享记录。"
      >
        <LoadingState v-if="loading && groups.length === 0" />
        <template v-else-if="selectedGroup">
          <div class="workspace-head surface-card">
            <div>
              <span class="eyebrow">Current Group</span>
              <h3>{{ selectedGroup.groupName }}</h3>
              <p>{{ selectedGroup.remark || '还没有补充群组说明。' }}</p>
            </div>
            <button v-if="isOwner" class="button button--ghost" type="button" @click="handleDeleteGroup">删除群组</button>
          </div>

          <div class="field">
            <label>邀请新成员</label>
            <select v-model="inviteIds" multiple size="5">
              <option v-for="user in outsiders" :key="user.userId" :value="user.userId">
                {{ user.nickname }}
              </option>
            </select>
          </div>

          <button class="button button--secondary" type="button" :disabled="savingMembers" @click="handleInviteMembers">
            {{ savingMembers ? '邀请中...' : '邀请加入群组' }}
          </button>

          <div class="member-list">
            <article v-for="member in members" :key="member.id" class="member-list__item">
              <div>
                <strong>{{ member.nickname }}</strong>
                <p>{{ formatDateTime(member.joinTime) }}</p>
              </div>
              <button
                v-if="isOwner && member.userId !== userStore.userInfo?.id"
                class="button button--ghost"
                type="button"
                @click="handleRemoveMember(member)"
              >
                移出
              </button>
            </article>
          </div>

          <div class="share-log">
            <article v-for="record in shareRecords" :key="record.id" class="share-log__item">
              <strong>{{ record.name || `目标 #${record.targetId}` }}</strong>
              <p>{{ record.nickName || `用户 ${record.sharePerson}` }} · {{ record.shareType }} · {{ formatDateTime(record.shareTime) }}</p>
            </article>
          </div>
        </template>
        <EmptyState v-else title="还没有可用群组" />
      </SectionPanel>

      <SectionPanel
        title="分享工作台"
        hint="把一本书或一个书架推入当前群组，让群内内容开始形成共同上下文。"
      >
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
            <option :value="0">选择内容</option>
            <option v-for="item in shareTargetOptions" :key="item.id" :value="item.id">{{ item.label }}</option>
          </select>
        </div>

        <button class="button button--primary" type="button" :disabled="sharing || !selectedGroup" @click="handleShare">
          {{ sharing ? '分享中...' : selectedGroup ? `分享至 ${selectedGroup.groupName}` : '先选择群组' }}
        </button>
      </SectionPanel>
    </section>
  </div>
</template>

<style scoped>
.community-layout > *:nth-child(1),
.community-layout > *:nth-child(3) {
  grid-column: span 4;
}

.community-layout > *:nth-child(2) {
  grid-column: span 8;
}

.group-list,
.member-list,
.share-log {
  display: grid;
  gap: 12px;
}

.group-list__item,
.member-list__item,
.share-log__item,
.workspace-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: start;
  padding: 16px;
  border: 1px solid var(--sl-line);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.54);
}

.group-list__item {
  text-align: left;
  cursor: pointer;
}

.group-list__item.is-active {
  border-color: rgba(31, 95, 107, 0.28);
  background: rgba(31, 95, 107, 0.08);
}

.group-list__item p,
.member-list__item p,
.share-log__item p,
.workspace-head p {
  margin: 6px 0 0;
  color: var(--sl-ink-soft);
  line-height: 1.6;
}

.workspace-head {
  align-items: center;
}

@media (max-width: 1200px) {
  .community-layout > * {
    grid-column: span 12 !important;
  }
}
</style>
