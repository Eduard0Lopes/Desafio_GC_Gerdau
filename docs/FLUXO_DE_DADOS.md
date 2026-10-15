# Fluxo de Dados — Onde cada coisa é gravada no banco (linha por linha)

> Documento de referência completa: **toda escrita** no MongoDB — endpoint → serviço →
> coleção → **campo a campo, com a origem exata de cada valor**.

---

## 1. Mapa geral das coleções

| Coleção | Quem escreve | Quando |
|---|---|---|
| `matriz_depara` | `DataSeeder` (carga oficial) | primeira subida do backend |
| `grupos_servico` | `DataSeeder` + `POST /api/v1/grupos-servico` | carga / cadastro |
| `codigos_fornecedor` | `DataSeeder` + `POST /api/v1/codigos-fornecedor` | carga / cadastro |
| `codigos_servico` | `DataSeeder` + `POST /api/v1/codigos-servico` | carga / cadastro |
| `unidades_medida` | `DataSeeder` + `POST /api/v1/unidades-medida` | carga / cadastro |
| `codigos_completos` | `DataSeeder` + `POST /api/v1/codigos-completos` | carga / vínculo |
| `excecoes` | `CodigoCompletoService` | criação com justificativa |
| `eventos_auditoria` | `AuditoriaService` (todas as ações) | qualquer ação |
| `contadores` | `SequenceService` | geração de novo `CS-XXXXXX` |

---

## 2. Fonte oficial (Time de IA) — somente leitura

`IA/data/processed/` **nunca é alterado**. Duas planilhas alimentam tudo:

### `base_saneada.xlsx` (dados tratados — 9.795 linhas)
| Coluna | Tipo | Exemplo | Vira… |
|---|---|---|---|
| `Grupo de Serviço` | texto | `Civil` | `matriz_depara.grupoServico` + `grupos_servico.nome` |
| `Código do Fornecedor` | número | `200982674` | `legacyData.codigoFornecedor` + `codigos_fornecedor._id` |
| `Código do Serviço` | número | `4033776` | `legacyData.legacyId` (chave do merge) |
| `Descrição do Item` | texto | `SV-EQUIPE TELHADO` | `legacyData.legacyDescription` |
| `Unidade medida` | texto | `SV` | `matriz_depara.unidadeMedida` + `unidades_medida._id` |
| `DescricaoOriginal` | texto | `SV-EQUIPE TELHADO` | (referência do saneamento) |
| `DescricaoNormalizada` | texto | `sv equipe telhado` | base da comparação de similaridade |
| `QuantidadeCaracteres` | número | `17` | `legacyData.quantidadeCaracteres` |
| `AlertaSAP` | boolean | `false` | `legacyData.alertaSap` |
| `DuplicadaExata` | boolean | `false` | (análise do Time de IA) |

### `matriz_de_para_gerdau.xlsx` (IDs + similaridade — 9.795 linhas)
| Coluna | Tipo | Exemplo | Vira… |
|---|---|---|---|
| `legacyId` | texto | `4033776` | `legacyData.legacyId` (chave do merge) |
| `legacyDescription` | texto | `SV-EQUIPE TELHADO` | `legacyData.legacyDescription` |
| `legacySource` | texto | `ARQUIVO_SAP_GERDAU` | `legacyData.legacySource` |
| `newEntityType` | texto | `CS` | (tipo do novo ID) |
| `newId` | texto | `CS-130835` | `newId` + `codigos_servico._id` + `codigos_completos.csId` |
| `descricaoCanonica` | texto | `SV-EQUIPE TELHADO` | `descricaoCanonica` + `codigos_servico.descricao` |
| `score` | número | `0.95` | `score` + `codigos_completos.melhorScore` |
| `decision` | texto | `DUPLICIDADE` | `decision` + `codigos_completos.decisaoSimilaridade` |
| `reason` | texto | `Provável duplicidade…` | `reason` |

