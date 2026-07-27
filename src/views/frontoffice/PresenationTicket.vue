<script setup>
import { addSolution, assignTechnician, getAll, getOne, getTicketAssignees, getTicketItems, getTicketSolutions, updateTicketStatus, NODE_API } from '@/services/api';
import { PRIORITY_MAP, STATUS_MAP, TYPE_MAP } from '@/services/glpiTickets';
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
    const router = useRouter()
    const tickets = ref([])
    const afficherFiche = ref(false)
    const itemsTickets = ref([])
    const selected = ref(null)
    const linkedAssets = ref([])

    const dragged = ref(null)
    const showAssignDialog = ref(false)
    const ticketToAssign = ref(null)
    const technicians = ref([])
    const selectedtech = ref(null)


    const showSolutiondialog = ref(false)
    const ticketToclose = ref(null)
    const solutionContent = ref('')
    const closeIntent = ref(false)

    const STATUS_MG = ref({})


    const colors = ref({ colNouveau: '#d6e9ff', colProgress: '#ffe6c7', colTermine: '#d3f4dd' })




    const ticketToNew = ref(null)
    onMounted(async ()=>{
        tickets.value = await getAll('Assistance/Ticket')
        console.log("Nouveau : "+nouveau.value.length);
        technicians.value = await getAll('Administration/User')
        colors.value = await (await fetch(`${NODE_API}/api/kanban-colors`)).json()
        const list = await (await fetch(`${NODE_API}/api/status-labels`)).json()
        STATUS_MG.value = Object.fromEntries(list.map(s=>[s.statusId,s.nameMg]))

    })

    function onDragStart(t) {
        dragged.value = t
    }
    const cout = ref(0)
    const showDialogCout = ref(false);
    const showDialogReouverture = ref(false)
    async function onDrop(targetStatus) {
        const t = dragged.value
        dragged.value = null

        if(!t) return
        const sourceStatus = Number(t.status?.id)
        if (sourceStatus === 6 && targetStatus === 2 ) {
          ticketToAssign.value =t
          showDialogReouverture.value= true
        }

        if (targetStatus === 2) {
            // const assignees = await getTicketAssignees(t.id)
            // if (assignees.length === 0) {
            //     ticketToAssign.value = t
            //     showAssignDialog.value = true
            //     return
            // }
            // else {
            //     ticketToAssign.value = t
            //     await confirmAssignTsotra()
            // }
            ticketToAssign.value = t
            confirmAssignTsotra()
        }
        if (targetStatus === 6) {
            // const assignees = await getTicketAssignees(t.id)
            // if (assignees.length===0) {
            //     closeIntent.value = true
            //     ticketToAssign.value = t
            //     showAssignDialog.value = true
            //     await proceedClose(t)
            //     return
            // }

            // const solutions = await getTicketSolutions(t.id)
            // if (solutions.length === 0 ) {
            //     ticketToclose.value = t
            //     solutionContent.value = ''
            //     showSolutiondialog.value = true
            //     return
            // }
            // else{
            //     ticketToclose.value = t
            //     confirmCloseTsotra()
            // }

            showDialogCout.value = true
            ticketToclose.value = t
            confirmCloseTsotra()

        }
        if (targetStatus === 1) {
            ticketToNew.value = t
            console.log("NOUVEAUUUU : "+dragged.value)

            await applyStatus(ticketToNew.value,1)
        }

    }
    const pourcentageFraisReouverture = ref(0)
    const fraisReouverture = ref(0)
    const model = ref(0)
    const mode = ref(0)

    async function Reouvrir() {
      console.log("MODEL : "+mode.value);

      const CoutRef = ref(0)
      if (mode.value == 1) {
          const resLastCout = await fetch(`${NODE_API}/api/cout/${ticketToAssign.value.id}/last`)
           const LastCout = await resLastCout.json()
           CoutRef.value = LastCout.cout
      }
      else if (mode.value == 2) {
        const resLastCout = await fetch(`${NODE_API}/api/cout/${ticketToAssign.value.id}/first`)
        const LastCout = await resLastCout.json()
        console.log("LAST COUT  TEST : "+Number(LastCout.cout+1));

        CoutRef.value = Number(LastCout.cout)

      }
      else if (mode.value == 4 ) {
        const resLastCout = await fetch(`${NODE_API}/api/cout/${ticketToAssign.value.id}`)

        const LastCout = await resLastCout.json()
        for (const c of LastCout) {
          CoutRef.value += c.cout
        }
      }
      else if (mode.value == 3) {
        const resLastCout = await fetch(`${NODE_API}/api/cout/${ticketToAssign.value.id}`)
        const LastCout = await resLastCout.json()
        let sum = 0
        for (const c of LastCout) {
          sum += c.cout
        }
        CoutRef.value = sum/LastCout.length
      }

      console.log("COUT REF :"+CoutRef.value);



      fraisReouverture.value =Number( pourcentageFraisReouverture.value * CoutRef.value /100)
       console.log("FRAIS  REOUVERTURE : "+fraisReouverture.value);

      await fetch(`${NODE_API}/api/frais`,{
        method : 'POST',
        headers : {'content-type' : 'application/json'},
        body : JSON.stringify({cout:Number(fraisReouverture.value),idTicket : ticketToAssign.value.id})
      })
      showDialogReouverture.value = false
    }
    async function Annulation() {
      const resLastCout = await fetch(`${NODE_API}/api/cout/${ticketToAssign.value.id}/last`)
      const LastCout = await resLastCout.json()
      console.log("LAST COUT : "+LastCout.id);

      await fetch(`${NODE_API}/api/cout/${LastCout.id}`,{
        method : 'DELETE',

      })
    }
    async function ValiderCout() {
      console.log('COUT :'+cout.value)
      await fetch(`${NODE_API}/api/cout`,{
        method : 'POST',
        headers : {'content-type' : 'application/json'},
        body : JSON.stringify({cout:Number(cout.value),idTicket : ticketToclose.value.id})
      })
      showDialogCout.value = false
    }
    async function applyStatus(t,statusId) {
        await  updateTicketStatus(t.id,statusId)
        t.status = {id : statusId}
    }



    async function confirmAssign() {
        await assignTechnician(ticketToAssign.value.id,Number(selectedtech.value))
        await applyStatus(ticketToAssign.value,2)
        showAssignDialog.value = false
        selectedtech.value = null
        if (closeIntent.value) {
            closeIntent.value = false
            await proceedClose(ticketToAssign.value)
        }
    }
    async function confirmAssignTsotra() {

        await applyStatus(ticketToAssign.value,2)
        showAssignDialog.value = false
        selectedtech.value = null
    }

    async function confirmClose() {
        if (!solutionContent.value.trim()) {

            return
        }

        console.log("TSY CONFIRME");
        await addSolution(ticketToclose.value.id,solutionContent.value)
        await applyStatus(ticketToclose.value,6)
        showSolutiondialog.value = false


    }

    async function proceedClose(t) {
        const solution = await getTicketSolutions(t.id)
        if (solution.length === 0) {
            ticketToclose.value = t
            solutionContent.value = ''
            showSolutiondialog.value = true
            return
        }
        await applyStatus(t,6)
    }


    async function confirmCloseTsotra() {
        await applyStatus(ticketToclose.value,6)
        showSolutiondialog.value = false
    }

    async function selectTicket(t) {
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



    }

    const nouveau = computed (()=> tickets.value.filter(t => t.status?.id == 1))
    const InProgress = computed(()=>tickets.value.filter(t => [2].includes(t.status?.id)))
    const termine = computed(()=>tickets.value.filter(t=> [6].includes(t.status?.id)))
    function handleAjout() {

        console.log("TESTTT")
        router.push('/createTicket')

    }
    const langueGasy = ref(false)

    function changelange() {
        langueGasy.value = !langueGasy.value
        changeLangeLable()
    }
    const langueLabel = ref('')
    function changeLangeLable(){
        if (langueGasy.value) {
            langueLabel.value = 'GASY'
        }
        else{
            langueLabel.value = 'FR'
        }
    }

    const TYPE_NAME = Object.fromEntries(
    Object.entries(TYPE_MAP).map(([name, id]) => [id, name])

)
    const STATUS_NAME = Object.fromEntries(
        Object.entries(STATUS_MAP).map(([name,id] )=> [id,name])
    )
    const PRIORITY_NAME = Object.fromEntries(
        Object.entries(PRIORITY_MAP).map(([name,id])=>[id,name])
    )


