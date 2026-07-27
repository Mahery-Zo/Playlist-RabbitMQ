<script setup>
import { ref, onMounted } from 'vue'
import { NODE_API } from '@/services/api'

const API = `${NODE_API}/api/status-labels`
const labels = ref([])
const message = ref('')

// repère FR (juste pour l'affichage à côté de chaque champ)
const FR = { 1: 'Nouveau', 2: 'Attribué', 3: 'Planifié', 4: 'En attente', 5: 'Résolu', 6: 'Clos' }

onMounted(async () => {
  const data = await (await fetch(API)).json()
  labels.value = data.sort((a, b) => a.statusId - b.statusId)
})

async function save() {
  for (const l of labels.value) {
    await fetch(`${API}/${l.statusId}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ nameMg: l.nameMg })
    })
  }
  message.value = 'Libellés enregistrés ✔'
}
</script>

<template>
  <h2>Libellés des statuts (Malgache)</h2>

  <div v-for="l in labels" :key="l.statusId" style="margin-bottom:.5rem">
    <label>
      {{ FR[l.statusId] }} (id {{ l.statusId }}) :
      <input v-model="l.nameMg">
    </label>
  </div>

  <button @click="save">Enregistrer</button>
  <p v-if="message">{{ message }}</p>
</template>
