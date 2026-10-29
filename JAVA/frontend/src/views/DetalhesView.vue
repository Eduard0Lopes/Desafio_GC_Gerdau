<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '../services/api'
import { toast } from '../services/toast'
import { downloadCsv } from '../services/csv'
import { formatDate } from '../services/formato'
import { actionLabel } from '../services/rotulos'
import { useStatistics } from '../composables/useEstatisticas'
import DonutChart from '../components/GraficoRosca.vue'
import BarChart from '../components/GraficoBarras.vue'
import RankingList from '../components/RankingLista.vue'
import type { AuditEvent } from '../types'

const { statistics, reduction, load } = useStatistics()

const auditPanel = ref<string | null>(null)
const auditEvents = ref<AuditEvent[]>([])
const loadingEvents = ref(false)

const auditPanelLabel = computed(() => ({
  TODOS: 'todos os eventos',
  CRIACAO_CODIGO_COMPLETO: 'criações de códigos completos',
  BLOQUEIO: 'bloqueios da política de similaridade',
  EXCECAO: 'exceções justificadas'
} as Record<string, string>)[auditPanel.value || ''] || 'events')

async function openAudit(acao: string): Promise<void> {
  auditPanel.value = auditPanel.value === acao ? null : acao
  if (!auditPanel.value) return
  loadingEvents.value = true
  try {
    auditEvents.value = await api.events(acao)
  } catch {
    auditEvents.value = []
    toast.error('Não foi possível carregar os registros de auditoria.')
  } finally {
    loadingEvents.value = false
  }
}

onMounted(async () => {
  if (!(await load())) {
    toast.error('Não foi possível carregar as estatísticas da base.')
  }
})

function exportar(nome: string, rows: string[][]): void {
  downloadCsv(nome, rows)
  toast.success(`Arquivo ${nome} exportado.`)
}

function exportSummary(): void {
  const summary = statistics.value
  exportar('resumo_geral.csv', [
    ['Métrica', 'Valor'],
    ['Serviços (catálogo)', String(summary.catalogo?.servicos ?? 0)],
    ['Grupos de Serviço', String(summary.catalogo?.grupos ?? 0)],
    ['Fornecedores', String(summary.catalogo?.fornecedores ?? 0)],
    ['Unidades de Medida', String(summary.catalogo?.unidades ?? 0)],
    ['Códigos completos', String(summary.catalogo?.fullCodes ?? 0)],
    ['Registros originais (matriz)', String(summary.matriz?.total ?? 0)],
    ['Possíveis repetidos', String(summary.matriz?.duplicidade ?? 0)],
    ['Alertas', String(summary.matriz?.alerta ?? 0)],
    ['Originais', String(summary.matriz?.original ?? 0)],
    ['Pendentes de revisão', String(summary.matriz?.pendentes ?? 0)],
    ['Revisados', String(summary.matriz?.revisados ?? 0)],
    ['Eventos de auditoria', String(summary.auditoria?.events ?? 0)]
  ])
}

function exportDetails(): void {
  const rows: string[][] = [['Seção', 'Item', 'Quantidade']]
  for (const service of statistics.value.servicosTop || []) rows.push(['Código de Serviço', `${service.id} — ${service.nome}`, String(service.quantidade)])
  for (const group of statistics.value.grupos || []) rows.push(['Grupo de Serviço', group.nome, String(group.quantidade)])
  for (const supplier of statistics.value.fornecedoresTop || []) rows.push(['Fornecedor', supplier.nome, String(supplier.quantidade)])
  for (const unit of statistics.value.unidades || []) rows.push(['Unidade de Medida', unit.nome, String(unit.quantidade)])
  exportar('detalhamento_categorias.csv', rows)
}

async function exportDecisions(): Promise<void> {
  try {
    const matriz = await api.matriz('', 500)
    const rows: string[][] = [[
      'newId', 'descricaoCanonica', 'grupoServico', 'unidadeMedida', 'decision', 'score',
      'legacyId', 'legacyDescription', 'codigoFornecedor', 'alertaSap', 'statusRevisao'
    ]]
    for (const entry of matriz) {
      rows.push([
        entry.newId, entry.descricaoCanonica, entry.grupoServico ?? '', entry.unidadeMedida ?? '', entry.decision,
        String(entry.score ?? 0), entry.legacyData?.legacyId ?? '', entry.legacyData?.legacyDescription ?? '',
        String(entry.legacyData?.codigoFornecedor ?? ''), String(entry.legacyData?.alertaSap ?? ''), entry.statusRevisao ?? ''
      ])
    }
    exportar('decisoes_matriz_depara.csv', rows)
  } catch (error) {
    toast.error((error as Error).message)
  }
}

