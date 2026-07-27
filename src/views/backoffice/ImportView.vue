<script setup>

  import { importAssets } from '@/services/glpiAssets';
import { importCosts } from '@/services/glpiCosts';
// import { linkDocumentItem } from '@/services/glpiHelpers';
import { importImages } from '@/services/glpiImages';
import { importTickets  } from '@/services/glpiTickets';


import { ref } from 'vue';
  const csvAssets = ref(null)
  const csvTickets = ref(null)
  const csvCosts = ref(null)
  const zipImages = ref(null)
  const status = ref('')

  function handleAssets(e){
    csvAssets.value = e.target.files[0]
  }
  function handleTickets(e) {
    csvTickets.value = e.target.files[0]
  }
  function handleCosts(e) {
    csvCosts.value = e.target.files[0]
  }
  function handleZip(e) {
    zipImages.value = e.target.files[0]
  }
  async function handleImport() {
    if (!csvAssets.value )  {
      status.value = 'Veuiller Selectionner les 4 fichiers'
      return
    }
    status.value = 'Import des assets ...'
    const assetMap = await importAssets(await csvAssets.value.text())

    status.value = 'Import des Tickets ...'
    const ticketMap = await importTickets(await csvTickets.value.text(),assetMap )

    status.value = 'Import des coûts ...'
    await importCosts(await csvCosts.value.text(), ticketMap)

    status.value = 'Import des images ...'
    await importImages(zipImages.value, assetMap)    
    
    
    status.value = 'Import Terminé .'

  }


</script>
<template>
  <h3>
    1. Assets (Computer , monitors ,..)
  </h3>
  <input type="file" accept=".csv" @change="handleAssets">
  <h3>
    2.Tickets 
  </h3> 
  <input type="file" accept=".csv" @change="handleTickets">
  <h3>Costs :</h3>
  <input type="file" accept=".csv" @change="handleCosts">
  <h3>Images :</h3>
  <input type="file" accept=".zip" @change="handleZip">
  <button @click="handleImport">Importer</button>
  <p v-if="status">{{ status }}</p>
</template>

<style scoped>
input[type="file"] { display: block; margin: .3rem 0 .8rem; }
button { margin-top: .5rem; }
</style>