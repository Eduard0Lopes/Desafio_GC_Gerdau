import { reactive } from 'vue'

export type Theme = 'claro' | 'escuro'

const state = reactive<{ theme: Theme }>({
  theme: (localStorage.getItem('theme') as Theme) || 'claro'
})

function apply(): void {
  document.documentElement.setAttribute('data-tema', state.theme)
  localStorage.setItem('theme', state.theme)
}

function toggle(): void {
  state.theme = state.theme === 'claro' ? 'escuro' : 'claro'
  apply()
}

function color(nome: string, padrao: string): string {
  try {
    const value = getComputedStyle(document.documentElement).getPropertyValue(nome).trim()
    return value || padrao
  } catch {
    return padrao
  }
}

export const theme = {
  state,
  toggle,
  apply,
  color
}
