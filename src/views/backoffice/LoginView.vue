

<template>
    <label for="code">Code Unique :</label>
    <input v-model="code" type="number" name="code" id="code">
    <button @click="handleLogin(code)">Se connecter</button>
    <p>Mdp par defaut : 123</p>
</template>
<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
const router = useRouter()
    let code = ref(123);
    async function handleLogin(code) {
        console.log("CODE SAISIE : "+code)
           console.log("CODE SAISIE : "+code)
        if (code==123) {
            localStorage.setItem("isLoged",true) ;
            router.push('/dashboard')
        }
    }
</script>

<style scoped>
input { display: block; margin: .3rem 0; }
button { margin-top: .5rem; }
</style>
