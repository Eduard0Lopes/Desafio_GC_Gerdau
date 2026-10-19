<template>
  <div>
    
    <section class="grade-seletores">
      <div class="painel">
        <label class="painel-rotulo">Grupo de Serviço</label>
        <ComboboxBusca
          ref="cbGs"
          placeholder="Digite para buscar…"
          :carregar="api.gruposServico.listar"
          :nomeDe="(i: any) => String(i.nome)"
          :idDe="() => ''"
          :permiteExcluir="isAdmin"
          v-model="gs"
          @criarNovo="(t: string) => irParaCadastro('Grupo de Serviço', t)"
          @excluir="(i: any) => pedirExclusao(i, 'GS')"
        />
      </div>

      <div class="painel">
        <label class="painel-rotulo">Código de Fornecedor</label>
        <ComboboxBusca
          ref="cbCf"
          placeholder="Digite para buscar…"
          :carregar="api.codigosFornecedor.listar"
          :nomeDe="(i: any) => String(i.nome)"
          :idDe="(i: any) => String(i.codigoFornecedorSAP || i.id)"
          :permiteExcluir="isAdmin"
          v-model="cf"
          @criarNovo="(t: string) => irParaCadastro('Código de Fornecedor', t)"
          @excluir="(i: any) => pedirExclusao(i, 'CF')"
        />
      </div>

      <div class="painel">
        <label class="painel-rotulo">Código de Serviço</label>
        <ComboboxBusca
          ref="cbCs"
          placeholder="Digite para buscar…"
          :carregar="api.codigosServico.listar"
          :nomeDe="(i: any) => String(i.descricao)"
          :idDe="(i: any) => String(i.id)"
          :sapDe="(i: any) => `${i.quantidadeCaracteres ?? String(i.descricao || '').length}/40 SAP`"
          :permiteExcluir="isAdmin"
          v-model="cs"
          @criarNovo="(t: string) => irParaCadastro('Código de Serviço', t)"
          @excluir="(i: any) => pedirExclusao(i, 'CS')"
        />
      </div>

      <div class="painel">
        <label class="painel-rotulo">Unidade de Medida</label>
        <ComboboxBusca
          ref="cbUm"
          placeholder="Digite para buscar…"
          :carregar="api.unidadesMedida.listar"
          :nomeDe="(i: any) => `${i.sigla} - ${i.nome}`"
          :idDe="() => ''"
          :permiteExcluir="isAdmin"
          v-model="um"
          @criarNovo="(t: string) => irParaCadastro('Unidade de Medida', t)"
          @excluir="(i: any) => pedirExclusao(i, 'UM')"
        />
      </div>
    </section>

    
    <div class="linha-botoes">
      <button class="botao botao-roxo" :disabled="!completo || processando" @click="vincular">
        {{ processando ? 'Processando…' : 'Vincular Serviço' }}
      </button>
      <button class="botao botao-escuro" @click="irParaCadastro('Código de Serviço', '')">
        Cadastrar Novo Item
      </button>
      <button class="botao botao-escuro" @click="limpar">Limpar</button>
    </div>

    
    <div v-if="modalPreview" class="modal-fundo" @click.self="modalPreview = false">
      <div class="modal">
        <h3>Vincular Serviço</h3>
        <div v-if="preview" class="item-similar" style="display: block">
          <div><span class="suave">ID Completo:</span> <strong>{{ preview.idCompleto }}</strong></div>
          <div style="margin-top: 6px"><span class="suave">Descrição:</span> {{ preview.descricaoSnapshot }}</div>
          <div style="margin-top: 6px">
            <span class="suave">Decisão:</span>
            <span class="badge" :class="classeDecisao(preview.decisao)">{{ preview.decisao }}</span>
            <span v-if="preview.melhorScore > 0" class="match" :class="{ alerta: preview.melhorScore < 0.88 }">
              {{ (preview.melhorScore * 100).toFixed(0) }}% Match
            </span>
          </div>

          
          <div
            v-for="a in preview.alertasComponentes || []"
            :key="a.tipo"
            class="item-similar"
            style="margin-top: 10px"
          >
            <span>{{ a.tipo }}:</span>
            <span>“{{ a.descricaoInformada }}”</span>
            <span class="suave">semelhante a</span>
            <span>{{ a.itemSimilarNome }}</span>
            <span
              v-if="a.casoIsolado"
              class="badge-excecao"
              :title="'Caso especial registrado.' + (a.motivoCasoIsolado ? ' Motivo: ' + a.motivoCasoIsolado : '')"
            >!</span>
            <span class="match" :class="{ alerta: a.score < 0.88 }">{{ (a.score * 100).toFixed(0) }}% Match</span>
          </div>
        </div>
        <div class="linha-botoes">
          <button class="botao botao-escuro" @click="modalPreview = false">Voltar</button>
          <button
            v-if="preview?.statusExistente === 'PENDENTE_VALIDACAO'"
            class="botao botao-escuro"
            @click="gerenciarPendente"
          >
            Gerenciar pedido pendente
          </button>
          <button class="botao botao-roxo" :disabled="processando" @click="confirmarVinculo">Confirmar</button>
        </div>
      </div>
    </div>

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

    <ModalBloqueio
      v-if="modalBloqueio"
      :itens="itensBloqueio"
      :motivo="motivoBloqueio"
      @reutilizar="reutilizarItem"
      @governanca="abrirGovernanca"
      @voltar="modalBloqueio = false"
    />

    
    <ModalGovernanca
      v-if="modalGovernanca"
      classe="Código de Serviço"
      :descricao="descricaoNovoServico"
      @voltar="modalGovernanca = false"
      @confirmar="confirmarGovernanca"
    />

    
    <ModalConfirmacao
      v-if="exclusao"
      titulo="Desativar componente do catálogo?"
      :mensagem="`${ROTULO_TIPO[exclusao.tipo]} “${exclusao.rotulo}” sairá de circulação: some das listas de uso e da similaridade, mas nada é perdido. É possível reativar no Catálogo quando quiser.`"
      rotulo-confirmar="Desativar"
      :acao-perigosa="false"
      @confirmar="executarExclusao"
      @cancelar="exclusao = null"
    />

    
    <div v-if="sucesso" class="modal-fundo" @click.self="sucesso = null">
      <div class="modal">
        <div class="aviso aviso-verde">
          <span class="icone">✓</span>
          <div><strong>{{ sucesso.titulo }}</strong><br />{{ sucesso.mensagem }}</div>
        </div>
        <div class="barra-tempo">
          <div class="barra-tempo-fill" :style="{ width: tempoRestante + '%' }"></div>
        </div>
        <div class="suave centralizado" style="margin-top: 8px; font-size: 0.82rem">
          Fecha automaticamente em {{ Math.ceil(tempoRestante / 100 * 5) }}s…
        </div>
        <div class="linha-botoes" style="margin-top: 14px">
          <button class="botao botao-roxo" @click="limpar">Novo registro</button>
          <button v-if="isAdmin" class="botao botao-escuro" @click="$router.push('/admin')">Ver Painel Admin</button>
        </div>
      </div>
    </div>

    
    
    <div v-if="isAdmin" class="painel" style="margin-top: 34px">
      <label class="painel-rotulo">Resumo da base</label>
      <div class="indicadores" style="margin: 0">
        <div class="indicador">
          <div class="rotulo">Serviços únicos</div>
          <div class="valor" style="color: var(--azul)">{{ est.catalogo?.servicos?.toLocaleString('pt-BR') ?? '…' }}</div>
        </div>
        <div class="indicador">
          <div class="rotulo">Registros originais</div>
          <div class="valor">{{ est.matriz?.total?.toLocaleString('pt-BR') ?? '…' }}</div>
        </div>
        <div class="indicador">
          <div class="rotulo">Menos repetições</div>
          <div class="valor" style="color: var(--verde)">{{ reducao }}%</div>
        </div>
        <div class="indicador">
          <div class="rotulo">Aguardando revisão</div>
          <div class="valor" style="color: var(--amarelo-alerta)">{{ est.matriz?.pendentes?.toLocaleString('pt-BR') ?? '…' }}</div>
        </div>
      </div>
      <div class="linha" style="margin-top: 14px">
        <button class="botao botao-escuro" @click="$router.push('/admin')">Abrir Painel Admin →</button>
      </div>
    </div>

    
    <div v-else class="guia">
      <button class="guia-cabecalho" :class="{ aberto: guiaAberto }" @click="guiaAberto = !guiaAberto">
        <span class="seta">{{ guiaAberto ? '▾' : '▸' }}</span>
        <div>
          <h2 style="margin: 0; font-size: 1.12rem; color: var(--azul-escuro)">Guia rápido</h2>
          <p class="suave" style="margin: 2px 0 0">
            {{ guiaAberto ? 'Tudo o que você pode encontrar ao usar o sistema.' : 'Toque para ver como o sistema funciona e o que esperar.' }}
          </p>
        </div>
      </button>

      <div v-if="guiaAberto" class="grade-seletores" style="margin-top: 18px">
        <div class="painel">
          <label class="painel-rotulo">1 · Como vincular um serviço</label>
          <p class="suave" style="margin: 0; line-height: 1.6">
            Escolha um item em cada painel — <strong>Grupo de Serviço</strong>, <strong>Código de Fornecedor</strong>,
            <strong>Código de Serviço</strong> e <strong>Unidade de Medida</strong> — e clique em
            <strong>Vincular Serviço</strong>. Você verá uma prévia com o código completo e a
            decisão antes de confirmar.
          </p>
        </div>

        <div class="painel">
          <label class="painel-rotulo">2 · O que significam as decisões</label>
          <div style="display: flex; flex-direction: column; gap: 8px">
            <div class="aviso aviso-verde" style="margin: 0">
              <span class="icone">✓</span>
              <div><strong>PERMITIR</strong> — sem item semelhante; pode salvar direto.</div>
            </div>
            <div class="aviso aviso-amarelo" style="margin: 0">
              <span class="icone">⚠</span>
              <div><strong>ALERTAR</strong> — há itens parecidos; confira antes de criar.</div>
            </div>
            <div class="aviso" style="margin: 0; background: var(--vermelho-fundo); border: 1px solid var(--vermelho); color: var(--vermelho)">
              <span class="icone">⛔</span>
              <div><strong>REVISAR</strong> — duplicidade provável; a criação é bloqueada.</div>
            </div>
          </div>
        </div>

        <div class="painel">
          <label class="painel-rotulo">3 · Se aparecer um item similar</label>
          <p class="suave" style="margin: 0 0 10px; line-height: 1.6">
            O sistema mostra os semelhantes com o <strong>% de match</strong> e o motivo.
            Você tem três caminhos:
          </p>
          <ul class="suave" style="margin: 0; padding-left: 18px; line-height: 1.9">
            <li><strong>Reutilizar o código existente</strong> — o recomendado;</li>
            <li><strong>Criar com justificativa</strong> — fica registrada na auditoria;</li>
            <li><strong>Enviar para Governança</strong> — um especialista decide depois.</li>
          </ul>
        </div>

        <div class="painel">
          <label class="painel-rotulo">4 · Cadastrar um item novo</label>
          <p class="suave" style="margin: 0; line-height: 1.6">
            Em qualquer busca, a opção <strong>＋ Cadastrar Novo Item</strong> aparece por
            último. A descrição do serviço deve caber em <strong>40 caracteres (limite SAP)</strong>
            — o contador na tela acompanha a digitação.
          </p>
        </div>

        <div class="painel">
          <label class="painel-rotulo">5 · Dicas de busca</label>
          <p class="suave" style="margin: 0; line-height: 1.6">
            A busca entende <strong>acentos ausentes</strong>, <strong>plural e singular</strong>
            (ex.: <em>telhados</em> acha <em>telhado</em> e vice-versa), pequenos erros de digitação e
            palavras invertidas (ex.: <em>mecanico manutencao</em>). Códigos legados também
            funcionam — digite <strong>4033776</strong> para achar o item original.
          </p>
        </div>

        <div class="painel">
          <label class="painel-rotulo">6 · Nada é perdido</label>
          <p class="suave" style="margin: 0; line-height: 1.6">
            Toda decisão (criação, bloqueio, justificativa) é <strong>auditada com autor e
            horário</strong>. A unificação de repetições apenas consolidam o catálogo — o
            histórico dos códigos legados é preservado.
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, inject, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, chaveIdempotente } from '../services/api'
import { toast } from '../services/toast'
import ComboboxBusca from '../components/ComboboxBusca.vue'
import ModalBloqueio from '../components/ModalBloqueio.vue'
import ModalConfirmacao from '../components/ModalConfirmacao.vue'
import ModalGovernanca from '../components/ModalGovernanca.vue'
import ModalPedidoPendente from '../components/ModalPedidoPendente.vue'
import type { CandidatoSimilar, CodigoCompletoResponse, CriarCodigoCompletoPayload, PreviewResponse } from '../types'

