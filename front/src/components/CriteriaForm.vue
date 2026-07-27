<script setup>
import { ref } from 'vue';

const props = defineProps({
  availableGenres: Array,
  availableArtists: Array
});

const emit = defineEmits(['generate']);

const targetMinutes = ref(60);
const includeGenres = ref([]);
const excludeGenres = ref([]);
const includeArtists = ref([]);
const excludeArtists = ref([]);

const toggleItem = (list, item) => {
  const idx = list.value.indexOf(item);
  if (idx > -1) list.value.splice(idx, 1);
  else list.value.push(item);
};

const toggleIncludeExclude = (item, type) => {
  let incList = type === 'genre' ? includeGenres : includeArtists;
  let excList = type === 'genre' ? excludeGenres : excludeArtists;

  if (incList.value.includes(item)) {
    incList.value = incList.value.filter(i => i !== item);
    excList.value.push(item);
  } else if (excList.value.includes(item)) {
    excList.value = excList.value.filter(i => i !== item);
  } else {
    incList.value.push(item);
  }
};

const getStatus = (item, type) => {
  let incList = type === 'genre' ? includeGenres : includeArtists;
  let excList = type === 'genre' ? excludeGenres : excludeArtists;
  if (incList.value.includes(item)) return 'include';
  if (excList.value.includes(item)) return 'exclude';
  return 'neutral';
};

const submit = () => {
  emit('generate', {
    targetDurationSec: targetMinutes.value * 60,
    includeGenres: includeGenres.value,
    excludeGenres: excludeGenres.value,
    includeArtists: includeArtists.value,
    excludeArtists: excludeArtists.value
  });
};
</script>

<template>
  <form @submit.prevent="submit" class="criteria-form">
    <div class="form-group">
      <label class="form-label">Durée cible (en minutes) :</label>
      <div style="display: flex; gap: 1rem; align-items: center; margin-top: 0.5rem;">
        <input type="number" v-model.number="targetMinutes" min="1" class="number-input" style="width: 80px; padding: 0.4rem; border-radius: 4px; border: 1px solid var(--border-color); background: var(--bg-surface); color: var(--text-primary);" />
        <input type="range" v-model.number="targetMinutes" min="1" max="300" step="1" class="slider" style="flex: 1;" />
      </div>
    </div>

    <div class="form-group mt-4" v-if="availableGenres?.length">
      <label class="form-label">Genres (clic: inclure, double-clic: exclure)</label>
      <div class="chips-container">
        <button type="button" 
                v-for="g in availableGenres" :key="g"
                class="chip" 
                :class="'chip-' + getStatus(g, 'genre')"
                @click="toggleIncludeExclude(g, 'genre')">
          <span class="icon" v-if="getStatus(g, 'genre') === 'include'">+</span>
          <span class="icon" v-if="getStatus(g, 'genre') === 'exclude'">-</span>
          {{ g }}
        </button>
      </div>
    </div>

    <div class="form-group mt-4" v-if="availableArtists?.length">
      <label class="form-label">Artistes</label>
      <div class="chips-container">
        <button type="button" 
                v-for="a in availableArtists" :key="a"
                class="chip" 
                :class="'chip-' + getStatus(a, 'artist')"
                @click="toggleIncludeExclude(a, 'artist')">
          <span class="icon" v-if="getStatus(a, 'artist') === 'include'">+</span>
          <span class="icon" v-if="getStatus(a, 'artist') === 'exclude'">-</span>
          {{ a }}
        </button>
      </div>
    </div>

    <button type="submit" class="btn btn-primary w-full mt-8">Générer la playlist</button>
  </form>
</template>

<style scoped>
.slider {
  -webkit-appearance: none;
  width: 100%;
  height: 6px;
  border-radius: 3px;
  background: var(--border-color);
  outline: none;
}
.slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--accent-primary);
  cursor: pointer;
  box-shadow: 0 0 10px rgba(168,85,247,0.5);
}

.chips-container {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.chip {
  background: var(--bg-surface);
  border: 1px solid var(--border-light);
  color: var(--text-secondary);
  padding: 0.4rem 0.8rem;
  border-radius: var(--radius-full);
  cursor: pointer;
  font-size: 0.85rem;
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  transition: all var(--transition-fast);
}

.chip:hover {
  background: var(--bg-surface-hover);
}

.chip-include {
  background: rgba(168,85,247,0.2);
  border-color: var(--accent-primary);
  color: white;
}

.chip-exclude {
  background: rgba(239,68,68,0.2);
  border-color: var(--danger);
  color: white;
}

.icon {
  font-weight: bold;
}

.w-full {
  width: 100%;
}
</style>
