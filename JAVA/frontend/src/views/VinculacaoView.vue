<template>
  <div>
    <!-- 4 painéis de seleção (print 1) -->
    <section class="grade-seletores">
      <div class="painel">
        <label class="painel-rotulo">Grupo de Serviço</label>
        <ComboboxBusca
          placeholder="Digite para buscar…"
          :carregar="api.gruposServico.listar"
          :nomeDe="(i: any) => String(i.nome)"
          :idDe="() => ''"
          v-model="gs"
          @criarNovo="(t: string) => irParaCadastro('Grupo de Serviço', t)"
        />
      </div>

      <div class="painel">
        <label class="painel-rotulo">Código de Fornecedor</label>
        <ComboboxBusca
          placeholder="Digite para buscar…"
          :carregar="api.codigosFornecedor.listar"
          :nomeDe="(i: any) => String(i.nome)"
          :idDe="(i: any) => String(i.codigoFornecedorSAP || i.id)"
          v-model="cf"
          @criarNovo="(t: string) => irParaCadastro('Código de Fornecedor', t)"
        />
      </div>

      <div class="painel">
        <label class="painel-rotulo">Código de Serviço</label>
        <ComboboxBusca
          placeholder="Digite para buscar…"
          :carregar="api.codigosServico.listar"
          :nomeDe="(i: any) => String(i.descricao)"
          :idDe="(i: any) => String(i.id)"
          :sapDe="(i: any) => `${i.quantidadeCaracteres ?? String(i.descricao || '').length}/40 SAP`"
          v-model="cs"
          @criarNovo="(t: string) => irParaCadastro('Código de Serviço', t)"
        />
      </div>

      <div class="painel">
        <label class="painel-rotulo">Unidade de Medida</label>
        <ComboboxBusca
          placeholder="Digite para buscar…"
          :carregar="api.unidadesMedida.listar"
          :nomeDe="(i: any) => `${i.sigla} - ${i.nome}`"
          :idDe="() => ''"
          v-model="um"
          @criarNovo="(t: string) => irParaCadastro('Unidade de Medida', t)"
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

    <!-- vínculo já existe: aviso simples (sem modal de duplicidade) -->
    <div v-if="vinculoExistente" class="modal-fundo" @click.self="vinculoExistente = false">
      <div class="modal">
        <h3>Este vínculo já existe</h3>
        <div class="aviso aviso-azul">
          <span class="icone">ℹ</span>
          <div>
            A composição <strong>{{ preview?.idCompleto }}</strong> já está registrada.<br />
            <span class="suave">{{ preview?.descricaoSnapshot }}</span>
          </div>
        </div>
        <div class="linha-botoes">
          <button class="botao botao-escuro" @click="vinculoExistente = false">Voltar</button>
          <button class="botao botao-roxo" @click="vinculoExistente = false; limpar()">Novo registro</button>
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
          <button class="botao botao-escuro" @click="$router.push('/admin')">Ver Painel Admin</button>
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
import { useRouter } from 'vue-router'
import { api } from '../services/api'
import { toast } from '../services/toast'
import ComboboxBusca from '../components/ComboboxBusca.vue'
import ModalBloqueio from '../components/ModalBloqueio.vue'
import ModalGovernanca from '../components/ModalGovernanca.vue'
import type { CandidatoSimilar, CriarCodigoCompletoPayload, PreviewResponse } from '../types'

type Item = Record<string, unknown>

const props = withDefaults(defineProps<{ usuario?: string; papel?: string }>(), { usuario: '', papel: 'USUARIO' })
const router = useRouter()
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

const preview = ref<PreviewResponse | null>(null)
const processando = ref(false)
const modalPreview = ref(false)
const modalBloqueio = ref(false)
const modalGovernanca = ref(false)
const vinculoExistente = ref(false)
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
  }, 160) // 8 segundos no total
})

const completo = computed(() => gs.value && cf.value && cs.value && um.value)
const descricaoNovoServico = computed(() => preview.value?.codigoServico?.nome || '')

// SEMPRE há pelo menos uma opção para o usuário escolher
const itensBloqueio = computed<CandidatoSimilar[]>(() => {
  const p = preview.value
  if (!p) return []

  // caso 1: a composição inteira já existe — o próprio registro é a opção
  if (p.idCompletoJaExiste) {
    return [{
      posicao: 1,
      id: p.idCompleto,
      codigoOriginal: null,
      descricao: p.descricaoSnapshot,
      score: 1,
      scoreTokens: 0,
      scoreCaracteres: 0,
      tokensEmComum: [],
      motivo: 'Este código completo (Grupo de Serviço + Código de Fornecedor + Código de Serviço + Unidade de Medida) já está cadastrado.'
    }]
  }

  // caso 2: itens semelhantes encontrados + alertas de componentes novos
  const lista: CandidatoSimilar[] = [...(p.similares || [])]
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
      motivo: `${nomeComponente(a.tipo)}: “${a.descricaoInformada}” é semelhante a este item.`
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
  preview.value?.idCompletoJaExiste
    ? 'Esta composição já existe na base. Reutilize o registro existente ou envie para governança.'
    : 'O sistema identificou itens semelhantes cadastrados. Escolha um item para reutilizar ou justifique a criação.')

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
      // vínculo repetido: aviso simples e direto (o alerta de duplicidade é do cadastro)
      vinculoExistente.value = true
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

async function confirmarVinculo(): Promise<void> {
  processando.value = true
  try {
    const r = await api.criarCodigoCompleto(payload({ acao: 'CRIAR' }))
    modalPreview.value = false
    sucesso.value = { titulo: `Serviço ${r.idCompleto} vinculado!`, mensagem: r.descricaoSnapshot }
    atualizarPendentes()
  } catch (e) {
    if ((e as { status?: number }).status === 409) {
      modalPreview.value = false
      modalBloqueio.value = true
    } else {
      toast.erro((e as Error).message)
    }
  } finally {
    processando.value = false
  }
}

function reutilizarItem(item: CandidatoSimilar): void {
  modalBloqueio.value = false
  const p = preview.value

  // opção: a própria composição existente — mantém os 4 componentes já selecionados
  if (p?.idCompletoJaExiste && item.id === p.idCompleto) {
    sucesso.value = {
      titulo: 'Registro existente em uso',
      mensagem: `O código completo ${item.id} já estava cadastrado — ele será usado como está.`
    }
    return
  }

  // opção: item semelhante escolhido — assume o C.S. (ou o componente correspondente)
  cs.value = { id: item.id, descricao: item.descricao }
  sucesso.value = {
    titulo: 'Código existente reutilizado',
    mensagem: `${item.id} - ${item.descricao} selecionado como Código de Serviço.`
  }
}

function abrirGovernanca(): void {
  modalBloqueio.value = false
  modalGovernanca.value = true
}

async function confirmarGovernanca(justificativa: string): Promise<void> {
  processando.value = true
  try {
    const r = await api.criarCodigoCompleto(payload({ acao: 'ENVIAR_VALIDACAO', justificativa }), 'gov-' + Date.now())
    modalGovernanca.value = false
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
  vinculoExistente.value = false
  modalPreview.value = modalBloqueio.value = modalGovernanca.value = false
  toast.sucesso('Pronto para um novo registro — selecione os componentes.')
}

onMounted(carregarResumoAdmin)
watch(() => props.papel, carregarResumoAdmin)
</script>


