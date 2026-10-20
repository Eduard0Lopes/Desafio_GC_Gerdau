<template>
  <div>
    <p class="suave" style="margin: 0 0 22px">
      Itens sinalizados como repetição no histórico e novos cadastros enviados para validação.
      <strong>Clique nos cartões</strong> para filtrar a fila.
    </p>

    
    <div class="indicadores">
      <div class="indicador" :class="{ ativo: filtro === 'PENDENTES' }" @click="alternarFiltro('PENDENTES')">
        <div class="rotulo">Aguardando revisão</div>
        <div class="valor" style="color: var(--amarelo-alerta)">{{ resumo.pendentesHistorico ?? '…' }}</div>
      </div>
      <div class="indicador" :class="{ ativo: filtro === 'DUPLICIDADE' }" @click="alternarFiltro('DUPLICIDADE')">
        <div class="rotulo">Possíveis repetidos</div>
        <div class="valor" style="color: var(--vermelho)">{{ resumo.decisionDuplicidade ?? '…' }}</div>
      </div>
      <div class="indicador" :class="{ ativo: filtro === 'ALERTA' }" @click="alternarFiltro('ALERTA')">
        <div class="rotulo">Em atenção</div>
        <div class="valor" style="color: var(--amarelo-alerta)">{{ resumo.decisionAlerta ?? '…' }}</div>
      </div>
      <div class="indicador" :class="{ ativo: filtro === 'REVISADOS' }" @click="alternarFiltro('REVISADOS')">
        <div class="rotulo">Revisados</div>
        <div class="valor" style="color: var(--verde)">{{ resumo.revisados ?? '…' }}</div>
      </div>
      <div class="indicador" :class="{ ativo: filtro === 'NOVOS' }" @click="alternarFiltro('NOVOS')">
        <div class="rotulo">Novos cadastros</div>
        <div class="valor">{{ resumo.pendentesNovos ?? '…' }}</div>
      </div>
    </div>

    
    <div class="linha-campo" style="margin-bottom: 18px">
      <button
        class="botao"
        :class="origem === 'HISTORICO' ? 'botao-roxo' : 'botao-escuro'"
        @click="trocarOrigem('HISTORICO')"
      >
        Base original
      </button>
      <button
        class="botao"
        :class="origem === 'NOVOS' ? 'botao-roxo' : 'botao-escuro'"
        @click="trocarOrigem('NOVOS')"
      >
        Novos cadastros
      </button>
      <span v-if="filtro !== 'TODOS'" class="badge badge-alertar">
        filtro: {{ rotuloFiltro }}
        <span style="cursor: pointer; margin-left: 6px" @click="alternarFiltro('TODOS')">✕</span>
      </span>
      <span style="flex: 1"></span>
      <div class="campo-busca-wrap" style="min-width: 260px">
        <input
          v-model="busca"
          placeholder="Buscar por código, descrição…"
          style="background: var(--campo); border: 1px solid var(--borda); border-radius: 9px;
                 color: var(--texto); padding: 9px 12px; width: 100%"
          @input="carregarComDebounce"
        />
        <button v-if="busca" type="button" class="btn-limpar-busca" title="Limpar busca" @click="busca = ''; carregar()">✕</button>
      </div>
    </div>

    <div v-if="!itensFiltrados.length" class="aviso aviso-verde">
      <span class="icone">✓</span>
      <div>Nenhum item {{ rotuloFiltro === 'revisados' ? 'revisado' : 'aguardando decisão' }} neste filtro.</div>
    </div>

    
    <div class="grade-cards">
      <div v-for="item in itensFiltrados" :key="item.origem + item.id" class="painel card-item">

        
        <div class="card-cabecalho">
          <span class="badge" :class="item.origem === 'HISTORICO' ? 'badge-alertar' : 'badge-permitir'">
            {{ item.origem === 'HISTORICO' ? 'BASE ORIGINAL' : 'NOVO CADASTRO' }}
          </span>
          <span class="card-identificador">{{ item.identificador }}</span>
          <span
            v-if="item.casoIsolado"
            class="badge-excecao"
            :title="'Caso especial registrado.' + (item.motivoCasoIsolado ? ' Motivo: ' + item.motivoCasoIsolado : '')"
          >!</span>
          <span class="match" :class="{ alerta: item.score < 0.88 }">
            {{ (item.score * 100).toFixed(0) }}% Match
          </span>
        </div>
        <div class="card-descricao">{{ item.descricao }}</div>

        
        <div class="card-campos">
          <div class="campo">
            <span class="rotulo">Grupo de Serviço</span>
            <span class="valor">{{ item.grupoServico || '—' }}</span>
          </div>
          <div class="campo">
            <span class="rotulo">Código de Fornecedor</span>
            <span class="valor">{{ item.fornecedor || '—' }}</span>
          </div>
          <div class="campo">
            <span class="rotulo">Unidade de Medida</span>
            <span class="valor">{{ item.unidadeMedida || '—' }}</span>
          </div>
          <div class="campo">
            <span class="rotulo">Código antigo</span>
            <span class="valor">{{ item.legacyId || '—' }}</span>
          </div>
          <div class="campo">
            <span class="rotulo">Natureza do serviço</span>
            <span class="valor">{{ item.natureza || '—' }}</span>
          </div>
          <div class="campo campo-largo">
            <span class="rotulo">Avaliação automática</span>
            <span class="valor">
              <strong>{{ item.decision }}</strong> — {{ item.reason }}
            </span>
          </div>
          <div v-if="item.solicitante" class="campo">
            <span class="rotulo">Solicitante</span>
            <span class="valor">{{ item.solicitante }}</span>
          </div>
        </div>

        
        <div v-if="item.justificativa" class="justificativa-destaque">
          <div class="rotulo">Justificativa do solicitante</div>
          <blockquote>“{{ item.justificativa }}”</blockquote>
          <div v-if="item.solicitante" class="autoria">
            enviada por <strong>{{ item.solicitante }}</strong>
          </div>
        </div>

        
        <div v-if="item.similares && item.similares.length" class="nao-escolhidos">
          <div class="nao-escolhidos-cabecalho">
            <strong>O que o usuário não escolheu</strong>
            <span class="suave">
              — matches do bloqueio de duplicidade que ele ignorou ao enviar para a Governança.
              <template v-if="podeDecidir(item)">Se algum deles atende, escolha e siga por ele.</template>
            </span>
          </div>
          <div class="lista-selecao">
            <div
              v-for="s in item.similares.slice(0, 5)"
              :key="chaveEscolha(s)"
              class="opcao-item"
              :class="{ selecionada: escolhidos[item.id] === chaveEscolha(s), clicavel: podeDecidir(item) }"
              @click="escolherMatch(item, s)"
            >
              <input
                v-if="podeDecidir(item)"
                type="radio"
                :name="`match-${item.id}`"
                :checked="escolhidos[item.id] === chaveEscolha(s)"
                @change="escolherMatch(item, s)"
              />
              <div class="opcao-conteudo">
                <div class="opcao-linha">
                  <span class="opcao-id">{{ s.id }}</span>
                  <span class="badge badge-permitir">{{ rotuloTipoMatch(s.tipo) }}</span>
                  <span class="suave">|</span>
                  <strong>{{ (s.descricao || '').toUpperCase() }}</strong>
                  <span
                    v-if="s.casoIsolado"
                    class="badge-excecao"
                    :title="'Caso especial registrado.' + (s.motivoCasoIsolado ? ' Motivo: ' + s.motivoCasoIsolado : '')"
                  >!</span>
                  <span class="match" :class="{ alerta: s.score < 0.88 }">{{ (s.score * 100).toFixed(0) }}% Match</span>
                </div>
                <div v-if="s.motivo" class="opcao-motivo">{{ s.motivo }}</div>
              </div>
            </div>
          </div>
        </div>

        
        <div class="linha-campo" style="margin-top: 14px">
          <textarea
            v-model="observacoes[item.id]"
            placeholder="Motivo da decisão (obrigatório)"
            style="min-height: 58px"
          ></textarea>
        </div>

        
        <div v-if="item.origem === 'HISTORICO' && item.status === 'REVISADO'" class="aviso aviso-verde" style="margin: 12px 0 0">
          <span class="icone">✓</span>
          <div>
            Decisão registrada por <strong>{{ item.solicitante || 'governança' }}</strong>.
            <span v-if="item.justificativa" class="suave"> — {{ item.justificativa }}</span>
          </div>
        </div>

        <div v-if="podeDecidir(item) && item.origem === 'HISTORICO'" class="linha-botoes" style="margin-top: 12px">
          
            <button class="botao botao-verde" :disabled="processando === item.id" @click="pedirConfirmacao(item, 'manter')">
              ✓ Aceitar
            </button>
            <button class="botao botao-vermelho" :disabled="processando === item.id" @click="pedirConfirmacao(item, 'excluir')">
              ✕ Apagar
            </button>
        </div>
        <div v-else-if="podeDecidir(item)" class="linha-botoes" style="margin-top: 12px">
            <button
              v-if="item.similares && item.similares.length"
              class="botao botao-roxo"
              :disabled="processando === item.id || !escolhidos[item.id]"
              :title="escolhidos[item.id] ? 'O pedido segue reutilizando o código escolhido' : 'Escolha um dos matches acima'"
              @click="pedirReutilizacao(item)"
            >
              ⇄ Reutilizar escolhido
            </button>
            <button class="botao botao-verde" :disabled="processando === item.id" @click="pedirConfirmacao(item, 'aprovar')">
              ✓ Aprovar cadastro
            </button>
            <button class="botao botao-vermelho" :disabled="processando === item.id" @click="pedirConfirmacao(item, 'rejeitar')">
              ✕ Rejeitar cadastro
            </button>
        </div>
      </div>
    </div>

    
    <div class="paginacao" style="margin-top: 20px">
      <button class="pulo" :disabled="pagina === 0" @click="irParaPagina(0)" title="Início">|‹</button>
      <button class="pulo" :disabled="pagina === 0" @click="paginaAnterior" title="Página anterior">‹</button>
      <span class="info"><strong>{{ pagina + 1 }}</strong> / {{ totalPaginas }}</span>
      <button class="pulo" :disabled="!temMais" @click="proximaPagina" title="Próxima página">›</button>
      <button class="pulo" :disabled="!temMais" @click="pular(10)" title="Avançar 10 páginas (ou ir ao fim)">››</button>
      <button class="pulo" :disabled="pagina + 1 >= totalPaginas" @click="irParaPagina(totalPaginas - 1)" title="Fim">›|</button>
    </div>

    
    <ModalConfirmacao
      v-if="confirmacao"
      :titulo="confirmacao.titulo"
      :mensagem="confirmacao.mensagem"
      :rotulo-confirmar="confirmacao.rotulo"
      :acao-perigosa="confirmacao.perigosa"
      @confirmar="executarConfirmacao"
      @cancelar="confirmacao = null"
    />

    <ModalConfirmacao
      v-if="reutilizacao"
      titulo="Reutilizar o código escolhido?"
      :mensagem="`O pedido ${reutilizacao.item.identificador} será resolvido reutilizando ${reutilizacao.candidato.id} — ${reutilizacao.candidato.descricao}. A composição passa a apontar para este código, o código novo do pedido não entra no catálogo e a decisão fica registrada na auditoria.`"
      rotulo-confirmar="⇄ Reutilizar e seguir"
      :acao-perigosa="false"
      @confirmar="executarReutilizacao"
      @cancelar="reutilizacao = null"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, inject, onMounted, reactive, ref } from 'vue'