---

## 3. Carga inicial (`seed/DataSeeder.java`) — o merge oficial

Executa **uma vez**, no primeiro boot (idempotente — pula se `matriz_depara` já estiver populada).
Junção: `base["Código do Serviço"]` = `matriz["legacyId"]` (100% de integridade conferida).

### 3.1 `matriz_depara` — documento MESCLADO (1 por registro legado)
| Campo do documento | Origem |
|---|---|
| `_id` | ObjectId gerado pelo Mongo |
| `newId` | `matriz.newId` |
| `descricaoCanonica` | `matriz.descricaoCanonica` |
| `grupoServico` | `base.Grupo de Serviço` |
| `unidadeMedida` | `base.Unidade medida` |
| `decision` | `matriz.decision` |
| `score` | `matriz.score` |
| `reason` | `matriz.reason` |
| `legacyData.legacyId` | `matriz.legacyId` |
| `legacyData.legacyDescription` | `matriz.legacyDescription` |
| `legacyData.legacySource` | `matriz.legacySource` |
| `legacyData.codigoFornecedor` | `base.Código do Fornecedor` |
| `legacyData.alertaSap` | `base.AlertaSAP` |
| `legacyData.quantidadeCaracteres` | `base.QuantidadeCaracteres` |
| `statusRevisao` | **regra**: `PENDENTE` se `decision` ∈ {DUPLICIDADE, ALERTA}; senão `SEM_PENDENCIA` |
| `reviewedBy` / `reviewedAt` / `observacaoRevisao` | `null` (preenchidos pela governança) |

### 3.2 `grupos_servico` (distintos de `base.Grupo de Serviço`)
| Campo | Origem |
|---|---|
| `_id` | slug do nome (`Civil` → `CIVIL`) |
| `nome` | `base.Grupo de Serviço` |
| `nomeNormalizado` | `normalizar(nome)` |
| `origem` | `MIGRACAO` |
| `active` / `createdAt` / `updatedAt` | complementados pelo `SchemaBackfill` |

### 3.3 `codigos_fornecedor` (distintos de `base.Código do Fornecedor`)
| Campo | Origem |
|---|---|
| `_id` | código SAP como texto (`"200982674"`) |
| `codigoFornecedorSAP` | `base.Código do Fornecedor` (número) |
| `nome` | `"Fornecedor <sap>"` |
| `nomeNormalizado` | `normalizar(nome)` |
| `origem` | `MIGRACAO` |

### 3.4 `unidades_medida` (distintos de `base.Unidade medida`)
| Campo | Origem |
|---|---|
| `_id` / `sigla` | `base.Unidade medida` em maiúsculas (`"SV"`) |
| `nome` | nome padrão da unidade (`SV` → `Serviço`) |
| `nomeNormalizado` | `normalizar(nome)` |
| `origem` | `MIGRACAO` |

### 3.5 `codigos_servico` (1 por `newId` — 7.308 canônicos)
| Campo | Origem |
|---|---|
| `_id` | `matriz.newId` (`CS-130835`) |
| `descricao` | `matriz.descricaoCanonica` |
| `descricaoNormalizada` | `normalizar(descricao)` |
| `quantidadeCaracteres` | `descricao.length()` |
| `origem` | `MIGRACAO` |
| `legacyIds` | todos os `legacyId` que apontam para este `newId` |

### 3.6 `codigos_completos` (composições distintas G.S.+C.F.+C.S.+U.M.)
| Campo | Origem |
|---|---|
| `_id` / `idCompleto` | `gsId_cfId_csId_umId` (composição única) |
| `gsId` | slug do grupo |
| `cfId` | código SAP do fornecedor |
| `csId` | `newId` |
| `umId` | sigla da unidade |
| `descricaoSnapshot` | `"{grupo} | Fornecedor {sap} | {descrição} | {sigla}"` |
| `origem` | `MIGRACAO` |
| `legacyIds` | `[legacyId]` de origem |
| `status` | `ATIVO` |
| `decisaoSimilaridade` | `matriz.decision` |
| `melhorScore` | `matriz.score` |

