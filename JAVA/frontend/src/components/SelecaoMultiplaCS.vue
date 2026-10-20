<template>
  <div class="selecao-multipla">
    <div class="campo-busca-wrap">
      <input
        v-model="busca"
        type="text"
        class="campo-busca"
        placeholder="Buscar Código de Serviço por descrição… — pode colar várias linhas"
        @input="aoDigitar"
        @paste="aoColar"
      />
      <button v-if="busca" type="button" class="btn-limpar-busca" title="Limpar busca" @click="busca = ''">✕</button>
    </div>

    <div v-if="carregando" class="suave resultados">Buscando…</div>
    <div v-else-if="resultados.length || mostraCriar" class="resultados">
      <label v-for="r in resultados" :key="r.id" class="opcao" :class="{ marcada: estaEscolhidoPorId(r.id) }">
        <input
          type="checkbox"
          :checked="estaEscolhidoPorId(r.id)"
          @change="alternarExistente(r)"
        />
        <span class="opcao-nome">{{ r.descricao }}</span>
        <span class="opcao-id">{{ r.id }}</span>
      </label>
      <label v-if="mostraCriar" class="opcao" :class="{ marcada: criacaoEscolhida }">
        <input type="checkbox" :checked="criacaoEscolhida" @change="alternarCriacao" />
        <span class="opcao-nome">＋ Criar “{{ busca.trim() }}”</span>
        <span class="suave">não existe na base — entra como código novo</span>
      </label>
    </div>

    <div class="escolhidos">
      <div class="escolhidos-cabecalho">
        <strong>Escolhidos ({{ modelValue.length }})</strong>
        <span class="suave">— pode continuar buscando: os marcados não se perdem</span>
      </div>
      <div v-if="!modelValue.length" class="suave vazio">
        Nenhum código marcado ainda — busque acima ou cole a lista de códigos.
      </div>
      <div v-for="item in modelValue" :key="item.chave" class="opcao marcada">
        <input type="checkbox" checked @change="remover(item)" />
        <div class="opcao-conteudo">
          <div class="opcao-nome">
            <span
              class="badge"
              :class="item.classificacao === 'DISPONIVEL' ? 'badge-permitir' : 'badge-alertar'"
            >
              {{ item.classificacao === 'DISPONIVEL' ? 'Disponível' : 'Criar' }}
            </span>
            {{ item.texto }}
          </div>
          <div class="suave opcao-motivo">
            <template v-if="item.match">
              {{ (item.match.score * 100).toFixed(0) }}% com {{ item.match.id }} —
            </template>
            {{ item.motivo }}
          </div>
        </div>
        <button type="button" class="btn-remover" title="Remover da seleção" @click="remover(item)">✕</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { normalizar } from '../services/texto'
import type { CodigoServico, ItemCsMassa } from '../types'

/**
 * Multi-seleção de Código de Serviço com checkbox (vinculação em massa):
 * a busca filtra os resultados sem perder o que já está marcado, dá para colar
 * várias linhas de uma vez e há a opção de criar um código que não existe.
 */
const props = defineProps<{
  modelValue: ItemCsMassa[]
  carregar: (termo: string) => Promise<CodigoServico[]>
  /** analisa textos informados (colagem/criação) e devolve o que pode entrar */
  resolver: (textos: string[]) => Promise<ItemCsMassa[]>
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', valor: ItemCsMassa[]): void
}>()

const busca = ref('')
const resultados = ref<CodigoServico[]>([])
const carregando = ref(false)

const textoBusca = computed(() => busca.value.trim())
const mostraCriar = computed(
  () => textoBusca.value.length > 0 && !resultados.value.some((r) => mesmoTexto(r.descricao, textoBusca.value))
)
const criacaoEscolhida = computed(() => props.modelValue.some((i) => i.chave === chaveCriacao(textoBusca.value)))

function mesmoTexto(a: string | null | undefined, b: string): boolean {
  return normalizar((a ?? '').trim()) === normalizar(b.trim())
}

function chaveCriacao(texto: string): string {
  return `novo:${normalizar(texto.trim())}`
}

function estaEscolhidoPorId(id: string): boolean {
  return props.modelValue.some((i) => i.csId === id)
}

