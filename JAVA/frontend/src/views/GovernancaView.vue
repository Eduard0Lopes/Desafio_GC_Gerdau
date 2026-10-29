<script setup lang="ts">
import { computed, inject, onMounted, reactive, ref } from 'vue'
import { api, errorMessage } from '../services/api'
import { toast } from '../services/toast'
import { debounce } from '../services/tempo'
import { shortTypeLabel } from '../services/rotulos'
import { isAdmin } from '../services/usuario'
import BadgeException from '../components/BadgeExcecao.vue'
import BadgeMatch from '../components/BadgeMatch.vue'
import ConfirmModal from '../components/ModalConfirmacao.vue'
import type { SimilarCandidate, GovernanceItem, GovernanceSummary } from '../types'

interface GroupUsage { nome: string; quantidade: number }
interface FullSummary extends Partial<GovernanceSummary> {
  grupos?: GroupUsage[]
  totalLegados?: number
}

const props = withDefaults(defineProps<{ user?: string }>(), { user: '' })

function podeDecidir(item: GovernanceItem): boolean {
  if (!isAdmin()) return false
  return item.origem === 'HISTORICO'
    ? item.status === 'PENDENTE'
    : item.status === 'PENDENTE_VALIDACAO'
}
const emit = defineEmits<{
  (event: 'pending', count: number): void
  (event: 'apiUnavailable', v: boolean): void
}>()
const updatePending = inject<() => void>('updatePending', () => {})

const summary = ref<FullSummary>({})
const itens = ref<GovernanceItem[]>([])
const notes = reactive<Record<string, string>>({})
const processing = ref<string | null>(null)
const origem = ref('HISTORICO')
const search = ref('')
const filter = ref<string>('TODOS')
const page = ref(0)
const pageSize = 4
const hasMore = ref(false)
const queueTotal = ref(0)
const totalPages = computed(() => Math.max(1, Math.ceil(queueTotal.value / pageSize)))
const confirmation = ref<{ title: string; message: string; label: string; perigosa: boolean; acao: string; item: GovernanceItem } | null>(null)

const chosen = reactive<Record<string, string>>({})
const reuse = ref<{ item: GovernanceItem; candidato: SimilarCandidate } | null>(null)

function choiceKey(similar: SimilarCandidate): string {
  return `${(similar.tipo || 'CS').toUpperCase()}:${similar.id}`
}

function chooseMatch(item: GovernanceItem, similar: SimilarCandidate): void {
  if (!podeDecidir(item)) return
  chosen[item.id] = choiceKey(similar)
}

function askReuse(item: GovernanceItem): void {
  const key = chosen[item.id]
  const candidato = (item.similares || []).find((similar) => choiceKey(similar) === key)
  if (!candidato) {
    toast.warning('Escolha um dos matches acima para reutilizar.')
    return
  }
  reuse.value = { item, candidato }
}

async function confirmReuse(): Promise<void> {
  const pendingReuse = reuse.value
  if (!pendingReuse || processing.value === pendingReuse.item.id) return
  reuse.value = null
  processing.value = pendingReuse.item.id
  try {
    await api.reuseCandidate(pendingReuse.item.id, {
      escolhidoId: pendingReuse.candidato.id,
      escolhidoTipo: pendingReuse.candidato.tipo || 'CS',
      aprovadoPor: props.user || 'governance',
      observacao: (notes[pendingReuse.item.id] || '').trim() || null
    })
    toast.success(
      `Pedido ${pendingReuse.item.identificador} resolvido reutilizando ${pendingReuse.candidato.id} — ${pendingReuse.candidato.descricao}.`
    )
    itens.value = itens.value.filter((item) => item.id !== pendingReuse.item.id)
    await load()
    updatePending()
  } catch (error) {
    toast.error(errorMessage(error))
  } finally {
    processing.value = null
  }
}

const filterLabel = computed(() => ({
  PENDENTES: 'aguardando revisão', DUPLICIDADE: 'possíveis repetidos', ALERTA: 'em atenção',
  REVISADOS: 'revisados', NOVOS: 'novos cadastros'
} as Record<string, string>)[filter.value] || filter.value)

const filteredItems = computed(() => {
  switch (filter.value) {
    case 'DUPLICIDADE':
    case 'ALERTA':
      return itens.value.filter((item) => item.decision === filter.value)
    case 'REVISADOS': return itens.value.filter((item) => item.status === 'REVISADO')
    case 'PENDENTES': return itens.value.filter((item) => item.status === 'PENDENTE')
    default: return itens.value
  }
})

