<template>
  <div>
    <h2 style="margin: 0 0 6px; font-size: 1.35rem; color: var(--azul-escuro)">Catálogo</h2>
    <p class="suave" style="margin: 0 0 20px">
      Consulta somente leitura de todos os componentes: grupos, fornecedores, serviços e unidades.
      Itens aguardando aprovação aparecem com o selo de status — só entram em uso depois de aprovados.
    </p>

    
    <div class="abas">
      <button
        v-for="a in abas"
        :key="a.chave"
        class="aba"
        :class="{ ativa: aba === a.chave }"
        @click="trocarAba(a.chave)"
      >
        {{ a.rotulo }}
        <span class="aba-contagem">{{ totais[a.chave] ?? '…' }}</span>
      </button>
    </div>

    
    <div class="linha-campo" style="margin: 18px 0">
      <div class="campo-busca-wrap">
        <input
          v-model="busca"
          type="text"
          class="campo-busca"
          :placeholder="`Buscar em ${abaAtual.rotulo.toLowerCase()} por nome, descrição ou código…`"
        />
        <button v-if="busca" type="button" class="btn-limpar-busca" title="Limpar busca" @click="busca = ''">✕</button>
      </div>
      <button
        type="button"
        class="botao-filtro-excecao"
        :class="{ ativo: somenteExcecoes }"
        :title="somenteExcecoes
          ? 'Mostrando apenas casos especiais — clique para ver todos'
          : 'Mostrar apenas os itens com (!): exceções aprovadas e duplicidades tratadas'"
        @click="alternarFiltroExcecao"
      >
        <span class="badge-excecao">!</span>
        Casos especiais
        <span class="contagem">{{ totalExcecoes }}</span>
      </button>
      <button
        type="button"
        class="botao-filtro-excecao btn-alteracoes"
        title="Ver a lista de itens alterados, transferidos e excluídos na base"
        @click="abrirAlteracoes"
      >
        📋 Alterações
      </button>
      <span v-if="isAdmin && selecionados.length" class="badge badge-alertar">
        {{ selecionados.length }} selecionado(s)
      </span>
      <span style="flex: 1"></span>
      <button class="botao botao-escuro" @click="abrirExportacao">⤓ Exportar CSV</button>
      <button v-if="isAdmin" class="botao botao-escuro" @click="abrirImportacao">⤓ Importar CSV</button>
      <button
        v-if="isAdmin && selecionadosAtivos.length"
        class="botao botao-escuro"
        :disabled="alterando"
        @click="confirmando = 'DESATIVADO'"
      >
        {{ alterando ? 'Aplicando…' : `✕ Desativar selecionados (${selecionadosAtivos.length})` }}
      </button>
      <button
        v-if="isAdmin && selecionadosDesativados.length"
        class="botao botao-verde"
        :disabled="alterando"
        @click="confirmando = 'ATIVO'"
      >
        ↻ Reativar selecionados ({{ selecionadosDesativados.length }})
      </button>
    </div>

    
    <div v-if="ultimaImportacao" class="aviso aviso-azul" style="margin-bottom: 16px">
      <span class="icone">↩</span>
      <div style="display: flex; align-items: center; gap: 10px; flex-wrap: wrap">
        <span>
          <strong>Importação concluída:</strong> {{ ultimaImportacao.ids.length }} item(ns)
          adicionado(s) a {{ ultimaImportacao.rotulo }}.
        </span>
        <button class="botao botao-escuro" @click="confirmandoDesfazer = true">Desfazer importação</button>
        <button class="cb-excluir" title="Dispensar aviso" @click="ultimaImportacao = null">✕</button>
      </div>
    </div>

    <div v-if="falhas.length" class="aviso aviso-amarelo" style="margin-bottom: 16px">
      <span class="icone">⚠</span>
      <div>
        <strong>{{ falhas.length }} item(ns) não tiveram a situação alterada:</strong>
        <div v-for="f in falhas.slice(0, 5)" :key="f.id" style="margin-top: 6px">
          <strong>{{ f.id }}</strong> — {{ f.motivo }}
        </div>
      </div>
    </div>

    
    <div class="painel" style="overflow-x: auto">
      <table class="tabela">
        <thead>
          <tr>
            <th v-if="isAdmin" style="width: 38px">
              <input
                type="checkbox"
                :checked="todosPaginaSelecionados"
                :indeterminate.prop="algumSelecionadoNaPagina && !todosPaginaSelecionados"
                title="Selecionar todos desta página"
                @change="alternarPaginaInteira"
              />
            </th>
            <th
              v-for="c in abaAtual.colunas"
              :key="c.chave"
              class="th-ordenable"
              :title="`Ordenar por ${c.rotulo.toLowerCase()}`"
              @click="ordenarPor(c.chave)"
            >
              {{ c.rotulo }}
              <span v-if="ordem?.chave === c.chave" class="seta-ordenacao">{{ ordem.direcao === 1 ? '▲' : '▼' }}</span>
            </th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in paginaVisivel" :key="String(item.id)" :class="{ 'linha-excecao': item.casoIsolado }">
            <td v-if="isAdmin">
              <input type="checkbox" :checked="estaSelecionado(item)" @change="alternarSelecao(item)" />
            </td>
            <td v-for="c in abaAtual.colunas" :key="c.chave">
              <span v-if="c.chave === 'id'" class="id-codigo">{{ valor(item, c.chave) }}</span>
              <template v-else>{{ valor(item, c.chave) }}</template>
              <button
                v-if="item.casoIsolado && (c.chave === 'id' || c.chave === 'sigla' || c.chave === 'nome' || c.chave === 'descricao')"
                type="button"
                class="badge-excecao"
                :title="String(item.motivoCasoIsolado || 'Caso especial registrado') + ' — clique para resolver e remover o (!)'"
                @click.stop="abrirResolucao(item)"
              >!</button>
            </td>
            <td>
              <span class="badge" :class="classeStatus(item.status)">{{ rotuloStatus(item.status) }}</span>
              <button
                v-if="!item.casoIsolado"
                type="button"
                class="btn-marcar-excecao"
                title="Marcar como caso especial (!) — para revisar, resolver ou excluir depois"
                @click.stop="abrirMarcacao(item)"
              >!</button>
            </td>
          </tr>
          <tr v-if="!paginaVisivel.length">
            <td :colspan="abaAtual.colunas.length + (isAdmin ? 2 : 1)" class="suave centralizado">
              <template v-if="somenteExcecoes">Nenhum caso especial ({{ totalExcecoes }}) nesta aba.</template>
              <template v-else>Nenhum item encontrado para “{{ busca }}”.</template>
            </td>
          </tr>
        </tbody>
      </table>

      
      <div v-if="totalPaginas > 1" class="paginacao" style="margin-top: 16px">
        <button class="pulo" :disabled="pagina === 0" @click="pagina = 0" title="Início">|‹</button>
        <button class="pulo" :disabled="pagina === 0" @click="pagina--" title="Página anterior">‹</button>
        <span class="info">
          <strong>{{ pagina + 1 }}</strong> / {{ totalPaginas }} — {{ filtrados.length }} item(ns)
        </span>
        <button class="pulo" :disabled="pagina + 1 >= totalPaginas" @click="pagina++" title="Próxima página">›</button>
        <button class="pulo" :disabled="pagina + 1 >= totalPaginas" @click="pagina = totalPaginas - 1" title="Fim">›|</button>
      </div>
    </div>

    
    <p class="legenda-excecao suave">
      <span class="badge-excecao">!</span>
      <span>
        <strong>Caso especial</strong> — item com exceção aprovada (similar aceito com justificativa)
        ou duplicidade tratada na varredura. Passe o mouse sobre o ! para ver o motivo.
      </span>
    </p>

    <div v-if="modalExportar" class="modal-fundo" @click.self="modalExportar = false">
      <div class="modal" style="max-width: 540px">
        <h3>Exportar catálogo (CSV)</h3>
        <p class="suave" style="margin: -8px 0 16px">
          O arquivo abre direto no Excel (separador “;”, com acentuação correta).
        </p>
        <div class="campo-form">
          <label for="tabela-exportar">Qual tabela exportar?</label>
          <select id="tabela-exportar" v-model="exportarTabela">
            <option value="TODAS">Todas as tabelas (um arquivo por tabela)</option>
            <option v-for="a in abas" :key="a.chave" :value="a.chave">{{ a.rotulo }}</option>
          </select>
        </div>
        <div class="linha-botoes">
          <button class="botao botao-escuro" @click="modalExportar = false">Cancelar</button>
          <button class="botao botao-roxo" @click="executarExportacao">⤓ Exportar</button>
        </div>
      </div>
    </div>

    
    <div v-if="modalImportar" class="modal-fundo" @click.self="modalImportar = false">
      <div class="modal" style="max-width: 620px">
        <h3>Importar catálogo (CSV)</h3>
        <div class="aviso aviso-verde" style="margin: 0 0 16px">
          <span class="icone">✓</span>
          <div>
            <strong>Só é adicionado o que for novo.</strong> Nada que já existe no banco é
            apagado ou alterado — linhas repetidas voltam no relatório como “ignoradas”.
          </div>
        </div>
        <div class="campo-form">
          <label for="tabela-importar">Qual tabela será alimentada?</label>
          <select id="tabela-importar" v-model="importarTabela" @change="limparImportacao">
            <option v-for="a in abas" :key="a.chave" :value="a.chave">{{ a.rotulo }}</option>
          </select>
        </div>
        <div class="campo-form">
          <label for="arquivo-importar">Arquivo CSV</label>
          <input id="arquivo-importar" type="file" accept=".csv,text/csv" @change="lerArquivoImportacao" />
        </div>
        <div class="aviso aviso-azul">
          <span class="icone">ℹ</span>
          <div>
            {{ dicaColunas }}<br />
            A <strong>primeira linha deve ser o cabeçalho</strong> — exporte antes para ver o formato exato.
            <strong>“Criado em” e “Criado por” são gravados automaticamente</strong> com o momento e o
            usuário que fez a importação (valores do arquivo são ignorados).
          </div>
        </div>

        
        <div v-if="itensImportacao.length" class="aviso" :class="previaImportacao.novos.length ? 'aviso-verde' : 'aviso-amarelo'" style="margin-top: 14px">
          <span class="icone">{{ previaImportacao.novos.length ? '✓' : 'ℹ' }}</span>
          <div>
            <strong>Só o que é novo entra: {{ previaImportacao.novos.length }} de {{ itensImportacao.length }} linha(s).</strong>
            {{ previaImportacao.repetidos.length }} já {{ previaImportacao.repetidos.length === 1 ? 'existe e será mantida' : 'existem e serão mantidas' }} como está/estão.
            <div v-for="r in previaImportacao.repetidos.slice(0, 5)" :key="rotuloItemImportado(r)" class="suave" style="margin-top: 5px">
              já existe: <strong>{{ rotuloItemImportado(r) }}</strong>
            </div>
            <div v-if="previaImportacao.repetidos.length > 5" class="suave" style="margin-top: 5px">
              … e mais {{ previaImportacao.repetidos.length - 5 }}.
            </div>
          </div>
        </div>

        <div class="linha-botoes">
          <button class="botao botao-escuro" @click="modalImportar = false">Cancelar</button>
          <button
            class="botao botao-roxo"
            :disabled="!previaImportacao.novos.length || importando"
            @click="executarImportacao"
          >
            {{ importando ? 'Importando…' : previaImportacao.novos.length ? `Importar ${previaImportacao.novos.length} nova(s)` : 'Nada novo para importar' }}
          </button>
        </div>
      </div>
    </div>

    
    <div v-if="transferindo" class="modal-fundo" @click.self="fecharTransferencia">
      <div class="modal" style="max-width: 620px">
        <h3>Transferir vínculos para excluir</h3>
        <p class="suave" style="margin: -8px 0 16px; line-height: 1.6">
          <strong>{{ transferindo.id }} — {{ abaAtual.rotuloDe(transferindo) }}</strong> está vinculado a
          <strong>{{ contagemVinculos }} código(s) completo(s)</strong>. Para liberar a exclusão, escolha
          um item da mesma tabela para <strong>receber esses vínculos</strong> — depois o item de origem
          é excluído definitivamente.
        </p>
        <div class="campo-form">
          <label>Receber os vínculos em</label>
          <ComboboxBusca
            :carregar="abaAtual.carregar"
            :nome-de="(i) => String(i.id ?? '') + ' — ' + abaAtual.rotuloDe(i)"
            :id-de="() => ''"
            :permite-criar="false"
            placeholder="Buscar item desta tabela para receber os vínculos…"
            @update:model-value="(v) => (destinoVinculos = (v as Item) || null)"
          />
        </div>
        <div class="linha-botoes">
          <button class="botao botao-escuro" @click="fecharTransferencia">Cancelar</button>
          <button
            class="botao botao-vermelho"
            :disabled="!destinoVinculos || processandoTransferencia"
            @click="confirmarTransferencia"
          >
            {{ processandoTransferencia ? 'Transferindo…' : '⇄ Transferir vínculos e excluir' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="modalAlteracoes" class="modal-fundo" @click.self="modalAlteracoes = false">
      <div class="modal" style="max-width: 860px">
        <h3>Itens alterados e excluídos na base</h3>
        <p class="suave" style="margin: -8px 0 14px; line-height: 1.6">
          Histórico completo das limpezas e decisões: itens <strong>excluídos</strong>,
          vínculos <strong>transferidos</strong>, composições <strong>mescladas</strong> e itens
          <strong>desativados/reativados</strong> — cada linha com o motivo registrado.
        </p>
        <div class="lista-alteracoes">
          <div v-if="carregandoAlteracoes" class="suave centralizado" style="padding: 22px">Carregando…</div>
          <div v-else-if="!alteracoes.length" class="suave centralizado" style="padding: 22px">
            Nenhuma alteração registrada ainda.
          </div>
          <div v-for="(e, i) in alteracoes" :key="e.id || i" class="linha-alteracao">
            <span class="badge" :class="classeAlteracao(e)">{{ rotuloAlteracao(e) }}</span>
            <strong class="id-alteracao">{{ e.entityId }}</strong>
            <span class="motivo-alteracao">{{ e.reason || '—' }}</span>
            <span class="data-alteracao">{{ formatarData(e.occurredAt) }}</span>
          </div>
        </div>
        <div class="linha-botoes">
          <button class="botao botao-escuro" @click="modalAlteracoes = false">Fechar</button>
        </div>
      </div>
    </div>

    <div v-if="marcando" class="modal-fundo" @click.self="marcando = null">
      <div class="modal" style="max-width: 540px">
        <h3>Marcar como caso especial</h3>
        <p class="suave" style="margin: -8px 0 16px; line-height: 1.6">
          O item <strong>{{ marcando.id }} — {{ abaAtual.rotuloDe(marcando) }}</strong> receberá o
          <span class="badge-excecao">!</span> e passará a aparecer no filtro
          <strong>Casos especiais</strong>. Registre o motivo — é ele que aparece no hover do badge.
        </p>
        <div class="campo-form">
          <label>Motivo do caso especial</label>
          <textarea
            v-model="motivoMarcacao"
            style="min-height: 84px"
            placeholder="Ex.: possível duplicidade de “X” — confirmar antes de excluir."
          ></textarea>
        </div>
        <div class="linha-botoes">
          <button class="botao botao-escuro" @click="marcando = null">Cancelar</button>
          <button
            class="botao botao-roxo"
            :disabled="!motivoMarcacao.trim() || resolvendoAgora"
            @click="confirmarMarcacao"
          >
            ✓ Marcar com (!)
          </button>
        </div>
      </div>
    </div>

    <ModalConfirmacao
      v-if="resolvendo"
      titulo="Resolver caso especial?"
      :mensagem="`Motivo registrado: ${String(resolvendo.motivoCasoIsolado || 'caso especial registrado')}. Ao resolver, o (!) é removido do item e ele sai do filtro de casos especiais. O histórico continua na auditoria.`"
      rotulo-secundario="✕ Excluir definitivamente"
      :secundario-perigoso="true"
      rotulo-confirmar="✓ Resolver e remover (!)"
      :acao-perigosa="false"
      @secundario="pedirExclusao"
      @confirmar="confirmarResolucao"
      @cancelar="resolvendo = null"
    />
    <ModalConfirmacao
      v-if="excluindo"
      titulo="Excluir definitivamente?"
      :mensagem="`O item ${excluindo.id} — ${abaAtual.rotuloDe(excluindo)} será APAGADO do banco de dados. Esta ação NÃO pode ser desfeita. Se houver vínculos ativos, a exclusão será bloqueada. Se o item puder voltar um dia, prefira apenas desativar.`"
      rotulo-confirmar="✕ Excluir para sempre"
      :acao-perigosa="true"
      @confirmar="confirmarExclusao"
      @cancelar="excluindo = null"
    />
    <ModalConfirmacao
      v-if="confirmandoDesfazer"
      titulo="Desfazer a importação?"
      :mensagem="`${ultimaImportacao?.ids.length ?? 0} item(ns) adicionado(s) agora serão removidos, voltando ao estado anterior. O que já existia não é tocado.`"
      rotulo-confirmar="Desfazer importação"
      :acao-perigosa="true"
      @confirmar="desfazerImportacao"
      @cancelar="confirmandoDesfazer = false"
    />
    <ModalConfirmacao
      v-if="confirmando"
      :titulo="confirmando === 'DESATIVADO' ? 'Desativar itens do catálogo?' : 'Reativar itens do catálogo?'"
      :mensagem="confirmando === 'DESATIVADO'
        ? `${selecionadosAtivos.length} item(ns) de ${abaAtual.rotulo.toLowerCase()} sairão de circulação: somem das listas de uso e da similaridade. Nada é perdido — vínculos, histórico e auditoria continuam, e dá para reativar quando quiser.`
        : `${selecionadosDesativados.length} item(ns) de ${abaAtual.rotulo.toLowerCase()} voltarão para as listas de uso. A ação fica registrada na auditoria.`"
      :rotulo-confirmar="confirmando === 'DESATIVADO' ? '✕ Desativar' : '↻ Reativar'"
      :acao-perigosa="confirmando === 'DESATIVADO'"
      @confirmar="executarSituacao"
      @cancelar="confirmando = null"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { api } from '../services/api'
import { toast } from '../services/toast'
import { casaComTermo } from '../services/texto'
import ModalConfirmacao from '../components/ModalConfirmacao.vue'
import ComboboxBusca from '../components/ComboboxBusca.vue'
import type { EventoAuditoria, ItemImportacao } from '../types'

type Item = Record<string, unknown>
type ChaveAba = 'GS' | 'CF' | 'CS' | 'UM'
type TipoExclusao = 'GS' | 'CF' | 'CS' | 'UM'

const props = withDefaults(defineProps<{ usuario?: string; papel?: string }>(), { usuario: '', papel: 'USUARIO' })
const isAdmin = computed(() => props.papel === 'ADMIN')

interface Coluna { chave: string; rotulo: string }
interface Aba {
  chave: ChaveAba
  rotulo: string
  colunas: Coluna[]
  carregar: (query?: string, limite?: number, incluirTodos?: boolean) => Promise<unknown[]>
  rotuloDe: (i: Item) => string
}

const abas: Aba[] = [
  {
    chave: 'GS',
    rotulo: 'Grupo de Serviço',
    colunas: [{ chave: 'id', rotulo: 'ID' }, { chave: 'nome', rotulo: 'Nome' }, { chave: 'origem', rotulo: 'Origem' },
      { chave: 'createdAt', rotulo: 'Criado em' }, { chave: 'criadoPor', rotulo: 'Criado por' }],
    carregar: api.gruposServico.listar,
    rotuloDe: (i) => String(i.nome ?? i.id)
  },
  {
    chave: 'CF',
    rotulo: 'Código de Fornecedor',
    colunas: [
      { chave: 'id', rotulo: 'ID' },
      { chave: 'codigoFornecedorSAP', rotulo: 'SAP' },
      { chave: 'nome', rotulo: 'Nome' },
      { chave: 'origem', rotulo: 'Origem' },
      { chave: 'createdAt', rotulo: 'Criado em' },
      { chave: 'criadoPor', rotulo: 'Criado por' }
    ],
    carregar: api.codigosFornecedor.listar,
    rotuloDe: (i) => String(i.nome ?? i.id)
  },
  {
    chave: 'CS',
    rotulo: 'Código de Serviço',
    colunas: [
      { chave: 'id', rotulo: 'ID' },
      { chave: 'descricao', rotulo: 'Descrição' },
      { chave: 'quantidadeCaracteres', rotulo: 'Caracteres' },
      { chave: 'origem', rotulo: 'Origem' },
      { chave: 'createdAt', rotulo: 'Criado em' },
      { chave: 'criadoPor', rotulo: 'Criado por' }
    ],
    carregar: api.codigosServico.listar,
    rotuloDe: (i) => String(i.descricao ?? i.id)
  },
  {
    chave: 'UM',
    rotulo: 'Unidade de Medida',
    colunas: [
      { chave: 'sigla', rotulo: 'Sigla' },
      { chave: 'nome', rotulo: 'Nome' },
      { chave: 'origem', rotulo: 'Origem' },
      { chave: 'createdAt', rotulo: 'Criado em' },
      { chave: 'criadoPor', rotulo: 'Criado por' }
    ],
    carregar: api.unidadesMedida.listar,
    rotuloDe: (i) => `${String(i.sigla ?? '')} - ${String(i.nome ?? '')}`
  }
]

const aba = ref<ChaveAba>('GS')
const busca = ref('')
const pagina = ref(0)
const porPagina = 25
const dados = ref<Record<ChaveAba, Item[]>>({ GS: [], CF: [], CS: [], UM: [] })
const carregado = ref<Record<ChaveAba, boolean>>({ GS: false, CF: false, CS: false, UM: false })
const selecionados = ref<Item[]>([])
const confirmando = ref<'DESATIVADO' | 'ATIVO' | null>(null)
const alterando = ref(false)
const falhas = ref<{ id: string; motivo: string }[]>([])

const abaAtual = computed(() => abas.find((a) => a.chave === aba.value) as Aba)
const totais = computed<Record<string, number>>(() => ({
  GS: dados.value.GS.length,
  CF: dados.value.CF.length,
  CS: dados.value.CS.length,
  UM: dados.value.UM.length
}))


function normalizar(texto: string): string {
  return texto
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
}

/* ============================================================================
   CASOS ESPECIAIS — o badge "(!)" que acompanha o item em toda a interface.
   Ciclo: MARCAR (manual ou pela governança/varredura) → REVISAR (filtro + hover
   com o motivo) → RESOLVER (limpa a marca) ou EXCLUIR (com transferência de
   vínculos). Também dá para transferir vínculos antes de excluir quando o item
   está preso a composições, e consultar tudo que foi mexido em "📋 Alterações".
   ========================================================================== */

// filtro dos casos especiais — os itens marcados com (!)
const somenteExcecoes = ref(false)
const totalExcecoes = computed(() => dados.value[aba.value].filter((i) => Boolean(i.casoIsolado)).length)
function alternarFiltroExcecao(): void {
  somenteExcecoes.value = !somenteExcecoes.value
  pagina.value = 0
}

// lista do que foi alterado/transferido/excluído na base (histórico de auditoria)
const modalAlteracoes = ref(false)
const alteracoes = ref<EventoAuditoria[]>([])
const carregandoAlteracoes = ref(false)
async function abrirAlteracoes(): Promise<void> {
  modalAlteracoes.value = true
  carregandoAlteracoes.value = true
  try {
    const eventos = await api.eventos()
    const acoes = new Set(['EXCLUSAO_COMPONENTE', 'TRANSFERENCIA_VINCULOS', 'DESATIVACAO_COMPONENTE', 'REATIVACAO_COMPONENTE'])
    alteracoes.value = eventos.filter((e) => acoes.has(e.action))
  } catch (e) {
    toast.erro((e as Error).message)
  } finally {
    carregandoAlteracoes.value = false
  }
}
function rotuloAlteracao(e: EventoAuditoria): string {
  if (e.action === 'EXCLUSAO_COMPONENTE') return 'Excluído'
  if (e.action === 'TRANSFERENCIA_VINCULOS') return e.decision === 'MESCLAGEM' ? 'Mesclado' : 'Transferido'
  if (e.action === 'DESATIVACAO_COMPONENTE') return 'Desativado'
  return 'Reativado'
}
function classeAlteracao(e: EventoAuditoria): string {
  return e.action === 'EXCLUSAO_COMPONENTE' ? 'badge-revisar'
    : e.action === 'TRANSFERENCIA_VINCULOS' ? 'badge-alertar' : 'badge-permitir'
}

// resolver um caso especial: clica no (!), confere o motivo e remove a marca
const resolvendo = ref<Item | null>(null)
const resolvendoAgora = ref(false)
const excluindo = ref<Item | null>(null)
const marcando = ref<Item | null>(null)
const motivoMarcacao = ref('')
function abrirResolucao(item: Item): void {
  resolvendo.value = item
}
function abrirMarcacao(item: Item): void {
  marcando.value = item
  motivoMarcacao.value = ''
}
async function confirmarMarcacao(): Promise<void> {
  const item = marcando.value
  const motivo = motivoMarcacao.value.trim()
  if (!item || !motivo || resolvendoAgora.value) return
  resolvendoAgora.value = true
  marcando.value = null
  try {
    await api.catalogo.marcarExcecao(String(item.id), motivo)
    item.casoIsolado = true
    item.motivoCasoIsolado = motivo
    toast.sucesso('Item marcado com (!) — ele aparece no filtro Casos especiais.')
  } catch (e) {
    toast.erro((e as Error).message)
  } finally {
    resolvendoAgora.value = false
  }
}
function pedirExclusao(): void {
  excluindo.value = resolvendo.value
  resolvendo.value = null
}
async function confirmarResolucao(): Promise<void> {
  const item = resolvendo.value
  if (!item || resolvendoAgora.value) return
  resolvendoAgora.value = true
  resolvendo.value = null
  try {
    await api.catalogo.limparCasoIsolado(String(item.id))
    item.casoIsolado = false
    item.motivoCasoIsolado = null
    toast.sucesso('Caso resolvido — o (!) foi removido do item.')
  } catch (e) {
    toast.erro((e as Error).message)
  } finally {
    resolvendoAgora.value = false
  }
}
async function confirmarExclusao(): Promise<void> {
  const item = excluindo.value
  if (!item || resolvendoAgora.value) return
  resolvendoAgora.value = true
  excluindo.value = null
  try {
    await api.componentes.excluir(aba.value as TipoExclusao, String(item.id))
    const lista = dados.value[aba.value]
    const i = lista.findIndex((x) => String(x.id) === String(item.id))
    if (i >= 0) lista.splice(i, 1)
    selecionados.value = selecionados.value.filter((s) => String(s.id) !== String(item.id))
    toast.sucesso(`Item ${item.id} excluído definitivamente.`)
  } catch (e) {
    const msg = (e as Error).message || ''
    if (msg.toLowerCase().includes('vincul')) {
      // tem vínculo: abre o modal para escolher quem recebe antes de excluir
      await abrirTransferencia(item)
    } else {
      toast.erro(msg)
    }
  } finally {
    resolvendoAgora.value = false
  }
}

// transferir vínculos para outro item e então excluir o de origem
const transferindo = ref<Item | null>(null)
const destinoVinculos = ref<Item | null>(null)
const contagemVinculos = ref(0)
const processandoTransferencia = ref(false)

async function abrirTransferencia(item: Item): Promise<void> {
  transferindo.value = item
  destinoVinculos.value = null
  contagemVinculos.value = 0
  try {
    const v = await api.catalogo.vinculos(aba.value as 'GS' | 'CF' | 'CS' | 'UM', String(item.id))
    contagemVinculos.value = v.total
  } catch {
    contagemVinculos.value = 0
  }
}
function fecharTransferencia(): void {
  transferindo.value = null
  destinoVinculos.value = null
}
async function confirmarTransferencia(): Promise<void> {
  const item = transferindo.value
  const destino = destinoVinculos.value
  if (!item || !destino || processandoTransferencia.value) return
  processandoTransferencia.value = true
  try {
    const r = await api.catalogo.transferirVinculos({
      tipo: aba.value as 'GS' | 'CF' | 'CS' | 'UM',
      de: String(item.id),
      para: String(destino.id),
      excluirOrigem: true
    })
    if (r.excluido) {
      const lista = dados.value[aba.value]
      const i = lista.findIndex((x) => String(x.id) === String(item.id))
      if (i >= 0) lista.splice(i, 1)
      selecionados.value = selecionados.value.filter((s) => String(s.id) !== String(item.id))
      const partes = [`${r.transferidos} vínculo(s) transferido(s) para ${destino.id}`]
      if (r.mesclados) partes.push(`${r.mesclados} composição(ões) redundante(s) mesclada(s)`)
      toast.sucesso(`${partes.join(' e ')} — ${item.id} excluído definitivamente.`)
      fecharTransferencia()
    } else {
      toast.aviso(`Transferidos ${r.transferidos}, mas ${r.conflitos.length} composição(ões) não puderam ser movidas — confira os vínculos antes de excluir.`)
    }
  } catch (e) {
    toast.erro((e as Error).message)
  } finally {
    processandoTransferencia.value = false
  }
}

const filtrados = computed<Item[]>(() => {
  const termo = busca.value.trim()
  const lista = dados.value[aba.value]
  const base = somenteExcecoes.value ? lista.filter((i) => Boolean(i.casoIsolado)) : lista
  if (!termo) return base
  return base.filter((i) =>
    casaComTermo(
      `${String(i.id ?? '')} ${String(i.nome ?? '')} ${String(i.descricao ?? '')} ${String(i.sigla ?? '')} ${String(i.codigoFornecedorSAP ?? '')}`,
      termo
    )
  )
})

const totalPaginas = computed(() => Math.max(1, Math.ceil(filtrados.value.length / porPagina)))

const ordem = ref<{ chave: string; direcao: 1 | -1 } | null>(null)

function ordenarPor(chave: string): void {
  // ciclo de 3 estados: primeira direção -> direção oposta -> sem ordenação
  const primeira = chave === 'createdAt' ? -1 : 1
  const segunda = primeira === 1 ? -1 : 1
  const atual = ordem.value
  if (!atual || atual.chave !== chave) {
    ordem.value = { chave, direcao: primeira }
  } else if (atual.direcao === primeira) {
    ordem.value = { chave, direcao: segunda }
  } else {
    ordem.value = null
  }
  pagina.value = 0
}

function comparar(a: Item, b: Item, chave: string): number {
  const va = a[chave]
  const vb = b[chave]
  const vazio = (v: unknown): boolean => v === null || v === undefined || v === ''
  if (vazio(va)) return vazio(vb) ? 0 : -1
  if (vazio(vb)) return 1
  if (chave === 'createdAt') return new Date(String(va)).getTime() - new Date(String(vb)).getTime()
  const na = Number(va)
  const nb = Number(vb)
  if (String(va).trim() !== '' && String(vb).trim() !== '' && Number.isFinite(na) && Number.isFinite(nb)) {
    return na - nb
  }
  return String(va).localeCompare(String(vb), 'pt-BR', { sensitivity: 'base', numeric: true })
}

const ordenados = computed<Item[]>(() => {
  const o = ordem.value
  if (!o) return filtrados.value
  return [...filtrados.value].sort((a, b) => comparar(a, b, o.chave) * o.direcao)
})

const paginaVisivel = computed<Item[]>(() =>
  ordenados.value.slice(pagina.value * porPagina, pagina.value * porPagina + porPagina)
)

function valor(item: Item, chave: string): string {
  const v = item[chave]
  if (v === null || v === undefined || v === '') return '—'
  if (chave === 'createdAt') return formatarData(String(v))
  return String(v)
}

function formatarData(iso: string): string {
  const d = new Date(iso)
  return isNaN(d.getTime()) ? iso : d.toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })
}

