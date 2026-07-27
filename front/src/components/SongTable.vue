<script setup>
import { usePlayerStore } from '../stores/player';

const props = defineProps({
  songs: { type: Array, required: true },
  playlistQueue: { type: Array, default: null }, // If played, what should be the queue
  showActions: { type: Boolean, default: true },
  actionIcon: { type: String, default: 'edit' } // 'edit', 'delete', 'remove'
});

const emit = defineEmits(['action', 'remove']);
const playerStore = usePlayerStore();

const play = (song) => {
  playerStore.playSong(song, props.playlistQueue || props.songs);
};

const formatTime = (seconds) => {
  if (!seconds) return '--:--';
  const mins = Math.floor(seconds / 60);
  const secs = Math.floor(seconds % 60);
  return `${mins}:${secs.toString().padStart(2, '0')}`;
};
</script>

<template>
  <div class="song-table-container glass-card">
    <table class="song-table">
      <thead>
        <tr>
          <th class="w-10"></th>
          <th>Titre</th>
          <th>Artiste</th>
          <th>Album</th>
          <th>Genre</th>
          <th>Durée</th>
          <th v-if="showActions" class="actions-col"></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(song, index) in songs" :key="song.id || index" 
            :class="{ 'is-playing': playerStore.currentSong?.id === song.id }">
          <td class="play-cell">
            <button class="icon-btn play-hover" @click="play(song)" v-if="playerStore.currentSong?.id !== song.id || !playerStore.isPlaying">
              <svg viewBox="0 0 24 24" fill="currentColor"><path d="M8 5v14l11-7z"/></svg>
            </button>
            <button class="icon-btn playing-icon" @click="playerStore.pause()" v-else>
              <div class="bars">
                <div class="bar"></div><div class="bar"></div><div class="bar"></div>
              </div>
            </button>
            <span class="index-num" v-if="playerStore.currentSong?.id !== song.id">{{ index + 1 }}</span>
          </td>
          <td>
            <div class="song-title">{{ song.title || song.originalName }}</div>
          </td>
          <td class="text-secondary">{{ song.artist || '-' }}</td>
          <td class="text-secondary">{{ song.album || '-' }}</td>
          <td><span class="tag" v-if="song.genre">{{ song.genre }}</span></td>
          <td class="text-secondary">{{ formatTime(song.durationSec) }}</td>
          <td v-if="showActions" class="actions-col">
            <button class="icon-btn action-btn" @click="emit('action', song)" v-if="actionIcon === 'edit'" title="Modifier">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path></svg>
            </button>
            <button class="icon-btn action-btn text-danger" @click="emit('remove', song, index)" v-if="actionIcon === 'remove'" title="Retirer de la playlist">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
            </button>
          </td>
        </tr>
        <tr v-if="songs.length === 0">
          <td colspan="7" class="empty-state">Aucune chanson trouvée.</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.song-table-container {
  overflow-x: auto;
}

.song-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
  font-size: 0.9rem;
}

th, td {
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--border-light);
}

th {
  color: var(--text-muted);
  font-weight: 500;
  text-transform: uppercase;
  font-size: 0.75rem;
  letter-spacing: 0.5px;
}

tbody tr {
  transition: background var(--transition-fast);
}

tbody tr:hover {
  background: rgba(255, 255, 255, 0.05);
}

tbody tr.is-playing {
  background: rgba(168, 85, 247, 0.1);
}

tbody tr.is-playing .song-title {
  color: var(--accent-primary);
}

.w-10 {
  width: 40px;
}

.play-cell {
  position: relative;
  text-align: center;
}

.index-num {
  color: var(--text-muted);
}

.play-hover {
  display: none;
  color: var(--text-primary);
}

tr:hover .index-num {
  display: none;
}

tr:hover .play-hover {
  display: inline-flex;
}

.icon-btn {
  background: none;
  border: none;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  color: inherit;
  transition: all 0.2s;
}

.icon-btn svg {
  width: 16px;
  height: 16px;
}

.icon-btn:hover {
  background: rgba(255, 255, 255, 0.1);
}

.action-btn {
  color: var(--text-secondary);
  opacity: 0;
}

tr:hover .action-btn {
  opacity: 1;
}

.text-danger:hover {
  color: var(--danger);
  background: rgba(239, 68, 68, 0.1);
}

.song-title {
  font-weight: 500;
  color: var(--text-primary);
}

.text-secondary {
  color: var(--text-secondary);
}

.tag {
  background: rgba(255, 255, 255, 0.1);
  padding: 0.2rem 0.6rem;
  border-radius: 12px;
  font-size: 0.75rem;
  color: var(--text-secondary);
}

.actions-col {
  width: 60px;
  text-align: right;
}

.empty-state {
  text-align: center;
  padding: 3rem;
  color: var(--text-muted);
}

/* Playing animation */
.playing-icon .bars {
  display: flex;
  align-items: flex-end;
  gap: 2px;
  height: 12px;
}
.playing-icon .bar {
  width: 3px;
  background: var(--accent-primary);
  animation: bounce 1s ease-in-out infinite;
}
.playing-icon .bar:nth-child(1) { animation-delay: 0s; }
.playing-icon .bar:nth-child(2) { animation-delay: 0.2s; }
.playing-icon .bar:nth-child(3) { animation-delay: 0.4s; }

@keyframes bounce {
  0%, 100% { height: 4px; }
  50% { height: 12px; }
}
</style>
