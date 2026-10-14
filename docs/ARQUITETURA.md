# Arquitetura e fluxo de dados

## Visão geral

1. **IA/Dados (`IA/src`)** lê a base SAP anonimizada, normaliza as descrições e gera:
   - `base_saneada.xlsx` (uma linha por registro legado);
   - `nosql/*.json` (as 5 coleções no formato exato dos modelos Java);
   - `matriz_de_para_gerdau.{xlsx,csv}` (legado → código canônico + score real de similaridade);
   - `catalogo_padronizado_servicos.xlsx` (um item por código de serviço canônico).
2. **Backend** importa os JSONs no MongoDB (`DadosIaSeeder`, por flag) e expõe a API REST.
   Novos cadastros seguem as mesmas regras da migração (IDs, normalização, snapshot).
3. **Front-end** consome a API: cadastros, geração de código completo e alerta de duplicidade em tempo real.

## Regras compartilhadas entre Python e Java

| Regra | Python | Java |
|---|---|---|
| Normalização (minúsculas, sem acento, `[a-z0-9 ]`) | `normalizacao.normalizar_texto` | `util.TextoUtil.normalizar` |
| Similaridade = 0,4·Jaccard(tokens) + 0,6·SequenceMatcher | `fuzzy_engine.similaridade` | `service.similaridade.FuzzyEngine` |
| Política: ≥ 0,88 DUPLICIDADE · ≥ 0,70 ALERTA · senão ORIGINAL | `ranking_policy.classificar` | `RankingPolicy.classificar` |
| Snapshot `Grupo \| Fornecedor <SAP> \| <descrição> \| <sigla>` | `migration.montar_snapshot` | `CodigoCompletoService.montarSnapshot` |
| Limite SAP de 40 caracteres | `normalizacao.LIMITE_SAP` | `TextoUtil.LIMITE_SAP` |

A paridade é testada pela fixture `IA/tests/fixtures/similaridade_casos.json`, lida por `test_fuzzy.py` e por `FuzzyEngineTest.java`.
Mudou um lado? Mude o outro e regenere a fixture.

## Modelo de dados (MongoDB)

| Coleção | `_id` | Campos principais |
|---|---|---|
| `grupos_servico` | 3 dígitos (`001`) | `nome`, `nomeNormalizado`, `origem` |
| `unidades_medida` | 2 dígitos (`15`) | `sigla`, `nome`, `nomeNormalizado`, `origem` |
| `codigos_fornecedor` | 3 dígitos | `codigoFornecedorSAP`, `nome`, `nomeNormalizado`, `origem` |
| `codigos_servico` | 6 dígitos | `descricao`, `descricaoNormalizada`, `quantidadeCaracteres`, `justificativa`, `legacyIds[]`, `origem` |
| `codigos_completos` | 14 dígitos = GS+CF+CS+UM | `gsId`, `cfId`, `csId`, `umId`, `descricaoSnapshot`, `createdAt`, `legacyIds[]`, `origem` |

- `origem`: `MIGRACAO` (veio da IA) ou `SISTEMA` (criado pela API).
- Novos IDs: `GeradorCodigoSequencial` pega o maior `_id` numérico da coleção + 1, com zeros à esquerda, e repete a inserção em caso de corrida (`DuplicateKeyException`).
- `codigos_completos`: registros legados com a mesma combinação GS+CF+CS+UM são consolidados em um documento, preservando todos os códigos em `legacyIds` (9.795 linhas → 9.327 documentos, sem `_id` duplicado).
- Exclusão de grupo, fornecedor, serviço ou unidade é bloqueada (409) se houver código completo vinculado.

## Backend — camadas

`controller` (REST, validação) → `service` (regras, IDs, vínculos) → `repository`/`MongoTemplate` → MongoDB.
`mapper` converte modelos em DTOs; `exception.GlobalExceptionHandler` padroniza erros em `ErrorResponse`
(`status`, `message`, `path`, `campos`). `config` tem CORS e criação de índices; `seed` tem o importador.

## Front-end — estrutura

- `services/apiService.js`: cliente axios único (`VITE_API_URL`) e `extrairErro` para o `ErrorResponse`.
- `views/CadastroSimples.vue`: CRUD genérico configurado em `App.vue` (grupos, fornecedores, unidades).
- `views/CodigosServicoView.vue`: lista paginada, cadastro com sugestão de similares e justificativa.
- `views/CodigosCompletosView.vue`: filtros, prévia do ID e criação com serviço existente ou novo.
- `components/`: resumo, paginação, lista de similares e notificações.
