

import type {
  CandidatoSimilar,
  CodigoCompletoResponse,
  CodigoFornecedor,
  CodigoServico,
  CriarCodigoCompletoPayload,
  Estatisticas,
  EventoAuditoria,
  Fechamentos,
  GrupoServico,
  ItemGovernanca,
  ItemImportacao,
  MatrizDePara,
  PreviewResponse,
  ResumoGovernanca,
  ResultadoImportacao,
  SimilaridadeResponse,
  TipoComponente,
  UnidadeMedida
} from '../types'

const BASE = '/api/v1'

export interface ErroApi extends Error {
  status: number
  detalhes?: { code?: string; message?: string; correlationId?: string }
}

function identidade(): Record<string, string> {
  // Identidade de demonstracao (headers). Em producao o papel vem do token autenticado —
  // ver a nota de permissoes no backend: config/AutorizacaoInterceptor.java
  return {
    'X-Usuario-Id': localStorage.getItem('usuario') || '403921',
    'X-Usuario-Papel': localStorage.getItem('papel') || 'USUARIO',
    'X-Correlation-Id': crypto.randomUUID()
  }
}

async function requisitarDetalhe<T>(caminho: string, opcoes: RequestInit = {}): Promise<{ dados: T; status: number }> {
  const { headers, ...resto } = opcoes
  const resposta = await fetch(BASE + caminho, {
    ...resto,
    headers: { 'Content-Type': 'application/json', ...identidade(), ...(headers as Record<string, string>) }
  })
  const dados = resposta.status === 204 ? null : await resposta.json().catch(() => null)
  if (!resposta.ok) {
    const erro = new Error(dados?.message || `Erro ${resposta.status}`) as ErroApi
    erro.status = resposta.status
    erro.detalhes = dados
    throw erro
  }
  return { dados: dados as T, status: resposta.status }
}

async function requisitar<T>(caminho: string, opcoes: RequestInit = {}): Promise<T> {
  return (await requisitarDetalhe<T>(caminho, opcoes)).dados
}

const lista = <T>(caminho: string) => requisitar<T[]>(caminho)
const criar = <T extends object>(caminho: string, dados: unknown, idempotencyKey?: string) =>
  // 200 = requisição repetida (nada novo criado); 201 = criação real
  requisitarDetalhe<T>(caminho, {
    method: 'POST',
    body: JSON.stringify(dados),
    headers: idempotencyKey ? { 'Idempotency-Key': idempotencyKey } : {}
  }).then(({ dados: corpo, status }) => ({ ...corpo, requisicaoRepetida: status === 200 }))

export function chaveIdempotente(prefixo: string, ...partes: (string | null | undefined)[]): string {
  const texto = partes.map((p) => (p ?? '').trim()).join('|')
  let hash = 5381
  for (let i = 0; i < texto.length; i++) hash = ((hash << 5) + hash + texto.charCodeAt(i)) >>> 0
  // o conteúdo entra por inteiro na chave: dois envios diferentes nunca podem
  // colidir na mesma Idempotency-Key (o hash sozinho tem 32 bits e colide)
  return prefixo + hash.toString(36) + '-' + texto
}

