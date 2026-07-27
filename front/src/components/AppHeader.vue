<script setup>
import { useAuthStore } from '../stores/auth';
import { useRouter } from 'vue-router';

const authStore = useAuthStore();
const router = useRouter();

const handleLogout = () => {
  authStore.logout();
  router.push({ name: 'login' });
};
</script>

<template>
  <header class="app-header glass-panel">
    <div class="container header-container">
      <div class="logo">
        <span class="text-gradient">Naina</span> Playlist
      </div>
      
      <nav class="nav-links">
        <router-link to="/" class="nav-link">Bibliothèque</router-link>
        <router-link to="/generator" class="nav-link">Générateur</router-link>
        <router-link to="/playlists" class="nav-link">Mes Playlists</router-link>
      </nav>
      
      <div class="user-menu">
        <span class="username">{{ authStore.user?.username }}</span>
        <button @click="handleLogout" class="btn btn-secondary btn-sm">Déconnexion</button>
      </div>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  position: sticky;
  top: 0;
  z-index: 100;
  border-radius: 0;
  border-top: none;
  border-left: none;
  border-right: none;
}

.header-container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
}

.logo {
  font-size: 1.25rem;
  font-weight: 700;
  letter-spacing: -0.5px;
}

.nav-links {
  display: flex;
  gap: 2rem;
}

.nav-link {
  color: var(--text-secondary);
  text-decoration: none;
  font-weight: 500;
  font-size: 0.95rem;
  transition: color var(--transition-fast);
}

.nav-link:hover,
.nav-link.router-link-active {
  color: var(--text-primary);
}

.nav-link.router-link-active {
  position: relative;
}

.nav-link.router-link-active::after {
  content: '';
  position: absolute;
  bottom: -4px;
  left: 0;
  width: 100%;
  height: 2px;
  background: var(--accent-gradient);
  border-radius: 2px;
}

.user-menu {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.username {
  font-size: 0.9rem;
  color: var(--text-muted);
}

.btn-sm {
  padding: 0.4rem 0.8rem;
  font-size: 0.85rem;
}
</style>
