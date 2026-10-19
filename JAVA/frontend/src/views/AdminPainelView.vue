<template>
  <div>
    <h2 style="margin: 0 0 6px; font-size: 1.35rem; color: var(--azul-escuro)">Painel Admin</h2>
    <p class="suave" style="margin: 0 0 20px">
      Área restrita de administração de dados — governança de cadastros e análise detalhada da base.
    </p>

    
    <div class="abas">
      <button
        class="aba"
        :class="{ ativa: aba === 'governanca' }"
        @click="aba = 'governanca'"
      >
        Governança
      </button>
      <button
        class="aba"
        :class="{ ativa: aba === 'detalhes' }"
        @click="aba = 'detalhes'"
      >
        Detalhes
      </button>
    </div>

    
    <div style="margin-top: 22px">
      <GovernancaView
        v-if="aba === 'governanca'"
        :usuario="usuario"
        @pendentes="(n: number) => emit('pendentes', n)"
        @api-fora="(v: boolean) => emit('api-fora', v)"
      />
      <DetalhesView v-else />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import GovernancaView from './GovernancaView.vue'
import DetalhesView from './DetalhesView.vue'

withDefaults(defineProps<{ usuario?: string }>(), { usuario: '' })
const emit = defineEmits<{
  (e: 'pendentes', n: number): void
  (e: 'api-fora', v: boolean): void
}>()

const aba = ref<'governanca' | 'detalhes'>('governanca')
</script>

<style scoped>
.abas {
  display: flex;
  gap: 8px;
  border-bottom: 2px solid var(--borda);
  padding-bottom: 10px;
}
.aba {
  background: transparent;
  border: 1px solid transparent;
  border-radius: 9px;
  padding: 10px 22px;
  font-size: 0.93rem;
  font-weight: 700;
  color: var(--texto-suave);
  cursor: pointer;
}
.aba:hover {
  color: var(--azul);
}
.aba.ativa {
  background: var(--azul);
  color: var(--texto-sobre-azul);
}
</style>
