<script setup>
import { computed, onMounted, reactive, ref } from 'vue'

import PaginacaoBar from '@/components/PaginacaoBar.vue'
import SimilaresLista from '@/components/SimilaresLista.vue'
import { useDebounce } from '@/composables/useDebounce'
import { useNotificacoes } from '@/composables/useNotificacoes'
import {
  CodigoCompletoService,
  CodigoFornecedorService,
  CodigoServicoService,
  GrupoServicoService,
  UnidadeMedidaService,
} from '@/services/apiService'

const emit = defineEmits(['alterado'])
const { notificar, notificarErro } = useNotificacoes()

const grupos = ref([])
const fornecedores = ref([])
const unidades = ref([])
const pagina = ref({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 })
const filtros = reactive({ gsId: '', cfId: '', umId: '', q: '' })

const form = reactive({ gsId: '', cfId: '', umId: '', modoServico: 'existente', csId: '', descricaoServico: '', justificativa: '' })
const servicoSelecionado = ref(null)
const buscaServico = ref('')
const errosCampos = ref({})
const salvando = ref(false)
const similares = ref([])
const buscandoSimilares = ref(false)

const previaId = computed(() => {
  const cs = form.modoServico === 'existente' ? form.csId : '??????'
  return [form.gsId || '???', form.cfId || '???', cs || '??????', form.umId || '??'].join('')
})

async function carregarReferencias() {
  try {
    ;[grupos.value, fornecedores.value, unidades.value] = await Promise.all([
      GrupoServicoService.listar(),
      CodigoFornecedorService.listar(),
      UnidadeMedidaService.listar(),
    ])
  } catch (e) {
    notificarErro(e)
  }
}

async function carregar(page = 0) {
  const params = Object.fromEntries(Object.entries(filtros).filter(([, v]) => v))
  try {
    pagina.value = await CodigoCompletoService.listar({ ...params, page, size: 20 })
  } catch (e) {
    notificarErro(e)
  }
}
const filtrar = useDebounce(() => carregar(0))

const buscarServicos = useDebounce(async () => {
  const texto = form.modoServico === 'existente' ? buscaServico.value : form.descricaoServico
  if (texto.trim().length < 3) {
    similares.value = []
    return
  }
  buscandoSimilares.value = true
  try {
    similares.value = await CodigoServicoService.similares(texto, 5)
  } catch (e) {
    notificarErro(e)
  } finally {
    buscandoSimilares.value = false
  }
})

function selecionarServico(servico) {
  servicoSelecionado.value = servico
  form.csId = servico.id
  form.modoServico = 'existente'
  similares.value = []
  buscaServico.value = ''
}

function trocarModo(modo) {
  form.modoServico = modo
  form.csId = ''
  servicoSelecionado.value = null
  similares.value = []
}

function limpar() {
  Object.assign(form, { csId: '', descricaoServico: '', justificativa: '' })
  servicoSelecionado.value = null
  similares.value = []
  errosCampos.value = {}
}

async function salvar() {
  salvando.value = true
  errosCampos.value = {}
  const existente = form.modoServico === 'existente'
  try {
    const salvo = await CodigoCompletoService.criar({
      gsId: form.gsId,
      cfId: form.cfId,
      umId: form.umId,
      csId: existente ? form.csId : null,
      descricaoServico: existente ? null : form.descricaoServico,
      justificativa: existente ? null : form.justificativa || null,
    })
    notificar(`Código completo ${salvo.id} gerado.`)
    limpar()
    emit('alterado')
    await carregar(0)
  } catch (e) {
    errosCampos.value = notificarErro(e).campos
  } finally {
    salvando.value = false
  }
}

async function excluir(item) {
  if (!confirm(`Excluir o código completo ${item.id}?`)) return
  try {
    await CodigoCompletoService.excluir(item.id)
    notificar(`Código completo ${item.id} excluído.`)
    emit('alterado')
    await carregar(pagina.value.page)
  } catch (e) {
    notificarErro(e)
  }
}

onMounted(() => Promise.all([carregarReferencias(), carregar()]))
</script>

