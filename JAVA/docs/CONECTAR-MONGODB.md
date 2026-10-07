# Conectar a API no seu MongoDB

A API usa **uma única configuração** para falar com o banco: a variável `MONGODB_URI` do arquivo `.env`.
Nada mais precisa ser alterado no código para o caso comum (banco com as coleções do PDF).

## 1. Escolha a sua URI

A URI **sempre termina com o nome do banco** (`.../NOME_DO_BANCO?opções`). Sem isso a API conecta, mas no banco errado (`test`).

| Onde está o seu banco | `MONGODB_URI` no `.env` |
|---|---|
| **MongoDB Atlas** | `mongodb+srv://USUARIO:SENHA@cluster0.abcde.mongodb.net/NOME_DO_BANCO?retryWrites=true&w=majority` |
| **MongoDB na sua máquina** (fora do Docker) | `mongodb://host.docker.internal:27017/NOME_DO_BANCO` |
| **MongoDB em outro servidor** | `mongodb://USUARIO:SENHA@servidor:27017/NOME_DO_BANCO?authSource=admin` |
| **Réplica / cluster** | `mongodb://USUARIO:SENHA@h1:27017,h2:27017,h3:27017/NOME_DO_BANCO?replicaSet=rs0&authSource=admin` |
| **MongoDB de teste do compose** (perfil `local-db`) | `mongodb://root:SENHA_DO_MONGO@mongo:27017/gerdau_servicos?authSource=admin` |

> `localhost` dentro do container é o **próprio container**. Para alcançar o Mongo da sua máquina use `host.docker.internal` (o compose já cuida disso, inclusive no Linux).

### Senha com caracteres especiais

Caracteres como `@ : / ? # % ` na senha precisam ser codificados (URL-encode): `@`→`%40`, `:`→`%3A`, `/`→`%2F`, `?`→`%3F`, `#`→`%23`, `%`→`%25`.
Exemplo: senha `p@ss:1` vira `p%40ss%3A1`. Dica: gere uma senha só com letras e números para evitar o problema.

## 2. MongoDB Atlas — checklist (passo a passo)

1. **Database Access → Add New Database User**: crie um usuário **dedicado** (ex.: `gerdau_servicos_api`) com o papel **`readWrite` somente no seu banco** (não use `atlasAdmin`).
2. **Network Access → Add IP Address**: libere o IP de quem roda a API (o seu, ou o IP de saída do servidor). Sem isso: *timeout* de 10 s. Não deixe `0.0.0.0/0` em produção.
3. **Connect → Drivers**: copie a *connection string*, troque `<password>` pela senha (codificada) e **acrescente o nome do banco** antes do `?`.
4. Cole no `.env` como `MONGODB_URI=...` (sem aspas) e rode `docker compose up --build`.
5. Confirme nos logs a linha `MongoDB OK — banco='...'` e as contagens das coleções.

## 3. O que a API espera encontrar no banco

Modelo do PDF (seção 7). Os nomes ficam em **um único lugar**: `backend/src/main/java/br/com/gerdau/servicos/api/domain/TipoComponente.java`
(se o seu banco usa outros nomes de coleção/campo, ajuste ali e reconstrua).

| Coleção | Campos lidos | Observações |
|---|---|---|
| `grupos_servico` | `_id`, `nome`, `nomeNormalizado` | G.S. — `_id` de 3 posições |
| `codigos_fornecedor` | `_id`, `nome`, `nomeNormalizado` | C.F. — 3 posições |
| `codigos_servico` | `_id`, `descricao`, `descricaoNormalizada`, `quantidadeCaracteres`, `legacyIds` | C.S. — 6 posições; descrição ≤ 40 |
| `unidades_medida` | `_id`, `nome`, `sigla`, `nomeNormalizado` | U.M. — 2 posições |
| `codigos_completos` | `_id`, `idCompleto`, `gsId`, `cfId`, `csId`, `umId`, `descricaoSnapshot`, `usuarioCriacaoId`, `createdAt`, `origem`, `legacyIds` | 14 posições |

A leitura é **tolerante**: campo ausente, nulo ou com tipo diferente não derruba a API (usa-se `""`).
As larguras dos IDs (3/3/6/2) ficam em `app.ids.*` no `application.yml`.

### Coleções criadas pela própria API (se não existirem)

| Coleção | Uso |
|---|---|
| `sequences` | contador atômico de IDs (semeado com o maior `_id` numérico já existente — nunca colide com o legado) |
| `idempotency_keys` | proteção contra duplo clique/retentativa (expira em 24 h) |
| `solicitacoes_governanca` | pedidos enviados à Equipe de Governança |
| `excecoes_similaridade` | trilha de auditoria de exceções (doc. 12.3) |

Por isso o usuário do banco precisa de **`readWrite`** (e não só leitura).

### Legado sem campos normalizados?

Se seus documentos não têm `nomeNormalizado` / `descricaoNormalizada`, a busca não os encontrará. Rode **uma vez**:

```bash
# no .env
BACKFILL_NORMALIZED=true
docker compose up -d --build backend     # veja nos logs: "Backfill codigos_servico: N documento(s)..."
# depois volte para false
```

O backfill **só adiciona** campos que faltam; nunca altera `nome`/`descricao`.

## 4. Índices

Na subida a API garante (se não existirem) os índices do doc. 14.3: normalizados, **únicos** em `idCompleto` e na composição `gsId+cfId+csId+umId`, simples em cada ID, TTL em `idempotency_keys`.
Se algo impedir (permissão, índice conflitante, **duplicatas já existentes** que violam a unicidade), a API **continua de pé** e registra um aviso:

```
Não foi possível criar o índice codigos_completos.uk_composicao: ...
```

Para achar duplicatas em `codigos_completos` (mongosh):

```js
db.codigos_completos.aggregate([
  { $group: { _id: { gs:"$gsId", cf:"$cfId", cs:"$csId", um:"$umId" }, n: { $sum: 1 }, ids: { $push: "$_id" } } },
  { $match: { n: { $gt: 1 } } }
])
```

Se o seu DBA gerencia índices, use `ENSURE_INDEXES=false`.

## 5. Motor de busca

| `SEARCH_ENGINE` | Quando usar |
|---|---|
| `local` (padrão) | Qualquer MongoDB. Usa regex do próprio Mongo; ótimo para protótipo e volumes pequenos/médios. |
| `atlas` | MongoDB Atlas com **Atlas Search**: autocomplete/fuzzy/stemming com índices dedicados (recomendado para volume alto). |

Para o Atlas Search, crie os índices (idempotente) com o `mongosh`:

```bash
mongosh "SUA_MONGODB_URI" scripts/mongo/create-atlas-search-indexes.js
```

Aguarde o status **READY** em *Atlas → Search* e então `SEARCH_ENGINE=atlas`. O nome do índice é `default` (mude com `ATLAS_SEARCH_INDEX` se usar outro).
O plano gratuito (M0) tem limite pequeno de índices de busca (confira no Atlas); se estourar, mantenha `SEARCH_ENGINE=local`.

> Em qualquer caso, a **nota de similaridade (0–1), o motivo e a decisão** são calculados pela API, de forma igual nos dois motores.

## 6. Como confirmar que está tudo certo

```bash
curl http://localhost:8080/actuator/health                                   # UP = API alcança o Mongo
curl -u admin.tecnico:SENHA http://localhost:8080/api/v1/admin/diagnostico   # banco, coleções, contagens, índices de busca
```

Você também pode conferir pelo **MongoDB Compass** ou `mongosh "SUA_MONGODB_URI"` (`show collections`, `db.codigos_servico.countDocuments()`).

## 7. Problemas comuns

| Mensagem / sintoma | Causa e solução |
|---|---|
| `Authentication failed` / `bad auth` | Usuário/senha errados, senha sem URL-encode, ou usuário criado em outro banco: acrescente `?authSource=admin` (Atlas já usa `admin`). |
| `Timed out after 10000 ms while waiting for a server` | IP não liberado no Atlas (*Network Access*), VPN/proxy bloqueando, host errado, ou Mongo fora do ar. |
| `UnknownHostException` / erro de SRV (`mongodb+srv`) | DNS bloqueado na rede corporativa. Use a string **standard** (`mongodb://h1,h2,h3/...`, sem `+srv`) que o Atlas oferece para drivers mais antigos, ou ajuste o DNS do Docker. |
| Conecta, mas coleções "NÃO ENCONTRADA" nos logs | Faltou o nome do banco na URI (caiu em `test`) ou o banco tem outro nome. |
| `not authorized on ... to execute command` | Usuário sem `readWrite` no banco. |
| De dentro do Docker não alcança `localhost:27017` | Use `host.docker.internal` no lugar de `localhost`. Confirme que o Mongo escuta em `0.0.0.0`/aceita conexões da rede Docker. |
| Busca vazia com `SEARCH_ENGINE=atlas` | Índice não está READY, nome diferente de `ATLAS_SEARCH_INDEX`, ou cluster sem Atlas Search. Rode o diagnóstico: ele lista os índices e o status. |
| Aviso de índice único ao subir | Há duplicatas em `codigos_completos` (veja a consulta acima) — consolide e reinicie. |
| `Defina MONGODB_URI` ao rodar `docker compose` | O `.env` não existe/está vazio (`cp .env.example .env`). |

## 8. Segurança

- `.env` **nunca** vai para o Git; em servidor/CI use secrets do ambiente (a API só lê variáveis de ambiente).
- Usuário do banco **dedicado**, com o menor privilégio (`readWrite` no banco da aplicação); rotacione a senha periodicamente.
- Prefira TLS (Atlas já exige) e restrinja o acesso por rede/IP.
- A API não registra senha, token nem a URI nos logs.