---

## 4. Escritas por endpoint (linha por linha)

### 4.1 `POST /api/v1/grupos-servico` → `grupos_servico`
`_id` = slug do nome · `nome` = digitado · `nomeNormalizado` = `normalizar(nome)` · `origem` = `SISTEMA` ·
`active` = `true` · `createdAt`/`updatedAt` = agora (UTC). **+ evento** `CRIACAO_COMPONENTE`.
*(Bloqueia com 409 se o `_id` já existir — nunca sobrescreve.)*

### 4.2 `POST /api/v1/codigos-fornecedor` → `codigos_fornecedor`
`_id`/`codigoFornecedorSAP` = código informado · `nome` = digitado · `nomeNormalizado` · `origem` = `SISTEMA` ·
`active` · timestamps. **+ evento** `CRIACAO_COMPONENTE`. *(409 se duplicado.)*

### 4.3 `POST /api/v1/codigos-servico` → `codigos_servico`
`_id` = **sequência atômica** `CS-XXXXXX` (coleção `contadores`, pulando IDs ocupados) ·
`descricao` = digitada (validada ≤ 40 — regra SAP) · `descricaoNormalizada` · `quantidadeCaracteres` ·
`origem` = `SISTEMA` · `legacyIds` = `[]` · `active` · timestamps. **+ evento** `CRIACAO_COMPONENTE`.

### 4.4 `POST /api/v1/unidades-medida` → `unidades_medida`
`_id`/`sigla` = sigla em maiúsculas · `nome` · `nomeNormalizado` · `origem` = `SISTEMA` · `active` · timestamps.
**+ evento** `CRIACAO_COMPONENTE`. *(409 se duplicada.)*

### 4.5 `POST /api/v1/codigos-completos` → `codigos_completos` (+ extras)
1. Componentes novos são criados (4.1–4.4) e recebem seus IDs oficiais;
2. `codigos_completos`:
   `_id`/`idCompleto` = composição · `gsId`/`cfId`/`csId`/`umId` · `descricaoSnapshot` ·
   `usuarioCriacaoId` (header `X-Usuario-Id`) · `createdAt` = agora · `origem` = `APLICACAO` ·
   `legacyIds` = `[]` · `status` = `ATIVO` (`acao=CRIAR`) ou `PENDENTE_VALIDACAO` (`acao=ENVIAR_VALIDACAO`) ·
   `decisaoSimilaridade`/`melhorScore` = da análise · `justificativa` = informada ·
   `similaresRegistrados` = instantâneo dos candidatos (para o aprovador ver a mesma evidência) ·
   `idempotencyKey`/`idempotencyHash` (header `Idempotency-Key` + SHA-256 do conteúdo);
3. **+ evento** `CRIACAO_CODIGO_COMPLETO`;
4. se criado com justificativa (apesar do alerta): **+ registro em `excecoes`** e **+ evento** `EXCECAO`;
5. se bloqueado (409): **+ evento** `BLOQUEIO` (nada é gravado na entidade).

### 4.6 `POST /api/v1/governanca/historico/{id}/revisar` → `matriz_depara`
`statusRevisao` = `REVISADO` · `reviewedBy` = responsável · `reviewedAt` = agora ·
`observacaoRevisao` = ação + observação. **+ evento** `REVISAO_HISTORICA`.

### 4.7 `POST /api/v1/governanca/historico/{id}/reverter` → `matriz_depara`
`statusRevisao` = `PENDENTE` · `observacaoRevisao` = "Decisão revertida por X — motivo".
**+ evento** `REVISAO_HISTORICA`. *(Decisões anteriores ficam nos eventos — nada é apagado.)*

