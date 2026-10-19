<template>
  <div class="combobox" ref="raiz">
    <div class="cb-campo" :class="{ focado }">
      <input
        ref="campo"
        type="text"
        autocomplete="off"
        :placeholder="placeholder"
        v-model="texto"
        @input="aoDigitar"
        @focus="aoFocar"
        @keydown="aoTeclar"
      />
      <span v-if="carregando" class="suave" style="font-size: 0.78rem">…</span>
      <button v-if="texto" type="button" class="cb-limpar" title="Limpar busca" @click="limparBusca">✕</button>
    </div>

    <ul v-if="aberto && (opcoes.length || mostrarCriar || (jaBuscou && !carregando))" class="cb-lista" role="listbox">
      <li
        v-for="(item, i) in opcoes"
        :key="i"
        role="option"
        :class="{ ativo: i === destaque }"
        @mousedown.prevent="selecionar(item)"
        @mouseenter="destaque = i"
      >
        <span v-if="idDe(item)" class="cb-item-id">{{ idDe(item) }} -</span>
        <span class="cb-item-nome">{{ nomeDe(item) }}</span>
        <span
          v-if="item.casoIsolado"
          class="badge-excecao"
          :title="'Caso especial registrado.' + (String(item.motivoCasoIsolado || '') ? ' Motivo: ' + item.motivoCasoIsolado : '')"
        >!</span>
        <span v-if="sapDe(item)" class="cb-sap">{{ sapDe(item) }}</span>
        <button
          v-if="permiteExcluir"
          class="cb-excluir"
          type="button"
          title="Desativar este item (ADMIN) — sai das listas e pode ser reativado no Catálogo"
          @mousedown.stop.prevent
          @click.stop="emit('excluir', item)"
        >
          ✕
        </button>
      </li>

      <li
        v-if="mostrarCriar"
        role="option"
        class="cb-criar"
        :class="{ ativo: destaque === opcoes.length }"
        @mousedown.prevent="criarNovo"
        @mouseenter="destaque = opcoes.length"
      >
        <span class="cb-item-nome">＋ Cadastrar Novo Item: “{{ texto.trim() }}”</span>
      </li>

      <li v-if="erroBusca && !carregando" class="cb-vazio">
        ⚠ Falha ao buscar — verifique a conexão e tente de novo.
      </li>
      <li v-else-if="!opcoes.length && !carregando && jaBuscou" class="cb-vazio">
        Nenhum resultado para “{{ texto }}”.
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'

type Item = Record<string, unknown>

const props = withDefaults(defineProps<{
  placeholder?: string
  carregar: (termo: string) => Promise<unknown[]>
  nomeDe?: (i: Item) => string
  idDe?: (i: Item) => string
  sapDe?: (i: Item) => string
  permiteCriar?: boolean
  permiteExcluir?: boolean
  modelValue?: Item | string | null
}>(), {
  placeholder: 'Digite para buscar…',
  nomeDe: (i: Item) => String(i?.nome ?? i?.descricao ?? i),
  idDe: (i: Item) => (i?.id ? String(i.id) : ''),
  sapDe: () => '',
  permiteCriar: true,
  permiteExcluir: false,
  modelValue: null
})

const emit = defineEmits<{
  (e: 'update:modelValue', v: Item | null): void
  (e: 'criarNovo', texto: string): void
  (e: 'excluir', item: Item): void
}>()

const texto = ref('')
const opcoes = ref<Item[]>([])
const aberto = ref(false)
const focado = ref(false)
const carregando = ref(false)
const jaBuscou = ref(false)
const erroBusca = ref(false)
const destaque = ref(-1)
const raiz = ref<HTMLElement | null>(null)
const campo = ref<HTMLInputElement | null>(null)

let debounce: ReturnType<typeof setTimeout> | null = null
let requisicaoId = 0

const mostrarCriar = computed(() => props.permiteCriar && texto.value.trim().length > 0 && !erroBusca.value)

async function buscar(termo: string): Promise<void> {
  const id = ++requisicaoId
  carregando.value = true
  erroBusca.value = false
  try {
    const itens = await props.carregar(termo)
    if (id !== requisicaoId) return
    opcoes.value = Array.isArray(itens) ? (itens as Item[]) : []
    jaBuscou.value = true
    destaque.value = -1
    aberto.value = true
  } catch {
    if (id === requisicaoId) {
      opcoes.value = []
      jaBuscou.value = true
      erroBusca.value = true
      aberto.value = true
    }
  } finally {
    if (id === requisicaoId) carregando.value = false
  }
}

function aoDigitar(): void {
  emit('update:modelValue', null)
  if (debounce) clearTimeout(debounce)
  debounce = setTimeout(() => buscar(texto.value.trim()), 250)
}

function limparBusca(): void {
  texto.value = ''
  emit('update:modelValue', null)
  void buscar('')
}

function aoFocar(): void {
  focado.value = true
  if (!texto.value) buscar('')
  else aberto.value = true
}

function selecionar(item: Item): void {
  texto.value = props.nomeDe(item)
  aberto.value = false
  emit('update:modelValue', item)
}

function criarNovo(): void {
  const termo = texto.value.trim()
  aberto.value = false
  emit('criarNovo', termo)
}

function aoTeclar(e: KeyboardEvent): void {
  const total = opcoes.value.length + (mostrarCriar.value ? 1 : 0)
  if (e.key === 'ArrowDown') {
    e.preventDefault()
    aberto.value = true
    destaque.value = (destaque.value + 1) % Math.max(total, 1)
  } else if (e.key === 'ArrowUp') {
    e.preventDefault()
    destaque.value = (destaque.value - 1 + total) % Math.max(total, 1)
  } else if (e.key === 'Enter') {
    if (destaque.value >= 0 && destaque.value < opcoes.value.length) {
      e.preventDefault()
      selecionar(opcoes.value[destaque.value])
    } else if (mostrarCriar.value && destaque.value === opcoes.value.length) {
      e.preventDefault()
      criarNovo()
    }
  } else if (e.key === 'Escape') {
    aberto.value = false
    destaque.value = -1
  }
}

function aoCliqueFora(e: MouseEvent): void {
  if (raiz.value && e.target instanceof Node && !raiz.value.contains(e.target)) aberto.value = false
}


watch(
  () => props.modelValue,
  (novo) => {
    if (!novo) {
      texto.value = ''
      opcoes.value = []
      destaque.value = -1
    } else if (typeof novo === 'object') {
      texto.value = props.nomeDe(novo)
    }
  }
)

onMounted(() => document.addEventListener('mousedown', aoCliqueFora))
onBeforeUnmount(() => {
  document.removeEventListener('mousedown', aoCliqueFora)
  if (debounce) clearTimeout(debounce)
})

defineExpose({
  campo,
  definirTexto(t: string): void {
    texto.value = t
  },
  
  recarregar(): void {
    void buscar(texto.value.trim())
  }
})
</script>
