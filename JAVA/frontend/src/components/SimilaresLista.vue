<script setup>
defineProps({
  similares: { type: Array, default: () => [] },
  carregando: Boolean,
  selecionavel: Boolean,
})
defineEmits(['selecionar'])
</script>

<template>
  <div v-if="carregando" class="dica">Buscando serviços similares…</div>
  <div v-else-if="similares.length" class="similares">
    <p class="dica">Serviços parecidos já cadastrados (mesmo motor da IA):</p>
    <ul>
      <li v-for="s in similares" :key="s.id">
        <span :class="['selo', s.status]">{{ (s.score * 100).toFixed(1) }}%</span>
        <span class="mono">{{ s.id }}</span>
        <span class="descricao">{{ s.descricao }}</span>
        <button v-if="selecionavel" type="button" class="link" @click="$emit('selecionar', s)">usar</button>
      </li>
    </ul>
    <p v-if="similares[0].status !== 'ORIGINAL'" :class="['aviso', similares[0].status]">
      {{ similares[0].acaoRecomendada }}
    </p>
  </div>
</template>

<style scoped>
.similares { background: var(--fundo); border-radius: 6px; padding: .6rem .75rem; margin-bottom: .85rem; }
ul { list-style: none; margin: 0; padding: 0; }
li { display: flex; gap: .5rem; align-items: center; font-size: .85rem; padding: .2rem 0; }
.descricao { flex: 1; }
.aviso { margin: .5rem 0 0; font-size: .85rem; font-weight: 600; }
.aviso.DUPLICIDADE { color: var(--erro); }
.aviso.ALERTA { color: var(--alerta); }
</style>
