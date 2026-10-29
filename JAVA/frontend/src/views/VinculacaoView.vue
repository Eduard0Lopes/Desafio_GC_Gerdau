<script setup lang="ts">
import { computed, inject, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, buildIdempotencyKey, isConflict, errorMessage } from '../services/api'
import { toast } from '../services/toast'
import { currentUser } from '../services/usuario'
import { decisionClass, itemFromCandidate, componentLabel, typeLabel } from '../services/rotulos'
import { useStatistics } from '../composables/useEstatisticas'
import { useAutoClose } from '../composables/useFechamentoAutomatico'
import { usePendingRequest } from '../composables/usePedidoPendente'
import BadgeException from '../components/BadgeExcecao.vue'
import BadgeMatch from '../components/BadgeMatch.vue'
import SearchCombobox from '../components/ComboboxBusca.vue'
import DuplicateBlockModal from '../components/ModalBloqueio.vue'
import ConfirmModal from '../components/ModalConfirmacao.vue'
import GovernanceModal from '../components/ModalGovernanca.vue'
import PendingRequestModal from '../components/ModalPedidoPendente.vue'
import BulkServiceLink from './VinculacaoMassa.vue'
import type {
  SimilarCandidate,
  CreateFullCodePayload,
  Item,
  PreviewResponse,
  BatchLinkResult,
  TipoComponente
} from '../types'

type BlockItem = SimilarCandidate & { componentType?: string }

const props = withDefaults(defineProps<{ user?: string; role?: string }>(), { user: '', role: 'USUARIO' })
const router = useRouter()
const route = useRoute()
const updatePending = inject<() => void>('updatePending', () => {})

const isAdmin = computed(() => props.role === 'ADMIN')
const guideOpen = ref(false)
const linkMode = ref<'unico' | 'massa'>('unico')
const bulkKey = ref(0)

const { statistics, reduction, load: carregarEstatisticas } = useStatistics()

async function loadAdminSummary(): Promise<void> {
  if (!isAdmin.value) return
  await carregarEstatisticas()
}

function onBulkLinked(result: BatchLinkResult): void {
  updatePending()
  void loadAdminSummary()
  if (result.aguardandoValidacao) {
    toast.warning(
      `${result.aguardandoValidacao} pedido(s) aguardando validação da Governança — acompanhe no Painel Admin.`
    )
  }
}

const serviceGroup = ref<Item | null>(null)
const supplierCode = ref<Item | null>(null)
const serviceCode = ref<Item | null>(null)
const unitOfMeasure = ref<Item | null>(null)

const serviceGroupCombobox = ref<InstanceType<typeof SearchCombobox> | null>(null)
const supplierCodeCombobox = ref<InstanceType<typeof SearchCombobox> | null>(null)
const serviceCodeCombobox = ref<InstanceType<typeof SearchCombobox> | null>(null)
const unitOfMeasureCombobox = ref<InstanceType<typeof SearchCombobox> | null>(null)

const deactivation = ref<{ tipo: TipoComponente; id: string; label: string } | null>(null)

function askDeactivation(item: Item, tipo: TipoComponente): void {
  deactivation.value = { tipo, id: String(item.id), label: componentLabel(item, tipo) }
}

async function confirmDeactivation(): Promise<void> {
  const target = deactivation.value
  if (!target) return
  deactivation.value = null
  try {
    const report = await api.setComponentStatus(target.tipo, [target.id], 'DESATIVADO')
    const result = report[0]
    if (!result?.alterado) {
      toast.warning(result?.motivo || 'Nada foi alterado.')
      return
    }
    toast.success(`${typeLabel(target.tipo)} “${target.label}” desativado — fora de circulação. Reative no Catálogo se precisar.`)
    const selecoes: Record<TipoComponente, { value: Item | null }> = { GS: serviceGroup, CF: supplierCode, CS: serviceCode, UM: unitOfMeasure }
    const paineis: Record<TipoComponente, { reload: () => void } | null> = {
      GS: serviceGroupCombobox.value, CF: supplierCodeCombobox.value, CS: serviceCodeCombobox.value, UM: unitOfMeasureCombobox.value
    }
    const selected = selecoes[target.tipo]
    if (selected.value && String(selected.value.id) === target.id) selected.value = null
    paineis[target.tipo]?.reload()
    updatePending()
  } catch (error) {
    toast.error(errorMessage(error))
  }
}

