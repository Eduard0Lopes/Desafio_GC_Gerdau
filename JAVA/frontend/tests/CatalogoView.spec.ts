import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import CatalogoView from '../src/views/CatalogoView.vue'
import ComboboxBusca from '../src/components/ComboboxBusca.vue'
import { toast } from '../src/services/toast'

const GRUPOS = [
  { id: 'civil', nome: 'CIVIL', nomeNormalizado: 'civil', origem: 'SISTEMA', status: 'ATIVO', createdAt: '2026-01-01T10:00:00Z' },
  {
    id: 'eletrica', nome: 'ELETRICA', nomeNormalizado: 'eletrica', origem: 'SISTEMA', status: 'PENDENTE_VALIDACAO',
    createdAt: '2026-03-01T10:00:00Z', casoIsolado: true, motivoCasoIsolado: 'Aprovado como exceção no comitê.'
  },
  { id: 'hidraulica', nome: 'HIDRAULICA', nomeNormalizado: 'hidraulica', origem: 'SISTEMA', status: 'DESATIVADO', createdAt: '2026-02-01T10:00:00Z' }
]

function mockFetch() {
  return vi.fn(async (url: string, init?: RequestInit) => {
    const rota = String(url)
    if (init?.method === 'DELETE' && rota.includes('/grupos-servico/civil')) {
      // CIVIL tem vínculos: a exclusão precisa de transferência antes
      return {
        ok: false,
        status: 409,
        json: async () => ({ message: 'Vínculo bloqueia a exclusão: o Grupo de Serviço está vinculado a 2 código(s) completo(s).' })
      }
    }
    if (rota.includes('/catalogo/vinculos')) {
      return { ok: true, status: 200, json: async () => ({ total: 2 }) }
    }
    if (rota.includes('/governanca/eventos')) {
      return {
        ok: true,
        status: 200,
        json: async () => ([
          { id: 'e1', actorId: '403921', action: 'EXCLUSAO_COMPONENTE', entityType: 'CODIGO_SERVICO', entityId: 'CS-934526', occurredAt: '2026-10-18T18:00:00Z', decision: null, reason: 'Duplicidade unificada na varredura', candidateIds: [], similarityScores: [], correlationId: null },
          { id: 'e2', actorId: '403921', action: 'TRANSFERENCIA_VINCULOS', entityType: 'CODIGO_COMPLETO', entityId: 'CIVIL_1_CS-1_SV', occurredAt: '2026-10-18T18:00:01Z', decision: 'MESCLAGEM', reason: 'Composicao redundante removida', candidateIds: [], similarityScores: [], correlationId: null },
          { id: 'e3', actorId: '403921', action: 'CRIACAO_COMPONENTE', entityType: 'CODIGO_SERVICO', entityId: 'NAO-APARECE', occurredAt: '2026-10-18T18:00:02Z', decision: null, reason: 'criado', candidateIds: [], similarityScores: [], correlationId: null }
        ])
      }
    }
    if (rota.includes('/catalogo/transferir-vinculos')) {
      return { ok: true, status: 200, json: async () => ({ transferidos: 2, conflitos: [], excluido: true }) }
    }
    if (rota.includes('/grupos-servico')) return { ok: true, status: 200, json: async () => GRUPOS.map((g) => ({ ...g })) }
    if (rota.includes('/catalogo/exclusao-em-massa')) {
      return {
        ok: true,
        status: 200,
        json: async () => [{ id: 'civil', excluido: true, motivo: 'Excluido com sucesso.' }]
      }
    }
    if (rota.includes('/catalogo/situacao')) {
      const corpo = JSON.parse(String(init?.body ?? '{}'))
      return {
        ok: true,
        status: 200,
        json: async () => [{ id: 'civil', alterado: true, motivo: `Situacao ${corpo.situacao} aplicada.` }]
      }
    }
    if (rota.includes('/catalogo/importacao')) {
      const corpo = JSON.parse(String(init?.body ?? '{}'))
      const itens: { nome?: string }[] = corpo.itens ?? []
      return {
        ok: true,
        status: 200,
        json: async () => ({
          tipo: corpo.tipo,
          total: itens.length,
          adicionados: itens.length ? 1 : 0,
          idsAdicionados: itens.length ? ['importado-1'] : [],
          ignorados: itens.slice(1).map((it, i) => ({
            linha: i + 2,
            item: it.nome ?? '',
            motivo: 'Ja existe o item atual foi mantido.'
          }))
        })
      }
    }
    if (init?.method === 'POST' || init?.method === 'DELETE') {
      return { ok: true, status: 200, json: async () => ({}) }
    }
    return { ok: true, status: 200, json: async () => [] }
  })
}

