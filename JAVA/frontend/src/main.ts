import { createApp } from 'vue'
import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import App from './App.vue'
import VinculacaoView from './views/VinculacaoView.vue'
import CadastrarItemView from './views/CadastrarItemView.vue'
import AdminPainelView from './views/AdminPainelView.vue'
import './style.css'

const rotas: RouteRecordRaw[] = [
  { path: '/', name: 'vinculacao', component: VinculacaoView },
  { path: '/cadastrar-item', name: 'cadastrar', component: CadastrarItemView },
  // painel admin: protegido no servidor (ADMIN) e oculto para usuários comuns
  { path: '/admin', name: 'admin', component: AdminPainelView },
  { path: '/governanca', redirect: '/admin' }
]

const router = createRouter({
  history: createWebHistory(),
  routes: rotas
})

createApp(App).use(router).mount('#app')
