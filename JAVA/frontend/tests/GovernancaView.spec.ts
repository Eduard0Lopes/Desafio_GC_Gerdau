import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import GovernanceView from '../src/views/GovernancaView.vue'

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
      const parts = rota.split('/')
      const id = parts[parts.length - 2] 
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
    localStorage.setItem('role', 'ADMIN') 
    localStorage.setItem('user', '403921')
  })

  it('aprovar remove o card decidido e mantém os demais', async () => {
    const wrapper = mount(GovernanceView, { props: { user: '403921' } })
    await flushPromises()

    expect(wrapper.text()).toContain('CIVIL_1_CS-000007_UN')
    expect(wrapper.text()).toContain('CIVIL_1_CS-000008_UN')
    expect(wrapper.find('.badge-excecao').exists()).toBe(true) 

    await wrapper.findAll('textarea')[0].setValue('aprovado como caso isolado')
    await flushPromises()

    const approve = wrapper.findAll('button').find((b) => b.text().includes('Aprovar cadastro'))
    expect(approve).toBeTruthy()
    await approve!.trigger('click')
    await flushPromises()

    const confirmar = wrapper.findAll('.modal button').find((b) => b.text().includes('Aprovar'))
    expect(confirmar).toBeTruthy()
    await confirmar!.trigger('click')
    await flushPromises()

    
    const cards = wrapper.findAll('.card-item')
    expect(cards.length).toBe(1)
    expect(cards[0].text()).toContain('CIVIL_1_CS-000008_UN')
    expect(wrapper.find('.card-item').text()).not.toContain('CIVIL_1_CS-000007_UN')
  })

  it('pede o motivo antes de decidir', async () => {
    const wrapper = mount(GovernanceView, { props: { user: '403921' } })
    await flushPromises()

    const approve = wrapper.findAll('button').find((b) => b.text().includes('Aprovar cadastro'))
    await approve!.trigger('click')
    await flushPromises()

    const confirmar = wrapper.findAll('.modal button').find((b) => b.text().includes('Aprovar'))
    await confirmar!.trigger('click')
    await flushPromises()

    
    expect(wrapper.findAll('.card-item').length).toBe(2)
    expect(wrapper.text()).toContain('CIVIL_1_CS-000007_UN')
  })
})


const COM_MATCHES = [
  {
    origem: 'NOVO_CADASTRO',
    id: 'cc-9',
    identificador: 'CIVIL_1_CS-000009_UN',
    descricao: 'SV EQUIPE TELHADO',
    grupoServico: 'CIVIL',
    fornecedor: 'F',
    unidadeMedida: 'UN',
    legacyId: null,
    legacyDescription: null,
    decision: 'REVISAR',
    score: 0.93,
    reason: '',
    solicitante: '403921',
    justificativa: 'não achei o código existente',
    status: 'PENDENTE_VALIDACAO',
    natureza: 'SERVICO',
    similares: [
      {
        posicao: 1,
        id: 'CS-000001',
        codigoOriginal: null,
        descricao: 'SV EQUIPE DE TELHADO',
        score: 0.93,
        scoreTokens: 0.9,
        scoreCaracteres: 0.95,
        tokensEmComum: [],
        motivo: 'muito parecido',
        casoIsolado: false,
        motivoCasoIsolado: null,
        tipo: 'CS'
      },
      {
        posicao: 2,
        id: 'GS-MECANICA',
        codigoOriginal: null,
        descricao: 'MECANICA',
        score: 0.8,
        scoreTokens: 0,
        scoreCaracteres: 0,
        tokensEmComum: [],
        motivo: 'grupo parecido',
        casoIsolado: false,
        motivoCasoIsolado: null,
        tipo: 'GS'
      }
    ],
    casoIsolado: false
  }
]

function mockFetchComMatches() {
  const decididos = new Set<string>()
  const chamadas: { url: string; body: unknown }[] = []
  const fn = vi.fn(async (url: string, init?: RequestInit) => {
    const rota = String(url)
    if (rota.includes('/governanca/resumo')) {
      return {
        ok: true,
        status: 200,
        json: async () => ({
          pendentesHistorico: 0,
          pendentesNovos: COM_MATCHES.length - decididos.size,
          decisionDuplicidade: 0,
          decisionAlerta: 0,
          decisionOriginal: 0,
          revisados: 0,
          excecoes: 0
        })
      }
    }
    if (rota.includes('/governanca/fila/contagem')) {
      return { ok: true, status: 200, json: async () => ({ total: COM_MATCHES.length - decididos.size }) }
    }
    if (rota.includes('/governanca/pendentes/') && init?.method === 'POST') {
      chamadas.push({ url: rota, body: init?.body ? JSON.parse(String(init.body)) : null })
      const parts = rota.split('/')
      const id = parts[parts.length - 2]
      decididos.add(id)
      return { ok: true, status: 200, json: async () => COM_MATCHES.find((p) => p.id === id) }
    }
    if (rota.includes('/governanca/pendentes')) {
      return { ok: true, status: 200, json: async () => COM_MATCHES.filter((p) => !decididos.has(p.id)) }
    }
    return { ok: true, status: 200, json: async () => [] }
  })
  return { fn, chamadas }
}

describe('GovernancaView — vê o que o usuário não escolheu e pode escolher por ele', () => {
  beforeEach(() => {
    localStorage.setItem('role', 'ADMIN')
    localStorage.setItem('user', 'ana')
  })

  it('mostra os matches ignorados no radio do bloqueio de duplicidade', async () => {
    vi.stubGlobal('fetch', mockFetchComMatches().fn)
    const wrapper = mount(GovernanceView, { props: { user: 'ana' } })
    await flushPromises()

    expect(wrapper.text()).toContain('O que o usuário não escolheu')
    expect(wrapper.text()).toContain('CS-000001')
    expect(wrapper.text()).toContain('SV EQUIPE DE TELHADO')
    expect(wrapper.text()).toContain('GS-MECANICA')
    
    const reuse = wrapper.findAll('button').find((b) => b.text().includes('Reutilizar escolhido'))
    expect(reuse).toBeTruthy()
    expect(reuse!.attributes('disabled')).toBeDefined()
  })

  it('a Governança escolhe o match e o pedido segue reutilizando o código', async () => {
    const mock = mockFetchComMatches()
    vi.stubGlobal('fetch', mock.fn)
    const wrapper = mount(GovernanceView, { props: { user: 'ana' } })
    await flushPromises()

    const opcao = wrapper.findAll('.opcao-item').find((o) => o.text().includes('CS-000001'))
    expect(opcao).toBeTruthy()
    await opcao!.trigger('click')

    const reuse = wrapper.findAll('button').find((b) => b.text().includes('Reutilizar escolhido'))
    expect(reuse!.attributes('disabled')).toBeUndefined()
    await reuse!.trigger('click')
    await flushPromises()

    const confirmar = wrapper.findAll('.modal button').find((b) => b.text().includes('Reutilizar e seguir'))
    expect(confirmar).toBeTruthy()
    await confirmar!.trigger('click')
    await flushPromises()

    
    const chamada = mock.chamadas.find((c) => c.url.includes('/reutilizar'))
    expect(chamada).toBeTruthy()
    expect((chamada!.body as { escolhidoId: string }).escolhidoId).toBe('CS-000001')
    expect(wrapper.findAll('.card-item').length).toBe(0)
  })
})
