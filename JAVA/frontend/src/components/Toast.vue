<template>
  <div class="toasts" aria-live="polite">
    <transition-group name="toast">
      <div v-for="t in toast.estado.itens" :key="t.id" class="toast" :class="'toast-' + t.tipo"
           :style="{ '--duracao': t.tipo === 'erro' ? '7s' : '4.2s' }">
        <span class="icone">{{ icone(t.tipo) }}</span>
        <span class="mensagem">{{ t.mensagem }}</span>
        <button class="fechar" aria-label="Fechar" @click="toast.remover(t.id)">✕</button>
      </div>
    </transition-group>
  </div>
</template>

<script setup lang="ts">
import { toast } from '../services/toast'

function icone(tipo: string): string {
  return tipo === 'sucesso' ? '✓' : tipo === 'erro' ? '⚠' : 'ℹ'
}
</script>

<style scoped>
.toasts {
  position: fixed;
  top: 18px;
  right: 18px;
  z-index: 200;
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-width: 380px;
}
.toast {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 13px 15px;
  border-radius: 11px;
  background: var(--cartao);
  border: 1px solid var(--borda);
  border-left: 4px solid var(--azul);
  box-shadow: 0 12px 34px rgba(0, 32, 91, 0.22);
  font-size: 0.9rem;
  line-height: 1.45;
  position: relative;
  overflow: hidden;
}

.toast::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: 0;
  height: 3px;
  width: 100%;
  background: currentColor;
  opacity: 0.55;
  transform-origin: left;
  animation: regrerir linear forwards;
  animation-duration: var(--duracao, 4.2s);
}
.toast-sucesso::after { background: var(--verde); }
.toast-erro::after { background: var(--vermelho); }
.toast-aviso::after { background: var(--amarelo); }
@keyframes regrerir {
  from { transform: scaleX(1); }
  to { transform: scaleX(0); }
}
.toast-sucesso { border-left-color: var(--verde); }
.toast-erro { border-left-color: var(--vermelho); }
.toast-aviso { border-left-color: var(--amarelo); }
.icone { font-size: 1rem; line-height: 1.3; }
.toast-sucesso .icone { color: var(--verde); }
.toast-erro .icone { color: var(--vermelho); }
.toast-aviso .icone { color: var(--amarelo-alerta); }
.mensagem { flex: 1; color: var(--texto); }
.fechar {
  border: none; background: transparent; color: var(--texto-suave);
  cursor: pointer; font-size: 0.85rem;
}
.fechar:hover { color: var(--texto); }

.toast-enter-active, .toast-leave-active { transition: all 0.3s ease; }
.toast-enter-from { opacity: 0; transform: translateX(30px); }
.toast-leave-to { opacity: 0; transform: translateX(30px); }
</style>
