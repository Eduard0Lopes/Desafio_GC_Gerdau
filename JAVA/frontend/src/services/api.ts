import type {
  SimilarCandidate,
  FullCodeResponse,
  SupplierCode,
  ServiceCode,
  CreateFullCodePayload,
  Statistics,
  AuditEvent,
  Closings,
  ServiceGroup,
  GovernanceItem,
  ImportItem,
  SimilarityBatch,
  MappingMatrix,
  PreviewResponse,
  GovernanceSummary,
  ImportResult,
  BatchLinkResult,
  SimilarityResponse,
  TipoComponente,
  UnitOfMeasure
} from '../types'
import { currentRole, currentUser } from './usuario'

const BASE = '/api/v1'

export interface ApiError extends Error {
  status: number
  detalhes?: { code?: string; message?: string; correlationId?: string }
}

export function isConflict(error: unknown): boolean {
  return (error as { status?: number } | null)?.status === 409
}

export function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : String(error)
}

function identityHeaders(): Record<string, string> {
  return {
    'X-Usuario-Id': currentUser(),
    'X-Usuario-Papel': currentRole(),
    'X-Correlation-Id': crypto.randomUUID()
  }
}

async function requestWithStatus<T>(path: string, options: RequestInit = {}): Promise<{ data: T; status: number }> {
  const { headers, ...resto } = options
  const response = await fetch(BASE + path, {
    ...resto,
    headers: { 'Content-Type': 'application/json', ...identityHeaders(), ...(headers as Record<string, string>) }
  })
  const data = response.status === 204 ? null : await response.json().catch(() => null)
  if (!response.ok) {
    const error = new Error(data?.message || `Erro ${response.status}`) as ApiError
    error.status = response.status
    error.detalhes = data
    throw error
  }
  return { data: data as T, status: response.status }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  return (await requestWithStatus<T>(path, options)).data
}

const list = <T>(path: string) => request<T[]>(path)

const encodePath = (value: string) => encodeURIComponent(value)

const create = <T extends object>(path: string, data: unknown, idempotencyKey?: string) =>
  requestWithStatus<T>(path, {
    method: 'POST',
    body: JSON.stringify(data),
    headers: idempotencyKey ? { 'Idempotency-Key': idempotencyKey } : {}
  }).then(({ data: body, status }) => ({ ...body, requisicaoRepetida: status === 200 }))

export function buildIdempotencyKey(prefix: string, ...parts: (string | null | undefined)[]): string {
  const content = parts.map((parte) => (parte ?? '').trim()).join('|')
  let hash = 5381
  for (let index = 0; index < content.length; index++) hash = ((hash << 5) + hash + content.charCodeAt(index)) >>> 0
  return prefix + hash.toString(36) + '-' + content
}

