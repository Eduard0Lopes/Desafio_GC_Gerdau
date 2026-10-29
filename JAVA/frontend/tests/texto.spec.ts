import { describe, expect, it } from 'vitest'
import { matchesTerm, normalize, singularize, wordVariants } from '../src/services/texto'

describe('texto — normalização e plural/singular', () => {
  it('normaliza acentos e caixa', () => {
    expect(normalize('Soluções Químicas')).toBe('solucoes quimicas')
    expect(normalize('MANUTENÇÃO')).toBe('manutencao')
  })

  it('singulariza plurais comuns do português', () => {
    expect(singularize('telhados')).toBe('telhado')
    expect(singularize('telhado')).toBe('telhado')
    expect(singularize('caixas')).toBe('caixa')
    expect(singularize('solucoes')).toBe('solucao')
    expect(singularize('acoes')).toBe('acao')
    expect(singularize('bens')).toBe('bem')
    expect(singularize('jardins')).toBe('jardim')
    expect(singularize('luzes')).toBe('luz')
    expect(singularize('papeis')).toBe('papel')
    expect(singularize('materiais')).toBe('material')
    expect(singularize('civis')).toBe('civil')
    expect(singularize('tratores')).toBe('trator')
  })

  it('não mexe em códigos, números ou palavras curtas', () => {
    expect(singularize('4521')).toBe('4521')
    expect(singularize('1/2')).toBe('1/2')
    expect(singularize('sv')).toBe('sv')
    expect(singularize('eixo')).toBe('eixo')
  })

  it('gera variantes com a forma digitada, o singular e os plurais', () => {
    const formas = wordVariants('telhados')
    expect(formas).toContain('telhados')
    expect(formas).toContain('telhado')
    expect(wordVariants('telhado')).toContain('telhados')
    expect(wordVariants('Soluções')).toContain('solucao')
    expect(wordVariants('Soluções')).toContain('solucoes')
  })

  it('casa plural ↔ singular nos dois sentidos', () => {
    expect(matchesTerm('cobertura de telhado galvanizado', 'telhados')).toBe(true)
    expect(matchesTerm('telhados metalicos', 'telhado')).toBe(true)
    expect(matchesTerm('registro de solucao quimica', 'solucoes')).toBe(true)
    expect(matchesTerm('solucoes industriais', 'solucao')).toBe(true)
    expect(matchesTerm('obras civis', 'civil')).toBe(true)
    expect(matchesTerm('pintura predial', 'telhados')).toBe(false)
  })

  it('continua entendendo acentos ausentes e várias palavras em qualquer ordem', () => {
    expect(matchesTerm('servico de manutenção mecânica', 'manutencao mecanica')).toBe(true)
    expect(matchesTerm('telhado metalico galvanizado', 'telhados galvanizados')).toBe(true)
    expect(matchesTerm('metalico de telhado', 'telhado metalico')).toBe(true)
    expect(matchesTerm('telhado pintado', 'telhado metalico')).toBe(false)
  })

  it('códigos continuam casando de forma literal', () => {
    expect(matchesTerm('sv eletroduto pvc 1/2', '1/2')).toBe(true)
    expect(matchesTerm('sv eletroduto pvc 3/4', '1/2')).toBe(false)
  })
})
