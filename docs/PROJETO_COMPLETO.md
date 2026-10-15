# Documentação Técnica Completa — Gerdau · Padronização de Códigos de Serviço

> Documento de referência para o time: o que é o projeto, **todas as decisões técnicas**,
> **o que é cada arquivo** e **como chegamos ao produto final**.

---

## 1. Visão geral

O sistema resolve o problema do cadastro descentralizado de códigos de serviço no SAP:
milhares de registros duplicados, sem padrão e sem controle de entrada. A solução tem
três frentes integradas:

1. **Saneamento** (Time de IA) — a base histórica foi tratada e mapeada em uma matriz
   de-para (código legado → código canônico, com score de similaridade);
2. **API Java** — catálogo oficial + **filtro de entrada anti-duplicidade** com
   governança (bloqueio, justificativa auditada, validação humana);
3. **Interface web** — fluxo completo para o comprador (vincular serviços, cadastrar
   itens) e painel admin (governança + indicadores com exportação).

**Resultado mensurável**: 9.795 registros legados → **7.308 serviços únicos (−25,4%)**,
com rastreabilidade total (nenhum legado é apagado).

---

## 2. Arquitetura

```
┌──────────────────────────────────────────────┐
│ Frontend (Vue 3 + TypeScript + Vite)         │
│  Vinculação · Cadastro · Painel Admin        │
│  (governança + detalhes/gráficos/exportação) │
└───────────────────┬──────────────────────────┘
                    │  HTTP JSON — /api/v1
┌───────────────────▼──────────────────────────┐
│ Backend (Spring Boot 4.1.1 / Java 17)        │
│  controllers → services → repositories       │
│  Preview · Governança · Auditoria ·          │
│  Idempotência · Permissões · Health checks   │
│          │                                   │
│   SearchClient (abstração de busca)          │
│    └─ motor local Java (port fiel do motor   │
│       Python do Time de IA)                  │
└────────┬──────────────────────┬──────────────┘
         │                      │
┌────────▼─────────┐   ┌────────▼──────────────┐
│ MongoDB          │   │ Time de IA (IA/)      │
│ matriz_depara    │◄──┤ base_saneada.xlsx ×   │
│ + 5 coleções     │   │ matriz_de_para.xlsx   │
│ + excecoes       │   │ (NÃO é alterado —     │
│ + eventos_auditoria│ │  somente leitura)     │
└──────────────────┘   └───────────────────────┘
```

---

## 3. Decisões técnicas (e por quê)

