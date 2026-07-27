<script setup>
    import SideBar from '@/components/SideBar.vue'
import { getAll } from '@/services/api';
import { computed, onMounted, ref } from 'vue';
// const itemType =[ 'Assets/Computer', 'Assets/Monitor' , 'Assets/Phone']
const computers = ref([]) 
const monitor = ref([]) 
const phone = ref([]) 
const all = ref([]) 

const search = ref({
    name : '' , status : '' , location : '' , manufacturer : '' , model : '' , user : ''
})

onMounted(async ()=> {
  computers.value = await getAll('Assets/Computer')  
  monitor.value = await getAll('Assets/Monitor')  
  phone.value = await getAll('Assets/Phone')
  all.value = [...computers.value , ...monitor.value , ...phone.value]
    console.log("ALL TAILLE :"+all.value.length)
  
})
function match(val,crit) {
    return (val||'').toLowerCase().includes(crit.toLowerCase())
}
const filtered = computed(()=>{
    return all.value.filter(item=>
        match(item.name , search.value.name)
        &&match(item.status?.name, search.value.status)
        &&match(item.location?.name,search.value.location)
        &&match(item.manufacturer?.name , search.value.manufacturer)
        &&match(item.model?.name, search.value.model)
        &&match(item.user?.name , search.value.user)
    )
})
console.log("SEARCH NAME :"+search.value.name)

</script>
<template>
<div>
    <input v-model="search.name" placeholder="Name">
    <input v-model="search.status" placeholder="Status">
    <input v-model="search.location" placeholder="Location">
    <input v-model="search.manufacturer" placeholder="Manufacturer">
    <input v-model="search.model" placeholder="model">
    <input v-model="search.user" placeholder="User">
</div>
<table>
        <tr>
            <th>Nom </th>
            <th>Status</th>
            <th>Otherserial</th>
            <th>Location</th>

            <th>Manufacturer</th>
            <th>Model</th>
            <th>User</th>
        </tr>
        <tr v-for="all1 in filtered">
            <td>{{ all1.name }}</td>
            <td>{{ all1.status.name }}</td>
            <td>{{ all1.otherserial }}</td>
            <td>{{ all1.location.name }}</td>
            <td>{{ all1.manufacturer.name }}</td>
            <td>{{ all1.model.name }}</td>
            <td>{{ all1.user?.name }}</td>
        </tr>
    </table>
</template>

<style scoped>
div { display: flex; gap: .3rem; flex-wrap: wrap; margin-bottom: .5rem; }
div input { flex: 1; min-width: 100px; }
</style>