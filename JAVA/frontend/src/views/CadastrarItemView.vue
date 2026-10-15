<template>
  <div class="cartao-central">
    <h2>Cadastrar Novo Item</h2>

    <div class="campo-form">
      <label for="tipo">Tipo de Componente</label>
      <select id="tipo" v-model="tipo">
        <option value="CS">Código de Serviço</option>
        <option value="GS">Grupo de Serviço</option>
        <option value="CF">Código de Fornecedor</option>
        <option value="UM">Unidade de Medida</option>
      </select>
    </div>

    <div class="campo-form">
      <div class="rotulo-com-badge">
        <label for="descricao">{{ rotuloDescricao }}</label>
        <span
          v-if="tipo === 'CS'"
          class="contador-sap"
          :class="{ estourado: descricao.length > 40 }"
        >
          {{ descricao.length }}/40 Caracteres SAP
        </span>
      </div>
      <div class="linha-campo">
        <input
          id="descricao"
          type="text"
          v-model="descricao"
          :placeholder="placeholderDescricao"
          @input="aoDigitar"
        />
      </div>
    </div>

    <div v-if="tipo === 'UM'" class="campo-form">
      <label for="nome-um">Nome da Unidade</label>
      <input id="nome-um" type="text" v-model="nomeExtra" placeholder="Ex.: Hora" />
    </div>

    <div v-if="tipo === 'CF'" class="campo-form">
      <label for="sap">Código SAP do Fornecedor (opcional)</label>
      <input id="sap" type="text" v-model="codigoSap" placeholder="Ex.: 200982674" />
    </div>

    <div v-if="tipo === 'CS'" class="campo-form">
      <label for="um">Unidade de Medida</label>
      <select id="um" v-model="umId">
        <option value="">— selecione —</option>
        <option v-for="u in unidades" :key="u.id" :value="u.id">{{ u.sigla }} - {{ u.nome }}</option>
      </select>
    </div>

    <!-- verificação de similaridade (na hora de criar — vale para todos os tipos) -->
    <div v-if="verificacao">
      <div v-if="verificacao.candidatos.length" class="aviso aviso-amarelo">
        <span class="icone">⚠</span>
        <div>
          Foram encontrados <strong>{{ verificacao.candidatos.length }} item(ns) semelhante(s)</strong>
          — decisão recomendada: <strong>{{ verificacao.decisao }}</strong>
        </div>
      </div>
      <div v-for="c in verificacao.candidatos.slice(0, 3)" :key="c.id" class="item-similar">
        <span>ID: {{ c.id }}</span>
        <span class="suave">|</span>
        <span>{{ (c.descricao || '').toUpperCase() }}</span>
        <span class="match" :class="{ alerta: c.score < 0.88 }">{{ (c.score * 100).toFixed(0) }}% Match</span>
      </div>
      <div v-if="!verificacao.candidatos.length" class="aviso aviso-verde">
        <span class="icone">✓</span>
        <div>Nenhum item semelhante na base — pode prosseguir com o cadastro.</div>
      </div>
    </div>

    <div class="linha-botoes">
      <button class="botao botao-roxo" :disabled="!valido || salvando" @click="salvar">
        {{ salvando ? 'Salvando…' : 'Salvar Cadastro' }}
      </button>
      <button class="botao botao-escuro" :disabled="!descricao.trim()" @click="verificarSimilaridade">
        Verificar Similaridade
      </button>
      <button class="botao botao-escuro" @click="$router.push('/')">Voltar</button>
    </div>

    <ModalBloqueio
      v-if="modalBloqueio"
      :itens="verificacao?.candidatos || []"
      motivo="O sistema identificou itens semelhantes cadastrados. Escolha um item para reutilizar ou envie para governança."
      @reutilizar="reutilizar"
      @governanca="(modalGovernanca = true), (modalBloqueio = false)"
      @voltar="modalBloqueio = false"
    />

    <ModalGovernanca
      v-if="modalGovernanca"
      classe="Novo item de catálogo"
      :descricao="descricao"
      @voltar="modalGovernanca = false"
      @confirmar="enviarGovernanca"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../services/api'
import { toast } from '../services/toast'
import ModalBloqueio from '../components/ModalBloqueio.vue'
import ModalGovernanca from '../components/ModalGovernanca.vue'
import type { CandidatoSimilar, SimilaridadeResponse, TipoComponente, UnidadeMedida } from '../types'

const props = withDefaults(defineProps<{ usuario?: string }>(), { usuario: '' })
const route = useRoute()
const router = useRouter()

