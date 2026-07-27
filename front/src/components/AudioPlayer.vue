<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue';
import { usePlayerStore } from '../stores/player';

const playerStore = usePlayerStore();
const audioRef = ref(null);
const progress = ref(0);
const duration = ref(0);
const currentTime = ref(0);

// Mettre à jour la source audio quand currentSong change
watch(() => playerStore.currentSong, (newSong) => {
  if (newSong && audioRef.value) {
    audioRef.value.src = `/api/songs/${newSong.id}/stream`;
    if (playerStore.isPlaying) {
      audioRef.value.play().catch(e => console.error("Auto-play prevented", e));
    }
  }
});

// Gérer play/pause
watch(() => playerStore.isPlaying, (isPlaying) => {
  if (!audioRef.value) return;
  if (isPlaying) {
    audioRef.value.play().catch(e => console.error("Play prevented", e));
  } else {
    audioRef.value.pause();
  }
});

const onTimeUpdate = () => {
  if (audioRef.value) {
    currentTime.value = audioRef.value.currentTime;
    duration.value = audioRef.value.duration || 0;
    progress.value = duration.value ? (currentTime.value / duration.value) * 100 : 0;
  }
};

const onEnded = () => {
  if (playerStore.hasNext) {
    playerStore.next();
  } else {
    playerStore.isPlaying = false;
    progress.value = 0;
    currentTime.value = 0;
  }
};

const seek = (e) => {
  if (!audioRef.value || !duration.value) return;
  const rect = e.target.getBoundingClientRect();
  const x = e.clientX - rect.left;
  const percentage = Math.max(0, Math.min(1, x / rect.width));
  audioRef.value.currentTime = percentage * duration.value;
};

const formatTime = (seconds) => {
  if (!seconds || isNaN(seconds)) return "0:00";
  const mins = Math.floor(seconds / 60);
  const secs = Math.floor(seconds % 60);
  return `${mins}:${secs.toString().padStart(2, '0')}`;
};

// Event listeners for media keys (optional)
onMounted(() => {
  if ('mediaSession' in navigator) {
    navigator.mediaSession.setActionHandler('play', () => playerStore.resume());
    navigator.mediaSession.setActionHandler('pause', () => playerStore.pause());
    navigator.mediaSession.setActionHandler('previoustrack', () => playerStore.prev());
    navigator.mediaSession.setActionHandler('nexttrack', () => playerStore.next());
  }
});

watch(() => playerStore.currentSong, (song) => {
  if (song && 'mediaSession' in navigator) {
    navigator.mediaSession.metadata = new MediaMetadata({
      title: song.title || song.originalName,
      artist: song.artist || 'Inconnu',
      album: song.album || 'Inconnu',
    });
  }
});
</script>

<template>
  <div class="audio-player glass-panel" :class="{ 'is-visible': playerStore.currentSong }">
    <!-- Hidden Audio Element -->
    <audio 
      ref="audioRef" 
      @timeupdate="onTimeUpdate" 
      @ended="onEnded"
      @loadedmetadata="onTimeUpdate"
    ></audio>

    <div class="progress-container" @click="seek">
      <div class="progress-bar" :style="{ width: `${progress}%` }"></div>
    </div>

    <div class="container player-content">
      <!-- Info -->
      <div class="song-info">
        <div class="album-art">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" v-if="!playerStore.currentSong">
            <path d="M9 18V5l12-2v13"></path>
            <circle cx="6" cy="18" r="3"></circle>
            <circle cx="18" cy="16" r="3"></circle>
          </svg>
          <div class="art-gradient" v-else></div>
        </div>
        <div class="meta" v-if="playerStore.currentSong">
          <div class="title">{{ playerStore.currentSong.title || playerStore.currentSong.originalName }}</div>
          <div class="artist">{{ playerStore.currentSong.artist || 'Artiste inconnu' }}</div>
        </div>
      </div>

      <!-- Controls -->
      <div class="controls">
        <button class="control-btn" @click="playerStore.prev()" :disabled="!playerStore.hasPrev">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="M6 6h2v12H6zm3.5 6l8.5 6V6z"/></svg>
        </button>
        
        <button class="control-btn play-btn" @click="playerStore.togglePlay()" :disabled="!playerStore.currentSong">
          <svg viewBox="0 0 24 24" fill="currentColor" v-if="!playerStore.isPlaying"><path d="M8 5v14l11-7z"/></svg>
          <svg viewBox="0 0 24 24" fill="currentColor" v-else><path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z"/></svg>
        </button>
        
        <button class="control-btn" @click="playerStore.next()" :disabled="!playerStore.hasNext">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="M6 18l8.5-6L6 6v12zM16 6v12h2V6h-2z"/></svg>
        </button>
      </div>

      <!-- Time -->
      <div class="time-info">
        <span>{{ formatTime(currentTime) }}</span>
        <span>/</span>
        <span>{{ formatTime(duration || playerStore.currentSong?.durationSec) }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.audio-player {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 80px;
  border-radius: 0;
  border-bottom: none;
  border-left: none;
  border-right: none;
  transform: translateY(100%);
  transition: transform 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  z-index: 1000;
}

.audio-player.is-visible {
  transform: translateY(0);
}

.progress-container {
  height: 4px;
  background: rgba(255, 255, 255, 0.1);
  cursor: pointer;
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  transition: height 0.2s;
}

.progress-container:hover {
  height: 6px;
}

.progress-bar {
  height: 100%;
  background: var(--accent-gradient);
  border-top-right-radius: 4px;
  border-bottom-right-radius: 4px;
  box-shadow: 0 0 10px rgba(168, 85, 247, 0.5);
}

.player-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  padding-top: 4px;
}

.song-info {
  display: flex;
  align-items: center;
  gap: 1rem;
  width: 30%;
  min-width: 200px;
}

.album-art {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.art-gradient {
  width: 100%;
  height: 100%;
  background: var(--accent-gradient);
  opacity: 0.8;
}

.album-art svg {
  width: 24px;
  height: 24px;
  color: var(--text-muted);
}

.meta {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.title {
  font-weight: 600;
  font-size: 0.95rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.artist {
  font-size: 0.8rem;
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.controls {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  flex: 1;
  justify-content: center;
}

.control-btn {
  background: none;
  border: none;
  color: var(--text-primary);
  cursor: pointer;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.2s;
}

.control-btn:disabled {
  color: var(--text-muted);
  cursor: not-allowed;
  opacity: 0.5;
}

.control-btn:not(:disabled):hover {
  color: var(--accent-primary);
  background: rgba(255, 255, 255, 0.05);
}

.play-btn {
  width: 48px;
  height: 48px;
  background: var(--text-primary);
  color: var(--bg-base);
}

.play-btn:not(:disabled):hover {
  background: white;
  transform: scale(1.05);
}

.play-btn svg {
  width: 24px;
  height: 24px;
}

.time-info {
  width: 30%;
  text-align: right;
  font-size: 0.85rem;
  color: var(--text-secondary);
  font-variant-numeric: tabular-nums;
  display: flex;
  justify-content: flex-end;
  gap: 0.25rem;
}
</style>
