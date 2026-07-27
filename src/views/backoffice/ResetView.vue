<script setup>
import { deleteItem, getAll, NODE_API } from '@/services/api';
import { ref } from 'vue';

    const status = ref('')
    async function resetType(itemtype) {
    const active = await getAll(itemtype, false)

    const deleted = await getAll(itemtype, true)
    const all = [...active, ...deleted]


    for (const item of all) {
          console.log("TEST !! ! ",itemtype ," iD :",item.id);
        await deleteItem(itemtype, item.id)

    }
    return all.length
}
const PROTECTED_USERS = ['glpi', 'post-only', 'tech', 'normal', 'glpi-system']
async function resetUser() {
    const active = await getAll('Administration/User',false)
    const deleted  = await getAll('Administration/User',true)
    const users  = [...active , ...deleted]
    let count = 0
    for (const user of users) {
        if (PROTECTED_USERS.includes(user.username)) {
            continue
        }
        await deleteItem('Administration/User',user.id)
        count ++
    }
    return count
}
    async function handleReset() {
        status.value = 'SUpression en Cours'
        try {
            const types = ['Assets/Computer' , 'Assets/Monitor' , 'Assets/NetworkEquipement' , 'Assets/Printer' , 'Assets/pHone' ,   'Dropdowns/State', 'Dropdowns/Location', 'Dropdowns/Manufacturer',
            'Dropdowns/ComputerModel', 'Dropdowns/MonitorModel', 'Dropdowns/PhoneModel' , 'Assistance/Ticket' , 'Management/Document']
            let total = 0
            for (const type of types) {
                const count = await resetType(type)
                status.value = `Supression en cours ${type} ... (${count} SUpprimeés)`
                total += count
            }

            const userCount = await resetUser()
            total+= userCount
            status.value = `Reinitialisation terminée -${total} élements suprimés `

        } catch (error) {
            status.value = 'Erreur  : '+error.message
        }
    }
    async function handletesetSQLite() {
      await fetch(`${NODE_API}/api/cout`,{
        method : 'DELETE'
      })
      status.value = 'BASE  SQLITE REINITIALISE'
    }
</script>
<template>
    <h1>Reinitailiser la base </h1>
    <button @click="handleReset()">
        Reininialiser
    </button>
    <button @click="handletesetSQLite()">
      Reinitialiser base sqlite
    </button>
    <p v-if="status">{{ status }}</p>
</template>
