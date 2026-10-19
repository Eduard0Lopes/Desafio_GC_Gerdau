<template>
  <div class="cartao-central">
    <h2>Cadastrar Novo Item</h2>

    <div class="campo-form">
      <label for="tipo">Tipo de Componente</label>
      <select id="tipo" v-model="tipo">
        <option value="CS">Código de Serviço</option>
        <option value="GS">Grupo de Serviço</option>
        <option value="CF">Código de Fornecedor</option>
        <option value="UM">Unidade de Medida</option>
      </select>
    </div>

    <div class="campo-form">
      <div class="rotulo-com-badge">
        <label for="descricao">{{ rotuloDescricao }}</label>
        <span
          v-if="tipo === 'CS'"
          class="contador-sap"
          :class="{ estourado: descricao.length > 40 }"
        >
          {{ descricao.length }}/40 Caracteres SAP
        </span>
      </div>
      <div class="linha-campo">
        <input
          id="descricao"
          type="text"
          v-model="descricao"
          :placeholder="placeholderDescricao"
          @input="aoDigitar"
        />
      </div>
    </div>

    <div v-if="tipo === 'UM'" class="campo-form">
      <label for="nome-um">Nome da Unidade</label>
      <input id="nome-um" type="text" v-model="nomeExtra" placeholder="Ex.: Hora" />
    </div>

    <div v-if="tipo === 'CF'" class="campo-form">
      <label for="sap">Código SAP do Fornecedor (opcional)</label>
      <input id="sap" type="text" v-model="codigoSap" placeholder="Ex.: 200982674" />
    </div>

    <div v-if="tipo === 'CS'" class="campo-form">
      <label for="um">Unidade de Medida</label>
      <select id="um" v-model="umId">
        <option value="">— selecione —</option>
        <option v-for="u in unidades" :key="u.id" :value="u.id">{{ u.sigla }} - {{ u.nome }}</option>
      </select>
    </div>

    
    <div v-if="verificacao">
      <div v-if="verificacao.candidatos.length" class="aviso aviso-amarelo">
        <span class="icone">⚠</span>
        <div>
          Foram encontrados <strong>{{ verificacao.candidatos.length }} item(ns) semelhante(s)</strong>
          — decisão recomendada: <strong>{{ verificacao.decisao }}</strong>
        </div>
      </div>
      <div v-for="c in verificacao.candidatos.slice(0, 3)" :key="c.id" class="item-similar">
        <span>ID: {{ c.id }}</span>
        <span class="suave">|</span>
        <span>{{ (c.descricao || '').toUpperCase() }}</span>
        <span
          v-if="c.casoIsolado"
          class="badge-excecao"
          :title="'Caso especial registrado.' + (c.motivoCasoIsolado ? ' Motivo: ' + c.motivoCasoIsolado : '')"
        >!</span>
        <span class="match" :class="{ alerta: c.score < 0.88 }">{{ (c.score * 100).toFixed(0) }}% Match</span>
      </div>
      <div v-if="!verificacao.candidatos.length" class="aviso aviso-verde">
        <span class="icone">✓</span>
        <div>Nenhum item semelhante na base — pode prosseguir com o cadastro.</div>
      </div>
    </div>

    <div class="linha-botoes">
      <button class="botao botao-roxo" :disabled="!valido || salvando" @click="salvar">
        {{ salvando ? 'Salvando…' : 'Salvar Cadastro' }}
      </button>
      <button class="botao botao-escuro" :disabled="!descricao.trim()" @click="verificarSimilaridade">
        Verificar Similaridade
      </button>
      <button class="botao botao-escuro" @click="$router.push('/')">Voltar</button>
    </div>

    <ModalBloqueio
      v-if="modalBloqueio"
      :itens="verificacao?.candidatos || []"
      motivo="O sistema identificou itens semelhantes cadastrados. Escolha um item para reutilizar ou envie para governança."
      @reutilizar="reutilizar"
      @governanca="abrirGovernanca"
      @voltar="modalBloqueio = false"
    />

    <ModalGovernanca
      v-if="modalGovernanca"
      classe="Novo item de catálogo"
      :descricao="descricao"
      @voltar="modalGovernanca = false"
      @confirmar="enviarGovernanca"
    />

    <ModalPedidoPendente
      v-if="modalPedido"
      :pedido="pedidoPendente"
      :pode-gerenciar="podeGerenciarPedido"
      :processando="processandoPedido"
      :salvando="salvandoPedido"
      @fechar="fecharPedido"
      @cancelar="confirmandoCancelamento = true"
      @salvar="salvarPedido"
    />

    <ModalConfirmacao
      v-if="confirmandoCancelamento"
      titulo="Cancelar o pedido?"
      mensagem="O pedido sai da fila da governança e a composição fica livre para um novo envio. O histórico fica preservado."
      rotulo-confirmar="✕ Cancelar pedido"
      :acao-perigosa="true"
      @confirmar="confirmarCancelamento"
      @cancelar="confirmandoCancelamento = false"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, chaveIdempotente } from '../services/api'