const preview = ref<PreviewResponse | null>(null)
const processing = ref(false)
const previewModal = ref(false)
const blockModal = ref(false)
const governanceModal = ref(false)
const success = ref<{ title: string; message: string } | null>(null)
const submissionKey = ref('')

const { timeLeft } = useAutoClose(success, () => {
  success.value = null
})

const {
  isOpen: requestModal,
  request: pedidoPendente,
  canManage: canManageRequest,
  saving: savingRequest,
  processing: processingRequest,
  confirmingCancel,
  open: openPendingRequest,
  close: closeRequest,
  save: saveRequest,
  confirmCancelation
} = usePendingRequest({ afterCancel: () => link() })

function managePending(): void {
  const id = preview.value?.idCompleto
  previewModal.value = false
  if (id) void openPendingRequest(id)
}

const complete = computed(() => serviceGroup.value && supplierCode.value && serviceCode.value && unitOfMeasure.value)
const newServiceDescription = computed(() => preview.value?.codigoServico?.nome || '')

const blockItems = computed<BlockItem[]>(() => {
  const currentPreview = preview.value
  if (!currentPreview) return []
  const list: BlockItem[] = (currentPreview.similares || []).map((candidate) => ({ ...candidate, componentType: 'CS' }))
  for (const alert of currentPreview.alertasComponentes || []) {
    list.push({
      posicao: 0,
      id: alert.itemSimilarId,
      codigoOriginal: null,
      descricao: alert.itemSimilarNome,
      score: alert.score,
      scoreTokens: 0,
      scoreCaracteres: 0,
      tokensEmComum: [],
      motivo: `${typeLabel(alert.tipo)}: “${alert.descricaoInformada}” é semelhante a este item.`,
      componentType: alert.tipo
    })
  }
  return list
})

const blockReason = computed(() =>
  'O sistema identificou itens semelhantes cadastrados. Escolha um item para reutilizar ou justifique a criação.')

function goToCreate(tipo: string, texto: string): void {
  router.push({ path: '/cadastrar-item', query: { tipo, descricao: texto } })
}

function buildPayload(extra: Partial<CreateFullCodePayload> = {}): CreateFullCodePayload {
  return {
    gsId: (serviceGroup.value?.id as string) ?? null,
    cfId: (supplierCode.value?.id as string) ?? null,
    csId: (serviceCode.value?.id as string) ?? null,
    umId: (unitOfMeasure.value?.id as string) ?? null,
    usuarioCriacaoId: props.user.trim() || currentUser(),
    ...extra
  }
}

async function link(): Promise<void> {
  processing.value = true
  try {
    preview.value = await api.preview(buildPayload())
    if (preview.value.idCompletoJaExiste) {
      toast.warning(`Este código já está vinculado (${preview.value.idCompleto}) — este envio criará mais um vínculo.`)
    }
    const body = buildPayload()
    submissionKey.value = buildIdempotencyKey('criar-', body.gsId, body.cfId, body.csId, body.umId, String(Date.now()))
    if (preview.value.decisao === 'PERMITIR') {
      previewModal.value = true
    } else {
      blockModal.value = true
    }
  } catch (error) {
    toast.error(errorMessage(error))
  } finally {
    processing.value = false
  }
}

function registerLink(idCompleto: string, descricaoSnapshot: string): void {
  toast.success(`Enviado com sucesso! Serviço ${idCompleto} vinculado.`)
  success.value = { title: `Serviço ${idCompleto} vinculado!`, message: descricaoSnapshot }
  updatePending()
}