type Item = Record<string, unknown>


type ItemBloqueio = CandidatoSimilar & { tipoComponente?: string }


type TipoComponenteCatalogo = 'GS' | 'CF' | 'CS' | 'UM'

const props = withDefaults(defineProps<{ usuario?: string; papel?: string }>(), { usuario: '', papel: 'USUARIO' })
const router = useRouter()
const route = useRoute()
const atualizarPendentes = inject<() => void>('atualizarPendentes', () => {})

const isAdmin = computed(() => props.papel === 'ADMIN')
const guiaAberto = ref(false)
const est = ref<Partial<import('../types').Estatisticas>>({})

async function carregarResumoAdmin(): Promise<void> {
  if (!isAdmin.value) return
  try {
    est.value = await api.estatisticas()
  } catch {
    est.value = {}
  }
}

const reducao = computed(() => {
  const total = est.value.matriz?.total || 0
  const servicos = est.value.catalogo?.servicos || 0
  if (!total || !servicos) return '0,0'
  return ((1 - servicos / total) * 100).toFixed(1).replace('.', ',')
})

const gs = ref<Item | null>(null)
const cf = ref<Item | null>(null)
const cs = ref<Item | null>(null)
const um = ref<Item | null>(null)


const cbGs = ref<InstanceType<typeof ComboboxBusca> | null>(null)
const cbCf = ref<InstanceType<typeof ComboboxBusca> | null>(null)
const cbCs = ref<InstanceType<typeof ComboboxBusca> | null>(null)
const cbUm = ref<InstanceType<typeof ComboboxBusca> | null>(null)

