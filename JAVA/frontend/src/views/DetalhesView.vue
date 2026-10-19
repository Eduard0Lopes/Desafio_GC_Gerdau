<template>
  <div>
    <div class="linha" style="margin-bottom: 18px">
      <div>
        <h2 style="margin: 0; font-size: 1.35rem; color: var(--azul-escuro)">Detalhes da base</h2>
        <p class="suave" style="margin: 6px 0 0">
          Panorama completo do catálogo e da governança.
          <strong>Clique em um gráfico</strong> para ocultar/mostrar categorias (a legenda traz de volta).
        </p>
      </div>
      <span class="espacador" style="flex: 1"></span>
      <button class="botao botao-escuro" @click="exportarResumo">⤓ Exportar resumo (CSV)</button>
      <button class="botao botao-escuro" @click="exportarDetalhamento">⤓ Exportar detalhamento (CSV)</button>
      <button class="botao botao-roxo" @click="exportarDecisoes">⤓ Exportar decisões (CSV)</button>
    </div>

    
    <div class="painel destaque-servicos">
      <div class="destaque-topo">
        <div>
          <div class="rotulo-destaque">Códigos de Serviço</div>
          <div class="numero-destaque">{{ est.catalogo?.servicos?.toLocaleString('pt-BR') ?? '…' }}</div>
          <div class="suave" style="margin-top: 6px">
            {{ est.matriz?.total?.toLocaleString('pt-BR') ?? '…' }} registros originais unificados
            em {{ est.catalogo?.servicos?.toLocaleString('pt-BR') ?? '…' }} serviços únicos —
            <strong>redução de {{ reducao }}%</strong>
          </div>
        </div>
        <div class="selo-consolidacao">
          <div class="selo-numero">{{ reducao }}%</div>
          <div class="selo-texto">menos registros repetidos</div>
        </div>
      </div>

    </div>

    
    <div class="indicadores" style="margin-top: 22px">
      <div class="indicador"><div class="rotulo">Grupos de Serviço</div>
        <div class="valor">{{ est.catalogo?.grupos ?? '…' }}</div></div>
      <div class="indicador"><div class="rotulo">Fornecedores</div>
        <div class="valor">{{ est.catalogo?.fornecedores ?? '…' }}</div></div>
      <div class="indicador"><div class="rotulo">Unidades de Medida</div>
        <div class="valor">{{ est.catalogo?.unidades ?? '…' }}</div></div>
      <div class="indicador"><div class="rotulo">Códigos completos</div>
        <div class="valor">{{ est.catalogo?.codigosCompletos?.toLocaleString('pt-BR') ?? '…' }}</div></div>
    </div>

    
    <div class="grade-graficos">
      <div class="painel">
        <label class="painel-rotulo">Progresso da revisão</label>
        <GraficoRosca
          :dados="[
            { rotulo: 'Revisados', valor: est.matriz?.revisados || 0, cor: '#1e9e5a' },
            { rotulo: 'Aguardando revisão', valor: est.matriz?.pendentes || 0, cor: '#ffc72c' },
            { rotulo: 'Sem revisão necessária', valor: est.matriz?.semPendencia || 0, cor: '#9fb3c4' }
          ]"
        />
      </div>

      <div class="painel">
        <label class="painel-rotulo">Situação dos vínculos</label>
        <GraficoRosca
          :dados="[
            { rotulo: 'Ativos', valor: est.vinculos?.ativos || 0, cor: '#1e9e5a' },
            { rotulo: 'Aguardando validação', valor: est.vinculos?.pendentes || 0, cor: '#ffc72c' },
            { rotulo: 'Rejeitados', valor: est.vinculos?.rejeitados || 0, cor: '#d5392e' }
          ]"
        />
      </div>

      <div class="painel">
        <label class="painel-rotulo">Natureza dos serviços</label>
        <GraficoBarras
          :dados="(est.porNatureza || []).map((n) => ({ nome: n.nome, valor: n.quantidade }))"
          titulo="Mão de obra · Material · Equipamento · Locação"
          rotulo="serviços"
          cor="#00205b"
        />
      </div>

      <div class="painel">
        <label class="painel-rotulo">Serviços mais vinculados</label>
        <RankingLista
          :itens="(est.servicosVinculados || []).map((s) => ({
            id: s.id, nome: s.nome, grupo: s.grupo, valor: s.quantidade
          }))"
          rotulo-valor="vínculos"
        />
      </div>

      <div class="painel">
        <label class="painel-rotulo">Uso por Grupo de Serviço</label>
        <GraficoBarras
          :dados="(est.grupos || []).map((g) => ({ nome: g.nome, valor: g.quantidade }))"
          titulo="Vínculos por grupo"
          rotulo="vínculos"
        />
      </div>

      <div class="painel">
        <label class="painel-rotulo">Fornecedores mais recorrentes</label>
        <GraficoBarras
          :dados="(est.fornecedoresTop || []).map((f) => ({ nome: 'Forn. ' + f.nome, valor: f.quantidade }))"
          titulo="Registros por fornecedor"
          rotulo="registros"
          cor="#00205b"
        />
      </div>
    </div>

    
    <h3 style="margin: 28px 0 14px; font-size: 1.12rem; color: var(--azul-escuro)">Auditoria e rastreabilidade</h3>
    <p class="suave" style="margin: -6px 0 14px">Clique em um card para ver os registros.</p>
    <div class="indicadores">
      <div class="indicador auditoria-card" :class="{ ativo: painelAuditoria === 'TODOS' }" @click="abrirAuditoria('TODOS')">
        <div class="icone-auditoria" style="background: var(--azul-claro); color: var(--azul)">◆</div>
        <div>
          <div class="rotulo">Eventos registrados</div>
          <div class="valor">{{ est.auditoria?.eventos ?? 0 }}</div>
          <div class="suave" style="font-size: 0.78rem">toda ação com autor e horário (UTC)</div>
        </div>
      </div>
      <div class="indicador auditoria-card" :class="{ ativo: painelAuditoria === 'CRIACAO_CODIGO_COMPLETO' }"
           @click="abrirAuditoria('CRIACAO_CODIGO_COMPLETO')">
        <div class="icone-auditoria" style="background: var(--verde-fundo); color: var(--verde)">✓</div>
        <div>
          <div class="rotulo">Criações</div>
          <div class="valor" style="color: var(--verde)">{{ est.auditoria?.criacoes ?? 0 }}</div>
          <div class="suave" style="font-size: 0.78rem">códigos completos criados</div>
        </div>
      </div>
      <div class="indicador auditoria-card" :class="{ ativo: painelAuditoria === 'BLOQUEIO' }"
           @click="abrirAuditoria('BLOQUEIO')">
        <div class="icone-auditoria" style="background: var(--vermelho-fundo); color: var(--vermelho)">⛔</div>
        <div>
          <div class="rotulo">Bloqueios</div>
          <div class="valor" style="color: var(--vermelho)">{{ est.auditoria?.bloqueios ?? 0 }}</div>
          <div class="suave" style="font-size: 0.78rem">tentativas barradas pela política</div>
        </div>
      </div>
      <div class="indicador auditoria-card" :class="{ ativo: painelAuditoria === 'EXCECAO' }"
           @click="abrirAuditoria('EXCECAO')">
        <div class="icone-auditoria" style="background: var(--amarelo-fundo); color: var(--amarelo-alerta)">!</div>
        <div>
          <div class="rotulo">Exceções justificadas</div>
          <div class="valor" style="color: var(--amarelo-alerta)">{{ est.auditoria?.excecoes ?? 0 }}</div>
          <div class="suave" style="font-size: 0.78rem">criações com justificativa auditada</div>
        </div>
      </div>
    </div>

    
    <div v-if="painelAuditoria" class="painel" style="margin-top: 16px">
      <div class="linha" style="margin-bottom: 12px; flex-wrap: nowrap">
        <label class="painel-rotulo" style="margin: 0">
          Registros — {{ rotuloPainelAuditoria }}
        </label>
        <span class="espacador" style="flex: 1"></span>
        <button class="fechar-painel" title="Fechar" aria-label="Fechar" @click="painelAuditoria = null">✕</button>
      </div>

      <div v-if="carregandoEventos" class="suave">Carregando registros…</div>
      <div v-else-if="!eventosAuditoria.length" class="suave">Nenhum registro nesta categoria.</div>
      <table v-else class="tabela">
        <thead>
          <tr><th>Quando (UTC)</th><th>Ação</th><th>Responsável</th><th>Registro</th><th>Detalhe</th></tr>
        </thead>
        <tbody>
          <tr v-for="ev in eventosAuditoria.slice(0, 15)" :key="ev.id">
            <td class="suave">{{ formatarData(ev.occurredAt) }}</td>
            <td><strong>{{ rotuloAcao(ev.action) }}</strong></td>
            <td>{{ ev.actorId || 'sistema' }}</td>
            <td>{{ ev.entityId }}</td>
            <td class="suave">{{ ev.reason || ev.decision || '—' }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '../services/api'
import { toast } from '../services/toast'
import GraficoRosca from '../components/GraficoRosca.vue'
import GraficoBarras from '../components/GraficoBarras.vue'
import RankingLista from '../components/RankingLista.vue'
import type { Estatisticas, EventoAuditoria } from '../types'

const est = ref<Partial<Estatisticas>>({})


const painelAuditoria = ref<string | null>(null)
const eventosAuditoria = ref<EventoAuditoria[]>([])
const carregandoEventos = ref(false)

const rotuloPainelAuditoria = computed(() => ({
  TODOS: 'todos os eventos',
  CRIACAO_CODIGO_COMPLETO: 'criações de códigos completos',
  BLOQUEIO: 'bloqueios da política de similaridade',
  EXCECAO: 'exceções justificadas'
} as Record<string, string>)[painelAuditoria.value || ''] || 'eventos')

function rotuloAcao(acao: string): string {
  return ({
    CRIACAO_COMPONENTE: 'Criação de componente',
    CRIACAO_CODIGO_COMPLETO: 'Criação de código completo',
    BLOQUEIO: 'Bloqueio',
    EXCECAO: 'Exceção justificada',
    APROVACAO: 'Aprovação',
    REJEICAO: 'Rejeição',
    REVISAO_HISTORICA: 'Revisão do histórico'
  } as Record<string, string>)[acao] || acao
}

async function abrirAuditoria(acao: string): Promise<void> {
  painelAuditoria.value = painelAuditoria.value === acao ? null : acao
  if (!painelAuditoria.value) return
  carregandoEventos.value = true
  try {
    eventosAuditoria.value = await api.eventos(acao)
  } catch {
    eventosAuditoria.value = []
    toast.erro('Não foi possível carregar os registros de auditoria.')
  } finally {
    carregandoEventos.value = false
  }
}

function formatarData(iso: string): string {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })
}