function rotuloStatus(status: unknown): string {
  const s = String(status || 'ATIVO').toUpperCase()
  return s === 'PENDENTE_VALIDACAO'
    ? 'Pendente de aprovação'
    : s === 'REJEITADO'
      ? 'Rejeitado'
      : s === 'DESATIVADO'
        ? 'Desativado'
        : 'Ativo'
}

function classeStatus(status: unknown): string {
  const s = String(status || 'ATIVO').toUpperCase()
  return s === 'PENDENTE_VALIDACAO'
    ? 'badge-alertar'
    : s === 'REJEITADO'
      ? 'badge-revisar'
      : s === 'DESATIVADO'
        ? 'badge-desativado'
        : 'badge-permitir'
}

async function carregarAba(): Promise<void> {
  if (carregado.value[aba.value]) return
  try {
    
    dados.value[aba.value] = (await abaAtual.value.carregar('', 20000, true)) as Item[]
    carregado.value[aba.value] = true
  } catch (e) {
    toast.erro((e as Error).message)
  }
}

function trocarAba(chave: ChaveAba): void {
  aba.value = chave
  pagina.value = 0
  busca.value = ''
  ordem.value = null
  selecionados.value = []
  falhas.value = []
  void carregarAba()
}

watch(busca, () => {
  pagina.value = 0
})



