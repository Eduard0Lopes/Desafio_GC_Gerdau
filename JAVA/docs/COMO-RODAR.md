# Como rodar o projeto

## 1. O que você precisa

| Para | Precisa de |
|---|---|
| **API (backend)** | Docker com Compose v2 (`docker compose version`) |
| **Aplicativo desktop** | JDK 21+ e Maven 3.9+ (`java -version`, `mvn -version`) |
| **Banco** | Seu MongoDB (veja [CONECTAR-MONGODB.md](CONECTAR-MONGODB.md)) ou o perfil `local-db` de teste |

> O aplicativo desktop **não roda dentro do Docker** (é uma janela gráfica). O Docker sobe a API e, opcionalmente, um MongoDB local.

## 2. Subir a API

```bash
cp .env.example .env
```

Abra o `.env` e preencha **só duas coisas**:

1. `MONGODB_URI` — a conexão do seu banco, **com o nome do banco no final** (veja [CONECTAR-MONGODB.md](CONECTAR-MONGODB.md)).
2. `APP_DEV_PASSWORD` — a senha (mínimo 8 caracteres) dos usuários de demonstração.

> Não use aspas nos valores do `.env`. Nunca suba o `.env` para o Git (já está no `.gitignore`).

```bash
docker compose up --build
```

Na primeira vez o build demora alguns minutos (Maven baixa as dependências). Quando subir, confira:

```bash
curl http://localhost:8080/actuator/health        # {"status":"UP"} => API e MongoDB OK
```

Nos logs você deve ver algo assim (confirma **em qual banco** a API conectou):

```
MongoDB OK — banco='seu_banco', busca=LOCAL, similaridade=ENFORCE
  coleção grupos_servico: 3 documento(s)
  coleção codigos_servico: 1520 documento(s)
```

Se aparecer `NÃO ENCONTRADA`, o nome do banco na URI está errado. Se aparecer `NÃO foi possível ler o MongoDB`, veja a seção *Problemas comuns* em [CONECTAR-MONGODB.md](CONECTAR-MONGODB.md).

Comandos úteis:

```bash
docker compose logs -f backend        # acompanhar logs
docker compose down                   # parar
docker compose up -d --build          # rebuild em segundo plano após mudar o código
```

### Sem banco? Use o MongoDB local de teste

```bash
# no .env, use a opção (C) da MONGODB_URI e defina MONGO_ROOT_PASSWORD (mesma senha da URI)
docker compose --profile local-db up --build
```

Ele já vem com os dados de exemplo do PDF (Eletromecânica, Facilities, Civil, `00100100000101`...). Para recomeçar do zero: `docker compose --profile local-db down -v` (apaga o volume).

## 3. Abrir o aplicativo desktop

```bash
./frontend/run.sh            # Linux/macOS
frontend\run.bat             # Windows
```

A API é procurada em `http://localhost:8080`. Para outra URL: `API_BASE_URL=https://minha-api ./frontend/run.sh`
(ou `-Dapi.baseUrl=...`). O aplicativo avisa se a URL remota não usar HTTPS.

### Usuários de demonstração (todos com a senha de `APP_DEV_PASSWORD`)

| Usuário | Pode |
|---|---|
| `consultor` | pesquisar e visualizar |
| `solicitante` | + vincular serviço, cadastrar item, enviar para Governança |
| `admin.dados` | + aprovar/rejeitar solicitações da Governança |
| `admin.tecnico` | + ver o diagnóstico do banco (`/api/v1/admin/diagnostico`) |

Esses usuários são **só para desenvolvimento/demonstração**. Em produção use login corporativo (perfil `oidc`, veja [DECISOES.md](DECISOES.md)).

### O que testar na tela

1. **Vincular serviço** — escolha G.S., C.F., C.S. e U.M. (digite 2+ letras ou o ID; setas ↓↑ e Enter funcionam). A prévia mostra o ID de 14 posições, a descrição, os caracteres e avisos.
2. **Cadastrar Novo Item** — digite uma descrição **parecida** com uma existente, ex.: `Mecânico manutenção` no tipo *Código de Serviço*: você verá o alerta/bloqueio, poderá **reutilizar**, **justificar** ou **enviar para a Governança**.

## 4. Rodar a API sem Docker (opcional)

```bash
cd backend
mvn spring-boot:run          # lê MONGODB_URI e APP_DEV_PASSWORD de ../.env automaticamente
```

## 5. Testes

```bash
cd backend && mvn test       # testes unitários (normalização, similaridade, IDs, política)
```

## 6. Testando a API com curl

Swagger UI: <http://localhost:8080/swagger-ui.html> (desligue em produção com `API_DOCS_ENABLED=false`).

