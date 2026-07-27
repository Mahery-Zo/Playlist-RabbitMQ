<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { usePlaylistsStore } from '../stores/playlists';
import { usePlayerStore } from '../stores/player';
import PlaylistCard from '../components/PlaylistCard.vue';

const playlistsStore = usePlaylistsStore();
const playerStore = usePlayerStore();
const router = useRouter();

const selectedIds = ref([]);
const mergeMode = ref(false);
const mergeName = ref('');

onMounted(() => {
  playlistsStore.fetchPlaylists();
});

const goToEdit = (id) => {
  if (mergeMode.value) return; // Empêcher la navigation quand on est en mode fusion
  router.push({ name: 'playlist-edit', params: { id } });
};

const handleDelete = async (id) => {
  if (confirm("Voulez-vous vraiment supprimer cette playlist ?")) {
    try {
      await playlistsStore.deletePlaylist(id);
      selectedIds.value = selectedIds.value.filter(sid => sid !== id);
      window.$toast("Playlist supprimée");
    } catch(e) {
      window.$toast("Erreur de suppression", "error");
    }
  }
};

const handlePlay = async (id) => {
  try {
    const data = await playlistsStore.fetchPlaylist(id);
    if (data.songs && data.songs.length > 0) {
      playerStore.playSong(data.songs[0], data.songs);
      window.$toast("Lecture en cours...", "success");
    } else {
      window.$toast("La playlist est vide", "error");
    }
  } catch (e) {
    window.$toast("Impossible de charger la playlist", "error");
  }
};

const handleDownload = (id) => {
  const token = localStorage.getItem('token');
  window.$toast("Préparation du téléchargement...");
  fetch(`/api/playlists/${id}/download`, {
    headers: { 'Authorization': `Bearer ${token}` }
  })
  .then(res => {
    if(!res.ok) throw new Error("Erreur");
    const filename = res.headers.get('content-disposition')?.split('filename=')[1]?.replace(/"/g, '') || `playlist-${id}.zip`;
    return res.blob().then(blob => ({blob, filename}));
  })
  .then(({blob, filename}) => {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    window.URL.revokeObjectURL(url);
    document.body.removeChild(a);
    window.$toast("Téléchargement démarré", "success");
  })
  .catch(() => window.$toast("Erreur de téléchargement", "error"));
};

const toggleSelection = (id) => {
  const idx = selectedIds.value.indexOf(id);
  if (idx > -1) {
    selectedIds.value.splice(idx, 1);
  } else {
    selectedIds.value.push(id);
  }
};

const canMerge = computed(() => selectedIds.value.length >= 2);

const toggleMergeMode = () => {
  mergeMode.value = !mergeMode.value;
  if (!mergeMode.value) {
    selectedIds.value = [];
    mergeName.value = '';
  }
};

const handleMerge = async () => {
  if (!canMerge.value) return;
  const name = mergeName.value.trim() || 'Fusion';
  try {
    await playlistsStore.mergePlaylists(name, selectedIds.value);
    window.$toast(`Playlist "${name}" créée par fusion (sans doublons)`, 'success');
    mergeMode.value = false;
    selectedIds.value = [];
    mergeName.value = '';
  } catch (e) {
    console.error(e);
    window.$toast('Erreur lors de la fusion', 'error');
  }
};
</script>

<template>
  <div class="container">
    <div class="header-section mb-8">
      <div>
        <h1>Mes Playlists</h1>
        <p class="text-secondary">Gérez vos sélections sauvegardées.</p>
      </div>
      <div style="display: flex; gap: 0.75rem; align-items: center;">
        <button class="btn" :class="mergeMode ? 'btn-merge-active' : 'btn-merge'" @click="toggleMergeMode">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18"><path d="M8 6h13"></path><path d="M8 12h13"></path><path d="M8 18h13"></path><path d="M3 6h.01"></path><path d="M3 12h.01"></path><path d="M3 18h.01"></path></svg>
          {{ mergeMode ? 'Annuler' : 'Fusionner' }}
        </button>
        <button class="btn btn-primary" @click="router.push('/generator')">
          Nouvelle Playlist
        </button>
      </div>
    </div>

    <!-- Barre de fusion -->
    <div v-if="mergeMode" class="merge-bar glass-panel mb-4">
      <div class="merge-info">
        <span v-if="selectedIds.length === 0">Cochez au moins 2 playlists à fusionner</span>
        <span v-else>{{ selectedIds.length }} playlist(s) sélectionnée(s)</span>
      </div>
      <div class="merge-actions">
        <input type="text" v-model="mergeName" placeholder="Nom de la fusion..." class="merge-name-input" />
        <button class="btn btn-primary" :disabled="!canMerge" @click="handleMerge">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16"><path d="M17 1l4 4-4 4"></path><path d="M3 11V9a4 4 0 0 1 4-4h14"></path><path d="M7 23l-4-4 4-4"></path><path d="M21 13v2a4 4 0 0 1-4 4H3"></path></svg>
          Fusionner ({{ selectedIds.length }})
        </button>
      </div>
    </div>

    <div v-if="playlistsStore.loading" class="text-center mt-8">Chargement...</div>
    <div v-else-if="playlistsStore.playlists.length === 0" class="text-center text-muted mt-8">
      Vous n'avez pas encore de playlist.
    </div>
    <div class="playlists-grid" v-else>
      <div v-for="p in playlistsStore.playlists" :key="p.id" class="playlist-wrapper" :class="{ 'selected': mergeMode && selectedIds.includes(p.id) }">
        <!-- Checkbox -->
        <div v-if="mergeMode" class="checkbox-overlay" @click.stop="toggleSelection(p.id)">
          <div class="custom-checkbox" :class="{ checked: selectedIds.includes(p.id) }">
            <svg v-if="selectedIds.includes(p.id)" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" width="14" height="14"><path d="M20 6L9 17l-5-5"></path></svg>
          </div>
        </div>
        <PlaylistCard 
          :playlist="p"
          @click="mergeMode ? toggleSelection(p.id) : goToEdit(p.id)"
          @delete="handleDelete"
          @download="handleDownload"
          @play="handlePlay"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.header-section {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
}

.playlists-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 1.5rem;
}