function estaSelecionado(item: Item): boolean {
  return selecionados.value.some((s) => String(s.id) === String(item.id))
}

function alternarSelecao(item: Item): void {
  const i = selecionados.value.findIndex((s) => String(s.id) === String(item.id))
  if (i >= 0) selecionados.value.splice(i, 1)
  else selecionados.value.push(item)
}

// botões de ação só aparecem quando há itens marcados e sempre no estado certo:
// desativar vale para os em circulação; reativar, para os já desativados
const ehDesativado = (i: Item): boolean => String(i.status ?? 'ATIVO').toUpperCase() === 'DESATIVADO'
const selecionadosAtivos = computed<Item[]>(() => selecionados.value.filter((i) => !ehDesativado(i)))
const selecionadosDesativados = computed<Item[]>(() => selecionados.value.filter((i) => ehDesativado(i)))

const todosPaginaSelecionados = computed(
  () => paginaVisivel.value.length > 0 && paginaVisivel.value.every(estaSelecionado)
)
const algumSelecionadoNaPagina = computed(() => paginaVisivel.value.some(estaSelecionado))

function alternarPaginaInteira(): void {
  if (todosPaginaSelecionados.value) {
    for (const item of paginaVisivel.value) {
      const i = selecionados.value.findIndex((s) => String(s.id) === String(item.id))
      if (i >= 0) selecionados.value.splice(i, 1)
    }
  } else {
    for (const item of paginaVisivel.value) {
      if (!estaSelecionado(item)) selecionados.value.push(item)
    }
  }
}


