# Referência da API (`http://localhost:8080/api`)

Erros seguem `ErrorResponse`: `{ timestamp, status, error, message, path, campos? }`.
`400` validação · `404` não encontrado · `409` conflito (duplicidade, vínculo, chave repetida) · `405` método inválido.

## Grupos, unidades e fornecedores

| Método | Rota | Corpo / parâmetros |
|---|---|---|
| GET | `/grupos-servico?q=` | filtro por nome |
| GET/PUT/DELETE | `/grupos-servico/{id}` | PUT: `{ "nome" }` |
| POST | `/grupos-servico` | `{ "nome": "Mecânica" }` |
| GET | `/unidades-medida?q=` | filtro por sigla ou nome |
| POST / PUT `{id}` | `/unidades-medida` | `{ "sigla": "PCT", "nome": "Pacote" }` (sigla vira maiúscula) |
| GET | `/codigos-fornecedor?q=` | filtro por nome ou código SAP |
| POST / PUT `{id}` | `/codigos-fornecedor` | `{ "codigoFornecedorSAP": 200982674, "nome": "..." }` |

Todas têm `GET /{id}` e `DELETE /{id}` (409 se estiver em uso por código completo).

## Códigos de serviço

| Método | Rota | Observações |
|---|---|---|
| GET | `/codigos-servico?q=&page=0&size=20` | `PageResponse`; `q` busca descrição, prefixo do ID ou código legado |
| GET | `/codigos-servico/similares?descricao=&limite=5&ignorarId=` | ranking `{ id, descricao, score, status, acaoRecomendada }` |
| POST | `/codigos-servico` | `{ "descricao": "SV ...", "justificativa": null }` |
| PUT | `/codigos-servico/{id}` | mesmo corpo |
| GET/DELETE | `/codigos-servico/{id}` | |

Regras do POST/PUT: descrição ≤ 40 caracteres, salva em maiúsculas; descrição normalizada igual a outra → 409;
similar com score ≥ 0,88 sem `justificativa` → 409 com o código mais parecido na mensagem.

## Códigos completos

| Método | Rota | Observações |
|---|---|---|
| GET | `/codigos-completos?gsId=&cfId=&csId=&umId=&q=&page=&size=` | resposta enriquecida com nomes de grupo, fornecedor, serviço e unidade |
| GET | `/codigos-completos/{id}` | |
| GET | `/codigos-completos/legado/{legacyId}` | busca pelo código SAP legado |
| POST | `/codigos-completos` | ver abaixo |
| DELETE | `/codigos-completos/{id}` | |

```json
{ "gsId": "001", "cfId": "001", "umId": "15", "csId": "000001" }
{ "gsId": "001", "cfId": "001", "umId": "15", "descricaoServico": "SV NOVO SERVICO", "justificativa": "..." }
```
Informe **exatamente um** entre `csId` (serviço existente) e `descricaoServico` (cria o serviço com as regras acima).
O `id` é `gsId + cfId + csId + umId`; combinação já existente → 409.

## Resumo

`GET /resumo` → `{ gruposServico, unidadesMedida, codigosFornecedor, codigosServico, codigosCompletos }`.
