<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { debounce } from '../services/tempo'
import BadgeException from './BadgeExcecao.vue'
import type { Item } from '../types'

const props = withDefaults(defineProps<{
  placeholder?: string
  load: (term: string) => Promise<unknown[]>
  nameOf?: (entry: Item) => string
  idOf?: (entry: Item) => string
  sapOf?: (entry: Item) => string
  allowCreate?: boolean
  allowRemove?: boolean
  modelValue?: Item | string | null
}>(), {
  placeholder: 'Digite para buscar…',
  nameOf: (entry: Item) => String(entry?.nome ?? entry?.descricao ?? entry),
  idOf: (entry: Item) => (entry?.id ? String(entry.id) : ''),
  sapOf: () => '',
  allowCreate: true,
  allowRemove: false,
  modelValue: null
})

const emit = defineEmits<{
  (event: 'update:modelValue', v: Item | null): void
  (event: 'createNew', typedText: string): void
  (event: 'remove', item: Item): void
}>()

const typedText = ref('')
const options = ref<Item[]>([])
const isOpen = ref(false)
const focado = ref(false)
const loading = ref(false)
const searched = ref(false)
const searchError = ref(false)
const highlight = ref(-1)
const root = ref<HTMLElement | null>(null)
const field = ref<HTMLInputElement | null>(null)

let requisicaoId = 0

const debouncedSearch = debounce((term: string) => void search(term), 250)

const showCreateOption = computed(() => props.allowCreate && typedText.value.trim().length > 0 && !searchError.value)

async function search(term: string): Promise<void> {
  const id = ++requisicaoId
  loading.value = true
  searchError.value = false
  try {
    const itens = await props.load(term)
    if (id !== requisicaoId) return
    options.value = Array.isArray(itens) ? (itens as Item[]) : []
    searched.value = true
    highlight.value = -1
    isOpen.value = true
  } catch {
    if (id === requisicaoId) {
      options.value = []
      searched.value = true
      searchError.value = true
      isOpen.value = true
    }
  } finally {
    if (id === requisicaoId) loading.value = false
  }
}

function aoDigitar(): void {
  emit('update:modelValue', null)
  debouncedSearch(typedText.value.trim())
}

function clearSearch(): void {
  typedText.value = ''
  emit('update:modelValue', null)
  void search('')
}

function onFocus(): void {
  focado.value = true
  if (!typedText.value) search('')
  else isOpen.value = true
}

function select(item: Item): void {
  typedText.value = props.nameOf(item)
  isOpen.value = false
  emit('update:modelValue', item)
}

function createNew(): void {
  const term = typedText.value.trim()
  isOpen.value = false
  emit('createNew', term)
}

function onKeyDown(event: KeyboardEvent): void {
  const total = options.value.length + (showCreateOption.value ? 1 : 0)
  if (event.key === 'ArrowDown') {
    event.preventDefault()
    isOpen.value = true
    highlight.value = (highlight.value + 1) % Math.max(total, 1)
  } else if (event.key === 'ArrowUp') {
    event.preventDefault()
    highlight.value = (highlight.value - 1 + total) % Math.max(total, 1)
  } else if (event.key === 'Enter') {
    if (highlight.value >= 0 && highlight.value < options.value.length) {
      event.preventDefault()
      select(options.value[highlight.value])
    } else if (showCreateOption.value && highlight.value === options.value.length) {
      event.preventDefault()
      createNew()
    }
  } else if (event.key === 'Escape') {
    isOpen.value = false
    highlight.value = -1
  }
}

function onClickOutside(event: MouseEvent): void {
  if (root.value && event.target instanceof Node && !root.value.contains(event.target)) isOpen.value = false
}

watch(
  () => props.modelValue,
  (novo) => {
    if (!novo) {
      typedText.value = ''
      options.value = []
      highlight.value = -1
    } else if (typeof novo === 'object') {
      typedText.value = props.nameOf(novo)
    }
  }
)

onMounted(() => document.addEventListener('mousedown', onClickOutside))
onBeforeUnmount(() => {
  document.removeEventListener('mousedown', onClickOutside)
  debouncedSearch.cancel()
})

defineExpose({
  field,
  setText(text: string): void {
    typedText.value = text
  },

  reload(): void {
    void search(typedText.value.trim())
  }
})
</script>

<template>
  <div class="combobox" ref="raiz">
    <div class="cb-campo" :class="{ focado }">
      <input
        ref="campo"
        type="text"
        autocomplete="off"
        :placeholder="placeholder"
        v-model="typedText"
        @input="aoDigitar"
        @focus="onFocus"
        @keydown="onKeyDown"
      />
      <span v-if="loading" class="suave" style="font-size: 0.78rem">…</span>
      <button v-if="typedText" type="button" class="cb-limpar" title="Limpar busca" @click="clearSearch">✕</button>
    </div>

    <ul v-if="isOpen && (options.length || showCreateOption || (searched && !loading))" class="cb-lista" role="listbox">
      <li
        v-for="(item, entry) in options"
        :key="entry"
        role="option"
        :class="{ ativo: entry === highlight }"
        @mousedown.prevent="select(item)"
        @mouseenter="highlight = entry"
      >
        <span v-if="idOf(item)" class="cb-item-id">{{ idOf(item) }} -</span>
        <span class="cb-item-nome">{{ nameOf(item) }}</span>
        <BadgeException v-if="item.casoIsolado" :motivo="item.motivoCasoIsolado" />
        <span v-if="sapOf(item)" class="cb-sap">{{ sapOf(item) }}</span>
        <button
          v-if="allowRemove"
          class="cb-excluir"
          type="button"
          title="Desativar este item (ADMIN) — sai das listas e pode ser reativado no Catálogo"
          @mousedown.stop.prevent
          @click.stop="emit('remove', item)"
        >
          ✕
        </button>
      </li>

      <li
        v-if="showCreateOption"
        role="option"
        class="cb-criar"
        :class="{ ativo: highlight === options.length }"
        @mousedown.prevent="createNew"
        @mouseenter="highlight = options.length"
      >
        <span class="cb-item-nome">＋ Cadastrar Novo Item: “{{ typedText.trim() }}”</span>
      </li>

      <li v-if="searchError && !loading" class="cb-vazio">
        ⚠ Falha ao buscar — verifique a conexão e tente de novo.
      </li>
      <li v-else-if="!options.length && !loading && searched" class="cb-vazio">
        Nenhum resultado para “{{ typedText }}”.
      </li>
    </ul>
  </div>
</template>