</script>

<template>
    <div class="kanban">
        <div class="column col-nouveau" @dragover.prevent @drop="onDrop(1)" :style="{background: colors.colNouveau}">
            <div class="col-header">
                <span class="col-title">
                     <p v-if="langueGasy">
                        {{ STATUS_MG[1] }}
                    </p>
                    <p v-else>
                        Nouveau
                    </p>
                </span>
                <span class="badge">
                    {{ nouveau.length }}
                </span>
            </div>
            <div class="card" draggable="true" @dragstart="onDragStart(n)" v-for="n in nouveau" :key="n.id" @click="selectTicket(n) ">{{ n.name }} </div>
            <button class="add-btn" @click="handleAjout">+ Ajouter Ticket</button>
        </div>

         <div class="column col-progress" @dragover.prevent @drop="onDrop(2)" :style="{background : colors.colProgress}">
            <div class="col-header">
                <span class="col-title">
                     <p v-if="langueGasy">
                        {{ STATUS_MG[2] }}
                    </p>
                    <p v-else>
                        In Progress
                    </p>
                </span>
                <span class="badge">
                    {{ InProgress.length }}
                </span>
            </div>
            <div class="card" draggable="true" @dragstart="onDragStart(i)" v-for="i in InProgress" :key="i.id" @click="selectTicket(i) ">{{ i.name }} </div>

        </div>

        <div class="column col-termine" @dragover.prevent @drop="onDrop(6)" :style="{background: colors.colTermine}">
            <div class="col-header">
                <span class="col-title">

                    <p v-if="langueGasy">
                        {{ STATUS_MG[6] }}
                    </p>
                    <p v-else>
                        Termine
                    </p>
                </span>
                <span class="badge">
                    {{ termine.length }}
                </span>
            </div>
            <div class="card" draggable="true" @dragstart="onDragStart(t)" v-for="t in termine" :key="t.id" @click="selectTicket(t) ">{{ t.name }}</div>

        </div>

    </div>

    <div v-if="selected" class="fiche-overlay" @click.self="selected=null">
        <div class="fiche">
            <button class="close" @click="selected = null">X</button>
            <h2>{{ selected.name }}</h2>
            <p><b>Statut :</b>{{ selected.status?.name }}</p>
            <p><b>Type : </b>{{ TYPE_NAME[selected.type] }}</p>
            <p><b>PRIORITY : </b>{{ PRIORITY_NAME[selected.priority]  }}</p>
            <p><b>Date : </b>{{ selected.date }}</p>
            <p><b>Decription : </b>{{ selected.content }}</p>

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

        </div>
    </div>

    <div v-if="showAssignDialog" class="fiche-overlay" @click.self="showAssignDialog=false">
        <div class="fiche">
            <h3>Attribuer un techniciens</h3>
            <select v-model="selectedtech">
                <option v-for="tec in technicians"  :key="tec.id" :value="tec.id">
                    {{ tec.username || tec.realname }}
                    {{ tec.firstname || '' }}
                </option>
            </select>
            <button @click="confirmAssign">Attribuer & passer en cours</button>
            <button @click="showAssignDialog = false">Annuler</button>
        </div>
    </div>
    <div v-if="showSolutiondialog">
            <div class="fiche">
                <h3>CLoturer Ticket</h3>
                <p>Saisis la solution</p>
                <textarea v-model="solutionContent" rows="4"></textarea>

            </div>
            <div>
                <button @click="confirmClose">Cloturer</button>
                <button @click="showSolutiondialog = false">Annuler</button>
            </div>
    </div>
    <div v-if="showDialogCout">
        <input type="number" name="" id="" v-model="cout">
        <button @click="ValiderCout">Close</button>
    </div>

    <div v-if="showDialogReouverture">
        <input type="number" name="" id="" v-model="pourcentageFraisReouverture">
        <select v-model="mode" >
          <option :key="1" :value="1">1</option>
          <option :key="2" :value="2">2</option>
          <option :key="3" :value="3">3</option>
          <option :key="4" :value="4">4</option>

        </select>
         <button @click="Annulation">Annuler</button>
        <button @click="Reouvrir">Reouvrir</button>
    </div>

    <button @click="changelange">Changer langue : {{ langueLabel }}</button>

