# Decisões, cobertura do PDF e limitações

## 1. O que foi verificado e o que NÃO foi

Este projeto foi gerado num ambiente **sem acesso ao Maven Central**, então:

- ✅ **Verificado de verdade:** a lógica pura (normalização, stemming, similaridade, política de faixas, montagem de IDs) foi compilada com o JDK 21 e passou nos **25 testes unitários** (rodados contra um runner mínimo; no projeto eles usam JUnit 5 via `mvn test`).
- ⚠️ **Não compilado aqui:** as camadas que dependem de Spring Boot, MongoDB e JavaFX (config, serviços, controllers, runners, o desktop inteiro) foram escritas com cuidado, mas **o primeiro `docker compose up --build` é a primeira compilação real**. Se aparecer algum erro de build, ele aponta o arquivo/linha: é correção pontual.
- ⚠️ **Não executado contra um MongoDB real** nem com a interface aberta — o fluxo ponta a ponta precisa de um teste seu (roteiro em [COMO-RODAR.md](COMO-RODAR.md), seção 3).

## 2. Mapa do PDF → implementação

| Seção do PDF | Onde |
|---|---|
| 3, 4 (camadas, API como ponte) | `backend/` (controller → service → repository/SearchClient); desktop só fala com a API |
| 5 (MongoDB Atlas + alternativa) | `search/SearchClient` com `AtlasSearchClient` e `LocalSearchClient`; trocar por OpenSearch = nova implementação |
| 6 (busca exata, prefixo, fuzzy, stemming, normalização, ranking) | `text/TextNormalizer`, `PortugueseStemmer`, `SimilarityScorer`; ranking em `ComponenteService` (ID exato > prefixo de ID > texto exato > prefixo > palavras > fuzzy) |
| 6.7 (faixas 0,90 / 0,75) | `SimilarityPolicy` (configurável) |
| 7, 8 (modelo, IDs, unicidade) | `TipoComponente`, `IdFormatter` (3+3+6+2), `SequenceService`, índices únicos em `MongoIndexInitializer` |
| 9 (saneamento do legado) | **Parcial:** `BackfillRunner` (normalizados), leitura tolerante, `legacyIds`. A consolidação de duplicatas históricas e a matriz de-para (9.3–9.6) são o trabalho de **dados/IA** do PDF e **não** estão neste projeto |
| 10 (API, preview, idempotência, validações) | `*Controller`, `CodigoCompletoService.preview`, `IdempotencyService`, `ItemValidator` |
| 11 (JavaFX) | `frontend/`: seleção com 4 componentes, debounce 300 ms, cancelamento, cadastro, contador SAP, similaridade, governança |
| 12 (controle, perfis, exceções, observação) | `ItemService`, `GovernancaService`, `ExcecaoSimilaridade`; `SIMILARITY_ENFORCEMENT` = `observe` ou `enforce` |
| 13 (segurança) | `SecurityConfig` (RBAC no servidor, stateless, OIDC), identidade vinda do token, IP opcional |
| 14, 15 (performance, observabilidade) | Respostas enxutas, limites, índices; `requestId` + logs JSON (`json-logs`), health/liveness |
| 17 (ambientes/config) | Tudo por variáveis de ambiente; nenhum segredo no código |

## 3. Decisões e divergências (com motivo)

1. **Spring Boot 3.5.16, não o 4.x.** O 3.5 saiu do suporte aberto em jun/2026 (3.5.16 é a última gratuita); o 4.1 é o atual. Mantive o 3.5 porque o 4.x mudou módulos/pacotes (starters, Jackson 3) e eu não consigo compilar aqui para garantir. **Recomendo migrar** (guia oficial + OpenRewrite) depois que tudo estiver rodando; o código não usa nada exótico.
2. **Autenticação:** a API suporta **Basic (demo)** e **JWT/OIDC** (perfil `oidc`). O **desktop só faz login Basic**; o fluxo OIDC (PKCE) no desktop é o próximo passo. Em produção, exponha a API somente com HTTPS.
3. **Governança:** o *envio* tem tela no desktop; **aprovar/rejeitar é só via API** (`/api/v1/governanca`), com `admin.dados`. Não há tela de revisão. Quem solicita não pode aprovar o próprio pedido.
4. **Política de similaridade:** aplicada na criação de **itens** (`POST /itens`): ALERTAR exige justificativa (registra exceção); REVISAR bloqueia e leva à Governança. Na **composição do código completo** a similaridade é **informativa** (prévia); o que bloqueia é código completo duplicado, descrição > 40 caracteres e componente inexistente.
5. **Contagem de caracteres:** conta *code points* reais. (O exemplo do PDF diz 29 para "Manutenção mecânica normal", mas são **26**.)
6. **Largura dos IDs:** segui o texto do PDF (3, 3, 6, 2 = 14 posições). As telas de exemplo mostram IDs de 7–8 dígitos e `CS-000001`; se o seu banco usa outra largura, ajuste `app.ids.*`.
7. **Normalização remove palavras vazias** ("de", "da"...) como no exemplo `fornecedor manutencao industrial` do PDF, e expande abreviações do arquivo `abbreviations.properties` (edite à vontade). No autocomplete as abreviações **não** são expandidas (digitar "mec" casa mecânica e mecânico).
8. **Nota de similaridade calculada pela API** (não pelo motor de busca): igual em `local` e `atlas`, com motivo explicável. Números diferentes ("2 pessoas" × "3 pessoas") limitam a nota a "alerta".
9. **Segurança do modo Basic:** BCrypt de custo baixo de propósito (a senha é conferida a cada requisição do autocomplete); é modo de desenvolvimento.
10. **Índices:** criados explicitamente na subida (`ENSURE_INDEXES`), nunca derrubam a API.

## 4. Fora do escopo desta entrega

- Tema claro (o CSS já usa variáveis; falta o arquivo do tema e o botão de troca).
- *Rate limiting* (doc. 13.3): faça no gateway/proxy ou adicione Bucket4j.
- Integração com o SAP (fase 6 do roadmap).
- Tela de revisão da Governança; consolidação histórica/de-para (item 2 acima).
- Testes de integração (Testcontainers) e de interface; só há testes unitários da lógica pura.
- Empacotamento do desktop (`jpackage`) e distribuição.

## 5. Próximos passos sugeridos

1. Rodar o roteiro de [COMO-RODAR.md](COMO-RODAR.md) e corrigir qualquer erro de build/execução.
2. Calibrar `SIMILARITY_ALERT_THRESHOLD`/`BLOCK_THRESHOLD` com uma amostra rotulada por especialistas (doc. 16.3) e começar em `SIMILARITY_ENFORCEMENT=observe`.
3. Ligar `SEARCH_ENGINE=atlas` se o volume pedir.
4. Login OIDC no desktop e HTTPS na frente da API.
5. Migrar para o Spring Boot 4.x.
