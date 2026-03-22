<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

import AppLogo from '@/components/AppLogo.vue'
import RefreshButton from '@/components/RefreshButton.vue'
import ThemeToggle from '@/components/ThemeToggle.vue'
import UserAvatar from '@/components/UserAvatar.vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const mobileMenuOpen = ref(false)

const navItems = [
  { path: '/home', title: '首页' },
  { path: '/books', title: '图书' },
  { path: '/borrow', title: '借阅' },
  { path: '/community', title: '社区' },
  { path: '/social', title: '社交' },
  { path: '/statistics', title: '统计' },
  { path: '/profile', title: '个人' },
]

const activePath = computed(() => {
  const current = navItems.find((item) => route.path.startsWith(item.path))
  return current?.path || '/home'
})

const handleLogout = async () => {
  await userStore.logout()
  router.replace('/login')
}

const toggleMobileMenu = () => {
  mobileMenuOpen.value = !mobileMenuOpen.value
}

onMounted(() => {
  if (userStore.isLoggedIn && !userStore.userInfo) {
    userStore.fetchProfile()
  }
})

watch(
  () => route.fullPath,
  () => {
    mobileMenuOpen.value = false
  },
)
</script>

<template>
  <div class="main-layout">
    <header class="main-layout__topbar surface-card">
      <div class="main-layout__brand">
        <AppLogo compact />
      </div>

      <nav class="main-layout__nav" aria-label="Primary navigation">
        <RouterLink
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="main-layout__nav-link focus-ring"
          :class="{ 'is-active': activePath === item.path }"
        >
          {{ item.title }}
        </RouterLink>
      </nav>

      <div class="main-layout__tools">
        <RefreshButton />
        <ThemeToggle compact icon-only class="main-layout__desktop-theme" />
        <ThemeToggle compact icon-only class="main-layout__mobile-theme" />

        <RouterLink to="/profile" class="main-layout__account surface-card focus-ring">
          <UserAvatar
            :src="userStore.userInfo?.avatar"
            :name="userStore.displayName"
            :size="32"
          />
          <div class="main-layout__account-copy">
            <strong>{{ userStore.displayName }}</strong>
          </div>
        </RouterLink>

        <button class="button button--ghost main-layout__logout" type="button" @click="handleLogout">
          退出
        </button>

        <button
          class="button button--ghost main-layout__mobile-toggle"
          type="button"
          :aria-expanded="mobileMenuOpen"
          @click="toggleMobileMenu"
        >
          {{ mobileMenuOpen ? '收起' : '菜单' }}
        </button>
      </div>
    </header>

    <section v-if="mobileMenuOpen" class="main-layout__mobile-sheet surface-card">
      <RouterLink to="/profile" class="main-layout__mobile-account focus-ring">
        <UserAvatar
          :src="userStore.userInfo?.avatar"
          :name="userStore.displayName"
          :size="42"
        />
        <div class="main-layout__mobile-account-copy">
          <strong>{{ userStore.displayName }}</strong>
        </div>
      </RouterLink>

      <nav class="main-layout__mobile-nav" aria-label="Mobile navigation">
        <RouterLink
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="main-layout__mobile-link focus-ring"
          :class="{ 'is-active': activePath === item.path }"
        >
          {{ item.title }}
        </RouterLink>
      </nav>

      <div class="main-layout__mobile-actions">
        <button class="button button--ghost" type="button" @click="handleLogout">退出当前账号</button>
      </div>
    </section>

    <main class="main-layout__page">
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.main-layout {
  min-height: 100vh;
  width: min(100%, var(--sl-max-width));
  margin: 0 auto;
  display: grid;
  align-content: start;
  gap: 18px;
  padding: 108px clamp(12px, 2vw, 18px) 18px;
}

.main-layout__topbar {
  position: fixed;
  top: 12px;
  left: 50%;
  transform: translateX(-50%);
  width: min(calc(100vw - 24px), var(--sl-max-width));
  z-index: 30;
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 14px 16px;
}

.main-layout__brand {
  display: flex;
  align-items: center;
  flex: 0 0 auto;
  min-width: max-content;
}

.main-layout__nav {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1 1 auto;
  gap: 10px;
  min-width: 0;
  overflow-x: auto;
  overflow-y: hidden;
  white-space: nowrap;
  padding: 2px 2px 4px;
}

