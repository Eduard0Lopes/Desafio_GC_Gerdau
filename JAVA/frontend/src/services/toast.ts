

import { reactive } from 'vue'

export interface Toast {
  id: number
  mensagem: string
  tipo: 'sucesso' | 'erro' | 'aviso'
}

const estado = reactive<{ itens: Toast[] }>({ itens: [] })
let proximoId = 1

function mostrar(mensagem: string, tipo: Toast['tipo']): void {
  const id = proximoId++
  estado.itens.push({ id, mensagem, tipo })
  setTimeout(() => remover(id), tipo === 'erro' ? 7000 : 4200)
}

function remover(id: number): void {
  const i = estado.itens.findIndex((t) => t.id === id)
  if (i >= 0) estado.itens.splice(i, 1)
}

export const toast = {
  estado,
  remover,
  sucesso: (m: string) => mostrar(m, 'sucesso'),
  erro: (m: string) => mostrar(m, 'erro'),
  aviso: (m: string) => mostrar(m, 'aviso')
}
