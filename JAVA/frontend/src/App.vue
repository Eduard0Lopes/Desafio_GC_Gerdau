<script setup lang="ts">
import { computed, onMounted, provide, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { api } from './services/api'
import { theme } from './services/tema'
import Toast from './components/Toast.vue'

const router = useRouter()
const user = ref(localStorage.getItem('user') || '403921')
const role = ref(localStorage.getItem('role') || 'USUARIO')
const pendentes = ref(0)
const apiUnavailable = ref(false)

const today = new Date().toLocaleDateString('pt-BR')
const isAdmin = computed(() => role.value === 'ADMIN')
const activeDatabase = ref('')

async function loadActiveDatabase(): Promise<void> {
  try {
    const response = await fetch('/api/v1/health')
    const data = await response.json()
    activeDatabase.value = data?.banco || ''

    apiUnavailable.value = false
  } catch {
    activeDatabase.value = ''
    apiUnavailable.value = true
  }
}

function save(): void {
  localStorage.setItem('user', user.value)
  localStorage.setItem('role', role.value)
}

async function updatePending(): Promise<void> {
  if (!isAdmin.value) {
    pendentes.value = 0
    return
  }
  try {
    const response = await api.summary()
    pendentes.value = (response.pendentesHistorico || 0) + (response.pendentesNovos || 0)
    apiUnavailable.value = false
  } catch {
    apiUnavailable.value = true
  }
}

watch(role, () => {
  save()
  if (router.currentRoute.value.path === '/admin' && role.value !== 'ADMIN') {
    router.push('/')
  }
  updatePending()
})

provide('updatePending', updatePending)

onMounted(() => {
  theme.apply()
  updatePending()
  loadActiveDatabase()
})

setInterval(() => {
  void updatePending()
  void loadActiveDatabase()
}, 30000)
</script>

<template>
  <header class="barra-sistema">
    <h1>Gerdau — Padronização de Códigos de Serviço</h1>
    <div class="barra-usuario">
      <span>Usuário:</span>
      <input v-model="user" placeholder="403921" @change="save" />
      <span>| Data: {{ today }}</span>

      <div class="troca-perfil">
        <span>Perfil:</span>
        <select v-model="role" @change="save">
          <option value="USUARIO">Usuário</option>
          <option value="ADMIN">Admin</option>
        </select>
      </div>

      <span v-if="activeDatabase" class="banco-ativo" title="Banco de dados que está recebendo os dados">
        ● dados em: <strong>{{ activeDatabase }}</strong>
      </span>

      <nav class="navegacao">
        <router-link to="/" exact-active-class="ativo">Início</router-link>
        <router-link to="/catalogo" active-class="ativo">Catálogo</router-link>

        <router-link v-if="isAdmin" to="/admin" active-class="ativo">
          Painel Admin
          <strong v-if="pendentes > 0">({{ pendentes }})</strong>
        </router-link>
      </nav>

      <button class="troca-tema" :title="theme.state.theme === 'claro' ? 'Ativar modo escuro' : 'Ativar modo claro'"
              @click="theme.toggle()">
        {{ theme.state.theme === 'claro' ? '☾' : '☀' }}
      </button>
    </div>
  </header>

  <Toast />

  <main class="area">
    <div v-if="apiUnavailable" class="status-api">
      ⚠️ <strong>Backend indisponível.</strong> Verifique se a aplicação está no ar
      (<code>docker compose up --build</code>) e recarregue a página.
    </div>
    <router-view :user="user" :role="role" @pending="pendentes = $event" @apiUnavailable="apiUnavailable = $event" />
  </main>
</template>

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