import { api } from '../services/api'
import { toast } from '../services/toast'
import ModalConfirmacao from '../components/ModalConfirmacao.vue'
import type { CandidatoSimilar, ItemGovernanca, ResumoGovernanca } from '../types'

interface GrupoUso { nome: string; quantidade: number }
interface ResumoCompleto extends Partial<ResumoGovernanca> {
  grupos?: GrupoUso[]
  totalLegados?: number
}

const props = withDefaults(defineProps<{ usuario?: string }>(), { usuario: '' })

// decisões são da governança: só ADMIN, e só enquanto o item está pendente
const isAdmin = computed(() => (localStorage.getItem('papel') || 'USUARIO').toUpperCase() === 'ADMIN')
function podeDecidir(item: ItemGovernanca): boolean {
  if (!isAdmin.value) return false
  return item.origem === 'HISTORICO'
    ? item.status === 'PENDENTE'
    : item.status === 'PENDENTE_VALIDACAO'
}
const emit = defineEmits<{
  (e: 'pendentes', n: number): void
  (e: 'api-fora', v: boolean): void
}>()
const atualizarPendentes = inject<() => void>('atualizarPendentes', () => {})

const resumo = ref<ResumoCompleto>({})
const itens = ref<ItemGovernanca[]>([])
const observacoes = reactive<Record<string, string>>({})
const processando = ref<string | null>(null)
const origem = ref('HISTORICO')
const busca = ref('')
const filtro = ref<string>('TODOS')
const pagina = ref(0)
const tamanhoPagina = 4
const temMais = ref(false)
const totalFila = ref(0)
const totalPaginas = computed(() => Math.max(1, Math.ceil(totalFila.value / tamanhoPagina)))
const confirmacao = ref<{ titulo: string; mensagem: string; rotulo: string; perigosa: boolean; acao: string; item: ItemGovernanca } | null>(null)

