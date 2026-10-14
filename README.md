# Desafio GC Gerdau — Catálogo Padronizado de Serviços

Saneamento do cadastro de serviços SAP com IA/Dados, API Java (Spring Boot + MongoDB) e front-end Vue integrado.

```
IA/ (Python)                     JAVA/backend (Spring Boot)            JAVA/frontend (Vue 3)
base_saneada.xlsx                                                       
   │  pipeline.py                                                       
   ▼                                                                    
data/processed/nosql/*.json ──► DadosIaSeeder ──► MongoDB ◄── REST /api/** ◄── axios (/api via proxy Vite)
matriz_de_para / catálogo XLSX                                         
normalizacao.py + fuzzy_engine.py  ≡  TextoUtil + FuzzyEngine (mesmo algoritmo, testado com fixture compartilhada)
```

| Pasta | Conteúdo | Documentação |
|---|---|---|
| `IA/` | Pipeline de saneamento, matriz De-Para, catálogo e geração das coleções NoSQL | [IA/README.md](IA/README.md) |
| `JAVA/backend/desafio` | API REST (CRUD, código completo composto, similaridade, seeder) | [JAVA/backend/README.md](JAVA/backend/README.md) |
| `JAVA/frontend` | Interface Vue 3 + Vite consumindo a API | [JAVA/frontend/README.md](JAVA/frontend/README.md) |
| `docs/` | Arquitetura, modelo de dados e referência da API | [docs/ARQUITETURA.md](docs/ARQUITETURA.md), [docs/API.md](docs/API.md) |

## Como rodar tudo localmente

Pré-requisitos: Java 17, Node 22 (`^22.18` ou `>=24.12`), Python 3.10+, Docker (ou um MongoDB acessível).

```bash
# 1. MongoDB local
docker run -d --name mongo-gerdau -p 27017:27017 mongo:7

# 2. (opcional) regenerar os artefatos da IA — os JSONs já estão versionados
cd IA && pip install -r requirements.txt && python src/pipeline.py && cd ..

# 3. API importando os JSONs da IA no primeiro start
cd JAVA/backend/desafio
SPRING_MONGODB_URI=mongodb://localhost:27017/gerdau_local \
SPRING_MONGODB_DATABASE=gerdau_local \
APP_SEED_ENABLED=true ./mvnw spring-boot:run

# 4. Front-end (outro terminal)
cd JAVA/frontend && npm ci && npm run dev   # http://localhost:5173
```

As variáveis de ambiente sobrescrevem o `application.properties`, então dá para rodar local sem editar o arquivo.
Para usar o Atlas, configure a URI no `application.properties` (ou em `SPRING_MONGODB_URI`) e ligue o seeder uma vez.

## Testes

```bash
cd IA && python -m pytest tests                 # normalização, fuzzy (paridade com Java), migração
cd IA && python src/validar_prova.py            # auditoria de integridade dos artefatos
cd JAVA/backend/desafio && ./mvnw test          # unitários + integração com Testcontainers (requer Docker)
cd JAVA/frontend && npm run build
```

## Variáveis de configuração

| Variável / propriedade | Padrão | Uso |
|---|---|---|
| `SPRING_MONGODB_URI` / `spring.mongodb.uri` | definido em `application.properties` | Conexão MongoDB |
| `SPRING_MONGODB_DATABASE` / `spring.mongodb.database` | `gerdau_prototipo` | Banco |
| `APP_SEED_ENABLED` / `app.seed.enabled` | `false` | Importa `IA/data/processed/nosql/*.json` nas coleções vazias |
| `APP_SEED_DIR` / `app.seed.dir` | `../../../IA/data/processed/nosql` | Pasta dos JSONs (relativa ao diretório de execução) |
| `APP_CORS_ALLOWED_ORIGINS` / `app.cors.allowed-origins` | `http://localhost:5173,http://127.0.0.1:5173` | Origens liberadas no CORS |
| `VITE_API_URL` | `/api` | URL base usada pelo front |
| `VITE_PROXY_TARGET` | `http://localhost:8080` | Destino do proxy `/api` do Vite em dev |

> `JAVA/.env` não é lido por nenhuma parte do projeto (Spring não carrega `.env` sozinho). Ele foi mantido apenas como referência.
> Credenciais não devem ser versionadas: troque a senha do Atlas que está no histórico do Git.
