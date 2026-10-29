export type Item = Record<string, unknown>

export interface ServiceGroup {
  id: string
  nome: string
  nomeNormalizado: string
  origem: string
  status?: string
  createdAt?: string | null
  criadoPor?: string | null
}

export interface SupplierCode {
  id: string
  codigoFornecedorSAP: number | null
  nome: string
  nomeNormalizado: string
  origem: string
  status?: string
  createdAt?: string | null
  criadoPor?: string | null
}

export interface ServiceCode {
  id: string
  descricao: string
  descricaoNormalizada: string
  quantidadeCaracteres: number
  origem: string
  legacyIds: string[]
  status?: string
  createdAt?: string | null
  criadoPor?: string | null
  casoIsolado?: boolean
  motivoCasoIsolado?: string | null
}

export interface UnitOfMeasure {
  id: string
  sigla: string
  nome: string
  nomeNormalizado: string
  origem: string
  status?: string
  createdAt?: string | null
  criadoPor?: string | null
}

export interface SimilarCandidate {
  posicao: number
  id: string
  codigoOriginal: string | null
  descricao: string
  score: number
  scoreTokens: number
  scoreCaracteres: number
  tokensEmComum: string[]
  motivo: string
  casoIsolado?: boolean
  motivoCasoIsolado?: string | null
  tipo?: string | null
}

export interface BatchItem {
  linha: number
  texto: string
  classificacao: 'DISPONIVEL' | 'CRIAR' | 'BLOQUEADA'
  situacao: string
  motivo: string
  decisao: string | null
  melhorScore: number
  similar: SimilarCandidate | null
}

export interface SimilarityBatch {
  tipo: string
  total: number
  available: number
  toCreate: number
  bloqueadas: number
  itens: BatchItem[]
}

export interface BulkCodeItem {
  key: string
  texto: string
  csId: string | null
  classificacao: 'DISPONIVEL' | 'CRIAR'
  situacao: string
  motivo: string
  match: { id: string; descricao: string; score: number } | null
}

export interface BatchLinkItem {
  linha: number
  texto: string
  classificacao: 'DISPONIVEL' | 'CRIAR' | 'BLOQUEADA' | 'FALHA'
  decisao: string | null
  motivo: string
  csId: string | null
  idCompleto: string | null
  requisicaoRepetida: boolean
}

export interface BatchLinkResult {
  total: number
  reutilizados: number
  criados: number
  vinculados: number
  aguardandoValidacao: number
  itens: BatchLinkItem[]
}

export interface SimilarityResponse {
  descricao: string
  descricaoNormalizada: string
  quantidadeCaracteres: number
  excedeLimiteSap: boolean
  status: string
  decisao: string
  acaoRecomendada: string
  limiares: Record<string, number>
  totalItensAvaliados: number
  candidatos: SimilarCandidate[]
}

export interface ComponentPreview {
  id: string
  nome: string
  criadoNovo: boolean
}

export interface ComponentAlert {
  tipo: string
  descricaoInformada: string
  itemSimilarId: string
  itemSimilarNome: string
  score: number
  decisao: string
  motivo: string
  casoIsolado?: boolean
  motivoCasoIsolado?: string | null
}

export interface PreviewResponse {
  idCompleto: string
  grupoServico: ComponentPreview
  codigoFornecedor: ComponentPreview
  codigoServico: ComponentPreview
  unidadeMedida: ComponentPreview
  descricaoSnapshot: string
  quantidadeCaracteres: number
  excedeLimiteSap: boolean
  idCompletoJaExiste: boolean
  statusExistente?: string | null
  status: string
  decisao: string
  acaoRecomendada: string
  melhorScore: number
  similares: SimilarCandidate[]
  alertasComponentes: ComponentAlert[]
}

export interface FullCodeResponse {
  id: string
  idCompleto: string
  gsId: string
  cfId: string
  csId: string
  umId: string
  descricaoSnapshot: string
  origem: string
  legacyIds: string[]
  status: string
  decisaoSimilaridade: string | null
  melhorScore: number | null
  justificativa: string | null
  usuarioCriacaoId: string | null
  createdAt: string | null
  aprovadoPor: string | null
  observacaoAprovacao: string | null
  requisicaoRepetida?: boolean
}

export interface GovernanceItem {
  origem: string
  id: string
  identificador: string
  descricao: string
  grupoServico: string | null
  fornecedor: string | null
  unidadeMedida: string | null
  legacyId: string | null
  legacyDescription: string | null
  decision: string
  score: number
  reason: string
  solicitante: string | null
  justificativa: string | null
  status: string
  natureza: string | null
  similares: SimilarCandidate[]
  casoIsolado?: boolean
  motivoCasoIsolado?: string | null
}

export interface GovernanceSummary {
  pendentesHistorico: number
  pendentesNovos: number
  decisionDuplicidade: number
  decisionAlerta: number
  decisionOriginal: number
  revisados: number
  excecoes: number
}

export interface CountItem {
  nome: string
  quantidade: number
}

export interface Closings {
  total: number
  porMes: { mes: string; quantidade: number }[]
  servicosTop: { id: string; nome: string; quantidade: number }[]
  de: string
  ate: string
}

export interface Statistics {
  catalogo: {
    grupos: number
    fornecedores: number
    servicos: number
    unidades: number
    fullCodes: number
  }
  matriz: {
    total: number
    duplicidade: number
    alerta: number
    original: number
    pendentes: number
    revisados: number
    semPendencia: number
  }
  grupos: CountItem[]
  porNatureza: CountItem[]
  fornecedoresTop: CountItem[]
  unidades: CountItem[]
  servicosTop: { nome: string; id: string; grupo: string; quantidade: number }[]
  servicosVinculados: { nome: string; id: string; grupo: string; quantidade: number }[]
  vinculos: { ativos: number; pendentes: number; rejeitados: number }
  tamanhos: CountItem[]
  auditoria: {
    events: number
    criacoes: number
    bloqueios: number
    excecoes: number
  }
}

export interface CreateFullCodePayload {
  gsId?: string | null
  gsNovoNome?: string | null
  cfId?: string | null
  cfNovoNome?: string | null
  cfNovoCodigoSap?: string | null
  csId?: string | null
  csNovaDescricao?: string | null
  umId?: string | null
  umNovaSigla?: string | null
  umNovoNome?: string | null
  justificativa?: string | null
  acao?: string
  usuarioCriacaoId?: string | null
}

export interface AuditEvent {
  id: string
  actorId: string | null
  action: string
  entityType: string
  entityId: string
  occurredAt: string
  decision: string | null
  reason: string | null
  candidateIds: string[]
  similarityScores: number[]
  correlationId: string | null
}

export type TipoComponente = 'CS' | 'GS' | 'CF' | 'UM'

export interface ImportItem {
  id?: string | null
  nome?: string | null
  descricao?: string | null
  sigla?: string | null
  codigoSap?: string | null
}

export interface ImportResult {
  tipo: string
  total: number
  adicionados: number
  idsAdicionados: string[]
  ignorados: { linha: number; item: string; motivo: string }[]
}

export interface MappingMatrix {
  id: string
  newId: string
  descricaoCanonica: string
  grupoServico: string | null
  unidadeMedida: string | null
  decision: string
  score: number | null
  reason: string | null
  legacyData: {
    legacyId: string
    legacyDescription: string
    legacySource: string
    codigoFornecedor: number | null
    alertaSap: boolean | null
    quantidadeCaracteres: number | null
  } | null
  statusRevisao: string | null
  reviewedBy: string | null
  reviewedAt: string | null
  observacaoRevisao: string | null
}
