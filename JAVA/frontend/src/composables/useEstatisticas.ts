import { computed, ref } from 'vue'
import { api } from '../services/api'
import type { Statistics } from '../types'

export function useStatistics() {
  const statistics = ref<Partial<Statistics>>({})

  const reduction = computed(() => {
    const total = statistics.value.matriz?.total || 0
    const servicos = statistics.value.catalogo?.servicos || 0
    if (!total || !servicos) return '0,0'
    return ((1 - servicos / total) * 100).toFixed(1).replace('.', ',')
  })

  async function load(): Promise<boolean> {
    try {
      statistics.value = await api.statistics()
      return true
    } catch {
      statistics.value = {}
      return false
    }
  }

  return { statistics, reduction, load }
}
