<script setup lang="ts">
import { ref } from 'vue'

withDefaults(defineProps<{ itemClass?: string; descricao?: string }>(), {
  itemClass: 'Código do Serviço',
  descricao: ''
})
defineEmits<{
  (event: 'back'): void
  (event: 'confirm', justificativa: string): void
}>()

const justificativa = ref('')
</script>

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
        <dd>{{ itemClass }}</dd>

        <dt>Descrição:</dt>
        <dd>{{ descricao }}</dd>

        <dt>Justificativa da Solicitação:</dt>
        <dd>
          <textarea v-model="justificativa" placeholder="Descreva o motivo da solicitação…(Minimo 10 caracteres)"></textarea>
        </dd>
      </dl>

      <div class="linha-botoes">
        <button class="botao botao-escuro" @click="$emit('back')">Voltar</button>
        <button
          class="botao botao-roxo"
          :disabled="justificativa.trim().length < 10"
          @click="$emit('confirm', justificativa.trim())"
        >
          Confirmar Envio para Governança
        </button>
      </div>
    </div>
  </div>
</template>