const ROTULO_TIPO: Record<TipoComponenteCatalogo, string> = {
  GS: 'Grupo de Serviço',
  CF: 'Código de Fornecedor',
  CS: 'Código de Serviço',
  UM: 'Unidade de Medida'
}
const exclusao = ref<{ tipo: TipoComponenteCatalogo; id: string; rotulo: string } | null>(null)

function pedirExclusao(item: Item, tipo: TipoComponenteCatalogo): void {
  const nome = tipo === 'CS'
    ? String(item.descricao ?? item.nome ?? item.id)
    : tipo === 'UM'
      ? String(`${item.sigla ?? ''} - ${item.nome ?? ''}`).trim()
      : String(item.nome ?? item.descricao ?? item.id)
  exclusao.value = { tipo, id: String(item.id), rotulo: nome }
}

async function executarExclusao(): Promise<void> {
  const alvo = exclusao.value
  if (!alvo) return
  exclusao.value = null
  try {
    
    const relatorio = await api.situacaoComponentes(alvo.tipo, [alvo.id], 'DESATIVADO')
    const resultado = relatorio[0]
    if (!resultado?.alterado) {
      toast.aviso(resultado?.motivo || 'Nada foi alterado.')
      return
    }
    toast.sucesso(`${ROTULO_TIPO[alvo.tipo]} “${alvo.rotulo}” desativado — fora de circulação. Reative no Catálogo se precisar.`)
    const selecoes: Record<TipoComponenteCatalogo, { value: Item | null }> = { GS: gs, CF: cf, CS: cs, UM: um }
    const paineis: Record<TipoComponenteCatalogo, { recarregar: () => void } | null> = {
      GS: cbGs.value, CF: cbCf.value, CS: cbCs.value, UM: cbUm.value
    }
    
    const selecionado = selecoes[alvo.tipo]
    if (selecionado.value && String(selecionado.value.id) === alvo.id) selecionado.value = null
    paineis[alvo.tipo]?.recarregar()
    atualizarPendentes()
  } catch (e) {
    toast.erro((e as Error).message)
  }
}