async function confirmLink(): Promise<void> {
  processing.value = true
  try {
    const body = buildPayload({ acao: 'CRIAR' })
    const response = await api.createFullCode(
      body,
      submissionKey.value || buildIdempotencyKey('criar-', body.gsId, body.cfId, body.csId, body.umId, String(Date.now()))
    )
    previewModal.value = false
    if (response.requisicaoRepetida) {
      toast.warning(`O vínculo ${response.idCompleto} já estava registrado — nada novo foi criado.`)
      success.value = { title: 'Vínculo já registrado', message: response.descricaoSnapshot }
      updatePending()
    } else {
      registerLink(response.idCompleto, response.descricaoSnapshot)
    }
  } catch (error) {
    if (isConflict(error) && preview.value) {
      previewModal.value = false
      await link()
    } else {
      toast.error(errorMessage(error))
    }
  } finally {
    processing.value = false
  }
}

function selectPanel(tipo: string, id: string, texto: string): void {
  const item = itemFromCandidate(id, texto, tipo)
  if (tipo === 'GS') serviceGroup.value = item
  else if (tipo === 'CF') supplierCode.value = item
  else if (tipo === 'UM') unitOfMeasure.value = item
  else serviceCode.value = item
}

function reuseItem(candidateItem: SimilarCandidate): void {
  const item = candidateItem as BlockItem
  blockModal.value = false
  selectPanel(item.componentType || 'CS', item.id, item.descricao || '')
  toast.success(`Código existente reutilizado: ${item.id} — ${item.descricao}. Confirme a vinculação.`)
  void link()
}

function openGovernance(): void {
  blockModal.value = false
  governanceModal.value = true
  const body = buildPayload()
  submissionKey.value = buildIdempotencyKey('gov-', body.gsId, body.cfId, body.csId, body.umId, String(Date.now()))
}

async function confirmGovernance(justificativa: string): Promise<void> {
  processing.value = true
  try {
    const body = buildPayload({ acao: 'ENVIAR_VALIDACAO', justificativa })
    const response = await api.createFullCode(
      body,
      submissionKey.value || buildIdempotencyKey('gov-', body.gsId, body.cfId, body.csId, body.umId, justificativa)
    )
    governanceModal.value = false
    if (response.requisicaoRepetida) {
      toast.warning(`Este pedido já havia sido enviado (${response.idCompleto}) — nada novo foi criado.`)
      success.value = {
        title: 'Pedido já enviado',
        message: `O cadastro ${response.idCompleto} já estava na fila de validação da Governança.`
      }
    } else {
      toast.success('Enviado com sucesso! O cadastro foi para a fila de validação da Governança.')
      success.value = {
        title: 'Enviado para Governança',
        message: `O cadastro ${response.idCompleto} passará por validação humana pela Equipe de Governança de Dados.`
      }
    }
    updatePending()
  } catch (error) {
    if (isConflict(error) && preview.value) {
      governanceModal.value = false
      await link()
    } else {
      toast.error(errorMessage(error))
    }
  } finally {
    processing.value = false
  }
}

function clearAll(): void {
  serviceGroup.value = supplierCode.value = serviceCode.value = unitOfMeasure.value = null
  preview.value = null
  success.value = null
  previewModal.value = blockModal.value = governanceModal.value = false
  bulkKey.value++
  closeRequest()
}

function applyReuse(): void {
  const id = route.query.reuse
  if (!id) return
  const itemType = String(route.query.tipo || 'CS').toUpperCase()
  const texto = String(route.query.descricao || '')
  selectPanel(itemType, String(id), texto)
  toast.success(`${String(id)} — ${texto} selecionado como ${typeLabel(itemType)}. Complete a vinculação.`)
  void router.replace({ query: {} })
}

onMounted(() => {
  void loadAdminSummary()
  applyReuse()
  if (route.query.criado) void router.replace({ query: {} })
})
watch(() => props.role, () => void loadAdminSummary())
</script>

