<script setup lang="ts">
import { ref } from 'vue'
import GovernanceView from './GovernancaView.vue'
import DetailsView from './DetalhesView.vue'

withDefaults(defineProps<{ user?: string }>(), { user: '' })
const emit = defineEmits<{
  (event: 'pending', n: number): void
  (event: 'apiUnavailable', v: boolean): void
}>()

const tab = ref<'governance' | 'detalhes'>('governance')
</script>

<template>
  <div>
    <h2 style="margin: 0 0 6px; font-size: 1.35rem; color: var(--azul-escuro)">Painel Admin</h2>
    <p class="suave" style="margin: 0 0 20px">
      Área restrita de administração de dados — governança de cadastros e análise detalhada da base.
    </p>

    <div class="abas">
      <button
        class="aba"
        :class="{ ativa: tab === 'governance' }"
        @click="tab = 'governance'"
      >
        Governança
      </button>
      <button
        class="aba"
        :class="{ ativa: tab === 'detalhes' }"
        @click="tab = 'detalhes'"
      >
        Detalhes
      </button>
    </div>

    <div style="margin-top: 22px">
      <GovernanceView
        v-if="tab === 'governance'"
        :user="user"
        @pending="(count: number) => emit('pending', count)"
        @apiUnavailable="(v: boolean) => emit('apiUnavailable', v)"
      />
      <DetailsView v-else />
    </div>
  </div>
</template>

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
