<script setup>
import SideBar from './components/SideBar.vue';
</script>

<template>
  <div style="display: flex">
    <SideBar />
    <main style="flex: 1; padding: 1rem">
      <RouterView />
    </main>
  </div>
</template>
