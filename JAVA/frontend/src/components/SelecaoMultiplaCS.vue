<template>
  <div class="selecao-multipla">
    <div class="campo-busca-wrap">
      <input
        :id="idCampo"
        v-model="busca"
        type="text"
        class="campo-busca"
        placeholder="Digite para buscar… ou cole a lista (um por linha)"
        aria-label="Buscar Códigos de Serviço"
        autocomplete="off"
        @input="aoDigitar"
        @paste="aoColar"
        @keydown.enter.prevent="confirmarBusca"
      />
      <button
        v-if="busca"
        type="button"
        class="btn-limpar-busca"
        title="Limpar busca"
        aria-label="Limpar busca"
        @click="limparBusca"
      >
        ✕
      </button>
    </div>

    <div v-if="carregando" class="estado">
      <span class="girando girando-azul" aria-hidden="true"></span>
      <span>Analisando códigos…</span>
    </div>

    <div v-else-if="resultados.length || mostraCriar" class="resultados">
      <div class="resultados-cabecalho">
        <span class="suave">{{ resultados.length }} encontrado(s)</span>
        <button
          type="button"
          class="link-acao"
          :disabled="!resultados.length"
          @click="marcarTodos"
        >
          Marcar todos
        </button>
      </div>

      <label v-for="r in resultados" :key="r.id" class="opcao" :class="{ marcada: estaEscolhidoPorId(r.id) }">
        <input
          type="checkbox"
          :checked="estaEscolhidoPorId(r.id)"
          @change="alternarExistente(r)"
        />
        <span class="opcao-nome">{{ r.descricao }}</span>
        <span class="opcao-id">{{ r.id }}</span>
      </label>

      <label v-if="mostraCriar" class="opcao opcao-criar" :class="{ marcada: criacaoEscolhida }">
        <input type="checkbox" :checked="criacaoEscolhida" @change="alternarCriacao" />
        <span class="opcao-nome">＋ Criar “{{ busca.trim() }}”</span>
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
          @click="limparSelecao"
        >
          Remover todos
        </button>
      </div>

      <div v-if="!modelValue.length" class="vazio">
        Nada marcado ainda — busque acima ou cole a lista de códigos (um por linha).
      </div>

      <ul v-else class="lista-escolhidos">
        <li v-for="(item, indice) in modelValue" :key="item.chave + ':' + indice" class="opcao marcada">
          <input type="checkbox" checked @change="remover(indice)" />
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
            @click="remover(indice)"
          >
            ✕
          </button>
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, useId } from 'vue'
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

const idCampo = `busca-cs-${useId()}`
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

function itemDisponivel(item: CodigoServico): ItemCsMassa {
  return {
    chave: item.id,
    texto: item.descricao,
    csId: item.id,
    classificacao: 'DISPONIVEL',
    situacao: 'SELECIONADO',
    motivo: 'Código existente escolhido para reutilizar.',
    match: null
  }
}

function alternarExistente(item: CodigoServico): void {
  const jaEscolhido = props.modelValue.find((i) => i.csId === item.id)
  if (jaEscolhido) {
    atualizar(props.modelValue.filter((i) => i.csId !== item.id))
    return
  }
  atualizar([...props.modelValue, itemDisponivel(item)])
}

/** marca de uma vez todos os resultados da busca atual */
function marcarTodos(): void {
  const novos = resultados.value
    .filter((r) => !estaEscolhidoPorId(r.id))
    .map(itemDisponivel)
  if (!novos.length) return
  atualizar([...props.modelValue, ...novos])
}

function limparSelecao(): void {
  atualizar([])
}

function limparBusca(): void {
  busca.value = ''
  resultados.value = []
}

/** Enter: usa o resultado exato se existir; senão entra como código novo */
async function confirmarBusca(): Promise<void> {
  const termo = textoBusca.value
  if (!termo || carregando.value) return
  const exato = resultados.value.find((r) => mesmoTexto(r.descricao, termo))
  if (exato) {
    if (!estaEscolhidoPorId(exato.id)) alternarExistente(exato)
    limparBusca()
    return
  }
  if (mostraCriar.value && !criacaoEscolhida.value) await alternarCriacao()
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
    atualizar([...props.modelValue, ...analisados])
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
  if (!linhas.length) return
  carregando.value = true
  try {
    // repetir um código não é barrado: cada linha vira uma marca própria
    const analisados = await props.resolver(linhas)
    atualizar([...props.modelValue, ...analisados])
    busca.value = ''
    resultados.value = []
  } finally {
    carregando.value = false
  }
}

function remover(indice: number): void {
  atualizar(props.modelValue.filter((_, i) => i !== indice))
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
