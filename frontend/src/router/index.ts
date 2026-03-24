import { createRouter, createWebHistory } from 'vue-router'

import { isAuthenticated } from '@/utils/auth'

const appTitle = import.meta.env.VITE_APP_TITLE || 'WSB SmartLibrary'

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior(_to, _from, savedPosition) {
    return savedPosition || { top: 0 }
  },
  routes: [
    {
      path: '/login',
      component: () => import('@/layouts/AuthLayout.vue'),
      meta: { title: '登录', guestOnly: true },
      children: [
        {
          path: '',
          name: 'login',
          component: () => import('@/views/auth/LoginView.vue'),
        },
      ],
    },
    {
      path: '/register',
      component: () => import('@/layouts/AuthLayout.vue'),
      meta: { title: '注册', guestOnly: true },
      children: [
        {
          path: '',
          name: 'register',
          component: () => import('@/views/auth/RegisterView.vue'),
        },
      ],
    },
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        { path: '', redirect: '/home' },
        {
          path: 'home',
          name: 'home',
          component: () => import('@/views/home/HomeView.vue'),
          meta: { title: '首页' },
        },
        {
          path: 'books',
          name: 'books',
          component: () => import('@/views/books/BookListView.vue'),
          meta: { title: '我的图书' },
        },
        {
          path: 'books/:id',
          name: 'book-detail',
          component: () => import('@/views/books/BookDetailView.vue'),
          meta: { title: '图书详情' },
        },
        {
          path: 'borrow',
          name: 'borrow',
          component: () => import('@/views/borrow/BorrowView.vue'),
          meta: { title: '借阅管理' },
        },
        {
          path: 'community',
          name: 'community',
          component: () => import('@/views/community/CommunityView.vue'),
          meta: { title: '社区' },
        },
        {
          path: 'social',
          name: 'social',
          component: () => import('@/views/social/SocialView.vue'),
          meta: { title: '社交动态' },
        },
        {
          path: 'profile',
          name: 'profile',
          component: () => import('@/views/profile/ProfileView.vue'),
          meta: { title: '个人中心' },
        },
        {
          path: 'statistics',
          name: 'statistics',
          component: () => import('@/views/statistics/StatisticsView.vue'),
          meta: { title: '数据统计' },
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/views/NotFoundView.vue'),
      meta: { title: '页面不存在' },
    },
  ],
})

router.beforeEach((to) => {
  const loggedIn = isAuthenticated()

  if (to.meta.requiresAuth && !loggedIn) {
    return {
      path: '/login',
      query: { redirect: to.fullPath },
    }
  }

  if (to.meta.guestOnly && loggedIn) {
    return '/home'
  }

  return true
})

router.afterEach((to) => {
  const title = typeof to.meta.title === 'string' ? to.meta.title : '智能书库'
  document.title = `${title} | ${appTitle}`
})

export default router
