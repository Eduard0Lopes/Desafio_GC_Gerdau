<template>
  <div class="ranking">
    <div v-for="(item, i) in itens" :key="item.id" class="ranking-item">
      <div class="posicao" :class="'posicao-' + (i + 1)">{{ i + 1 }}º</div>
      <div class="info">
        <div class="linha">
          <span class="id">{{ item.id }}</span>
          <strong class="nome">{{ item.nome }}</strong>
          <span v-if="item.grupo" class="grupo">{{ item.grupo }}</span>
        </div>
        <div class="trilha">
          <div class="barra" :style="{ width: largura(item.valor), background: corBarra(i) }"></div>
          <span class="valor">{{ item.valor.toLocaleString('pt-BR') }} {{ rotuloValor }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Item {
  id: string
  nome: string
  grupo?: string
  valor: number
}

const props = defineProps<{ itens: Item[]; rotuloValor?: string }>()

const maximo = computed(() => Math.max(...props.itens.map((i) => i.valor), 1))

function largura(valor: number): string {
  return `${Math.max(8, (valor / maximo.value) * 100)}%`
}

function corBarra(indice: number): string {
  return ['var(--barra-1)', 'var(--barra-2)', 'var(--barra-3)', 'var(--barra-4)'][indice % 4]
}
</script>

<style scoped>
.ranking {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.ranking-item {
  display: flex;
  align-items: center;
  gap: 12px;
  background: var(--fundo);
  border: 1px solid var(--borda);
  border-radius: 10px;
  padding: 10px 14px;
}
.posicao {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-weight: 800;
  font-size: 0.85rem;
  background: var(--borda);
  color: var(--texto);
  flex-shrink: 0;
}
.posicao-1 { background: var(--amarelo); color: var(--texto-forte); }
.posicao-2 { background: var(--barra-1); color: var(--texto-sobre-azul); }
.posicao-3 { background: var(--barra-3); color: var(--texto-sobre-azul); }
.info { flex: 1; min-width: 0; }
.linha {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 6px;
}
.id {
  color: var(--texto-suave);
  font-variant-numeric: tabular-nums;
  font-size: 0.72rem; font-weight: 600; letter-spacing: 0.04em; opacity: 0.75;
  flex-shrink: 0;
}
.nome {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 0.93rem;
  font-weight: 700;
  color: var(--texto);
}
.grupo {
  color: var(--texto-suave);
  font-size: 0.78rem;
}
.trilha {
  display: flex;
  align-items: center;
  gap: 10px;
}
.barra {
  height: 9px;
  border-radius: 6px;
  transition: width 0.9s cubic-bezier(0.22, 1, 0.36, 1);
}
.valor {
  color: var(--texto-suave);
  font-size: 0.8rem;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}
</style>
