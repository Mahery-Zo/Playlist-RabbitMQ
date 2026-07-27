<script setup>
import { ref, onMounted } from 'vue';
import { useSongsStore } from '../stores/songs';
import SongTable from '../components/SongTable.vue';
import SongFormModal from '../components/SongFormModal.vue';

const songsStore = useSongsStore();
const searchQuery = ref('');

const showModal = ref(false);
const editingSong = ref(null);

onMounted(() => {
  songsStore.fetchSongs();
});

const handleSearch = () => {
  songsStore.fetchSongs(searchQuery.value);
};

const openAddModal = () => {
  editingSong.value = null;
  showModal.value = true;
};

const openEditModal = (song) => {
  editingSong.value = song;
  showModal.value = true;
};

const handleDelete = async (song) => {
  if (confirm(`Supprimer définitivement "${song.title || song.originalName}" ?`)) {
    try {
      await songsStore.deleteSong(song.id);
      window.$toast('Chanson supprimée');
    } catch (e) {
      window.$toast('Erreur de suppression', 'error');
    }
  }
};

const handleSaveModal = async ({ form, file }) => {
  try {
    if (editingSong.value) {
      await songsStore.updateSong(editingSong.value.id, form);
      window.$toast('Chanson modifiée');
    } else {
      await songsStore.uploadSong(file, form);
      window.$toast('Chanson ajoutée', 'success');
    }
    showModal.value = false;
  } catch (e) {
    window.$toast('Erreur lors de l\'enregistrement', 'error');
  }
};

const handleResetAll = async () => {
  if (confirm('⚠️ ATTENTION : Voulez-vous vraiment effacer TOUTES les musiques, playlists, et vider le répertoire source ? Cette action est irréversible.')) {
    try {
      window.$toast('Effacement en cours...', 'info');
      const api = (await import('../api/axios')).default;
      const res = await api.delete('/admin/reset');
      window.$toast(res.data.message || 'Tout a été effacé avec succès', 'success');
      songsStore.fetchSongs(); // Recharger la liste vide
    } catch (e) {
      console.error(e);
      window.$toast('Erreur lors de la remise à zéro', 'error');
    }
  }
};
</script>

<template>
  <div class="container">
    <div class="header-section mb-8">
      <div>
        <h1>Bibliothèque</h1>
        <p class="text-secondary">Gérez votre collection musicale locale.</p>
      </div>
      <div style="display: flex; gap: 1rem;">
        <button class="btn" style="background-color: var(--danger); color: white; border: none;" @click="handleResetAll">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18"><path d="M3 6h18"></path><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path></svg>
          Tout effacer
        </button>
        <button class="btn btn-primary" @click="openAddModal">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
          Ajouter
        </button>
      </div>
    </div>

    <div class="toolbar mb-4">
      <div class="search-box">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
        <input type="text" v-model="searchQuery" @keyup.enter="handleSearch" placeholder="Rechercher titre, artiste..." class="search-input" />
      </div>
    </div>

    <div v-if="songsStore.loading" class="text-center mt-8">Chargement...</div>
    <SongTable 
      v-else
      :songs="songsStore.songs" 
      action-icon="edit"
      @action="openEditModal" 
      @remove="handleDelete" 
    />

    <SongFormModal 
      :show="showModal" 
      :song="editingSong" 
      @close="showModal = false"
      @save="handleSaveModal"
    />
  </div>
</template>

<style scoped>
.header-section {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
}

.toolbar {
  display: flex;
  gap: 1rem;
}

.search-box {
  display: flex;
  align-items: center;
  background: var(--bg-surface);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-full);
  padding: 0.5rem 1rem;
  flex: 1;
  max-width: 400px;
}

.search-box svg {
  color: var(--text-muted);
  margin-right: 0.5rem;
}

.search-input {
  background: none;
  border: none;
  color: var(--text-primary);
  width: 100%;
  outline: none;
}
</style>
