<script setup>
import { ref, watch } from 'vue';
import Modal from './Modal.vue';

const props = defineProps({
  show: Boolean,
  song: { type: Object, default: null }
});

const emit = defineEmits(['close', 'save']);

const form = ref({
  title: '',
  artist: '',
  album: '',
  genre: '',
  year: '',
});
const file = ref(null);

watch(() => props.song, (newVal) => {
  if (newVal) {
    form.value = { ...newVal };
    file.value = null;
  } else {
    form.value = { title: '', artist: '', album: '', genre: '', year: '' };
    file.value = null;
  }
}, { immediate: true });

const handleFile = (e) => {
  file.value = e.target.files[0];
};

const submit = () => {
  emit('save', { form: form.value, file: file.value });
};
</script>

<template>
  <Modal :show="show" :title="song ? 'Modifier chanson' : 'Ajouter chanson'" @close="emit('close')">
    <form @submit.prevent="submit">
      <div class="form-group" v-if="!song">
        <label class="form-label">Fichier MP3</label>
        <input type="file" accept="audio/mpeg" class="form-control" @change="handleFile" required />
      </div>

      <div class="form-group">
        <label class="form-label">Titre</label>
        <input type="text" v-model="form.title" class="form-control" placeholder="Titre de la chanson" />
      </div>

      <div class="form-group">
        <label class="form-label">Artiste</label>
        <input type="text" v-model="form.artist" class="form-control" placeholder="Nom de l'artiste" />
      </div>
      
      <div class="form-group">
        <label class="form-label">Album</label>
        <input type="text" v-model="form.album" class="form-control" placeholder="Nom de l'album" />
      </div>

      <div style="display: flex; gap: 1rem;">
        <div class="form-group" style="flex: 1;">
          <label class="form-label">Genre</label>
          <input type="text" v-model="form.genre" class="form-control" placeholder="Ex: Rock" />
        </div>
        
        <div class="form-group" style="width: 100px;">
          <label class="form-label">Année</label>
          <input type="number" v-model="form.year" class="form-control" placeholder="YYYY" />
        </div>
      </div>

      <div style="display: flex; justify-content: flex-end; gap: 1rem; margin-top: 1rem;">
        <button type="button" class="btn btn-secondary" @click="emit('close')">Annuler</button>
        <button type="submit" class="btn btn-primary">Sauvegarder</button>
      </div>
    </form>
  </Modal>
</template>
