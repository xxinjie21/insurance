<template>
  <router-view />
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

onMounted(() => {
  // 检查本地存储中的 token
  const token = localStorage.getItem('token')
  if (token) {
    userStore.setToken(token)
  }
})
</script>

<style>
#app {
  width: 100%;
  height: 100vh;
}
</style>