const preview = ref<PreviewResponse | null>(null)
const processando = ref(false)
const modalPreview = ref(false)
const modalBloqueio = ref(false)
const modalGovernanca = ref(false)
const sucesso = ref<{ titulo: string; mensagem: string } | null>(null)

// gestão do pedido pendente na governança (ver / editar / cancelar)
const modalPedido = ref(false)
const pedidoPendente = ref<CodigoCompletoResponse | null>(null)
const salvandoPedido = ref(false)
const confirmandoCancelamento = ref(false)
const processandoPedido = ref(false)
const chaveEnvio = ref('')

// só quem enviou o pedido (ou um admin) pode editá-lo/cancelá-lo
const podeGerenciarPedido = computed(() => {
  const p = pedidoPendente.value
  if (!p) return false
  const papel = (localStorage.getItem('papel') || 'USUARIO').toUpperCase()
  return papel === 'ADMIN' || (p.usuarioCriacaoId ?? '') === (props.usuario || '')
})

function formatarData(iso: string | null | undefined): string {
  if (!iso) return ''
  const d = new Date(iso)
  return isNaN(d.getTime()) ? iso : d.toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })
}

async function abrirPedidoPendente(idCompleto: string): Promise<void> {
  try {
    pedidoPendente.value = await api.codigosCompletos.buscar(idCompleto)
    modalPedido.value = true
  } catch (e) {
    toast.erro((e as Error).message)
  }
}

