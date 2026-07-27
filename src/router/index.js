import { createRouter, createWebHistory } from 'vue-router'
import { isAuthenticated } from '../services/api.js'
import LoginView from '@/views/backoffice/LoginView.vue'
import LoginGLPIView from '@/views/backoffice/loginGLPIView.vue'
import DashboardView from '@/views/backoffice/DashboardView.vue'
import TestView from '@/views/TestView.vue'
import ImportView from '@/views/backoffice/ImportView.vue'
import ResetView from '@/views/backoffice/ResetView.vue'
import TIcketsList from '@/views/backoffice/TIcketsList.vue'
import ListElement from '@/views/frontoffice/ListElement.vue'
import CreateTicket from '@/views/frontoffice/CreateTicket.vue'
import PresenationTicket from '@/views/frontoffice/PresenationTicket.vue'
import KanbanColorView from '@/views/backoffice/KanbanColorView.vue'
import StatusLabelView from '@/views/backoffice/StatusLabelView.vue'
import CoutView from '@/views/backoffice/CoutView.vue'
import ImportCoutView from '@/views/backoffice/ImportCoutView.vue'

// --- Routes Frontoffice (pages publiques) ---
const frontofficeRoutes = [
  { path: '/login', component: LoginView },
  { path: '/loginGlpi', component: LoginGLPIView },
  { path: '/listElement', component: ListElement },
  { path: '/createTicket', component: CreateTicket },
  { path: '/presentationTicket', component: PresenationTicket },
  { path: '/cout', component: CoutView },
]

// --- Routes Backoffice (espace admin protégé) ---
const backofficeRoutes = [
  { path: '/dashboard', component: DashboardView, meta: { requiresAuth: true } },
  { path: '/test', component: TestView, meta: { requiresAuth: true } },
  { path: '/import', component: ImportView, meta: { requiresAuth: true } },
  { path: '/reinit', component: ResetView, meta: { requiresAuth: true } },
  { path: '/listTicket', component: TIcketsList, meta: { requiresAuth: true } },
  { path: '/kanban-colors', component: KanbanColorView, meta: { requiresAuth: true } },
  { path: '/status-labels', component: StatusLabelView, meta: { requiresAuth: true } },
  { path: '/importCout', component: ImportCoutView, meta: { requiresAuth: true } },
]


const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    ...frontofficeRoutes,
    ...backofficeRoutes,
    { path: '/', redirect: '/login' },
  ],
})

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !isAuthenticated()) {
    return '/login'
  }
})

export default router