| # | Decisão | Justificativa |
|---|---|---|
| 1 | **MongoDB** (Atlas ou local via Docker) | Domínio de catálogos documentais; flexibilidade de schema (Doc §5) |
| 2 | **Merge `base_saneada.xlsx` × `matriz_de_para_gerdau.xlsx`** como fonte do banco | São os dados oficiais do Time de IA; o documento mesclado é a imagem acordada com o time |
| 3 | **IDs oficiais** do Time de IA como identidade (`CS-130835`, `200982674`, `SV`, `CIVIL`) | Alinha aplicação, matriz de-para e o banco existente; legados preservados em `legacyIds`/`legacyData` |
| 4 | **`idCompleto`** = `GS_CF_CS_UM` (composição única) | Doc §8.3 — composição dos 4 componentes; índice único no Mongo |
| 5 | **Motor de similaridade**: 40% Jaccard de tokens + 60% Ratcliff/Obershelp | Port fiel do `IA/src/fuzzy_engine.py`; valores validados nos dois lados (paridade) |
| 6 | **Limiares 0,88 / 0,70** configuráveis por ambiente | Calibragem da trilha IA sobre a base real (Doc §6.7 sugeria 0,90/0,75 como ponto de partida) |
| 7 | **Regra numérica** (medidas diferentes limitam score a 0,85) | Evita casar `ELETRODUTO 1/2` com `ELETRODUTO 2 1/2` |
| 8 | **`SearchClient` abstraído** (pluggável) | Doc §4.2 — trocar o motor (Atlas Search/OpenSearch futuramente) não muda contratos |
| 9 | **Governança**: PERMITIR/ALERTAR/REVISAR + fila humana pré-carregada | Filtro de entrada do TAP; os 4.383 casos reais entram como evidência para revisão |
| 10 | **Idempotência** com `Idempotency-Key` + hash SHA-256 do conteúdo | Duplo clique/retentativa não duplica; mesma chave com conteúdo diferente → 409 (Doc §10.3) |
| 11 | **Auditoria completa** (`eventos_auditoria`) | Toda criação/decisão/bloqueio com ator, timestamp UTC, candidatos, scores e `correlationId` (Doc §5/§9) |
| 12 | **Perfis USUÁRIO × ADMIN** com bloqueio no servidor (403) | "Esconder botão não é segurança" (Doc §12.2) |
| 13 | **Sequência atômica** (`contadores`) para novos C.S. | Sem corrida/ID reutilizado; erro explícito se a faixa esgotar (Doc §5) |
| 14 | **Índices** (Doc §14.3 + padrões de consulta reais) | Único em `idCompleto` e `idempotencyKey`; text index em português; compostos para a fila de governança |
| 15 | **Soft state** (`active`, `createdAt`, `updatedAt`) | Nada é apagado fisicamente — referências preservadas (Doc §5) |
| 16 | **Nomes de rotas/campos em português** | Alinhados ao Doc §7/§10.1 e aos nomes das coleções do banco |
| 17 | **Docker Compose** com seed automático | "Clone e rode" em qualquer máquina; `MONGODB_URI` sobrescreve o destino |
| 18 | **TypeScript** no frontend + testes de interface | Qualidade e refatoração segura |
| 19 | **Toasts** no lugar de `alert()` e tema claro/escuro | Experiência de uso profissional |
| 20 | **Sem integrações fakes** | SAP/OIDC/Atlas Search declarados fora do escopo, sem simulação |

---

## 4. O que é cada arquivo

### 4.1 Backend (`JAVA/backend/desafio/`)

```
pom.xml                      Spring Boot 4.1.1, Lombok, POI (ler xlsx), springdoc (Swagger)
Dockerfile                   build multi-estágio (Maven → JRE 17)

src/main/java/com/gerdau/desafio/
├── DesafioApplication.java          bootstrap
├── config/
│   ├── CorsConfig.java              CORS para o dev local (5173/3000)
│   ├── SimilaridadeConfig.java      bean da RankingPolicy (limiares configuráveis)
│   ├── MongoIndexConfig.java        índices (§14.3) — idempotente, não derruba a app
│   ├── AutorizacaoInterceptor.java  exige ADMIN em /governanca/** (403)
│   ├── AutorizacaoConfig.java       registra o interceptor
│   └── CorrelationIdFilter.java     X-Correlation-Id por requisição
├── controller/                      contrato REST /api/v1 (ver docs/CONTRATO_API.md)
│   ├── GrupoServicoController       GET ?query= (autocomplete) + POST
│   ├── CodigoFornecedorController   idem
│   ├── CodigoServicoController      idem + limite SAP 40 no DTO
│   ├── UnidadeMedidaController      idem
│   ├── CodigosCompletosController   POST /preview · POST (governança) · GET
│   ├── SimilaridadeController       GET/POST /itens/similaridade?tipo=CS|GS|CF|UM
│   ├── GovernancaController         fila · resumo · estatísticas · fechamentos ·
│   │                                contagem · revisar · reverter · aprovar · rejeitar · exceções
│   ├── MatrizDeParaController       rastreabilidade legado↔canônico (§9.6)
│   └── HealthController             /health · /ready (com destino do banco)
├── dto/                             contratos de entrada/saída (records + validação)
├── exception/                       400/404/403/409/500 com code + correlationId
├── model/                           documentos Mongo (ver docs/MODELO_DADOS.md)
│   ├── MatrizDePara                 o documento MESCLADO (imagem do banco) + revisão
│   ├── GrupoServico/CodigoFornecedor/CodigoServico/UnidadeMedida   5 coleções do Doc §7
│   ├── CodigoCompleto               composição + governança + idempotência
│   ├── EventoAuditoria · Excecao · RegistroSimilar · Contador
├── repository/                      Spring Data + queries por regex/aggregation
├── search/
│   ├── SearchClient.java            abstração (Doc §4.2) — tipos CS/GS/CF/UM
│   ├── LocalSearchClient.java       motor Java, cache aquecido no startup
│   └── PythonSearchClient.java      opcional (SEARCH_ENGINE=python → sidecar IA)
├── seed/
│   ├── DataSeeder                   merge dos xlsx oficiais → coleções (idempotente)
│   ├── PlanilhaOficial              leitura POI (somente leitura das planilhas)
│   └── SchemaBackfill               completa active/createdAt/updatedAt
├── service/
│   ├── PreviewService               resolve componentes + análise de similaridade
│   │                                em TODOS os campos + decisão final
│   ├── CodigoCompletoService        criação com governança, idempotência, exceções
│   ├── GovernancaService            fila, revisões, fechamentos, estatísticas
│   ├── SimilaridadeService          consulta + política (PERMITIR/ALERTAR/REVISAR)
│   ├── CatalogoService              CRUD dos 4 componentes + anti-duplicidade
│   ├── AuditoriaService             eventos com ator/horário/correlationId
│   └── SequenceService              sequência atômica CS-XXXXXX (faixa explícita)
├── similarity/
│   ├── SimilarityEngine             port fiel do fuzzy Python (testado contra difflib)
│   ├── RankingEngine                ordenação determinística + motivos explicados
│   ├── RankingPolicy                0,88/0,70 → DUPLICIDADE/ALERTA/ORIGINAL
│   └── CatalogoItem                 item preparado (tokens/números)
└── util/TextoUtil                   normalização (minúsculas, acentos, pontuação) + SAP

src/test/java/                       23 testes (ver §7)
```