function gerenciarPendente(): void {
  const id = preview.value?.idCompleto
  modalPreview.value = false
  if (id) void abrirPedidoPendente(id)
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
    await vincular()
  } catch (e) {
    toast.erro((e as Error).message)
  } finally {
    processandoPedido.value = false
  }
}
const tempoRestante = ref(100)


watch(sucesso, (novo) => {
  if (!novo) {
    tempoRestante.value = 100
    return
  }
  tempoRestante.value = 100
  const timer = setInterval(() => {
    tempoRestante.value -= 2
    if (tempoRestante.value <= 0) {
      clearInterval(timer)
      if (sucesso.value) sucesso.value = null
    }
  }, 100)
})

const completo = computed(() => gs.value && cf.value && cs.value && um.value)
const descricaoNovoServico = computed(() => preview.value?.codigoServico?.nome || '')


const itensBloqueio = computed<ItemBloqueio[]>(() => {
  const p = preview.value
  if (!p) return []

  
  const lista: ItemBloqueio[] = (p.similares || []).map((c) => ({ ...c, tipoComponente: 'CS' }))
  for (const a of p.alertasComponentes || []) {
    lista.push({
      posicao: 0,
      id: a.itemSimilarId,
      codigoOriginal: null,
      descricao: a.itemSimilarNome,
      score: a.score,
      scoreTokens: 0,
      scoreCaracteres: 0,
      tokensEmComum: [],
      motivo: `${nomeComponente(a.tipo)}: “${a.descricaoInformada}” é semelhante a este item.`,
      tipoComponente: a.tipo
    })
  }
  return lista
})


