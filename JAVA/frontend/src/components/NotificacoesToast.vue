<script setup>
import { useNotificacoes } from '@/composables/useNotificacoes'

const { estado, remover } = useNotificacoes()
</script>

<template>
  <div class="toasts" aria-live="polite">
    <div v-for="n in estado.itens" :key="n.id" :class="['toast', n.tipo]" @click="remover(n.id)">
      {{ n.texto }}
    </div>
  </div>
</template>

<style scoped>
.toasts { position: fixed; right: 1rem; bottom: 1rem; display: flex; flex-direction: column; gap: .5rem; z-index: 10; max-width: 420px; }
.toast { padding: .75rem 1rem; border-radius: 6px; color: #fff; box-shadow: 0 4px 12px rgba(0,0,0,.15); cursor: pointer; }
.toast.sucesso { background: var(--ok); }
.toast.erro { background: var(--erro); }
.toast.info { background: var(--azul); }
</style>
