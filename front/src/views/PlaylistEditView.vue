<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { usePlaylistsStore } from '../stores/playlists';
import { useSongsStore } from '../stores/songs';
import SongTable from '../components/SongTable.vue';
import Modal from '../components/Modal.vue';

const route = useRoute();
const router = useRouter();
const playlistsStore = usePlaylistsStore();
const songsStore = useSongsStore();

const playlist = computed(() => playlistsStore.currentPlaylist);
const name = ref('');
const songs = ref([]);
const showAddModal = ref(false);
const searchQuery = ref('');

onMounted(async () => {
  try {
    const data = await playlistsStore.fetchPlaylist(route.params.id);
    name.value = data.name;
    songs.value = [...data.songs];
    await songsStore.fetchSongs();
  } catch (e) {
    window.$toast("Erreur chargement playlist", "error");
    router.push('/playlists');
  }
});

const removeSong = (song, index) => {
  songs.value.splice(index, 1);
};

const moveUp = (index) => {
  if (index > 0) {
    const temp = songs.value[index];
    songs.value[index] = songs.value[index - 1];
    songs.value[index - 1] = temp;
  }
};

const moveDown = (index) => {
  if (index < songs.value.length - 1) {
    const temp = songs.value[index];
    songs.value[index] = songs.value[index + 1];
    songs.value[index + 1] = temp;
  }
};

const handleSave = async () => {
  if (!name.value.trim()) return window.$toast("Le nom est requis", "error");
  try {
    const songIds = songs.value.map(s => s.id);
    await playlistsStore.updatePlaylist(route.params.id, name.value, songIds);
    window.$toast("Playlist modifiée", "success");
    router.push('/playlists');
  } catch(e) {
    window.$toast("Erreur de sauvegarde", "error");
  }
};

const filteredLibrary = computed(() => {
  if (!searchQuery.value) return songsStore.songs;
  const q = searchQuery.value.toLowerCase();
  return songsStore.songs.filter(s => 
    s.title?.toLowerCase().includes(q) || 
    s.artist?.toLowerCase().includes(q)
  );
});

const addSong = (song) => {
  songs.value.push(song);
  window.$toast("Chanson ajoutée");
};
</script>

<template>
  <div class="container" v-if="playlist">
    <div class="header-section mb-8">
      <div>
        <input type="text" v-model="name" class="playlist-name-input" />
        <p class="text-secondary">Édition de la playlist</p>
      </div>
      <div class="actions">
        <button class="btn btn-secondary" @click="showAddModal = true">Ajouter des chansons</button>
        <button class="btn btn-primary" @click="handleSave">Sauvegarder</button>
      </div>
    </div>

    <div class="glass-card">
      <table class="song-table">
        <thead>
          <tr>
            <th class="w-10">Pos</th>
            <th>Titre</th>
            <th>Artiste</th>
            <th>Durée</th>
            <th class="actions-col"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(song, index) in songs" :key="index + '-' + song.id">
            <td class="text-muted">{{ index + 1 }}</td>
            <td class="font-medium">{{ song.title || song.originalName }}</td>
            <td class="text-secondary">{{ song.artist || '-' }}</td>
            <td class="text-secondary">{{ song.durationSec ? Math.floor(song.durationSec/60) + ':' + (song.durationSec%60).toString().padStart(2,'0') : '--:--' }}</td>
            <td class="actions-col">
              <button class="icon-btn" @click="moveUp(index)" :disabled="index === 0" title="Monter">↑</button>
              <button class="icon-btn" @click="moveDown(index)" :disabled="index === songs.length - 1" title="Descendre">↓</button>
              <button class="icon-btn text-danger" @click="removeSong(song, index)" title="Retirer">✕</button>
            </td>
          </tr>
          <tr v-if="songs.length === 0">
            <td colspan="5" class="empty-state">Playlist vide.</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Modal d'ajout -->
    <Modal :show="showAddModal" title="Ajouter depuis la bibliothèque" @close="showAddModal = false">
      <input type="text" v-model="searchQuery" class="form-control mb-4" placeholder="Rechercher..." style="width: 100%" />
      
      <div class="add-list">
        <div v-for="song in filteredLibrary" :key="song.id" class="add-item">
          <div>
            <div class="title">{{ song.title || song.originalName }}</div>
            <div class="artist">{{ song.artist || '-' }}</div>
          </div>
          <button class="btn btn-sm btn-secondary" @click="addSong(song)">Ajouter</button>
        </div>
      </div>
    </Modal>
  </div>
  <div v-else class="text-center p-8 mt-8">Chargement...</div>
</template>

<style scoped>
.header-section {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
}

.playlist-name-input {
  font-size: 2rem;
  font-weight: 700;
  background: transparent;
  border: none;
  border-bottom: 2px solid transparent;
  color: var(--text-primary);
  outline: none;
  font-family: inherit;
  margin-bottom: 0.25rem;
  transition: border-color var(--transition-fast);
  width: 100%;
}

.playlist-name-input:focus {
  border-color: var(--accent-primary);
}

.actions {
  display: flex;
  gap: 1rem;
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
}

.font-medium { font-weight: 500; color: var(--text-primary); }

.icon-btn {
  background: none;
  border: none;
  color: var(--text-secondary);
  cursor: pointer;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
}

.icon-btn:not(:disabled):hover {
  background: rgba(255,255,255,0.1);
  color: var(--text-primary);
}

.icon-btn:disabled { opacity: 0.3; cursor: not-allowed; }

.text-danger:hover { color: var(--danger); background: rgba(239,68,68,0.1) !important; }

.add-list {
  max-height: 400px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.add-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.5rem;
  background: rgba(0,0,0,0.2);
  border-radius: var(--radius-sm);
}

.add-item .title { font-weight: 500; font-size: 0.9rem; }
.add-item .artist { font-size: 0.8rem; color: var(--text-secondary); }

.empty-state { text-align: center; padding: 2rem; color: var(--text-muted); }
</style>