async function executarSituacao(): Promise<void> {
  const situacao = confirmando.value
  if (!situacao) return
  confirmando.value = null
  alterando.value = true
  try {
    const alvos = situacao === 'DESATIVADO' ? selecionadosAtivos.value : selecionadosDesativados.value
    const ids = alvos.map((i) => String(i.id))
    const relatorio = await api.situacaoComponentes(aba.value as TipoExclusao, ids, situacao)
    const ok = relatorio.filter((r) => r.alterado)
    falhas.value = relatorio.filter((r) => !r.alterado).map((r) => ({ id: r.id, motivo: r.motivo }))
    if (ok.length) {
      toast.sucesso(
        situacao === 'DESATIVADO'
          ? `${ok.length} item(ns) desativado(s) — fora de circulação, dá para reativar quando quiser.`
          : `${ok.length} item(ns) reativado(s) — de volta às listas de uso.`
      )
    }
    if (falhas.value.length) {
      toast.aviso(`${falhas.value.length} item(ns) mantido(s) — veja o relatório na tela.`)
    }
    selecionados.value = []
    carregado.value[aba.value] = false
    await carregarAba()
  } catch (e) {
    toast.erro((e as Error).message)
  } finally {
    alterando.value = false
  }
}


const modalExportar = ref(false)
const exportarTabela = ref<ChaveAba | 'TODAS'>('TODAS')

