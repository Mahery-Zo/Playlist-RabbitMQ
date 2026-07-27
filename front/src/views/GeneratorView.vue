<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useSongsStore } from '../stores/songs';
import { usePlaylistsStore } from '../stores/playlists';
import CriteriaForm from '../components/CriteriaForm.vue';
import SongTable from '../components/SongTable.vue';

const songsStore = useSongsStore();
const playlistsStore = usePlaylistsStore();
const router = useRouter();

const generated = ref(false);
const playlistName = ref('');

onMounted(() => {
  songsStore.fetchGenresAndArtists();
});

const handleGenerate = async (criteria) => {
  await playlistsStore.generatePlaylist(criteria);
  generated.value = true;
  playlistName.value = 'Ma Nouvelle Playlist';
  window.$toast('Playlist générée !');
};

const removeSong = (song, index) => {
  playlistsStore.generatedSongs.splice(index, 1);
};

const savePlaylist = async () => {
  if (!playlistName.value.trim()) {
    window.$toast('Le nom est requis', 'error');
    return;
  }
  const songIds = playlistsStore.generatedSongs.map(s => s.id);
  if (songIds.length === 0) {
    window.$toast('La playlist est vide', 'error');
    return;
  }
  try {
    await playlistsStore.savePlaylist(playlistName.value, songIds);
    window.$toast('Playlist sauvegardée', 'success');
    router.push({ name: 'playlists' });
  } catch(e) {
    window.$toast('Erreur lors de la sauvegarde', 'error');
  }
};
</script>

<template>
  <div class="container">
    <div class="mb-8">
      <h1>Générateur de Playlist</h1>
      <p class="text-secondary">Définissez vos critères et laissez la magie opérer.</p>
    </div>

    <div class="generator-layout">
      <!-- Formulaire -->
      <div class="criteria-side">
        <div class="glass-card p-6">
          <CriteriaForm 
            :available-genres="songsStore.genres"
            :available-artists="songsStore.artists"
            @generate="handleGenerate"
          />
        </div>
      </div>

      <!-- Résultat -->
      <div class="result-side" v-if="generated">
        <div class="flex justify-between items-center mb-4">
          <input type="text" v-model="playlistName" class="playlist-name-input" placeholder="Nom de la playlist" />
          <button class="btn btn-primary" @click="savePlaylist">Sauvegarder</button>
        </div>

        <div v-if="playlistsStore.loading" class="text-center p-8">Génération en cours...</div>
        <SongTable 
          v-else
          :songs="playlistsStore.generatedSongs" 
          :playlist-queue="playlistsStore.generatedSongs"
          action-icon="remove"
          @remove="removeSong" 
        />
      </div>
      <div class="result-side empty-state" v-else>
        <div class="text-muted text-center p-8">Remplissez les critères pour générer une playlist.</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.generator-layout {
  display: flex;
  gap: 2rem;
}

.criteria-side {
  flex: 0 0 350px;
}

.result-side {
  flex: 1;
  min-width: 0; /* evite overflow flex */
}

.p-6 {
  padding: 1.5rem;
}
.p-8 {
  padding: 2rem;
}

.playlist-name-input {
  font-size: 1.5rem;
  font-weight: 600;
  background: transparent;
  border: none;
  border-bottom: 2px solid var(--border-color);
  color: var(--text-primary);
  outline: none;
  padding: 0.2rem 0;
  transition: border-color var(--transition-fast);
  font-family: inherit;
  width: 50%;
}

.playlist-name-input:focus {
  border-color: var(--accent-primary);
}

@media (max-width: 900px) {
  .generator-layout {
    flex-direction: column;
  }
  .criteria-side {
    flex: auto;
  }
}
</style>
