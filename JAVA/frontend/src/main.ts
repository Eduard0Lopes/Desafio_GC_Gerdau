import { createApp } from 'vue'
import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import App from './App.vue'
import ServiceLinkView from './views/VinculacaoView.vue'
import CreateItemView from './views/CadastrarItemView.vue'
import CatalogView from './views/CatalogoView.vue'
import AdminPanelView from './views/AdminPainelView.vue'
import { currentRole } from './services/usuario'
import './style.css'

const rotas: RouteRecordRaw[] = [
  { path: '/', name: 'vinculacao', component: ServiceLinkView },
  { path: '/cadastrar-item', name: 'create', component: CreateItemView },

  { path: '/catalogo', name: 'catalogo', component: CatalogView },

  { path: '/admin', name: 'admin', component: AdminPanelView },
  { path: '/governanca', redirect: '/admin' }
]

const router = createRouter({
  history: createWebHistory(),
  routes: rotas
})

router.beforeEach((para) => {
  if (para.path === '/admin' && currentRole() !== 'ADMIN') {
    return { path: '/' }
  }
  return true
})

createApp(App).use(router).mount('#app')