const ARQUIVO_TABELA: Record<ChaveAba, string> = {
  GS: 'grupos_servico',
  CF: 'codigos_fornecedor',
  CS: 'codigos_servico',
  UM: 'unidades_medida'
}

function abrirExportacao(): void {
  exportarTabela.value = aba.value
  modalExportar.value = true
}


function baixarCsv(nome: string, linhas: string[][]): void {
  const csv = linhas.map((l) => l.map((c) => `"${String(c).replace(/"/g, '""')}"`).join(';')).join('\n')
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = nome
  a.click()
  URL.revokeObjectURL(url)
}

function linhasDaTabela(chave: ChaveAba): string[][] {
  const config = abas.find((a) => a.chave === chave) as Aba
  const cabecalho = [...config.colunas.map((c) => c.rotulo), 'Status']
  const linhas = dados.value[chave].map((item) => [
    ...config.colunas.map((c) => String(item[c.chave] ?? '')),
    rotuloStatus(item.status)
  ])
  return [cabecalho, ...linhas]
}

async function executarExportacao(): Promise<void> {
  const alvos: ChaveAba[] = exportarTabela.value === 'TODAS' ? abas.map((a) => a.chave) : [exportarTabela.value]
  modalExportar.value = false
  for (const chave of alvos) {
    if (!carregado.value[chave]) {
      try {
        dados.value[chave] = (await (abas.find((a) => a.chave === chave) as Aba).carregar('', 20000, true)) as Item[]
        carregado.value[chave] = true
      } catch {
        continue
      }
    }
    baixarCsv(`${ARQUIVO_TABELA[chave]}.csv`, linhasDaTabela(chave))
    
    if (alvos.length > 1) await new Promise((r) => setTimeout(r, 350))
  }
  toast.sucesso(
    alvos.length > 1 ? `${alvos.length} arquivos exportados (um por tabela).` : `Arquivo ${ARQUIVO_TABELA[alvos[0]]}.csv exportado.`
  )
}