import { toast } from '../services/toast'
import ModalBloqueio from '../components/ModalBloqueio.vue'
import ModalGovernanca from '../components/ModalGovernanca.vue'
import ModalPedidoPendente from '../components/ModalPedidoPendente.vue'
import ModalConfirmacao from '../components/ModalConfirmacao.vue'
import type { CandidatoSimilar, CodigoCompletoResponse, CriarCodigoCompletoPayload, SimilaridadeResponse, TipoComponente, UnidadeMedida } from '../types'

const props = withDefaults(defineProps<{ usuario?: string }>(), { usuario: '' })
const route = useRoute()
const router = useRouter()


const ROTULOS: Record<string, string> = { CS: 'Código de Serviço', GS: 'Grupo de Serviço', CF: 'Código de Fornecedor', UM: 'Unidade de Medida' }
const PARA_TIPO: Record<string, TipoComponente> = { 'Código de Serviço': 'CS', 'Grupo de Serviço': 'GS', 'Código de Fornecedor': 'CF', 'Unidade de Medida': 'UM' }

const tipo = ref<TipoComponente>('CS')
const descricao = ref('')
const nomeExtra = ref('')
const codigoSap = ref('')
const umId = ref('')
const unidades = ref<UnidadeMedida[]>([])
const verificacao = ref<SimilaridadeResponse | null>(null)
const salvando = ref(false)
const modalBloqueio = ref(false)
const modalGovernanca = ref(false)

const rotuloDescricao = computed(() => (tipo.value === 'UM' ? 'Sigla da Unidade' : 'Descrição do Item'))
const placeholderDescricao = computed(() => (tipo.value === 'UM' ? 'Ex.: HR' : 'Descreva o item…'))

const valido = computed(() => {
  if (!descricao.value.trim()) return false
  if (tipo.value === 'CS' && descricao.value.length > 40) return false
  return true
})

onMounted(async () => {
  const informado = String(route.query.tipo || '')
  tipo.value = PARA_TIPO[informado]
    || (Object.keys(ROTULOS).find((k) => ROTULOS[k].normalize('NFC') === informado.normalize('NFC')) as TipoComponente)
    || 'CS'
  if (route.query.descricao) descricao.value = String(route.query.descricao)
  try {
    unidades.value = await api.unidadesMedida.listar('', 100)
  } catch {
    unidades.value = []
  }
})


watch(tipo, () => {
  verificacao.value = null
  if (descricao.value.trim()) verificarSimilaridade()
})

let timer: ReturnType<typeof setTimeout> | null = null
function aoDigitar(): void {
  if (timer) clearTimeout(timer)
  timer = setTimeout(async () => {
    if (!descricao.value.trim()) {
      verificacao.value = null
      return
    }
    try {
      verificacao.value = await api.similaridade(descricao.value.trim(), 5, tipo.value)
    } catch {
      verificacao.value = null
    }
  }, 450)
}

async function verificarSimilaridade(): Promise<void> {
  if (!descricao.value.trim()) return
  verificacao.value = await api.similaridade(descricao.value.trim(), 5, tipo.value)
}

async function salvar(): Promise<void> {
  salvando.value = true
  try {
    const v = await api.similaridade(descricao.value.trim(), 5, tipo.value)
    verificacao.value = v
    if (v.decisao !== 'PERMITIR' && v.candidatos.length > 0) {
      modalBloqueio.value = true
      return
    }
    await criarItem()
    
    const nome = tipo.value === 'UM' && nomeExtra.value.trim()
      ? `${descricao.value.trim()} - ${nomeExtra.value.trim()}`
      : descricao.value.trim()
    toast.sucesso(`${ROTULOS[tipo.value]} “${nome}” cadastrado com sucesso!`)
    router.push({ path: '/', query: { criado: '1' } })
  } catch (e) {
    if ((e as { status?: number }).status === 409 && (verificacao.value?.candidatos.length ?? 0) > 0) {
      modalBloqueio.value = true
    } else if ((e as { status?: number }).status === 409) {
      toast.erro((e as Error).message)
    } else {
      toast.erro((e as Error).message)
    }
  } finally {
    salvando.value = false
  }
}

async function criarItem(): Promise<void> {
  if (tipo.value === 'CS') {
    await api.codigosServico.criar({ descricao: descricao.value.trim() })
  } else if (tipo.value === 'GS') {
    await api.gruposServico.criar({ nome: descricao.value.trim() })
  } else if (tipo.value === 'CF') {
    await api.codigosFornecedor.criar({
      nome: descricao.value.trim(),
      codigoSap: codigoSap.value.trim() || null
    })
  } else {
    await api.unidadesMedida.criar({
      sigla: descricao.value.trim(),
      nome: nomeExtra.value.trim() || descricao.value.trim()
    })
  }
}

