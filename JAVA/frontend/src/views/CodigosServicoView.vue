<script setup>
import { onMounted, reactive, ref } from 'vue'

import PaginacaoBar from '@/components/PaginacaoBar.vue'
import SimilaresLista from '@/components/SimilaresLista.vue'
import { useDebounce } from '@/composables/useDebounce'
import { useNotificacoes } from '@/composables/useNotificacoes'
import { CodigoServicoService } from '@/services/apiService'

const LIMITE_SAP = 40
const emit = defineEmits(['alterado'])
const { notificar, notificarErro } = useNotificacoes()

const pagina = ref({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 })
const filtro = ref('')
const form = reactive({ descricao: '', justificativa: '' })
const editandoId = ref(null)
const errosCampos = ref({})
const salvando = ref(false)
const similares = ref([])
const buscandoSimilares = ref(false)

async function carregar(page = 0) {
  try {
    pagina.value = await CodigoServicoService.listar({ q: filtro.value || undefined, page, size: 20 })
  } catch (e) {
    notificarErro(e)
  }
}
const filtrar = useDebounce(() => carregar(0))

const buscarSimilares = useDebounce(async () => {
  if (form.descricao.trim().length < 3) {
    similares.value = []
    return
  }
  buscandoSimilares.value = true
  try {
    similares.value = await CodigoServicoService.similares(form.descricao, 5, editandoId.value || undefined)
  } catch (e) {
    notificarErro(e)
  } finally {
    buscandoSimilares.value = false
  }
})

function limpar() {
  Object.assign(form, { descricao: '', justificativa: '' })
  editandoId.value = null
  errosCampos.value = {}
  similares.value = []
}

function editar(item) {
  Object.assign(form, { descricao: item.descricao, justificativa: item.justificativa || '' })
  editandoId.value = item.id
  errosCampos.value = {}
  buscarSimilares()
}

async function salvar() {
  salvando.value = true
  errosCampos.value = {}
  try {
    const payload = { descricao: form.descricao, justificativa: form.justificativa || null }
    const salvo = editandoId.value
      ? await CodigoServicoService.atualizar(editandoId.value, payload)
      : await CodigoServicoService.criar(payload)
    notificar(`Código de serviço ${salvo.id} salvo.`)
    limpar()
    emit('alterado')
    await carregar(pagina.value.page)
  } catch (e) {
    errosCampos.value = notificarErro(e).campos
  } finally {
    salvando.value = false
  }
}

async function excluir(item) {
  if (!confirm(`Excluir o código de serviço ${item.id}?`)) return
  try {
    await CodigoServicoService.excluir(item.id)
    notificar(`Código de serviço ${item.id} excluído.`)
    emit('alterado')
    await carregar(pagina.value.page)
  } catch (e) {
    notificarErro(e)
  }
}

onMounted(() => carregar())
</script>

<template>
  <div class="grade">
    <form class="card" @submit.prevent="salvar">
      <h3>{{ editandoId ? `Editar código de serviço ${editandoId}` : 'Novo código de serviço' }}</h3>
      <div class="campo">
        <label for="descricao">Descrição (máx. {{ LIMITE_SAP }} caracteres – regra SAP)</label>
        <input id="descricao" v-model="form.descricao" :maxlength="LIMITE_SAP" required
               :class="{ invalido: errosCampos.descricao }" @input="buscarSimilares" />
        <span class="dica">{{ form.descricao.length }}/{{ LIMITE_SAP }}</span>
        <span v-if="errosCampos.descricao" class="erro-campo">{{ errosCampos.descricao }}</span>
      </div>
      <SimilaresLista :similares="similares" :carregando="buscandoSimilares" />
      <div class="campo">
        <label for="justificativa">Justificativa (obrigatória se houver provável duplicidade)</label>
        <textarea id="justificativa" v-model="form.justificativa" rows="2" maxlength="500"></textarea>
      </div>
      <div class="acoes">
        <button type="submit" :disabled="salvando">{{ editandoId ? 'Salvar alterações' : 'Cadastrar' }}</button>
        <button v-if="editandoId" type="button" class="secundario" @click="limpar">Cancelar</button>
      </div>
    </form>

    <section class="card">
      <div class="filtros">
        <input v-model="filtro" type="search" placeholder="Filtrar por descrição, ID ou código legado…" @input="filtrar" />
      </div>
      <div class="tabela-wrapper">
        <table>
          <thead>
            <tr><th>ID</th><th>Descrição</th><th>Legados</th><th>Origem</th><th></th></tr>
          </thead>
          <tbody>
            <tr v-for="item in pagina.content" :key="item.id">
              <td class="mono">{{ item.id }}</td>
              <td>{{ item.descricao }}<div v-if="item.justificativa" class="dica">{{ item.justificativa }}</div></td>
              <td class="mono" :title="item.legacyIds.join(', ')">
                {{ item.legacyIds.slice(0, 3).join(', ') }}{{ item.legacyIds.length > 3 ? ` +${item.legacyIds.length - 3}` : '' }}
              </td>
              <td><span :class="['selo', item.origem]">{{ item.origem }}</span></td>
              <td class="acoes">
                <button class="link" @click="editar(item)">editar</button>
                <button class="link" @click="excluir(item)">excluir</button>
              </td>
            </tr>
          </tbody>
        </table>
        <p v-if="!pagina.content.length" class="vazio">Nenhum código encontrado.</p>
      </div>
      <PaginacaoBar :pagina="pagina" @ir="carregar" />
    </section>
  </div>
</template>
