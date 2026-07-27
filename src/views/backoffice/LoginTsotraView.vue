<template>
    <label for="code">Code Unique :</label>
    <input v-model="code" type="number" name="code" id="code" value="123">
    <button @click="handleLogin"></button>
</template>
<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
const router = useRouter()
    const code = ref(123);
    console.log("CODE SAISIE : "+code)
    async function handleLogin(code) {
        if (code = "123") {
            router.push('/test')
        }
    }
</script>
