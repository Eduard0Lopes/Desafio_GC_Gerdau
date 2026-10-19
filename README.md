# Desafio GC — Gerdau · Padronização de Códigos de Serviço

Solução integrada para **sanear, padronizar e controlar** os códigos de serviço dos
contratos da Gerdau: dados oficiais do Time de IA mesclados no MongoDB + API Java com
filtro anti-duplicidade + interface web com governança (admin).

## Estrutura

```
├── IA/                     Time de IA & Dados — planilhas oficiais e pipeline (NÃO alterado)
│   └── data/processed/     base_saneada.xlsx × matriz_de_para_gerdau.xlsx  ← fonte do banco
├── JAVA/backend//   API Spring Boot (Mongo, similaridade, governança, permissões, Swagger)
├── JAVA/frontend/          Interface Vue 3 (fiel ao protótipo)
├── docs/                   Documentação técnica
└── start.bat / dev.bat     Atalhos para rodar — com Docker / sem Docker (Linux/macOS: start.sh / dev.sh)
```

## Rodando

| Sua máquina | Comando | Onde rodar |
|---|---|---|
| Tem **Docker Desktop** | `start.bat` (Windows, duplo clique) · `./start.sh` (Linux/macOS) | qualquer pasta |
| **Não tem Docker** | `dev.bat` (Windows, duplo clique) · `./dev.sh` (Linux/macOS) | qualquer pasta |

Os scripts ficam na raiz do repositório (a pasta do `docker-compose.yml`), funcionam de
qualquer lugar, conferem os pré-requisitos e avisam o que falta instalar. Preferindo os
comandos manuais, o caminho com Docker é `docker compose up --build` rodando **na raiz do
repositório**.

- **Aplicação:** http://localhost (com Docker) ou http://localhost:5173 (sem Docker)
- **API:** http://localhost:8080 · **Swagger:** http://localhost:8080/swagger-ui.html

Na primeira subida o backend **carrega automaticamente** os dados oficiais
(merge de `base_saneada.xlsx` × `matriz_de_para_gerdau.xlsx`) e cria os índices.

Comandos manuais **com a pasta exata de cada um**, problemas comuns e variáveis de
ambiente: [`docs/COMO-RODAR.md`](docs/COMO-RODAR.md).

## O que a solução faz

1. **Banco de dados oficial** — merge dos dados tratados com a matriz de-para do
   Time de IA: 9.795 registros legados → **7.308 códigos canônicos** (redução de 25,4%)
   com rastreabilidade completa (`legacyData` / `legacyIds`)
2. **Filtro de entrada anti-duplicidade** — a mesma regra (40% tokens + 60% fuzzy,
   política 0,88/0,70) vale para **G.S., C.F., C.S. e U.M.**
3. **Governança** — fila de aprovação pré-carregada com os **4.384** itens
   DUPLICIDADE/ALERTA do histórico (scores reais como evidência) + novos cadastros;
   registro de exceções auditado; **permissões admin × usuário**
4. **Índices** (seção 14.3) e **Swagger** completos

## Telas (fiéis ao protótipo)

| Tela | Descrição |
|---|---|
| **Vinculação** | 4 seletores com busca incremental → *Vincular Serviço* / *Cadastrar Novo Item* / *Limpar* |
| **Cadastrar Novo Item** | tipo, descrição (contador `n/40 Caracteres SAP`), unidade → *Salvar* / *Verificar Similaridade* / *Voltar* |
| **Bloqueio de Duplicidade** | itens semelhantes com `% Match` → *Reutilizar* / *Enviar para Governança* / *Voltar* |
| **Envio para Governança** | aviso de validação humana + justificativa → *Confirmar* |
| **Governança** *(admin)* | fila histórica + novos cadastros, indicadores, aprovar/revisar |

## Documentação

| Documento | Conteúdo |
|---|---|
| [`docs/COMO-RODAR.md`](docs/COMO-RODAR.md) | execução (Docker e nativo) e variáveis |
| [`docs/ARQUITETURA.md`](docs/ARQUITETURA.md) | arquitetura e decisões técnicas |
| [`docs/FLUXO_DE_DADOS.md`](docs/FLUXO_DE_DADOS.md) | **onde cada dado é gravado — endpoint → coleção → campo** |
| [`docs/MODELO_DADOS.md`](docs/MODELO_DADOS.md) | coleções, merge e índices |
| [`docs/CONTRATO_API.md`](docs/CONTRATO_API.md) | endpoints e payloads |
| [`docs/PROJETO_COMPLETO.md`](docs/PROJETO_COMPLETO.md) | documentação técnica completa (decisões, arquivos, evolução) |
| [`docs/PERMISSOES.md`](docs/PERMISSOES.md) | perfis admin × usuário e autorização |
| [`docs/DEMO.md`](docs/DEMO.md) | roteiro de demonstração (dia 19) |
| [`docs/analises/`](docs/analises/) | diagnóstico, EDA, hipótese de similaridade, impacto |