export const api = {
  
  gruposServico: {
    listar: (query = '', limite = 20000, incluirTodos = false) =>
      lista<GrupoServico>(`/grupos-servico?query=${encodeURIComponent(query)}&limite=${limite}&incluirTodos=${incluirTodos}`),
    criar: (dados: { nome: string }) => criar<GrupoServico>('/grupos-servico', dados)
  },
  codigosFornecedor: {
    listar: (query = '', limite = 20000, incluirTodos = false) =>
      lista<CodigoFornecedor>(`/codigos-fornecedor?query=${encodeURIComponent(query)}&limite=${limite}&incluirTodos=${incluirTodos}`),
    criar: (dados: { nome: string; codigoSap?: string | null }) => criar<CodigoFornecedor>('/codigos-fornecedor', dados)
  },
  codigosServico: {
    listar: (query = '', limite = 20000, incluirTodos = false) =>
      lista<CodigoServico>(`/codigos-servico?query=${encodeURIComponent(query)}&limite=${limite}&incluirTodos=${incluirTodos}`),
    criar: (dados: { descricao: string }) => criar<CodigoServico>('/codigos-servico', dados)
  },
  unidadesMedida: {
    listar: (query = '', limite = 20000, incluirTodos = false) =>
      lista<UnidadeMedida>(`/unidades-medida?query=${encodeURIComponent(query)}&limite=${limite}&incluirTodos=${incluirTodos}`),
    criar: (dados: { sigla: string; nome?: string }) => criar<UnidadeMedida>('/unidades-medida', dados)
  },

  componentes: {
    excluir: (tipo: 'GS' | 'CF' | 'CS' | 'UM', id: string) => {
      const caminhos = {
        GS: 'grupos-servico',
        CF: 'codigos-fornecedor',
        CS: 'codigos-servico',
        UM: 'unidades-medida'
      } as const
      return requisitar<void>(`/${caminhos[tipo]}/${encodeURIComponent(id)}`, { method: 'DELETE' })
    }
  },

  situacaoComponentes: (tipo: 'GS' | 'CF' | 'CS' | 'UM', ids: string[], situacao: 'DESATIVADO' | 'ATIVO') =>
    requisitar<{ id: string; alterado: boolean; motivo: string }[]>('/catalogo/situacao', {
      method: 'POST',
      body: JSON.stringify({ tipo, ids, situacao })
    }),

  catalogo: {
    /** marca o item como caso especial: ganha o (!) com o motivo no hover */
    marcarExcecao: (id: string, motivo: string) =>
      requisitar<{ marcados: number }>('/catalogo/marcar-excecao', {
        method: 'POST',
        body: JSON.stringify({ ids: [id], motivo })
      }),
    /** resolve o caso especial do item: remove o (!) */
    limparCasoIsolado: (id: string) =>
      requisitar<{ limpos: number }>('/catalogo/marcar-excecao', {
        method: 'POST',
        body: JSON.stringify({ ids: [id], limpar: true })
      }),
    /** quantos códigos completos referenciam o componente */
    vinculos: (tipo: 'GS' | 'CF' | 'CS' | 'UM', id: string) =>
      requisitar<{ total: number }>(`/catalogo/vinculos?tipo=${tipo}&id=${encodeURIComponent(id)}`),
    /** transfere os vínculos para outro componente e (opcionalmente) exclui o de origem */
    transferirVinculos: (dados: { tipo: 'GS' | 'CF' | 'CS' | 'UM'; de: string; para: string; excluirOrigem?: boolean }) =>
      requisitar<{ transferidos: number; mesclados: number; conflitos: string[]; excluido: boolean }>('/catalogo/transferir-vinculos', {
        method: 'POST',
        body: JSON.stringify(dados)
      })
  },

  exclusaoEmMassa: (tipo: 'GS' | 'CF' | 'CS' | 'UM', ids: string[]) =>
    requisitar<{ id: string; excluido: boolean; motivo: string }[]>('/catalogo/exclusao-em-massa', {
      method: 'POST',
      body: JSON.stringify({ tipo, ids })
    }),

  importarCatalogo: (tipo: 'GS' | 'CF' | 'CS' | 'UM', itens: ItemImportacao[]) =>
    requisitar<ResultadoImportacao>('/catalogo/importacao', {
      method: 'POST',
      body: JSON.stringify({ tipo, itens })
    }),

  preview: (payload: CriarCodigoCompletoPayload) =>
    requisitar<PreviewResponse>('/codigos-completos/preview', { method: 'POST', body: JSON.stringify(payload) }),
  criarCodigoCompleto: (payload: CriarCodigoCompletoPayload, idempotencyKey?: string) =>
    criar<CodigoCompletoResponse>('/codigos-completos', payload, idempotencyKey),

  codigosCompletos: {
    buscar: (id: string) =>
      requisitar<CodigoCompletoResponse>(`/codigos-completos/${encodeURIComponent(id)}`),
    atualizarPedido: (id: string, dados: { descricao?: string | null; justificativa?: string | null }) =>
      requisitar<CodigoCompletoResponse>(`/codigos-completos/${encodeURIComponent(id)}`, {
        method: 'PATCH',
        body: JSON.stringify(dados)
      }),
    cancelarPedido: (id: string) =>
      requisitar<CodigoCompletoResponse>(`/codigos-completos/${encodeURIComponent(id)}/cancelar`, {
        method: 'POST'
      })
  },

  pendentes: (origem = 'TODOS', query = '', limite = 50, status = 'PENDENTE', decision?: string, pagina = 0) =>
    lista<ItemGovernanca>(
      `/governanca/pendentes?origem=${origem}&query=${encodeURIComponent(query)}&limite=${limite}` +
      `&status=${status}${decision ? `&decision=${decision}` : ''}&pagina=${pagina}`
    ),
  resumo: () => requisitar<ResumoGovernanca>('/governanca/resumo'),
  estatisticas: () => requisitar<Estatisticas>('/governanca/estatisticas'),
  fechamentos: (de?: string, ate?: string) =>
    requisitar<Fechamentos>(
      `/governanca/fechamentos${de ? `&de=${de}` : ''}${ate ? `&ate=${ate}` : ''}`.replace('&', '?')
    ),
  contarFila: (origem = 'TODOS', status = 'PENDENTE', decision?: string, query = '') =>
    requisitar<{ total: number }>(
      `/governanca/fila/contagem?origem=${origem}&status=${status}` +
      `${decision ? `&decision=${decision}` : ''}&query=${encodeURIComponent(query)}`
    ),
  revisarHistorico: (id: string, corpo: { revisadoPor: string; acao?: string; observacao?: string | null }) =>
    requisitar<ItemGovernanca>(`/governanca/historico/${id}/revisar`, { method: 'POST', body: JSON.stringify(corpo) }),
  reverterRevisao: (id: string, corpo: { revisadoPor: string; observacao?: string | null }) =>
    requisitar<ItemGovernanca>(`/governanca/historico/${id}/reverter`, { method: 'POST', body: JSON.stringify(corpo) }),
  aprovar: (id: string, corpo: { aprovadoPor: string; observacao?: string | null }) =>
    requisitar<ItemGovernanca>(`/governanca/pendentes/${id}/aprovar`, { method: 'POST', body: JSON.stringify(corpo) }),
  rejeitar: (id: string, corpo: { aprovadoPor: string; observacao?: string | null }) =>
    requisitar<ItemGovernanca>(`/governanca/pendentes/${id}/rejeitar`, { method: 'POST', body: JSON.stringify(corpo) }),

  similaridade: (descricao: string, limite = 5, tipo: TipoComponente | string = 'CS') =>
    requisitar<SimilaridadeResponse>(
      `/itens/similaridade?descricao=${encodeURIComponent(descricao)}&limite=${limite}&tipo=${tipo}`
    ),

  eventos: (acao?: string) =>
    lista<EventoAuditoria>(`/governanca/eventos${acao ? `?acao=${acao}` : ''}`),

  matriz: (query = '', limite = 50) =>
    lista<MatrizDePara>(`/matriz-de-para?query=${encodeURIComponent(query)}&limite=${limite}`)
}

export type { CandidatoSimilar }