### 4.2 Frontend (`JAVA/frontend/`)

```
package.json / tsconfig.json / vitest.config.ts   Vue 3 + TS + Chart.js + testes
Dockerfile + nginx.conf                           build Node → nginx (proxy /api)
src/
├── main.ts                  rotas: / · /cadastrar-item · /admin (+ /governanca→redirect)
├── App.vue                  cabeçalho (usuário/data/perfil/tema/banco ativo) + toasts
├── style.css                design system (claro/escuro, variáveis CSS)
├── types.ts                 contratos TS da API
├── services/
│   ├── api.ts               cliente tipado (envia X-Usuario-Id/Papel/Correlation-Id)
│   ├── toast.ts             toasts globais (substituem alert())
│   └── tema.ts              tema reativo (gráficos acompanham)
├── components/
│   ├── ComboboxBusca.vue    seletor com busca (debounce 250ms, teclado, "＋ Cadastrar Novo Item" por último)
│   ├── ModalBloqueio.vue    tela de Bloqueio de Duplicidade (% Match + 3 saídas)
│   ├── ModalGovernanca.vue  Envio para Governança (justificativa)
│   ├── ModalConfirmacao.vue confirmação obrigatória em decisões
│   ├── GraficoRosca.vue     Chart.js doughnut (clique/legenda oculta/mostra)
│   ├── GraficoBarras.vue    Chart.js barras animadas (tooltip com valor real)
│   ├── RankingLista.vue     pódio de serviços mais consolidados
│   └── Toast.vue            fila visual de avisos
└── views/
    ├── VinculacaoView.vue   tela principal: 4 seletores + prévia + vínculo
    │                        + guia rápido (usuário) / resumo da base (admin)
    ├── CadastrarItemView.vue cadastro de item (contador SAP, verificação de similaridade)
    ├── AdminPainelView.vue  Painel Admin (abas Governança | Detalhes)
    ├── GovernancaView.vue   fila em cards + filtros + paginação + decisões/reverter
    └── DetalhesView.vue     gráficos, fechamentos por período, exportações CSV
tests/                       10 testes de interface (combobox, bloqueio, permissões)
```

### 4.3 Infraestrutura e documentação
```
docker-compose.yml    mongo + backend + frontend (seed automático)
.env / .env.example   configuração (MONGODB_URI = destino dos dados)
docs/                 COMO-RODAR · INSTALACAO_E_USO · ARQUITETURA · MODELO_DADOS
                      CONTRATO_API · PERMISSOES · DEMO · CONFORMIDADE · ENTREGA
                      PROJETO_COMPLETO (este) · analises/ (Sprint 2)
```

