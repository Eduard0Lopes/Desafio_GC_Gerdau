import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import VinculacaoMassa from '../src/views/VinculacaoMassa.vue'
import { toast } from '../src/services/toast'

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
  reutilizados: 1,
  criados: 2,
  vinculados: 2,
  aguardandoValidacao: 1,
  itens: [
    {
      linha: 1,
      texto: 'SV EQUIPE TELHADO',
      classificacao: 'DISPONIVEL',
      decisao: null,
      motivo: 'Código existente escolhido para reutilizar.',
      csId: 'CS-0001',
      idCompleto: 'CIVIL_1440531_CS-0001_UN',
      requisicaoRepetida: false
    },
    {
      linha: 2,
      texto: 'SV MONTAGEM PIPA',
      classificacao: 'CRIAR',
      decisao: 'PERMITIR',
      motivo: 'Precisa ser criado.',
      csId: 'CS-0010',
      idCompleto: 'CIVIL_1440531_CS-0010_UN',
      requisicaoRepetida: false
    },
    {
      linha: 3,
      texto: 'SV REPARO COBERTURA',
      classificacao: 'CRIAR',
      decisao: 'ALERTAR',
      motivo: 'Encaminhado para validação da Governança.',
      csId: 'CS-0011',
      idCompleto: 'CIVIL_1440531_CS-0011_UN',
      requisicaoRepetida: false
    }
  ]
}

function mockFetch(resultado: unknown = RESULTADO) {
  const chamadas: { url: string; corpo: unknown }[] = []
  const fn = vi.fn(async (url: string, init?: RequestInit) => {
    const rota = String(url)
    if (rota.includes('/itens/similaridade/lote')) {
      return { ok: true, status: 200, json: async () => ANALISE }
    }
    if (rota.includes('/codigos-completos/lote')) {
      chamadas.push({ url: rota, corpo: init?.body ? JSON.parse(String(init.body)) : null })
      return { ok: true, status: 201, json: async () => resultado }
    }
    return { ok: true, status: 200, json: async () => [] }
  })
  return { fn, chamadas }
}

function montar(props = {
  gs: { id: 'CIVIL', nome: 'CIVIL' },
  cf: { id: '1440531', nome: 'FORNECEDOR X' },
  um: { id: 'UN', sigla: 'UN', nome: 'Unidade' },
  usuario: '403921'
}) {
  return mount(VinculacaoMassa, { props })
}

const LISTA = 'SV EQUIPE TELHADO\nSV MONTAGEM PIPA\nSV REPARO COBERTURA'

async function colarLista(wrapper: ReturnType<typeof montar>): Promise<void> {
  await wrapper.find('input').trigger('paste', { clipboardData: { getData: () => LISTA } })
  await flushPromises()
}

/** a ação principal fica no rodapé da seleção — fora dos modais */
function botaoVincular(wrapper: ReturnType<typeof montar>) {
  return wrapper.find('.massa-rodape .botao-roxo')
}

async function abrirPrevia(wrapper: ReturnType<typeof montar>): Promise<void> {
  await botaoVincular(wrapper).trigger('click')
  await flushPromises()
}

async function confirmar(wrapper: ReturnType<typeof montar>): Promise<void> {
  const botao = wrapper.findAll('.modal button').find((b) => b.text().includes('Confirmar'))
  expect(botao).toBeTruthy()
  await botao!.trigger('click')
  await flushPromises()
}

function avisos(): string[] {
  return toast.estado.itens.map((t) => t.mensagem)
}

