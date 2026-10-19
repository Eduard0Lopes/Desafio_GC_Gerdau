<template>
  <div class="modal-fundo">
    <div class="modal">
      <h3>Bloqueio de Duplicidade — Item Similar Detected</h3>

      <div class="aviso aviso-amarelo">
        <span class="icone">⚠</span>
        <div>{{ motivo || 'O sistema identificou itens semelhantes cadastrados.' }}</div>
      </div>

      
      <div v-if="itens.length" class="lista-selecao">
        <div
          v-for="c in itens"
          :key="c.id"
          class="opcao-item"
          :class="{ selecionada: selecionado?.id === c.id }"
          @click="selecionado = c"
        >
          <input
            type="radio"
            name="item-reutilizar"
            :checked="selecionado?.id === c.id"
            @change="selecionado = c"
          />
          <div class="opcao-conteudo">
            <div class="opcao-linha">
              <span class="opcao-id">{{ c.id }}</span>
              <span class="separador">|</span>
              <strong>{{ (c.descricao || '').toUpperCase() }}</strong>
              <span
                v-if="c.casoIsolado"
                class="badge-excecao"
                :title="'Caso especial registrado.' + (c.motivoCasoIsolado ? ' Motivo: ' + c.motivoCasoIsolado : '')"
              >!</span>
              <span class="match" :class="{ alerta: c.score < 0.88 }">{{ (c.score * 100).toFixed(0) }}% Match</span>
            </div>
            <div v-if="c.motivo" class="opcao-motivo">{{ c.motivo }}</div>
          </div>
        </div>
      </div>

      <div class="linha-botoes">
        <button class="botao botao-roxo" :disabled="!selecionado" :title="selecionado ? '' : 'Selecione um item da lista'"
                @click="reutilizar">
          Reutilizar Código Existente
        </button>
        <button class="botao botao-escuro" @click="$emit('governanca')">Enviar para Governança</button>
        <button class="botao botao-escuro" @click="$emit('voltar')">Voltar</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { CandidatoSimilar } from '../types'

defineProps<{
  itens: CandidatoSimilar[]
  motivo?: string
}>()
const emit = defineEmits<{
  (e: 'reutilizar', item: CandidatoSimilar): void
  (e: 'governanca'): void
  (e: 'voltar'): void
}>()


const selecionado = ref<CandidatoSimilar | null>(null)

function reutilizar(): void {
  if (selecionado.value) emit('reutilizar', selecionado.value)
}
</script>

<style scoped>
.lista-selecao {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 20px;
}
.opcao-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  background: var(--fundo);
  border: 1.5px solid var(--borda);
  border-radius: 10px;
  padding: 13px 16px;
  cursor: pointer;
  transition: all 0.12s;
}
.opcao-item:hover {
  border-color: var(--azul);
}
.opcao-item.selecionada {
  border-color: var(--azul);
  background: var(--azul-claro);
}
.opcao-item input[type='radio'] {
  margin-top: 3px;
  accent-color: var(--azul);
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}
.opcao-conteudo {
  flex: 1;
  min-width: 0;
}
.opcao-linha {
  display: flex;
  align-items: baseline;
  gap: 10px;
  flex-wrap: wrap;
  font-variant-numeric: tabular-nums;
}
.opcao-id {
  font-size: 0.72rem; font-weight: 600; letter-spacing: 0.04em;
  color: var(--texto-suave); opacity: 0.75;
}
.separador {
  color: var(--texto-suave);
}
.opcao-motivo {
  margin-top: 5px;
  color: var(--texto-suave);
  font-size: 0.84rem;
}
</style>