---

## 5. Fluxo de dados: das planilhas ao produto

```
IA/data/processed/base_saneada.xlsx        (9.795 linhas — dados tratados)
IA/data/processed/matriz_de_para_gerdau.xlsx (9.795 — newId/score/decision)
        │  DataSeeder (backend, somente leitura, no boot)
        ▼
   MESCLAGEM por legacyId (100% de integridade)
        ▼
matriz_depara (9.795 docs — o documento da imagem + statusRevisao)
grupos_servico (3) · codigos_fornecedor (111) · unidades_medida (17)
codigos_servico (7.308 canônicos, com legacyIds)
codigos_completos (7.554 composições únicas)
        ▼
   índices (§14.3) + cache de catálogo aquecido → buscas em ~30–100ms
        ▼
   API /api/v1 → Frontend (com governança e auditoria)
```

---

## 6. Como o produto foi evoluindo (caminho até aqui)

| Etapa | O que foi feito |
|---|---|
| **1. Diagnóstico** | Inventario do repositório; identificação do contrato do Doc §7/§10 nos dados do Time de IA |
| **2. Base oficial** | Descoberta de que `nosql/` era de testes e que o banco real é o **merge** `base_saneada × matriz` (imagem do time) — implementado no `DataSeeder` |
| **3. Backend core** | Modelos, repositórios, CRUD, motor de similaridade (port do Python), testes de paridade com `difflib` |
| **4. Governança** | Prévia → decisão → bloqueio/justificativa/envio; fila humana; `excecoes`; idempotência |
| **5. Proteção** | Perfis ADMIN/USUÁRIO com bloqueio no servidor; auditoria completa; health checks |
| **6. Frontend** | Telas fiéis ao protótipo; combobox com busca; modais de bloqueio/governança; Painel Admin |
| **7. Dados reais** | Carga idempotente + índices + `SequenceCounter` + `active/timestamps` + backfill |
| **8. Observabilidade** | Painel de Detalhes (gráficos animados), fechamentos por período, exportações CSV |
| **9. Qualidade** | 23 testes backend + 10 frontend; TypeScript; toasts; dark mode; Docker one-command |
| **10. Documentação** | Este documento + guias operacionais + análises da Sprint 2 + roteiro de demo |

---

## 7. Testes e qualidade

| Suíte | Comando | Cobertura |
|---|---|---|
| Backend (23) | `cd JAVA/backend/desafio && ./mvnw test` | motor (paridade difflib), política, ranking determinístico, **limite SAP (39/40/41)**, idempotência (mesmo/diferente conteúdo), auditoria (criação/bloqueio), contexto |
| Frontend (10) | `cd JAVA/frontend && npm test` | combobox (opções, "criar novo" por último, seleção, vazio), modal de bloqueio (% Match + ações), permissões (admin vê Painel Admin, usuário não) |
| Tipos | `npm run typecheck` | strict TypeScript |
| Build | `npm run build` / `./mvnw package` | produção |

---

## 8. Como rodar

```bash
docker compose up --build     # app em http://localhost · API em :8080 · Swagger em /swagger-ui.html
```

Detalhes, variáveis de ambiente e modo nativo: `docs/INSTALACAO_E_USO.md`.

---

## 9. Limitações e evoluções previstas

| Item | Estado |
|---|---|
| Integração SAP | Fora do escopo — fronteiras preparadas (DTOs/IDs) |
| OIDC corporativo | Modo demonstração (headers); evolução documentada em `docs/PERMISSOES.md` |
| Atlas Search / OpenSearch | `SearchClient` pronto para receber; hoje o motor local (determinístico e testado) |
| Calibração de limiares | 0,88/0,70 provisórios — recomenda-se amostra aprovada pela área |
| Transações multi-documento | MongoDB local sem transações distribuídas — limitação documentada |
| Testes E2E de interface | Testes unitários de componentes; sugere-se Playwright para E2E |