<template>
  <div>
    <div class="modos-vinculo" role="tablist" aria-label="Modo de vinculação">
      <button
        type="button"
        role="tab"
        class="aba"
        :class="{ ativa: linkMode === 'unico' }"
        :aria-selected="linkMode === 'unico'"
        @click="linkMode = 'unico'"
      >
        <svg class="aba-icone" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71" />
          <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71" />
        </svg>
        <span class="aba-texto">
          <strong>Vínculo único</strong>
          <small>um serviço por vez</small>
        </span>
      </button>
      <button
        type="button"
        role="tab"
        class="aba"
        :class="{ ativa: linkMode === 'massa' }"
        :aria-selected="linkMode === 'massa'"
        @click="linkMode = 'massa'"
      >
        <svg class="aba-icone" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M12.83 2.18a2 2 0 0 0-1.66 0L2.6 6.08a1 1 0 0 0 0 1.83l8.58 3.91a2 2 0 0 0 1.66 0l8.58-3.9a1 1 0 0 0 0-1.83Z" />
          <path d="m22 17.65-9.17 4.16a2 2 0 0 1-1.66 0L2 17.65" />
          <path d="m22 12.65-9.17 4.16a2 2 0 0 1-1.66 0L2 12.65" />
        </svg>
        <span class="aba-texto">
          <strong>Vincular em massa</strong>
          <small>vários de uma vez</small>
        </span>
      </button>
    </div>

    <currentPreview class="modos-dica">
      <template v-if="linkMode === 'unico'">
        Escolha Grupo, Fornecedor, Código de Serviço e Unidade — e veja a prévia antes de confirmar.
      </template>
      <template v-else>
        Escolha Grupo, Fornecedor e Unidade <strong>uma vez</strong>: eles valem para todos os códigos
        que você marcar na lista.
      </template>
    </currentPreview>

    <div :class="linkMode === 'massa' ? 'painel painel-massa' : ''">
      <label v-if="linkMode === 'massa'" class="painel-rotulo">1 · Composição do vínculo</label>

      <section class="grade-seletores" :class="{ 'tres-colunas': linkMode === 'massa' }">
        <div class="painel">
          <label class="painel-rotulo">Grupo de Serviço</label>
          <SearchCombobox
            ref="cbGs"
            placeholder="Digite para buscar…"
            :load="api.serviceGroups.list"
            :nameOf="(item: any) => String(item.nome)"
            :idOf="() => ''"
            :allowRemove="isAdmin"
            v-model="serviceGroup"
            @createNew="(text: string) => goToCreate('Grupo de Serviço', text)"
            @remove="(item: any) => askDeactivation(item, 'GS')"
          />
        </div>

        <div class="painel">
          <label class="painel-rotulo">Código de Fornecedor</label>
          <SearchCombobox
            ref="cbCf"
            placeholder="Digite para buscar…"
            :load="api.supplierCodes.list"
            :nameOf="(item: any) => String(item.nome)"
            :idOf="(item: any) => String(item.codigoFornecedorSAP || item.id)"
            :allowRemove="isAdmin"
            v-model="supplierCode"
            @createNew="(text: string) => goToCreate('Código de Fornecedor', text)"
            @remove="(item: any) => askDeactivation(item, 'CF')"
          />
        </div>

        <div v-if="linkMode === 'unico'" class="painel">
          <label class="painel-rotulo">Código de Serviço</label>
          <SearchCombobox
            ref="cbCs"
            placeholder="Digite para buscar…"
            :load="api.serviceCodes.list"
            :nameOf="(item: any) => String(item.descricao)"
            :idOf="(item: any) => String(item.id)"
            :sapOf="(item: any) => `${item.quantidadeCaracteres ?? String(item.descricao || '').length}/40 SAP`"
            :allowRemove="isAdmin"
            v-model="serviceCode"
            @createNew="(text: string) => goToCreate('Código de Serviço', text)"
            @remove="(item: any) => askDeactivation(item, 'CS')"
          />
        </div>

        <div class="painel">
          <label class="painel-rotulo">Unidade de Medida</label>
          <SearchCombobox
            ref="cbUm"
            placeholder="Digite para buscar…"
            :load="api.unitsOfMeasure.list"
            :nameOf="(item: any) => `${item.sigla} - ${item.nome}`"
            :idOf="() => ''"
            :allowRemove="isAdmin"
            v-model="unitOfMeasure"
            @createNew="(text: string) => goToCreate('Unidade de Medida', text)"
            @remove="(item: any) => askDeactivation(item, 'UM')"
          />
        </div>
      </section>

      <div v-if="linkMode === 'unico'" class="linha-botoes">
        <button
          class="botao botao-roxo"
          :disabled="!complete || processing"
          @click="link"
        >
          {{ processing ? 'Processando…' : 'Vincular Serviço' }}
        </button>
        <button class="botao botao-escuro" @click="goToCreate('Código de Serviço', '')">
          Cadastrar Novo Item
        </button>
        <button class="botao botao-escuro" @click="clearAll">Limpar tudo</button>
      </div>

      <template v-if="linkMode === 'massa'">
        <label class="painel-rotulo">2 · Códigos de Serviço</label>
        <BulkServiceLink
          :key="bulkKey"
          :serviceGroup="serviceGroup"
          :supplierCode="supplierCode"
          :unitOfMeasure="unitOfMeasure"
          :user="props.user"
          @linked="onBulkLinked"
          @create="goToCreate('Código de Serviço', '')"
          @clear="clearAll"
        />
      </template>
    </div>

    <div v-if="previewModal" class="modal-fundo" @click.self="previewModal = false">
      <div class="modal">
        <h3>Vincular Serviço</h3>
        <div v-if="preview" class="item-similar" style="display: block">
          <div><span class="suave">ID Completo:</span> <strong>{{ preview.idCompleto }}</strong></div>
          <div style="margin-top: 6px"><span class="suave">Descrição:</span> {{ preview.descricaoSnapshot }}</div>
          <div style="margin-top: 6px">
            <span class="suave">Decisão:</span>
            <span class="badge" :class="decisionClass(preview.decisao)">{{ preview.decisao }}</span>
            <BadgeMatch v-if="preview.melhorScore > 0" :score="preview.melhorScore" />
          </div>

          <div
            v-for="alert in preview.alertasComponentes || []"
            :key="alert.tipo"
            class="item-similar"
            style="margin-top: 10px"
          >
            <span>{{ alert.tipo }}:</span>
            <span>“{{ alert.descricaoInformada }}”</span>
            <span class="suave">semelhante a</span>
            <span>{{ alert.itemSimilarNome }}</span>
            <BadgeException v-if="alert.casoIsolado" :motivo="alert.motivoCasoIsolado" />
            <BadgeMatch :score="alert.score" />
          </div>
        </div>
        <div class="linha-botoes">
          <button class="botao botao-escuro" @click="previewModal = false">Voltar</button>
          <button
            v-if="preview?.statusExistente === 'PENDENTE_VALIDACAO'"
            class="botao botao-escuro"
            @click="managePending"
          >
            Gerenciar pedido pendente
          </button>
          <button class="botao botao-roxo" :disabled="processing" @click="confirmLink">Confirmar</button>
        </div>
      </div>
    </div>

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

    <DuplicateBlockModal
      v-if="blockModal"
      :itens="blockItems"
      :motivo="blockReason"
      @reuse="reuseItem"
      @governance="openGovernance"
      @back="blockModal = false"
    />

    <GovernanceModal
      v-if="governanceModal"
      itemClass="Código de Serviço"
      :descricao="newServiceDescription"
      @back="governanceModal = false"
      @confirm="confirmGovernance"
    />

    <ConfirmModal
      v-if="deactivation"
      title="Desativar componente do catálogo?"
      :message="`${typeLabel(deactivation.tipo)} “${deactivation.label}” sairá de circulação: some das listas de uso e da similaridade, mas nada é perdido. É possível reativar no Catálogo quando quiser.`"
      confirm-label="Desativar"
      :dangerous-action="false"
      @confirm="confirmDeactivation"
      @cancel="deactivation = null"
    />

    <div v-if="success" class="modal-fundo" @click.self="success = null">
      <div class="modal">
        <div class="aviso aviso-verde">
          <span class="icone">✓</span>
          <div><strong>{{ success.title }}</strong><br />{{ success.message }}</div>
        </div>
        <div class="barra-tempo">
          <div class="barra-tempo-fill" :style="{ width: timeLeft + '%' }"></div>
        </div>
        <div class="suave centralizado" style="margin-top: 8px; font-size: 0.82rem">
          Fecha automaticamente em {{ Math.ceil(timeLeft / 100 * 5) }}s…
        </div>
        <div class="linha-botoes" style="margin-top: 14px">
          <button class="botao botao-roxo" @click="clearAll">Novo registro</button>
          <button v-if="isAdmin" class="botao botao-escuro" @click="$router.push('/admin')">Ver Painel Admin</button>
        </div>
      </div>
    </div>

    <div v-if="isAdmin" class="painel" style="margin-top: 34px">
      <label class="painel-rotulo">Resumo da base</label>
      <div class="indicadores" style="margin: 0">
        <div class="indicador">
          <div class="rotulo">Serviços únicos</div>
          <div class="valor" style="color: var(--azul)">{{ statistics.catalogo?.servicos?.toLocaleString('pt-BR') ?? '…' }}</div>
        </div>
        <div class="indicador">
          <div class="rotulo">Registros originais</div>
          <div class="valor">{{ statistics.matriz?.total?.toLocaleString('pt-BR') ?? '…' }}</div>
        </div>
        <div class="indicador">
          <div class="rotulo">Menos repetições</div>
          <div class="valor" style="color: var(--verde)">{{ reduction }}%</div>
        </div>
        <div class="indicador">
          <div class="rotulo">Aguardando revisão</div>
          <div class="valor" style="color: var(--amarelo-alerta)">{{ statistics.matriz?.pendentes?.toLocaleString('pt-BR') ?? '…' }}</div>
        </div>
      </div>
      <div class="linha" style="margin-top: 14px">
        <button class="botao botao-escuro" @click="$router.push('/admin')">Abrir Painel Admin →</button>
      </div>
    </div>

    <div v-else class="guia">
      <button class="guia-cabecalho" :class="guideOpen ? 'isOpen' : ''" @click="guideOpen = !guideOpen">
        <span class="seta">{{ guideOpen ? '▾' : '▸' }}</span>
        <div>
          <h2 style="margin: 0; font-size: 1.12rem; color: var(--azul-escuro)">Guia rápido</h2>
          <currentPreview class="suave" style="margin: 2px 0 0">
            {{ guideOpen ? 'Tudo o que você pode encontrar ao usar o sistema.' : 'Toque para ver como o sistema funciona e o que esperar.' }}
          </currentPreview>
        </div>
      </button>

      <div v-if="guideOpen" class="grade-seletores" style="margin-top: 18px">
        <div class="painel">
          <label class="painel-rotulo">1 · Como vincular um serviço</label>
          <currentPreview class="suave" style="margin: 0; line-height: 1.6">
            Escolha um item em cada painel — <strong>Grupo de Serviço</strong>, <strong>Código de Fornecedor</strong>,
            <strong>Código de Serviço</strong> e <strong>Unidade de Medida</strong> — e clique em
            <strong>Vincular Serviço</strong>. Você verá uma prévia com o código completo e a
            decisão antes de confirmar.
          </currentPreview>
          <currentPreview class="suave" style="margin: 10px 0 0; line-height: 1.6">
            Para vários de uma vez, use a aba <strong>Vincular em massa</strong>: escolha Grupo,
            Fornecedor e Unidade uma vez, marque os Códigos de Serviço com checkbox (buscando ou
            colando a lista) e o sistema avisa quantos <strong>já estão disponíveis</strong> e quantos
            <strong>precisam ser criados</strong> antes de vincular todos.
          </currentPreview>
        </div>

        <div class="painel">
          <label class="painel-rotulo">2 · O que significam as decisões</label>
          <div style="display: flex; flex-direction: column; gap: 8px">
            <div class="aviso aviso-verde" style="margin: 0">
              <span class="icone">✓</span>
              <div><strong>PERMITIR</strong> — sem item semelhante; pode salvar direto.</div>
            </div>
            <div class="aviso aviso-amarelo" style="margin: 0">
              <span class="icone">⚠</span>
              <div><strong>ALERTAR</strong> — há itens parecidos; confira antes de criar.</div>
            </div>
            <div class="aviso" style="margin: 0; background: var(--vermelho-fundo); border: 1px solid var(--vermelho); color: var(--vermelho)">
              <span class="icone">⛔</span>
              <div><strong>REVISAR</strong> — duplicidade provável; a criação é bloqueada.</div>
            </div>
          </div>
        </div>

        <div class="painel">
          <label class="painel-rotulo">3 · Se aparecer um item similar</label>
          <currentPreview class="suave" style="margin: 0 0 10px; line-height: 1.6">
            O sistema mostra os semelhantes com o <strong>% de match</strong> e o motivo.
            Você tem três caminhos:
          </currentPreview>
          <ul class="suave" style="margin: 0; padding-left: 18px; line-height: 1.9">
            <li><strong>Reutilizar o código existente</strong> — o recomendado;</li>
            <li><strong>Criar com justificativa</strong> — fica registrada na auditoria;</li>
            <li><strong>Enviar para Governança</strong> — um especialista decide depois.</li>
          </ul>
        </div>

        <div class="painel">
          <label class="painel-rotulo">4 · Cadastrar um item novo</label>
          <currentPreview class="suave" style="margin: 0; line-height: 1.6">
            Em qualquer busca, a opção <strong>＋ Cadastrar Novo Item</strong> aparece por
            último. A descrição do serviço deve caber em <strong>40 caracteres (limite SAP)</strong>
            — o contador na tela acompanha a digitação.
          </currentPreview>
        </div>

        <div class="painel">
          <label class="painel-rotulo">5 · Dicas de busca</label>
          <currentPreview class="suave" style="margin: 0; line-height: 1.6">
            A busca entende <strong>acentos ausentes</strong>, <strong>plural e singular</strong>
            (ex.: <em>telhados</em> acha <em>telhado</em> e vice-versa), pequenos erros de digitação e
            palavras invertidas (ex.: <em>mecanico manutencao</em>). Códigos legados também
            funcionam — digite <strong>4033776</strong> para achar o item original.
          </currentPreview>
        </div>

        <div class="painel">
          <label class="painel-rotulo">6 · Nada é perdido</label>
          <currentPreview class="suave" style="margin: 0; line-height: 1.6">
            Toda decisão (criação, bloqueio, justificativa) é <strong>auditada com autor e
            horário</strong>. A unificação de repetições apenas consolidam o catálogo — o
            histórico dos códigos legados é preservado.
          </currentPreview>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modos-vinculo {
  display: inline-flex;
  gap: 6px;
  margin: 0 0 8px;
  padding: 6px;
  border-radius: 14px;
  background: var(--cartao);
  border: 1px solid var(--borda);
  box-shadow: var(--sombra);
  max-width: 100%;
  flex-wrap: wrap;
}
.modos-vinculo .aba {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  background: transparent;
  border: 1.5px solid transparent;
  border-radius: 10px;
  padding: 9px 18px;
  font-size: 0.9rem;
  font-weight: 700;
  color: var(--texto-suave);
  cursor: pointer;
  text-align: left;
  transition: background 0.15s, color 0.15s, border-color 0.15s;
}
.modos-vinculo .aba:hover {
  color: var(--azul);
  background: var(--azul-claro);
}
.modos-vinculo .aba.ativa {
  background: var(--azul);
  border-color: var(--azul);
  color: var(--texto-sobre-azul);
  box-shadow: 0 4px 12px rgba(0, 32, 91, 0.18);
}
.modos-vinculo .aba.ativa .aba-texto small {
  color: var(--texto-sobre-azul);
  opacity: 0.85;
}
.aba-icone {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}
.aba-texto {
  display: flex;
  flex-direction: column;
  gap: 1px;
  line-height: 1.25;
}
.aba-texto small {
  font-size: 0.74rem;
  font-weight: 600;
  color: var(--texto-suave);
}
.modos-dica {
  margin: 0 0 18px;
  font-size: 0.85rem;
  color: var(--texto-suave);
  line-height: 1.55;
}
.painel-massa {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.painel-massa .grade-seletores .painel {
  background: transparent;
  border: none;
  border-top: none;
  box-shadow: none;
  padding: 0;
}
.grade-seletores.tres-colunas {
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
}
@media (max-width: 1100px) {
  .grade-seletores.tres-colunas {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 640px) {
  .grade-seletores.tres-colunas {
    grid-template-columns: 1fr;
  }
}
</style>