/* ============================================================================
   O QUE O USUÁRIO NÃO ESCOLHEU — o radio do bloqueio de duplicidade que foi
   ignorado ao enviar para a Governança. Quem decide vê os matches e pode
   escolher um deles e seguir: o pedido é resolvido reutilizando o código
   existente, em vez de criar outro igual.
   ========================================================================== */
const escolhidos = reactive<Record<string, string>>({})
const reutilizacao = ref<{ item: ItemGovernanca; candidato: CandidatoSimilar } | null>(null)

const TIPOS_MATCH: Record<string, string> = { CS: 'Serviço', GS: 'Grupo', CF: 'Fornecedor', UM: 'Unidade' }

function rotuloTipoMatch(tipo?: string | null): string {
  return TIPOS_MATCH[(tipo || 'CS').toUpperCase()] || 'Serviço'
}

function chaveEscolha(s: CandidatoSimilar): string {
  return `${(s.tipo || 'CS').toUpperCase()}:${s.id}`
}

function escolherMatch(item: ItemGovernanca, s: CandidatoSimilar): void {
  if (!podeDecidir(item)) return
  escolhidos[item.id] = chaveEscolha(s)
}

function pedirReutilizacao(item: ItemGovernanca): void {
  const chave = escolhidos[item.id]
  const candidato = (item.similares || []).find((s) => chaveEscolha(s) === chave)
  if (!candidato) {
    toast.aviso('Escolha um dos matches acima para reutilizar.')
    return
  }
  reutilizacao.value = { item, candidato }
}

