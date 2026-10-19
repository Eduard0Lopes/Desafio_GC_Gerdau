

/**
 * Normalização e casamento tolerante a plural/singular para as buscas.
 * As regras de singularizar/variantesEspelho espelham `TextoUtil` no backend,
 * para que busca local e busca no servidor entendam o mesmo idioma.
 */

export function normalizar(texto: string): string {
  return (texto ?? '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
}

function semAcentoMinusculo(texto: string): string {
  return normalizar(texto.trim())
}

function soLetras(texto: string): boolean {
  return texto.length > 0 && /^[a-z]+$/.test(texto)
}

/**
 * Reduz uma palavra ao singular provável em português.
 * Ex.: "telhados" → "telhado", "solucoes" → "solucao", "papeis" → "papel".
 * Palavras curtas, com dígitos ou com pontuação passam intactas.
 */
export function singularizar(palavra: string): string {
  const p = semAcentoMinusculo(palavra)
  if (p.length < 4 || !soLetras(p)) return p

  if (p.endsWith('oes') && p.length >= 5) return p.slice(0, -3) + 'ao' // solucoes → solucao
  if (p.endsWith('ns')) return p.slice(0, -2) + 'm' // bens → bem
  if (p.endsWith('zes') && p.length >= 5) return p.slice(0, -3) + 'z' // luzes → luz
  if (p.endsWith('eis') && p.length >= 6) return p.slice(0, -3) + 'el' // papeis → papel
  if (p.endsWith('ais') && p.length >= 6) return p.slice(0, -3) + 'al' // materiais → material
  if (p.endsWith('ois') && p.length >= 5) return p.slice(0, -3) + 'ol' // farois → farol
  if (p.endsWith('is') && p.length >= 5) return p.slice(0, -2) + 'il' // civis → civil
  if (p.endsWith('ores') && p.length >= 6) return p.slice(0, -2) // tratores → trator
  if (p.endsWith('s') && !p.endsWith('ss')) return p.slice(0, -1) // telhados → telhado
  return p
}

/** Formas plurais prováveis de uma palavra já no singular. */
function plurais(singular: string): string[] {
  if (singular.length < 4 || !soLetras(singular)) return []
  const formas: string[] = [singular + 's']
  const fim = singular.slice(-1)

  if (singular.endsWith('ao')) {
    const base = singular.slice(0, -2)
    formas.push(base + 'oes', base + 'aes', base + 'os') // acao → acoes; mao → maos/maes
  } else if (fim === 'm') {
    formas.push(singular.slice(0, -1) + 'ns') // bem → bens
  } else if (fim === 'z') {
    formas.push(singular + 'zes') // luz → luzes
  } else if (fim === 'r') {
    formas.push(singular + 'es') // trator → tratores
  } else if (singular.endsWith('il')) {
    // civil → civis; facil → faceis
    formas.push(singular.slice(0, -2) + 'is', singular.slice(0, -2) + 'eis')
  } else if (singular.endsWith('al') || singular.endsWith('el')
    || singular.endsWith('ol') || singular.endsWith('ul')) {
    formas.push(singular.slice(0, -1) + 'is') // animal → animais; papel → papeis
  }
  return formas
}

/**
 * Formas de uma palavra que devem casar na busca: como digitada,
 * normalizada, no singular e nos plurais prováveis.
 */
export function variantesPalavra(palavra: string): string[] {
  const digitada = (palavra ?? '').trim()
  if (!digitada) return []
  const formas = new Set<string>([digitada])

  const norm = semAcentoMinusculo(digitada)
  if (norm && !norm.includes(' ')) formas.add(norm)
  if (soLetras(norm)) {
    const singular = singularizar(norm)
    formas.add(singular)
    for (const plural of plurais(singular)) formas.add(plural)
  }
  return [...formas]
}

/**
 * O texto (já normalizado) casa com o termo digitado?
 * Cada palavra do termo precisa aparecer no texto em alguma das suas formas
 * (digitada, normalizada, singular ou plural).
 */
export function casaComTermo(texto: string, termo: string): boolean {
  const alvo = normalizar(texto ?? '')
  const palavras = normalizar(termo ?? '').trim().split(/\s+/).filter(Boolean)
  if (!palavras.length) return true
  return palavras.every((palavra) =>
    variantesPalavra(palavra).some((forma) => alvo.includes(forma))
  )
}
