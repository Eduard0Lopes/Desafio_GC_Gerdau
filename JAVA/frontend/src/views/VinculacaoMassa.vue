<template>
  <div class="vinculacao-massa">
    <SelecaoMultiplaCS v-model="escolhidos" :carregar="buscarCodigos" :resolver="resolverLinhas" />

    <div class="massa-rodape">
      <div class="massa-resumo" aria-live="polite">
        <template v-if="escolhidos.length">
          <strong>{{ escolhidos.length }} {{ plural(escolhidos.length, 'código', 'códigos') }}</strong>
          <span class="verde">{{ disponiveis }} reutilizar</span>
          <span class="amarelo">{{ aCriar }} criar</span>
          <span v-if="comAlerta" class="vermelho">{{ comAlerta }} Governança</span>
        </template>
        <span v-else class="suave">Marque os códigos que quer vincular.</span>
        <span v-if="faltantes" class="vermelho">Falta escolher {{ faltantes }}.</span>
      </div>

      <div class="acoes">
        <button class="botao botao-roxo" @click="pedirConfirmacao">{{ rotuloAcao }}</button>
        <button class="botao botao-escuro" @click="emit('cadastrar')">Cadastrar Novo Item</button>
        <button class="botao botao-escuro" @click="emit('limpar')">Limpar tudo</button>
      </div>
    </div>

    <!-- prévia — mesma linguagem do modal "Vincular Serviço" -->
    <div v-if="confirmando" class="modal-fundo" @click.self="confirmando = false">
      <div class="modal">
        <h3>Vincular em massa</h3>
        <div class="item-similar" style="display: block">
          <div><span class="suave">Composição:</span> <strong>{{ resumoComposicao }}</strong></div>
          <div style="margin-top: 6px">
            <span class="suave">Códigos:</span>
            <span class="badge badge-permitir">{{ disponiveis }} reutilizar</span>
            <span class="badge badge-alertar">{{ aCriar }} criar</span>
            <span v-if="comAlerta" class="badge badge-revisar">{{ comAlerta }} Governança</span>
          </div>
        </div>
        <div class="linha-botoes">
          <button class="botao botao-escuro" @click="confirmando = false">Voltar</button>
          <button class="botao botao-roxo" :disabled="processando" @click="vincular">Confirmar</button>
        </div>
      </div>
    </div>

    <!-- resultado — mesmo modal de sucesso do vínculo único -->
    <div v-if="resultado" class="modal-fundo" @click.self="fechar">
      <div class="modal">
        <div class="aviso" :class="pendencias.length ? 'aviso-amarelo' : 'aviso-verde'">
          <span class="icone">{{ pendencias.length ? '⚠' : '✓' }}</span>
          <div>
            <strong>{{ resultado.vinculados }} {{ plural(resultado.vinculados, 'vínculo ativo', 'vínculos ativos') }}</strong>
            <span v-if="resultado.aguardandoValidacao">
              · {{ resultado.aguardandoValidacao }} aguardando validação da Governança
            </span>
            <br />{{ resultado.reutilizados }} reutilizado(s), {{ resultado.criados }} criado(s).
          </div>
        </div>

        <ul v-if="pendencias.length" class="lista-pendencias">
          <li v-for="p in pendencias" :key="p.linha">
            <strong>{{ p.texto }}</strong> <span class="suave">— {{ p.motivo }}</span>
          </li>
        </ul>

        <template v-if="!pendencias.length">
          <div class="barra-tempo">
            <div class="barra-tempo-fill" :style="{ width: tempoRestante + '%' }"></div>
          </div>
          <div class="suave centralizado" style="margin-top: 8px; font-size: 0.82rem">
            Fecha automaticamente em {{ Math.ceil((tempoRestante / 100) * 5) }}s…
          </div>
        </template>

        <div class="linha-botoes" style="margin-top: 14px">
          <button class="botao botao-roxo" @click="novaSelecao">Nova seleção</button>
          <button class="botao botao-escuro" @click="fechar">Fechar</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onUnmounted, ref, watch } from 'vue'
