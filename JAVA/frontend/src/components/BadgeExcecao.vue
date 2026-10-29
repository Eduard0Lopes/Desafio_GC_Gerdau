<script setup lang="ts">
import { specialCaseTitle } from '../services/formato'

defineProps<{ motivo?: unknown }>()
</script>

<template>
  <span class="badge-excecao" :title="specialCaseTitle(motivo)">!</span>
</template>
