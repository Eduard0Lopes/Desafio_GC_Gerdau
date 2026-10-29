<script setup lang="ts">
import { computed, ref, useId } from 'vue'
import { debounce } from '../services/tempo'
import { normalize } from '../services/texto'
import type { ServiceCode, BulkCodeItem } from '../types'

const props = defineProps<{
  modelValue: BulkCodeItem[]
  load: (term: string) => Promise<ServiceCode[]>
  analyze: (textos: string[]) => Promise<BulkCodeItem[]>
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: BulkCodeItem[]): void
}>()

const fieldId = `busca-cs-${useId()}`
const search = ref('')
const results = ref<ServiceCode[]>([])
const loading = ref(false)

const searchText = computed(() => search.value.trim())
const showCreate = computed(
  () => searchText.value.length > 0 && !results.value.some((result) => sameText(result.descricao, searchText.value))
)
const creationChosen = computed(() => props.modelValue.some((entry) => entry.key === creationKey(searchText.value)))

function sameText(first: string | null | undefined, second: string): boolean {
  return normalize((first ?? '').trim()) === normalize(second.trim())
}

function creationKey(text: string): string {
  return `novo:${normalize(text.trim())}`
}

function isChosenById(id: string): boolean {
  return props.modelValue.some((entry) => entry.csId === id)
}

function update(novos: BulkCodeItem[]): void {
  emit('update:modelValue', novos)
}

function availableItem(item: ServiceCode): BulkCodeItem {
  return {
    key: item.id,
    texto: item.descricao,
    csId: item.id,
    classificacao: 'DISPONIVEL',
    situacao: 'SELECIONADO',
    motivo: 'Código existente escolhido para reutilizar.',
    match: null
  }
}

function toggleExisting(item: ServiceCode): void {
  const jaEscolhido = props.modelValue.find((entry) => entry.csId === item.id)
  if (jaEscolhido) {
    update(props.modelValue.filter((entry) => entry.csId !== item.id))
    return
  }
  update([...props.modelValue, availableItem(item)])
}

function selectAll(): void {
  const novos = results.value
    .filter((result) => !isChosenById(result.id))
    .map(availableItem)
  if (!novos.length) return
  update([...props.modelValue, ...novos])
}

function clearSelection(): void {
  update([])
}

function clearSearch(): void {
  search.value = ''
  results.value = []
}

async function confirmSearch(): Promise<void> {
  const term = searchText.value
  if (!term || loading.value) return
  const exact = results.value.find((result) => sameText(result.descricao, term))
  if (exact) {
    if (!isChosenById(exact.id)) toggleExisting(exact)
    clearSearch()
    return
  }
  if (showCreate.value && !creationChosen.value) await toggleCreation()
}

async function toggleCreation(): Promise<void> {
  const typedText = searchText.value
  if (!typedText || loading.value) return
  if (creationChosen.value) {
    update(props.modelValue.filter((entry) => entry.key !== creationKey(typedText)))
    return
  }
  loading.value = true
  try {
    const analyzed = await props.analyze([typedText])
    if (!analyzed.length) return
    update([...props.modelValue, ...analyzed])
    search.value = ''
    results.value = []
  } finally {
    loading.value = false
  }
}

async function onPaste(event: ClipboardEvent): Promise<void> {
  const typedText = event.clipboardData?.getData('text') ?? ''
  const lines = typedText
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => line !== '')
  if (lines.length < 2) return
  event.preventDefault()
  await addLines(lines)
}

async function addLines(linhas: string[]): Promise<void> {
  if (!linhas.length) return
  loading.value = true
  try {
    const analyzed = await props.analyze(linhas)
    update([...props.modelValue, ...analyzed])
    search.value = ''
    results.value = []
  } finally {
    loading.value = false
  }
}

function removeItem(index: number): void {
  update(props.modelValue.filter((_, entry) => entry !== index))
}

const aoDigitar = debounce(async () => {
  const term = searchText.value
  if (!term || term.includes('\n')) {
    results.value = []
    return
  }
  loading.value = true
  try {
    results.value = (await props.load(term)).slice(0, 8)
  } catch {
    results.value = []
  } finally {
    loading.value = false
  }
}, 250)
</script>

