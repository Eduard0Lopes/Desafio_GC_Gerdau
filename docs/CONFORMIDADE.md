# Conformidade — Especificação × Implementação

Cruzamento item a item da *Especificação de implementação para OpenCode* com o que
foi efetivamente construído. Legenda: ✅ atendido · 🟡 parcial · ⚠️ divergência
decidida · ❌ não feito.

---

## 1. Resultado esperado

| Requisito | Status | Observação |
|---|---|---|
| Aplicação web executável/testável/documentada | ✅ | Docker `up --build` + Swagger + testes |
| Roda local sem credenciais corporativas | ✅ | Mongo local no compose; seed automático |
| Não alegar SAP/Atlas/OIDC sem configuração | ✅ | docs declaram explicitamente o que está fora |

## 2. Decisões de implementação

| Tema | Especificação | Implementado | Status |
|---|---|---|---|
| Interface | Vue 3 + **TypeScript** | Vue 3 + **JavaScript** | 🟡 funcional; port para TS é opcional |
| Backend | **Java 21 + Spring Boot 3** | **Java 17 + Spring Boot 4.1.1** (base oficial do repositório) | ⚠️ ver Nota A |
| Persistência | Mongo local (compose) + Atlas por config | idêntico | ✅ |
| Busca | `SearchProvider` local determinístico + Atlas isolado | `SearchClient` local (port fiel do motor da IA, testado) | 🟡 abstração pronta; adaptador Atlas só como interface |
| SAP | fora do MVP, apenas fronteiras | nenhuma integração | ✅ |
| Autenticação | identidade demonstrativa + OIDC preparado | headers `X-Usuario-Id`/`X-Usuario-Papel` + documentação de evolução OIDC | 🟡 falta flag de "modo demo" restrito a dev |
| Identificadores | larguras fixas **3/3/6/2** | **IDs oficiais** (`CS-130835`, `200982674`, `SV`, `CIVIL`) | ⚠️ ver Nota B |
| Código completo | 14 caracteres concatenados | `GS_CF_CS_UM` com IDs oficiais | ⚠️ ver Nota B |
| Limite SAP | 40 caracteres configurável, front+back | validado nos dois; valor fixo em constante | 🟡 tornar configurável |
| Governança | **sem** aprovação humana no MVP | fila de aprovação completa (histórico + novos) | ⚠️ ver Nota C |

## 3. Escopo funcional

| Item | Status | Observação |
|---|---|---|
| Pesquisar G.S./C.F./C.S./U.M. por ID/nome/descrição | ✅ | `?query=` em todas |
| Resultados com ID + rótulo; selecionar/limpar/trocar | ✅ | combobox com "＋ Cadastrar Novo Item" por último |
| Prévia só após os quatro componentes | ✅ | |
| Cadastrar componentes (mínimo C.S.; estrutura p/ 4) | ✅ | os **4 tipos** implementados |
| Validações (obrigatórios, limites, existentes, composição) | ✅ | índice único em `idCompleto` |
| Procurar similares antes de criar (candidatos, score, motivo, ação) | ✅ | verificação automática + botão |
| Reutilizar resultado encontrado | ✅ | "Reutilizar Código Existente" |
| Auditoria com usuário e data/hora do servidor | 🟡 | exceções auditadas; falta evento de auditoria para **toda** criação (ver Nota D) |
| Dados de demonstração, instruções, testes | ✅ | dados oficiais da IA (ver Nota E) |

## 5. Modelo de dados

| Item | Status | Observação |
|---|---|---|
| Entidades do domínio completas | ✅ | 6 coleções (imagem do banco) + `excecoes` + `eventos_auditoria` |
| Campos em inglês (`name`, `normalizedName`…) | ✅ | campos em **português** — **decisão confirmada** (imagem do banco e doc §7, Nota F) |
| `active` + ativar em vez de apagar | ✅ | `active`, `createdAt`, `updatedAt` nos componentes (seção 5); backfill idempotente para dados migrados; nenhum delete físico |
| `AuditEvent` completo | ✅ | `eventos_auditoria`: actor, action, entityType/Id, occurredAt (UTC), decision, reason, candidateIds, scores, correlationId — validado |
| `SequenceCounter` atômico | ✅ | coleção `contadores` com `findAndModify` atômico, alinhamento com o maior id migrado, faixa explícita e **erro de domínio ao esgotar** (seção 5) |
| IDs gerados com preenchimento e nunca reutilizados | ✅ | formato `CS-%06d`; legados em campo separado |
| Unicidade de ID de negócio, `fullCode` e composição | ✅ | índice único em `idCompleto` (§14.3) |
| Gravação código + auditoria consistente | 🟡 | sem transação multi-doc (limitação do Mongo local — documentar) |

## 6. Normalização e similaridade

| Item | Status |
|---|---|
| Texto original preservado para exibição/auditoria | ✅ |
| Chave de comparação (trim, minúsculas, acentos, espaços, pontuação; números preservados) | ✅ |
| Não tratar stemming/fuzzy como equivalência garantida (sinais de revisão) | ✅ (decisões PERMITIR/ALERTAR/REVISAR) |
| Score 0–1 ordenado; prioridade ID exato → nome exato → textual | ✅ |
| Busca local determinística + testes (acentos, typos, invertidos, parecidos) | ✅ (16 testes; paridade com difflib/Python) |
| Motor encapsulado (`SearchProvider`) | ✅ (`SearchClient`) |
| Limiares em configuração | ✅ (`SIMILARITY_LIMIAR_*`) |
| Valores 0,75/0,90 | ⚠️ implementados **0,70/0,88** (calibragem da trilha IA) — configuráveis; ver Nota G |
| Aviso de "não calibrado" na tela/documentação | 🟡 documentado; falta aviso na tela |
| Explicar o motivo (não só percentual) | ✅ |

