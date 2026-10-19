# Como Rodar

## Resumo — o que rodar na **sua** máquina?

| Sua situação | Comando | Onde rodar |
|---|---|---|
| Tem **Docker Desktop** instalado | `start.bat` (Windows, duplo clique) · `./start.sh` (Linux/macOS) | qualquer pasta |
| **Não tem Docker** | `dev.bat` (Windows, duplo clique) · `./dev.sh` (Linux/macOS) | qualquer pasta |

Os quatro scripts ficam na **raiz do repositório** e funcionam a partir de qualquer pasta:
eles voltam sozinhos para a raiz, conferem os pré-requisitos e dizem exatamente o que
falta instalar. Se preferir digitar os comandos, use as opções 1 e 2 abaixo — sempre
indicando **em qual pasta** cada comando roda.

## Onde fica cada pasta?

```
Desafio_GC_Gerdau/                ← RAIZ do repositório
│                                   (docker-compose.yml, start.bat, dev.bat, start.sh, dev.sh)
├── JAVA/
│   ├── backend/                 ← backend Java — os comandos do Maven rodam AQUI
│   └── frontend/                 ← frontend Node — os comandos npm rodam AQUI
├── IA/data/processed/            ← planilhas oficiais do Time de IA (somente leitura)
└── docs/                         ← esta documentação
```

> Nos comandos abaixo, **"raiz do repositório"** = a pasta que contém o `docker-compose.yml`
> (ex.: `C:\Users\seu.nome\IdeaProjects\Desafio_GC_Gerdau`).

## Opção 1 — Com Docker (recomendado quando houver Docker Desktop)

Pré-requisito: [Docker Desktop](https://www.docker.com/products/docker-desktop/) instalado **e aberto**.

| Passo | Comando | Pasta exata |
|---|---|---|
| Subir tudo (mongo + backend + frontend) | `docker compose up --build` | **raiz do repositório** |
| Parar | `docker compose down` | **raiz do repositório** |

| Serviço | URL |
|---|---|
| **Aplicação** | http://localhost |
| **API** | http://localhost:8080 |
| **Swagger** | http://localhost:8080/swagger-ui.html |
| **MongoDB** | mongodb://localhost:27017/gerdau_prototipo |

Na primeira subida o backend carrega **automaticamente** os dados oficiais do Time de IA
(`IA/data/processed/` — somente leitura), fazendo o merge de
`base_saneada.xlsx` × `matriz_de_para_gerdau.xlsx` e criando os índices (seção 14.3).

## Opção 2 — Sem Docker (máquina sem Docker Desktop)

Pré-requisitos instalados na máquina: [Java 17+](https://adoptium.net) e
[Node 20+](https://nodejs.org). Quanto ao banco, o padrão é o cluster Atlas do projeto
(só precisa de internet) — para usar outro, veja `MONGODB_URI` abaixo.

**Caminho fácil:** duplo clique em `dev.bat` (Windows) ou rode `./dev.sh` (Linux/macOS).
Ele instala as dependências do frontend na primeira vez e sobe backend + frontend em
janelas separadas.

**Passo a passo manual** (duas janelas de terminal):

| Passo | Comando | Pasta exata |
|---|---|---|
| 1 · backend | `mvnw.cmd spring-boot:run` (Windows) · `./mvnw spring-boot:run` (Linux/macOS) | `JAVA/backend/` |
| 2 · frontend (outra janela) | `npm install` (só na 1ª vez) e depois `npm run dev` | `JAVA/frontend` |

| Serviço | URL |
|---|---|
| **Aplicação** | http://localhost:5173 |
| **API** | http://localhost:8080 |
| **Swagger** | http://localhost:8080/swagger-ui.html |

> No modo nativo o frontend sobe no **5173** (Vite); no Docker ele é servido pelo nginx
> na **porta 80**. As duas formas conversam com a API na **8080**.

## Problemas comuns

| Sintoma | Causa | Solução |
|---|---|---|
| `start.bat` diz **"O Docker não está instalado"** | Máquina sem Docker Desktop | Use o `dev.bat` / `./dev.sh` (Opção 2) |
| `failed to connect to the docker API ... O sistema não pode encontrar o arquivo especificado` (ou *directory not found*) | Docker instalado, mas o **motor está parado** | Abra o Docker Desktop, espere o ícone da baleia ficar estável e rode o `start.bat` (ele detecta e avisa) |
| `ports are not available ... 8080: bind` | O **backend nativo** (`java`/IDE) ou o `npm run dev` seguram a porta | Pare os processos nativos antes de subir o Docker — o `start.bat` avisa quando a porta está ocupada |
| `no configuration file provided: not found` | Comando rodando **fora da raiz** do repositório | Rode na raiz (a pasta do `docker-compose.yml`) ou use o `start.bat` / `dev.bat` |
| `'java' não é reconhecido` / `'node' não é reconhecido` (ou `command not found`) | Java/Node não instalados | Instale [Java 17+](https://adoptium.net) e [Node 20+](https://nodejs.org) — os scripts avisam o que falta |
| 1ª execução demora bastante | Maven e npm baixando as dependências | Normal — nas próximas já usam o cache |
| Tela abre em branco / API não responde | Backend ainda subindo (Spring leva alguns segundos) | Aguarde e atualize a página; o nginx do Docker retenta sozinho |

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

| Teste | Comando | Pasta exata |
|---|---|---|
| Backend (similaridade, ranking, contexto) | `mvnw.cmd test` (Windows) · `./mvnw test` (Linux/macOS) | `JAVA/backend/` |
| Frontend (interface) | `npm test` | `JAVA/frontend` |
