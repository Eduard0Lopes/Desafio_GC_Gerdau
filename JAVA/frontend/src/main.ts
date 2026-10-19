import { createApp } from 'vue'
import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import App from './App.vue'
import VinculacaoView from './views/VinculacaoView.vue'
import CadastrarItemView from './views/CadastrarItemView.vue'
import CatalogoView from './views/CatalogoView.vue'
import AdminPainelView from './views/AdminPainelView.vue'
import './style.css'

const rotas: RouteRecordRaw[] = [
  { path: '/', name: 'vinculacao', component: VinculacaoView },
  { path: '/cadastrar-item', name: 'cadastrar', component: CadastrarItemView },
  
  { path: '/catalogo', name: 'catalogo', component: CatalogoView },
  
  { path: '/admin', name: 'admin', component: AdminPainelView },
  { path: '/governanca', redirect: '/admin' }
]

const router = createRouter({
  history: createWebHistory(),
  routes: rotas
})

// área administrativa é só para ADMIN (o backend também bloqueia — aqui evita expor a tela)
router.beforeEach((para) => {
  const papel = (localStorage.getItem('papel') || 'USUARIO').toUpperCase()
  if (para.path === '/admin' && papel !== 'ADMIN') {
    return { path: '/' }
  }
  return true
})

createApp(App).use(router).mount('#app')