const modalImportar = ref(false)
const importarTabela = ref<ChaveAba>('GS')
const itensImportacao = ref<ItemImportacao[]>([])
const importando = ref(false)
const ultimaImportacao = ref<{ tabela: ChaveAba; rotulo: string; ids: string[] } | null>(null)
const confirmandoDesfazer = ref(false)

function rotuloItemImportado(item: ItemImportacao): string {
  return String(item.nome || item.descricao || item.sigla || item.codigoSap || item.id || '')
}

function jaExisteNoCatalogo(tipo: ChaveAba, item: ItemImportacao, existentes: Item[]): boolean {
  const alvo = (v: unknown) => normalizar(String(v ?? '').trim())
  const id = String(item.id ?? '').trim()
  const sap = String(item.codigoSap ?? id).trim()
  const descricao = String(item.descricao ?? item.nome ?? '').trim()
  return existentes.some((e) => {
    switch (tipo) {
      case 'GS':
        return alvo(e.nome) === alvo(item.nome) || String(e.id).toUpperCase() === alvo(item.nome).replace(/ /g, '_').toUpperCase()
      case 'CF':
        return (sap && String(e.id) === sap) || (!sap && alvo(e.nome) === alvo(item.nome))
      case 'CS':
        return alvo(e.descricao) === alvo(descricao)
      default:
        return String(e.id).toUpperCase() === String(item.sigla ?? item.id ?? '').trim().toUpperCase()
    }
  })
}

const previaImportacao = computed(() => {
  const existentes = dados.value[importarTabela.value] ?? []
  const novos: ItemImportacao[] = []
  const repetidos: ItemImportacao[] = []
  for (const item of itensImportacao.value) {
    ;(jaExisteNoCatalogo(importarTabela.value, item, existentes) ? repetidos : novos).push(item)
  }
  return { novos, repetidos }
})

