<script setup>
import { ref } from 'vue';

const props = defineProps({
  show: Boolean,
  title: String,
});

const emit = defineEmits(['close']);
</script>

<template>
  <transition name="modal">
    <div v-if="show" class="modal-mask">
      <div class="modal-wrapper" @click.self="emit('close')">
        <div class="modal-container glass-panel">
          <div class="modal-header">
            <h3>{{ title }}</h3>
            <button class="close-btn" @click="emit('close')">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
            </button>
          </div>
          <div class="modal-body">
            <slot></slot>
          </div>
        </div>
      </div>
    </div>
  </transition>
</template>

<style scoped>
.modal-mask {
  position: fixed;
  z-index: 9998;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(4px);
  display: flex;
  transition: opacity 0.3s ease;
}

.modal-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

.modal-container {
  width: 90%;
  max-width: 500px;
  padding: 1.5rem;
  transform: translateY(0);
  transition: transform 0.3s ease, opacity 0.3s ease;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.modal-header h3 {
  margin: 0;
  font-size: 1.25rem;
}

.close-btn {
  background: none;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  padding: 0.25rem;
  border-radius: 50%;
  display: flex;
}

.close-btn:hover {
  color: var(--text-primary);
  background: rgba(255, 255, 255, 0.1);
}

.close-btn svg {
  width: 20px;
  height: 20px;
}

/* Transitions */
.modal-enter-from {
  opacity: 0;
}

.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .modal-container,
.modal-leave-to .modal-container {
  transform: scale(0.95) translateY(-20px);
}
</style>
