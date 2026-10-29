<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { api } from '../services/api'
import { toast } from '../services/toast'
import { downloadCsv, parseCsv } from '../services/csv'
import { formatDate } from '../services/formato'
import { statusClass, statusLabel } from '../services/rotulos'
import { matchesTerm, normalize, normalizeHeader } from '../services/texto'
import ConfirmModal from '../components/ModalConfirmacao.vue'
import SearchCombobox from '../components/ComboboxBusca.vue'
import type { AuditEvent, Item, ImportItem } from '../types'

type TabKey = 'GS' | 'CF' | 'CS' | 'UM'
type ComponentKind = 'GS' | 'CF' | 'CS' | 'UM'

const props = withDefaults(defineProps<{ user?: string; role?: string }>(), { user: '', role: 'USUARIO' })
const isAdmin = computed(() => props.role === 'ADMIN')

interface Column { key: string; label: string }
interface Tab {
  key: TabKey
  label: string
  columns: Column[]
  load: (query?: string, limite?: number, incluirTodos?: boolean) => Promise<unknown[]>
  labelOf: (index: Item) => string
}

const tabs: Tab[] = [
  {
    key: 'GS',
    label: 'Grupo de Serviço',
    columns: [{ key: 'id', label: 'ID' }, { key: 'nome', label: 'Nome' }, { key: 'origem', label: 'Origem' },
      { key: 'createdAt', label: 'Criado em' }, { key: 'criadoPor', label: 'Criado por' }],
    load: api.serviceGroups.list,
    labelOf: (index) => String(index.nome ?? index.id)
  },
  {
    key: 'CF',
    label: 'Código de Fornecedor',
    columns: [
      { key: 'id', label: 'ID' },
      { key: 'codigoFornecedorSAP', label: 'SAP' },
      { key: 'nome', label: 'Nome' },
      { key: 'origem', label: 'Origem' },
      { key: 'createdAt', label: 'Criado em' },
      { key: 'criadoPor', label: 'Criado por' }
    ],
    load: api.supplierCodes.list,
    labelOf: (index) => String(index.nome ?? index.id)
  },
  {
    key: 'CS',
    label: 'Código de Serviço',
    columns: [
      { key: 'id', label: 'ID' },
      { key: 'descricao', label: 'Descrição' },
      { key: 'quantidadeCaracteres', label: 'Caracteres' },
      { key: 'origem', label: 'Origem' },
      { key: 'createdAt', label: 'Criado em' },
      { key: 'criadoPor', label: 'Criado por' }
    ],
    load: api.serviceCodes.list,
    labelOf: (index) => String(index.descricao ?? index.id)
  },
  {
    key: 'UM',
    label: 'Unidade de Medida',
    columns: [
      { key: 'sigla', label: 'Sigla' },
      { key: 'nome', label: 'Nome' },
      { key: 'origem', label: 'Origem' },
      { key: 'createdAt', label: 'Criado em' },
      { key: 'criadoPor', label: 'Criado por' }
    ],
    load: api.unitsOfMeasure.list,
    labelOf: (index) => `${String(index.sigla ?? '')} - ${String(index.nome ?? '')}`
  }
]

const tab = ref<TabKey>('GS')
const search = ref('')
const page = ref(0)
const porPagina = 25
const data = ref<Record<TabKey, Item[]>>({ GS: [], CF: [], CS: [], UM: [] })
const carregado = ref<Record<TabKey, boolean>>({ GS: false, CF: false, CS: false, UM: false })
const selecionados = ref<Item[]>([])
const confirmando = ref<'DESATIVADO' | 'ATIVO' | null>(null)
const applying = ref(false)
const failures = ref<{ id: string; motivo: string }[]>([])

const currentTab = computed(() => tabs.find((section) => section.key === tab.value) as Tab)
const totals = computed<Record<string, number>>(() => ({
  GS: data.value.GS.length,
  CF: data.value.CF.length,
  CS: data.value.CS.length,
  UM: data.value.UM.length
}))

const onlyExceptions = ref(false)
const exceptionCount = computed(() => data.value[tab.value].filter((index) => Boolean(index.casoIsolado)).length)
function toggleExceptionFilter(): void {
  onlyExceptions.value = !onlyExceptions.value
  page.value = 0
}

const changesModal = ref(false)
const changes = ref<AuditEvent[]>([])
const loadingChanges = ref(false)
async function openChanges(): Promise<void> {
  changesModal.value = true
  loadingChanges.value = true
  try {
    const events = await api.events()
    const actions = new Set(['EXCLUSAO_COMPONENTE', 'TRANSFERENCIA_VINCULOS', 'DESATIVACAO_COMPONENTE', 'REATIVACAO_COMPONENTE'])
    changes.value = events.filter((event) => actions.has(event.action))
  } catch (error) {
    toast.error((error as Error).message)
  } finally {
    loadingChanges.value = false
  }
}
function changeLabel(event: AuditEvent): string {
  if (event.action === 'EXCLUSAO_COMPONENTE') return 'Excluído'
  if (event.action === 'TRANSFERENCIA_VINCULOS') return event.decision === 'MESCLAGEM' ? 'Mesclado' : 'Transferido'
  if (event.action === 'DESATIVACAO_COMPONENTE') return 'Desativado'
  return 'Reativado'
}
function changeClass(event: AuditEvent): string {
  return event.action === 'EXCLUSAO_COMPONENTE' ? 'badge-revisar'
    : event.action === 'TRANSFERENCIA_VINCULOS' ? 'badge-alertar' : 'badge-permitir'
}