const dicaColunas = computed(
  () =>
    ({
      GS: 'Colunas aceitas: id; nome; origem; status — obrigatória: nome.',
      CF: 'Colunas aceitas: id; codigoSap; nome; origem; status — obrigatória: nome (recomendado: codigoSap).',
      CS: 'Colunas aceitas: id; descricao; caracteres; origem; status — obrigatória: descricao (máx. 40 caracteres).',
      UM: 'Colunas aceitas: id; sigla; nome; origem; status — obrigatória: sigla (nome é opcional).'
    })[importarTabela.value]
)

function abrirImportacao(): void {
  importarTabela.value = aba.value
  modalImportar.value = true
  limparImportacao()
}

function limparImportacao(): void {
  itensImportacao.value = []
  const campo = document.getElementById('arquivo-importar') as HTMLInputElement | null
  if (campo) campo.value = ''
}


function parseCsv(texto: string): string[][] {
  const limpo = texto.replace(/^\uFEFF/, '').replace(/\r\n/g, '\n').replace(/\r/g, '\n')
  const primeiraLinha = limpo.split('\n')[0] || ''
  const separador = (primeiraLinha.match(/;/g)?.length ?? 0) >= (primeiraLinha.match(/,/g)?.length ?? 0) ? ';' : ','
  const linhas: string[][] = []
  let linha: string[] = []
  let campo = ''
  let aspas = false
  for (let i = 0; i < limpo.length; i++) {
    const c = limpo[i]
    if (aspas) {
      if (c === '"') {
        if (limpo[i + 1] === '"') {
          campo += '"'
          i++
        } else {
          aspas = false
        }
      } else {
        campo += c
      }
    } else if (c === '"') {
      aspas = true
    } else if (c === separador) {
      linha.push(campo)
      campo = ''
    } else if (c === '\n') {
      linha.push(campo)
      campo = ''
      if (linha.some((v) => v.trim() !== '')) linhas.push(linha)
      linha = []
    } else {
      campo += c
    }
  }
  linha.push(campo)
  if (linha.some((v) => v.trim() !== '')) linhas.push(linha)
  return linhas
}

function normalizarCabecalho(c: string): string {
  return c
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]/g, '')
}

const CAMPOS_CABECALHO: Record<string, keyof ItemImportacao> = {
  id: 'id',
  codigo: 'id',
  chave: 'id',
  nome: 'nome',
  name: 'nome',
  descricao: 'descricao',
  description: 'descricao',
  sigla: 'sigla',
  abreviacao: 'sigla',
  codigosap: 'codigoSap',
  sap: 'codigoSap',
  codigofornecedor: 'codigoSap',
  codigofornecedorsap: 'codigoSap'
}

const ASSINATURA_TABELA: { campo: string; tabela: ChaveAba }[] = [
  { campo: 'sigla', tabela: 'UM' },
  { campo: 'codigosap', tabela: 'CF' },
  { campo: 'sap', tabela: 'CF' },
  { campo: 'codigofornecedor', tabela: 'CF' },
  { campo: 'codigofornecedorsap', tabela: 'CF' },
  { campo: 'descricao', tabela: 'CS' },
  { campo: 'nome', tabela: 'GS' }
]

function rotuloDaTabela(chave: ChaveAba): string {
  return (abas.find((a) => a.chave === chave) as Aba).rotulo
}

function detectarTabelaDoArquivo(cabecalho: string[]): ChaveAba | null {
  const campos = new Set(cabecalho.map(normalizarCabecalho))
  for (const assinatura of ASSINATURA_TABELA) {
    if (campos.has(assinatura.campo)) return assinatura.tabela
  }
  return null
}

function paraItensImportacao(tipo: ChaveAba, matriz: string[][]): ItemImportacao[] {
  const cabecalho = matriz[0] || []
  const posicoes: Partial<Record<keyof ItemImportacao, number>> = {}
  cabecalho.forEach((coluna, i) => {
    const campo = CAMPOS_CABECALHO[normalizarCabecalho(coluna)]
    if (campo && posicoes[campo] === undefined) posicoes[campo] = i
  })
  const obrigatorio: Record<ChaveAba, keyof ItemImportacao> = { GS: 'nome', CF: 'nome', CS: 'descricao', UM: 'sigla' }
  const exigido = obrigatorio[tipo]
  if (posicoes[exigido] === undefined) {
    throw new Error(`Cabeçalho sem a coluna “${exigido}”. Exporte a tabela antes para ver o formato exato.`)
  }
  return matriz.slice(1).map((linha) => {
    const celula = (campo: keyof ItemImportacao): string | null => {
      const i = posicoes[campo]
      return i === undefined ? null : (linha[i] ?? '').trim() || null
    }
    return {
      id: celula('id'),
      nome: celula('nome'),
      descricao: celula('descricao'),
      sigla: celula('sigla'),
      codigoSap: celula('codigoSap')
    }
  })
}

async function lerArquivoImportacao(event: Event): Promise<void> {
  const alvo = event.target as HTMLInputElement
  const arquivo = alvo.files?.[0]
  itensImportacao.value = []
  if (!arquivo) return
  try {
    const matriz = parseCsv(await arquivo.text())
    if (matriz.length < 2) {
      throw new Error('O arquivo precisa do cabeçalho e de pelo menos uma linha de dados.')
    }
    const tabelaDoArquivo = detectarTabelaDoArquivo(matriz[0] || [])
    if (tabelaDoArquivo && tabelaDoArquivo !== importarTabela.value) {
      throw new Error(
        `Este arquivo parece ser de ${rotuloDaTabela(tabelaDoArquivo)}, mas a tabela escolhida é ` +
          `${rotuloDaTabela(importarTabela.value)}. Troque a tabela ou envie o arquivo certo — nada foi importado.`
      )
    }
    if (!carregado.value[importarTabela.value]) {
      const config = abas.find((a) => a.chave === importarTabela.value) as Aba
      dados.value[importarTabela.value] = (await config.carregar('', 20000, true)) as Item[]
      carregado.value[importarTabela.value] = true
    }
    itensImportacao.value = paraItensImportacao(importarTabela.value, matriz)
    const { novos, repetidos } = previaImportacao.value
    toast.aviso(
      `${itensImportacao.value.length} linha(s) lida(s): ${novos.length} nova(s) entra(m), ` +
        `${repetidos.length} já existe(m) e não ${repetidos.length === 1 ? 'muda' : 'mudam'}.`
    )
  } catch (e) {
    toast.erro((e as Error).message)
  }
}

