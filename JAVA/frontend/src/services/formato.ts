export const ALERT_THRESHOLD = 0.88

export function formatDate(iso?: string | null): string {
  if (!iso) return '—'
  const date = new Date(iso)
  return isNaN(date.getTime()) ? iso : date.toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })
}

export function formatScore(score: number): string {
  return `${(score * 100).toFixed(0)}%`
}

export function specialCaseTitle(motivo?: unknown): string {
  return 'Caso especial registrado.' + (motivo ? ` Motivo: ${motivo}` : '')
}

export function pluralize(count: number, singular: string, pluralForm: string): string {
  return count === 1 ? singular : pluralForm
}
