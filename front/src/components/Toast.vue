<script setup>
import { ref, onMounted } from 'vue';

const toasts = ref([]);
let toastId = 0;

const addToast = (message, type = 'success', duration = 3000) => {
  const id = toastId++;
  toasts.value.push({ id, message, type });
  setTimeout(() => removeToast(id), duration);
};

const removeToast = (id) => {
  toasts.value = toasts.value.filter(t => t.id !== id);
};

// Expose globalment
if (typeof window !== 'undefined') {
  window.$toast = addToast;
}
</script>

<template>
  <div class="toast-container">
    <transition-group name="toast-list">
      <div v-for="toast in toasts" :key="toast.id" class="toast glass-panel" :class="`toast-${toast.type}`">
        {{ toast.message }}
      </div>
    </transition-group>
  </div>
</template>

<style scoped>
.toast-container {
  position: fixed;
  bottom: 90px; /* Above audio player */
  right: 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  z-index: 9999;
}

.toast {
  padding: 12px 20px;
  border-radius: 8px;
  font-size: 0.9rem;
  font-weight: 500;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  display: flex;
  align-items: center;
}

.toast-success {
  border-left: 4px solid var(--success);
}

.toast-error {
  border-left: 4px solid var(--danger);
}

.toast-list-enter-active,
.toast-list-leave-active {
  transition: all 0.3s ease;
}
.toast-list-enter-from {
  opacity: 0;
  transform: translateX(30px);
}
.toast-list-leave-to {
  opacity: 0;
  transform: scale(0.9);
}
</style>