### 4.8 `POST /api/v1/governanca/pendentes/{id}/aprovar` → `codigos_completos`
`status` = `ATIVO` · `aprovadoPor` · `observacaoAprovacao` · `aprovadoEm` = agora.
**+ evento** `APROVACAO`.

### 4.9 `POST /api/v1/governanca/pendentes/{id}/rejeitar` → `codigos_completos`
`status` = `REJEITADO` · `aprovadoPor` · `observacaoAprovacao` · `aprovadoEm`.
**+ evento** `REJEICAO`.

---

## 5. Registros transversais

### `eventos_auditoria` (toda ação acima)
`actorId` (header `X-Usuario-Id`) · `action` · `entityType` · `entityId` · `occurredAt` (UTC, do servidor) ·
`decision` · `reason` · `candidateIds` · `similarityScores` · `correlationId` (header `X-Correlation-Id` ou gerado).

### `excecoes` (criação forçada com justificativa — seção 12.3)
`solicitante` · `dataHora` · `payloadOriginal` · `similaresEncontrados` · `justificativa` ·
`decisaoTomada` · `observacao` · `codigoCompletoId`.

### `contadores` (sequência atômica de IDs)
`_id` = `codigos_servico` · `valor` = último número entregue. IDs ocupados por dados migrados são
**pulados** (nunca reutilizados); faixa esgotada → erro de domínio explícito.

### `SchemaBackfill` (rodada idempotente no boot)
Completa `active=true` · `createdAt` · `updatedAt` nos documentos migrados que não os possuem.

---

## 6. Índices (seção 14.3 + consultas reais)

| Coleção | Índices |
|---|---|
| `codigos_completos` | **único** `idCompleto` · **único sparse** `idempotencyKey` · `gsId` · `cfId` · `csId` · `umId` · `status+createdAt` · `origem` |
| `codigos_servico` | `descricaoNormalizada` · `origem` · `active` · **text** (`descricao`, `descricaoNormalizada` — português) |
| `matriz_depara` | `statusRevisao+score` · `newId` · `legacyData.legacyId` · `decision` · `grupoServico` · `unidadeMedida` |
| `grupos_servico` | `nomeNormalizado` · `active` |
| `codigos_fornecedor` | `nomeNormalizado` · `codigoFornecedorSAP` · `active` |
| `unidades_medida` | **único** `sigla` · `nomeNormalizado` |
| `excecoes` | `dataHora` · `codigoCompletoId` |
| `eventos_auditoria` | `occurredAt` · `action+occurredAt` · `correlationId` · `entityId` · `actorId` |

---

## 7. O que é apenas LEITURA (não grava nada)

| Endpoint | Coleção lida |
|---|---|
| `GET /grupos-servico?query=` etc. | respectivas coleções de catálogo |
| `GET /codigos-completos` | `codigos_completos` |
| `POST /codigos-completos/preview` | leitura de todas (análise sem gravação) |
| `GET|POST /itens/similaridade` | `codigos_servico` (via `SearchClient`) |
| `GET /matriz-de-para*` | `matriz_depara` |
| `GET /governanca/pendentes|resumo|estatisticas|fechamentos|fila/contagem|eventos|excecoes` | várias |
| `GET /health`, `/ready` | — |

---

## 8. Conferência de integridade (dados oficiais)

| Coleção | Registros esperados |
|---|---|
| `matriz_depara` | 9.795 (1 por registro legado) |
| `codigos_servico` | 7.308 (canônicos únicos) |
| `codigos_completos` | 7.554 (composições distintas) |
| `grupos_servico` | 3 (Civil, Eletromecânica, Facilities) |
| `codigos_fornecedor` | 111 |
| `unidades_medida` | 17 |

> Estado oficial pode ser recriado a qualquer momento: `docker compose down -v && docker compose up --build`
> (o `DataSeeder` recarrega tudo a partir das planilhas do Time de IA).