```bash
PW='sua-senha'
# busca (autocomplete / fuzzy / por ID)
curl -u solicitante:$PW "http://localhost:8080/api/v1/codigos-servico?query=mecnico%20manut"

# similaridade
curl -u solicitante:$PW -H 'Content-Type: application/json' \
  -d '{"tipo":"CS","texto":"Mecânico manutenção"}' http://localhost:8080/api/v1/itens/similaridade

# prévia e criação do código completo (a criação exige Idempotency-Key)
curl -u solicitante:$PW -H 'Content-Type: application/json' \
  -d '{"gsId":"001","cfId":"001","csId":"000002","umId":"01"}' http://localhost:8080/api/v1/codigos-completos/preview
curl -u solicitante:$PW -H 'Content-Type: application/json' -H "Idempotency-Key: $(uuidgen)" \
  -d '{"gsId":"001","cfId":"001","csId":"000002","umId":"01"}' http://localhost:8080/api/v1/codigos-completos

# novo item (409 SIMILARIDADE_ALTA / SIMILARIDADE_INTERMEDIARIA conforme a política)
curl -u solicitante:$PW -H 'Content-Type: application/json' -H "Idempotency-Key: $(uuidgen)" \
  -d '{"tipo":"CS","texto":"Limpeza de escritório"}' http://localhost:8080/api/v1/itens

# Governança: solicitar (solicitante) e decidir (admin.dados) — quem pede não aprova o próprio pedido
curl -u solicitante:$PW -H 'Content-Type: application/json' -H "Idempotency-Key: $(uuidgen)" \
  -d '{"tipo":"CS","texto":"Mecânico manutenção com especialização","justificativa":"Exige certificação NR-10 específica"}' \
  http://localhost:8080/api/v1/governanca
curl -u admin.dados:$PW "http://localhost:8080/api/v1/governanca?status=PENDENTE"
curl -u admin.dados:$PW -H 'Content-Type: application/json' \
  -d '{"decisao":"APROVAR","observacao":"Confirmado com a área"}' http://localhost:8080/api/v1/governanca/ID_DA_SOLICITACAO/decisao

# diagnóstico do banco conectado
curl -u admin.tecnico:$PW http://localhost:8080/api/v1/admin/diagnostico
```

> No Windows PowerShell, `curl` é um apelido de outro comando: use `curl.exe` ou o Swagger UI.

## 7. Variáveis de ambiente (`.env`)

| Variável | Padrão | O que faz |
|---|---|---|
| `MONGODB_URI` | — (**obrigatória**) | Conexão com o MongoDB, com o nome do banco |
| `APP_DEV_PASSWORD` | — (**obrigatória**) | Senha dos usuários de demonstração (≥ 8 caracteres) |
| `SEARCH_ENGINE` | `local` | `local` (qualquer MongoDB) ou `atlas` (Atlas Search) |
| `SIMILARITY_ENFORCEMENT` | `enforce` | `observe` só registra candidatos; `enforce` alerta/bloqueia |
| `SIMILARITY_ALERT_THRESHOLD` / `SIMILARITY_BLOCK_THRESHOLD` | `0.75` / `0.90` | Faixas da política (calibre com amostra real) |
| `SPRING_PROFILES_ACTIVE` | vazio | `json-logs` e/ou `oidc` (separe por vírgula) |
| `BACKFILL_NORMALIZED` | `false` | `true` uma vez: preenche normalizados que faltem no legado |
| `ENSURE_INDEXES` | `true` | `false` se o DBA gerencia os índices |
| `API_DOCS_ENABLED` | `true` | Swagger UI e `/v3/api-docs` |
| `AUDIT_RECORD_IP` | `false` | Grava o IP de origem como metadado (LGPD: só se houver finalidade) |
| `BACKEND_PORT` | `8080` | Porta publicada |
| `OIDC_ISSUER_URI`, `OIDC_ROLES_CLAIM` | — / `roles` | Só com o perfil `oidc` |

## 8. Problemas frequentes

| Sintoma | Causa provável |
|---|---|
| `docker compose up` reclama de `MONGODB_URI`/`APP_DEV_PASSWORD` | `.env` não existe ou está sem esses valores |
| Container sobe mas `/actuator/health` dá `DOWN` | A API não alcança o MongoDB: [CONECTAR-MONGODB.md](CONECTAR-MONGODB.md) |
| Desktop: "Não foi possível conectar à API" | API fora do ar, porta diferente ou `API_BASE_URL` errada |
| Desktop: "Usuário ou senha inválidos" | Senha diferente da `APP_DEV_PASSWORD` do `.env` (reinicie a API após mudar) |
| Busca não acha itens do legado | Faltam `nomeNormalizado`/`descricaoNormalizada`: rode uma vez com `BACKFILL_NORMALIZED=true` |
| `mvn javafx:run` falha ao baixar dependências | Rede/proxy corporativo: configure o `settings.xml` do Maven |
