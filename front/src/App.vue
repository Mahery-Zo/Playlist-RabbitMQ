<script setup>
import { onMounted } from 'vue';
import AppHeader from './components/AppHeader.vue';
import AudioPlayer from './components/AudioPlayer.vue';
import Toast from './components/Toast.vue';
import { useAuthStore } from './stores/auth';

const authStore = useAuthStore();

onMounted(() => {
  // Initialiser l'état d'authentification depuis le localStorage
  authStore.initAuth();
});
</script>

<template>
  <div class="app-layout">
    <AppHeader v-if="authStore.isAuthenticated" />
    <main class="main-content">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
    <AudioPlayer v-if="authStore.isAuthenticated" />
    <Toast />
  </div>
</template>

<style scoped>
.app-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.main-content {
  flex: 1;
  padding: 2rem 0;
}

/* Page Transitions */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}
</style>