## 7. API mínima

| Item | Status | Observação |
|---|---|---|
| Prefixo `/api/v1` | ✅ | |
| Endpoints equivalentes aos da especificação | ✅ | nomes em **português** conforme doc §10.1 — **decisão confirmada** (Nota F) |
| `POST …/preview` sem gravar | ✅ | `/codigos-completos/preview` |
| `POST …` com `Idempotency-Key` + índice único | ✅ | |
| Mesma chave + conteúdo diferente → conflito | ✅ | hash SHA-256 do conteúdo; conteúdo diferente → **409** (validado) |
| `POST` de criação de componente com similaridade + justificativa | ✅ | |
| `GET /health` e `GET /ready` | ✅ | `/api/v1/health` · `/api/v1/ready` (validados) |
| DTOs, validação, HTTP coerente | ✅ | |
| Erro `{code, message, details, correlationId}` | ✅ | `{timestamp,status,error,code,message,path,campos,correlationId}` |
| OpenAPI com exemplos | ✅ | Swagger + `docs/CONTRATO_API.md` |

## 8. Interface

| Item | Status |
|---|---|
| 4 seletores + prévia + cadastro + similares | ✅ |
| Estados: carregamento, vazio, erro, sucesso | ✅ |
| Debounce ~300 ms + cancelamento de requisições antigas | ✅ (250 ms + id de requisição) |
| Componentes reutilizáveis; regras no servidor | ✅ |
| Mensagens distinguindo validação/alerta/bloqueio | ✅ (texto + ícone, não só cor) |
| Acessibilidade por teclado | ✅ (combobox ↑↓/Enter/Esc) |
| Testes essenciais de interface | ❌ pendente |

## 9. Segurança, privacidade e auditoria

| Item | Status | Observação |
|---|---|---|
| Autorização no servidor | ✅ | governança exige `ADMIN` (403) — validado |
| Modo demo restrito a dev | 🟡 | flag documentada (`APP_MODO_DEMONSTRACAO`); falta travar em prod |
| OIDC sem bypass oculto | 🟡 | preparado/documented; não implementado (correto — fora do escopo) |
| `.env.example` sem credenciais | ✅ | criado (obs.: o `application.properties` mantém o default demo por decisão anterior) |
| Sem segredos em logs | ✅ | |
| Timestamps do servidor em UTC | ✅ | `Instant` |
| Tentativas bloqueadas e exceções auditadas | 🟡 | exceções ✅; bloqueios em log (falta persistir como evento) |

## 10/11. Entregáveis e critérios de aceite

| Critério | Status |
|---|---|
| Subir banco+API+frontend sem Atlas/SAP pelo README | ✅ |
| 4 seletores com seed; código completo na ordem definida | ✅ (com IDs oficiais — Nota B) |
| Limite 40 validado no servidor + testes no limite/abaixo/acima | 🟡 validação ✅; testes de limite pendentes |
| Busca acha acentos ausentes/typos; ordena; explica | ✅ |
| Duplicidade impedida + índice único | ✅ |
| Auditoria com identidade e timestamp | 🟡 (ver Nota D) |
| Requisições repetidas não duplicam | ✅ |
| Erros consistentes; front preserva dados digitados | ✅ |
| Testes passam (comandos listados) | ✅ (`./mvnw test` — 16/16) |
| Nenhuma integração externa apresentada como funcional | ✅ |

---

## Notas — pontos que exigem sua decisão

**Nota A — Java/Spring:** o repositório oficial usa **Spring Boot 4.1.1 / Java 17**.
A especificação pede Spring Boot 3 / Java 21. Mantive a base oficial (mais nova);
posso trocar para Java 21 facilmente. Migrar Boot 4→3 seria *downgrade* com retrabalho.

**Nota B — IDs (principal divergência):** a especificação define larguras fixas
3/3/6/2 (`00100100000101`), mas os **IDs oficiais do Time de IA** são `CS-130835`
(matriz de-para) — e você decidiu usá-los como identidade. A composição atual é
`CIVIL_200982674_CS-130835_SV`. Alternativas: (1) manter oficiais; (2) adotar
3/3/6/2 com IDs internos e guardar o `CS-130835` como código canônico.

**Nota C — Governança:** a especificação coloca aprovação humana **fora do MVP**;
você pediu explicitamente a tela de aprovação — está implementada além do exigido.

**Nota D — Auditoria:** hoje registramos exceções (justificativas). A especificação
quer `AuditEvent` para **toda** criação/decisão (actor, action, timestamps,
candidateIds, scores, correlationId) — pendência de implementação curta.

**Nota E — Dados:** a especificação pede dados *fictícios*; você pediu os **oficiais**
da pasta da IA — implementado conforme seu pedido.

**Nota F — Idioma de rotas/campos:** a especificação pede nomes em inglês
(`/service-groups`, `name`, `fullCode`…); o documento técnico (§7/§10.1) e a
**imagem do seu banco** usam português (`grupos_servico`, `nome`, `descricaoCanonica`).
Implementei em português para casar com o banco existente.

**Nota G — Limiares:** especificação 0,75/0,90 (provisórios) × calibragem da trilha
IA 0,70/0,88 — ambos configuráveis por variável de ambiente.
