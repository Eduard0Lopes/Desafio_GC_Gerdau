import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import ModalBloqueio from '../src/components/ModalBloqueio.vue'

const ITENS = [
  {
    posicao: 1,
    id: 'CS-130835',
    codigoOriginal: null,
    descricao: 'SV-EQUIPE TELHADO',
    score: 0.95,
    scoreTokens: 0,
    scoreCaracteres: 0,
    tokensEmComum: [],
    motivo: 'Mesma descrição'
  }
]

describe('ModalBloqueio (Bloqueio de Duplicidade)', () => {
  it('exibe os itens semelhantes com percentual de match', () => {
    const wrapper = mount(ModalBloqueio, { props: { itens: ITENS } })
    expect(wrapper.text()).toContain('Bloqueio de Duplicidade')
    expect(wrapper.text()).toContain('O sistema identificou itens semelhantes cadastrados')
    expect(wrapper.text()).toContain('SV-EQUIPE TELHADO')
    expect(wrapper.text()).toContain('95% Match')
  })

  it('expõe as três ações do fluxo', () => {
    const wrapper = mount(ModalBloqueio, { props: { itens: ITENS } })
    expect(wrapper.text()).toContain('Reutilizar Código Existente')
    expect(wrapper.text()).toContain('Enviar para Governança')
    expect(wrapper.text()).toContain('Voltar')
  })

  it('emite as ações correspondentes (após escolher o item)', async () => {
    const wrapper = mount(ModalBloqueio, { props: { itens: ITENS } })

    const botoes = wrapper.findAll('button')
    expect((botoes[0].element as HTMLButtonElement).disabled).toBe(true)

    await wrapper.find('input[type=radio]').trigger('change')
    expect((wrapper.findAll('button')[0].element as HTMLButtonElement).disabled).toBe(false)

    await wrapper.findAll('button')[0].trigger('click')
    await wrapper.findAll('button')[1].trigger('click')
    await wrapper.findAll('button')[2].trigger('click')

    expect(wrapper.emitted('reutilizar')?.[0]).toEqual([ITENS[0]])
    expect(wrapper.emitted('governanca')).toBeTruthy()
    expect(wrapper.emitted('voltar')).toBeTruthy()
  })
})