function nomeComponente(tipo: string): string {
  return ({
    GS: 'Grupo de Serviço',
    CF: 'Código de Fornecedor',
    UM: 'Unidade de Medida',
    CS: 'Código de Serviço'
  } as Record<string, string>)[tipo] || 'Código de Serviço'
}

const motivoBloqueio = computed(() =>
  'O sistema identificou itens semelhantes cadastrados. Escolha um item para reutilizar ou justifique a criação.')

function irParaCadastro(tipo: string, texto: string): void {
  router.push({ path: '/cadastrar-item', query: { tipo, descricao: texto } })
}

function payload(extra: Partial<CriarCodigoCompletoPayload> = {}): CriarCodigoCompletoPayload {
  return {
    gsId: (gs.value?.id as string) ?? null,
    cfId: (cf.value?.id as string) ?? null,
    csId: (cs.value?.id as string) ?? null,
    umId: (um.value?.id as string) ?? null,
    usuarioCriacaoId: props.usuario || null,
    ...extra
  }
}

async function vincular(): Promise<void> {
  processando.value = true
  try {
    preview.value = await api.preview(payload())
    // a composição pode já estar vinculada — isso não bloqueia: cada envio cria um novo vínculo
    if (preview.value.idCompletoJaExiste) {
      toast.aviso(`Este código já está vinculado (${preview.value.idCompleto}) — este envio criará mais um vínculo.`)
    }
    // Idempotency-Key por tentativa: duplo clique no Confirmar não duplica, mas cada
    // clique em "Vincular Serviço" é uma nova tentativa — e cria um novo vínculo
    const corpo = payload()
    chaveEnvio.value = chaveIdempotente('criar-', corpo.gsId, corpo.cfId, corpo.csId, corpo.umId, String(Date.now()))
    if (preview.value.decisao === 'PERMITIR') {
      modalPreview.value = true
    } else {
      modalBloqueio.value = true
    }
  } catch (e) {
    toast.erro((e as Error).message)
  } finally {
    processando.value = false
  }
}


function registrarVinculo(idCompleto: string, descricaoSnapshot: string): void {
  toast.sucesso(`Enviado com sucesso! Serviço ${idCompleto} vinculado.`)
  sucesso.value = { titulo: `Serviço ${idCompleto} vinculado!`, mensagem: descricaoSnapshot }
  atualizarPendentes()
}

async function confirmarVinculo(): Promise<void> {
  processando.value = true
  try {
    const corpo = payload({ acao: 'CRIAR' })
    // Idempotency-Key por tentativa: duplo clique/rede não cria dois registros para o mesmo clique
    const r = await api.criarCodigoCompleto(
      corpo,
      chaveEnvio.value || chaveIdempotente('criar-', corpo.gsId, corpo.cfId, corpo.csId, corpo.umId, String(Date.now()))
    )
    modalPreview.value = false
    if (r.requisicaoRepetida) {
      // nada novo foi criado: dizer isso em vez de fingir uma nova criação
      toast.aviso(`O vínculo ${r.idCompleto} já estava registrado — nada novo foi criado.`)
      sucesso.value = { titulo: 'Vínculo já registrado', mensagem: r.descricaoSnapshot }
      atualizarPendentes()
    } else {
      registrarVinculo(r.idCompleto, r.descricaoSnapshot)
    }
  } catch (e) {
    if ((e as { status?: number }).status === 409 && preview.value) {
      // reavalia o que existe: pedido pendente (abre gestão), ativo (confirma) ou liberado para envio
      modalPreview.value = false
      await vincular()
    } else {
      toast.erro((e as Error).message)
    }
  } finally {
    processando.value = false
  }
}

