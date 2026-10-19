

import { reactive } from 'vue'

export type Tema = 'claro' | 'escuro'

const estado = reactive<{ tema: Tema }>({
  tema: (localStorage.getItem('tema') as Tema) || 'claro'
})

function aplicar(): void {
  document.documentElement.setAttribute('data-tema', estado.tema)
  localStorage.setItem('tema', estado.tema)
}

function alternar(): void {
  estado.tema = estado.tema === 'claro' ? 'escuro' : 'claro'
  aplicar()
}

function cor(nome: string, padrao: string): string {
  try {
    const valor = getComputedStyle(document.documentElement).getPropertyValue(nome).trim()
    return valor || padrao
  } catch {
    return padrao
  }
}

export const tema = {
  estado,
  alternar,
  aplicar,
  cor
}
