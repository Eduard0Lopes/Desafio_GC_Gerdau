import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import VinculacaoMassa from '../src/views/VinculacaoMassa.vue'

const ANALISE = {
  tipo: 'CS',
  total: 3,
  disponiveis: 1,
  aCriar: 2,
  bloqueadas: 0,
  itens: [
    {
      linha: 1,
      texto: 'SV EQUIPE TELHADO',
      classificacao: 'DISPONIVEL',
      situacao: 'JA_EXISTE',
      motivo: 'Já existe na base — reutilize CS-0001.',
      decisao: 'REVISAR',
      melhorScore: 1,
      similar: {
        posicao: 1,
        id: 'CS-0001',
        codigoOriginal: null,
        descricao: 'SV EQUIPE TELHADO',
        score: 1,
        scoreTokens: 1,
        scoreCaracteres: 1,
        tokensEmComum: [],
        motivo: 'mesmo item',
        tipo: 'CS'
      }
    },
    {
      linha: 2,
      texto: 'SV MONTAGEM PIPA',
      classificacao: 'CRIAR',
      situacao: 'OK',
      motivo: 'Precisa ser criado.',
      decisao: 'PERMITIR',
      melhorScore: 0,
      similar: null
    },
    {
      linha: 3,
      texto: 'SV REPARO COBERTURA',
      classificacao: 'CRIAR',
      situacao: 'ALERTA',
      motivo: 'Precisa ser criado — há parecidos.',
      decisao: 'ALERTAR',
      melhorScore: 0.75,
      similar: {
        posicao: 1,
        id: 'CS-0002',
        codigoOriginal: null,
        descricao: 'SV REPARO DE COBERTURA',
        score: 0.75,
        scoreTokens: 0.7,
        scoreCaracteres: 0.8,
        tokensEmComum: [],
        motivo: 'parecido',
        tipo: 'CS'
      }
    }
  ]
}

const RESULTADO = {
  total: 3,
  disponiveis: 1,
  criados: 2,
  vinculados: 2,
  aguardandoValidacao: 1,
  bloqueadas: 0,
  falhas: 0,
  itens: []
}

function mockFetch() {
  const chamadas: { url: string; corpo: unknown }[] = []
  const fn = vi.fn(async (url: string, init?: RequestInit) => {
    const rota = String(url)
    if (rota.includes('/itens/similaridade/lote')) {
      return { ok: true, status: 200, json: async () => ANALISE }
    }
    if (rota.includes('/codigos-completos/lote')) {
      chamadas.push({ url: rota, corpo: init?.body ? JSON.parse(String(init.body)) : null })
      return { ok: true, status: 201, json: async () => RESULTADO }
    }
    return { ok: true, status: 200, json: async () => [] }
  })
  return { fn, chamadas }
}

function montar() {
  return mount(VinculacaoMassa, {
    props: {
      gs: { id: 'CIVIL', nome: 'CIVIL' },
      cf: { id: '1440531', nome: 'FORNECEDOR X' },
      um: { id: 'UN', sigla: 'UN', nome: 'Unidade' },
      usuario: '403921'
    }
  })
}

const LISTA = 'SV EQUIPE TELHADO\nSV MONTAGEM PIPA\nSV REPARO COBERTURA'

async function colarLista(wrapper: ReturnType<typeof montar>): Promise<void> {
  await wrapper.find('input').trigger('paste', { clipboardData: { getData: () => LISTA } })
  await flushPromises()
}

describe('VinculacaoMassa — feedback Gerdau: vincular em massa com aviso do que é disponível', () => {
  beforeEach(() => {
    localStorage.setItem('papel', 'ADMIN')
    localStorage.setItem('usuario', '403921')
  })

  it('avisa "há X disponíveis — porém Y precisam ser criados"', async () => {
    vi.stubGlobal('fetch', mockFetch().fn)
    const wrapper = montar()

    await colarLista(wrapper)

    expect(wrapper.text()).toContain('Há 1 disponível(is)')
    expect(wrapper.text()).toContain('2 precisa(m) ser criado(s)')
    expect(wrapper.text()).toContain('1 com aviso de similaridade')
  })

  it('vincula em massa só o que foi marcado, reutilizando o disponível', async () => {
    const mock = mockFetch()
    vi.stubGlobal('fetch', mock.fn)
    const wrapper = montar()
    await colarLista(wrapper)

    await wrapper.findAll('button').find((b) => b.text().includes('Vincular 3 código(s)'))!.trigger('click')
    await flushPromises()
    const confirmar = wrapper.findAll('.modal button').find((b) => b.text().includes('Vincular em massa'))
    expect(confirmar).toBeTruthy()
    await confirmar!.trigger('click')
    await flushPromises()

    const chamada = mock.chamadas.find((c) => c.url.includes('/codigos-completos/lote'))
    expect(chamada).toBeTruthy()
    const corpo = chamada!.corpo as { gsId: string; itens: { csId: string | null; texto: string | null }[] }
    expect(corpo.gsId).toBe('CIVIL')
    expect(corpo.itens).toEqual([
      { csId: 'CS-0001', texto: null },
      { csId: null, texto: 'SV MONTAGEM PIPA' },
      { csId: null, texto: 'SV REPARO COBERTURA' }
    ])
    expect(wrapper.text()).toContain('2 vínculo(s) ativo(s)')
    expect(wrapper.text()).toContain('1 aguardando validação da Governança')
  })
})
