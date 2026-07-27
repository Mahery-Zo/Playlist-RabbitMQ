<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router';
import { login } from '@/services/api';

const username = ref ('')
const password = ref('')
const error = ref ('')
const router = useRouter()

async function handleLOgin() {
    error.value = ''
    try{
        await login(username.value,password.value)
        await router.push('/dashboard')
        console.log("TESTTT")
    }catch(e){
        error.value = e.message
    }
}
</script>
<template>
    <form @submit.prevent="handleLOgin">

    
        <h3><label for="loginName">Username</label></h3>
        <input v-model="username" id="loginName" type="text" placeholder="Identifiant" required>
    
        <h3><label for="password">Password</label></h3>
        <input v-model="password" id="password" type="password" placeholder="Mot de passe" required>
    
       
        <button type="submit">Se connecter</button>
       
           <p v-if="error">{{ error }}</p>
        
    </form>
    

</template>

<style scoped>
form { max-width: 300px; }
input { display: block; width: 100%; margin: .3rem 0 .5rem; }
button { margin-top: .5rem; }
p { color: #c00; margin-top: .3rem; }
</style>
