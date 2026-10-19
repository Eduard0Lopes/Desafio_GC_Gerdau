import { describe, expect, it } from 'vitest'
import { casaComTermo, normalizar, singularizar, variantesPalavra } from '../src/services/texto'

describe('texto — normalização e plural/singular', () => {
  it('normaliza acentos e caixa', () => {
    expect(normalizar('Soluções Químicas')).toBe('solucoes quimicas')
    expect(normalizar('MANUTENÇÃO')).toBe('manutencao')
  })

  it('singulariza plurais comuns do português', () => {
    expect(singularizar('telhados')).toBe('telhado')
    expect(singularizar('telhado')).toBe('telhado')
    expect(singularizar('caixas')).toBe('caixa')
    expect(singularizar('solucoes')).toBe('solucao')
    expect(singularizar('acoes')).toBe('acao')
    expect(singularizar('bens')).toBe('bem')
    expect(singularizar('jardins')).toBe('jardim')
    expect(singularizar('luzes')).toBe('luz')
    expect(singularizar('papeis')).toBe('papel')
    expect(singularizar('materiais')).toBe('material')
    expect(singularizar('civis')).toBe('civil')
    expect(singularizar('tratores')).toBe('trator')
  })

  it('não mexe em códigos, números ou palavras curtas', () => {
    expect(singularizar('4521')).toBe('4521')
    expect(singularizar('1/2')).toBe('1/2')
    expect(singularizar('sv')).toBe('sv')
    expect(singularizar('eixo')).toBe('eixo')
  })

  it('gera variantes com a forma digitada, o singular e os plurais', () => {
    const formas = variantesPalavra('telhados')
    expect(formas).toContain('telhados')
    expect(formas).toContain('telhado')
    expect(variantesPalavra('telhado')).toContain('telhados')
    expect(variantesPalavra('Soluções')).toContain('solucao')
    expect(variantesPalavra('Soluções')).toContain('solucoes')
  })

  it('casa plural ↔ singular nos dois sentidos', () => {
    expect(casaComTermo('cobertura de telhado galvanizado', 'telhados')).toBe(true)
    expect(casaComTermo('telhados metalicos', 'telhado')).toBe(true)
    expect(casaComTermo('registro de solucao quimica', 'solucoes')).toBe(true)
    expect(casaComTermo('solucoes industriais', 'solucao')).toBe(true)
    expect(casaComTermo('obras civis', 'civil')).toBe(true)
    expect(casaComTermo('pintura predial', 'telhados')).toBe(false)
  })

  it('continua entendendo acentos ausentes e várias palavras em qualquer ordem', () => {
    expect(casaComTermo('servico de manutenção mecânica', 'manutencao mecanica')).toBe(true)
    expect(casaComTermo('telhado metalico galvanizado', 'telhados galvanizados')).toBe(true)
    expect(casaComTermo('metalico de telhado', 'telhado metalico')).toBe(true)
    expect(casaComTermo('telhado pintado', 'telhado metalico')).toBe(false)
  })

  it('códigos continuam casando de forma literal', () => {
    expect(casaComTermo('sv eletroduto pvc 1/2', '1/2')).toBe(true)
    expect(casaComTermo('sv eletroduto pvc 3/4', '1/2')).toBe(false)
  })
})
