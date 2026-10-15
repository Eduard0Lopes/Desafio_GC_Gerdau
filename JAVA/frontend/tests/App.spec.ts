import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import App from '../src/App.vue'

function montarComPapel(papel: string) {
  localStorage.setItem('papel', papel)
  localStorage.setItem('usuario', '403921')

  const router = createRouter({
    history: createWebHistory(),
    routes: [{ path: '/', component: { template: '<div>tela</div>' } }]
  })

  return mount(App, { global: { plugins: [router] } })
}

describe('App — permissões de interface', () => {
  beforeEach(() => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({ ok: true, status: 200, json: async () => ({ pendentesHistorico: 10, pendentesNovos: 2 }) })
    )
    localStorage.clear()
  })

  it('perfil USUÁRIO não exibe o menu de Governança', async () => {
    const wrapper = montarComPapel('USUARIO')
    await vi.waitFor(() => expect(wrapper.html()).toBeTruthy())
    expect(wrapper.text()).toContain('Gerdau — Padronização de Códigos de Serviço')
    expect(wrapper.text()).not.toContain('Governança')
  })

  it('perfil ADMIN exibe o menu de Painel Admin', async () => {
    const wrapper = montarComPapel('ADMIN')
    await vi.waitFor(() => expect(wrapper.text()).toContain('Painel Admin'))
  })

  it('exibe identificação do usuário e data', async () => {
    const wrapper = montarComPapel('USUARIO')
    await vi.waitFor(() => expect(wrapper.text()).toContain('Usuário:'))
    expect(wrapper.text()).toContain('Data:')
  })
})
