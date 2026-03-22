<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

import AppLogo from '@/components/AppLogo.vue'
import UserAvatar from '@/components/UserAvatar.vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const navItems = [
  { path: '/home', title: '首页', note: 'Overview', code: 'H' },
  { path: '/books', title: '图书', note: 'Library', code: 'B' },
  { path: '/borrow', title: '借阅', note: 'Flow', code: 'R' },
  { path: '/community', title: '社区', note: 'Groups', code: 'C' },
  { path: '/social', title: '社交', note: 'Pulse', code: 'S' },
  { path: '/statistics', title: '统计', note: 'Charts', code: 'T' },
  { path: '/profile', title: '个人', note: 'Profile', code: 'P' },
]

const activePath = computed(() => {
  const current = navItems.find((item) => route.path.startsWith(item.path))
  return current?.path || '/home'
})

const currentTitle = computed(() => {
  const item = navItems.find((entry) => entry.path === activePath.value)
  return item?.title || '工作台'
})

const handleLogout = async () => {
  await userStore.logout()
  router.replace('/login')
}

onMounted(() => {
  if (userStore.isLoggedIn && !userStore.userInfo) {
    userStore.fetchProfile()
  }
})
</script>

<template>
  <div class="main-layout">
    <aside class="main-layout__rail surface-card">
      <AppLogo />

      <nav class="main-layout__nav" aria-label="Primary navigation">
        <RouterLink
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="main-layout__nav-link focus-ring"
          :class="{ 'is-active': activePath === item.path }"
        >
          <span class="main-layout__nav-code">{{ item.code }}</span>
          <span class="main-layout__nav-copy">
            <strong>{{ item.title }}</strong>
            <small>{{ item.note }}</small>
          </span>
        </RouterLink>
      </nav>

      <div class="main-layout__user surface-card">
        <UserAvatar
          :src="userStore.userInfo?.avatar"
          :name="userStore.displayName"
          :size="52"
        />
        <div>
          <strong>{{ userStore.displayName }}</strong>
          <p>{{ userStore.userInfo?.userName || '书库成员' }}</p>
        </div>
        <button class="button button--ghost" type="button" @click="handleLogout">退出</button>
      </div>
    </aside>

    <div class="main-layout__content">
      <header class="main-layout__header surface-card">
        <div>
          <span class="eyebrow">Current Desk</span>
          <h2>{{ currentTitle }}</h2>
        </div>

        <div class="pill-nav" aria-label="Mobile navigation">
          <RouterLink
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            class="badge focus-ring"
            :class="{ 'badge--accent': activePath === item.path }"
          >
            {{ item.title }}
          </RouterLink>
        </div>
      </header>

      <main class="main-layout__page">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style scoped>
.main-layout {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 18px;
  padding: 18px;
}

.main-layout__rail {
  display: grid;
  align-content: start;
  gap: 20px;
  padding: 22px;
}

.main-layout__nav {
  display: grid;
  gap: 10px;
}

.main-layout__nav-link {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px;
  border-radius: 22px;
  border: 1px solid transparent;
  transition: background-color 180ms ease, border-color 180ms ease, transform 180ms ease;
}

.main-layout__nav-link:hover {
  transform: translateX(2px);
  background: rgba(31, 95, 107, 0.05);
}

.main-layout__nav-link.is-active {
  background: rgba(31, 95, 107, 0.08);
  border-color: rgba(31, 95, 107, 0.18);
}

.main-layout__nav-code {
  display: grid;
  place-items: center;
  width: 40px;
  height: 58px;
  border-radius: 14px;
  background: linear-gradient(180deg, rgba(31, 95, 107, 0.84), rgba(201, 119, 46, 0.84));
  color: #fff;
  font-weight: 700;
}

.main-layout__nav-copy {
  display: grid;
}

.main-layout__nav-copy strong {
  font-size: 0.98rem;
}

.main-layout__nav-copy small {
  color: var(--sl-ink-soft);
}

.main-layout__user {
  display: grid;
  gap: 10px;
  padding: 18px;
}

.main-layout__user p {
  margin: 4px 0 0;
  color: var(--sl-ink-soft);
}

.main-layout__content {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 18px;
}

.main-layout__header {
  display: grid;
  gap: 14px;
  padding: 22px;
}

.main-layout__header h2 {
  margin: 8px 0 0;
  font-size: 2rem;
}

.main-layout__page {
  min-width: 0;
}

@media (max-width: 1080px) {
  .main-layout {
    grid-template-columns: 1fr;
  }

  .main-layout__rail {
    display: none;
  }
}
</style>
