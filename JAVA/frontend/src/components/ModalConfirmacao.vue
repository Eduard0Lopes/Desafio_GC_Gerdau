<script setup lang="ts">
withDefaults(defineProps<{
  title: string
  message: string
  confirmLabel?: string
  dangerousAction?: boolean
  secondaryLabel?: string
  secondaryDangerous?: boolean
}>(), {
  confirmLabel: 'Confirmar',
  dangerousAction: false,
  secondaryLabel: '',
  secondaryDangerous: false
})
defineEmits<{
  (event: 'confirm'): void
  (event: 'cancel'): void
  (event: 'secondary'): void
}>()
</script>

<template>
  <div class="modal-fundo" @click.self="$emit('cancel')">
    <div class="modal" style="max-width: 480px">
      <h3>{{ title }}</h3>
      <p style="line-height: 1.6; margin: 0 0 8px">{{ message }}</p>
      <div class="linha-botoes">
        <button class="botao botao-escuro" @click="$emit('cancel')">Cancelar</button>
        <button
          v-if="secondaryLabel"
          class="botao"
          :class="secondaryDangerous ? 'botao-vermelho' : 'botao-escuro'"
          @click="$emit('secondary')"
        >
          {{ secondaryLabel }}
        </button>
        <button class="botao" :class="dangerousAction ? 'botao-vermelho' : 'botao-verde'" @click="$emit('confirm')">
          {{ confirmLabel }}
        </button>
      </div>
    </div>
  </div>
</template>
