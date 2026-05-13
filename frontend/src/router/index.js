import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

import LoginPage from '../views/auth/LoginPage.vue'
import RegisterPage from '../views/auth/RegisterPage.vue'
import AthleteDashboardPage from '../views/athlete/AthleteDashboardPage.vue'
import { CoachWorkspacePage } from '../modules/coach'
import {
  AdminShellLayout,
  AdminUsersPage,
  AdminLibraryPage,
  AdminAiPage,
} from '../modules/admin'
import RecommendationsPage from '../views/shared/RecommendationsPage.vue'
import MyTrainingsPage from '../views/athlete/MyTrainingsPage.vue'
import AchievementJournalPage from '../views/athlete/AchievementJournalPage.vue'
import MyAchievementsMediaPage from '../views/athlete/MyAchievementsMediaPage.vue'
import ProfilePage from '../views/shared/ProfilePage.vue'
import { CoachCabinetPage } from '../modules/coachCabinet'
import { CoachTrainingLibraryPage } from '../modules/trainingLibrary'

const routes = [
  { path: '/login', component: LoginPage, meta: { guest: true } },
  { path: '/register', component: RegisterPage, meta: { guest: true } },
  { path: '/dashboard', component: AthleteDashboardPage, meta: { requiresAuth: true, role: 'ATHLETE' } },
  { path: '/coach/dashboard', redirect: '/coach/cabinet' },
  { path: '/coach/cabinet', component: CoachCabinetPage, meta: { requiresAuth: true, role: 'COACH' } },
  { path: '/coach/workspace', component: CoachWorkspacePage, meta: { requiresAuth: true, role: 'COACH' } },
  { path: '/coach/library', component: CoachTrainingLibraryPage, meta: { requiresAuth: true, role: 'COACH' } },
  {
    path: '/admin',
    component: AdminShellLayout,
    meta: { requiresAuth: true, role: 'ADMIN' },
    redirect: '/admin/users',
    children: [
      { path: 'users', component: AdminUsersPage },
      { path: 'library', component: AdminLibraryPage },
      { path: 'ai', component: AdminAiPage },
    ],
  },
  { path: '/recommendations', component: RecommendationsPage, meta: { requiresAuth: true, role: 'ATHLETE' } },
  { path: '/my-trainings', component: MyTrainingsPage, meta: { requiresAuth: true, role: 'ATHLETE' } },
  {
    path: '/achievement-journal',
    component: AchievementJournalPage,
    meta: { requiresAuth: true, role: 'ATHLETE' },
  },
  {
    path: '/my-achievements-media',
    component: MyAchievementsMediaPage,
    meta: { requiresAuth: true, role: 'ATHLETE' },
  },
  { path: '/profile', component: ProfilePage, meta: { requiresAuth: true } },
  { path: '/', redirect: '/login' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()

  // Пока есть токен, но профиль ещё не подтянут — сначала /auth/me, иначе role-guest ломают маршрут (белый экран)
  if (auth.token && !auth.user) {
    await auth.fetchMe()
  }

  if (to.meta?.guest && auth.isAuthenticated) {
    const home = auth.homeRoute
    if (home.path === to.path) {
      return true
    }
    return home
  }

  if (to.meta?.requiresAuth && !auth.isAuthenticated) {
    return { path: '/login' }
  }

  if (to.meta?.role && auth.user?.role !== to.meta.role) {
    return auth.homeRoute
  }

  return true
})

export default router

