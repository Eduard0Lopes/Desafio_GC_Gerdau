# Como Rodar

## Opção 1 — Docker (recomendado)

Pré-requisito: [Docker Desktop](https://www.docker.com/products/docker-desktop/).

```bash
docker compose up --build
```

| Serviço | URL |
|---|---|
| **Aplicação** | http://localhost |
| **API** | http://localhost:8080 |
| **Swagger** | http://localhost:8080/swagger-ui.html |
| **MongoDB** | mongodb://localhost:27017/gerdau_prototipo |

Na primeira subida o backend carrega **automaticamente** os dados oficiais do Time de IA
(`IA/data/processed/` — somente leitura), fazendo o merge de
`base_saneada.xlsx` × `matriz_de_para_gerdau.xlsx` e criando os índices (seção 14.3).

## Opção 2 — Nativo

Pré-requisitos: JDK 17+, Maven 3.9+, Node 20+, MongoDB (o padrão é o cluster Atlas configurado).

```bash
# backend (sobe em http://localhost:8080)
cd JAVA/backend/desafio
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run

# frontend (http://localhost:5173, proxy /api → :8080)
cd JAVA/frontend
npm install
npm run dev
```

## Variáveis de ambiente (opcionais)

| Variável | Padrão | Descrição |
|---|---|---|
| `MONGODB_URI` | cluster Atlas do projeto | string de conexão |
| `MONGODB_DATABASE` | `gerdau_prototipo` | banco |
| `SEED_DIR` | auto-detecção (`IA/data/processed`) | planilhas oficiais (somente leitura) |
| `SEED_ENABLED` | `true` | carga automática dos dados oficiais |
| `ENSURE_INDEXES` | `true` | criação de índices na subida (seção 14.3) |
| `SEARCH_ENGINE` | `local` | motor de similaridade |
| `SIMILARITY_ENFORCEMENT` | `enforce` | `enforce` bloqueia duplicidades · `observe` apenas registra |
| `SIMILARITY_LIMIAR_DUPLICIDADE` | `0.88` | limiar de duplicidade |
| `SIMILARITY_LIMIAR_ALERTA` | `0.70` | limiar de alerta |
| `API_DOCS_ENABLED` | `true` | Swagger/OpenAPI |

## Testes

```bash
cd JAVA/backend/desafio
./mvnw test    # motor de similaridade, política de ranking e carga do contexto
```
