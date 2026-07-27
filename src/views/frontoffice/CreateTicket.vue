<script setup>
import { CreateObj, getAll, linkItemToTicket } from '@/services/api.js';
import { PRIORITY_MAP, STATUS_MAP, TYPE_MAP } from '@/services/glpiTickets';
import { onMounted, ref } from 'vue';

const computers = ref([]) 
const monitor = ref([]) 
const phone = ref([]) 
const all = ref([]) 
const linkCheck = ref(false)
const assets = ref([])
const seletedAssets = ref([])


onMounted(async ()=> {
  computers.value = await getAll('Assets/Computer')  
  monitor.value = await getAll('Assets/Monitor')  
  phone.value = await getAll('Assets/Phone')
  all.value = [...computers.value , ...monitor.value , ...phone.value]
    console.log("ALL TAILLE :"+all.value.length)
assets.value = [
    ...computers.value.map(a=>({...a , itemtype:'Computer'})),
    ...monitor.value.map(a=>({...a , itemtype : 'Monitor'})),
    ...phone.value.map(a=>({...a,itemtype : 'Phone'}))
]
})

function showLink() {
    linkCheck .value = true
}

const event = ref('')
   
    const form = ref({
        status : 0  ,
        priority : 0 ,
        type : 0 ,
        name : '',
        content : ''
});
 const TYPE_NAME = Object.fromEntries(
    Object.entries(TYPE_MAP).map(([name, id]) => [id, name])
    
)
    const STATUS_NAME = Object.fromEntries(
        Object.entries(STATUS_MAP).map(([name,id] )=> [id,name])
    )
    const PRIORITY_NAME = Object.fromEntries(
        Object.entries(PRIORITY_MAP).map(([name,id])=>[id,name])
    )
    async function handleForm() {
        event.value = 'Creation'
        const result = await CreateObj('Assistance/Ticket',form.value)
        for (const asset of seletedAssets.value) {
            await linkItemToTicket(result.id,asset.itemtype,asset.id)
        }
        alert('Ticket Créé :'+result.id)
    }
    

</script>
<template>
    <label for="title">Titre : </label><span><input type="text" id="title" v-model="form.name" ></span>
    <label for="content">Content : </label><span><textarea  id="content" v-model="form.content"></textarea></span>
    <label for="type">Type</label><select  id="type" v-model="form.type">
        <option  v-for="(name,id) in TYPE_NAME" :key="id" :value="id">{{ name }}</option>
    </select>
    <label for="status">Status :</label><select  id="status" v-model="form.status">
        <option v-for="(name,id) in STATUS_NAME" :key="id" :value="id">{{ name }}</option>
    </select>
    <label for="status">Priority :</label><select  id="priority" v-model="form.priority">
        <option v-for="(name,id) in PRIORITY_NAME" :key="id" :value="id">{{ name }}</option>
    </select>
    <p v-if="event">{{ event }}</p>
    <button @click="handleForm()">Create Ticket</button>
    <button @click="showLink">Associé plusieur element</button>

    <table v-if="linkCheck">
        <tr>
            <th>Nom </th>
            <th>Status</th>
            <th>Otherserial</th>
            <th>Location</th>

            <th>Manufacturer</th>
            <th>Model</th>
            <th>User</th>
        </tr>
        <tr v-for="all1 in assets">
            <td>{{ all1.name }}</td>
            <td>{{ all1.status.name }}</td>
            <td>{{ all1.otherserial }}</td>
            <td>{{ all1.location.name }}</td>
            <td>{{ all1.manufacturer.name }}</td>
            <td>{{ all1.model.name }}</td>
            <td>{{ all1.user?.name }}</td>
            <td><input type="checkbox"  id="" :value="all1" v-model="seletedAssets"></td>
        </tr>
    </table> 

</template>

<style scoped>
label { display: block; margin-top: .4rem; }
input[type="text"], textarea, select { display: block; width: 100%; max-width: 400px; margin: .2rem 0 .3rem; }
button { margin-top: .5rem; }
</style>