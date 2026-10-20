<template>
  <div>
    <div v-if="!completo" class="aviso aviso-azul">
      <span class="icone">ℹ</span>
      <div>
        Escolha <strong>Grupo de Serviço</strong>, <strong>Código de Fornecedor</strong> e
        <strong>Unidade de Medida</strong> nos painéis acima — valem para todos os códigos marcados.
      </div>
    </div>

    <SelecaoMultiplaCS v-model="escolhidos" :carregar="buscarCodigos" :resolver="resolverLinhas" />

    <div v-if="escolhidos.length" class="aviso" :class="aCriar ? 'aviso-amarelo' : 'aviso-verde'">
      <span class="icone">{{ aCriar ? '⚠' : '✓' }}</span>
      <div>
        Há <strong>{{ disponiveis }} disponível(is)</strong> — porém
        <strong>{{ aCriar }} precisa(m) ser criado(s)</strong>.
        <div class="suave" style="margin-top: 5px">
          Disponíveis são reutilizados como estão. Dos novos, os que têm similaridade alertada vão para a
          Governança — que vê os matches que ninguém escolheu e decide.
          <template v-if="comAlerta"> Nesta seleção: {{ comAlerta }} com aviso de similaridade.</template>
        </div>
      </div>
    </div>

    <div class="linha-botoes">
      <button class="botao botao-roxo" :disabled="!podeVincular" @click="confirmando = true">
        {{ processando ? 'Vinculando…' : `Vincular ${escolhidos.length} código(s) em massa` }}
      </button>
    </div>

    <ModalConfirmacao
      v-if="confirmando"
      titulo="Vincular os códigos marcados?"
      :mensagem="`Os ${escolhidos.length} códigos escolhidos serão vinculados a ${resumoComposicao}. ` +
        `${disponiveis} já ${disponiveis === 1 ? 'está disponível e será reutilizado' : 'estão disponíveis e serão reutilizados'}; ` +
        `${aCriar} precisa(m) ser criado(s)${comAlerta ? ` — e ${comAlerta} com similaridade alertada vão para a Governança` : ''}.`"
      rotulo-confirmar="Vincular em massa"
      :acao-perigosa="false"
      @confirmar="vincular"
      @cancelar="confirmando = false"
    />

    <div
      v-if="resultado"
      class="aviso"
      :class="resultado.falhas ? 'aviso-amarelo' : 'aviso-verde'"
      style="margin-top: 14px"
    >
      <span class="icone">{{ resultado.falhas ? '⚠' : '✓' }}</span>
      <div>
        <strong>{{ resultado.vinculados }} vínculo(s) ativo(s)</strong>
        <span v-if="resultado.disponiveis"> · {{ resultado.disponiveis }} reutilizando código disponível</span>
        <span v-if="resultado.criados"> · {{ resultado.criados }} com código novo criado</span>
        <span v-if="resultado.aguardandoValidacao">
          · {{ resultado.aguardandoValidacao }} aguardando validação da Governança
        </span>
        <span v-if="resultado.bloqueadas"> · {{ resultado.bloqueadas }} bloqueada(s)</span>
        <span v-if="resultado.falhas"> · {{ resultado.falhas }} falha(s)</span>
        <div
          v-for="f in pendencias"
          :key="f.linha"
          class="suave"
          style="margin-top: 5px"
        >
          <strong>{{ f.texto }}</strong> — {{ f.motivo }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { api, chaveIdempotente } from '../services/api'
import { toast } from '../services/toast'
import { normalizar } from '../services/texto'
import ModalConfirmacao from '../components/ModalConfirmacao.vue'
import SelecaoMultiplaCS from '../components/SelecaoMultiplaCS.vue'
import type { ItemCsMassa, ResultadoVinculacaoLote } from '../types'

type Item = Record<string, unknown>

/**
 * Vinculação em massa (feedback Gerdau): GS/CF/UM escolhidos uma vez e vários
 * Códigos de Serviço marcados com checkbox. A tela de informação avisa
 * "há X disponíveis — porém Y precisam ser criados" antes de vincular.
 */
const props = withDefaults(
  defineProps<{ gs: Item | null; cf: Item | null; um: Item | null; usuario?: string }>(),
  { usuario: '' }
)
const emit = defineEmits<{ (e: 'vinculado', resultado: ResultadoVinculacaoLote): void }>()

const escolhidos = ref<ItemCsMassa[]>([])
const confirmando = ref(false)
const processando = ref(false)
const resultado = ref<ResultadoVinculacaoLote | null>(null)

const completo = computed(() => Boolean(props.gs && props.cf && props.um))
const podeVincular = computed(() => completo.value && escolhidos.value.length > 0 && !processando.value)
const disponiveis = computed(() => escolhidos.value.filter((i) => i.classificacao === 'DISPONIVEL').length)
const aCriar = computed(() => escolhidos.value.filter((i) => i.classificacao === 'CRIAR').length)
const comAlerta = computed(() => escolhidos.value.filter((i) => i.situacao === 'ALERTA').length)
const resumoComposicao = computed(
  () => `${String(props.gs?.nome ?? props.gs?.id)} / ${String(props.cf?.nome ?? props.cf?.id)} / ${String(props.um?.sigla ?? props.um?.id)}`
)
const pendencias = computed(() =>
  (resultado.value?.itens ?? []).filter((i) => i.classificacao === 'FALHA' || i.classificacao === 'BLOQUEADA').slice(0, 5)
)

function normalizarTexto(texto: string): string {
  return normalizar(texto.trim())
}

function buscarCodigos(termo: string) {
  return api.codigosServico.listar(termo, 8)
}

/** o mesmo motor da tela de informação: diz o que é disponível e o que precisa ser criado */
async function resolverLinhas(textos: string[]): Promise<ItemCsMassa[]> {
  const lote = await api.similaridadeLote(textos, 'CS')
  const itens: ItemCsMassa[] = []
  for (const item of lote.itens) {
    if (item.classificacao === 'BLOQUEADA') {
      toast.aviso(`“${item.texto}” não entrou: ${item.motivo}`)
      continue
    }
    const match = item.similar
      ? { id: item.similar.id, descricao: item.similar.descricao, score: item.similar.score }
      : null
    if (item.classificacao === 'DISPONIVEL' && item.similar) {
      itens.push({
        chave: item.similar.id,
        texto: item.texto,
        csId: item.similar.id,
        classificacao: 'DISPONIVEL',
        situacao: item.situacao,
        motivo: item.motivo,
        match
      })
    } else {
      itens.push({
        chave: `novo:${normalizarTexto(item.texto)}`,
        texto: item.texto,
        csId: null,
        classificacao: 'CRIAR',
        situacao: item.situacao,
        motivo: item.motivo,
        match
      })
    }
  }
  return itens
}

async function vincular(): Promise<void> {
  confirmando.value = false
  if (!podeVincular.value) return
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
      `${efetivos} vínculo(s) processado(s) — ${resposta.disponiveis} reutilizado(s), ` +
        `${resposta.criados} criado(s)` +
        (resposta.aguardandoValidacao ? `, ${resposta.aguardandoValidacao} para a Governança` : '') +
        '.'
    )
    escolhidos.value = []
    emit('vinculado', resposta)
  } catch (e) {
    toast.erro((e as Error).message)
  } finally {
    processando.value = false
  }
}
</script>

<style scoped>
.linha-botoes {
  margin-top: 18px;
}
</style>