const resolving = ref<Item | null>(null)
const resolvingNow = ref(false)
const deleting = ref<Item | null>(null)
const marking = ref<Item | null>(null)
const markingReason = ref('')
function openResolution(item: Item): void {
  resolving.value = item
}
function openMarking(item: Item): void {
  marking.value = item
  markingReason.value = ''
}
async function confirmMarking(): Promise<void> {
  const item = marking.value
  const motivo = markingReason.value.trim()
  if (!item || !motivo || resolvingNow.value) return
  resolvingNow.value = true
  marking.value = null
  try {
    await api.catalogo.markException(String(item.id), motivo)
    item.casoIsolado = true
    item.motivoCasoIsolado = motivo
    toast.success('Item marcado com (!) — ele aparece no filtro Casos especiais.')
  } catch (error) {
    toast.error((error as Error).message)
  } finally {
    resolvingNow.value = false
  }
}
function askDeactivation(): void {
  deleting.value = resolving.value
  resolving.value = null
}
async function confirmResolution(): Promise<void> {
  const item = resolving.value
  if (!item || resolvingNow.value) return
  resolvingNow.value = true
  resolving.value = null
  try {
    await api.catalogo.clearSpecialCase(String(item.id))
    item.casoIsolado = false
    item.motivoCasoIsolado = null
    toast.success('Caso resolvido — o (!) foi removido do item.')
  } catch (error) {
    toast.error((error as Error).message)
  } finally {
    resolvingNow.value = false
  }
}
async function confirmDeletion(): Promise<void> {
  const item = deleting.value
  if (!item || resolvingNow.value) return
  resolvingNow.value = true
  deleting.value = null
  try {
    await api.components.remove(tab.value as ComponentKind, String(item.id))
    const list = data.value[tab.value]
    const index = list.findIndex((candidato) => String(candidato.id) === String(item.id))
    if (index >= 0) list.splice(index, 1)
    selecionados.value = selecionados.value.filter((selecionado) => String(selecionado.id) !== String(item.id))
    toast.success(`Item ${item.id} excluído definitivamente.`)
  } catch (error) {
    const errorMessage = (error as Error).message || ''
    if (errorMessage.toLowerCase().includes('vincul')) {
      await openTransfer(item)
    } else {
      toast.error(errorMessage)
    }
  } finally {
    resolvingNow.value = false
  }
}

const transferring = ref<Item | null>(null)
const linkTarget = ref<Item | null>(null)
const contagemVinculos = ref(0)
const transferInProgress = ref(false)

async function openTransfer(item: Item): Promise<void> {
  transferring.value = item
  linkTarget.value = null
  contagemVinculos.value = 0
  try {
    const rawValue = await api.catalogo.vinculos(tab.value as 'GS' | 'CF' | 'CS' | 'UM', String(item.id))
    contagemVinculos.value = rawValue.total
  } catch {
    contagemVinculos.value = 0
  }
}
function closeTransfer(): void {
  transferring.value = null
  linkTarget.value = null
}
async function confirmTransfer(): Promise<void> {
  const item = transferring.value
  const destino = linkTarget.value
  if (!item || !destino || transferInProgress.value) return
  transferInProgress.value = true
  try {
    const response = await api.catalogo.transferLinks({
      tipo: tab.value as 'GS' | 'CF' | 'CS' | 'UM',
      de: String(item.id),
      para: String(destino.id),
      excluirOrigem: true
    })
    if (response.excluido) {
      const list = data.value[tab.value]
      const index = list.findIndex((candidato) => String(candidato.id) === String(item.id))
      if (index >= 0) list.splice(index, 1)
      selecionados.value = selecionados.value.filter((selecionado) => String(selecionado.id) !== String(item.id))
      const parts = [`${response.transferidos} vínculo(s) transferido(s) para ${destino.id}`]
      if (response.mesclados) parts.push(`${response.mesclados} composição(ões) redundante(s) mesclada(s)`)
      toast.success(`${parts.join(' e ')} — ${item.id} excluído definitivamente.`)
      closeTransfer()
    } else {
      toast.warning(`Transferidos ${response.transferidos}, mas ${response.conflitos.length} composição(ões) não puderam ser movidas — confira os vínculos antes de excluir.`)
    }
  } catch (error) {
    toast.error((error as Error).message)
  } finally {
    transferInProgress.value = false
  }
}

const filtered = computed<Item[]>(() => {
  const term = search.value.trim()
  const list = data.value[tab.value]
  const base = onlyExceptions.value ? list.filter((index) => Boolean(index.casoIsolado)) : list
  if (!term) return base
  return base.filter((index) =>
    matchesTerm(
      `${String(index.id ?? '')} ${String(index.nome ?? '')} ${String(index.descricao ?? '')} ${String(index.sigla ?? '')} ${String(index.codigoFornecedorSAP ?? '')}`,
      term
    )
  )
})

const totalPages = computed(() => Math.max(1, Math.ceil(filtered.value.length / porPagina)))

const order = ref<{ key: string; direction: 1 | -1 } | null>(null)

function sortBy(key: string): void {
  const primeira = key === 'createdAt' ? -1 : 1
  const segunda = primeira === 1 ? -1 : 1
  const atual = order.value
  if (!atual || atual.key !== key) {
    order.value = { key, direction: primeira }
  } else if (atual.direction === primeira) {
    order.value = { key, direction: segunda }
  } else {
    order.value = null
  }
  page.value = 0
}

function compare(left: Item, right: Item, key: string): number {
  const leftValue = left[key]
  const rightValue = right[key]
  const isBlank = (value: unknown): boolean => value === null || value === undefined || value === ''
  if (isBlank(leftValue)) return isBlank(rightValue) ? 0 : -1
  if (isBlank(rightValue)) return 1
  if (key === 'createdAt') {
    return new Date(String(leftValue)).getTime() - new Date(String(rightValue)).getTime()
  }
  const leftNumber = Number(leftValue)
  const rightNumber = Number(rightValue)
  const bothNumeric = String(leftValue).trim() !== '' && String(rightValue).trim() !== ''
    && Number.isFinite(leftNumber) && Number.isFinite(rightNumber)
  if (bothNumeric) return leftNumber - rightNumber
  return String(leftValue).localeCompare(String(rightValue), 'pt-BR', { sensitivity: 'base', numeric: true })
}

