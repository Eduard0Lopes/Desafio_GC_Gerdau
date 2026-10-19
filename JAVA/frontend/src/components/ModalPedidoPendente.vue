<template>
  <div class="modal-fundo" @click.self="$emit('fechar')">
    <div class="modal modal-pedido">
      <button class="fechar-x" type="button" title="Fechar (Esc)" @click="$emit('fechar')">✕</button>

      <h3>Já existe um pedido pendente na governança</h3>
      <p class="suave" style="margin: -6px 0 16px; line-height: 1.6">
        Esta composição já está na fila de validação. Veja o pedido abaixo — você pode editá-lo
        ou cancelá-lo antes de enviar outro.
      </p>

      <div class="pedido-info">
        <div class="campo">
          <span class="rotulo">Identificador</span>
          <strong>{{ pedido?.idCompleto || '—' }}</strong>
        </div>
        <div class="campo">
          <span class="rotulo">Enviado em</span>
          <span>{{ formatarData(pedido?.createdAt) }}</span>
        </div>
        <div class="campo">
          <span class="rotulo">Situação</span>
          <span class="badge badge-alertar">Pendente de aprovação</span>
        </div>
        <div class="campo">
          <span class="rotulo">Enviado por</span>
          <span>{{ pedido?.usuarioCriacaoId || '—' }}</span>
        </div>
        <div class="campo campo-largo">
          <span class="rotulo">Descrição do pedido</span>
          <span>{{ pedido?.descricaoSnapshot || '—' }}</span>
        </div>
        <div class="campo campo-largo">
          <span class="rotulo">Justificativa</span>
          <span>{{ pedido?.justificativa || '—' }}</span>
        </div>
      </div>

      <template v-if="editando">
        <div class="campo-form" style="margin-top: 18px">
          <label>Alterar descrição do serviço (máx. 40 caracteres SAP)</label>
          <input
            v-model="descricaoEdicao"
            type="text"
            maxlength="40"
            placeholder="Nova descrição do serviço do pedido"
          />
        </div>
        <div class="campo-form">
          <label>Alterar justificativa</label>
          <textarea v-model="justificativaEdicao" style="min-height: 78px"></textarea>
        </div>
      </template>

      <div class="linha-botoes">
        <button class="botao botao-escuro" type="button" @click="$emit('fechar')">Fechar</button>
        <template v-if="podeGerenciar">
          <button v-if="!editando" class="botao botao-roxo" type="button" @click="abrirEdicao">
            ✎ Editar pedido
          </button>
          <button
            v-if="!editando"
            class="botao botao-vermelho"
            type="button"
            :disabled="processando"
            @click="$emit('cancelar')"
          >
            ✕ Cancelar pedido
          </button>
          <template v-else>
            <button class="botao botao-escuro" type="button" @click="editando = false">Voltar</button>
            <button class="botao botao-roxo" type="button" :disabled="salvando" @click="salvar">
              {{ salvando ? 'Salvando…' : '💾 Salvar alterações' }}
            </button>
          </template>
        </template>
        <span v-else class="suave" style="font-size: 0.85rem">
          Somente quem enviou este pedido pode editá-lo ou cancelá-lo.
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import type { CodigoCompletoResponse } from '../types'

const props = withDefaults(defineProps<{
  pedido: CodigoCompletoResponse | null
  podeGerenciar?: boolean
  processando?: boolean
  salvando?: boolean
}>(), {
  podeGerenciar: false,
  processando: false,
  salvando: false
})

const emit = defineEmits<{
  (e: 'fechar'): void
  (e: 'cancelar'): void
  (e: 'salvar', dados: { descricao: string; justificativa: string }): void
}>()

const editando = ref(false)
const descricaoEdicao = ref('')
const justificativaEdicao = ref('')

watch(() => props.pedido, (p) => {
  editando.value = false
  descricaoEdicao.value = ''
  justificativaEdicao.value = p?.justificativa ?? ''
}, { immediate: true })

function abrirEdicao(): void {
  descricaoEdicao.value = ''
  justificativaEdicao.value = props.pedido?.justificativa ?? ''
  editando.value = true
}

function salvar(): void {
  emit('salvar', {
    descricao: descricaoEdicao.value.trim(),
    justificativa: justificativaEdicao.value.trim()
  })
}

function formatarData(iso: string | null | undefined): string {
  if (!iso) return '—'
  const d = new Date(iso)
  return isNaN(d.getTime()) ? iso : d.toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })
}
</script>

<style scoped>
.modal-pedido {
  max-width: 620px;
  position: relative;
}
.fechar-x {
  position: absolute;
  top: 14px;
  right: 14px;
  width: 30px;
  height: 30px;
  border: 1px solid var(--borda);
  border-radius: 8px;
  background: transparent;
  color: var(--texto-suave);
  font-size: 0.95rem;
  cursor: pointer;
  display: grid;
  place-items: center;
}
.fechar-x:hover {
  color: var(--vermelho);
  border-color: var(--vermelho);
}
.pedido-info {
  background: var(--fundo);
  border: 1px solid var(--borda);
  border-radius: 10px;
  padding: 14px 16px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px 18px;
}
.pedido-info .campo {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}
.pedido-info .campo-largo {
  grid-column: 1 / -1;
}
.pedido-info .rotulo {
  font-size: 0.72rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--texto-suave);
  font-weight: 700;
}
.pedido-info strong,
.pedido-info span:not(.rotulo):not(.badge) {
  overflow-wrap: anywhere;
}
</style>