.main-layout__nav-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  min-height: 40px;
  padding: 0 16px;
  border-radius: 999px;
  border: 1px solid transparent;
  color: var(--sl-ink-soft);
  background: transparent;
  transition: background-color 180ms ease, border-color 180ms ease, color 180ms ease,
    transform 180ms ease;
}

.main-layout__nav-link:hover {
  transform: translateY(-1px);
  background: var(--sl-nav-hover);
  color: var(--sl-ink);
}

.main-layout__nav-link.is-active {
  background: var(--sl-nav-active-bg);
  border-color: var(--sl-nav-active-border);
  color: var(--sl-brand-strong);
}

.main-layout__tools {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex: 0 0 auto;
  margin-left: auto;
  gap: 10px;
  min-width: max-content;
}

.main-layout__mobile-theme {
  display: none;
}

.main-layout__account {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  min-height: 40px;
  padding: 4px 12px 4px 6px;
  border-radius: 999px;
  box-shadow: none;
}

.main-layout__account:hover {
  border-color: var(--sl-nav-active-border);
  background: var(--sl-nav-hover);
}

.main-layout__account-copy {
  display: grid;
  min-width: 0;
}

.main-layout__account-copy strong {
  margin: 0;
}

.main-layout__account-copy strong {
  font-size: 0.94rem;
  white-space: nowrap;
  line-height: 1;
}

.main-layout__mobile-toggle {
  display: none;
}

.main-layout__mobile-sheet {
  display: none;
  position: fixed;
  top: 82px;
  left: 50%;
  transform: translateX(-50%);
  width: min(calc(100vw - 24px), var(--sl-max-width));
  z-index: 29;
  max-height: calc(100vh - 96px);
  overflow: auto;
}

.main-layout__page {
  min-width: 0;
}

.main-layout__mobile-account {
  display: none;
  align-items: center;
  gap: 12px;
  padding: 4px 0;
}

.main-layout__mobile-account-copy {
  display: grid;
  min-width: 0;
}

.main-layout__mobile-account-copy strong {
  margin: 0;
}

.main-layout__mobile-account:hover {
  color: var(--sl-brand-strong);
}

.main-layout__mobile-nav {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.main-layout__mobile-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 14px;
  border-radius: 18px;
  border: 1px solid var(--sl-line);
  background: var(--sl-mobile-card-bg);
  font-size: 0.96rem;
  font-weight: 600;
}

.main-layout__mobile-link.is-active {
  background: var(--sl-nav-active-bg);
  border-color: var(--sl-nav-active-border);
}

.main-layout__mobile-actions {
  display: none;
  gap: 10px;
  flex-wrap: wrap;
}

@media (max-width: 1080px) {
  .main-layout {
    gap: 14px;
    padding: 88px 10px 14px;
  }

  .main-layout__topbar {
    padding: 12px 14px;
    gap: 12px;
  }

  .main-layout__tools {
    gap: 8px;
  }

  .main-layout__nav,
  .main-layout__account,
  .main-layout__logout {
    display: none;
  }

  .main-layout__desktop-theme {
    display: none;
  }

  .main-layout__mobile-theme {
    display: inline-flex;
  }

  .main-layout__mobile-toggle {
    display: inline-flex;
  }

  .main-layout__mobile-sheet {
    display: grid;
    gap: 14px;
    padding: 16px;
  }

  .main-layout__mobile-account,
  .main-layout__mobile-actions {
    display: flex;
  }

  .main-layout__mobile-account {
    align-items: center;
  }
}

@media (max-width: 640px) {
  .main-layout {
    padding-top: 84px;
  }

  .main-layout__topbar {
    top: 8px;
    width: min(calc(100vw - 16px), var(--sl-max-width));
    padding: 10px 12px;
  }

  .main-layout__mobile-sheet {
    top: 74px;
    width: min(calc(100vw - 16px), var(--sl-max-width));
    max-height: calc(100vh - 86px);
    padding: 14px;
  }

  .main-layout__mobile-nav {
    grid-template-columns: 1fr;
  }

  .main-layout__brand :deep(.logo) {
    gap: 10px;
  }

  .main-layout__brand :deep(.logo__mark) {
    width: 42px;
    height: 42px;
    padding: 5px;
    border-radius: 14px;
  }

  .main-layout__brand :deep(.logo__copy strong) {
    font-size: 0.98rem;
  }

  .main-layout__mobile-toggle {
    padding-inline: 10px;
  }

  .main-layout__tools {
    gap: 8px;
  }
}
</style>