const sorted = computed<Item[]>(() => {
  const criterio = order.value
  if (!criterio) return filtered.value
  return [...filtered.value].sort((left, right) => compare(left, right, criterio.key) * criterio.direction)
})

const visiblePage = computed<Item[]>(() =>
  sorted.value.slice(page.value * porPagina, page.value * porPagina + porPagina)
)

function value(item: Item, key: string): string {
  const rawValue = item[key]
  if (rawValue === null || rawValue === undefined || rawValue === '') return '—'
  if (key === 'createdAt') return formatDate(String(rawValue))
  return String(rawValue)
}

async function loadTab(): Promise<void> {
  if (carregado.value[tab.value]) return
  try {

    data.value[tab.value] = (await currentTab.value.load('', 20000, true)) as Item[]
    carregado.value[tab.value] = true
  } catch (error) {
    toast.error((error as Error).message)
  }
}

function switchTab(key: TabKey): void {
  tab.value = key
  page.value = 0
  search.value = ''
  order.value = null
  selecionados.value = []
  failures.value = []
  void loadTab()
}

watch(search, () => {
  page.value = 0
})

function isSelected(item: Item): boolean {
  return selecionados.value.some((selecionado) => String(selecionado.id) === String(item.id))
}

function toggleSelection(item: Item): void {
  const index = selecionados.value.findIndex((selecionado) => String(selecionado.id) === String(item.id))
  if (index >= 0) selecionados.value.splice(index, 1)
  else selecionados.value.push(item)
}

const isDeactivated = (index: Item): boolean => String(index.status ?? 'ATIVO').toUpperCase() === 'DESATIVADO'
const activeSelected = computed<Item[]>(() => selecionados.value.filter((index) => !isDeactivated(index)))
const deactivatedSelected = computed<Item[]>(() => selecionados.value.filter((index) => isDeactivated(index)))

const allPageSelected = computed(
  () => visiblePage.value.length > 0 && visiblePage.value.every(isSelected)
)
const somePageSelected = computed(() => visiblePage.value.some(isSelected))

function toggleWholePage(): void {
  if (allPageSelected.value) {
    for (const item of visiblePage.value) {
      const index = selecionados.value.findIndex((selecionado) => String(selecionado.id) === String(item.id))
      if (index >= 0) selecionados.value.splice(index, 1)
    }
  } else {
    for (const item of visiblePage.value) {
      if (!isSelected(item)) selecionados.value.push(item)
    }
  }
}

async function applyStatus(): Promise<void> {
  const situacao = confirmando.value
  if (!situacao) return
  confirmando.value = null
  applying.value = true
  try {
    const targets = situacao === 'DESATIVADO' ? activeSelected.value : deactivatedSelected.value
    const ids = targets.map((index) => String(index.id))
    const report = await api.setComponentStatus(tab.value as ComponentKind, ids, situacao)
    const ok = report.filter((response) => response.alterado)
    failures.value = report.filter((response) => !response.alterado).map((response) => ({ id: response.id, motivo: response.motivo }))
    if (ok.length) {
      toast.success(
        situacao === 'DESATIVADO'
          ? `${ok.length} item(ns) desativado(s) — fora de circulação, dá para reativar quando quiser.`
          : `${ok.length} item(ns) reativado(s) — de volta às listas de uso.`
      )
    }
    if (failures.value.length) {
      toast.warning(`${failures.value.length} item(ns) mantido(s) — veja o relatório na tela.`)
    }
    selecionados.value = []
    carregado.value[tab.value] = false
    await loadTab()
  } catch (error) {
    toast.error((error as Error).message)
  } finally {
    applying.value = false
  }
}

const exportModal = ref(false)
const exportTable = ref<TabKey | 'TODAS'>('TODAS')

const TABLE_FILES: Record<TabKey, string> = {
  GS: 'grupos_servico',
  CF: 'codigos_fornecedor',
  CS: 'codigos_servico',
  UM: 'unidades_medida'
}

function openExport(): void {
  exportTable.value = tab.value
  exportModal.value = true
}

function tableRows(key: TabKey): string[][] {
  const config = tabs.find((section) => section.key === key) as Tab
  const header = [...config.columns.map((column) => column.label), 'Status']
  const linhas = data.value[key].map((item) => [
    ...config.columns.map((column) => String(item[column.key] ?? '')),
    statusLabel(item.status)
  ])
  return [header, ...linhas]
}

async function runExport(): Promise<void> {
  const targets: TabKey[] = exportTable.value === 'TODAS' ? tabs.map((section) => section.key) : [exportTable.value]
  exportModal.value = false
  for (const key of targets) {
    if (!carregado.value[key]) {
      try {
        data.value[key] = (await (tabs.find((section) => section.key === key) as Tab).load('', 20000, true)) as Item[]
        carregado.value[key] = true
      } catch {
        continue
      }
    }
    downloadCsv(`${TABLE_FILES[key]}.csv`, tableRows(key))

    if (targets.length > 1) await new Promise((response) => setTimeout(response, 350))
  }
  toast.success(
    targets.length > 1 ? `${targets.length} arquivos exportados (um por tabela).` : `Arquivo ${TABLE_FILES[targets[0]]}.csv exportado.`
  )
}

const importModal = ref(false)
const importTable = ref<TabKey>('GS')
const importItems = ref<ImportItem[]>([])
const importing = ref(false)
const lastImport = ref<{ tabela: TabKey; label: string; ids: string[] } | null>(null)
const confirmingUndo = ref(false)

function importedItemLabel(item: ImportItem): string {
  return String(item.nome || item.descricao || item.sigla || item.codigoSap || item.id || '')
}