async function executarReutilizacao(): Promise<void> {
  const r = reutilizacao.value
  if (!r || processando.value === r.item.id) return
  reutilizacao.value = null
  processando.value = r.item.id
  try {
    await api.reutilizarNovo(r.item.id, {
      escolhidoId: r.candidato.id,
      escolhidoTipo: r.candidato.tipo || 'CS',
      aprovadoPor: props.usuario || 'governanca',
      observacao: (observacoes[r.item.id] || '').trim() || null
    })
    toast.sucesso(
      `Pedido ${r.item.identificador} resolvido reutilizando ${r.candidato.id} — ${r.candidato.descricao}.`
    )
    itens.value = itens.value.filter((i) => i.id !== r.item.id)
    await carregar()
    atualizarPendentes()
  } catch (e) {
    toast.erro((e as Error).message)
  } finally {
    processando.value = null
  }
}

const rotuloFiltro = computed(() => ({
  PENDENTES: 'aguardando revisão', DUPLICIDADE: 'possíveis repetidos', ALERTA: 'em atenção',
  REVISADOS: 'revisados', NOVOS: 'novos cadastros'
} as Record<string, string>)[filtro.value] || filtro.value)

const itensFiltrados = computed(() => {
  switch (filtro.value) {
    case 'DUPLICIDADE':
    case 'ALERTA':
      return itens.value.filter((i) => i.decision === filtro.value)
    case 'REVISADOS': return itens.value.filter((i) => i.status === 'REVISADO')
    case 'PENDENTES': return itens.value.filter((i) => i.status === 'PENDENTE')
    default: return itens.value
  }
})