<template>
  <div class="selecao-multipla">
    <div class="campo-busca-wrap">
      <input
        :id="fieldId"
        v-model="search"
        type="text"
        class="campo-busca"
        placeholder="Digite para buscar… ou cole a lista (um por linha)"
        aria-label="Buscar Códigos de Serviço"
        autocomplete="off"
        @input="aoDigitar"
        @paste="onPaste"
        @keydown.enter.prevent="confirmSearch"
      />
      <button
        v-if="search"
        type="button"
        class="btn-limpar-busca"
        title="Limpar busca"
        aria-label="Limpar busca"
        @click="clearSearch"
      >
        ✕
      </button>
    </div>

    <div v-if="loading" class="estado">
      <span class="girando girando-azul" aria-hidden="true"></span>
      <span>Analisando códigos…</span>
    </div>

    <div v-else-if="results.length || showCreate" class="resultados">
      <div class="resultados-cabecalho">
        <span class="suave">{{ results.length }} encontrado(s)</span>
        <button
          type="button"
          class="link-acao"
          :disabled="!results.length"
          @click="selectAll"
        >
          Marcar todos
        </button>
      </div>

      <label v-for="result in results" :key="result.id" class="opcao" :class="{ marcada: isChosenById(result.id) }">
        <input
          type="checkbox"
          :checked="isChosenById(result.id)"
          @change="toggleExisting(result)"
        />
        <span class="opcao-nome">{{ result.descricao }}</span>
        <span class="opcao-id">{{ result.id }}</span>
      </label>

      <label v-if="showCreate" class="opcao opcao-criar" :class="{ marcada: creationChosen }">
        <input type="checkbox" :checked="creationChosen" @change="toggleCreation" />
        <span class="opcao-nome">＋ Criar “{{ search.trim() }}”</span>
        <span class="suave opcao-apoio">não existe na base — entra como código novo</span>
      </label>
    </div>

    <div class="escolhidos">
      <div class="escolhidos-cabecalho">
        <strong>Escolhidos ({{ modelValue.length }})</strong>
        <button
          v-if="modelValue.length"
          type="button"
          class="link-acao"
          @click="clearSelection"
        >
          Remover todos
        </button>
      </div>

      <div v-if="!modelValue.length" class="vazio">
        Nada marcado ainda — busque acima ou cole a lista de códigos (um por linha).
      </div>

      <ul v-else class="lista-escolhidos">
        <li v-for="(item, index) in modelValue" :key="item.key + ':' + index" class="opcao marcada">
          <input type="checkbox" checked @change="removeItem(index)" />
          <div class="opcao-conteudo">
            <div class="opcao-nome">
              <span
                class="badge"
                :class="item.classificacao === 'DISPONIVEL' ? 'badge-permitir' : 'badge-alertar'"
              >
                {{ item.classificacao === 'DISPONIVEL' ? 'Disponível' : 'Criar' }}
              </span>
              <span v-if="item.situacao === 'ALERTA'" class="tag-governanca">Governança</span>
              {{ item.texto }}
            </div>
            <div class="suave opcao-motivo">
              <template v-if="item.match">
                {{ (item.match.score * 100).toFixed(0) }}% com {{ item.match.id }} —
              </template>
              {{ item.motivo }}
            </div>
          </div>
          <button
            type="button"
            class="btn-remover"
            title="Remover da seleção"
            :aria-label="`Remover ${item.texto} da seleção`"
            @click="removeItem(index)"
          >
            ✕
          </button>
        </li>
      </ul>
    </div>
  </div>
</template>

<style scoped>
.selecao-multipla {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.campo-busca-wrap {
  position: relative;
  display: flex;
  align-items: center;
}
.campo-busca {
  width: 100%;
  background: var(--campo);
  border: 1.5px solid var(--borda);
  border-radius: 10px;
  color: var(--texto);
  padding: 11px 36px 11px 12px;
  font-size: 0.94rem;
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.campo-busca:focus {
  border-color: var(--azul);
  box-shadow: 0 0 0 3px var(--azul-claro);
}
.estado {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border: 1px solid var(--borda);
  border-radius: 10px;
  background: var(--fundo);
  font-size: 0.86rem;
  color: var(--texto-suave);
}
.resultados {
  border: 1px solid var(--borda);
  border-radius: 10px;
  overflow: hidden;
  background: var(--cartao);
}
.resultados-cabecalho {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 13px;
  background: var(--fundo);
  border-bottom: 1px solid var(--borda);
  font-size: 0.8rem;
}
.opcao {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 13px;
  border-bottom: 1px solid var(--borda);
  cursor: pointer;
  font-size: 0.9rem;
}
.resultados .opcao:last-child {
  border-bottom: none;
}
.opcao:hover {
  background: var(--fundo);
}
.opcao.marcada {
  background: var(--azul-claro);
}
.opcao input[type='checkbox'] {
  width: 16px;
  height: 16px;
  accent-color: var(--azul);
  cursor: pointer;
  flex-shrink: 0;
}
.opcao-conteudo {
  flex: 1;
  min-width: 0;
}
.opcao-nome {
  font-weight: 600;
  line-height: 1.5;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.opcao-apoio {
  font-size: 0.78rem;
  margin-left: auto;
  text-align: right;
}
.opcao-criar {
  background: var(--verde-fundo);
  border-top: 1px dashed var(--borda);
}
.opcao-criar:hover {
  background: var(--verde-fundo);
}
.opcao-id {
  margin-left: auto;
  font-size: 0.72rem;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: var(--texto-suave);
  font-variant-numeric: tabular-nums;
}
.opcao-motivo {
  font-size: 0.8rem;
  line-height: 1.5;
}
.tag-governanca {
  display: inline-flex;
  align-items: center;
  padding: 2px 9px;
  border-radius: 999px;
  font-size: 0.7rem;
  font-weight: 800;
  letter-spacing: 0.03em;
  text-transform: uppercase;
  background: var(--vermelho-fundo);
  color: var(--vermelho);
  border: 1px solid var(--vermelho);
}
.escolhidos {
  padding: 0;
}
.escolhidos-cabecalho {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 0.88rem;
  margin: 0 2px 10px;
}
.lista-escolhidos {
  list-style: none;
  margin: 0;
  padding: 0;
}
.escolhidos .opcao {
  border: 1px solid var(--borda);
  border-radius: 9px;
  margin-bottom: 6px;
}
.escolhidos .opcao:last-child {
  margin-bottom: 0;
}
.vazio {
  padding: 4px 2px;
  font-size: 0.84rem;
  color: var(--texto-suave);
  line-height: 1.5;
}
.btn-remover {
  border: none;
  background: transparent;
  color: var(--texto-suave);
  cursor: pointer;
  font-size: 0.85rem;
  width: 26px;
  height: 26px;
  border-radius: 6px;
  flex-shrink: 0;
}
.btn-remover:hover {
  background: var(--vermelho-fundo);
  color: var(--vermelho);
}
</style>
