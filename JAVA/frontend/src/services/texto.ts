export function normalize(texto: string): string {
  return (texto ?? '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
}

export function normalizeHeader(texto: string): string {
  return normalize(texto).replace(/[^a-z0-9]/g, '')
}

function lowercaseNoAccents(texto: string): string {
  return normalize(texto.trim())
}

function lettersOnly(texto: string): boolean {
  return texto.length > 0 && /^[a-z]+$/.test(texto)
}

export function singularize(word: string): string {
  const normalized = lowercaseNoAccents(word)
  if (normalized.length < 4 || !lettersOnly(normalized)) return normalized

  if (normalized.endsWith('oes') && normalized.length >= 5) return normalized.slice(0, -3) + 'ao'
  if (normalized.endsWith('ns')) return normalized.slice(0, -2) + 'm'
  if (normalized.endsWith('zes') && normalized.length >= 5) return normalized.slice(0, -3) + 'z'
  if (normalized.endsWith('eis') && normalized.length >= 6) return normalized.slice(0, -3) + 'el'
  if (normalized.endsWith('ais') && normalized.length >= 6) return normalized.slice(0, -3) + 'al'
  if (normalized.endsWith('ois') && normalized.length >= 5) return normalized.slice(0, -3) + 'ol'
  if (normalized.endsWith('is') && normalized.length >= 5) return normalized.slice(0, -2) + 'il'
  if (normalized.endsWith('ores') && normalized.length >= 6) return normalized.slice(0, -2)
  if (normalized.endsWith('s') && !normalized.endsWith('ss')) return normalized.slice(0, -1)
  return normalized
}

function pluralForms(singular: string): string[] {
  if (singular.length < 4 || !lettersOnly(singular)) return []
  const formas: string[] = [singular + 's']
  const end = singular.slice(-1)

  if (singular.endsWith('ao')) {
    const base = singular.slice(0, -2)
    formas.push(base + 'oes', base + 'aes', base + 'os')
  } else if (end === 'm') {
    formas.push(singular.slice(0, -1) + 'ns')
  } else if (end === 'z') {
    formas.push(singular + 'zes')
  } else if (end === 'r') {
    formas.push(singular + 'es')
  } else if (singular.endsWith('il')) {
    formas.push(singular.slice(0, -2) + 'is', singular.slice(0, -2) + 'eis')
  } else if (singular.endsWith('al') || singular.endsWith('el')
    || singular.endsWith('ol') || singular.endsWith('ul')) {
    formas.push(singular.slice(0, -1) + 'is')
  }
  return formas
}

export function wordVariants(word: string): string[] {
  const digitada = (word ?? '').trim()
  if (!digitada) return []
  const formas = new Set<string>([digitada])

  const norm = lowercaseNoAccents(digitada)
  if (norm && !norm.includes(' ')) formas.add(norm)
  if (lettersOnly(norm)) {
    const singular = singularize(norm)
    formas.add(singular)
    for (const pluralize of pluralForms(singular)) formas.add(pluralize)
  }
  return [...formas]
}

export function matchesTerm(texto: string, term: string): boolean {
  const target = normalize(texto ?? '')
  const palavras = normalize(term ?? '').trim().split(/\s+/).filter(Boolean)
  if (!palavras.length) return true
  return palavras.every((word) =>
    wordVariants(word).some((forma) => target.includes(forma))
  )
}
