<script setup lang="ts">
import { ref } from 'vue'
import BadgeException from './BadgeExcecao.vue'
import BadgeMatch from './BadgeMatch.vue'
import type { SimilarCandidate } from '../types'

defineProps<{
  itens: SimilarCandidate[]
  motivo?: string
}>()
const emit = defineEmits<{
  (event: 'reuse', item: SimilarCandidate): void
  (event: 'governance'): void
  (event: 'back'): void
}>()

const selected = ref<SimilarCandidate | null>(null)

function reuse(): void {
  if (selected.value) emit('reuse', selected.value)
}
</script>

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
          v-for="candidato in itens"
          :key="candidato.id"
          class="opcao-item"
          :class="{ selecionada: selected?.id === candidato.id }"
          @click="selected = candidato"
        >
          <input
            type="radio"
            name="item-reutilizar"
            :checked="selected?.id === candidato.id"
            @change="selected = candidato"
          />
          <div class="opcao-conteudo">
            <div class="opcao-linha">
              <span class="opcao-id">{{ candidato.id }}</span>
              <span class="separador">|</span>
              <strong>{{ (candidato.descricao || '').toUpperCase() }}</strong>
              <BadgeException v-if="candidato.casoIsolado" :motivo="candidato.motivoCasoIsolado" />
              <BadgeMatch :score="candidato.score" />
            </div>
            <div v-if="candidato.motivo" class="opcao-motivo">{{ candidato.motivo }}</div>
          </div>
        </div>
      </div>

      <div class="linha-botoes">
        <button class="botao botao-roxo" :disabled="!selected" :title="selected ? '' : 'Selecione um item da lista'"
                @click="reuse">
          Reutilizar Código Existente
        </button>
        <button class="botao botao-escuro" @click="$emit('governance')">Enviar para Governança</button>
        <button class="botao botao-escuro" @click="$emit('back')">Voltar</button>
      </div>
    </div>
  </div>
</template>

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