const reducao = computed(() => {
  const total = est.value.matriz?.total || 0
  const servicos = est.value.catalogo?.servicos || 0
  if (!total || !servicos) return '0,0'
  return ((1 - servicos / total) * 100).toFixed(1).replace('.', ',')
})

onMounted(async () => {
  try {
    est.value = await api.estatisticas()
  } catch {
    toast.erro('Não foi possível carregar as estatísticas da base.')
  }
})


function baixarCsv(nome: string, linhas: string[][]): void {
  const csv = linhas.map((l) => l.map((c) => `"${String(c).replace(/"/g, '""')}"`).join(';')).join('\n')
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = nome
  a.click()
  URL.revokeObjectURL(url)
  toast.sucesso(`Arquivo ${nome} exportado.`)
}

function exportarResumo(): void {
  const e = est.value
  baixarCsv('resumo_geral.csv', [
    ['Métrica', 'Valor'],
    ['Serviços (catálogo)', String(e.catalogo?.servicos ?? 0)],
    ['Grupos de Serviço', String(e.catalogo?.grupos ?? 0)],
    ['Fornecedores', String(e.catalogo?.fornecedores ?? 0)],
    ['Unidades de Medida', String(e.catalogo?.unidades ?? 0)],
    ['Códigos completos', String(e.catalogo?.codigosCompletos ?? 0)],
    ['Registros originais (matriz)', String(e.matriz?.total ?? 0)],
    ['Possíveis repetidos', String(e.matriz?.duplicidade ?? 0)],
    ['Alertas', String(e.matriz?.alerta ?? 0)],
    ['Originais', String(e.matriz?.original ?? 0)],
    ['Pendentes de revisão', String(e.matriz?.pendentes ?? 0)],
    ['Revisados', String(e.matriz?.revisados ?? 0)],
    ['Eventos de auditoria', String(e.auditoria?.eventos ?? 0)]
  ])
}