</template>


<style scoped>
.kanban {
  display: flex;
  gap: 1rem;
  align-items: flex-start;
  padding: 1rem;
}
.column {
  flex: 1;
  border-radius: 12px;
  padding: .75rem;
}
/* .col-nouveau  { background: #d6e9ff; }
.col-progress { background: #ffe6c7; }
.col-termine  { background: #d3f4dd; } */

.col-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: .75rem;
}
.col-title { font-weight: bold; font-size: 1.1rem; }
.badge {
  background: rgba(0,0,0,.1);
  border-radius: 999px;
  padding: .1rem .6rem;
  font-size: .85rem;
}
.card {
  background: #fff;
  border-radius: 8px;
  padding: .6rem .8rem;
  margin-bottom: .5rem;
  box-shadow: 0 1px 2px rgba(0,0,0,.1);
}
.add-btn {
  width: 100%;
  background: rgba(255,255,255,.5);
  border: none;
  border-radius: 8px;
  padding: .5rem;
  font-size: .8rem;
  cursor: pointer;
  text-align: left;
  color: #333;
}
.fiche-overlay{
    position :fixed;
    inset : 0 ;
    background: rgba(0, 0, 0, 4);
    display: flex;
    align-items: center;
    justify-content: center;
}
.fiche{
    background: #fff;
    border-radius: 12px;
    padding: 1.5rem;
    min-width: 350px;
    max-width: 600px;
    position: relative;
}
.close{
    position: absolute;
    top: .5rem;
    right: .5rem;
    border: none;
    background: none;
    cursor:  pointer;
    font-size: 1.1rem;

}
</style>
