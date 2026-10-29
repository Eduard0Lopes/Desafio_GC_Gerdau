<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, buildIdempotencyKey, isConflict, errorMessage } from '../services/api'
import { toast } from '../services/toast'
import { debounce } from '../services/tempo'
import { currentUser } from '../services/usuario'
import { TYPE_BY_LABEL, TYPE_LABELS } from '../services/rotulos'
import { usePendingRequest } from '../composables/usePedidoPendente'
import BadgeException from '../components/BadgeExcecao.vue'
import BadgeMatch from '../components/BadgeMatch.vue'
import DuplicateBlockModal from '../components/ModalBloqueio.vue'
import GovernanceModal from '../components/ModalGovernanca.vue'
import PendingRequestModal from '../components/ModalPedidoPendente.vue'
import ConfirmModal from '../components/ModalConfirmacao.vue'
import type {
  SimilarCandidate,
  CreateFullCodePayload,
  SimilarityResponse,
  TipoComponente,
  UnitOfMeasure
} from '../types'

const props = withDefaults(defineProps<{ user?: string }>(), { user: '' })
const route = useRoute()
const router = useRouter()

const tipo = ref<TipoComponente>('CS')
const descricao = ref('')
const extraName = ref('')
const codigoSap = ref('')
const umId = ref('')
const unidades = ref<UnitOfMeasure[]>([])
const check = ref<SimilarityResponse | null>(null)
const saving = ref(false)
const blockModal = ref(false)
const governanceModal = ref(false)
const submissionKey = ref('')

const descriptionLabel = computed(() => (tipo.value === 'UM' ? 'Sigla da Unidade' : 'Descrição do Item'))
const descriptionPlaceholder = computed(() => (tipo.value === 'UM' ? 'Ex.: HR' : 'Descreva o item…'))

const valid = computed(() => {
  if (!descricao.value.trim()) return false
  if (tipo.value === 'CS' && descricao.value.length > 40) return false
  return true
})

const creator = computed(() => props.user.trim() || currentUser())

onMounted(async () => {
  const informado = String(route.query.tipo || '')
  tipo.value = TYPE_BY_LABEL[informado]
    || (Object.keys(TYPE_LABELS).find(
      (typeKey) => TYPE_LABELS[typeKey as TipoComponente].normalize('NFC') === informado.normalize('NFC')
    ) as TipoComponente)
    || 'CS'
  if (route.query.descricao) descricao.value = String(route.query.descricao)
  try {
    unidades.value = await api.unitsOfMeasure.list('', 100)
  } catch {
    unidades.value = []
  }
})

watch(tipo, () => {
  check.value = null
  if (descricao.value.trim()) void runSimilarityCheck()
})

const aoDigitar = debounce(async () => {
  if (!descricao.value.trim()) {
    check.value = null
    return
  }
  try {
    check.value = await api.similarity(descricao.value.trim(), 5, tipo.value)
  } catch {
    check.value = null
  }
}, 450)

async function runSimilarityCheck(): Promise<void> {
  if (!descricao.value.trim()) return
  check.value = await api.similarity(descricao.value.trim(), 5, tipo.value)
}

async function save(): Promise<void> {
  saving.value = true
  try {
    const verdict = await api.similarity(descricao.value.trim(), 5, tipo.value)
    check.value = verdict
    if (verdict.decisao !== 'PERMITIR' && verdict.candidatos.length > 0) {
      blockModal.value = true
      return
    }
    await createItem()
    const nome = tipo.value === 'UM' && extraName.value.trim()
      ? `${descricao.value.trim()} - ${extraName.value.trim()}`
      : descricao.value.trim()
    toast.success(`${TYPE_LABELS[tipo.value]} “${nome}” cadastrado com sucesso!`)
    router.push({ path: '/', query: { criado: '1' } })
  } catch (error) {
    if (isConflict(error) && (check.value?.candidatos.length ?? 0) > 0) {
      blockModal.value = true
    } else {
      toast.error(errorMessage(error))
    }
  } finally {
    saving.value = false
  }
}

async function createItem(): Promise<void> {
  if (tipo.value === 'CS') {
    await api.serviceCodes.create({ descricao: descricao.value.trim() })
  } else if (tipo.value === 'GS') {
    await api.serviceGroups.create({ nome: descricao.value.trim() })
  } else if (tipo.value === 'CF') {
    await api.supplierCodes.create({
      nome: descricao.value.trim(),
      codigoSap: codigoSap.value.trim() || null
    })
  } else {
    await api.unitsOfMeasure.create({
      sigla: descricao.value.trim(),
      nome: extraName.value.trim() || descricao.value.trim()
    })
  }
}

function reuse(item: SimilarCandidate): void {
  toast.success(`Código existente reutilizado: ${item.id} — ${item.descricao}. Nenhum novo item foi criado.`)
  router.push({ path: '/', query: { reuse: item.id, tipo: tipo.value, descricao: item.descricao } })
}

const {
  isOpen: requestModal,
  request: pedidoPendente,
  canManage: canManageRequest,
  saving: savingRequest,
  processing: processingRequest,
  confirmingCancel,
  open: openRequest,
  close: closeRequest,
  save: saveRequest,
  confirmCancelation
} = usePendingRequest()

function openGovernance(): void {
  blockModal.value = false
  governanceModal.value = true
  submissionKey.value = buildIdempotencyKey('gov-', tipo.value, descricao.value, String(Date.now()))
}