function alreadyInCatalog(tipo: TabKey, item: ImportItem, existentes: Item[]): boolean {
  const target = (rawValue: unknown) => normalize(String(rawValue ?? '').trim())
  const id = String(item.id ?? '').trim()
  const sap = String(item.codigoSap ?? id).trim()
  const descricao = String(item.descricao ?? item.nome ?? '').trim()
  return existentes.some((event) => {
    switch (tipo) {
      case 'GS':
        return target(event.nome) === target(item.nome) || String(event.id).toUpperCase() === target(item.nome).replace(/ /g, '_').toUpperCase()
      case 'CF':
        return (sap && String(event.id) === sap) || (!sap && target(event.nome) === target(item.nome))
      case 'CS':
        return target(event.descricao) === target(descricao)
      default:
        return String(event.id).toUpperCase() === String(item.sigla ?? item.id ?? '').trim().toUpperCase()
    }
  })
}

const importPreview = computed(() => {
  const existentes = data.value[importTable.value] ?? []
  const novos: ImportItem[] = []
  const repetidos: ImportItem[] = []
  for (const item of importItems.value) {
    ;(alreadyInCatalog(importTable.value, item, existentes) ? repetidos : novos).push(item)
  }
  return { novos, repetidos }
})

const columnsHint = computed(
  () =>
    ({
      GS: 'Colunas aceitas: id; nome; origem; status — obrigatória: nome.',
      CF: 'Colunas aceitas: id; codigoSap; nome; origem; status — obrigatória: nome (recomendado: codigoSap).',
      CS: 'Colunas aceitas: id; descricao; caracteres; origem; status — obrigatória: descricao (máx. 40 caracteres).',
      UM: 'Colunas aceitas: id; sigla; nome; origem; status — obrigatória: sigla (nome é opcional).'
    })[importTable.value]
)

function openImport(): void {
  importTable.value = tab.value
  importModal.value = true
  clearImport()
}

function clearImport(): void {
  importItems.value = []
  const field = document.getElementById('arquivo-importar') as HTMLInputElement | null
  if (field) field.value = ''
}

const HEADER_FIELDS: Record<string, keyof ImportItem> = {
  id: 'id',
  codigo: 'id',
  key: 'id',
  nome: 'nome',
  name: 'nome',
  descricao: 'descricao',
  description: 'descricao',
  sigla: 'sigla',
  abreviacao: 'sigla',
  codigosap: 'codigoSap',
  sap: 'codigoSap',
  codigofornecedor: 'codigoSap',
  codigofornecedorsap: 'codigoSap'
}

const TABLE_SIGNATURE: { field: string; tabela: TabKey }[] = [
  { field: 'sigla', tabela: 'UM' },
  { field: 'codigosap', tabela: 'CF' },
  { field: 'sap', tabela: 'CF' },
  { field: 'codigofornecedor', tabela: 'CF' },
  { field: 'codigofornecedorsap', tabela: 'CF' },
  { field: 'descricao', tabela: 'CS' },
  { field: 'nome', tabela: 'GS' }
]

function tableLabel(key: TabKey): string {
  return (tabs.find((section) => section.key === key) as Tab).label
}

function detectFileTable(header: string[]): TabKey | null {
  const campos = new Set(header.map(normalizeHeader))
  for (const assinatura of TABLE_SIGNATURE) {
    if (campos.has(assinatura.field)) return assinatura.tabela
  }
  return null
}

function toImportItems(tipo: TabKey, matriz: string[][]): ImportItem[] {
  const header = matriz[0] || []
  const positions: Partial<Record<keyof ImportItem, number>> = {}
  header.forEach((coluna, index) => {
    const field = HEADER_FIELDS[normalizeHeader(coluna)]
    if (field && positions[field] === undefined) positions[field] = index
  })
  const required: Record<TabKey, keyof ImportItem> = { GS: 'nome', CF: 'nome', CS: 'descricao', UM: 'sigla' }
  const requiredField = required[tipo]
  if (positions[requiredField] === undefined) {
    throw new Error(`Cabeçalho sem a coluna “${requiredField}”. Exporte a tabela antes para ver o formato exato.`)
  }
  return matriz.slice(1).map((linha) => {
    const cell = (field: keyof ImportItem): string | null => {
      const index = positions[field]
      return index === undefined ? null : (linha[index] ?? '').trim() || null
    }
    return {
      id: cell('id'),
      nome: cell('nome'),
      descricao: cell('descricao'),
      sigla: cell('sigla'),
      codigoSap: cell('codigoSap')
    }
  })
}

async function readImportFile(event: Event): Promise<void> {
  const target = event.target as HTMLInputElement
  const arquivo = target.files?.[0]
  importItems.value = []
  if (!arquivo) return
  try {
    const matriz = parseCsv(await arquivo.text())
    if (matriz.length < 2) {
      throw new Error('O arquivo precisa do cabeçalho e de pelo menos uma linha de dados.')
    }
    const tabelaDoArquivo = detectFileTable(matriz[0] || [])
    if (tabelaDoArquivo && tabelaDoArquivo !== importTable.value) {
      throw new Error(
        `Este arquivo parece ser de ${tableLabel(tabelaDoArquivo)}, mas a tabela escolhida é ` +
          `${tableLabel(importTable.value)}. Troque a tabela ou envie o arquivo certo — nada foi importado.`
      )
    }
    if (!carregado.value[importTable.value]) {
      const config = tabs.find((section) => section.key === importTable.value) as Tab
      data.value[importTable.value] = (await config.load('', 20000, true)) as Item[]
      carregado.value[importTable.value] = true
    }
    importItems.value = toImportItems(importTable.value, matriz)
    const { novos, repetidos } = importPreview.value
    toast.warning(
      `${importItems.value.length} linha(s) lida(s): ${novos.length} nova(s) entra(m), ` +
        `${repetidos.length} já existe(m) e não ${repetidos.length === 1 ? 'muda' : 'mudam'}.`
    )
  } catch (error) {
    toast.error((error as Error).message)
  }
}