function toggleFilter(filterKey: string): void {
  filter.value = filter.value === filterKey || filterKey === 'TODOS' ? 'TODOS' : filterKey
  page.value = 0
  if (filterKey === 'NOVOS') {
    filter.value = 'TODOS'
    origem.value = 'NOVOS'
    load()
    return
  }
  if (filterKey !== 'TODOS' && origem.value !== 'HISTORICO') {
    origem.value = 'HISTORICO'
  }
  load()
}

function nextPage(): void {
  if (!hasMore.value) return
  page.value++
  load()
}

function previousPage(): void {
  if (page.value === 0) return
  page.value--
  load()
}

function skipPages(count: number): void {
  page.value = Math.min(page.value + count, totalPages.value - 1)
  load()
}

function goToPage(count: number): void {
  page.value = Math.max(0, Math.min(count, totalPages.value - 1))
  load()
}

async function load(): Promise<void> {
  try {
    summary.value = (await api.summary()) as FullSummary
    const statusServidor =
      filter.value === 'REVISADOS' ? 'REVISADO'
      : filter.value === 'SEM_PENDENCIA' ? 'SEM_PENDENCIA'
      : filter.value === 'DUPLICIDADE' || filter.value === 'ALERTA' ? 'TODOS'
      : 'PENDENTE'
    const decisaoServidor =
      filter.value === 'DUPLICIDADE' ? 'DUPLICIDADE'
      : filter.value === 'ALERTA' ? 'ALERTA'
      : undefined
    const lote = await api.pendentes(origem.value, search.value, pageSize, statusServidor, decisaoServidor, page.value)
    itens.value = lote
    hasMore.value = lote.length === pageSize
    const count = await api.countQueue(origem.value, statusServidor, decisaoServidor, search.value)
    queueTotal.value = count.total
    emit('pending', (summary.value.pendentesHistorico || 0) + (summary.value.pendentesNovos || 0))
    emit('apiUnavailable', false)
  } catch {
    emit('apiUnavailable', true)
  }
}

function switchOrigin(o: string): void {
  origem.value = o
  page.value = 0
  load()
}

const debouncedLoad = debounce(() => void load(), 350)

function askConfirmation(item: GovernanceItem, acao: string): void {
  const ehDuplicidade = item.decision === 'DUPLICIDADE'
  const textos: Record<string, { title: string; message: string; label: string; perigosa: boolean }> = {
    keep: {
      title: 'Aceitar este item?',
      message: `O item ${item.identificador} será aceito como válido${ehDuplicidade
        ? ' — a possível repetição está aceitável' : ''}. O motivo informado fica registrado na auditoria.`,
      label: '✓ Aceitar', perigosa: false
    },
    remove: {
      title: 'Apagar do catálogo?',
      message: `O item ${item.identificador} será apagado da lista ativa (o registro original continua preservado no histórico). O motivo informado fica registrado na auditoria.`,
      label: '✕ Apagar', perigosa: true
    },
    approve: {
      title: 'Aprovar cadastro?',
      message: `O cadastro ${item.identificador} será aprovado e ativado. O motivo informado fica registrado na auditoria.`,
      label: '✓ Aprovar', perigosa: false
    },
    reject: {
      title: 'Rejeitar cadastro?',
      message: `O cadastro ${item.identificador} será rejeitado. O motivo informado fica registrado na auditoria.`,
      label: '✕ Rejeitar', perigosa: true
    }
  }
  confirmation.value = { ...textos[acao], acao, item }
}

const HISTORY_ACTION: Record<string, { acao: string; passado: string }> = {
  keep: { acao: 'MANTER', passado: 'aceito' },
  remove: { acao: 'EXCLUIR', passado: 'apagado' }
}

async function confirmDecision(): Promise<void> {
  const pendingDecision = confirmation.value
  if (!pendingDecision) return
  const { item, acao } = pendingDecision

  const motivo = (notes[item.id] || '').trim()
  if (!motivo) {
    toast.warning('Informe o motivo da decisão — ele é obrigatório.')
    return
  }
  confirmation.value = null
  processing.value = item.id
  try {
    const historico = HISTORY_ACTION[acao]
    if (historico) {
      await api.reviewHistory(item.id, {
        revisadoPor: props.user || 'governance',
        acao: historico.acao,
        observacao: motivo
      })
      toast.success(`Item ${item.identificador} ${historico.passado} — decisão registrada.`)
    } else if (acao === 'approve') {
      await api.approve(item.id, { aprovadoPor: props.user || 'governance', observacao: motivo })
      toast.success(`Cadastro ${item.identificador} aprovado.`)
    } else {
      await api.reject(item.id, { aprovadoPor: props.user || 'governance', observacao: motivo })
      toast.success(`Cadastro ${item.identificador} rejeitado.`)
    }
    itens.value = itens.value.filter((item) => item.id !== item.id)
    await load()
    updatePending()
  } catch (error) {
    toast.error(errorMessage(error))
  } finally {
    processing.value = null
  }
}