function exportarDetalhamento(): void {
  const linhas: string[][] = [['Seção', 'Item', 'Quantidade']]
  for (const s of est.value.servicosTop || []) linhas.push(['Código de Serviço', `${s.id} — ${s.nome}`, String(s.quantidade)])
  for (const g of est.value.grupos || []) linhas.push(['Grupo de Serviço', g.nome, String(g.quantidade)])
  for (const f of est.value.fornecedoresTop || []) linhas.push(['Fornecedor', f.nome, String(f.quantidade)])
  for (const u of est.value.unidades || []) linhas.push(['Unidade de Medida', u.nome, String(u.quantidade)])
  baixarCsv('detalhamento_categorias.csv', linhas)
}

async function exportarDecisoes(): Promise<void> {
  try {
    const matriz = await api.matriz('', 500)
    const linhas: string[][] = [[
      'newId', 'descricaoCanonica', 'grupoServico', 'unidadeMedida', 'decision', 'score',
      'legacyId', 'legacyDescription', 'codigoFornecedor', 'alertaSap', 'statusRevisao'
    ]]
    for (const m of matriz) {
      linhas.push([
        m.newId, m.descricaoCanonica, m.grupoServico ?? '', m.unidadeMedida ?? '', m.decision,
        String(m.score ?? 0), m.legacyData?.legacyId ?? '', m.legacyData?.legacyDescription ?? '',
        String(m.legacyData?.codigoFornecedor ?? ''), String(m.legacyData?.alertaSap ?? ''), m.statusRevisao ?? ''
      ])
    }
    baixarCsv('decisoes_matriz_depara.csv', linhas)
  } catch (e) {
    toast.erro((e as Error).message)
  }
}

