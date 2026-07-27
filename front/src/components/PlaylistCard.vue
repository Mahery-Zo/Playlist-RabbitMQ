<script setup>
import { formatTime } from '../utils'; // On va créer utils.js

const props = defineProps({
  playlist: { type: Object, required: true }
});

const emit = defineEmits(['click', 'delete', 'download', 'play']);

const time = (sec) => {
  if(!sec) return '--:--';
  const m = Math.floor(sec/60);
  const s = Math.floor(sec%60);
  return `${m}:${s.toString().padStart(2, '0')}`;
};
</script>

<template>
  <div class="playlist-card glass-card" @click="emit('click', playlist.id)">
    <div class="card-art">
      <div class="art-placeholder">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 18V5l12-2v13"></path><circle cx="6" cy="18" r="3"></circle><circle cx="18" cy="16" r="3"></circle></svg>
      </div>
    </div>
    <div class="card-content">
      <h3 class="name">{{ playlist.name }}</h3>
      <div class="meta">
        <span>{{ playlist.songCount }} titres</span>
        <span class="dot">•</span>
        <span>{{ time(playlist.totalDurationSec) }}</span>
      </div>
      <div class="actions" @click.stop>
        <button class="icon-btn" title="Jouer" @click.stop="emit('play', playlist.id)">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="M8 5v14l11-7z"/></svg>
        </button>
        <button class="icon-btn" title="Télécharger .zip" @click.stop="emit('download', playlist.id)">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="7 10 12 15 17 10"></polyline><line x1="12" y1="15" x2="12" y2="3"></line></svg>
        </button>
        <button class="icon-btn text-danger" title="Supprimer" @click.stop="emit('delete', playlist.id)">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path></svg>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.playlist-card {
  cursor: pointer;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.card-art {
  aspect-ratio: 1;
  background: var(--border-light);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.art-placeholder svg {
  width: 48px;
  height: 48px;
  color: var(--text-muted);
  opacity: 0.5;
}

.card-content {
  padding: 1.25rem;
  position: relative;
}

.name {
  margin: 0 0 0.5rem 0;
  font-size: 1.1rem;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.meta {
  color: var(--text-secondary);
  font-size: 0.85rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.dot {
  font-size: 0.6rem;
}

.actions {
  position: absolute;
  top: -48px; /* Hover effect to slide up from image */
  right: 10px;
  display: flex;
  gap: 0.5rem;
  opacity: 0;
  transition: all var(--transition-normal);
  background: rgba(0,0,0,0.6);
  backdrop-filter: blur(4px);
  padding: 0.5rem;
  border-radius: var(--radius-full);
}

.playlist-card:hover .actions {
  top: -24px;
  opacity: 1;
}

.icon-btn {
  background: none;
  border: none;
  color: white;
  cursor: pointer;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.icon-btn:hover {
  background: rgba(255,255,255,0.2);
}

.icon-btn svg { width: 16px; height: 16px; }

.text-danger:hover {
  color: var(--danger);
  background: rgba(239,68,68,0.2);
}
</style>
