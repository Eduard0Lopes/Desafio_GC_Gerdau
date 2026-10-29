import { beforeEach, describe, expect, it, vi } from 'vitest'
import { api } from '../src/services/api'


describe('api — ids de vínculo com "#" nunca cortam a rota', () => {
  const chamadas: string[] = []

  beforeEach(() => {
    chamadas.length = 0
    localStorage.setItem('role', 'ADMIN')
    localStorage.setItem('user', 'ana')
    vi.stubGlobal(
      'fetch',
      vi.fn(async (url: string) => {
        chamadas.push(String(url))
        return { ok: true, status: 200, json: async () => ({}) }
      })
    )
  })

  it('aprovar, rejeitar e reutilizar codificam o id na URL', async () => {
    const id = 'GOVERNANCA_1440531_CS-000031_UN#2'

    await api.approve(id, { aprovadoPor: 'ana' })
    await api.reject(id, { aprovadoPor: 'ana' })
    await api.reuseCandidate(id, { escolhidoId: 'CS-000031', aprovadoPor: 'ana' })

    expect(chamadas[0]).toContain('/governanca/pendentes/GOVERNANCA_1440531_CS-000031_UN%232/aprovar')
    expect(chamadas[1]).toContain('/governanca/pendentes/GOVERNANCA_1440531_CS-000031_UN%232/rejeitar')
    expect(chamadas[2]).toContain('/governanca/pendentes/GOVERNANCA_1440531_CS-000031_UN%232/reutilizar')
    
    expect(chamadas.every((url) => !url.includes('#'))).toBe(true)
  })

  it('rotas de histórico e de pedido também codificam', async () => {
    await api.reviewHistory('mat-1#2', { revisadoPor: 'ana', acao: 'MANTER' })
    await api.fullCodes.cancelRequest('GS_1_CS_UN#3')

    expect(chamadas[0]).toContain('/governanca/historico/mat-1%232/revisar')
    expect(chamadas[1]).toContain('/codigos-completos/GS_1_CS_UN%233/cancelar')
    expect(chamadas.every((url) => !url.includes('#'))).toBe(true)
  })
})
