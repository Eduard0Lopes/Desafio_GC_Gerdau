import { describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import ComboboxBusca from '../src/components/ComboboxBusca.vue'

const ITENS = [
  { id: 'CS-000001', descricao: 'SV EQUIPE TELHADO', quantidadeCaracteres: 17 },
  { id: 'CS-000002', descricao: 'DIARIA RESGATISTA', quantidadeCaracteres: 18 }
]

describe('ComboboxBusca', () => {
  it('lista as opções encontradas e mantém "Cadastrar Novo Item" como ÚLTIMA opção', async () => {
    const carregar = vi.fn().mockResolvedValue(ITENS)
    const wrapper = mount(ComboboxBusca, {
      props: {
        carregar,
        nomeDe: (i: Record<string, unknown>) => String(i.descricao),
        idDe: (i: Record<string, unknown>) => String(i.id),
        sapDe: (i: Record<string, unknown>) => `${i.quantidadeCaracteres}/40 SAP`
      }
    })

    const input = wrapper.get('input')
    await input.setValue('sv')
    await input.trigger('focus')
    // 2 opções + "Cadastrar Novo Item" (sempre por último)
    await vi.waitFor(() => expect(wrapper.findAll('li').length).toBe(3))

    const itens = wrapper.findAll('li')
    const ultimo = itens[itens.length - 1]
    expect(ultimo.text()).toContain('Cadastrar Novo Item')
    expect(itens[0].text()).toContain('SV EQUIPE TELHADO')
    expect(itens[0].text()).toContain('17/40 SAP')
  })

  it('emite "criarNovo" com o texto digitado ao escolher a última opção', async () => {
    const carregar = vi.fn().mockResolvedValue(ITENS)
    const wrapper = mount(ComboboxBusca, {
      props: { carregar, nomeDe: (i: Record<string, unknown>) => String(i.descricao) }
    })

    const input = wrapper.get('input')
    await input.setValue('novo servico')
    await input.trigger('focus')
    await vi.waitFor(() => expect(wrapper.findAll('li').length).toBe(3))

    const itens = wrapper.findAll('li')
    await itens[itens.length - 1].trigger('mousedown')
    expect(wrapper.emitted('criarNovo')?.[0]).toEqual(['novo servico'])
  })

  it('emite a seleção ao clicar em uma opção', async () => {
    const carregar = vi.fn().mockResolvedValue(ITENS)
    const wrapper = mount(ComboboxBusca, {
      props: { carregar, nomeDe: (i: Record<string, unknown>) => String(i.descricao) }
    })

    await wrapper.get('input').trigger('focus')
    // sem texto digitado não há a opção "criar novo" — apenas os 2 resultados
    await vi.waitFor(() => expect(wrapper.findAll('li').length).toBe(2))
    await wrapper.findAll('li')[0].trigger('mousedown')

    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual([ITENS[0]])
    expect((wrapper.get('input').element as HTMLInputElement).value).toBe('SV EQUIPE TELHADO')
  })

  it('mostra estado vazio quando não há resultados', async () => {
    const carregar = vi.fn().mockResolvedValue([])
    const wrapper = mount(ComboboxBusca, {
      props: { carregar, permiteCriar: false, nomeDe: (i: Record<string, unknown>) => String(i.descricao) }
    })
    await wrapper.get('input').setValue('zzz')
    await vi.waitFor(() => expect(wrapper.text()).toContain('Nenhum resultado'))
  })
})
