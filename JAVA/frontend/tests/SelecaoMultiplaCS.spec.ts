import { describe, expect, it } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, ref } from 'vue'
import ServiceCodeMultiSelect from '../src/components/SelecaoMultiplaCS.vue'
import type { ServiceCode, BulkCodeItem } from '../src/types'

const CATALOGO: ServiceCode[] = [
  {
    id: 'CS-001',
    descricao: 'SV EQUIPE TELHADO',
    descricaoNormalizada: 'sv equipe telhado',
    quantidadeCaracteres: 17,
    origem: 'BASE',
    legacyIds: []
  },
  {
    id: 'CS-002',
    descricao: 'SV MONTAGEM PIPA',
    descricaoNormalizada: 'sv montagem pipa',
    quantidadeCaracteres: 16,
    origem: 'BASE',
    legacyIds: []
  }
]

function itemNovo(text: string): BulkCodeItem {
  return {
    key: `novo:${text}`,
    texto: text,
    csId: null,
    classificacao: 'CRIAR',
    situacao: 'OK',
    motivo: 'Precisa ser criado.',
    match: null
  }
}


function montar() {
  return mount(
    defineComponent({
      components: { ServiceCodeMultiSelect },
      setup() {
        const chosen = ref<BulkCodeItem[]>([])
        const load = async (term: string) =>
          CATALOGO.filter((item) => item.descricao.toLowerCase().includes(term.toLowerCase()))
        const analyze = async (texts: string[]) => texts.map(itemNovo)
        return { chosen, load, analyze }
      },
      template: '<ServiceCodeMultiSelect v-model="chosen" :load="load" :analyze="analyze" />'
    })
  )
}

async function search(wrapper: ReturnType<typeof montar>, term: string): Promise<void> {
  await wrapper.find('input').setValue(term)
  await new Promise((resolve) => setTimeout(resolve, 300)) 
  await flushPromises()
}

describe('SelecaoMultiplaCS — checkbox sem perder os escolhidos', () => {
  it('marca um resultado e mantém a marca ao pesquisar outra coisa', async () => {
    const wrapper = montar()

    await search(wrapper, 'telhado')
    expect(wrapper.text()).toContain('SV EQUIPE TELHADO')
    await wrapper.find('.resultados .opcao input').setValue(true)
    await flushPromises()
    expect(wrapper.text()).toContain('Escolhidos (1)')

    
    await search(wrapper, 'pipa')
    expect(wrapper.text()).toContain('SV MONTAGEM PIPA')
    expect(wrapper.text()).toContain('Escolhidos (1)')
    expect(wrapper.find('.escolhidos').text()).toContain('SV EQUIPE TELHADO')
  })

  it('desmarcar o checkbox tira o item da seleção', async () => {
    const wrapper = montar()

    await search(wrapper, 'telhado')
    await wrapper.find('.resultados .opcao input').setValue(true)
    await flushPromises()
    await wrapper.find('.escolhidos .opcao input').setValue(false)
    await flushPromises()

    expect(wrapper.text()).toContain('Escolhidos (0)')
  })

  it('colar várias linhas adiciona todos os códigos de uma vez', async () => {
    const wrapper = montar()

    await wrapper.find('input').trigger('paste', {
      clipboardData: { getData: () => 'SV EQUIPE TELHADO\nSV MONTAGEM PIPA' }
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Escolhidos (2)')
    expect(wrapper.find('.escolhidos').text()).toContain('SV EQUIPE TELHADO')
    expect(wrapper.find('.escolhidos').text()).toContain('SV MONTAGEM PIPA')
  })

  it('oferece criar um código que não existe na base', async () => {
    const wrapper = montar()

    await search(wrapper, 'SERVICO NOVO')
    expect(wrapper.text()).toContain('Criar “SERVICO NOVO”')
    await wrapper.findAll('.resultados .opcao').pop()!.find('input').setValue(true)
    await flushPromises()

    expect(wrapper.text()).toContain('Escolhidos (1)')
    expect(wrapper.find('.escolhidos').text()).toContain('Criar')
    expect(wrapper.find('.escolhidos').text()).toContain('SERVICO NOVO')
  })
})