export const api = {
  serviceGroups: {
    list: (query = '', limite = 20000, incluirTodos = false) =>
      list<ServiceGroup>(`/grupos-servico?query=${encodeURIComponent(query)}&limite=${limite}&incluirTodos=${incluirTodos}`),
    create: (data: { nome: string }) => create<ServiceGroup>('/grupos-servico', data)
  },
  supplierCodes: {
    list: (query = '', limite = 20000, incluirTodos = false) =>
      list<SupplierCode>(`/codigos-fornecedor?query=${encodeURIComponent(query)}&limite=${limite}&incluirTodos=${incluirTodos}`),
    create: (data: { nome: string; codigoSap?: string | null }) => create<SupplierCode>('/codigos-fornecedor', data)
  },
  serviceCodes: {
    list: (query = '', limite = 20000, incluirTodos = false) =>
      list<ServiceCode>(`/codigos-servico?query=${encodeURIComponent(query)}&limite=${limite}&incluirTodos=${incluirTodos}`),
    create: (data: { descricao: string }) => create<ServiceCode>('/codigos-servico', data)
  },
  unitsOfMeasure: {
    list: (query = '', limite = 20000, incluirTodos = false) =>
      list<UnitOfMeasure>(`/unidades-medida?query=${encodeURIComponent(query)}&limite=${limite}&incluirTodos=${incluirTodos}`),
    create: (data: { sigla: string; nome?: string }) => create<UnitOfMeasure>('/unidades-medida', data)
  },

  components: {
    remove: (tipo: TipoComponente, id: string) => {
      const paths = {
        GS: 'grupos-servico',
        CF: 'codigos-fornecedor',
        CS: 'codigos-servico',
        UM: 'unidades-medida'
      } as const
      return request<void>(`/${paths[tipo]}/${encodePath(id)}`, { method: 'DELETE' })
    }
  },

  setComponentStatus: (tipo: TipoComponente, ids: string[], situacao: 'DESATIVADO' | 'ATIVO') =>
    request<{ id: string; alterado: boolean; motivo: string }[]>('/catalogo/situacao', {
      method: 'POST',
      body: JSON.stringify({ tipo, ids, situacao })
    }),

  catalogo: {
    markException: (id: string, motivo: string) =>
      request<{ marcados: number }>('/catalogo/marcar-excecao', {
        method: 'POST',
        body: JSON.stringify({ ids: [id], motivo })
      }),
    clearSpecialCase: (id: string) =>
      request<{ limpos: number }>('/catalogo/marcar-excecao', {
        method: 'POST',
        body: JSON.stringify({ ids: [id], clearAll: true })
      }),
    vinculos: (tipo: TipoComponente, id: string) =>
      request<{ total: number }>(`/catalogo/vinculos?tipo=${tipo}&id=${encodePath(id)}`),
    transferLinks: (data: { tipo: TipoComponente; de: string; para: string; excluirOrigem?: boolean }) =>
      request<{ transferidos: number; mesclados: number; conflitos: string[]; excluido: boolean }>('/catalogo/transferir-vinculos', {
        method: 'POST',
        body: JSON.stringify(data)
      })
  },

  bulkDelete: (tipo: TipoComponente, ids: string[]) =>
    request<{ id: string; excluido: boolean; motivo: string }[]>('/catalogo/exclusao-em-massa', {
      method: 'POST',
      body: JSON.stringify({ tipo, ids })
    }),

  importCatalog: (tipo: TipoComponente, itens: ImportItem[]) =>
    request<ImportResult>('/catalogo/importacao', {
      method: 'POST',
      body: JSON.stringify({ tipo, itens })
    }),

  preview: (buildPayload: CreateFullCodePayload) =>
    request<PreviewResponse>('/codigos-completos/preview', { method: 'POST', body: JSON.stringify(buildPayload) }),
  createFullCode: (buildPayload: CreateFullCodePayload, idempotencyKey?: string) =>
    create<FullCodeResponse>('/codigos-completos', buildPayload, idempotencyKey),

  fullCodes: {
    getById: (id: string) =>
      request<FullCodeResponse>(`/codigos-completos/${encodePath(id)}`),
    updateRequest: (id: string, data: { descricao?: string | null; justificativa?: string | null }) =>
      request<FullCodeResponse>(`/codigos-completos/${encodePath(id)}`, {
        method: 'PATCH',
        body: JSON.stringify(data)
      }),
    cancelRequest: (id: string) =>
      request<FullCodeResponse>(`/codigos-completos/${encodePath(id)}/cancelar`, {
        method: 'POST'
      })
  },

  pendentes: (origem = 'TODOS', query = '', limite = 50, status = 'PENDENTE', decision?: string, page = 0) =>
    list<GovernanceItem>(
      `/governanca/pendentes?origem=${origem}&query=${encodeURIComponent(query)}&limite=${limite}` +
      `&status=${status}${decision ? `&decision=` : ''}&pagina=${page}`
    ),
  summary: () => request<GovernanceSummary>('/governanca/resumo'),
  statistics: () => request<Statistics>('/governanca/estatisticas'),
  closings: (de?: string, ate?: string) =>
    request<Closings>(
      `/governanca/fechamentos${de ? `&de=` : ''}${ate ? `&ate=` : ''}`.replace('&', '?')
    ),
  countQueue: (origem = 'TODOS', status = 'PENDENTE', decision?: string, query = '') =>
    request<{ total: number }>(
      `/governanca/fila/contagem?origem=${origem}&status=${status}` +
      `${decision ? `&decision=` : ''}&query=${encodeURIComponent(query)}`
    ),
  reviewHistory: (id: string, body: { revisadoPor: string; acao?: string; observacao?: string | null }) =>
    request<GovernanceItem>(`/governanca/historico/${encodePath(id)}/revisar`, { method: 'POST', body: JSON.stringify(body) }),
  revertReview: (id: string, body: { revisadoPor: string; observacao?: string | null }) =>
    request<GovernanceItem>(`/governanca/historico/${encodePath(id)}/reverter`, { method: 'POST', body: JSON.stringify(body) }),
  approve: (id: string, body: { aprovadoPor: string; observacao?: string | null }) =>
    request<GovernanceItem>(`/governanca/pendentes/${encodePath(id)}/aprovar`, { method: 'POST', body: JSON.stringify(body) }),
  reject: (id: string, body: { aprovadoPor: string; observacao?: string | null }) =>
    request<GovernanceItem>(`/governanca/pendentes/${encodePath(id)}/rejeitar`, { method: 'POST', body: JSON.stringify(body) }),
  reuseCandidate: (id: string, body: { escolhidoId: string; escolhidoTipo?: string | null; aprovadoPor: string; observacao?: string | null }) =>
    request<GovernanceItem>(`/governanca/pendentes/${encodePath(id)}/reutilizar`, { method: 'POST', body: JSON.stringify(body) }),

  similarity: (descricao: string, limite = 5, tipo: TipoComponente | string = 'CS') =>
    request<SimilarityResponse>(
      `/itens/similaridade?descricao=${encodePath(descricao)}&limite=${limite}&tipo=${tipo}`
    ),
  similarityBatch: (textos: string[], tipo: TipoComponente | string = 'CS', limite = 3) =>
    request<SimilarityBatch>('/itens/similaridade/lote', {
      method: 'POST',
      body: JSON.stringify({ textos, tipo, limite })
    }),

  linkBatch: (body: {
    gsId: string
    cfId: string
    umId: string
    itens: { csId?: string | null; texto?: string | null }[]
    usuarioCriacaoId?: string | null
  }, idempotencyKey?: string) =>
    request<BatchLinkResult>('/codigos-completos/lote', {
      method: 'POST',
      body: JSON.stringify(body),
      headers: idempotencyKey ? { 'Idempotency-Key': idempotencyKey } : {}
    }),

  events: (acao?: string) =>
    list<AuditEvent>(`/governanca/eventos${acao ? `?acao=` : ''}`),

  matriz: (query = '', limite = 50) =>
    list<MappingMatrix>(`/matriz-de-para?query=${encodeURIComponent(query)}&limite=${limite}`)
}

export type { SimilarCandidate }
