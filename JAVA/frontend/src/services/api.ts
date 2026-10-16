/**
 * Cliente da API — contrato /api/v1 (documento técnico, seção 10).
 * Envia identidade/perfil (seção 12.2) e correlationId em todas as requisições.
 */
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
  MatrizDePara,
  PreviewResponse,
  ResumoGovernanca,
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
  return {
    'X-Usuario-Id': localStorage.getItem('usuario') || '403921',
    'X-Usuario-Papel': localStorage.getItem('papel') || 'USUARIO',
    'X-Correlation-Id': crypto.randomUUID()
  }
}

async function requisitar<T>(caminho: string, opcoes: RequestInit = {}): Promise<T> {
  const { headers, ...resto } = opcoes
  const resposta = await fetch(BASE + caminho, {
    ...resto,
    headers: { 'Content-Type': 'application/json', ...identidade(), ...(headers as Record<string, string>) }
  })
  if (resposta.status === 204) return null as T
  const dados = await resposta.json().catch(() => null)
  if (!resposta.ok) {
    const erro = new Error(dados?.message || `Erro ${resposta.status}`) as ErroApi
    erro.status = resposta.status
    erro.detalhes = dados
    throw erro
  }
  return dados as T
}

const lista = <T>(caminho: string) => requisitar<T[]>(caminho)
const criar = <T>(caminho: string, dados: unknown, idempotencyKey?: string) =>
  requisitar<T>(caminho, {
    method: 'POST',
    body: JSON.stringify(dados),
    headers: idempotencyKey ? { 'Idempotency-Key': idempotencyKey } : {}
  })

/**
 * Chave de idempotência estável por conteúdo: reenviar a MESMA solicitação
 * (ex.: repetir o envio para Governança) devolve o resultado original em vez
 * de duplicar o cadastro. Conteúdo diferente gera chave diferente.
 */
export function chaveIdempotente(prefixo: string, ...partes: (string | null | undefined)[]): string {
  const texto = partes.map((p) => (p ?? '').trim()).join('|')
  let hash = 5381
  for (let i = 0; i < texto.length; i++) hash = ((hash << 5) + hash + texto.charCodeAt(i)) >>> 0
  return prefixo + hash.toString(36) + '-' + texto.length.toString(36)
}

export const api = {
  // catálogos (seção 10.1 — autocomplete + cadastro com anti-duplicidade)
  gruposServico: {
    listar: (query = '', limite = 20000) =>
      lista<GrupoServico>(`/grupos-servico?query=${encodeURIComponent(query)}&limite=${limite}`),
    criar: (dados: { nome: string }) => criar<GrupoServico>('/grupos-servico', dados)
  },
  codigosFornecedor: {
    listar: (query = '', limite = 20000) =>
      lista<CodigoFornecedor>(`/codigos-fornecedor?query=${encodeURIComponent(query)}&limite=${limite}`),
    criar: (dados: { nome: string; codigoSap?: string | null }) => criar<CodigoFornecedor>('/codigos-fornecedor', dados)
  },
  codigosServico: {
    listar: (query = '', limite = 20000) =>
      lista<CodigoServico>(`/codigos-servico?query=${encodeURIComponent(query)}&limite=${limite}`),
    criar: (dados: { descricao: string }) => criar<CodigoServico>('/codigos-servico', dados)
  },
  unidadesMedida: {
    listar: (query = '', limite = 20000) =>
      lista<UnidadeMedida>(`/unidades-medida?query=${encodeURIComponent(query)}&limite=${limite}`),
    criar: (dados: { sigla: string; nome?: string }) => criar<UnidadeMedida>('/unidades-medida', dados)
  },

  // exclusão de componentes do catálogo — permitida apenas ao perfil ADMIN (seção 12.2)
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

  // código completo + governança
  preview: (payload: CriarCodigoCompletoPayload) =>
    requisitar<PreviewResponse>('/codigos-completos/preview', { method: 'POST', body: JSON.stringify(payload) }),
  criarCodigoCompleto: (payload: CriarCodigoCompletoPayload, idempotencyKey?: string) =>
    criar<CodigoCompletoResponse>('/codigos-completos', payload, idempotencyKey),

  // governança (histórico da matriz + novos cadastros) — requer perfil ADMIN
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

  // similaridade (CS | GS | CF | UM) — usada na hora de criar
  similaridade: (descricao: string, limite = 5, tipo: TipoComponente | string = 'CS') =>
    requisitar<SimilaridadeResponse>(
      `/itens/similaridade?descricao=${encodeURIComponent(descricao)}&limite=${limite}&tipo=${tipo}`
    ),

  // auditoria (filtros por ação)
  eventos: (acao?: string) =>
    lista<EventoAuditoria>(`/governanca/eventos${acao ? `?acao=${acao}` : ''}`),

  // matriz de-para (rastreabilidade — seção 9.6)
  matriz: (query = '', limite = 50) =>
    lista<MatrizDePara>(`/matriz-de-para?query=${encodeURIComponent(query)}&limite=${limite}`)
}

export type { CandidatoSimilar }
