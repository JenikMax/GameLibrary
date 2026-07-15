<template>
  <Card class="game-card">
    <template #header>
      <Skeleton height="280px" class="border-round-top" />
    </template>
    <template #title>
      <Skeleton width="80%" height="1.2rem" class="mb-2" />
    </template>
    <template #subtitle>
      <div class="flex gap-2">
        <Skeleton width="4rem" height="1.5rem" />
        <Skeleton width="3rem" height="1.5rem" />
      </div>
    </template>
    <template #content>
      <div class="flex flex-wrap gap-1">
        <Skeleton v-for="i in 3" :key="i" width="3.5rem" height="1.2rem" borderRadius="1rem" />
      </div>
    </template>
    <template #footer>
      <div class="flex gap-2">
        <Skeleton width="6rem" height="2rem" />
        <Skeleton width="6rem" height="2rem" />
      </div>
    </template>
  </Card>
</template>

<script setup>
import Card from 'primevue/card'
import Skeleton from 'primevue/skeleton'
</script>

<style scoped>
.game-card {
  cursor: default;
}
</style>