</script>

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
      <button class="botao botao-escuro" @click="exportSummary">⤓ Exportar resumo (CSV)</button>
      <button class="botao botao-escuro" @click="exportDetails">⤓ Exportar detalhamento (CSV)</button>
      <button class="botao botao-roxo" @click="exportDecisions">⤓ Exportar decisões (CSV)</button>
    </div>

    <div class="painel destaque-servicos">
      <div class="destaque-topo">
        <div>
          <div class="rotulo-destaque">Códigos de Serviço</div>
          <div class="numero-destaque">{{ statistics.catalogo?.servicos?.toLocaleString('pt-BR') ?? '…' }}</div>
          <div class="suave" style="margin-top: 6px">
            {{ statistics.matriz?.total?.toLocaleString('pt-BR') ?? '…' }} registros originais unificados
            em {{ statistics.catalogo?.servicos?.toLocaleString('pt-BR') ?? '…' }} serviços únicos —
            <strong>redução de {{ reduction }}%</strong>
          </div>
        </div>
        <div class="selo-consolidacao">
          <div class="selo-numero">{{ reduction }}%</div>
          <div class="selo-texto">menos registros repetidos</div>
        </div>
      </div>

    </div>

    <div class="indicadores" style="margin-top: 22px">
      <div class="indicador"><div class="rotulo">Grupos de Serviço</div>
        <div class="valor">{{ statistics.catalogo?.grupos ?? '…' }}</div></div>
      <div class="indicador"><div class="rotulo">Fornecedores</div>
        <div class="valor">{{ statistics.catalogo?.fornecedores ?? '…' }}</div></div>
      <div class="indicador"><div class="rotulo">Unidades de Medida</div>
        <div class="valor">{{ statistics.catalogo?.unidades ?? '…' }}</div></div>
      <div class="indicador"><div class="rotulo">Códigos completos</div>
        <div class="valor">{{ statistics.catalogo?.fullCodes?.toLocaleString('pt-BR') ?? '…' }}</div></div>
    </div>

    <div class="grade-graficos">
      <div class="painel">
        <label class="painel-rotulo">Progresso da revisão</label>
        <DonutChart
          :data="[
            { label: 'Revisados', value: statistics.matriz?.revisados || 0, color: '#1e9e5a' },
            { label: 'Aguardando revisão', value: statistics.matriz?.pendentes || 0, color: '#ffc72c' },
            { label: 'Sem revisão necessária', value: statistics.matriz?.semPendencia || 0, color: '#9fb3c4' }
          ]"
        />
      </div>

      <div class="painel">
        <label class="painel-rotulo">Situação dos vínculos</label>
        <DonutChart
          :data="[
            { label: 'Ativos', value: statistics.vinculos?.ativos || 0, color: '#1e9e5a' },
            { label: 'Aguardando validação', value: statistics.vinculos?.pendentes || 0, color: '#ffc72c' },
            { label: 'Rejeitados', value: statistics.vinculos?.rejeitados || 0, color: '#d5392e' }
          ]"
        />
      </div>

      <div class="painel">
        <label class="painel-rotulo">Natureza dos serviços</label>
        <BarChart
          :data="(statistics.porNatureza || []).map((nature) => ({ nome: nature.nome, value: nature.quantidade }))"
          title="Mão de obra · Material · Equipamento · Locação"
          label="serviços"
          color="#00205b"
        />
      </div>

      <div class="painel">
        <label class="painel-rotulo">Serviços mais vinculados</label>
        <RankingList
          :itens="(statistics.servicosVinculados || []).map((service) => ({
            id: service.id, nome: service.nome, grupo: service.grupo, value: service.quantidade
          }))"
          value-label="vínculos"
        />
      </div>

      <div class="painel">
        <label class="painel-rotulo">Uso por Grupo de Serviço</label>
        <BarChart
          :data="(statistics.grupos || []).map((group) => ({ nome: group.nome, value: group.quantidade }))"
          title="Vínculos por grupo"
          label="vínculos"
        />
      </div>

      <div class="painel">
        <label class="painel-rotulo">Fornecedores mais recorrentes</label>
        <BarChart
          :data="(statistics.fornecedoresTop || []).map((supplier) => ({ nome: 'Forn. ' + supplier.nome, value: supplier.quantidade }))"
          title="Registros por fornecedor"
          label="registros"
          color="#00205b"
        />
      </div>
    </div>

    <h3 style="margin: 28px 0 14px; font-size: 1.12rem; color: var(--azul-escuro)">Auditoria e rastreabilidade</h3>
    <p class="suave" style="margin: -6px 0 14px">Clique em um card para ver os registros.</p>
    <div class="indicadores">
      <div class="indicador auditoria-card" :class="{ ativo: auditPanel === 'TODOS' }" @click="openAudit('TODOS')">
        <div class="icone-auditoria" style="background: var(--azul-claro); color: var(--azul)">◆</div>
        <div>
          <div class="rotulo">Eventos registrados</div>
          <div class="valor">{{ statistics.auditoria?.events ?? 0 }}</div>
          <div class="suave" style="font-size: 0.78rem">toda ação com autor e horário (UTC)</div>
        </div>
      </div>
      <div class="indicador auditoria-card" :class="{ ativo: auditPanel === 'CRIACAO_CODIGO_COMPLETO' }"
           @click="openAudit('CRIACAO_CODIGO_COMPLETO')">
        <div class="icone-auditoria" style="background: var(--verde-fundo); color: var(--verde)">✓</div>
        <div>
          <div class="rotulo">Criações</div>
          <div class="valor" style="color: var(--verde)">{{ statistics.auditoria?.criacoes ?? 0 }}</div>
          <div class="suave" style="font-size: 0.78rem">códigos completos criados</div>
        </div>
      </div>
      <div class="indicador auditoria-card" :class="{ ativo: auditPanel === 'BLOQUEIO' }"
           @click="openAudit('BLOQUEIO')">
        <div class="icone-auditoria" style="background: var(--vermelho-fundo); color: var(--vermelho)">⛔</div>
        <div>
          <div class="rotulo">Bloqueios</div>
          <div class="valor" style="color: var(--vermelho)">{{ statistics.auditoria?.bloqueios ?? 0 }}</div>
          <div class="suave" style="font-size: 0.78rem">tentativas barradas pela política</div>
        </div>
      </div>
      <div class="indicador auditoria-card" :class="{ ativo: auditPanel === 'EXCECAO' }"
           @click="openAudit('EXCECAO')">
        <div class="icone-auditoria" style="background: var(--amarelo-fundo); color: var(--amarelo-alerta)">!</div>
        <div>
          <div class="rotulo">Exceções justificadas</div>
          <div class="valor" style="color: var(--amarelo-alerta)">{{ statistics.auditoria?.excecoes ?? 0 }}</div>
          <div class="suave" style="font-size: 0.78rem">criações com justificativa auditada</div>
        </div>
      </div>
    </div>

    <div v-if="auditPanel" class="painel" style="margin-top: 16px">
      <div class="linha" style="margin-bottom: 12px; flex-wrap: nowrap">
        <label class="painel-rotulo" style="margin: 0">
          Registros — {{ auditPanelLabel }}
        </label>
        <span class="espacador" style="flex: 1"></span>
        <button class="fechar-painel" title="Fechar" aria-label="Fechar" @click="auditPanel = null">✕</button>
      </div>

      <div v-if="loadingEvents" class="suave">Carregando registros…</div>
      <div v-else-if="!auditEvents.length" class="suave">Nenhum registro nesta categoria.</div>
      <table v-else class="tabela">
        <thead>
          <tr><th>Quando (UTC)</th><th>Ação</th><th>Responsável</th><th>Registro</th><th>Detalhe</th></tr>
        </thead>
        <tbody>
          <tr v-for="event in auditEvents.slice(0, 15)" :key="event.id">
            <td class="suave">{{ formatDate(event.occurredAt) }}</td>
            <td><strong>{{ actionLabel(event.action) }}</strong></td>
            <td>{{ event.actorId || 'sistema' }}</td>
            <td>{{ event.entityId }}</td>
            <td class="suave">{{ event.reason || event.decision || '—' }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

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
