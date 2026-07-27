<script setup>
import { ref, onMounted } from 'vue'
import { NODE_API } from '@/services/api'

const API = `${NODE_API}/api/kanban-colors`
const colors = ref({ colNouveau: '#d6e9ff', colProgress: '#ffe6c7', colTermine: '#d3f4dd' })
const message = ref('')

onMounted(async () => {
  colors.value = await (await fetch(API)).json()
})

async function save() {
  await fetch(API, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(colors.value)
  })
  message.value = 'Couleurs enregistrées ✔'
}
</script>

<template>
  <h2>Configuration des couleurs du Kanban</h2>

  <div style="display: flex; flex-direction: column; gap: .75rem; max-width: 300px">
    <label>
      Nouveau
      <input type="color" v-model="colors.colNouveau">
    </label>
    <label>
      In Progress
      <input type="color" v-model="colors.colProgress">
    </label>
    <label>
      Terminé
      <input type="color" v-model="colors.colTermine">
    </label>

    <button @click="save">Enregistrer</button>
    <p v-if="message">{{ message }}</p>
  </div>

  <h3>Aperçu</h3>
  <div style="display: flex; gap: 1rem">
    <div :style="{ background: colors.colNouveau, padding: '1rem', borderRadius: '8px', flex: 1 }">Nouveau</div>
    <div :style="{ background: colors.colProgress, padding: '1rem', borderRadius: '8px', flex: 1 }">In Progress</div>
    <div :style="{ background: colors.colTermine, padding: '1rem', borderRadius: '8px', flex: 1 }">Terminé</div>
  </div>
</template>
