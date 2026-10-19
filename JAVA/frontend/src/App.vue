<template>
  <header class="barra-sistema">
    <h1>Gerdau — Padronização de Códigos de Serviço</h1>
    <div class="barra-usuario">
      <span>Usuário:</span>
      <input v-model="usuario" placeholder="403921" @change="salvar" />
      <span>| Data: {{ dataHoje }}</span>

      
      <div class="troca-perfil">
        <span>Perfil:</span>
        <select v-model="papel" @change="salvar">
          <option value="USUARIO">Usuário</option>
          <option value="ADMIN">Admin</option>
        </select>
      </div>

      
      <span v-if="bancoAtivo" class="banco-ativo" title="Banco de dados que está recebendo os dados">
        ● dados em: <strong>{{ bancoAtivo }}</strong>
      </span>

      
      <nav class="navegacao">
        <router-link to="/" exact-active-class="ativo">Início</router-link>
        <router-link to="/catalogo" active-class="ativo">Catálogo</router-link>
        
        <router-link v-if="isAdmin" to="/admin" active-class="ativo">
          Painel Admin
          <strong v-if="pendentes > 0">({{ pendentes }})</strong>
        </router-link>
      </nav>

      
      <button class="troca-tema" :title="tema.estado.tema === 'claro' ? 'Ativar modo escuro' : 'Ativar modo claro'"
              @click="tema.alternar()">
        {{ tema.estado.tema === 'claro' ? '☾' : '☀' }}
      </button>
    </div>
  </header>

  <Toast />

  <main class="area">
    <div v-if="apiFora" class="status-api">
      ⚠️ <strong>Backend indisponível.</strong> Verifique se a aplicação está no ar
      (<code>docker compose up --build</code>) e recarregue a página.
    </div>
    <router-view :usuario="usuario" :papel="papel" @pendentes="pendentes = $event" @api-fora="apiFora = $event" />
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, provide, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { api } from './services/api'
import { tema } from './services/tema'
import Toast from './components/Toast.vue'

const router = useRouter()
const usuario = ref(localStorage.getItem('usuario') || '403921')
const papel = ref(localStorage.getItem('papel') || 'USUARIO')
const pendentes = ref(0)
const apiFora = ref(false)

const dataHoje = new Date().toLocaleDateString('pt-BR')
const isAdmin = computed(() => papel.value === 'ADMIN')
const bancoAtivo = ref('')

async function carregarBancoAtivo(): Promise<void> {
  try {
    const r = await fetch('/api/v1/health')
    const dados = await r.json()
    bancoAtivo.value = dados?.banco || ''
    
    apiFora.value = false
  } catch {
    bancoAtivo.value = ''
    apiFora.value = true
  }
}

function salvar(): void {
  localStorage.setItem('usuario', usuario.value)
  localStorage.setItem('papel', papel.value)
}

async function atualizarPendentes(): Promise<void> {
  if (!isAdmin.value) {
    pendentes.value = 0
    return
  }
  try {
    const r = await api.resumo()
    pendentes.value = (r.pendentesHistorico || 0) + (r.pendentesNovos || 0)
    apiFora.value = false
  } catch {
    apiFora.value = true
  }
}


watch(papel, () => {
  salvar()
  if (router.currentRoute.value.path === '/admin' && papel.value !== 'ADMIN') {
    router.push('/')
  }
  atualizarPendentes()
})

provide('atualizarPendentes', atualizarPendentes)

onMounted(() => {
  tema.aplicar()
  atualizarPendentes()
  carregarBancoAtivo()
})

setInterval(() => {
  void atualizarPendentes()
  void carregarBancoAtivo()
}, 30000)
</script>

<style scoped>
.troca-perfil {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: 18px;
}
.troca-perfil select {
  background: var(--campo);
  border: 1px solid var(--borda);
  border-radius: 7px;
  color: var(--texto);
  padding: 6px 10px;
  font-size: 0.86rem;
}
.banco-ativo {
  margin-left: 16px;
  color: var(--texto-suave);
  font-size: 0.8rem;
}
.banco-ativo strong {
  color: var(--verde);
  font-weight: 600;
}
.troca-tema {
  margin-left: 14px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.25);
  border-radius: 8px;
  color: #fff;
  width: 34px;
  height: 32px;
  cursor: pointer;
  font-size: 1rem;
}
.troca-tema:hover {
  background: var(--amarelo);
  color: var(--azul-escuro);
}
</style>
