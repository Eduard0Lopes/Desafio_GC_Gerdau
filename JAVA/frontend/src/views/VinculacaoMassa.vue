<script setup lang="ts">
import { computed, ref } from 'vue'
import { api, buildIdempotencyKey, errorMessage } from '../services/api'
import { toast } from '../services/toast'
import { pluralize } from '../services/formato'
import { normalize } from '../services/texto'
import { currentUser } from '../services/usuario'
import { useAutoClose } from '../composables/useFechamentoAutomatico'
import ServiceCodeMultiSelect from '../components/SelecaoMultiplaCS.vue'
import type { Item, BulkCodeItem, BatchItem, BatchLinkResult } from '../types'

const props = withDefaults(
  defineProps<{ serviceGroup: Item | null; supplierCode: Item | null; unitOfMeasure: Item | null; user?: string }>(),
  { user: '' }
)
const emit = defineEmits<{
  (event: 'linked', result: BatchLinkResult): void
  (event: 'create'): void
  (event: 'clear'): void
}>()

const chosen = ref<BulkCodeItem[]>([])
const confirmando = ref(false)
const processing = ref(false)
const result = ref<BatchLinkResult | null>(null)

const available = computed(() => chosen.value.filter((item) => item.classificacao === 'DISPONIVEL').length)
const toCreate = computed(() => chosen.value.filter((item) => item.classificacao === 'CRIAR').length)
const withAlert = computed(() => chosen.value.filter((item) => item.situacao === 'ALERTA').length)

const missing = computed(() => {
  const parts: string[] = []
  if (!props.serviceGroup) parts.push('Grupo de Serviço')
  if (!props.supplierCode) parts.push('Código de Fornecedor')
  if (!props.unitOfMeasure) parts.push('Unidade de Medida')
  if (!parts.length) return ''
  if (parts.length === 1) return parts[0]
  return `${parts.slice(0, -1).join(', ')} e ${parts[parts.length - 1]}`
})

const actionLabel = computed(() => {
  if (processing.value) return 'Processando…'
  if (!chosen.value.length) return 'Vincular em massa'
  return `Vincular ${chosen.value.length} ${pluralize(chosen.value.length, 'código', 'códigos')}`
})

const unitLabel = computed(() => {
  const unitOfMeasure = props.unitOfMeasure
  if (!unitOfMeasure) return 'a escolher'
  const sigla = String(unitOfMeasure.sigla ?? '')
  const nome = String(unitOfMeasure.nome ?? '')
  if (sigla && nome && nome !== sigla) return `${sigla} · ${nome}`
  return sigla || nome || itemLabel(unitOfMeasure, 'nome')
})
const compositionSummary = computed(
  () => `${itemLabel(props.serviceGroup, 'nome')} / ${itemLabel(props.supplierCode, 'nome')} / ${unitLabel.value}`
)

const pendingIssues = computed(() =>
  (result.value?.itens ?? []).filter((item) => item.classificacao === 'BLOQUEADA' || item.classificacao === 'FALHA')
)

const { timeLeft } = useAutoClose(
  result,
  () => {
    result.value = null
  },
  () => !pendingIssues.value.length
)

function itemLabel(item: Item | null, field: string): string {
  if (!item) return 'a escolher'
  const value = item[field] ?? item.id ?? ''
  return String(value) || '—'
}

function searchCodes(term: string) {
  return api.serviceCodes.list(term, 8)
}

async function analyzeLines(textos: string[]): Promise<BulkCodeItem[]> {
  const keyOf = (texto: string) => normalize(texto.trim())
  const unique: string[] = []
  const seen = new Set<string>()
  for (const texto of textos) {
    const key = keyOf(texto)
    if (!key || seen.has(key)) continue
    seen.add(key)
    unique.push(texto.trim())
  }
  const analyses = new Map<string, BulkCodeItem>()
  if (unique.length) {
    const lote = await api.similarityBatch(unique, 'CS')
    for (const item of lote.itens) {
      if (item.classificacao === 'BLOQUEADA') {
        toast.warning(`“${item.texto}” não entrou: ${item.motivo}`)
        continue
      }
      analyses.set(keyOf(item.texto), toItem(item))
    }
  }
  return textos.map((text) => analyses.get(keyOf(text))).filter((item): item is BulkCodeItem => Boolean(item))
}

function toItem(item: BatchItem): BulkCodeItem {
  const similar = item.similar
  const disponivel = item.classificacao === 'DISPONIVEL' && similar !== null
  return {
    key: disponivel && similar ? String(similar.id) : `novo:${normalize(item.texto.trim())}`,
    texto: item.texto,
    csId: disponivel && similar ? String(similar.id) : null,
    classificacao: disponivel ? 'DISPONIVEL' : 'CRIAR',
    situacao: item.situacao,
    motivo: item.motivo,
    match: similar
      ? { id: similar.id, descricao: similar.descricao, score: similar.score }
      : null
  }
}

function askConfirmation(): void {
  if (processing.value) return
  if (missing.value) {
    toast.error(`Falta escolher ${missing.value} para compor o vínculo.`)
    return
  }
  if (!chosen.value.length) {
    toast.warning('Marque ao menos um Código de Serviço para vincular.')
    return
  }
  confirmando.value = true
}

