import type { Item, TipoComponente } from '../types'

export const TYPE_LABELS: Record<TipoComponente, string> = {
  CS: 'Código de Serviço',
  GS: 'Grupo de Serviço',
  CF: 'Código de Fornecedor',
  UM: 'Unidade de Medida'
}

export const TYPE_BY_LABEL: Record<string, TipoComponente> = {
  'Código de Serviço': 'CS',
  'Grupo de Serviço': 'GS',
  'Código de Fornecedor': 'CF',
  'Unidade de Medida': 'UM'
}

const SHORT_TYPE_LABELS: Record<TipoComponente, string> = {
  CS: 'Serviço',
  GS: 'Grupo',
  CF: 'Fornecedor',
  UM: 'Unidade'
}

export function typeLabel(tipo?: string | null): string {
  return TYPE_LABELS[(tipo || 'CS').toUpperCase() as TipoComponente] || TYPE_LABELS.CS
}

export function shortTypeLabel(tipo?: string | null): string {
  return SHORT_TYPE_LABELS[(tipo || 'CS').toUpperCase() as TipoComponente] || SHORT_TYPE_LABELS.CS
}

export function typeFromLabel(label: string): TipoComponente | undefined {
  return TYPE_BY_LABEL[label]
}

export function decisionClass(decisao: string): string {
  return decisao === 'PERMITIR' ? 'badge-permitir' : decisao === 'ALERTAR' ? 'badge-alertar' : 'badge-revisar'
}

export function statusLabel(status?: unknown): string {
  const normalizedStatus = String(status || 'ATIVO').toUpperCase()
  return normalizedStatus === 'PENDENTE_VALIDACAO'
    ? 'Pendente de aprovação'
    : normalizedStatus === 'REJEITADO'
      ? 'Rejeitado'
      : normalizedStatus === 'DESATIVADO'
        ? 'Desativado'
        : 'Ativo'
}

export function statusClass(status?: unknown): string {
  const normalizedStatus = String(status || 'ATIVO').toUpperCase()
  return normalizedStatus === 'PENDENTE_VALIDACAO'
    ? 'badge-alertar'
    : normalizedStatus === 'REJEITADO'
      ? 'badge-revisar'
      : normalizedStatus === 'DESATIVADO'
        ? 'badge-desativado'
        : 'badge-permitir'
}

export function componentLabel(item: Item, tipo: string): string {
  if (tipo === 'CS') return String(item.descricao ?? item.nome ?? item.id)
  if (tipo === 'UM') return String(`${item.sigla ?? ''} - ${item.nome ?? ''}`).trim()
  return String(item.nome ?? item.descricao ?? item.id)
}

export function parseUnit(texto: string): { sigla: string; nome: string } {
  const [sigla, ...resto] = String(texto || '').split(' - ')
  return { sigla, nome: resto.join(' - ') || sigla }
}

export function itemFromCandidate(id: string, texto: string, tipo: string): Item {
  const base: Item = { id, descricao: texto, nome: texto }
  return tipo === 'UM' ? { ...base, ...parseUnit(texto) } : base
}

const ACTION_LABELS: Record<string, string> = {
  CRIACAO_COMPONENTE: 'Criação de componente',
  EXCLUSAO_COMPONENTE: 'Exclusão de componente',
  DESATIVACAO_COMPONENTE: 'Desativação de componente',
  REATIVACAO_COMPONENTE: 'Reativação de componente',
  CRIACAO_CODIGO_COMPLETO: 'Criação de código completo',
  BLOQUEIO: 'Bloqueio',
  EXCECAO: 'Exceção justificada',
  APROVACAO: 'Aprovação',
  REJEICAO: 'Rejeição',
  REUTILIZACAO: 'Reutilização',
  REVISAO_HISTORICA: 'Revisão do histórico',
  ALTERACAO_PEDIDO: 'Alteração de pedido',
  CANCELAMENTO_PEDIDO: 'Cancelamento de pedido',
  REENVIO_PEDIDO: 'Reenvio de pedido',
  TRANSFERENCIA_VINCULOS: 'Transferência de vínculos'
}

export function actionLabel(acao: string): string {
  return ACTION_LABELS[acao] || acao
}
