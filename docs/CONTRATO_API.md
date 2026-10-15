# Contrato da API — `/api/v1`

Todas as respostas são JSON. Erros: `{ "timestamp", "status", "error", "message", "path" }`.

## Catálogos (seção 10.1 — autocomplete dos seletores)

| Método | Rota | Descrição |
|---|---|---|
| GET | `/grupos-servico?query=&limite=` | busca de G.S. |
| POST | `/grupos-servico` | cadastro (com anti-duplicidade) |
| GET | `/codigos-fornecedor?query=&limite=` | busca de C.F. |
| POST | `/codigos-fornecedor` | cadastro |
| GET | `/codigos-servico?query=&limite=` | busca de C.S. |
| GET | `/codigos-servico/{id}` | detalhe |
| POST | `/codigos-servico` | cadastro (limite SAP de 40) |
| GET | `/unidades-medida?query=&limite=` | busca de U.M. |
| POST | `/unidades-medida` | cadastro |

## Similaridade e código completo

| Método | Rota | Descrição |
|---|---|---|
| GET/POST | `/itens/similaridade?descricao=&limite=&tipo=` | similares (tipo: CS/GS/CF/UM) |
| POST | `/codigos-completos/preview` | prévia com decisão (não grava) |
| POST | `/codigos-completos` | cria com governança (aceita `Idempotency-Key`) |
| GET | `/codigos-completos?status=` | lista por status |
| GET | `/codigos-completos/{id}` | detalhe |

## Governança (seção 12)

| Método | Rota | Descrição |
|---|---|---|
| GET | `/governanca/pendentes?origem=HISTORICO\|NOVOS\|TODOS&query=&limite=` | fila de validação |
| GET | `/governanca/resumo` | indicadores (pendências, duplicidades, revisados) |
| POST | `/governanca/historico/{id}/revisar` | revisão do histórico (reviewedBy/reviewedAt) |
| POST | `/governanca/pendentes/{id}/aprovar` | aprova novo cadastro |
| POST | `/governanca/pendentes/{id}/rejeitar` | rejeita novo cadastro |
| GET | `/governanca/excecoes` | auditoria (últimas 50) |

## Matriz de-para (seção 9.6)

| Método | Rota | Descrição |
|---|---|---|
| GET | `/matriz-de-para?query=&limite=` | consulta por legado, descrição ou newId |
| GET | `/matriz-de-para/legado/{legacyId}` | de-para de um código legado |
| GET | `/matriz-de-para/novo/{newId}` | todos os legados de um canônico |
| GET | `/matriz-de-para/{id}` | detalhe |

## Payload — criação de código completo

```jsonc
POST /api/v1/codigos-completos
Headers: { "Idempotency-Key": "uuid" }          // opcional (seção 10.3)

{
  "gsId": "CIVIL",            // OU "gsNovoNome": "Facilities"
  "cfId": "200982674",        // OU "cfNovoNome": "...", "cfNovoCodigoSap": "..."
  "csId": "CS-130835",        // OU "csNovaDescricao": "SV EQUIPE TELHADO"
  "umId": "SV",               // OU "umNovaSigla": "HR", "umNovoNome": "Hora"
  "acao": "CRIAR",            // ou "ENVIAR_VALIDACAO"
  "justificativa": "...",     // exigida quando a decisão != PERMITIR (modo enforce)
  "usuarioCriacaoId": "403921"
}
```

## Decisões (seção 10.2)

| Decisão | Significado | Modo `enforce` |
|---|---|---|
| `PERMITIR` | sem similar relevante | cria normalmente |
| `ALERTAR` | itens semelhantes (score ≥ 0,70) | exige justificativa ou envio para validação (**409** caso contrário) |
| `REVISAR` | duplicidade provável (score ≥ 0,88) / composição existente | idem |

## Exemplo — similaridade

```
GET /api/v1/itens/similaridade?descricao=SV-EQUIPE TELHADO&limite=3
→ { "decisao": "REVISAR", "candidatos": [ { "id": "CS-130835", "score": 1.0, "motivo": "…" } ] }
```

Swagger interativo: **http://localhost:8080/swagger-ui.html**
