<script setup>
import { onMounted, ref } from 'vue'

import NotificacoesToast from '@/components/NotificacoesToast.vue'
import ResumoPainel from '@/components/ResumoPainel.vue'
import CadastroSimples from '@/views/CadastroSimples.vue'
import CodigosCompletosView from '@/views/CodigosCompletosView.vue'
import CodigosServicoView from '@/views/CodigosServicoView.vue'
import { CodigoFornecedorService, GrupoServicoService, UnidadeMedidaService } from '@/services/apiService'

const abas = [
  { id: 'completos', titulo: 'Códigos Completos' },
  { id: 'servicos', titulo: 'Códigos de Serviço' },
  { id: 'grupos', titulo: 'Grupos de Serviço' },
  { id: 'fornecedores', titulo: 'Fornecedores' },
  { id: 'unidades', titulo: 'Unidades de Medida' },
]

const cadastros = {
  grupos: {
    titulo: 'Grupo de Serviço',
    service: GrupoServicoService,
    campos: [{ nome: 'nome', rotulo: 'Nome', maxlength: 100 }],
    colunas: [{ campo: 'id', rotulo: 'ID', mono: true }, { campo: 'nome', rotulo: 'Nome' }],
  },
  fornecedores: {
    titulo: 'Fornecedor',
    service: CodigoFornecedorService,
    campos: [
      { nome: 'codigoFornecedorSAP', rotulo: 'Código SAP', tipo: 'number' },
      { nome: 'nome', rotulo: 'Nome', maxlength: 150 },
    ],
    colunas: [
      { campo: 'id', rotulo: 'ID', mono: true },
      { campo: 'codigoFornecedorSAP', rotulo: 'Código SAP', mono: true },
      { campo: 'nome', rotulo: 'Nome' },
    ],
  },
  unidades: {
    titulo: 'Unidade de Medida',
    service: UnidadeMedidaService,
    campos: [
      { nome: 'sigla', rotulo: 'Sigla', maxlength: 10 },
      { nome: 'nome', rotulo: 'Nome', maxlength: 100 },
    ],
    colunas: [
      { campo: 'id', rotulo: 'ID', mono: true },
      { campo: 'sigla', rotulo: 'Sigla', mono: true },
      { campo: 'nome', rotulo: 'Nome' },
    ],
  },
}

const abaAtiva = ref('completos')
const resumo = ref(null)

async function atualizarResumo() {
  resumo.value?.carregar()
}

onMounted(atualizarResumo)
</script>

<template>
  <header class="topo">
    <div class="marca">
      <strong>GERDAU</strong>
      <span>Catálogo Padronizado de Serviços</span>
    </div>
  </header>

  <main class="conteudo">
    <ResumoPainel ref="resumo" @navegar="abaAtiva = $event" />

    <nav class="abas">
      <button
        v-for="aba in abas"
        :key="aba.id"
        :class="{ ativa: abaAtiva === aba.id }"
        @click="abaAtiva = aba.id"
      >
        {{ aba.titulo }}
      </button>
    </nav>

    <CodigosCompletosView v-if="abaAtiva === 'completos'" @alterado="atualizarResumo" />
    <CodigosServicoView v-else-if="abaAtiva === 'servicos'" @alterado="atualizarResumo" />
    <CadastroSimples v-else :key="abaAtiva" v-bind="cadastros[abaAtiva]" @alterado="atualizarResumo" />
  </main>

  <NotificacoesToast />
</template>

<style scoped>
.topo { background: var(--azul); color: #fff; padding: .9rem 1.5rem; border-bottom: 4px solid var(--amarelo); }
.marca { display: flex; gap: .75rem; align-items: baseline; max-width: 1280px; margin: 0 auto; }
.marca strong { font-size: 1.3rem; letter-spacing: .12em; }
.marca span { opacity: .85; }
.conteudo { max-width: 1280px; margin: 0 auto; padding: 1.25rem 1.5rem 3rem; }
.abas { display: flex; gap: .25rem; border-bottom: 2px solid var(--borda); margin-bottom: 1.25rem; flex-wrap: wrap; }
.abas button { background: none; color: var(--cinza); border: none; border-bottom: 3px solid transparent; border-radius: 0; margin-bottom: -2px; }
.abas button.ativa { color: var(--azul); border-bottom-color: var(--amarelo); font-weight: 600; }
</style>
