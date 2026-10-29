<script setup lang="ts">
import { ALERT_THRESHOLD, formatScore } from '../services/formato'

defineProps<{ score: number }>()
</script>

<template>
  <span class="match" :class="{ alerta: score < ALERT_THRESHOLD }">{{ formatScore(score) }} Match</span>
</template>
