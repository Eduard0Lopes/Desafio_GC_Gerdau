import { onBeforeUnmount } from 'vue'

export function useDebounce(fn, atraso = 350) {
  let timer
  const executar = (...args) => {
    clearTimeout(timer)
    timer = setTimeout(() => fn(...args), atraso)
  }
  onBeforeUnmount(() => clearTimeout(timer))
  return executar
}
