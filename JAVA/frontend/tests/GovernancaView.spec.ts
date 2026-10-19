import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import GovernancaView from '../src/views/GovernancaView.vue'

const PENDENTES = [
  {
    origem: 'NOVO_CADASTRO',
    id: 'cc-1',
    identificador: 'CIVIL_1_CS-000007_UN',
    descricao: 'SV OFICIAL MENSAL',
    grupoServico: 'CIVIL',
    fornecedor: 'F',
    unidadeMedida: 'UN',
    legacyId: null,
    legacyDescription: null,
    decision: 'REVISAR',
    score: 0.95,
    reason: '',
    solicitante: '403921',
    justificativa: 'semelhante ao existente',
    status: 'PENDENTE_VALIDACAO',
    natureza: 'MATERIAL',
    similares: [],
    casoIsolado: true
  },
  {
    origem: 'NOVO_CADASTRO',
    id: 'cc-2',
    identificador: 'CIVIL_1_CS-000008_UN',
    descricao: 'OUTRO SERVICO',
    grupoServico: 'CIVIL',
    fornecedor: 'F',
    unidadeMedida: 'UN',
    legacyId: null,
    legacyDescription: null,
    decision: 'PERMITIR',
    score: 0.1,
    reason: '',
    solicitante: '403921',
    justificativa: null,
    status: 'PENDENTE_VALIDACAO',
    natureza: 'MATERIAL',
    similares: [],
    casoIsolado: false
  }
]

function mockFetch() {
  const decididos = new Set<string>()
  return vi.fn(async (url: string, init?: RequestInit) => {
    const rota = String(url)
    if (rota.includes('/governanca/resumo')) {
      return {
        ok: true,
        status: 200,
        json: async () => ({
          pendentesHistorico: 0,
          pendentesNovos: PENDENTES.length - decididos.size,
          decisionDuplicidade: 0,
          decisionAlerta: 0,
          decisionOriginal: 0,
          revisados: 0,
          excecoes: 0
        })
      }
    }
    if (rota.includes('/governanca/fila/contagem')) {
      return { ok: true, status: 200, json: async () => ({ total: PENDENTES.length - decididos.size }) }
    }
    if (rota.includes('/governanca/pendentes/') && init?.method === 'POST') {
      const partes = rota.split('/')
      const id = partes[partes.length - 2] // /governanca/pendentes/{id}/aprovar
      decididos.add(id)
      return { ok: true, status: 200, json: async () => PENDENTES.find((p) => p.id === id) }
    }
    if (rota.includes('/governanca/pendentes')) {
      return { ok: true, status: 200, json: async () => PENDENTES.filter((p) => !decididos.has(p.id)) }
    }
    return { ok: true, status: 200, json: async () => [] }
  })
}

describe('GovernancaView — depois da decisão o card sai da fila', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', mockFetch())
    localStorage.setItem('papel', 'ADMIN') // decisões da governança são só de admin
    localStorage.setItem('usuario', '403921')
  })

  it('aprovar remove o card decidido e mantém os demais', async () => {
    const wrapper = mount(GovernancaView, { props: { usuario: '403921' } })
    await flushPromises()

    expect(wrapper.text()).toContain('CIVIL_1_CS-000007_UN')
    expect(wrapper.text()).toContain('CIVIL_1_CS-000008_UN')
    expect(wrapper.find('.badge-excecao').exists()).toBe(true) // marcador [!] de exceção aprovada

    await wrapper.findAll('textarea')[0].setValue('aprovado como caso isolado')
    await flushPromises()

    const aprovar = wrapper.findAll('button').find((b) => b.text().includes('Aprovar cadastro'))
    expect(aprovar).toBeTruthy()
    await aprovar!.trigger('click')
    await flushPromises()

    const confirmar = wrapper.findAll('.modal button').find((b) => b.text().includes('Aprovar'))
    expect(confirmar).toBeTruthy()
    await confirmar!.trigger('click')
    await flushPromises()

    // o card decidido some da fila (o toast até cita o id, mas o cartão não está mais lá)
    const cards = wrapper.findAll('.card-item')
    expect(cards.length).toBe(1)
    expect(cards[0].text()).toContain('CIVIL_1_CS-000008_UN')
    expect(wrapper.find('.card-item').text()).not.toContain('CIVIL_1_CS-000007_UN')
  })

  it('pede o motivo antes de decidir', async () => {
    const wrapper = mount(GovernancaView, { props: { usuario: '403921' } })
    await flushPromises()

    const aprovar = wrapper.findAll('button').find((b) => b.text().includes('Aprovar cadastro'))
    await aprovar!.trigger('click')
    await flushPromises()

    const confirmar = wrapper.findAll('.modal button').find((b) => b.text().includes('Aprovar'))
    await confirmar!.trigger('click')
    await flushPromises()

    // sem motivo a decisão não acontece e o card continua na tela
    expect(wrapper.findAll('.card-item').length).toBe(2)
    expect(wrapper.text()).toContain('CIVIL_1_CS-000007_UN')
  })
})
