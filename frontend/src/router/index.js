import { createRouter, createWebHistory } from 'vue-router'

import AdminLayout from '@/layouts/AdminLayout.vue'
import { isLoggedIn } from '@/api/authTokens'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition
    if (to.hash) return { el: to.hash, top: 110 }
    return { top: 0 }
  },
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/RegisterView.vue'),
    },
    {
      // 管理端布局：左侧场景菜单 + 右侧详情，登录后才可进入
      path: '/',
      component: AdminLayout,
      meta: { requiresAuth: true },
      children: [
        { path: '', name: 'home', component: () => import('@/views/HomeView.vue'), meta: { title: '场景集合' } },
        {
          path: 'scenario/01-auth',
          name: 'scenario-01-auth',
          component: () => import('@/views/scenario/AuthView.vue'),
          meta: { title: '用户登录与认证' },
        },
        {
          path: 'scenario/01-auth/doc',
          name: 'scenario-01-auth-doc',
          component: () => import('@/views/scenario/DocView.vue'),
          props: { scenarioId: '01-auth' },
          meta: { title: '用户登录与认证 · 技术文档' },
        },
        {
          path: 'scenario/03-flash-sale',
          name: 'scenario-03-flash-sale',
          component: () => import('@/views/scenario/FlashSaleView.vue'),
          meta: { title: '秒杀抢购' },
        },
        {
          path: 'scenario/03-flash-sale/doc',
          name: 'scenario-03-flash-sale-doc',
          component: () => import('@/views/scenario/DocView.vue'),
          props: { scenarioId: '03-flash-sale' },
          meta: { title: '秒杀抢购 · 技术文档' },
        },
        // 后续场景：详情页 + /doc 文档页成对追加，并在 config/scenarios.js、config/scenarioDocs.js 登记
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/',
    },
  ],
})

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !isLoggedIn()) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
})

router.afterEach((to) => {
  document.title = `${to.meta.title || (to.path === '/register' ? '创建账号' : '登录')} · reprise`
})

export default router
