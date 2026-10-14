# Backend — Spring Boot 4 + MongoDB

Código em `desafio/`. Java 17, Spring Boot 4.1, Spring Data MongoDB, Bean Validation, Lombok.

```bash
cd desafio
# Mongo local + importação dos JSONs da IA (sem editar application.properties)
SPRING_MONGODB_URI=mongodb://localhost:27017/gerdau_local SPRING_MONGODB_DATABASE=gerdau_local \
APP_SEED_ENABLED=true ./mvnw spring-boot:run
./mvnw test   # requer Docker (Testcontainers sobe um mongo:7)
```

- Endpoints: [docs/API.md](../../docs/API.md). Arquitetura e regras: [docs/ARQUITETURA.md](../../docs/ARQUITETURA.md).
- O seeder só popula coleções vazias; para reimportar, apague as coleções/banco.
- Se o download do Maven pelo wrapper falhar (HTTP 429), use `MVNW_REPOURL=https://maven-central.storage-download.googleapis.com/maven2`.

| Pacote | Responsabilidade |
|---|---|
| `controller` | Rotas REST e validação de entrada |
| `service` | Regras de negócio, geração de IDs (`GeradorCodigoSequencial`), paginação (`ConsultaPaginada`) |
| `service.similaridade` | `FuzzyEngine`, `RankingPolicy`, `SimilaridadeService` (espelho do Python) |
| `model` / `repository` | Documentos MongoDB e acesso a dados |
| `dto` / `mapper` | Contratos de entrada/saída |
| `exception` | Exceções de negócio e `GlobalExceptionHandler` |
| `config` | CORS e índices |
| `seed` | `DadosIaSeeder` (importa `IA/data/processed/nosql`) |
| `util` | `TextoUtil` (normalização igual à do Python) |
