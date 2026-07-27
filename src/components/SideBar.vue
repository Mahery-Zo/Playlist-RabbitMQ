<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { isAuthenticated, logout } from '@/services/api'

const route = useRoute()
const router = useRouter()

// recalculé à chaque changement de route (donc après login/logout)
const loggedIn = computed(() => {
    route.path            // dépendance réactive sur la navigation
    return isAuthenticated()
})

function handleLogout() {
    logout()
    router.push('/login')
}
</script>

<template>
  <aside class="sidebar">
    <nav>

      <RouterLink to="/login">Login Admin</RouterLink>
      <RouterLink to="/loginGLPI">Login GLPI</RouterLink>
      <RouterLink to="/listElement">Liste des Elements</RouterLink>
      <RouterLink to="/createTicket">Creer Ticket</RouterLink>
      <RouterLink to="/presentationTicket">Presentation Ticket Kanban</RouterLink>
      <RouterLink to="/cout">Cout Ticket</RouterLink>

    </nav>
    <nav v-if="loggedIn">
      <RouterLink to="/import">Importer Computer</RouterLink>
      <RouterLink to="/importCout">Importer Cout</RouterLink>
      <RouterLink to="/reinit">Réinitialisation</RouterLink>
      <RouterLink to="/dashboard">Dashboard</RouterLink>
      <RouterLink to="/listTicket">Liste de Tickets</RouterLink>
      <RouterLink to="/Kanban-colors">Couleurs KanBan</RouterLink>
      <RouterLink to="/status-labels">Label malagasy</RouterLink>
    </nav>
    <button v-if="loggedIn" @click="handleLogout">Se déconnecter</button>
  </aside>
</template>


<style scoped>
.sidebar {
  width: 220px;
  min-height: 100vh;
  background: #1a1a1a;
  color: #fff;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.sidebar nav {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.sidebar a {
  color: #fff;
  text-decoration: none;
  padding: 0.5rem;
  border-radius: 4px;
}
.sidebar a:hover {
  background: #333;
}
.sidebar a.router-link-active {
  background: #444;
}
</style>
