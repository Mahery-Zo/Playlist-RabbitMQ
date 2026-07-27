import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

// Views
import LoginView from '../views/LoginView.vue'
import LibraryView from '../views/LibraryView.vue'
import GeneratorView from '../views/GeneratorView.vue'
import PlaylistsView from '../views/PlaylistsView.vue'
import PlaylistEditView from '../views/PlaylistEditView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { guestOnly: true }
    },
    {
      path: '/',
      name: 'library',
      component: LibraryView,
      meta: { requiresAuth: true }
    },
    {
      path: '/generator',
      name: 'generator',
      component: GeneratorView,
      meta: { requiresAuth: true }
    },
    {
      path: '/playlists',
      name: 'playlists',
      component: PlaylistsView,
      meta: { requiresAuth: true }
    },
    {
      path: '/playlists/:id',
      name: 'playlist-edit',
      component: PlaylistEditView,
      meta: { requiresAuth: true }
    }
  ]
})

// Navigation Guard
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  const isAuthenticated = authStore.isAuthenticated || !!localStorage.getItem('token')

  if (to.meta.requiresAuth && !isAuthenticated) {
    next({ name: 'login' })
  } else if (to.meta.guestOnly && isAuthenticated) {
    next({ name: 'library' })
  } else {
    next()
  }
})

export default router
