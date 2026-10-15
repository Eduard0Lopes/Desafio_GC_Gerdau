<template>
  <div>
    <!-- 4 painéis de seleção (print 1) -->
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

    <!-- botões principais (print 1) -->
    <div class="linha-botoes">
      <button class="botao botao-roxo" :disabled="!completo || processando" @click="vincular">
        {{ processando ? 'Processando…' : 'Vincular Serviço' }}
      </button>
      <button class="botao botao-escuro" @click="irParaCadastro('Código de Serviço', '')">
        Cadastrar Novo Item
      </button>
      <button class="botao botao-escuro" @click="limpar">Limpar</button>
    </div>

    <!-- confirmação de composição -->
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

          <!-- alertas por componente (mesma regra anti-duplicidade em todos os campos) -->
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
            <span class="match" :class="{ alerta: a.score < 0.88 }">{{ (a.score * 100).toFixed(0) }}% Match</span>
          </div>
        </div>
        <div class="linha-botoes">
          <button class="botao botao-escuro" @click="modalPreview = false">Voltar</button>
          <button class="botao botao-roxo" @click="confirmarVinculo">Confirmar</button>
        </div>
      </div>
    </div>

    <!-- Bloqueio de Duplicidade (print 3) — apenas em criação de item novo -->
    <ModalBloqueio
      v-if="modalBloqueio"
      :itens="itensBloqueio"
      :motivo="motivoBloqueio"
      @reutilizar="reutilizarItem"
      @governanca="abrirGovernanca"
      @voltar="modalBloqueio = false"
    />

    <!-- Envio para Governança (print 4) -->
    <ModalGovernanca
      v-if="modalGovernanca"
      classe="Código de Serviço"
      :descricao="descricaoNovoServico"
      @voltar="modalGovernanca = false"
      @confirmar="confirmarGovernanca"
    />

    <!-- exclusão de componente do catálogo (ADMIN) -->
    <ModalConfirmacao
      v-if="exclusao"
      titulo="Excluir componente do catálogo?"
      :mensagem="`${ROTULO_TIPO[exclusao.tipo]} “${exclusao.rotulo}” será excluído definitivamente. A ação fica registrada na auditoria.`"
      rotulo-confirmar="✕ Excluir"
      :acao-perigosa="true"
      @confirmar="executarExclusao"
      @cancelar="exclusao = null"
    />

    <!-- sucesso (fecha automaticamente com barra de tempo) -->
    <div v-if="sucesso" class="modal-fundo">
      <div class="modal">
        <div class="aviso aviso-verde">
          <span class="icone">✓</span>
          <div><strong>{{ sucesso.titulo }}</strong><br />{{ sucesso.mensagem }}</div>
        </div>
        <div class="barra-tempo">
          <div class="barra-tempo-fill" :style="{ width: tempoRestante + '%' }"></div>
        </div>
        <div class="suave centralizado" style="margin-top: 8px; font-size: 0.82rem">
          Fecha automaticamente em {{ Math.ceil(tempoRestante / 100 * 8) }}s…
        </div>
        <div class="linha-botoes" style="margin-top: 14px">
          <button class="botao botao-roxo" @click="limpar">Novo registro</button>
          <button v-if="isAdmin" class="botao botao-escuro" @click="$router.push('/admin')">Ver Painel Admin</button>
        </div>
      </div>
    </div>

    <!-- ===== conteúdo contextual por perfil ===== -->
    <!-- ADMIN: resumo da base -->
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

    <!-- USUÁRIO: guia rápido do fluxo (inicia fechado) -->
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
            A busca entende <strong>acentos ausentes</strong>, pequenos erros de digitação e
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
import type { CandidatoSimilar, CriarCodigoCompletoPayload, PreviewResponse } from '../types'

type Item = Record<string, unknown>

/** Candidato do modal de bloqueio — carrega o tipo do componente quando o item é de outro catálogo. */
type ItemBloqueio = CandidatoSimilar & { tipoComponente?: string }

/** Componentes do catálogo que o ADMIN pode excluir. */
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