.playlist-wrapper {
  position: relative;
  border-radius: var(--radius-lg, 12px);
  transition: all 0.2s ease;
}

.playlist-wrapper.selected {
  outline: 2px solid var(--accent-primary);
  outline-offset: 2px;
  transform: scale(1.02);
}

.checkbox-overlay {
  position: absolute;
  top: 10px;
  left: 10px;
  z-index: 10;
  cursor: pointer;
}

.custom-checkbox {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  border: 2px solid var(--text-muted);
  background: rgba(0,0,0,0.4);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.custom-checkbox.checked {
  background: var(--accent-primary);
  border-color: var(--accent-primary);
}

.custom-checkbox svg {
  color: white;
}

.merge-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.5rem;
  gap: 1rem;
  flex-wrap: wrap;
}

.merge-info {
  font-size: 0.95rem;
  color: var(--text-secondary);
}

.merge-actions {
  display: flex;
  gap: 0.75rem;
  align-items: center;
}

.merge-name-input {
  padding: 0.5rem 0.75rem;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  background: var(--bg-surface);
  color: var(--text-primary);
  font-size: 0.9rem;
  outline: none;
  width: 200px;
}

.merge-name-input:focus {
  border-color: var(--accent-primary);
}

.btn-merge {
  background: var(--bg-surface);
  border: 1px solid var(--border-color);
  color: var(--text-primary);
}

.btn-merge:hover {
  background: var(--bg-surface-hover);
}

.btn-merge-active {
  background: rgba(239, 68, 68, 0.15);
  border: 1px solid var(--danger);
  color: var(--danger);
}

.btn-merge-active:hover {
  background: rgba(239, 68, 68, 0.25);
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