function governancePayload(justificativa: string): CreateFullCodePayload {
  return {
    gsNovoNome: creator.value,
    cfNovoNome: creator.value,
    csNovaDescricao: descricao.value.trim(),
    umNovaSigla: 'UN',
    acao: 'ENVIAR_VALIDACAO',
    justificativa,
    usuarioCriacaoId: creator.value
  }
}

async function sendToGovernance(justificativa: string): Promise<void> {
  saving.value = true
  try {
    const response = await api.createFullCode(
      governancePayload(justificativa),
      submissionKey.value || buildIdempotencyKey('gov-', tipo.value, descricao.value, justificativa)
    )
    governanceModal.value = false
    if (response.requisicaoRepetida) {
      toast.warning(`Este pedido já havia sido enviado (${response.idCompleto}) — nada novo foi criado.`)
    } else {
      toast.success('Enviado para Governança — o cadastro passará por validação humana pela Equipe de Governança de Dados.')
    }
    router.push('/')
  } catch (error) {
    if (isConflict(error)) {
      governanceModal.value = false
      await openPendingRequest()
    } else {
      toast.error(errorMessage(error))
    }
  } finally {
    saving.value = false
  }
}

async function openPendingRequest(): Promise<void> {
  try {
    const preview = await api.preview(governancePayload(''))
    if (preview.idCompletoJaExiste && preview.statusExistente === 'PENDENTE_VALIDACAO') {
      await openRequest(preview.idCompleto)
    } else {
      toast.error('Não foi possível enviar: houve conflito de requisição. Tente novamente em instantes.')
    }
  } catch (error) {
    toast.error(errorMessage(error))
  }
}
</script>

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
        <label for="descricao">{{ descriptionLabel }}</label>
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
          :placeholder="descriptionPlaceholder"
          @input="aoDigitar"
        />
      </div>
    </div>

    <div v-if="tipo === 'UM'" class="campo-form">
      <label for="nome-um">Nome da Unidade</label>
      <input id="nome-um" type="text" v-model="extraName" placeholder="Ex.: Hora" />
    </div>

    <div v-if="tipo === 'CF'" class="campo-form">
      <label for="sap">Código SAP do Fornecedor (opcional)</label>
      <input id="sap" type="text" v-model="codigoSap" placeholder="Ex.: 200982674" />
    </div>

    <div v-if="tipo === 'CS'" class="campo-form">
      <label for="um">Unidade de Medida</label>
      <select id="um" v-model="umId">
        <option value="">— selecione —</option>
        <option v-for="unit in unidades" :key="unit.id" :value="unit.id">{{ unit.sigla }} - {{ unit.nome }}</option>
      </select>
    </div>

    <div v-if="check">
      <div v-if="check.candidatos.length" class="aviso aviso-amarelo">
        <span class="icone">⚠</span>
        <div>
          Foram encontrados <strong>{{ check.candidatos.length }} item(ns) semelhante(s)</strong>
          — decisão recomendada: <strong>{{ check.decisao }}</strong>
        </div>
      </div>
      <div v-for="candidate in check.candidatos.slice(0, 3)" :key="candidate.id" class="item-similar">
        <span>ID: {{ candidate.id }}</span>
        <span class="suave">|</span>
        <span>{{ (candidate.descricao || '').toUpperCase() }}</span>
        <BadgeException v-if="candidate.casoIsolado" :motivo="candidate.motivoCasoIsolado" />
        <BadgeMatch :score="candidate.score" />
      </div>
      <div v-if="!check.candidatos.length" class="aviso aviso-verde">
        <span class="icone">✓</span>
        <div>Nenhum item semelhante na base — pode prosseguir com o cadastro.</div>
      </div>
    </div>

    <div class="linha-botoes">
      <button class="botao botao-roxo" :disabled="!valid || saving" @click="save">
        {{ saving ? 'Salvando…' : 'Salvar Cadastro' }}
      </button>
      <button class="botao botao-escuro" :disabled="!descricao.trim()" @click="runSimilarityCheck">
        Verificar Similaridade
      </button>
      <button class="botao botao-escuro" @click="$router.push('/')">Voltar</button>
    </div>

    <DuplicateBlockModal
      v-if="blockModal"
      :itens="check?.candidatos || []"
      motivo="O sistema identificou itens semelhantes cadastrados. Escolha um item para reutilizar ou envie para governança."
      @reuse="reuse"
      @governance="openGovernance"
      @back="blockModal = false"
    />

    <GovernanceModal
      v-if="governanceModal"
      itemClass="Novo item de catálogo"
      :descricao="descricao"
      @back="governanceModal = false"
      @confirm="sendToGovernance"
    />

    <PendingRequestModal
      v-if="requestModal"
      :request="pedidoPendente"
      :can-manage="canManageRequest"
      :processing="processingRequest"
      :saving="savingRequest"
      @close="closeRequest"
      @cancel="confirmingCancel = true"
      @save="saveRequest"
    />

    <ConfirmModal
      v-if="confirmingCancel"
      title="Cancelar o pedido?"
      message="O pedido sai da fila da governança e a composição fica livre para um novo envio. O histórico fica preservado."
      confirm-label="✕ Cancelar pedido"
      :dangerous-action="true"
      @confirm="confirmCancelation"
      @cancel="confirmingCancel = false"
    />
  </div>
</template>