<template>
  <div class="grade">
    <form class="card" @submit.prevent="salvar">
      <h3>Gerar código completo</h3>
      <p class="dica">Composição: Grupo (3) + Fornecedor (3) + Serviço (6) + Unidade (2)</p>
      <p class="previa mono">{{ previaId }}</p>

      <div class="campo">
        <label for="gs">Grupo de serviço</label>
        <select id="gs" v-model="form.gsId" required>
          <option value="" disabled>Selecione…</option>
          <option v-for="g in grupos" :key="g.id" :value="g.id">{{ g.id }} – {{ g.nome }}</option>
        </select>
      </div>
      <div class="campo">
        <label for="cf">Fornecedor</label>
        <select id="cf" v-model="form.cfId" required>
          <option value="" disabled>Selecione…</option>
          <option v-for="f in fornecedores" :key="f.id" :value="f.id">{{ f.id }} – {{ f.codigoFornecedorSAP }} {{ f.nome }}</option>
        </select>
      </div>
      <div class="campo">
        <label for="um">Unidade de medida</label>
        <select id="um" v-model="form.umId" required>
          <option value="" disabled>Selecione…</option>
          <option v-for="u in unidades" :key="u.id" :value="u.id">{{ u.id }} – {{ u.sigla }} ({{ u.nome }})</option>
        </select>
      </div>

      <div class="campo">
        <label>Serviço</label>
        <div class="acoes">
          <button type="button" :class="form.modoServico === 'existente' ? '' : 'secundario'" @click="trocarModo('existente')">Existente</button>
          <button type="button" :class="form.modoServico === 'novo' ? '' : 'secundario'" @click="trocarModo('novo')">Novo</button>
        </div>
      </div>

      <template v-if="form.modoServico === 'existente'">
        <div v-if="servicoSelecionado" class="selecionado">
          <span class="mono">{{ servicoSelecionado.id }}</span> {{ servicoSelecionado.descricao }}
          <button type="button" class="link" @click="trocarModo('existente')">trocar</button>
        </div>
        <div v-else class="campo">
          <input v-model="buscaServico" placeholder="Digite para buscar o serviço…" @input="buscarServicos" />
        </div>
        <SimilaresLista :similares="similares" :carregando="buscandoSimilares" selecionavel @selecionar="selecionarServico" />
      </template>
      <template v-else>
        <div class="campo">
          <input v-model="form.descricaoServico" maxlength="40" placeholder="Descrição do novo serviço (máx. 40)"
                 :class="{ invalido: errosCampos.descricaoServico }" @input="buscarServicos" />
          <span v-if="errosCampos.descricaoServico" class="erro-campo">{{ errosCampos.descricaoServico }}</span>
        </div>
        <SimilaresLista :similares="similares" :carregando="buscandoSimilares" selecionavel @selecionar="selecionarServico" />
        <div class="campo">
          <textarea v-model="form.justificativa" rows="2" maxlength="500" placeholder="Justificativa (se houver provável duplicidade)"></textarea>
        </div>
      </template>
      <span v-if="errosCampos.servicoInformadoUmaVez" class="erro-campo">{{ errosCampos.servicoInformadoUmaVez }}</span>

      <div class="acoes">
        <button type="submit" :disabled="salvando || (form.modoServico === 'existente' && !form.csId)">Gerar código</button>
      </div>
    </form>

    <section class="card">
      <div class="filtros">
        <select v-model="filtros.gsId" @change="carregar(0)">
          <option value="">Todos os grupos</option>
          <option v-for="g in grupos" :key="g.id" :value="g.id">{{ g.nome }}</option>
        </select>
        <select v-model="filtros.cfId" @change="carregar(0)">
          <option value="">Todos os fornecedores</option>
          <option v-for="f in fornecedores" :key="f.id" :value="f.id">{{ f.codigoFornecedorSAP }}</option>
        </select>
        <select v-model="filtros.umId" @change="carregar(0)">
          <option value="">Todas as unidades</option>
          <option v-for="u in unidades" :key="u.id" :value="u.id">{{ u.sigla }}</option>
        </select>
        <input v-model="filtros.q" type="search" placeholder="Código, snapshot ou código legado…" @input="filtrar" />
      </div>
      <div class="tabela-wrapper">
        <table>
          <thead>
            <tr><th>Código completo</th><th>Grupo</th><th>Fornecedor</th><th>Serviço</th><th>UM</th><th>Legados</th><th>Origem</th><th></th></tr>
          </thead>
          <tbody>
            <tr v-for="c in pagina.content" :key="c.id">
              <td class="mono">{{ c.id }}</td>
              <td>{{ c.grupoNome }}</td>
              <td class="mono">{{ c.fornecedorCodigoSAP }}</td>
              <td>{{ c.servicoDescricao }}</td>
              <td class="mono">{{ c.unidadeSigla }}</td>
              <td class="mono" :title="c.legacyIds.join(', ')">{{ c.legacyIds.slice(0, 2).join(', ') }}{{ c.legacyIds.length > 2 ? '…' : '' }}</td>
              <td><span :class="['selo', c.origem]">{{ c.origem }}</span></td>
              <td><button class="link" @click="excluir(c)">excluir</button></td>
            </tr>
          </tbody>
        </table>
        <p v-if="!pagina.content.length" class="vazio">Nenhum código completo encontrado.</p>
      </div>
      <PaginacaoBar :pagina="pagina" @ir="carregar" />
    </section>
  </div>
</template>

<style scoped>
.previa { font-size: 1.2rem; letter-spacing: .08em; background: var(--azul-claro); color: var(--azul); padding: .5rem .75rem; border-radius: 6px; }
.selecionado { background: var(--azul-claro); padding: .5rem .75rem; border-radius: 6px; margin-bottom: .85rem; font-size: .9rem; }
</style>
