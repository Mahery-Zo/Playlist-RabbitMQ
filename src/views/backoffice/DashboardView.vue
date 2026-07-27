<script setup>
import SideBar from '@/components/SideBar.vue'
import { getAll } from '@/services/api';
import { computed, onMounted, ref } from 'vue';
// const itemType =[ 'Assets/Computer', 'Assets/Monitor' , 'Assets/Phone']
const computers = ref([]) 
const monitor = ref([]) 
const phone = ref([]) 
const all = ref([]) 
const allticket = ref([])


onMounted(async ()=> {
  computers.value = await getAll('Assets/Computer')  
  monitor.value = await getAll('Assets/Monitor')  
  phone.value = await getAll('Assets/Phone')
  all.value = [...computers.value , ...monitor.value , ...phone.value]
  allticket.value =  await getAll('Assistance/Ticket')
  
})
const incident =  computed(()=>allticket.value.filter(t=>t.type === 1))
const demande = computed(()=>allticket.value.filter(t=>t.type === 2))
</script>

<template>
  <div style="display: flex">
   
    <main style="flex: 1">
      <h1>Tableau de Bord</h1>
      <h2>Elements : </h2>
      <!-- ton nouveau contenu de dashboard ici -->

      <p id="elementGle">Element Generale : {{ all.length }}</p>   
 
      <p id="computer">Type Computer :{{ computers.length }}</p>   
     
      <p id="monitor">Type Monitor  : {{ monitor.length }}</p>   

      <p id="phone">Type Phone  :{{ phone.length }}</p>   
      <h2>Tickets :</h2>
      
      <p id="ticketGle">Ticket Generale : {{ allticket.length }}</p>
   
      <p id="ticketIncident">Incident : {{ incident.length }}</p>
      
      <p id="ticketDemande">Demande : {{ demande.length }}</p>
    </main>
  </div>
</template>

<style scoped>
main { padding: .5rem; }
p { padding: .2rem .4rem; }
</style>