async function link(): Promise<void> {
  if (processing.value) return
  confirmando.value = false
  processing.value = true
  try {
    const response = await api.linkBatch(
      {
        gsId: String(props.serviceGroup?.id ?? ''),
        cfId: String(props.supplierCode?.id ?? ''),
        umId: String(props.unitOfMeasure?.id ?? ''),
        itens: chosen.value.map((item) => ({ csId: item.csId, texto: item.csId ? null : item.texto })),
        usuarioCriacaoId: props.user.trim() || currentUser()
      },
      buildIdempotencyKey(
        'lote-',
        String(props.serviceGroup?.id ?? ''),
        String(props.supplierCode?.id ?? ''),
        String(props.unitOfMeasure?.id ?? ''),
        chosen.value.map((item) => item.key).join('|'),
        String(Date.now())
      )
    )
    result.value = response
    const effective = response.vinculados + response.aguardandoValidacao
    toast.success(
      `${effective} ${pluralize(effective, 'vínculo processado', 'vínculos processados')} — ` +
        `${response.reutilizados} reutilizado(s), ${response.criados} criado(s).`
    )
    emit('linked', response)
  } catch (error) {
    toast.error(errorMessage(error))
  } finally {
    processing.value = false
  }
}

function newSelection(): void {
  close()
  chosen.value = []
}

function close(): void {
  result.value = null
}
</script>

<template>
  <div class="vinculacao-massa">
    <ServiceCodeMultiSelect v-model="chosen" :load="searchCodes" :analyze="analyzeLines" />

    <div class="massa-rodape">
      <div class="massa-resumo" aria-live="polite">
        <template v-if="chosen.length">
          <strong>{{ chosen.length }} {{ pluralize(chosen.length, 'código', 'códigos') }}</strong>
          <span class="verde">{{ available }} reutilizar</span>
          <span class="amarelo">{{ toCreate }} criar</span>
          <span v-if="withAlert" class="vermelho">{{ withAlert }} Governança</span>
        </template>
        <span v-else class="suave">Marque os códigos que quer vincular.</span>
        <span v-if="missing" class="vermelho">Falta escolher {{ missing }}.</span>
      </div>

      <div class="acoes">
        <button class="botao botao-roxo" @click="askConfirmation">{{ actionLabel }}</button>
        <button class="botao botao-escuro" @click="emit('create')">Cadastrar Novo Item</button>
        <button class="botao botao-escuro" @click="emit('clear')">Limpar tudo</button>
      </div>
    </div>

    <div v-if="confirmando" class="modal-fundo" @click.self="confirmando = false">
      <div class="modal">
        <h3>Vincular em massa</h3>
        <div class="item-similar" style="display: block">
          <div><span class="suave">Composição:</span> <strong>{{ compositionSummary }}</strong></div>
          <div style="margin-top: 6px">
            <span class="suave">Códigos:</span>
            <span class="badge badge-permitir">{{ available }} reutilizar</span>
            <span class="badge badge-alertar">{{ toCreate }} criar</span>
            <span v-if="withAlert" class="badge badge-revisar">{{ withAlert }} Governança</span>
          </div>
        </div>
        <div class="linha-botoes">
          <button class="botao botao-escuro" @click="confirmando = false">Voltar</button>
          <button class="botao botao-roxo" :disabled="processing" @click="link">Confirmar</button>
        </div>
      </div>
    </div>

    <div v-if="result" class="modal-fundo" @click.self="close">
      <div class="modal">
        <div class="aviso" :class="pendingIssues.length ? 'aviso-amarelo' : 'aviso-verde'">
          <span class="icone">{{ pendingIssues.length ? '⚠' : '✓' }}</span>
          <div>
            <strong>{{ result.vinculados }} {{ pluralize(result.vinculados, 'vínculo ativo', 'vínculos ativos') }}</strong>
            <span v-if="result.aguardandoValidacao">
              · {{ result.aguardandoValidacao }} aguardando validação da Governança
            </span>
            <br />{{ result.reutilizados }} reutilizado(s), {{ result.criados }} criado(s).
          </div>
        </div>

        <ul v-if="pendingIssues.length" class="lista-pendencias">
          <li v-for="pendingIssue in pendingIssues" :key="pendingIssue.linha">
            <strong>{{ pendingIssue.texto }}</strong> <span class="suave">— {{ pendingIssue.motivo }}</span>
          </li>
        </ul>

        <template v-if="!pendingIssues.length">
          <div class="barra-tempo">
            <div class="barra-tempo-fill" :style="{ width: timeLeft + '%' }"></div>
          </div>
          <div class="suave centralizado" style="margin-top: 8px; font-size: 0.82rem">
            Fecha automaticamente em {{ Math.ceil((timeLeft / 100) * 5) }}s…
          </div>
        </template>

        <div class="linha-botoes" style="margin-top: 14px">
          <button class="botao botao-roxo" @click="newSelection">Nova seleção</button>
          <button class="botao botao-escuro" @click="close">Fechar</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.massa-rodape {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
  padding-top: 14px;
  border-top: 1px solid var(--borda);
}
.massa-resumo {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  font-size: 0.87rem;
}
.massa-resumo .verde { color: var(--verde); font-weight: 700; }
.massa-resumo .amarelo { color: var(--amarelo-alerta); font-weight: 700; }
.massa-resumo .vermelho { color: var(--vermelho); font-weight: 700; }
.massa-rodape .acoes {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.lista-pendencias {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 0.86rem;
  line-height: 1.5;
}
</style>
