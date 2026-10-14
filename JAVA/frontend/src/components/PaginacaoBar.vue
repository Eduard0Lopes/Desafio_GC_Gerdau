<script setup>
defineProps({ pagina: { type: Object, required: true } })
defineEmits(['ir'])
</script>

<template>
  <div v-if="pagina.totalPages > 1" class="paginacao">
    <button class="secundario" :disabled="pagina.page === 0" @click="$emit('ir', pagina.page - 1)">Anterior</button>
    <span>Página {{ pagina.page + 1 }} de {{ pagina.totalPages }} · {{ pagina.totalElements.toLocaleString('pt-BR') }} registros</span>
    <button class="secundario" :disabled="pagina.page + 1 >= pagina.totalPages" @click="$emit('ir', pagina.page + 1)">Próxima</button>
  </div>
  <p v-else class="dica">{{ pagina.totalElements }} registro(s)</p>
</template>

<style scoped>
.paginacao { display: flex; align-items: center; justify-content: space-between; gap: 1rem; margin-top: 1rem; color: var(--cinza); font-size: .9rem; }
</style>