function alternarFiltro(f: string): void {
  filtro.value = filtro.value === f || f === 'TODOS' ? 'TODOS' : f
  pagina.value = 0
  if (f === 'NOVOS') {
    filtro.value = 'TODOS'
    origem.value = 'NOVOS'
    carregar()
    return
  }
  if (f !== 'TODOS' && origem.value !== 'HISTORICO') {
    origem.value = 'HISTORICO'
  }
  carregar()
}

function proximaPagina(): void {
  if (!temMais.value) return
  pagina.value++
  carregar()
}

function paginaAnterior(): void {
  if (pagina.value === 0) return
  pagina.value--
  carregar()
}

function pular(n: number): void {
  pagina.value = Math.min(pagina.value + n, totalPaginas.value - 1)
  carregar()
}

function irParaPagina(n: number): void {
  pagina.value = Math.max(0, Math.min(n, totalPaginas.value - 1))
  carregar()
}

async function carregar(): Promise<void> {
  try {
    resumo.value = (await api.resumo()) as ResumoCompleto
    const statusServidor =
      filtro.value === 'REVISADOS' ? 'REVISADO'
      : filtro.value === 'SEM_PENDENCIA' ? 'SEM_PENDENCIA'
      
      : filtro.value === 'DUPLICIDADE' || filtro.value === 'ALERTA' ? 'TODOS'
      : 'PENDENTE'
    const decisaoServidor =
      filtro.value === 'DUPLICIDADE' ? 'DUPLICIDADE'
      : filtro.value === 'ALERTA' ? 'ALERTA'
      : undefined
    const lote = await api.pendentes(origem.value, busca.value, tamanhoPagina, statusServidor, decisaoServidor, pagina.value)
    itens.value = lote
    temMais.value = lote.length === tamanhoPagina
    const contagem = await api.contarFila(origem.value, statusServidor, decisaoServidor, busca.value)
    totalFila.value = contagem.total
    emit('pendentes', (resumo.value.pendentesHistorico || 0) + (resumo.value.pendentesNovos || 0))
    emit('api-fora', false)
  } catch {
    emit('api-fora', true)
  }
}

function trocarOrigem(o: string): void {
  origem.value = o
  pagina.value = 0
  carregar()
}

let timer: ReturnType<typeof setTimeout> | null = null
function carregarComDebounce(): void {
  if (timer) clearTimeout(timer)
  timer = setTimeout(carregar, 350)
}

function pedirConfirmacao(item: ItemGovernanca, acao: string): void {
  const ehDuplicidade = item.decision === 'DUPLICIDADE'
  const textos: Record<string, { titulo: string; mensagem: string; rotulo: string; perigosa: boolean }> = {
    manter: {
      titulo: 'Aceitar este item?',
      mensagem: `O item ${item.identificador} será aceito como válido${ehDuplicidade
        ? ' — a possível repetição está aceitável' : ''}. O motivo informado fica registrado na auditoria.`,
      rotulo: '✓ Aceitar', perigosa: false
    },
    excluir: {
      titulo: 'Apagar do catálogo?',
      mensagem: `O item ${item.identificador} será apagado da lista ativa (o registro original continua preservado no histórico). O motivo informado fica registrado na auditoria.`,
      rotulo: '✕ Apagar', perigosa: true
    },
    aprovar: {
      titulo: 'Aprovar cadastro?',
      mensagem: `O cadastro ${item.identificador} será aprovado e ativado. O motivo informado fica registrado na auditoria.`,
      rotulo: '✓ Aprovar', perigosa: false
    },
    rejeitar: {
      titulo: 'Rejeitar cadastro?',
      mensagem: `O cadastro ${item.identificador} será rejeitado. O motivo informado fica registrado na auditoria.`,
      rotulo: '✕ Rejeitar', perigosa: true
    }
  }
  confirmacao.value = { ...textos[acao], acao, item }
}


