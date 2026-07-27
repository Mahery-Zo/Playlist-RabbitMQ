<template>
  <table>
      <tr>
        <th>Categorie</th>
        <th>Cout GLPI</th>
        <th>Super Cout</th>
        <th>Frais de reouverture</th>
        <th>Total</th>
        <th></th>
      </tr>
      <tr v-for="type in Object.keys(coutGlpi)" :key="type">
          <td>
            {{ type }}
          </td>
          <td>
            {{ (coutGlpi[type] || 0).toFixed(3) }}
          </td>
          <td>{{ (coutSqlite[type] || 0).toFixed(3) }}</td>
          <td>{{ (fraisSqlite[type] || 0).toFixed(3) }}</td>
          <td>
            {{ Number((coutGlpi[type] || 0) + (coutSqlite[type] || 0) + (fraisSqlite[type] || 0) )}}
          </td>
          <td>
            <button @click="selected = type"></button>
          </td>
      </tr>
      <div v-if="selected">
        <table>
          <tr>
            <th>Ticket</th>
          <th>Item ID </th>
          <th>Super Cout </th>
          <th>Frais</th>
          </tr>
          <tr v-for=" (d,id) in (details[selected] || [])" :key="id">
            <td>
                {{ d.ticketId }}
            </td>
            <td>
              {{ d.itemId }}
            </td>
            <td>
                {{ d.coutSuper }}
            </td>

            <td>
              {{ d.frais }}
            </td>
            <td>
              {{  }}
            </td>
          </tr>
        </table>
      </div>
  </table>
</template>
<script setup>
import { getAll, getTicketCosts, getTicketItems, NODE_API } from '@/services/api';
import { onMounted, ref } from 'vue';

const coutGlpi = ref([])
const coutSqlite = ref([])
const fraisSqlite = ref([])
  const selected = ref(null)

  const details = ref(null)



onMounted(async()=>{
  const tickets = await getAll('/Assistance/Ticket')
  const glpi = {}
  const sqlite = {}
  const fraisqlite = {}
  const ticketNampiasaina = ref([])

  const detailsTemp = {}

  for (const t of tickets) {
    ticketNampiasaina.value.push(t)
    console.log("TICKET ID : "+t.id);

    let sumcoutsGLPI = 0 ;
    const coutsGLPI =await getTicketCosts(t.id)
    for (const cGLPI of coutsGLPI) {
          sumcoutsGLPI += Number( cGLPI.cost_fixed+ (cGLPI.cost_time * cGLPI.duration/3600))
      //  console.log("SUMM : "+sumcoutsGLPI)
    }

    let sumCoutsSuper = 0
    let sumFraisSuper = 0


     let partSuperSum = 0 ;
    let partFraisSum = 0 ;
    // const resSuperCoutTicket = await fetch(`http://localhost:8083/api/cout/${t.id}/last`)
    // const SuperCoutTicket = await resSuperCoutTicket.json()
    const items = await getTicketItems(t.id);
    let partGLPI = 0 ;
     partGLPI = sumcoutsGLPI/items.length

     for (const it of items) {
          glpi[it.itemtype] = Number((glpi[it.itemtype] || 0) + partGLPI)

     }


     const resFraisSuperTicketMaro =  await fetch(`${NODE_API}/api/frais/${t.id}`)
    const SuperFraisTicketMaro = await resFraisSuperTicketMaro.json()
     for (const SuperFraisTicket of SuperFraisTicketMaro) {
      sumFraisSuper = SuperFraisTicket?.cout || 0
      console.log("FRAIS  TICKET :"+SuperFraisTicket?.cout );

        let partFrais = 0 ;

          partFrais = Number(sumFraisSuper/items.length)
          // console.log("FRAIS PART : "+partFrais);

          for (const it of items) {
            fraisqlite[it.itemtype] = Number((fraisqlite[it.itemtype] || 0 )+ partFrais)
            partFraisSum += partFrais

          }

     }

    const resSuperCoutsTicket =  await fetch(`${NODE_API}/api/cout/${t.id}`)
    const SuperCoutsTicket = await resSuperCoutsTicket.json()
    for (const SuperCoutTicket of SuperCoutsTicket) {
        console.log("SUPER TICKET LENGHT : "+SuperCoutsTicket.length);
    // console.log("SUPER COUT :"+SuperCoutTicket.cout);
    sumCoutsSuper = SuperCoutTicket?.cout || 0


    let partSuper = 0 ;
    // console.log("Ticket id : "+t.id+" ITEMS LENGHT :"+items.length);
    partSuper = sumCoutsSuper/items.length
    // console.log("PART GLPI :"+partGLPI);
    // console.log("PART Super :"+partSuper);
    for (const it of items) {
      sqlite[it.itemtype] = Number((sqlite[it.itemtype] || 0) + partSuper)
      partSuperSum += partSuper
    }
    // console.log("PART FRAIS : " + fraisqlite['Computer']);
    }

    for (const it of items) {


      if (!detailsTemp[it.itemtype]) {
        detailsTemp[it.itemtype] = []

      }
         detailsTemp[it.itemtype].push({
        ticketId : t.id ,
        itemId : it.items_id ,
        coutSuper : partSuperSum/items.length,
        frais : partFraisSum/items.length ,

      })
    }



  }





      fraisSqlite.value = fraisqlite
      coutGlpi.value = glpi
      coutSqlite.value = sqlite
      details.value = detailsTemp
})


</script>
