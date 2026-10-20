import { describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import VinculacaoView from '../src/views/VinculacaoView.vue'

function montar() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/', component: VinculacaoView }]
  })
  return router.push('/').then(() =>
    mount(VinculacaoView, {
      props: { usuario: '403921', papel: 'ADMIN' },
      global: { plugins: [router], stubs: { 'router-link': true } }
    })
  )
}

describe('VinculacaoView — abas de vinculação', () => {
  it('a aba "Vincular em massa" mostra 3 painéis e o painel de vinculação em massa', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => ({ ok: true, status: 200, json: async () => [] })))
    const wrapper = await montar()
    await flushPromises()

    // no vínculo único são 4 painéis (inclui Código de Serviço) e não há painel de massa
    expect(wrapper.findAll('.grade-seletores .painel')).toHaveLength(4)
    expect(wrapper.find('.vinculacao-massa').exists()).toBe(false)

    await wrapper.findAll('.modos-vinculo .aba')[1].trigger('click')
    await flushPromises()

    // em massa o Código de Serviço sai da grade — sem coluna vazia sobrando
    expect(wrapper.findAll('.grade-seletores .painel')).toHaveLength(3)
    expect(wrapper.find('.grade-seletores').classes()).toContain('tres-colunas')
    expect(wrapper.find('.vinculacao-massa').exists()).toBe(true)
    expect(wrapper.text()).toContain('Códigos de Serviço')
  })

  it('no modo em massa o fluxo é um painel só — composição, códigos e ação', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => ({ ok: true, status: 200, json: async () => [] })))
    const wrapper = await montar()
    await flushPromises()

    await wrapper.findAll('.modos-vinculo .aba')[1].trigger('click')
    await flushPromises()

    expect(wrapper.find('.painel-massa').exists()).toBe(true)
    expect(wrapper.text()).toContain('1 · Composição do vínculo')
    expect(wrapper.text()).toContain('2 · Códigos de Serviço')
    // o botão nunca fica desabilitado — o clique guia o usuário
    const botao = wrapper.find('.vinculacao-massa .botao-roxo')
    expect(botao.attributes('disabled')).toBeUndefined()
    const acoes = wrapper.findAll('.vinculacao-massa .acoes .botao').map((b) => b.text())
    expect(acoes).toContain('Cadastrar Novo Item')
    expect(acoes).toContain('Limpar tudo')
  })
})