function reutilizarItem(itemCandidato: CandidatoSimilar): void {
  const item = itemCandidato as ItemBloqueio
  modalBloqueio.value = false

  
  const escolhido: Item = { id: item.id, descricao: item.descricao, nome: item.descricao }
  switch (item.tipoComponente) {
    case 'GS':
      gs.value = escolhido
      break
    case 'CF':
      cf.value = escolhido
      break
    case 'UM': {
      const [sigla, ...resto] = String(item.descricao || '').split(' - ')
      um.value = { ...escolhido, sigla, nome: resto.join(' - ') || sigla }
      break
    }
    default:
      cs.value = escolhido
  }
  toast.sucesso(`Código existente reutilizado: ${item.id} — ${item.descricao}. Confirme a vinculação.`)
  
  void vincular()
}

function abrirGovernanca(): void {
  modalBloqueio.value = false
  modalGovernanca.value = true
  // chave por tentativa: duplo clique/rede não duplica, mas um novo envio é um novo pedido
  const corpo = payload()
  chaveEnvio.value = chaveIdempotente('gov-', corpo.gsId, corpo.cfId, corpo.csId, corpo.umId, String(Date.now()))
}

async function confirmarGovernanca(justificativa: string): Promise<void> {
  processando.value = true
  try {
    const corpo = payload({ acao: 'ENVIAR_VALIDACAO', justificativa })
    const r = await api.criarCodigoCompleto(
      corpo,
      chaveEnvio.value || chaveIdempotente('gov-', corpo.gsId, corpo.cfId, corpo.csId, corpo.umId, justificativa)
    )
    modalGovernanca.value = false
    if (r.requisicaoRepetida) {
      toast.aviso(`Este pedido já havia sido enviado (${r.idCompleto}) — nada novo foi criado.`)
      sucesso.value = {
        titulo: 'Pedido já enviado',
        mensagem: `O cadastro ${r.idCompleto} já estava na fila de validação da Governança.`
      }
    } else {
      toast.sucesso('Enviado com sucesso! O cadastro foi para a fila de validação da Governança.')
      sucesso.value = {
        titulo: 'Enviado para Governança',
        mensagem: `O cadastro ${r.idCompleto} passará por validação humana pela Equipe de Governança de Dados.`
      }
    }
    atualizarPendentes()
  } catch (e) {
    if ((e as { status?: number }).status === 409 && preview.value) {
      modalGovernanca.value = false
      await vincular()
    } else {
      toast.erro((e as Error).message)
    }
  } finally {
    processando.value = false
  }
}

function classeDecisao(d: string): string {
  return d === 'PERMITIR' ? 'badge-permitir' : d === 'ALERTAR' ? 'badge-alertar' : 'badge-revisar'
}

function limpar(): void {
  gs.value = cf.value = cs.value = um.value = null
  preview.value = null
  sucesso.value = null
  modalPreview.value = modalBloqueio.value = modalGovernanca.value = false
  fecharPedido()
}





function aplicarReutilizacao(): void {
  const id = route.query.reutilizar
  if (!id) return
  const tipoItem = String(route.query.tipo || 'CS').toUpperCase()
  const texto = String(route.query.descricao || '')
  const item: Item = { id: String(id), descricao: texto, nome: texto }
  let painel = 'Código de Serviço'
  if (tipoItem === 'GS') {
    gs.value = item
    painel = 'Grupo de Serviço'
  } else if (tipoItem === 'CF') {
    cf.value = item
    painel = 'Código de Fornecedor'
  } else if (tipoItem === 'UM') {
    const [sigla, ...resto] = texto.split(' - ')
    um.value = { ...item, sigla, nome: resto.join(' - ') || sigla }
    painel = 'Unidade de Medida'
  } else {
    cs.value = item
  }
  toast.sucesso(`${String(id)} — ${texto} selecionado como ${painel}. Complete a vinculação.`)
  
  void router.replace({ query: {} })
}

onMounted(() => {
  carregarResumoAdmin()
  aplicarReutilizacao()
  
  if (route.query.criado) void router.replace({ query: {} })
})
watch(() => props.papel, carregarResumoAdmin)
</script>