const ACAO_HISTORICO: Record<string, { acao: string; passado: string }> = {
  manter: { acao: 'MANTER', passado: 'aceito' },
  excluir: { acao: 'EXCLUIR', passado: 'apagado' }
}

async function executarConfirmacao(): Promise<void> {
  const c = confirmacao.value
  if (!c) return
  const { item, acao } = c

  
  const motivo = (observacoes[item.id] || '').trim()
  if (!motivo) {
    toast.aviso('Informe o motivo da decisão — ele é obrigatório.')
    return
  }
  confirmacao.value = null
  processando.value = item.id
  try {
    const historico = ACAO_HISTORICO[acao]
    if (historico) {
      await api.revisarHistorico(item.id, {
        revisadoPor: props.usuario || 'governanca',
        acao: historico.acao,
        observacao: motivo
      })
      toast.sucesso(`Item ${item.identificador} ${historico.passado} — decisão registrada.`)
    } else if (acao === 'aprovar') {
      await api.aprovar(item.id, { aprovadoPor: props.usuario || 'governanca', observacao: motivo })
      toast.sucesso(`Cadastro ${item.identificador} aprovado.`)
    } else {
      await api.rejeitar(item.id, { aprovadoPor: props.usuario || 'governanca', observacao: motivo })
      toast.sucesso(`Cadastro ${item.identificador} rejeitado.`)
    }
    // a decisão sai da fila imediatamente — o card não fica na tela
    itens.value = itens.value.filter((i) => i.id !== item.id)
    await carregar()
    atualizarPendentes()
  } catch (e) {
    toast.erro((e as Error).message)
  } finally {
    processando.value = null
  }
}

onMounted(carregar)
</script>

<style scoped>
.grade-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(460px, 1fr));
  gap: 16px;
}
.card-item {
  display: flex;
  flex-direction: column;
}
.card-cabecalho {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}
.card-identificador {
  font-size: 0.76rem; font-weight: 600; letter-spacing: 0.04em;
  color: var(--texto-suave); opacity: 0.8;
  font-variant-numeric: tabular-nums;
}
.card-descricao {
  font-size: 1.04rem; font-weight: 700;
  line-height: 1.5;
  margin-bottom: 14px;
}
.card-campos {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px 18px;
}
.card-campos .campo {
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.card-campos .campo-largo {
  grid-column: 1 / -1;
}
.card-campos .rotulo {
  font-size: 0.72rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--texto-suave);
  font-weight: 700;
}
.card-campos .valor {
  font-size: 0.95rem;
  line-height: 1.5;
}
.nao-escolhidos {
  margin-top: 12px;
  border: 1px dashed var(--borda);
  border-radius: 10px;
  padding: 12px 14px;
}
.nao-escolhidos-cabecalho {
  font-size: 0.88rem;
  margin-bottom: 10px;
  line-height: 1.5;
}
.lista-selecao {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.opcao-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  background: var(--fundo);
  border: 1.5px solid var(--borda);
  border-radius: 10px;
  padding: 11px 14px;
  transition: all 0.12s;
}
.opcao-item.clicavel {
  cursor: pointer;
}
.opcao-item.clicavel:hover {
  border-color: var(--azul);
}
.opcao-item.selecionada {
  border-color: var(--azul);
  background: var(--azul-claro);
}
.opcao-item input[type='radio'] {
  margin-top: 3px;
  accent-color: var(--azul);
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}
.opcao-conteudo {
  flex: 1;
  min-width: 0;
}
.opcao-linha {
  display: flex;
  align-items: baseline;
  gap: 10px;
  flex-wrap: wrap;
  font-variant-numeric: tabular-nums;
}
.opcao-id {
  font-size: 0.72rem; font-weight: 600; letter-spacing: 0.04em;
  color: var(--texto-suave); opacity: 0.75;
}
.opcao-motivo {
  margin-top: 5px;
  color: var(--texto-suave);
  font-size: 0.84rem;
}
@media (max-width: 1020px) {
  .grade-cards {
    grid-template-columns: 1fr;
  }
}
</style>