// chaves ASCII evitam qualquer problema de normalização de acentos
const ROTULOS: Record<string, string> = { CS: 'Código de Serviço', GS: 'Grupo de Serviço', CF: 'Código de Fornecedor', UM: 'Unidade de Medida' }
const PARA_TIPO: Record<string, TipoComponente> = { 'Código de Serviço': 'CS', 'Grupo de Serviço': 'GS', 'Código de Fornecedor': 'CF', 'Unidade de Medida': 'UM' }

const tipo = ref<TipoComponente>('CS')
const descricao = ref('')
const nomeExtra = ref('')
const codigoSap = ref('')
const umId = ref('')
const unidades = ref<UnidadeMedida[]>([])
const verificacao = ref<SimilaridadeResponse | null>(null)
const salvando = ref(false)
const modalBloqueio = ref(false)
const modalGovernanca = ref(false)

const rotuloDescricao = computed(() => (tipo.value === 'UM' ? 'Sigla da Unidade' : 'Descrição do Item'))
const placeholderDescricao = computed(() => (tipo.value === 'UM' ? 'Ex.: HR' : 'Descreva o item…'))

const valido = computed(() => {
  if (!descricao.value.trim()) return false
  if (tipo.value === 'CS' && descricao.value.length > 40) return false
  return true
})

onMounted(async () => {
  const informado = String(route.query.tipo || '')
  tipo.value = PARA_TIPO[informado]
    || (Object.keys(ROTULOS).find((k) => ROTULOS[k].normalize('NFC') === informado.normalize('NFC')) as TipoComponente)
    || 'CS'
  if (route.query.descricao) descricao.value = String(route.query.descricao)
  try {
    unidades.value = await api.unidadesMedida.listar('', 100)
  } catch {
    unidades.value = []
  }
})

// ao trocar o tipo, refaz a verificação com o novo catálogo
watch(tipo, () => {
  verificacao.value = null
  if (descricao.value.trim()) verificarSimilaridade()
})

let timer: ReturnType<typeof setTimeout> | null = null
function aoDigitar(): void {
  if (timer) clearTimeout(timer)
  timer = setTimeout(async () => {
    if (!descricao.value.trim()) {
      verificacao.value = null
      return
    }
    try {
      verificacao.value = await api.similaridade(descricao.value.trim(), 5, tipo.value)
    } catch {
      verificacao.value = null
    }
  }, 450)
}

async function verificarSimilaridade(): Promise<void> {
  if (!descricao.value.trim()) return
  verificacao.value = await api.similaridade(descricao.value.trim(), 5, tipo.value)
}

async function salvar(): Promise<void> {
  salvando.value = true
  try {
    const v = await api.similaridade(descricao.value.trim(), 5, tipo.value)
    verificacao.value = v
    if (v.decisao !== 'PERMITIR' && v.candidatos.length > 0) {
      modalBloqueio.value = true
      return
    }
    await criarItem()
    router.push({ path: '/', query: { criado: '1' } })
  } catch (e) {
    if ((e as { status?: number }).status === 409 && (verificacao.value?.candidatos.length ?? 0) > 0) {
      modalBloqueio.value = true
    } else if ((e as { status?: number }).status === 409) {
      toast.erro((e as Error).message)
    } else {
      toast.erro((e as Error).message)
    }
  } finally {
    salvando.value = false
  }
}

async function criarItem(): Promise<void> {
  if (tipo.value === 'CS') {
    await api.codigosServico.criar({ descricao: descricao.value.trim() })
  } else if (tipo.value === 'GS') {
    await api.gruposServico.criar({ nome: descricao.value.trim() })
  } else if (tipo.value === 'CF') {
    await api.codigosFornecedor.criar({
      nome: descricao.value.trim(),
      codigoSap: codigoSap.value.trim() || null
    })
  } else {
    await api.unidadesMedida.criar({
      sigla: descricao.value.trim(),
      nome: nomeExtra.value.trim() || descricao.value.trim()
    })
  }
}

function reutilizar(item: CandidatoSimilar): void {
  router.push({ path: '/', query: { reutilizar: item.id } })
}

async function enviarGovernanca(justificativa: string): Promise<void> {
  salvando.value = true
  try {
    await api.criarCodigoCompleto(
      {
        gsNovoNome: 'Governança',
        cfNovoNome: 'Governança',
        csNovaDescricao: descricao.value.trim(),
        umNovaSigla: 'UN',
        acao: 'ENVIAR_VALIDACAO',
        justificativa,
        usuarioCriacaoId: props.usuario || null
      },
      'gov-' + Date.now()
    )
    modalGovernanca.value = false
    router.push('/')
  } catch (e) {
    toast.erro((e as Error).message)
  } finally {
    salvando.value = false
  }
}
</script>


