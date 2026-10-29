<script setup lang="ts">
import { ref, watch } from 'vue'
import { formatDate } from '../services/formato'
import type { FullCodeResponse } from '../types'

const props = withDefaults(defineProps<{
  request: FullCodeResponse | null
  canManage?: boolean
  processing?: boolean
  saving?: boolean
}>(), {
  canManage: false,
  processing: false,
  saving: false
})

const emit = defineEmits<{
  (event: 'close'): void
  (event: 'cancel'): void
  (event: 'save', data: { descricao: string; justificativa: string }): void
}>()

const editing = ref(false)
const editDescription = ref('')
const editJustification = ref('')

watch(() => props.request, (updatedRequest) => {
  editing.value = false
  editDescription.value = ''
  editJustification.value = updatedRequest?.justificativa ?? ''
}, { immediate: true })

function startEdit(): void {
  editDescription.value = ''
  editJustification.value = props.request?.justificativa ?? ''
  editing.value = true
}

function save(): void {
  emit('save', {
    descricao: editDescription.value.trim(),
    justificativa: editJustification.value.trim()
  })
}
</script>

<template>
  <div class="modal-fundo" @click.self="$emit('close')">
    <div class="modal modal-pedido">
      <button class="fechar-x" type="button" title="Fechar (Esc)" @click="$emit('close')">✕</button>

      <h3>Já existe um pedido pendente na governança</h3>
      <updatedRequest class="suave" style="margin: -6px 0 16px; line-height: 1.6">
        Esta composição já está na fila de validação. Veja o pedido abaixo — você pode editá-lo
        ou cancelá-lo antes de enviar outro.
      </updatedRequest>

      <div class="pedido-info">
        <div class="campo">
          <span class="rotulo">Identificador</span>
          <strong>{{ request?.idCompleto || '—' }}</strong>
        </div>
        <div class="campo">
          <span class="rotulo">Enviado em</span>
          <span>{{ formatDate(request?.createdAt) }}</span>
        </div>
        <div class="campo">
          <span class="rotulo">Situação</span>
          <span class="badge badge-alertar">Pendente de aprovação</span>
        </div>
        <div class="campo">
          <span class="rotulo">Enviado por</span>
          <span>{{ request?.usuarioCriacaoId || '—' }}</span>
        </div>
        <div class="campo campo-largo">
          <span class="rotulo">Descrição do pedido</span>
          <span>{{ request?.descricaoSnapshot || '—' }}</span>
        </div>
        <div class="campo campo-largo">
          <span class="rotulo">Justificativa</span>
          <span>{{ request?.justificativa || '—' }}</span>
        </div>
      </div>

      <template v-if="editing">
        <div class="campo-form" style="margin-top: 18px">
          <label>Alterar descrição do serviço (máx. 40 caracteres SAP)</label>
          <input
            v-model="editDescription"
            type="text"
            maxlength="40"
            placeholder="Nova descrição do serviço do pedido"
          />
        </div>
        <div class="campo-form">
          <label>Alterar justificativa</label>
          <textarea v-model="editJustification" style="min-height: 78px"></textarea>
        </div>
      </template>

      <div class="linha-botoes">
        <button class="botao botao-escuro" type="button" @click="$emit('close')">Fechar</button>
        <template v-if="canManage">
          <button v-if="!editing" class="botao botao-roxo" type="button" @click="startEdit">
            ✎ Editar pedido
          </button>
          <button
            v-if="!editing"
            class="botao botao-vermelho"
            type="button"
            :disabled="processing"
            @click="$emit('cancel')"
          >
            ✕ Cancelar pedido
          </button>
          <template v-else>
            <button class="botao botao-escuro" type="button" @click="editing = false">Voltar</button>
            <button class="botao botao-roxo" type="button" :disabled="saving" @click="save">
              {{ saving ? 'Salvando…' : '💾 Salvar alterações' }}
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