function reutilizar(item: CandidatoSimilar): void {
  toast.sucesso(`Código existente reutilizado: ${item.id} — ${item.descricao}. Nenhum novo item foi criado.`)
  router.push({ path: '/', query: { reutilizar: item.id, tipo: tipo.value, descricao: item.descricao } })
}

// pedido pendente na governança (ver / editar / cancelar)
const modalPedido = ref(false)
const pedidoPendente = ref<CodigoCompletoResponse | null>(null)
const salvandoPedido = ref(false)
const processandoPedido = ref(false)
const confirmandoCancelamento = ref(false)
const chaveEnvio = ref('')

const podeGerenciarPedido = computed(() => {
  const p = pedidoPendente.value
  if (!p) return false
  const papel = (localStorage.getItem('papel') || 'USUARIO').toUpperCase()
  return papel === 'ADMIN' || (p.usuarioCriacaoId ?? '') === (props.usuario || '')
})

function abrirGovernanca(): void {
  modalBloqueio.value = false
  modalGovernanca.value = true
  // chave por tentativa: duplo clique não duplica, mas um novo envio é um novo pedido
  chaveEnvio.value = chaveIdempotente('gov-', tipo.value, descricao.value, String(Date.now()))
}

function payloadGovernanca(justificativa: string): CriarCodigoCompletoPayload {
  return {
    gsNovoNome: 'Governança',
    cfNovoNome: 'Governança',
    csNovaDescricao: descricao.value.trim(),
    umNovaSigla: 'UN',
    acao: 'ENVIAR_VALIDACAO',
    justificativa,
    usuarioCriacaoId: props.usuario || null
  } as CriarCodigoCompletoPayload
}

async function enviarGovernanca(justificativa: string): Promise<void> {
  salvando.value = true
  try {
    const r = await api.criarCodigoCompleto(
      payloadGovernanca(justificativa),
      chaveEnvio.value || chaveIdempotente('gov-', tipo.value, descricao.value, justificativa)
    )
    modalGovernanca.value = false
    if (r.requisicaoRepetida) {
      // nada novo foi criado: ser honesto em vez de fingir um novo envio
      toast.aviso(`Este pedido já havia sido enviado (${r.idCompleto}) — nada novo foi criado.`)
    } else {
      toast.sucesso('Enviado para Governança — o cadastro passará por validação humana pela Equipe de Governança de Dados.')
    }
    router.push('/')
  } catch (e) {
    if ((e as { status?: number }).status === 409) {
      // já existe pedido/composição igual: abre o popup do pedido em vez de fingir sucesso
      modalGovernanca.value = false
      await abrirPedidoPendente()
    } else {
      toast.erro((e as Error).message)
    }
  } finally {
    salvando.value = false
  }
}

async function abrirPedidoPendente(): Promise<void> {
  try {
    const preview = await api.preview(payloadGovernanca('') as never)
    if (preview.idCompletoJaExiste && preview.statusExistente === 'PENDENTE_VALIDACAO') {
      pedidoPendente.value = await api.codigosCompletos.buscar(preview.idCompleto)
      modalPedido.value = true
    } else {
      toast.erro('Não foi possível enviar: houve conflito de requisição. Tente novamente em instantes.')
    }
  } catch (e) {
    toast.erro((e as Error).message)
  }
}

function fecharPedido(): void {
  modalPedido.value = false
  pedidoPendente.value = null
  confirmandoCancelamento.value = false
}

async function salvarPedido(dados: { descricao: string; justificativa: string }): Promise<void> {
  if (!pedidoPendente.value || salvandoPedido.value) return
  salvandoPedido.value = true
  try {
    pedidoPendente.value = await api.codigosCompletos.atualizarPedido(pedidoPendente.value.id, {
      descricao: dados.descricao || null,
      justificativa: dados.justificativa || null
    })
    toast.sucesso('Pedido atualizado — a governança verá a versão mais nova.')
  } catch (e) {
    toast.erro((e as Error).message)
  } finally {
    salvandoPedido.value = false
  }
}

async function confirmarCancelamento(): Promise<void> {
  if (!pedidoPendente.value || processandoPedido.value) return
  processandoPedido.value = true
  confirmandoCancelamento.value = false
  try {
    await api.codigosCompletos.cancelarPedido(pedidoPendente.value.id)
    fecharPedido()
    toast.sucesso('Pedido cancelado. A composição está livre para um novo envio.')
  } catch (e) {
    toast.erro((e as Error).message)
  } finally {
    processandoPedido.value = false
  }
}
</script>


