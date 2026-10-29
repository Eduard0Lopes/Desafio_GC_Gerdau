import { reactive } from 'vue'

export interface Toast {
  id: number
  message: string
  tipo: 'success' | 'error' | 'warning'
}

const state = reactive<{ itens: Toast[] }>({ itens: [] })
let nextId = 1

function show(message: string, tipo: Toast['tipo']): void {
  const id = nextId++
  state.itens.push({ id, message, tipo })
  setTimeout(() => removeItem(id), tipo === 'error' ? 7000 : 4200)
}

function removeItem(id: number): void {
  const index = state.itens.findIndex((entry) => entry.id === id)
  if (index >= 0) state.itens.splice(index, 1)
}

export const toast = {
  state,
  removeItem,
  success: (message: string) => show(message, 'success'),
  error: (message: string) => show(message, 'error'),
  warning: (message: string) => show(message, 'warning')
}