onMounted(load)
</script>

<template>
  <div>
    <p class="suave" style="margin: 0 0 22px">
      Itens sinalizados como repetição no histórico e novos cadastros enviados para validação.
      <strong>Clique nos cartões</strong> para filtrar a fila.
    </p>

    <div class="indicadores">
      <div class="indicador" :class="{ ativo: filter === 'PENDENTES' }" @click="toggleFilter('PENDENTES')">
        <div class="rotulo">Aguardando revisão</div>
        <div class="valor" style="color: var(--amarelo-alerta)">{{ summary.pendentesHistorico ?? '…' }}</div>
      </div>
      <div class="indicador" :class="{ ativo: filter === 'DUPLICIDADE' }" @click="toggleFilter('DUPLICIDADE')">
        <div class="rotulo">Possíveis repetidos</div>
        <div class="valor" style="color: var(--vermelho)">{{ summary.decisionDuplicidade ?? '…' }}</div>
      </div>
      <div class="indicador" :class="{ ativo: filter === 'ALERTA' }" @click="toggleFilter('ALERTA')">
        <div class="rotulo">Em atenção</div>
        <div class="valor" style="color: var(--amarelo-alerta)">{{ summary.decisionAlerta ?? '…' }}</div>
      </div>
      <div class="indicador" :class="{ ativo: filter === 'REVISADOS' }" @click="toggleFilter('REVISADOS')">
        <div class="rotulo">Revisados</div>
        <div class="valor" style="color: var(--verde)">{{ summary.revisados ?? '…' }}</div>
      </div>
      <div class="indicador" :class="{ ativo: filter === 'NOVOS' }" @click="toggleFilter('NOVOS')">
        <div class="rotulo">Novos cadastros</div>
        <div class="valor">{{ summary.pendentesNovos ?? '…' }}</div>
      </div>
    </div>

    <div class="linha-campo" style="margin-bottom: 18px">
      <button
        class="botao"
        :class="origem === 'HISTORICO' ? 'botao-roxo' : 'botao-escuro'"
        @click="switchOrigin('HISTORICO')"
      >
        Base original
      </button>
      <button
        class="botao"
        :class="origem === 'NOVOS' ? 'botao-roxo' : 'botao-escuro'"
        @click="switchOrigin('NOVOS')"
      >
        Novos cadastros
      </button>
      <span v-if="filter !== 'TODOS'" class="badge badge-alertar">
        filtro: {{ filterLabel }}
        <span style="cursor: pointer; margin-left: 6px" @click="toggleFilter('TODOS')">✕</span>
      </span>
      <span style="flex: 1"></span>
      <div class="campo-busca-wrap" style="min-width: 260px">
        <input
          v-model="search"
          placeholder="Buscar por código, descrição…"
          style="background: var(--campo); border: 1px solid var(--borda); border-radius: 9px;
                 color: var(--texto); padding: 9px 12px; width: 100%"
          @input="debouncedLoad"
        />
        <button v-if="search" type="button" class="btn-limpar-busca" title="Limpar busca" @click="search = ''; load()">✕</button>
      </div>
    </div>

    <div v-if="!filteredItems.length" class="aviso aviso-verde">
      <span class="icone">✓</span>
      <div>Nenhum item {{ filterLabel === 'revisados' ? 'revisado' : 'aguardando decisão' }} neste filtro.</div>
    </div>

    <div class="grade-cards">
      <div v-for="item in filteredItems" :key="item.origem + item.id" class="painel card-item">

        <div class="card-cabecalho">
          <span class="badge" :class="item.origem === 'HISTORICO' ? 'badge-alertar' : 'badge-permitir'">
            {{ item.origem === 'HISTORICO' ? 'BASE ORIGINAL' : 'NOVO CADASTRO' }}
          </span>
          <span class="card-identificador">{{ item.identificador }}</span>
          <BadgeException v-if="item.casoIsolado" :motivo="item.motivoCasoIsolado" />
          <BadgeMatch :score="item.score" />
        </div>
        <div class="card-descricao">{{ item.descricao }}</div>

        <div class="card-campos">
          <div class="campo">
            <span class="rotulo">Grupo de Serviço</span>
            <span class="valor">{{ item.grupoServico || '—' }}</span>
          </div>
          <div class="campo">
            <span class="rotulo">Código de Fornecedor</span>
            <span class="valor">{{ item.fornecedor || '—' }}</span>
          </div>
          <div class="campo">
            <span class="rotulo">Unidade de Medida</span>
            <span class="valor">{{ item.unidadeMedida || '—' }}</span>
          </div>
          <div class="campo">
            <span class="rotulo">Código antigo</span>
            <span class="valor">{{ item.legacyId || '—' }}</span>
          </div>
          <div class="campo">
            <span class="rotulo">Natureza do serviço</span>
            <span class="valor">{{ item.natureza || '—' }}</span>
          </div>
          <div class="campo campo-largo">
            <span class="rotulo">Avaliação automática</span>
            <span class="valor">
              <strong>{{ item.decision }}</strong> — {{ item.reason }}
            </span>
          </div>
          <div v-if="item.solicitante" class="campo">
            <span class="rotulo">Criado por</span>
            <span class="valor">{{ item.solicitante }}</span>
          </div>
        </div>

        <div v-if="item.justificativa" class="justificativa-destaque">
          <div class="rotulo">Justificativa do solicitante</div>
          <blockquote>“{{ item.justificativa }}”</blockquote>
          <div v-if="item.solicitante" class="autoria">
            enviada por <strong>{{ item.solicitante }}</strong>
          </div>
        </div>

        <div v-if="item.similares && item.similares.length" class="nao-escolhidos">
          <div class="nao-escolhidos-cabecalho">
            <strong>O que o usuário não escolheu</strong>
            <span class="suave">
              — matches do bloqueio de duplicidade que ele ignorou ao enviar para a Governança.
              <template v-if="podeDecidir(item)">Se algum deles atende, escolha e siga por ele.</template>
            </span>
          </div>
          <div class="lista-selecao">
            <div
              v-for="similar in item.similares.slice(0, 5)"
              :key="choiceKey(similar)"
              class="opcao-item"
              :class="{ selecionada: chosen[item.id] === choiceKey(similar), clicavel: podeDecidir(item) }"
              @click="chooseMatch(item, similar)"
            >
              <input
                v-if="podeDecidir(item)"
                type="radio"
                :name="`match-${item.id}`"
                :checked="chosen[item.id] === choiceKey(similar)"
                @change="chooseMatch(item, similar)"
              />
              <div class="opcao-conteudo">
                <div class="opcao-linha">
                  <span class="opcao-id">{{ similar.id }}</span>
                  <span class="badge badge-permitir">{{ shortTypeLabel(similar.tipo) }}</span>
                  <span class="suave">|</span>
                  <strong>{{ (similar.descricao || '').toUpperCase() }}</strong>
                  <BadgeException v-if="similar.casoIsolado" :motivo="similar.motivoCasoIsolado" />
                  <BadgeMatch :score="similar.score" />
                </div>
                <div v-if="similar.motivo" class="opcao-motivo">{{ similar.motivo }}</div>
              </div>
            </div>
          </div>
        </div>

        <div class="linha-campo" style="margin-top: 14px">
          <textarea
            v-model="notes[item.id]"
            placeholder="Motivo da decisão (obrigatório)"
            style="min-height: 58px"
          ></textarea>
        </div>

        <div v-if="item.origem === 'HISTORICO' && item.status === 'REVISADO'" class="aviso aviso-verde" style="margin: 12px 0 0">
          <span class="icone">✓</span>
          <div>
            Decisão registrada por <strong>{{ item.solicitante || 'governança' }}</strong>.
            <span v-if="item.justificativa" class="suave"> — {{ item.justificativa }}</span>
          </div>
        </div>

        <div v-if="podeDecidir(item) && item.origem === 'HISTORICO'" class="linha-botoes" style="margin-top: 12px">
            <button class="botao botao-verde" :disabled="processing === item.id" @click="askConfirmation(item, 'keep')">
              ✓ Aceitar
            </button>
            <button class="botao botao-vermelho" :disabled="processing === item.id" @click="askConfirmation(item, 'remove')">
              ✕ Apagar
            </button>
        </div>
        <div v-else-if="podeDecidir(item)" class="linha-botoes" style="margin-top: 12px">
            <button
              v-if="item.similares && item.similares.length"
              class="botao botao-roxo"
              :disabled="processing === item.id || !chosen[item.id]"
              :title="chosen[item.id] ? 'O pedido segue reutilizando o código escolhido' : 'Escolha um dos matches acima'"
              @click="askReuse(item)"
            >
              ⇄ Reutilizar escolhido
            </button>
            <button class="botao botao-verde" :disabled="processing === item.id" @click="askConfirmation(item, 'approve')">
              ✓ Aprovar cadastro
            </button>
            <button class="botao botao-vermelho" :disabled="processing === item.id" @click="askConfirmation(item, 'reject')">
              ✕ Rejeitar cadastro
            </button>
        </div>
      </div>
    </div>

    <div class="paginacao" style="margin-top: 20px">
      <button class="pulo" :disabled="page === 0" @click="goToPage(0)" title="Início">|‹</button>
      <button class="pulo" :disabled="page === 0" @click="previousPage" title="Página anterior">‹</button>
      <span class="info"><strong>{{ page + 1 }}</strong> / {{ totalPages }}</span>
      <button class="pulo" :disabled="!hasMore" @click="nextPage" title="Próxima página">›</button>
      <button class="pulo" :disabled="!hasMore" @click="skipPages(10)" title="Avançar 10 páginas (ou ir ao fim)">››</button>
      <button class="pulo" :disabled="page + 1 >= totalPages" @click="goToPage(totalPages - 1)" title="Fim">›|</button>
    </div>

    <ConfirmModal
      v-if="confirmation"
      :title="confirmation.title"
      :message="confirmation.message"
      :confirm-label="confirmation.label"
      :dangerous-action="confirmation.perigosa"
      @confirm="confirmDecision"
      @cancel="confirmation = null"
    />

    <ConfirmModal
      v-if="reuse"
      title="Reutilizar o código escolhido?"
      :message="`O pedido ${reuse.item.identificador} será resolvido reutilizando ${reuse.candidato.id} — ${reuse.candidato.descricao}. A composição passa a apontar para este código, o código novo do pedido não entra no catálogo e a decisão fica registrada na auditoria.`"
      confirm-label="⇄ Reutilizar e seguir"
      :dangerous-action="false"
      @confirm="confirmReuse"
      @cancel="reuse = null"
    />
  </div>
