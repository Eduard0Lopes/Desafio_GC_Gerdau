<template>
  <div class="modal-fundo" @click.self="$emit('cancelar')">
    <div class="modal" style="max-width: 480px">
      <h3>{{ titulo }}</h3>
      <p style="line-height: 1.6; margin: 0 0 8px">{{ mensagem }}</p>
      <div class="linha-botoes">
        <button class="botao botao-escuro" @click="$emit('cancelar')">Cancelar</button>
        <button
          v-if="rotuloSecundario"
          class="botao"
          :class="secundarioPerigoso ? 'botao-vermelho' : 'botao-escuro'"
          @click="$emit('secundario')"
        >
          {{ rotuloSecundario }}
        </button>
        <button class="botao" :class="acaoPerigosa ? 'botao-vermelho' : 'botao-verde'" @click="$emit('confirmar')">
          {{ rotuloConfirmar }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  titulo: string
  mensagem: string
  rotuloConfirmar?: string
  acaoPerigosa?: boolean
  rotuloSecundario?: string
  secundarioPerigoso?: boolean
}>(), {
  rotuloConfirmar: 'Confirmar',
  acaoPerigosa: false,
  rotuloSecundario: '',
  secundarioPerigoso: false
})
defineEmits<{
  (e: 'confirmar'): void
  (e: 'cancelar'): void
  (e: 'secundario'): void
}>()
</script>
