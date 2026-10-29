import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import PendingRequestModal from '../src/components/ModalPedidoPendente.vue'
import type { FullCodeResponse } from '../src/types'

const PEDIDO = {
  id: 'CIVIL_200982674_CS-000004_SV',
  idCompleto: 'CIVIL_200982674_CS-000004_SV',
  gsId: 'CIVIL',
  cfId: '200982674',
  csId: 'CS-000004',
  umId: 'SV',
  descricaoSnapshot: 'Civil | Fornecedor 200982674 | SV ENGENHEIRO SEGURANCA TRABALHO | SV',
  origem: 'APLICACAO',
  legacyIds: [],
  status: 'PENDENTE_VALIDACAO',
  decisaoSimilaridade: 'ALERTAR',
  melhorScore: 0.9,
  justificativa: 'uso recorrente na obra',
  usuarioCriacaoId: '403921',
  createdAt: '2026-10-14T23:50:11Z',
  aprovadoPor: null,
  observacaoAprovacao: null
} as unknown as FullCodeResponse

function montar(canManage = true) {
  return mount(PendingRequestModal, {
    props: { request: PEDIDO, canManage }
  })
}

describe('ModalPedidoPendente — aviso de pedido já existente', () => {
  it('mostra o pedido (ver) com identificador, descrição e justificativa', () => {
    const wrapper = montar()
    expect(wrapper.text()).toContain('Já existe um pedido pendente na governança')
    expect(wrapper.text()).toContain('CIVIL_200982674_CS-000004_SV')
    expect(wrapper.text()).toContain('SV ENGENHEIRO SEGURANCA TRABALHO')
    expect(wrapper.text()).toContain('uso recorrente na obra')
  })

  it('tem X para fechar e botão Fechar, ambos emitindo fechar', async () => {
    const wrapper = montar()
    const closeButton = wrapper.get('.fechar-x')
    await closeButton.trigger('click')
    expect(wrapper.emitted('close')).toHaveLength(1)

    const close = wrapper.findAll('button').find((b) => b.text().trim() === 'Fechar')!
    await close.trigger('click')
    expect(wrapper.emitted('close')).toHaveLength(2)
  })

  it('editar abre os campos e salvar emite as alterações', async () => {
    const wrapper = montar()
    await wrapper.findAll('button').find((b) => b.text().includes('Editar pedido'))!.trigger('click')

    const inputs = wrapper.findAll('input, textarea')
    await inputs[0].setValue('SV ENGENHEIRO SEGURANCA TRABALHO 2')
    await inputs[1].setValue('ajuste após revisão')

    await wrapper.findAll('button').find((b) => b.text().includes('Salvar alterações'))!.trigger('click')
    expect(wrapper.emitted('save')?.[0]?.[0]).toEqual({
      descricao: 'SV ENGENHEIRO SEGURANCA TRABALHO 2',
      justificativa: 'ajuste após revisão'
    })
  })

  it('cancelar emite cancelar', async () => {
    const wrapper = montar()
    await wrapper.findAll('button').find((b) => b.text().includes('Cancelar pedido'))!.trigger('click')
    expect(wrapper.emitted('cancel')).toHaveLength(1)
  })

  it('quem não é dono/admin só vê o pedido, sem editar/cancelar', () => {
    const wrapper = montar(false)
    const botoes = wrapper.findAll('button').map((b) => b.text())
    expect(botoes.some((t) => t.includes('Editar pedido'))).toBe(false)
    expect(botoes.some((t) => t.includes('Cancelar pedido'))).toBe(false)
    expect(wrapper.text()).toContain('Somente quem enviou este pedido')
    expect(wrapper.text()).toContain('CIVIL_200982674_CS-000004_SV')
  })
})
