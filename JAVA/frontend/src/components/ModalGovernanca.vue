<template>
  <div class="modal-fundo">
    <div class="modal">
      <div class="aviso aviso-azul">
        <span class="icone">ℹ</span>
        <div>
          <strong>Atenção:</strong> Este cadastro passará por validação humana pela
          Equipe de Governança de Dados.
        </div>
      </div>

      <h3>Envio para Governança</h3>

      <dl class="dados-classe">
        <dt>Classe:</dt>
        <dd>{{ classe }}</dd>

        <dt>Descrição:</dt>
        <dd>{{ descricao }}</dd>

        <dt>Justificativa da Solicitação:</dt>
        <dd>
          <textarea v-model="justificativa" placeholder="Descreva o motivo da solicitação…"></textarea>
        </dd>
      </dl>

      <div class="linha-botoes">
        <button class="botao botao-escuro" @click="$emit('voltar')">Voltar</button>
        <button
          class="botao botao-roxo"
          :disabled="justificativa.trim().length < 10"
          @click="$emit('confirmar', justificativa.trim())"
        >
          Confirmar Envio para Governança
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

withDefaults(defineProps<{ classe?: string; descricao?: string }>(), {
  classe: 'Código do Serviço',
  descricao: ''
})
defineEmits<{
  (e: 'voltar'): void
  (e: 'confirmar', justificativa: string): void
}>()

const justificativa = ref('')
</script>