</template>

<style scoped>
.grade-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(460px, 1fr));
  gap: 16px;
}
.card-item {
  display: flex;
  flex-direction: column;
}
.card-cabecalho {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}
.card-identificador {
  font-size: 0.76rem; font-weight: 600; letter-spacing: 0.04em;
  color: var(--texto-suave); opacity: 0.8;
  font-variant-numeric: tabular-nums;
}
.card-descricao {
  font-size: 1.04rem; font-weight: 700;
  line-height: 1.5;
  margin-bottom: 14px;
}
.card-campos {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px 18px;
}
.card-campos .campo {
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.card-campos .campo-largo {
  grid-column: 1 / -1;
}
.card-campos .rotulo {
  font-size: 0.72rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--texto-suave);
  font-weight: 700;
}
.card-campos .valor {
  font-size: 0.95rem;
  line-height: 1.5;
}
.nao-escolhidos {
  margin-top: 12px;
  border: 1px dashed var(--borda);
  border-radius: 10px;
  padding: 12px 14px;
}
.nao-escolhidos-cabecalho {
  font-size: 0.88rem;
  margin-bottom: 10px;
  line-height: 1.5;
}
.lista-selecao {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.opcao-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  background: var(--fundo);
  border: 1.5px solid var(--borda);
  border-radius: 10px;
  padding: 11px 14px;
  transition: all 0.12s;
}
.opcao-item.clicavel {
  cursor: pointer;
}
.opcao-item.clicavel:hover {
  border-color: var(--azul);
}
.opcao-item.selecionada {
  border-color: var(--azul);
  background: var(--azul-claro);
}
.opcao-item input[type='radio'] {
  margin-top: 3px;
  accent-color: var(--azul);
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}
.opcao-conteudo {
  flex: 1;
  min-width: 0;
}
.opcao-linha {
  display: flex;
  align-items: baseline;
  gap: 10px;
  flex-wrap: wrap;
  font-variant-numeric: tabular-nums;
}
.opcao-id {
  font-size: 0.72rem; font-weight: 600; letter-spacing: 0.04em;
  color: var(--texto-suave); opacity: 0.75;
}
.opcao-motivo {
  margin-top: 5px;
  color: var(--texto-suave);
  font-size: 0.84rem;
}
@media (max-width: 1020px) {
  .grade-cards {
    grid-template-columns: 1fr;
  }
}
</style>