async function executarImportacao(): Promise<void> {
  importando.value = true
  try {
    const r = await api.importarCatalogo(importarTabela.value, itensImportacao.value)
    const novos = r.adicionados
    const mantidos = r.ignorados.length
    if (novos && mantidos) {
      toast.sucesso(
        `Importação concluída: ${novos} novo(s) adicionado(s) · ${mantidos} já existiam e foram mantidos como estavam.`
      )
    } else if (novos) {
      toast.sucesso(`${novos} item(ns) adicionado(s) a ${abaAtual.value.rotulo.toLowerCase()}.`)
    } else {
      toast.aviso(
        `Nada novo: ${mantidos} linha(s) já ${mantidos === 1 ? 'existia e foi mantida' : 'existiam e foram mantidas'} como estavam.`
      )
    }
    const foraDoPadrao = r.ignorados.find((f) => !/mantid/i.test(f.motivo))
    if (foraDoPadrao) {
      toast.aviso(`Linha ${foraDoPadrao.linha} não entrou: ${foraDoPadrao.motivo}`)
    }
    ultimaImportacao.value = novos
      ? { tabela: importarTabela.value, rotulo: rotuloDaTabela(importarTabela.value), ids: r.idsAdicionados }
      : null
    modalImportar.value = false
    limparImportacao()
    carregado.value[importarTabela.value] = false
    if (importarTabela.value === aba.value) await carregarAba()
  } catch (e) {
    toast.erro((e as Error).message)
  } finally {
    importando.value = false
  }
}

async function desfazerImportacao(): Promise<void> {
  const alvo = ultimaImportacao.value
  confirmandoDesfazer.value = false
  if (!alvo) return
  try {
    const relatorio = await api.exclusaoEmMassa(alvo.tabela, alvo.ids)
    const ok = relatorio.filter((r) => r.excluido)
    if (ok.length) {
      toast.sucesso(`Importação desfeita: ${ok.length} item(ns) removido(s) — estado anterior restaurado.`)
    }
    const naoRemovidos = relatorio.filter((r) => !r.excluido)
    if (naoRemovidos.length) {
      toast.aviso(`${naoRemovidos.length} item(ns) não puderam ser removidos (${naoRemovidos[0].motivo}).`)
    }
    ultimaImportacao.value = null
    carregado.value[alvo.tabela] = false
    if (alvo.tabela === aba.value) await carregarAba()
  } catch (e) {
    toast.erro((e as Error).message)
  }
}

onMounted(async () => {
  await carregarAba()
  
  for (const a of abas) {
    if (a.chave === aba.value || carregado.value[a.chave]) continue
    a.carregar('', 20000, true)
      .then((itens) => {
        dados.value[a.chave] = itens as Item[]
        carregado.value[a.chave] = true
      })
      .catch(() => {})
  }
})
</script>

<script lang="ts">
export default { name: 'CatalogoView' }
</script>

<style scoped>
.abas {
  display: flex;
  gap: 8px;
  border-bottom: 2px solid var(--borda);
  padding-bottom: 10px;
  flex-wrap: wrap;
}
.aba {
  background: transparent;
  border: 1px solid transparent;
  border-radius: 9px;
  padding: 10px 18px;
  font-size: 0.93rem;
  font-weight: 700;
  color: var(--texto-suave);
  cursor: pointer;
}
.aba:hover {
  color: var(--azul);
}
.aba.ativa {
  background: var(--azul);
  color: var(--texto-sobre-azul);
}
.aba-contagem {
  margin-left: 6px;
  font-size: 0.76rem;
  opacity: 0.85;
  font-variant-numeric: tabular-nums;
}
.th-ordenable {
  cursor: pointer;
  user-select: none;
  white-space: nowrap;
}
.th-ordenable:hover { color: var(--azul); }
.seta-ordenacao { font-size: 0.62rem; margin-left: 4px; }
.botao-filtro-excecao {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 8px 14px;
  border-radius: 999px;
  border: 1.5px solid var(--borda);
  background: transparent;
  color: var(--texto-suave);
  font-size: 0.85rem;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
}
.botao-filtro-excecao:hover {
  border-color: var(--amarelo-alerta);
  color: var(--texto);
}
.botao-filtro-excecao.ativo {
  background: var(--amarelo-fundo);
  border-color: var(--amarelo-alerta);
  color: var(--texto);
  box-shadow: 0 0 0 3px rgba(255, 199, 44, 0.18);
}
.botao-filtro-excecao .contagem {
  font-size: 0.75rem;
  background: var(--amarelo);
  color: var(--texto-forte);
  border-radius: 999px;
  padding: 1px 8px;
  font-weight: 800;
}
button.badge-excecao {
  cursor: pointer;
}
button.badge-excecao:hover {
  transform: scale(1.12);
  box-shadow: 0 2px 8px rgba(255, 199, 44, 0.55);
}
.btn-marcar-excecao {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  padding: 0;
  margin-left: 6px;
  border: 1.5px dashed var(--borda);
  border-radius: 50%;
  background: transparent;
  color: var(--texto-suave);
  font-size: 0.8rem;
  font-weight: 900;
  line-height: 1;
  cursor: pointer;
  vertical-align: middle;
}
.btn-marcar-excecao:hover {
  border-color: var(--amarelo-alerta);
  border-style: solid;
  color: var(--texto-forte);
  background: var(--amarelo);
}
.lista-alteracoes {
  max-height: 55vh;
  overflow-y: auto;
  border: 1px solid var(--borda);
  border-radius: 10px;
}
.linha-alteracao {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border-bottom: 1px solid var(--borda);
  font-size: 0.86rem;
}
.linha-alteracao:last-child { border-bottom: none; }
.id-alteracao { white-space: nowrap; }
.motivo-alteracao {
  flex: 1;
  color: var(--texto-suave);
  min-width: 0;
  overflow-wrap: anywhere;
}
.data-alteracao {
  color: var(--texto-suave);
  font-size: 0.78rem;
  white-space: nowrap;
}
.linha-excecao {
  background: var(--amarelo-fundo);
  box-shadow: inset 5px 0 0 var(--amarelo);
}
.legenda-excecao {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 12px 2px 0;
  font-size: 0.82rem;
  line-height: 1.5;
}
.campo-busca {
  flex: 1;
  min-width: 240px;
  background: var(--campo);
  border: 1px solid var(--borda);
  border-radius: 9px;
  color: var(--texto);
  padding: 9px 12px;
}
.id-codigo {
  font-size: 0.78rem;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: var(--texto-suave);
  font-variant-numeric: tabular-nums;
}
.badge-desativado {
  background: var(--fundo);
  color: var(--texto-suave);
  border: 1px solid var(--borda);
}
.tabela input[type='checkbox'] {
  width: 16px;
  height: 16px;
  accent-color: var(--azul);
  cursor: pointer;
}
</style>