function montar(papel: string) {
  return mount(CatalogoView, { props: { usuario: '403921', papel } })
}

let fetchMock: ReturnType<typeof mockFetch>

describe('CatalogoView — leitura para todos, exclusão em massa só para ADMIN', () => {
  beforeEach(() => {
    fetchMock = mockFetch()
    vi.stubGlobal('fetch', fetchMock)
    toast.estado.itens.splice(0)
  })

  it('lista todos os itens com selo de status (inclusive os em aprovação)', async () => {
    const wrapper = montar('USUARIO')
    await flushPromises()
    expect(wrapper.text()).toContain('CIVIL')
    expect(wrapper.text()).toContain('ELETRICA')
    expect(wrapper.text()).toContain('Ativo')
    expect(wrapper.text()).toContain('Pendente de aprovação')
  })

  it('usuário comum não vê checkboxes nem as ações de admin (desativar/reativar/importar)', async () => {
    const wrapper = montar('USUARIO')
    await flushPromises()
    expect(wrapper.find('input[type="checkbox"]').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('Desativar selecionados')
    expect(wrapper.text()).not.toContain('Reativar selecionados')
    expect(wrapper.text()).not.toContain('Importar CSV')
  })

  it('ADMIN desativa em massa com confirmação (soft delete — nada é apagado)', async () => {
    const wrapper = montar('ADMIN')
    await flushPromises()

    const caixas = wrapper.findAll('input[type="checkbox"]')
    expect(caixas.length).toBeGreaterThan(1) 

    await caixas[1].setValue(true)
    await flushPromises()
    expect(wrapper.text()).toContain('1 selecionado(s)')

    const botao = wrapper.findAll('button').find((b) => b.text().includes('Desativar selecionados'))
    expect(botao).toBeTruthy()
    await botao!.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Desativar itens do catálogo?')
    expect(wrapper.text()).toContain('Nada é perdido')
    const confirmar = wrapper
      .findAll('.modal button')
      .find((b) => b.text().includes('Desativar') && !b.text().includes('selecionados'))
    expect(confirmar).toBeTruthy()
    await confirmar!.trigger('click')
    await flushPromises()

    const chamadas = fetchMock.mock.calls
    const situacao = chamadas.find(([url]) => String(url).includes('/catalogo/situacao'))
    expect(situacao).toBeTruthy()
    expect(String(situacao![1]?.body)).toContain('"DESATIVADO"')
    expect(String(situacao![1]?.body)).toContain('"civil"')
  })

  it('ADMIN reativa em massa os itens desativados', async () => {
    const wrapper = montar('ADMIN')
    await flushPromises()

    const caixas = wrapper.findAll('input[type="checkbox"]')
    await caixas[3].setValue(true) // HIDRAULICA está desativada
    await flushPromises()

    const botao = wrapper.findAll('button').find((b) => b.text().includes('Reativar selecionados'))
    expect(botao).toBeTruthy()
    await botao!.trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('Reativar itens do catálogo?')

    const confirmar = wrapper.findAll('.modal button').find((b) => b.text().trim() === '↻ Reativar')
    await confirmar!.trigger('click')
    await flushPromises()

    const chamadas = fetchMock.mock.calls
    const situacao = chamadas.find(([url]) => String(url).includes('/catalogo/situacao'))
    expect(situacao).toBeTruthy()
    expect(String(situacao![1]?.body)).toContain('"ATIVO"')
  })

  it('botões de desativar/reativar ficam invisíveis sem seleção e aparecem conforme o estado do marcado', async () => {
    const wrapper = montar('ADMIN')
    await flushPromises()
    const textosBotoes = () => wrapper.findAll('button').map((b) => b.text())

    expect(textosBotoes().some((t) => t.includes('Desativar selecionados'))).toBe(false)
    expect(textosBotoes().some((t) => t.includes('Reativar selecionados'))).toBe(false)

    const caixas = wrapper.findAll('input[type="checkbox"]')
    await caixas[1].setValue(true) // CIVIL está ativa
    await flushPromises()
    expect(textosBotoes().some((t) => t.includes('Desativar selecionados'))).toBe(true)
    expect(textosBotoes().some((t) => t.includes('Reativar selecionados'))).toBe(false)

    await caixas[1].setValue(false)
    await caixas[3].setValue(true) // HIDRAULICA está desativada
    await flushPromises()
    expect(textosBotoes().some((t) => t.includes('Desativar selecionados'))).toBe(false)
    expect(textosBotoes().some((t) => t.includes('Reativar selecionados'))).toBe(true)
  })

  it('ordena por "Criado em" ao clicar no cabeçalho e no 3º clique volta à ordem normal', async () => {
    const wrapper = montar('USUARIO')
    await flushPromises()

    const thCriadoEm = wrapper.findAll('th').find((th) => th.text().includes('Criado em'))
    expect(thCriadoEm).toBeTruthy()

    await thCriadoEm!.trigger('click')
    await flushPromises()
    let linhas = wrapper.findAll('tbody tr').map((tr) => tr.text())
    expect(linhas[0]).toContain('ELETRICA') // 2026-03 (mais recentes primeiro)
    expect(linhas[2]).toContain('CIVIL') // 2026-01

    await thCriadoEm!.trigger('click')
    await flushPromises()
    linhas = wrapper.findAll('tbody tr').map((tr) => tr.text())
    expect(linhas[0]).toContain('CIVIL')
    expect(linhas[2]).toContain('ELETRICA')

    await thCriadoEm!.trigger('click')
    await flushPromises()
    linhas = wrapper.findAll('tbody tr').map((tr) => tr.text())
    expect(linhas[0]).toContain('CIVIL')
    expect(linhas[1]).toContain('ELETRICA')
    expect(linhas[2]).toContain('HIDRAULICA')
  })

  it('badge "Exceção aprovada" perseguem o item: linha destacada e badge no ID e na descrição', async () => {
    const wrapper = montar('USUARIO')
    await flushPromises()

    const linhas = wrapper.findAll('tbody tr')
    const linhaExcecao = linhas.find((tr) => tr.text().includes('ELETRICA'))!
    expect(linhaExcecao.classes()).toContain('linha-excecao')
    expect(linhaExcecao.findAll('.badge-excecao').length).toBe(2)

    const linhaNormal = linhas.find((tr) => tr.text().includes('CIVIL'))!
    expect(linhaNormal.classes()).not.toContain('linha-excecao')
    expect(linhaNormal.findAll('.badge-excecao').length).toBe(0)

    // o catálogo explica o que é o !
    expect(wrapper.text()).toContain('Caso especial')
    expect(wrapper.find('.legenda-excecao .badge-excecao').exists()).toBe(true)
  })

  it('exporta CSV da tabela escolhida (qualquer perfil pode exportar)', async () => {
    const criarUrl = vi.fn((_blob: Blob) => 'blob:csv')
    const revogarUrl = vi.fn()
    URL.createObjectURL = criarUrl as unknown as typeof URL.createObjectURL
    URL.revokeObjectURL = revogarUrl

    const wrapper = montar('USUARIO')
    await flushPromises()

    await wrapper.findAll('button').find((b) => b.text().includes('Exportar CSV'))!.trigger('click')
    expect(wrapper.text()).toContain('Qual tabela exportar?')
    await wrapper.findAll('.modal button').find((b) => b.text().includes('Exportar'))!.trigger('click')
    await flushPromises()

    expect(criarUrl).toHaveBeenCalledTimes(1)
    const blob = criarUrl.mock.calls[0][0] as Blob
    const csv = await blob.text()
    expect(csv).toContain('CIVIL')
    expect(csv).toContain('Pendente de aprovação')
  })

  it('importa CSV: avisa que só o novo entra, dá toast e fecha o modal', async () => {
    const wrapper = montar('ADMIN')
    await flushPromises()

    await wrapper.findAll('button').find((b) => b.text().includes('Importar CSV'))!.trigger('click')
    expect(wrapper.text()).toContain('Qual tabela será alimentada?')
    expect(wrapper.text()).toContain('Só é adicionado o que for novo')

    const arquivo = new File(['id;nome\n;novo grupo importado'], 'grupos.csv', { type: 'text/csv' })
    const input = wrapper.get('#arquivo-importar')
    Object.defineProperty(input.element, 'files', { value: [arquivo], configurable: true })
    await input.trigger('change')
    await flushPromises()
    expect(wrapper.text()).toContain('Só o que é novo entra: 1 de 1 linha(s).')
    expect(wrapper.text()).toContain('Importar 1 nova(s)')

    await wrapper.findAll('.modal button').find((b) => b.text().includes('Importar 1 nova'))!.trigger('click')
    await flushPromises()

    const chamadas = (fetchMock as ReturnType<typeof mockFetch>).mock.calls
    const importacao = chamadas.find(([url]) => String(url).includes('/catalogo/importacao'))
    expect(importacao).toBeTruthy()
    expect(String(importacao![1]?.body)).toContain('novo grupo importado')
    expect(toast.estado.itens.some((t) => t.tipo === 'sucesso' && t.mensagem.includes('adicionado'))).toBe(true)
    expect(wrapper.text()).not.toContain('Qual tabela será alimentada?')
  })

  it('com arquivo misto (5 repetidas + 1 nova), avisa que só a linha nova entra', async () => {
    const wrapper = montar('ADMIN')
    await flushPromises()

    await wrapper.findAll('button').find((b) => b.text().includes('Importar CSV'))!.trigger('click')
    const csv = 'id;nome\n;CIVIL\n;ELETRICA\n;CIVIL\n;ELETRICA\n;CIVIL\n;GRUPO NOVO'
    const arquivo = new File([csv], 'grupos.csv', { type: 'text/csv' })
    const input = wrapper.get('#arquivo-importar')
    Object.defineProperty(input.element, 'files', { value: [arquivo], configurable: true })
    await input.trigger('change')
    await flushPromises()

    expect(wrapper.text()).toContain('Só o que é novo entra: 1 de 6 linha(s).')
    expect(wrapper.text()).toContain('5 já existem e serão mantidas')
    expect(wrapper.text()).toContain('já existe: CIVIL')
    expect(wrapper.text()).toContain('Importar 1 nova(s)')

    await wrapper.findAll('.modal button').find((b) => b.text().includes('Importar 1 nova'))!.trigger('click')
    await flushPromises()

    expect(
      toast.estado.itens.some(
        (t) => t.tipo === 'sucesso' && t.mensagem.includes('1 novo(s) adicionado(s)') && t.mensagem.includes('5 já existiam')
      )
    ).toBe(true)
    expect(wrapper.text()).not.toContain('Qual tabela será alimentada?')
  })

  it('bloqueia arquivo da tabela errada (ex.: grupo de serviço em código de serviço)', async () => {
    const wrapper = montar('ADMIN')
    await flushPromises()

    await wrapper.findAll('button').find((b) => b.text().includes('Importar CSV'))!.trigger('click')
    await wrapper.get('#tabela-importar').setValue('CS')

    const arquivo = new File(['id;nome\n;CIVIL'], 'grupos.csv', { type: 'text/csv' })
    const input = wrapper.get('#arquivo-importar')
    Object.defineProperty(input.element, 'files', { value: [arquivo], configurable: true })
    await input.trigger('change')
    await flushPromises()

    expect(
      toast.estado.itens.some((t) => t.tipo === 'erro' && t.mensagem.includes('Grupo de Serviço'))
    ).toBe(true)
    expect(wrapper.text()).not.toContain('Só o que é novo entra')
    expect(wrapper.text()).toContain('Nada novo para importar')
  })

  it('permite desfazer uma importação (volta ao estado anterior)', async () => {
    const wrapper = montar('ADMIN')
    await flushPromises()

    await wrapper.findAll('button').find((b) => b.text().includes('Importar CSV'))!.trigger('click')
    const arquivo = new File(['id;nome\n;novo grupo importado'], 'grupos.csv', { type: 'text/csv' })
    const input = wrapper.get('#arquivo-importar')
    Object.defineProperty(input.element, 'files', { value: [arquivo], configurable: true })
    await input.trigger('change')
    await flushPromises()
    await wrapper.findAll('.modal button').find((b) => b.text().includes('Importar 1 nova'))!.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Importação concluída:')
    await wrapper.findAll('button').find((b) => b.text().includes('Desfazer importação'))!.trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('Desfazer a importação?')

    await wrapper.findAll('.modal button').find((b) => b.text().includes('Desfazer importação'))!.trigger('click')
    await flushPromises()

    const chamadas = (fetchMock as ReturnType<typeof mockFetch>).mock.calls
    const desfazer = chamadas.find(([url]) => String(url).includes('/catalogo/exclusao-em-massa'))
    expect(desfazer).toBeTruthy()
    expect(String(desfazer![1]?.body)).toContain('importado-1')
    expect(toast.estado.itens.some((t) => t.tipo === 'sucesso' && t.mensagem.includes('Importação desfeita'))).toBe(true)
  })

  it('busca entende plural e singular (eletricas ↔ eletrica, civis ↔ civil)', async () => {
    const wrapper = montar('USUARIO')
    await flushPromises()

    await wrapper.get('input.campo-busca').setValue('eletricas')
    await flushPromises()
    expect(wrapper.text()).toContain('ELETRICA')
    expect(wrapper.text()).not.toContain('CIVIL')

    await wrapper.get('input.campo-busca').setValue('civis')
    await flushPromises()
    expect(wrapper.text()).toContain('CIVIL')
    expect(wrapper.text()).not.toContain('ELETRICA')
  })

  it('filtro "Casos especiais" mostra só os itens marcados com (!)', async () => {
    const wrapper = montar('USUARIO')
    await flushPromises()

    const filtro = wrapper.get('.botao-filtro-excecao')
    expect(filtro.text()).toContain('Casos especiais')

    await filtro.trigger('click')
    await flushPromises()
    const linhas = wrapper.findAll('tbody tr')
    expect(linhas.length).toBe(1)
    expect(linhas[0].text()).toContain('ELETRICA')
    expect(filtro.classes()).toContain('ativo')

    await filtro.trigger('click')
    await flushPromises()
    expect(wrapper.findAll('tbody tr').length).toBe(3)
  })

  it('clicar no (!) abre a resolução e remove a marca do item', async () => {
    const wrapper = montar('USUARIO')
    await flushPromises()

    await wrapper.get('.botao-filtro-excecao').trigger('click')
    await flushPromises()
    expect(wrapper.findAll('tbody button.badge-excecao').length).toBeGreaterThan(0)

    await wrapper.get('tbody button.badge-excecao').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('Resolver caso especial?')

    const confirmar = wrapper.findAll('.modal button').find((b) => b.text().includes('Resolver'))!
    await confirmar.trigger('click')
    await flushPromises()

    expect(toast.estado.itens.some((t) => t.mensagem.includes('Caso resolvido'))).toBe(true)
    expect(wrapper.findAll('tbody button.badge-excecao').length).toBe(0)
    expect(fetchMock.mock.calls.some(([u]) => String(u).includes('/catalogo/marcar-excecao'))).toBe(true)
  })

  it('no (!) também dá para excluir definitivamente, com confirmação dupla', async () => {
    const wrapper = montar('USUARIO')
    await flushPromises()

    await wrapper.get('.botao-filtro-excecao').trigger('click')
    await flushPromises()
    await wrapper.get('tbody button.badge-excecao').trigger('click')
    await flushPromises()

    const excluir = wrapper.findAll('.modal button').find((b) => b.text().includes('Excluir definitivamente'))!
    await excluir.trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('Excluir definitivamente?')
    expect(wrapper.text()).toContain('NÃO pode ser desfeita')

    const confirmar = wrapper.findAll('.modal button').find((b) => b.text().includes('Excluir para sempre'))!
    await confirmar.trigger('click')
    await flushPromises()

    expect(toast.estado.itens.some((t) => t.mensagem.includes('excluído definitivamente'))).toBe(true)
    expect(
      fetchMock.mock.calls.some(([u, i]) => String(u).includes('/grupos-servico/') && (i as RequestInit)?.method === 'DELETE')
    ).toBe(true)
  })

  it('permite marcar manualmente um item com (!) e registrar o motivo', async () => {
    const wrapper = montar('USUARIO')
    await flushPromises()

    await wrapper.get('tbody .btn-marcar-excecao').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('Marcar como caso especial')

    await wrapper.get('.modal textarea').setValue('Conferir se é duplicidade do item X')
    await marcar(wrapper)
    expect(toast.estado.itens.some((t) => t.mensagem.includes('marcado com'))).toBe(true)
    expect(
      fetchMock.mock.calls.some(([u, i]) =>
        String(u).includes('/catalogo/marcar-excecao') &&
        String((i as RequestInit)?.body ?? '').includes('Conferir se é duplicidade do item X'))
    ).toBe(true)

    const linha = wrapper.findAll('tbody tr').find((tr) => tr.text().includes('CIVIL'))!
    expect(linha.findAll('button.badge-excecao').length).toBe(2)
    expect(linha.findAll('.btn-marcar-excecao').length).toBe(0)
  })

  it('✕ limpa a barra de busca', async () => {
    const wrapper = montar('USUARIO')
    await flushPromises()

    await wrapper.get('input.campo-busca').setValue('civil')
    await flushPromises()
    expect(wrapper.find('.btn-limpar-busca').exists()).toBe(true)

    await wrapper.get('.btn-limpar-busca').trigger('click')
    await flushPromises()
    expect((wrapper.get('input.campo-busca').element as HTMLInputElement).value).toBe('')
    expect(wrapper.findAll('tbody tr').length).toBe(3)
  })

  it('item com vínculos abre o modal para transferir antes de excluir', async () => {
    const wrapper = montar('USUARIO')
    await flushPromises()

    // marca CIVIL e pede a exclusão dele (o mock bloqueia CIVIL por vínculo)
    await wrapper.get('tbody .btn-marcar-excecao').trigger('click')
    await wrapper.get('.modal textarea').setValue('testar transferencia')
    await marcar(wrapper)

    const linha = wrapper.findAll('tbody tr').find((tr) => tr.text().includes('CIVIL'))!
    await linha.get('button.badge-excecao').trigger('click')
    await wrapper.findAll('.modal button').find((b) => b.text().includes('Excluir definitivamente'))!.trigger('click')
    await wrapper.findAll('.modal button').find((b) => b.text().includes('Excluir para sempre'))!.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Transferir vínculos para excluir')

    const cb = wrapper.findComponent(ComboboxBusca)
    await cb.get('input').trigger('focus')
    await flushPromises()
    await cb.findAll('.cb-lista li')[1].trigger('mousedown')
    await flushPromises()

    const transferir = wrapper.findAll('.modal button').find((b) => b.text().includes('Transferir'))!
    expect(transferir.attributes('disabled')).toBeUndefined()
    await transferir.trigger('click')
    await flushPromises()

    expect(
      fetchMock.mock.calls.some(([u, i]) =>
        String(u).includes('/catalogo/transferir-vinculos') && (i as RequestInit)?.method === 'POST')
    ).toBe(true)
    expect(toast.estado.itens.some((t) => t.mensagem.includes('excluído definitivamente'))).toBe(true)
  })

  it('lista os itens alterados e excluídos na base (histórico)', async () => {
    const wrapper = montar('USUARIO')
    await flushPromises()

    await wrapper.findAll('button').find((b) => b.text().includes('Alterações'))!.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Itens alterados e excluídos na base')
    expect(wrapper.text()).toContain('CS-934526')
    expect(wrapper.text()).toContain('Excluído')
    expect(wrapper.text()).toContain('Mesclado')
    expect(wrapper.text()).not.toContain('NAO-APARECE')
  })
})

async function marcar(wrapper: ReturnType<typeof montar>): Promise<void> {
  const botao = wrapper.findAll('.modal button').find((b) => b.text().includes('Marcar com (!)'))!
  await botao.trigger('click')
  await flushPromises()
}