async function runImport(): Promise<void> {
  importing.value = true
  try {
    const response = await api.importCatalog(importTable.value, importItems.value)
    const novos = response.adicionados
    const mantidos = response.ignorados.length
    if (novos && mantidos) {
      toast.success(
        `Importação concluída: ${novos} novo(s) adicionado(s) · ${mantidos} já existiam e foram mantidos como estavam.`
      )
    } else if (novos) {
      toast.success(`${novos} item(ns) adicionado(s) a ${currentTab.value.label.toLowerCase()}.`)
    } else {
      toast.warning(
        `Nada novo: ${mantidos} linha(s) já ${mantidos === 1 ? 'existia e foi mantida' : 'existiam e foram mantidas'} como estavam.`
      )
    }
    const offPattern = response.ignorados.find((failure) => !/mantid/i.test(failure.motivo))
    if (offPattern) {
      toast.warning(`Linha ${offPattern.linha} não entrou: ${offPattern.motivo}`)
    }
    lastImport.value = novos
      ? { tabela: importTable.value, label: tableLabel(importTable.value), ids: response.idsAdicionados }
      : null
    importModal.value = false
    clearImport()
    carregado.value[importTable.value] = false
    if (importTable.value === tab.value) await loadTab()
  } catch (error) {
    toast.error((error as Error).message)
  } finally {
    importing.value = false
  }
}

async function undoImport(): Promise<void> {
  const target = lastImport.value
  confirmingUndo.value = false
  if (!target) return
  try {
    const report = await api.bulkDelete(target.tabela, target.ids)
    const ok = report.filter((response) => response.excluido)
    if (ok.length) {
      toast.success(`Importação desfeita: ${ok.length} item(ns) removido(s) — estado anterior restaurado.`)
    }
    const notRemoved = report.filter((response) => !response.excluido)
    if (notRemoved.length) {
      toast.warning(`${notRemoved.length} item(ns) não puderam ser removidos (${notRemoved[0].motivo}).`)
    }
    lastImport.value = null
    carregado.value[target.tabela] = false
    if (target.tabela === tab.value) await loadTab()
  } catch (error) {
    toast.error((error as Error).message)
  }
}

onMounted(async () => {
  await loadTab()

  for (const section of tabs) {
    if (section.key === tab.value || carregado.value[section.key]) continue
    section.load('', 20000, true)
      .then((itens) => {
        data.value[section.key] = itens as Item[]
        carregado.value[section.key] = true
      })
      .catch(() => {})
  }
})
</script>

<script lang="ts">
export default { name: 'CatalogView' }
</script>

