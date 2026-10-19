

export interface GrupoServico {
  id: string
  nome: string
  nomeNormalizado: string
  origem: string
  status?: string
  createdAt?: string | null
  criadoPor?: string | null
}

export interface CodigoFornecedor {
  id: string
  codigoFornecedorSAP: number | null
  nome: string
  nomeNormalizado: string
  origem: string
  status?: string
  createdAt?: string | null
  criadoPor?: string | null
}

export interface CodigoServico {
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

export interface UnidadeMedida {
  id: string
  sigla: string
  nome: string
  nomeNormalizado: string
  origem: string
  status?: string
  createdAt?: string | null
  criadoPor?: string | null
}

export interface CandidatoSimilar {
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
}

export interface SimilaridadeResponse {
  descricao: string
  descricaoNormalizada: string
  quantidadeCaracteres: number
  excedeLimiteSap: boolean
  status: string
  decisao: string
  acaoRecomendada: string
  limiares: Record<string, number>
  totalItensAvaliados: number
  candidatos: CandidatoSimilar[]
}

export interface ComponentePreview {
  id: string
  nome: string
  criadoNovo: boolean
}

export interface AlertaComponente {
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
  grupoServico: ComponentePreview
  codigoFornecedor: ComponentePreview
  codigoServico: ComponentePreview
  unidadeMedida: ComponentePreview
  descricaoSnapshot: string
  quantidadeCaracteres: number
  excedeLimiteSap: boolean
  idCompletoJaExiste: boolean
  statusExistente?: string | null
  status: string
  decisao: string
  acaoRecomendada: string
  melhorScore: number
  similares: CandidatoSimilar[]
  alertasComponentes: AlertaComponente[]
}

export interface CodigoCompletoResponse {
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
  /** presente só em POST /codigos-completos: true = requisição repetida, nada novo foi criado */
  requisicaoRepetida?: boolean
}

export interface ItemGovernanca {
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
  similares: CandidatoSimilar[]
  casoIsolado?: boolean
  motivoCasoIsolado?: string | null
}

export interface ResumoGovernanca {
  pendentesHistorico: number
  pendentesNovos: number
  decisionDuplicidade: number
  decisionAlerta: number
  decisionOriginal: number
  revisados: number
  excecoes: number
}

export interface ContagemItem {
  nome: string
  quantidade: number
}

export interface Fechamentos {
  total: number
  porMes: { mes: string; quantidade: number }[]
  servicosTop: { id: string; nome: string; quantidade: number }[]
  de: string
  ate: string
}

export interface Estatisticas {
  catalogo: {
    grupos: number
    fornecedores: number
    servicos: number
    unidades: number
    codigosCompletos: number
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
  grupos: ContagemItem[]
  porNatureza: ContagemItem[]
  fornecedoresTop: ContagemItem[]
  unidades: ContagemItem[]
  servicosTop: { nome: string; id: string; grupo: string; quantidade: number }[]
  servicosVinculados: { nome: string; id: string; grupo: string; quantidade: number }[]
  vinculos: { ativos: number; pendentes: number; rejeitados: number }
  tamanhos: ContagemItem[]
  auditoria: {
    eventos: number
    criacoes: number
    bloqueios: number
    excecoes: number
  }
}

export interface CriarCodigoCompletoPayload {
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

export interface EventoAuditoria {
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

export interface ItemImportacao {
  id?: string | null
  nome?: string | null
  descricao?: string | null
  sigla?: string | null
  codigoSap?: string | null
}

export interface ResultadoImportacao {
  tipo: string
  total: number
  adicionados: number
  idsAdicionados: string[]
  ignorados: { linha: number; item: string; motivo: string }[]
}

export interface MatrizDePara {
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