function atualizar(novos: ItemCsMassa[]): void {
  emit('update:modelValue', novos)
}

function alternarExistente(item: CodigoServico): void {
  const jaEscolhido = props.modelValue.find((i) => i.csId === item.id)
  if (jaEscolhido) {
    atualizar(props.modelValue.filter((i) => i.csId !== item.id))
    return
  }
  atualizar([
    ...props.modelValue,
    {
      chave: item.id,
      texto: item.descricao,
      csId: item.id,
      classificacao: 'DISPONIVEL',
      situacao: 'SELECIONADO',
      motivo: 'Código existente escolhido para reutilizar.',
      match: null
    }
  ])
}

async function alternarCriacao(): Promise<void> {
  const texto = textoBusca.value
  if (!texto || carregando.value) return
  if (criacaoEscolhida.value) {
    atualizar(props.modelValue.filter((i) => i.chave !== chaveCriacao(texto)))
    return
  }
  carregando.value = true
  try {
    // a análise pode descobrir que o "novo" já existe — aí entra como disponível
    const analisados = await props.resolver([texto])
    if (!analisados.length) return
    atualizar([...props.modelValue, ...novos(analisados)])
    busca.value = ''
    resultados.value = []
  } finally {
    carregando.value = false
  }
}

async function aoColar(evento: ClipboardEvent): Promise<void> {
  const texto = evento.clipboardData?.getData('text') ?? ''
  const linhas = texto
    .split(/\r?\n/)
    .map((l) => l.trim())
    .filter((l) => l !== '')
  if (linhas.length < 2) return // colagem simples continua valendo no campo
  evento.preventDefault()
  await adicionarLinhas(linhas)
}

async function adicionarLinhas(linhas: string[]): Promise<void> {
  const pendentes = linhas.filter((t) => !props.modelValue.some((i) => mesmoTexto(i.texto, t)))
  if (!pendentes.length) return
  carregando.value = true
  try {
    const analisados = await props.resolver(pendentes)
    atualizar([...props.modelValue, ...novos(analisados)])
    busca.value = ''
    resultados.value = []
  } finally {
    carregando.value = false
  }
}

/** só entra o que ainda não está escolhido (por código ou por texto) */
function novos(itens: ItemCsMassa[]): ItemCsMassa[] {
  return itens.filter(
    (novo) =>
      !props.modelValue.some((i) => i.chave === novo.chave || (novo.csId !== null && i.csId === novo.csId)) &&
      !itens.slice(0, itens.indexOf(novo)).some((anterior) => anterior.chave === novo.chave)
  )
}

function remover(item: ItemCsMassa): void {
  atualizar(props.modelValue.filter((i) => i.chave !== item.chave))
}

let debounce: ReturnType<typeof setTimeout> | null = null
function aoDigitar(): void {
  if (debounce) clearTimeout(debounce)
  debounce = setTimeout(async () => {
    const termo = textoBusca.value
    if (!termo || termo.includes('\n')) {
      resultados.value = []
      return
    }
    carregando.value = true
    try {
      resultados.value = (await props.carregar(termo)).slice(0, 8)
    } catch {
      resultados.value = []
    } finally {
      carregando.value = false
    }
  }, 250)
}
</script>

<style scoped>
.selecao-multipla {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.resultados {
  border: 1px solid var(--borda);
  border-radius: 10px;
  overflow: hidden;
}
.opcao {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
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
.escolhidos {
  border: 1px dashed var(--borda);
  border-radius: 10px;
  padding: 10px;
}
.escolhidos-cabecalho {
  display: flex;
  align-items: baseline;
  gap: 10px;
  font-size: 0.88rem;
  margin: 2px 4px 8px;
  flex-wrap: wrap;
}
.escolhidos .opcao {
  border: 1px solid var(--borda);
  border-radius: 9px;
  margin-bottom: 6px;
}
.escolhidos .vazio {
  padding: 6px 4px 4px;
  font-size: 0.85rem;
}
.btn-remover {
  border: none;
  background: transparent;
  color: var(--texto-suave);
  cursor: pointer;
  font-size: 0.85rem;
}
.btn-remover:hover {
  color: var(--vermelho);
}
</style>
