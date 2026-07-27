<script setup>
import { getAll, getOne, getTicketCosts, getTicketItems } from '@/services/api';
import { TYPE_MAP , STATUS_MAP , PRIORITY_MAP } from '@/services/glpiTickets';
import { onMounted, ref } from 'vue';
const afficherFiche = ref(false)
const selectedTicket = ref(null)
const listTicket = ref([])
    onMounted(async () =>{
        listTicket.value = await getAll('Assistance/Ticket')
        console.log('LISTE TICKET TAILLE :'+listTicket.value.length);
        
    })
     const TYPE_NAME = Object.fromEntries(
    Object.entries(TYPE_MAP).map(([name, id]) => [id, name])
    
)
    const STATUS_NAME = Object.fromEntries(
        Object.entries(STATUS_MAP).map(([name,id] )=> [id,name])
    )
    const PRIORITY_NAME = Object.fromEntries(
        Object.entries(PRIORITY_MAP).map(([name,id])=>[id,name])
    )
    const itemsTickets = ref([])
    const selected = ref(null)
    const linkedAssets = ref([])

    const costsTickets = ref([])
    const linkedCosts = ref([])
async function   showFiche(t) {
    selectedTicket.value = t 
    afficherFiche.value = true 

    selected.value = t
        itemsTickets.value = await getTicketItems(selected.value.id)
        console.log("TICKET ITEM :"+itemsTickets.value.length)
        console.log("TICKET SELECTED : "+selected.value.id);
        linkedAssets.value = []
        console.log("Linked ASSET Taille :"+itemsTickets.value.length);
        for (const it of itemsTickets.value) {
            const asset = await getOne(`Assets/${it.itemtype}`,it.items_id)
            if (asset) {
                linkedAssets.value.push(asset)
            }
        }
        
        
        
        costsTickets.value = await getTicketCosts(selected.value.id)
        linkedCosts.value =  []


        
}

async function selectTicket(t) {
        
        

        
    }
    
    
</script>
<template>
    <table>
        <table>
            <tr>
                <th>Id</th>
                <th>Titre</th>
                <th>Type</th>
                <th>Voir fiche</th>
            </tr>
            <tr v-for="t in listTicket">
                <td>{{ t.id }}</td>
                <td>{{ t.name }}</td>
                <td>{{ TYPE_NAME[t.type] }}</td>
                <td><button @click="showFiche(t)">Fiche</button></td>
            </tr>
        </table>
    </table>
    <table v-if="afficherFiche" >
        <tr>
            <th>Titre</th>
            <th>Description</th>
            <th>Date</th>
            <th>Priority</th>
            <th>Type</th>
            <th>Status</th>
        </tr>
        <tr>
            <td>{{ selectedTicket.name }}</td>
            <td>{{ selectedTicket.content }}</td>
            <td>{{ selectedTicket.date }}</td>
            <td>{{ PRIORITY_NAME[selectedTicket.priority] }}</td>
            <td>{{ TYPE_NAME[selectedTicket.type] }}</td>
            <td>{{ selectedTicket.status.name }}</td>
        </tr>
    </table>
     <div class="fiche" v-if="afficherFiche">
           
            <h2>Items Liés</h2>
            <table v-if="linkedAssets.length>0">
                 <tr>
            <th>Nom </th>
            <th>Status</th>
            <th>Otherserial</th>
            <th>Location</th>
            <th>Manufacturer</th>
            <th>Model</th>
            <th>User</th>
        </tr>
        <tr v-for="all1 in linkedAssets">
            <td>{{all1.name}}</td>
            <td>{{ all1.status?.name }}</td>
            <td>{{ all1.otherserial }}</td>
            <td>{{ all1.location?.name }}</td>
            <td>{{ all1.manufacturer?.name }}</td>
            <td>{{ all1.model?.name }}</td>
            <td>{{ all1.user?.name }}</td>
        </tr>
            </table>

            <h2>Costs Liés</h2>
            <table v-if="costsTickets.length>0">
                 <tr>
            <th>Nom </th>
            <th>Cotetime</th>
            <th>Duration</th>
            <th>Cost fixed</th>
           
        </tr>
        <tr v-for="all1 in costsTickets">
            <td>{{all1.name}}</td>
            <td>{{ all1.cost_time}}</td>
            <td>{{ all1.duration }}</td>
            <td>{{ all1.cost_fixed}}</td>
            
        </tr>
            </table>
            
        </div>
</template>