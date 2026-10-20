import { describe, expect, it } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, ref } from 'vue'
import SelecaoMultiplaCS from '../src/components/SelecaoMultiplaCS.vue'
import type { CodigoServico, ItemCsMassa } from '../src/types'

const CATALOGO: CodigoServico[] = [
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

function itemNovo(texto: string): ItemCsMassa {
  return {
    chave: `novo:${texto}`,
    texto,
    csId: null,
    classificacao: 'CRIAR',
    situacao: 'OK',
    motivo: 'Precisa ser criado.',
    match: null
  }
}

/** harness com v-model de verdade: o checkbox depende do estado do pai */
function montar() {
  return mount(
    defineComponent({
      components: { SelecaoMultiplaCS },
      setup() {
        const escolhidos = ref<ItemCsMassa[]>([])
        const carregar = async (termo: string) =>
          CATALOGO.filter((c) => c.descricao.toLowerCase().includes(termo.toLowerCase()))
        const resolver = async (textos: string[]) => textos.map(itemNovo)
        return { escolhidos, carregar, resolver }
      },
      template: '<SelecaoMultiplaCS v-model="escolhidos" :carregar="carregar" :resolver="resolver" />'
    })
  )
}

async function buscar(wrapper: ReturnType<typeof montar>, termo: string): Promise<void> {
  await wrapper.find('input').setValue(termo)
  await new Promise((r) => setTimeout(r, 300)) // debounce da busca
  await flushPromises()
}

describe('SelecaoMultiplaCS — checkbox sem perder os escolhidos', () => {
  it('marca um resultado e mantém a marca ao pesquisar outra coisa', async () => {
    const wrapper = montar()

    await buscar(wrapper, 'telhado')
    expect(wrapper.text()).toContain('SV EQUIPE TELHADO')
    await wrapper.find('.resultados .opcao input').setValue(true)
    await flushPromises()
    expect(wrapper.text()).toContain('Escolhidos (1)')

    // a busca muda e o que já foi escolhido continua lá
    await buscar(wrapper, 'pipa')
    expect(wrapper.text()).toContain('SV MONTAGEM PIPA')
    expect(wrapper.text()).toContain('Escolhidos (1)')
    expect(wrapper.find('.escolhidos').text()).toContain('SV EQUIPE TELHADO')
  })

  it('desmarcar o checkbox tira o item da seleção', async () => {
    const wrapper = montar()

    await buscar(wrapper, 'telhado')
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

    await buscar(wrapper, 'SERVICO NOVO')
    expect(wrapper.text()).toContain('Criar “SERVICO NOVO”')
    await wrapper.findAll('.resultados .opcao').pop()!.find('input').setValue(true)
    await flushPromises()

    expect(wrapper.text()).toContain('Escolhidos (1)')
    expect(wrapper.find('.escolhidos').text()).toContain('Criar')
    expect(wrapper.find('.escolhidos').text()).toContain('SERVICO NOVO')
  })
})