// ---------- exclusão de componentes (apenas ADMIN — auditada; bloqueada se houver vínculos) ----------
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
    await api.componentes.excluir(alvo.tipo, alvo.id)
    toast.sucesso(`${ROTULO_TIPO[alvo.tipo]} “${alvo.rotulo}” excluído com sucesso.`)
    const selecoes: Record<TipoComponenteCatalogo, { value: Item | null }> = { GS: gs, CF: cf, CS: cs, UM: um }
    const paineis: Record<TipoComponenteCatalogo, { recarregar: () => void } | null> = {
      GS: cbGs.value, CF: cbCf.value, CS: cbCs.value, UM: cbUm.value
    }
    // se o item excluído estava selecionado no painel, a seleção é limpa
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
const tempoRestante = ref(100)

// balão de sucesso: fecha sozinho com barra de tempo regressiva
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
      if (sucesso.value) limpar()
    }
  }, 160)
})

const completo = computed(() => gs.value && cf.value && cs.value && um.value)
const descricaoNovoServico = computed(() => preview.value?.codigoServico?.nome || '')

// SEMPRE há pelo menos uma opção para o usuário escolher
const itensBloqueio = computed<ItemBloqueio[]>(() => {
  const p = preview.value
  if (!p) return []

  // itens semelhantes encontrados + alertas de componentes novos
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

/** Nome completo do componente (sem abreviações). */
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
    if (preview.value.idCompletoJaExiste) {
      // vínculo é livre: composição já registrada = ação confirmada usando o registro existente
      registrarVinculo(preview.value.idCompleto, preview.value.descricaoSnapshot)
    } else if (preview.value.decisao === 'PERMITIR') {
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

/** Confirmação de vínculo (novo ou já existente) — feedback de sucesso igual nos dois casos. */
function registrarVinculo(idCompleto: string, descricaoSnapshot: string): void {
  toast.sucesso(`Enviado com sucesso! Serviço ${idCompleto} vinculado.`)
  sucesso.value = { titulo: `Serviço ${idCompleto} vinculado!`, mensagem: descricaoSnapshot }
  atualizarPendentes()
}

async function confirmarVinculo(): Promise<void> {
  processando.value = true
  try {
    const r = await api.criarCodigoCompleto(payload({ acao: 'CRIAR' }))
    modalPreview.value = false
    registrarVinculo(r.idCompleto, r.descricaoSnapshot)
  } catch (e) {
    if ((e as { status?: number }).status === 409 && preview.value) {
      // composição já registrada (ex.: criada entre a prévia e a confirmação) — vínculo é livre
      modalPreview.value = false
      registrarVinculo(preview.value.idCompleto, preview.value.descricaoSnapshot)
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

  // item semelhante escolhido — assume o painel do componente correspondente
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
  // refaz a prévia com o item existente — o vínculo é gravado na confirmação
  void vincular()
}

function abrirGovernanca(): void {
  modalBloqueio.value = false
  modalGovernanca.value = true
}

async function confirmarGovernanca(justificativa: string): Promise<void> {
  processando.value = true
  try {
    const corpo = payload({ acao: 'ENVIAR_VALIDACAO', justificativa })
    const r = await api.criarCodigoCompleto(
      corpo,
      chaveIdempotente('gov-', corpo.gsId, corpo.cfId, corpo.csId, corpo.umId, justificativa)
    )
    modalGovernanca.value = false
    toast.sucesso('Enviado com sucesso! O cadastro foi para a fila de validação da Governança.')
    sucesso.value = {
      titulo: 'Enviado para Governança',
      mensagem: `O cadastro ${r.idCompleto} passará por validação humana pela Equipe de Governança de Dados.`
    }
    atualizarPendentes()
  } catch (e) {
    toast.erro((e as Error).message)
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
}

/**
 * Item reutilizado vindo do "Cadastrar Novo Item" (?reutilizar=<id>): seleciona o
 * item existente no painel correspondente e avisa o usuário (toast).
 */
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
  // limpa a query para o refresh não repetir a seleção/toast
  void router.replace({ query: {} })
}

onMounted(() => {
  carregarResumoAdmin()
  aplicarReutilizacao()
})
watch(() => props.papel, carregarResumoAdmin)
</script>


