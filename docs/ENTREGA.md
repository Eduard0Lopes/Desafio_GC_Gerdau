# O que entregar à Gerdau

Checklist do pacote final de entrega (baseado nos entregáveis da seção 21 do
documento técnico e nos critérios da Sprint).

## 1. Pacote de código (repositório)

| Item | Onde está | Status |
|---|---|---|
| Backend (API Spring Boot + testes) | `JAVA/backend//` | ✅ |
| Frontend (Vue 3) | `JAVA/frontend/` | ✅ |
| Dados oficiais (Time de IA) | `IA/data/processed/` | ✅ (preservado, não alterado) |
| Execução em uma linha | `docker-compose.yml` | ✅ |
| Variáveis de ambiente (sem segredos) | `.env.example` | ✅ |
| OpenAPI (Swagger) | `/swagger-ui.html` | ✅ |

> Como empacotar: enviar o **repositório Git** (privado) OU um `.zip` do projeto sem
> as pastas `node_modules/`, `target/` e `dist/`. Incluir a tag/versão da entrega.

## 2. Documentação (pasta `docs/`)

| Documento | Para quê | Público |
|---|---|---|
| **INSTALACAO_E_USO.md** | instalar, rodar e usar (passo a passo) | qualquer pessoa |
| **ARQUITETURA.md** | visão técnica e decisões | time técnico da Gerdau |
| **MODELO_DADOS.md** | coleções, merge dos dados, índices | time técnico/DADOS |
| **CONTRATO_API.md** | endpoints, payloads, decisões | integrações (futuro SAP) |
| **PERMISSOES.md** | perfis admin × usuário | segurança/governança |
| **DEMO.md** | roteiro de demonstração | apresentação |
| **CONFORMIDADE.md** | aderência à especificação + pendências | avaliadores |

## 3. Entregáveis visuais (documento técnico, seção 21)

| Entregável | Como produzir |
|---|---|
| Desenho de arquitetura | diagrama da seção "Arquitetura" (`docs/ARQUITETURA.md`) |
| Mapa mental | conceitos: problema → saneamento → catálogo → filtro → governança |
| Protótipo visual | as telas do sistema (prints das telas reais) |
| Fluxograma operacional | fluxo de decisão (PERMITIR/ALERTAR/REVISAR) |
| Vídeo explicativo | percorrer o roteiro de `docs/DEMO.md` (8 min) |

## 4. O que NÃO fazer parte da entrega (e deve ser dito)

- Nenhuma integração real com **SAP** (fora do escopo — apenas fronteiras preparadas).
- Identidade corporativa **OIDC** não configurada (modo demonstração documentado).
- **Atlas Search** não ativado — a busca local é determinística e testada.
- Limiares de similaridade são **provisórios** (configuráveis) até calibração com a área.

## 5. Roteiro de apresentação (dia 19)

Seguir `docs/DEMO.md` com a aplicação já no ar
(`docker compose up --build` executado antes do início).