</script>

<style scoped>
.destaque-servicos {
  border-top: 4px solid var(--amarelo);
}
.destaque-topo {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 22px;
  flex-wrap: wrap;
}
.rotulo-destaque {
  font-size: 0.82rem;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--texto-suave);
  font-weight: 700;
}
.numero-destaque {
  font-size: 3.2rem;
  font-weight: 800;
  color: var(--azul);
  line-height: 1.1;
  font-variant-numeric: tabular-nums;
}
.selo-consolidacao {
  background: var(--azul);
  color: var(--texto-sobre-azul);
  border-radius: 16px;
  padding: 18px 26px;
  text-align: center;
}
.selo-numero {
  font-size: 2.3rem;
  font-weight: 800;
}
.selo-texto {
  font-size: 0.82rem;
  opacity: 0.9;
}
.auditoria-card {
  display: flex;
  align-items: center;
  gap: 14px;
}
.fechar-painel {
  background: transparent;
  border: none;
  color: var(--texto-suave);
  font-size: 1rem;
  width: 30px;
  height: 30px;
  border-radius: 8px;
  cursor: pointer;
}
.fechar-painel:hover {
  color: var(--texto);
  background: var(--fundo);
}
.icone-auditoria {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  font-size: 1.2rem;
  flex-shrink: 0;
}
</style>
