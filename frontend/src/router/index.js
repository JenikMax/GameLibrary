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
import DownloadsView from '../views/DownloadsView.vue'
import NotFoundView from '../views/NotFoundView.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: LoginView,
    meta: { guest: true }
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
    meta: { requiresAuth: true }
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
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: ProfileView,
    meta: { requiresAuth: true }
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
    path: '/downloads',
    name: 'Downloads',
    component: DownloadsView,
    meta: { requiresAuth: true }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: NotFoundView
  }
]

const router = createRouter({
  history: createWebHistory('/game-library/'),
  routes
})

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    next({ name: 'Login' })
  } else if (to.meta.guest && authStore.isAuthenticated) {
    next({ name: 'Library' })
  } else if (to.meta.requiresAdmin && !authStore.isAdmin) {
    next({ name: 'Library' })
  } else {
    next()
  }
})

export default router