import { api, chaveIdempotente } from '../services/api'
import { toast } from '../services/toast'
import { normalizar } from '../services/texto'
import SelecaoMultiplaCS from '../components/SelecaoMultiplaCS.vue'
import type { ItemCsMassa, ItemLote, ResultadoVinculacaoLote } from '../types'

type Item = Record<string, unknown>

/**
 * Vinculação em massa (feedback Gerdau): GS/CF/UM escolhidos uma vez e vários
 * Códigos de Serviço marcados com checkbox. Segue o mesmo fluxo do vínculo
 * único: prévia antes de confirmar e modal de sucesso — e o que tem similaridade
 * alertada vai para a Governança.
 *
 * Nada aqui trava o usuário: o botão nunca fica desabilitado (o que falta é
 * dito na tela e no clique) e a seleção permanece após vincular — o mesmo
 * lote pode ser vinculado quantas vezes forem necessárias.
 */
const props = withDefaults(
  defineProps<{ gs: Item | null; cf: Item | null; um: Item | null; usuario?: string }>(),
  { usuario: '' }
)
const emit = defineEmits<{
  (e: 'vinculado', resultado: ResultadoVinculacaoLote): void
  (e: 'cadastrar'): void
  (e: 'limpar'): void
}>()

const escolhidos = ref<ItemCsMassa[]>([])
const confirmando = ref(false)
const processando = ref(false)
const resultado = ref<ResultadoVinculacaoLote | null>(null)
const tempoRestante = ref(100)

const disponiveis = computed(() => escolhidos.value.filter((i) => i.classificacao === 'DISPONIVEL').length)
const aCriar = computed(() => escolhidos.value.filter((i) => i.classificacao === 'CRIAR').length)
const comAlerta = computed(() => escolhidos.value.filter((i) => i.situacao === 'ALERTA').length)

const faltantes = computed(() => {
  const partes: string[] = []
  if (!props.gs) partes.push('Grupo de Serviço')
  if (!props.cf) partes.push('Código de Fornecedor')
  if (!props.um) partes.push('Unidade de Medida')
  if (!partes.length) return ''
  if (partes.length === 1) return partes[0]
  return `${partes.slice(0, -1).join(', ')} e ${partes[partes.length - 1]}`
})

const rotuloAcao = computed(() => {
  if (processando.value) return 'Processando…'
  if (!escolhidos.value.length) return 'Vincular em massa'
  return `Vincular ${escolhidos.value.length} ${plural(escolhidos.value.length, 'código', 'códigos')}`
})

const rotuloUm = computed(() => {
  const um = props.um
  if (!um) return 'a escolher'
  const sigla = String(um.sigla ?? '')
  const nome = String(um.nome ?? '')
  if (sigla && nome && nome !== sigla) return `${sigla} · ${nome}`
  return sigla || nome || rotuloItem(um, 'nome')
})
const resumoComposicao = computed(
  () => `${rotuloItem(props.gs, 'nome')} / ${rotuloItem(props.cf, 'nome')} / ${rotuloUm.value}`
)

const pendencias = computed(() =>
  (resultado.value?.itens ?? []).filter((i) => i.classificacao === 'BLOQUEADA' || i.classificacao === 'FALHA')
)

/** concorda o número com o substantivo: 1 código · 2 códigos */
function plural(n: number, singular: string, pluralForma: string): string {
  return n === 1 ? singular : pluralForma
}

function rotuloItem(item: Item | null, campo: string): string {
  if (!item) return 'a escolher'
  const valor = item[campo] ?? item.id ?? ''
  return String(valor) || '—'
}

function buscarCodigos(termo: string) {
  return api.codigosServico.listar(termo, 8)
}