describe('VinculacaoMassa — mesmo fluxo do vínculo único: prévia, confirmação e sucesso', () => {
  beforeEach(() => {
    localStorage.setItem('papel', 'ADMIN')
    localStorage.setItem('usuario', '403921')
    toast.estado.itens.splice(0)
  })

  it('mostra na prévia o que será reutilizado, criado e enviado à Governança', async () => {
    vi.stubGlobal('fetch', mockFetch().fn)
    const wrapper = montar()
    await colarLista(wrapper)

    // o item com similaridade alertada fica marcado na lista
    expect(wrapper.find('.tag-governanca').exists()).toBe(true)

    await abrirPrevia(wrapper)
    const previa = wrapper.find('.modal')
    expect(previa.text()).toContain('CIVIL / FORNECEDOR X / UN')
    expect(previa.text()).toContain('1 reutilizar')
    expect(previa.text()).toContain('2 criar')
    expect(previa.text()).toContain('1 Governança')
  })

  it('vincula em massa e mantém a seleção — o mesmo lote pode ser vinculado de novo', async () => {
    const mock = mockFetch()
    vi.stubGlobal('fetch', mock.fn)
    const wrapper = montar()
    await colarLista(wrapper)

    await abrirPrevia(wrapper)
    await confirmar(wrapper)

    const chamada = mock.chamadas.find((c) => c.url.includes('/codigos-completos/lote'))
    expect(chamada).toBeTruthy()
    const corpo = chamada!.corpo as { gsId: string; itens: { csId: string | null; texto: string | null }[] }
    expect(corpo.gsId).toBe('CIVIL')
    expect(corpo.itens).toEqual([
      { csId: 'CS-0001', texto: null },
      { csId: null, texto: 'SV MONTAGEM PIPA' },
      { csId: null, texto: 'SV REPARO COBERTURA' }
    ])
    // o resultado entra no mesmo modal de sucesso do vínculo único
    expect(wrapper.text()).toContain('2 vínculos ativos')
    expect(wrapper.text()).toContain('1 aguardando validação da Governança')
    expect(wrapper.text()).toContain('1 reutilizado(s), 2 criado(s)')

    // nada é barrado: a seleção continua pronta para um novo envio igual
    await wrapper.find('.modal .botao-escuro').trigger('click')
    await flushPromises()
    expect(botaoVincular(wrapper).text()).toBe('Vincular 3 códigos')
    expect(botaoVincular(wrapper).attributes('disabled')).toBeUndefined()
  })

  it('pendências ficam no modal de sucesso, sem fechar sozinho', async () => {
    const resultado = {
      ...RESULTADO,
      vinculados: 1,
      aguardandoValidacao: 0,
      itens: [
        ...RESULTADO.itens.slice(0, 2),
        {
          linha: 3,
          texto: 'SV REPARO COBERTURA',
          classificacao: 'FALHA',
          decisao: null,
          motivo: 'recusado pelo servidor',
          csId: null,
          idCompleto: null,
          requisicaoRepetida: false
        }
      ]
    }
    vi.stubGlobal('fetch', mockFetch(resultado).fn)
    const wrapper = montar()
    await colarLista(wrapper)
    await abrirPrevia(wrapper)
    await confirmar(wrapper)

    expect(wrapper.find('.lista-pendencias').text()).toContain('SV REPARO COBERTURA')
    expect(wrapper.find('.lista-pendencias').text()).toContain('recusado pelo servidor')
    expect(wrapper.text()).toContain('Fechar')
  })

  it('colar linhas repetidas não é barrado — cada ocorrência vira um vínculo', async () => {
    const mock = mockFetch()
    vi.stubGlobal('fetch', mock.fn)
    const wrapper = montar()

    await wrapper.find('input').trigger('paste', {
      clipboardData: { getData: () => 'SV MONTAGEM PIPA\nSV MONTAGEM PIPA' }
    })
    await flushPromises()

    expect(wrapper.find('.massa-resumo').text()).toContain('2 códigos')
    await abrirPrevia(wrapper)
    await confirmar(wrapper)

    const chamada = mock.chamadas.find((c) => c.url.includes('/codigos-completos/lote'))
    const corpo = chamada!.corpo as { itens: { texto: string | null }[] }
    expect(corpo.itens).toEqual([
      { csId: null, texto: 'SV MONTAGEM PIPA' },
      { csId: null, texto: 'SV MONTAGEM PIPA' }
    ])
  })

  it('a ação fica numa única linha de botões, igual ao vínculo único', async () => {
    vi.stubGlobal('fetch', mockFetch().fn)
    const wrapper = montar()

    const botoes = wrapper.findAll('.massa-rodape .acoes .botao')
    expect(botoes.map((b) => b.text())).toEqual([
      'Vincular em massa',
      'Cadastrar Novo Item',
      'Limpar tudo'
    ])
  })

  it('o botão nunca fica desabilitado — o clique diz o que falta', async () => {
    vi.stubGlobal('fetch', mockFetch().fn)
    const wrapper = montar()

    expect(botaoVincular(wrapper).attributes('disabled')).toBeUndefined()

    // sem nada marcado
    await abrirPrevia(wrapper)
    expect(avisos().some((m) => m.includes('Marque ao menos um Código de Serviço'))).toBe(true)
    expect(wrapper.find('.modal').exists()).toBe(false)

    // sem composição escolhida
    toast.estado.itens.splice(0)
    await colarLista(wrapper)
    await wrapper.setProps({ gs: null })
    await abrirPrevia(wrapper)
    expect(avisos().some((m) => m.includes('Falta escolher Grupo de Serviço'))).toBe(true)
    expect(wrapper.find('.modal').exists()).toBe(false)
    expect(wrapper.find('.massa-resumo').text()).toContain('Falta escolher Grupo de Serviço')
  })
})
