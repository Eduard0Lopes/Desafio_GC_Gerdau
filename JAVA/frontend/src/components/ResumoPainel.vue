<script setup>
import { ref } from 'vue'

import { ResumoService, extrairErro } from '@/services/apiService'

defineEmits(['navegar'])

const itens = [
  { campo: 'codigosCompletos', rotulo: 'Códigos completos', aba: 'completos' },
  { campo: 'codigosServico', rotulo: 'Códigos de serviço', aba: 'servicos' },
  { campo: 'gruposServico', rotulo: 'Grupos', aba: 'grupos' },
  { campo: 'codigosFornecedor', rotulo: 'Fornecedores', aba: 'fornecedores' },
  { campo: 'unidadesMedida', rotulo: 'Unidades', aba: 'unidades' },
]

const resumo = ref(null)
const erro = ref('')

async function carregar() {
  try {
    resumo.value = await ResumoService.obter()
    erro.value = ''
  } catch (e) {
    erro.value = extrairErro(e).mensagem
  }
}

defineExpose({ carregar })
</script>

<template>
  <p v-if="erro" class="card erro">{{ erro }}</p>
  <section v-else class="resumo">
    <button v-for="item in itens" :key="item.campo" class="indicador" @click="$emit('navegar', item.aba)">
      <span class="valor">{{ resumo ? resumo[item.campo].toLocaleString('pt-BR') : '—' }}</span>
      <span class="rotulo">{{ item.rotulo }}</span>
    </button>
  </section>
</template>

<style scoped>
.resumo { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: .75rem; margin-bottom: 1.25rem; }
.indicador { background: #fff; color: inherit; border: 1px solid var(--borda); border-left: 4px solid var(--amarelo); text-align: left; display: flex; flex-direction: column; padding: .75rem 1rem; }
.valor { font-size: 1.5rem; font-weight: 700; color: var(--azul); }
.rotulo { color: var(--cinza); font-size: .85rem; }
.erro { color: var(--erro); }
</style>
