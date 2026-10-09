<script setup>
import { ref, onMounted } from 'vue'
import { GrupoServicoService, UnidadeMedidaService, CodigoServicoService } from './services/apiService'

import GrupoServicoForm from './components/GrupoServicoForm.vue'
import UnidadeMedidaForm from './components/UnidadeMedidaForm.vue'
import CodigoServicoForm from './components/CodigoServicoForm.vue'

const grupos = ref([])
const unidades = ref([])
const codigos = ref([])

const carregarDados = async () => {
  try {
    const [resGrupos, resUnidades, resCodigos] = await Promise.all([
      GrupoServicoService.listarTodos(),
      UnidadeMedidaService.listarTodos(),
      CodigoServicoService.listarTodos()
    ])
    grupos.value = resGrupos.data
    unidades.value = resUnidades.data
    codigos.value = resCodigos.data
  } catch (err) {
    console.error('Erro ao carregar dados do Spring Boot:', err)
  }
}

const handleSalvarGrupo = async (novoGrupo) => {
  await GrupoServicoService.salvar(novoGrupo)
  carregarDados()
}

const handleSalvarUnidade = async (novaUnidade) => {
  await UnidadeMedidaService.salvar(novaUnidade)
  carregarDados()
}

const handleSalvarCodigo = async (novoCodigo) => {
  await CodigoServicoService.salvar(novoCodigo)
  carregarDados()
}

onMounted(() => {
  carregarDados()
})
</script>
<template>
  <div class="container">
    <header>
      <h1>Padronização de Serviços SAP — Gerdau GC 2026</h1>
    </header>

    <div class="grid">
      <GrupoServicoForm 
        :grupos="grupos" 
        @salvarGrupo="handleSalvarGrupo" 
      />

      <UnidadeMedidaForm 
        :unidades="unidades" 
        @salvarUnidade="handleSalvarUnidade" 
      />

      <CodigoServicoForm 
        :grupos="grupos" 
        :unidades="unidades" 
        :codigos="codigos" 
        @salvarCodigo="handleSalvarCodigo" 
      />
    </div>
  </div>
</template>
<style scoped>
.container {
    max-width: 1100px;
    margin: 0 auto;
    padding: 20px;
    font-family: Arial, sans-serif;
    color: #f8f9fa;
    background-color: #1e1e2e;
    min-height: 100vh; 
  }
header { border-bottom: 2px solid #ff79c6; padding-bottom: 10px; margin-bottom: 20px; }
.grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
</style>