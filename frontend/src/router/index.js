// Конфигурация маршрутов Vue Router с guards аутентификации и ролей
import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import LibraryView from '../views/LibraryView.vue'
import GameDetailView from '../views/GameDetailView.vue'
import GameEditView from '../views/GameEditView.vue'
import ProfileView from '../views/ProfileView.vue'
import AdminUsersView from '../views/AdminUsersView.vue'
import AdminScrapersView from '../views/AdminScrapersView.vue'
import AdminView from '../views/AdminView.vue'
import DownloadsView from '../views/DownloadsView.vue'
import CollectionsView from '../views/CollectionsView.vue'
import CollectionDetailView from '../views/CollectionDetailView.vue'
import StatisticsView from '../views/StatisticsView.vue'
import LibraryHealthView from '../views/LibraryHealthView.vue'
import NotFoundView from '../views/NotFoundView.vue'

// Определение маршрутов с мета-тегами для guards
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: LoginView,
    meta: { guest: true }            // Только для неавторизованных
  },
  {
    path: '/register',
    name: 'Register',
    component: RegisterView,
    meta: { guest: true }
  },
  {
    path: '/',
    name: 'Library',
    component: LibraryView,
    meta: { requiresAuth: true }     // Требуется авторизация
  },
  {
    path: '/game/:id',
    name: 'GameDetail',
    component: GameDetailView,
    meta: { requiresAuth: true }
  },
  {
    path: '/game/:id/edit',
    name: 'GameEdit',
    component: GameEditView,
    meta: { requiresAuth: true, requiresAdmin: true }  // Только админ
  },
  {
    path: '/profile',
    name: 'Profile',
    component: ProfileView,
    meta: { requiresAuth: true }
  },
  {
    path: '/admin',
    name: 'Admin',
    component: AdminView,
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/users',
    name: 'AdminUsers',
    component: AdminUsersView,
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/scrapers',
    name: 'AdminScrapers',
    component: AdminScrapersView,
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/collections',
    name: 'Collections',
    component: CollectionsView,
    meta: { requiresAuth: true }
  },
  {
    path: '/collections/:id',
    name: 'CollectionDetail',
    component: CollectionDetailView,
    meta: { requiresAuth: true }
  },
  {
    path: '/statistics',
    name: 'Statistics',
    component: StatisticsView,
    meta: { requiresAuth: true }
  },
  {
    path: '/downloads',
    name: 'Downloads',
    component: DownloadsView,
    meta: { requiresAuth: true }
  },
  {
    path: '/health',
    name: 'LibraryHealth',
    component: LibraryHealthView,
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    // Catch-all для 404
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: NotFoundView
  }
]

const router = createRouter({
  history: createWebHistory('/game-library/'),  // Базовый URL с учётом контекстного пути
  routes
})

// Guard маршрутизации: проверка аутентификации и прав администратора
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    next({ name: 'Login' })                      // Редирект на логин
  } else if (to.meta.guest && authStore.isAuthenticated) {
    next({ name: 'Library' })                     // Авторизованных с guest-страниц → в библиотеку
  } else if (to.meta.requiresAdmin && !authStore.isAdmin) {
    next({ name: 'Library' })                     // Не-админов → в библиотеку
  } else {
    next()                                        // Всё в порядке — пропускаем
  }
})

export default router
