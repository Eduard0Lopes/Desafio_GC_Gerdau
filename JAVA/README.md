# Padronização dos Códigos de Serviço — Gerdau

Backend (Spring Boot) + aplicativo desktop (JavaFX) + Docker, seguindo a documentação técnica do desafio.
O banco MongoDB **já existente** é usado como está: basta apontar a `MONGODB_URI`.

```
┌──────────────────┐  HTTPS/JSON   ┌────────────────────────┐        ┌──────────────────────┐
│  Desktop JavaFX  │ ────────────► │  API Spring Boot       │ ─────► │  MongoDB (seu banco) │
│  frontend/       │               │  backend/ (Docker)     │        │  + Atlas Search (opc)│
└──────────────────┘               └────────────────────────┘        └──────────────────────┘
```

## Começo rápido (5 minutos)

```bash
cp .env.example .env              # edite: MONGODB_URI e APP_DEV_PASSWORD
docker compose up --build         # sobe a API em http://localhost:8080
./frontend/run.sh                 # abre o aplicativo desktop (Windows: frontend\run.bat)
```

Sem banco à mão? Suba um MongoDB local **com dados de exemplo**:

```bash
docker compose --profile local-db up --build   # use a opção (C) da MONGODB_URI no .env.example
```

Entre com `solicitante` e a senha definida em `APP_DEV_PASSWORD`.

## Documentação

| Documento | Para quê |
|---|---|
| [docs/COMO-RODAR.md](docs/COMO-RODAR.md) | Passo a passo completo: Docker, desktop, testes, exemplos de API, variáveis |
| [docs/CONECTAR-MONGODB.md](docs/CONECTAR-MONGODB.md) | Conectar no seu MongoDB (Atlas, local, Docker), permissões, índices, Atlas Search, problemas comuns |
| [docs/DECISOES.md](docs/DECISOES.md) | O que foi implementado do PDF, decisões, diferenças e **limitações conhecidas** |

## Estrutura

```
backend/    API Spring Boot 3.5 (Java 21): busca, similaridade, cadastro, governança, auditoria
frontend/   Desktop JavaFX 21: login, seleção com autocomplete, cadastro, similaridade, governança
scripts/    Seed do MongoDB local e script dos índices do Atlas Search
docker-compose.yml, .env.example
```