/**
 * O mesmo motor da tela de informação: diz o que é disponível e o que precisa
 * ser criado. Textos repetidos são avaliados uma vez e cada ocorrência entra —
 * repetir um código nunca é barrado.
 */
async function resolverLinhas(textos: string[]): Promise<ItemCsMassa[]> {
  const chaveDe = (texto: string) => normalizar(texto.trim())
  const unicos: string[] = []
  const vistas = new Set<string>()
  for (const texto of textos) {
    const chave = chaveDe(texto)
    if (!chave || vistas.has(chave)) continue
    vistas.add(chave)
    unicos.push(texto.trim())
  }
  const analises = new Map<string, ItemCsMassa>()
  if (unicos.length) {
    const lote = await api.similaridadeLote(unicos, 'CS')
    for (const item of lote.itens) {
      if (item.classificacao === 'BLOQUEADA') {
        toast.aviso(`“${item.texto}” não entrou: ${item.motivo}`)
        continue
      }
      analises.set(chaveDe(item.texto), paraItem(item))
    }
  }
  return textos.map((t) => analises.get(chaveDe(t))).filter((i): i is ItemCsMassa => Boolean(i))
}

function paraItem(item: ItemLote): ItemCsMassa {
  const similar = item.similar
  const disponivel = item.classificacao === 'DISPONIVEL' && similar !== null
  return {
    chave: disponivel && similar ? String(similar.id) : `novo:${normalizar(item.texto.trim())}`,
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

function pedirConfirmacao(): void {
  if (processando.value) return
  if (faltantes.value) {
    toast.erro(`Falta escolher ${faltantes.value} para compor o vínculo.`)
    return
  }
  if (!escolhidos.value.length) {
    toast.aviso('Marque ao menos um Código de Serviço para vincular.')
    return
  }
  confirmando.value = true
}

async function vincular(): Promise<void> {
  if (processando.value) return
  confirmando.value = false
  processando.value = true
  try {
    const resposta = await api.vincularLote(
      {
        gsId: String(props.gs?.id ?? ''),
        cfId: String(props.cf?.id ?? ''),
        umId: String(props.um?.id ?? ''),
        itens: escolhidos.value.map((i) => ({ csId: i.csId, texto: i.csId ? null : i.texto })),
        usuarioCriacaoId: props.usuario || null
      },
      chaveIdempotente(
        'lote-',
        String(props.gs?.id ?? ''),
        String(props.cf?.id ?? ''),
        String(props.um?.id ?? ''),
        escolhidos.value.map((i) => i.chave).join('|'),
        String(Date.now())
      )
    )
    resultado.value = resposta
    const efetivos = resposta.vinculados + resposta.aguardandoValidacao
    toast.sucesso(
      `${efetivos} ${plural(efetivos, 'vínculo processado', 'vínculos processados')} — ` +
        `${resposta.reutilizados} reutilizado(s), ${resposta.criados} criado(s).`
    )
    // a seleção fica: o mesmo lote pode ser vinculado de novo sem refazer tudo
    emit('vinculado', resposta)
  } catch (e) {
    toast.erro((e as Error).message)
  } finally {
    processando.value = false
  }
}

function novaSelecao(): void {
  fechar()
  escolhidos.value = []
}

function fechar(): void {
  resultado.value = null
}

// fecha sozinho quando não há pendências — igual ao vínculo único
let relogio: ReturnType<typeof setInterval> | null = null
watch(resultado, (novo) => {
  if (relogio) clearInterval(relogio)
  relogio = null
  if (!novo) {
    tempoRestante.value = 100
    return
  }
  tempoRestante.value = 100
  relogio = setInterval(() => {
    tempoRestante.value -= 2
    if (tempoRestante.value <= 0) {
      if (relogio) clearInterval(relogio)
      relogio = null
      if (resultado.value && !pendencias.value.length) resultado.value = null
    }
  }, 100)
})
onUnmounted(() => {
  if (relogio) clearInterval(relogio)
})
</script>

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
