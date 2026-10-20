# Contrato da API — `/api/v1`

Todas as respostas são JSON. Erros: `{ "timestamp", "status", "error", "message", "path" }`.

## Catálogos (seção 10.1 — autocomplete dos seletores)

| Método | Rota | Descrição |
|---|---|---|
| GET | `/grupos-servico?query=&limite=&incluirTodos=` | busca de G.S. |
| POST | `/grupos-servico` | cadastro (com anti-duplicidade) |
| GET | `/codigos-fornecedor?query=&limite=&incluirTodos=` | busca de C.F. |
| POST | `/codigos-fornecedor` | cadastro |
| GET | `/codigos-servico?query=&limite=&incluirTodos=` | busca de C.S. |
| GET | `/codigos-servico/{id}` | detalhe |
| POST | `/codigos-servico` | cadastro (limite SAP de 40) |
| GET | `/unidades-medida?query=&limite=&incluirTodos=` | busca de U.M. |
| POST | `/unidades-medida` | cadastro |
| POST | `/catalogo/exclusao-em-massa` | exclusão em massa (apenas ADMIN) — `{ tipo: "GS\|CF\|CS\|UM", ids: [] }`, devolve relatório item a item |
| POST | `/catalogo/situacao` | **desativar/reativar** (apenas ADMIN) — `{ tipo, ids, situacao: "DESATIVADO"\|"ATIVO" }`, devolve relatório item a item |
| POST | `/catalogo/importacao` | importação (apenas ADMIN) — `{ tipo: "GS\|CF\|CS\|UM", itens: [{ id?, nome?, descricao?, sigla?, codigoSap? }] }`, **só adiciona** |

**Importação:** o que já existe é **ignorado** (item mantido) e apenas o que é novo entra — nada é
apagado ou alterado. A resposta é um relatório por linha (`adicionados` × `ignorados`, com o motivo).
A **exportação CSV** é gerada na própria interface do Catálogo (colunas iguais às da importação:
`id;nome;origem;status` em G.S., `id;codigoSap;nome;origem;status` em C.F.,
`id;descricao;caracteres;origem;status` em C.S. e `id;sigla;nome;origem;status` em U.M.).

**Status dos componentes:** o campo `status` (`ATIVO`, `PENDENTE_VALIDACAO`, `REJEITADO`, `DESATIVADO`)
controla a visibilidade. Listas de uso (seletores, similaridade) retornam apenas `ATIVO`;
enviar para Governança deixa os componentes novos fora das listas até a aprovação; **desativar**
(soft delete) tira o item de circulação sem apagar nada — vínculos, histórico e auditoria
continuam, e **reativar** devolve o item às listas. Com `incluirTodos=true` (catálogo em tela,
somente leitura) todos aparecem com o selo de status.

## Similaridade e código completo

| Método | Rota | Descrição |
|---|---|---|
| GET/POST | `/itens/similaridade?descricao=&limite=&tipo=` | similares (tipo: CS/GS/CF/UM) |
| POST | `/itens/similaridade/lote` | avaliação em massa: diz `disponiveis` / `aCriar` / `bloqueadas` por linha |
| POST | `/codigos-completos/preview` | prévia com decisão (não grava) |
| POST | `/codigos-completos` | cria com governança (aceita `Idempotency-Key`) |
| POST | `/codigos-completos/lote` | vinculação em massa: GS/CF/UM fixos + vários CS (reutiliza o disponível, cria o novo, manda para a Governança o que tem similaridade alertada) |
| GET | `/codigos-completos?status=` | lista por status |
| GET | `/codigos-completos/{id}` | detalhe |

## Governança (seção 12)

| Método | Rota | Descrição |
|---|---|---|
| GET | `/governanca/pendentes?origem=HISTORICO\|NOVOS\|TODOS&query=&limite=` | fila de validação |
| GET | `/governanca/resumo` | indicadores (pendências, duplicidades, revisados) |
| POST | `/governanca/historico/{id}/revisar` | revisão do histórico (reviewedBy/reviewedAt) |
| POST | `/governanca/pendentes/{id}/aprovar` | aprova novo cadastro (ativa os componentes criados no envio) |
| POST | `/governanca/pendentes/{id}/rejeitar` | rejeita novo cadastro (componentes ficam fora de circulação) |
| POST | `/governanca/pendentes/{id}/reutilizar` | a Governança escolhe um dos matches ignorados (`escolhidoId`/`escolhidoTipo`) e o pedido segue reutilizando o código — o código novo não entra no catálogo |
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

## Exemplo — vinculação em massa

Primeiro a tela de informação avalia os códigos escolhidos:

```
POST /api/v1/itens/similaridade/lote
{ "tipo": "CS", "textos": ["SV EQUIPE TELHADO", "SV MONTAGEM PIPA", "SV EQUIPE TELHADO"] }
→ {
    "total": 3, "disponiveis": 1, "aCriar": 1, "bloqueadas": 1,
    "itens": [
      { "linha": 1, "classificacao": "DISPONIVEL", "situacao": "JA_EXISTE", "similar": { "id": "CS-130835", … } },
      { "linha": 2, "classificacao": "CRIAR", "situacao": "OK", … },
      { "linha": 3, "classificacao": "BLOQUEADA", "situacao": "REPETIDA", … }
    ]
  }
```

`classificacao`: `DISPONIVEL` (já existe — reutilizar) | `CRIAR` (precisa ser criado) | `BLOQUEADA`
(vazia, repetida na lista ou acima do limite SAP). É o aviso da tela de informação:
**"há X disponíveis, porém Y precisam ser criados"**.

Depois a vinculação em massa (GS/CF/UM fixos + vários CS):

```
POST /api/v1/codigos-completos/lote
{ "gsId": "CIVIL", "cfId": "1440531", "umId": "UN",
  "itens": [ { "csId": "CS-130835" }, { "texto": "SV MONTAGEM PIPA" } ] }
→ {
    "total": 2, "disponiveis": 1, "criados": 1, "vinculados": 2,
    "aguardandoValidacao": 0, "bloqueadas": 0, "falhas": 0, "itens": [ … ]
  }
```

Por linha: `csId` reutiliza o código existente; `texto` cria o código novo — e, havendo
similaridade alertada, o pedido vai para a Governança (`aguardandoValidacao`), que vê os
matches que o usuário não escolheu e pode decidir reutilizar um deles.

Swagger interativo: **http://localhost:8080/swagger-ui.html**