<template>
  <div>
    <h2 style="margin: 0 0 6px; font-size: 1.35rem; color: var(--azul-escuro)">Catálogo</h2>
    <p class="suave" style="margin: 0 0 20px">
      Consulta somente leitura de todos os componentes: grupos, fornecedores, serviços e unidades.
      Itens aguardando aprovação aparecem com o selo de status — só entram em uso depois de aprovados.
    </p>

    <div class="abas">
      <button
        v-for="section in tabs"
        :key="section.key"
        class="aba"
        :class="{ ativa: tab === section.key }"
        @click="switchTab(section.key)"
      >
        {{ section.label }}
        <span class="aba-contagem">{{ totals[section.key] ?? '…' }}</span>
      </button>
    </div>

    <div class="linha-campo" style="margin: 18px 0">
      <div class="campo-busca-wrap">
        <input
          v-model="search"
          type="text"
          class="campo-busca"
          :placeholder="`Buscar em ${currentTab.label.toLowerCase()} por nome, descrição ou código…`"
        />
        <button v-if="search" type="button" class="btn-limpar-busca" title="Limpar busca" @click="search = ''">✕</button>
      </div>
      <button
        type="button"
        class="botao-filtro-excecao"
        :class="{ ativo: onlyExceptions }"
        :title="onlyExceptions
          ? 'Mostrando apenas casos especiais — clique para ver todos'
          : 'Mostrar apenas os itens com (!): exceções aprovadas e duplicidades tratadas'"
        @click="toggleExceptionFilter"
      >
        <span class="badge-excecao">!</span>
        Casos especiais
        <span class="contagem">{{ exceptionCount }}</span>
      </button>
      <button
        type="button"
        class="botao-filtro-excecao btn-alteracoes"
        title="Ver a lista de itens alterados, transferidos e excluídos na base"
        @click="openChanges"
      >
        📋 Alterações
      </button>
      <span v-if="isAdmin && selecionados.length" class="badge badge-alertar">
        {{ selecionados.length }} selecionado(s)
      </span>
      <span style="flex: 1"></span>
      <button class="botao botao-escuro" @click="openExport">⤓ Exportar CSV</button>
      <button v-if="isAdmin" class="botao botao-escuro" @click="openImport">⤓ Importar CSV</button>
      <button
        v-if="isAdmin && activeSelected.length"
        class="botao botao-escuro"
        :disabled="applying"
        @click="confirmando = 'DESATIVADO'"
      >
        {{ applying ? 'Aplicando…' : `✕ Desativar selecionados (${activeSelected.length})` }}
      </button>
      <button
        v-if="isAdmin && deactivatedSelected.length"
        class="botao botao-verde"
        :disabled="applying"
        @click="confirmando = 'ATIVO'"
      >
        ↻ Reativar selecionados ({{ deactivatedSelected.length }})
      </button>
    </div>

    <div v-if="lastImport" class="aviso aviso-azul" style="margin-bottom: 16px">
      <span class="icone">↩</span>
      <div style="display: flex; align-items: center; gap: 10px; flex-wrap: wrap">
        <span>
          <strong>Importação concluída:</strong> {{ lastImport.ids.length }} item(ns)
          adicionado(s) a {{ lastImport.label }}.
        </span>
        <button class="botao botao-escuro" @click="confirmingUndo = true">Desfazer importação</button>
        <button class="cb-excluir" title="Dispensar aviso" @click="lastImport = null">✕</button>
      </div>
    </div>

    <div v-if="failures.length" class="aviso aviso-amarelo" style="margin-bottom: 16px">
      <span class="icone">⚠</span>
      <div>
        <strong>{{ failures.length }} item(ns) não tiveram a situação alterada:</strong>
        <div v-for="failure in failures.slice(0, 5)" :key="failure.id" style="margin-top: 6px">
          <strong>{{ failure.id }}</strong> — {{ failure.motivo }}
        </div>
      </div>
    </div>

    <div class="painel" style="overflow-x: auto">
      <table class="tabela">
        <thead>
          <tr>
            <th v-if="isAdmin" style="width: 38px">
              <input
                type="checkbox"
                :checked="allPageSelected"
                :indeterminate.prop="somePageSelected && !allPageSelected"
                title="Selecionar todos desta página"
                @change="toggleWholePage"
              />
            </th>
            <th
              v-for="column in currentTab.columns"
              :key="column.key"
              class="th-ordenable"
              :title="`Ordenar por ${column.label.toLowerCase()}`"
              @click="sortBy(column.key)"
            >
              {{ column.label }}
              <span v-if="order?.key === column.key" class="seta-ordenacao">{{ order.direction === 1 ? '▲' : '▼' }}</span>
            </th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in visiblePage" :key="String(item.id)" :class="{ 'linha-excecao': item.casoIsolado }">
            <td v-if="isAdmin">
              <input type="checkbox" :checked="isSelected(item)" @change="toggleSelection(item)" />
            </td>
            <td v-for="column in currentTab.columns" :key="column.key">
              <span v-if="column.key === 'id'" class="id-codigo">{{ value(item, column.key) }}</span>
              <template v-else>{{ value(item, column.key) }}</template>
              <button
                v-if="item.casoIsolado && (column.key === 'id' || column.key === 'sigla' || column.key === 'nome' || column.key === 'descricao')"
                type="button"
                class="badge-excecao"
                :title="String(item.motivoCasoIsolado || 'Caso especial registrado') + ' — clique para resolver e remover o (!)'"
                @click.stop="openResolution(item)"
              >!</button>
            </td>
            <td>
              <span class="badge" :class="statusClass(item.status)">{{ statusLabel(item.status) }}</span>
              <button
                v-if="!item.casoIsolado"
                type="button"
                class="btn-marcar-excecao"
                title="Marcar como caso especial (!) — para revisar, resolver ou excluir depois"
                @click.stop="openMarking(item)"
              >!</button>
            </td>
          </tr>
          <tr v-if="!visiblePage.length">
            <td :colspan="currentTab.columns.length + (isAdmin ? 2 : 1)" class="suave centralizado">
              <template v-if="onlyExceptions">Nenhum caso especial ({{ exceptionCount }}) nesta aba.</template>
              <template v-else>Nenhum item encontrado para “{{ search }}”.</template>
            </td>
          </tr>
        </tbody>
      </table>

      <div v-if="totalPages > 1" class="paginacao" style="margin-top: 16px">
        <button class="pulo" :disabled="page === 0" @click="page = 0" title="Início">|‹</button>
        <button class="pulo" :disabled="page === 0" @click="page--" title="Página anterior">‹</button>
        <span class="info">
          <strong>{{ page + 1 }}</strong> / {{ totalPages }} — {{ filtered.length }} item(ns)
        </span>
        <button class="pulo" :disabled="page + 1 >= totalPages" @click="page++" title="Próxima página">›</button>
        <button class="pulo" :disabled="page + 1 >= totalPages" @click="page = totalPages - 1" title="Fim">›|</button>
      </div>
    </div>

    <p class="legenda-excecao suave">
      <span class="badge-excecao">!</span>
      <span>
        <strong>Caso especial</strong> — item com exceção aprovada (similar aceito com justificativa)
        ou duplicidade tratada na varredura. Passe o mouse sobre o ! para ver o motivo.
      </span>
    </p>

    <div v-if="exportModal" class="modal-fundo" @click.self="exportModal = false">
      <div class="modal" style="max-width: 540px">
        <h3>Exportar catálogo (CSV)</h3>
        <p class="suave" style="margin: -8px 0 16px">
          O arquivo abre direto no Excel (separador “;”, com acentuação correta).
        </p>
        <div class="campo-form">
          <label for="tabela-exportar">Qual tabela exportar?</label>
          <select id="tabela-exportar" v-model="exportTable">
            <option value="TODAS">Todas as tabelas (um arquivo por tabela)</option>
            <option v-for="section in tabs" :key="section.key" :value="section.key">{{ section.label }}</option>
          </select>
        </div>
        <div class="linha-botoes">
          <button class="botao botao-escuro" @click="exportModal = false">Cancelar</button>
          <button class="botao botao-roxo" @click="runExport">⤓ Exportar</button>
        </div>
      </div>
    </div>

    <div v-if="importModal" class="modal-fundo" @click.self="importModal = false">
      <div class="modal" style="max-width: 620px">
        <h3>Importar catálogo (CSV)</h3>
        <div class="aviso aviso-verde" style="margin: 0 0 16px">
          <span class="icone">✓</span>
          <div>
            <strong>Só é adicionado o que for novo.</strong> Nada que já existe no banco é
            apagado ou alterado — linhas repetidas voltam no relatório como “ignoradas”.
          </div>
        </div>
        <div class="campo-form">
          <label for="tabela-importar">Qual tabela será alimentada?</label>
          <select id="tabela-importar" v-model="importTable" @change="clearImport">
            <option v-for="section in tabs" :key="section.key" :value="section.key">{{ section.label }}</option>
          </select>
        </div>
        <div class="campo-form">
          <label for="arquivo-importar">Arquivo CSV</label>
          <input id="arquivo-importar" type="file" accept=".csv,text/csv" @change="readImportFile" />
        </div>
        <div class="aviso aviso-azul">
          <span class="icone">ℹ</span>
          <div>
            {{ columnsHint }}<br />
            A <strong>primeira linha deve ser o cabeçalho</strong> — exporte antes para ver o formato exato.
            <strong>“Criado em” e “Criado por” são gravados automaticamente</strong> com o momento e o
            usuário que fez a importação (valores do arquivo são ignorados).
          </div>
        </div>

        <div v-if="importItems.length" class="aviso" :class="importPreview.novos.length ? 'aviso-verde' : 'aviso-amarelo'" style="margin-top: 14px">
          <span class="icone">{{ importPreview.novos.length ? '✓' : 'ℹ' }}</span>
          <div>
            <strong>Só o que é novo entra: {{ importPreview.novos.length }} de {{ importItems.length }} linha(s).</strong>
            {{ importPreview.repetidos.length }} já {{ importPreview.repetidos.length === 1 ? 'existe e será mantida' : 'existem e serão mantidas' }} como está/estão.
            <div v-for="response in importPreview.repetidos.slice(0, 5)" :key="importedItemLabel(response)" class="suave" style="margin-top: 5px">
              já existe: <strong>{{ importedItemLabel(response) }}</strong>
            </div>
            <div v-if="importPreview.repetidos.length > 5" class="suave" style="margin-top: 5px">
              … e mais {{ importPreview.repetidos.length - 5 }}.
            </div>
          </div>
        </div>

        <div class="linha-botoes">
          <button class="botao botao-escuro" @click="importModal = false">Cancelar</button>
          <button
            class="botao botao-roxo"
            :disabled="!importPreview.novos.length || importing"
            @click="runImport"
          >
            {{ importing ? 'Importando…' : importPreview.novos.length ? `Importar ${importPreview.novos.length} nova(s)` : 'Nada novo para importar' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="transferring" class="modal-fundo" @click.self="closeTransfer">
      <div class="modal" style="max-width: 620px">
        <h3>Transferir vínculos para excluir</h3>
        <p class="suave" style="margin: -8px 0 16px; line-height: 1.6">
          <strong>{{ transferring.id }} — {{ currentTab.labelOf(transferring) }}</strong> está vinculado a
          <strong>{{ contagemVinculos }} código(s) completo(s)</strong>. Para liberar a exclusão, escolha
          um item da mesma tabela para <strong>receber esses vínculos</strong> — depois o item de origem
          é excluído definitivamente.
        </p>
        <div class="campo-form">
          <label>Receber os vínculos em</label>
          <SearchCombobox
            :load="currentTab.load"
            :name-of="(item) => String(item.id ?? '') + ' — ' + currentTab.labelOf(item)"
            :id-of="() => ''"
            :allow-create="false"
            placeholder="Buscar item desta tabela para receber os vínculos…"
            @update:model-value="(rawValue) => (linkTarget = (rawValue as Item) || null)"
          />
        </div>
        <div class="linha-botoes">
          <button class="botao botao-escuro" @click="closeTransfer">Cancelar</button>
          <button
            class="botao botao-vermelho"
            :disabled="!linkTarget || transferInProgress"
            @click="confirmTransfer"
          >
            {{ transferInProgress ? 'Transferindo…' : '⇄ Transferir vínculos e excluir' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="changesModal" class="modal-fundo" @click.self="changesModal = false">
      <div class="modal" style="max-width: 860px">
        <h3>Itens alterados e excluídos na base</h3>
        <p class="suave" style="margin: -8px 0 14px; line-height: 1.6">
          Histórico completo das limpezas e decisões: itens <strong>excluídos</strong>,
          vínculos <strong>transferidos</strong>, composições <strong>mescladas</strong> e itens
          <strong>desativados/reativados</strong> — cada linha com o motivo registrado.
        </p>
        <div class="lista-alteracoes">
          <div v-if="loadingChanges" class="suave centralizado" style="padding: 22px">Carregando…</div>
          <div v-else-if="!changes.length" class="suave centralizado" style="padding: 22px">
            Nenhuma alteração registrada ainda.
          </div>
          <div v-for="(event, index) in changes" :key="event.id || index" class="linha-alteracao">
            <span class="badge" :class="changeClass(event)">{{ changeLabel(event) }}</span>
            <strong class="id-alteracao">{{ event.entityId }}</strong>
            <span class="motivo-alteracao">{{ event.reason || '—' }}</span>
            <span class="data-alteracao">{{ formatDate(event.occurredAt) }}</span>
          </div>
        </div>
        <div class="linha-botoes">
          <button class="botao botao-escuro" @click="changesModal = false">Fechar</button>
        </div>
      </div>
    </div>

    <div v-if="marking" class="modal-fundo" @click.self="marking = null">
      <div class="modal" style="max-width: 540px">
        <h3>Marcar como caso especial</h3>
        <p class="suave" style="margin: -8px 0 16px; line-height: 1.6">
          O item <strong>{{ marking.id }} — {{ currentTab.labelOf(marking) }}</strong> receberá o
          <span class="badge-excecao">!</span> e passará a aparecer no filtro
          <strong>Casos especiais</strong>. Registre o motivo — é ele que aparece no hover do badge.
        </p>
        <div class="campo-form">
          <label>Motivo do caso especial</label>
          <textarea
            v-model="markingReason"
            style="min-height: 84px"
            placeholder="Ex.: possível duplicidade de “X” — confirmar antes de excluir."
          ></textarea>
        </div>
        <div class="linha-botoes">
          <button class="botao botao-escuro" @click="marking = null">Cancelar</button>
          <button
            class="botao botao-roxo"
            :disabled="!markingReason.trim() || resolvingNow"
            @click="confirmMarking"
          >
            ✓ Marcar com (!)
          </button>
        </div>
      </div>
    </div>

    <ConfirmModal
      v-if="resolving"
      title="Resolver caso especial?"
      :message="`Motivo registrado: ${String(resolving.motivoCasoIsolado || 'caso especial registrado')}. Ao resolver, o (!) é removido do item e ele sai do filtro de casos especiais. O histórico continua na auditoria.`"
      secondary-label="✕ Excluir definitivamente"
      :secondary-dangerous="true"
      confirm-label="✓ Resolver e remover (!)"
      :dangerous-action="false"
      @secondary="askDeactivation"
      @confirm="confirmResolution"
      @cancel="resolving = null"
    />
    <ConfirmModal
      v-if="deleting"
      title="Excluir definitivamente?"
      :message="`O item ${deleting.id} — ${currentTab.labelOf(deleting)} será APAGADO do banco de dados. Esta ação NÃO pode ser desfeita. Se houver vínculos ativos, a exclusão será bloqueada. Se o item puder voltar um dia, prefira apenas desativar.`"
      confirm-label="✕ Excluir para sempre"
      :dangerous-action="true"
      @confirm="confirmDeletion"
      @cancel="deleting = null"
    />
    <ConfirmModal
      v-if="confirmingUndo"
      title="Desfazer a importação?"
      :message="`${lastImport?.ids.length ?? 0} item(ns) adicionado(s) agora serão removidos, voltando ao estado anterior. O que já existia não é tocado.`"
      confirm-label="Desfazer importação"
      :dangerous-action="true"
      @confirm="undoImport"
      @cancel="confirmingUndo = false"
    />
    <ConfirmModal
      v-if="confirmando"
      :title="confirmando === 'DESATIVADO' ? 'Desativar itens do catálogo?' : 'Reativar itens do catálogo?'"
      :message="confirmando === 'DESATIVADO'
        ? `${activeSelected.length} item(ns) de ${currentTab.label.toLowerCase()} sairão de circulação: somem das listas de uso e da similaridade. Nada é perdido — vínculos, histórico e auditoria continuam, e dá para reativar quando quiser.`
        : `${deactivatedSelected.length} item(ns) de ${currentTab.label.toLowerCase()} voltarão para as listas de uso. A ação fica registrada na auditoria.`"
      :confirm-label="confirmando === 'DESATIVADO' ? '✕ Desativar' : '↻ Reativar'"
      :dangerous-action="confirmando === 'DESATIVADO'"
      @confirm="applyStatus"
      @cancel="confirmando = null"
    />
  </div>
</template>

<style scoped>
.abas {
  display: flex;
  gap: 8px;
  border-bottom: 2px solid var(--borda);
  padding-bottom: 10px;
  flex-wrap: wrap;
}
.aba {
  background: transparent;
  border: 1px solid transparent;
  border-radius: 9px;
  padding: 10px 18px;
  font-size: 0.93rem;
  font-weight: 700;
  color: var(--texto-suave);
  cursor: pointer;
}
.aba:hover {
  color: var(--azul);
}
.aba.ativa {
  background: var(--azul);
  color: var(--texto-sobre-azul);
}
.aba-contagem {
  margin-left: 6px;
  font-size: 0.76rem;
  opacity: 0.85;
  font-variant-numeric: tabular-nums;
}
.th-ordenable {
  cursor: pointer;
  user-select: none;
  white-space: nowrap;
}
.th-ordenable:hover { color: var(--azul); }
.seta-ordenacao { font-size: 0.62rem; margin-left: 4px; }
.botao-filtro-excecao {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 8px 14px;
  border-radius: 999px;
  border: 1.5px solid var(--borda);
  background: transparent;
  color: var(--texto-suave);
  font-size: 0.85rem;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
}
.botao-filtro-excecao:hover {
  border-color: var(--amarelo-alerta);
  color: var(--texto);
}
.botao-filtro-excecao.ativo {
  background: var(--amarelo-fundo);
  border-color: var(--amarelo-alerta);
  color: var(--texto);
  box-shadow: 0 0 0 3px rgba(255, 199, 44, 0.18);
}
.botao-filtro-excecao .contagem {
  font-size: 0.75rem;
  background: var(--amarelo);
  color: var(--texto-forte);
  border-radius: 999px;
  padding: 1px 8px;
  font-weight: 800;
}
button.badge-excecao {
  cursor: pointer;
}
button.badge-excecao:hover {
  transform: scale(1.12);
  box-shadow: 0 2px 8px rgba(255, 199, 44, 0.55);
}
.btn-marcar-excecao {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  padding: 0;
  margin-left: 6px;
  border: 1.5px dashed var(--borda);
  border-radius: 50%;
  background: transparent;
  color: var(--texto-suave);
  font-size: 0.8rem;
  font-weight: 900;
  line-height: 1;
  cursor: pointer;
  vertical-align: middle;
}
.btn-marcar-excecao:hover {
  border-color: var(--amarelo-alerta);
  border-style: solid;
  color: var(--texto-forte);
  background: var(--amarelo);
}
.lista-alteracoes {
  max-height: 55vh;
  overflow-y: auto;
  border: 1px solid var(--borda);
  border-radius: 10px;
}
.linha-alteracao {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border-bottom: 1px solid var(--borda);
  font-size: 0.86rem;
}
.linha-alteracao:last-child { border-bottom: none; }
.id-alteracao { white-space: nowrap; }
.motivo-alteracao {
  flex: 1;
  color: var(--texto-suave);
  min-width: 0;
  overflow-wrap: anywhere;
}
.data-alteracao {
  color: var(--texto-suave);
  font-size: 0.78rem;
  white-space: nowrap;
}
.linha-excecao {
  background: var(--amarelo-fundo);
  box-shadow: inset 5px 0 0 var(--amarelo);
}
.legenda-excecao {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 12px 2px 0;
  font-size: 0.82rem;
  line-height: 1.5;
}
.campo-busca {
  flex: 1;
  min-width: 240px;
  background: var(--campo);
  border: 1px solid var(--borda);
  border-radius: 9px;
  color: var(--texto);
  padding: 9px 12px;
}
.id-codigo {
  font-size: 0.78rem;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: var(--texto-suave);
  font-variant-numeric: tabular-nums;
}
.badge-desativado {
  background: var(--fundo);
  color: var(--texto-suave);
  border: 1px solid var(--borda);
}
.tabela input[type='checkbox'] {
  width: 16px;
  height: 16px;
  accent-color: var(--azul);
  cursor: pointer;
}
</style>
