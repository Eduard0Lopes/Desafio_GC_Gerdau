# Instalação e Uso

Guia completo para instalar, executar e usar o **Gerdau Service Code Standardization System**.

---

## 1. Pré-requisitos

| Forma | Pré-requisitos |
|---|---|
| **Docker (recomendado)** | [Docker Desktop](https://www.docker.com/products/docker-desktop/) (Windows/macOS) ou Docker Engine + Compose (Linux) |
| **Nativo** | JDK 17+, Maven 3.9+, Node 20+, MongoDB (local ou Atlas) |

---

## 2. Instalação e execução

### 2.1 Com Docker (uma linha)

```bash
docker compose up --build
```

A primeira execução:
1. sobe o **MongoDB** local;
2. o **backend** carrega automaticamente os dados oficiais do Time de IA
   (`IA/data/processed/base_saneada.xlsx` × `matriz_de_para_gerdau.xlsx` — merge) e cria os índices;
3. o **frontend** fica disponível em http://localhost.

| Serviço | URL |
|---|---|
| Aplicação | http://localhost |
| API | http://localhost:8080 |
| Swagger (OpenAPI) | http://localhost:8080/swagger-ui.html |
| Health / Ready | http://localhost:8080/api/v1/health · `/api/v1/ready` |
| MongoDB | mongodb://localhost:27017/gerdau_prototipo |

### 2.2 Nativo

```bash
# backend (http://localhost:8080)
cd JAVA/backend/
./mvnw spring-boot:run            # Windows: mvnw.cmd spring-boot:run

# frontend (http://localhost:5173)
cd JAVA/frontend
npm install
npm run dev
```

### 2.3 Apontar para outro banco

Defina a variável de ambiente `MONGODB_URI` (ver `.env.example`):

```bash
# exemplo (PowerShell)
$env:MONGODB_URI="mongodb+srv://USUARIO:SENHA@SEU-CLUSTER.mongodb.net/gerdau_prototipo"
./mvnw spring-boot:run
```

> **Onde os dados são gravados?** A aplicação grava sempre no banco configurado em
> `MONGODB_URI` — e a barra superior mostra o destino ativo (ex.: `● dados em:
> cluster0.fsmqcef.mongodb.net`). O padrão é o **cluster Atlas** (o mesmo do
> "Mongo web"): cadastros feitos no app aparecem lá imediatamente. Para o Mongo
> local do Docker, use `MONGODB_URI=mongodb://mongo:27017/gerdau_prototipo`.

---

## 3. Como usar

### 3.1 Tela principal — Vinculação
1. Em cada um dos **4 painéis** (Grupo de Serviço, Código de Fornecedor, Código de
   Serviço, Unidade de Medida), digite para buscar e escolha um item.
2. Com os quatro selecionados, clique em **Vincular Serviço**.
3. A prévia mostra o **código completo**, a descrição, a contagem de caracteres e a
   **decisão de similaridade** (PERMITIR / ALERTAR / REVISAR).
4. **Limpar** reinicia a seleção.

### 3.2 Cadastrar Novo Item
1. Clique em **Cadastrar Novo Item** (ou use a opção "＋ Cadastrar Novo Item" que
   aparece por último em cada busca).
2. Escolha o **Tipo de Componente**, informe a descrição (o contador
   `n/40 Caracteres SAP` acompanha a digitação) e a unidade.
3. **Verificar Similaridade** (também roda automaticamente ao digitar) mostra os
   itens semelhantes com **% Match** e o motivo.
4. **Salvar Cadastro** cria o item — ou, havendo duplicidade, abre o **Bloqueio de
   Duplicidade** com as opções: *Reutilizar Código Existente*, *Enviar para Governança*
   (com justificativa) ou *Voltar*.

### 3.3 Governança (perfil Admin)
1. Na barra superior, troque **Perfil: Admin**.
2. O menu passa a exibir **Governança**, com indicadores e a fila de itens:
   - **Revisão histórica**: registros da matriz com decisão DUPLICIDADE/ALERTA e o
     score real como evidência (*Consolidar* / *Manter como exceção*);
   - **Novos cadastros**: itens enviados para validação (*Aprovar* / *Rejeitar*).
3. Toda decisão registra **quem, quando e por quê** (`reviewedBy`/`reviewedAt`).

### 3.4 Permissões
- **Usuário**: consulta, cadastro e vinculação.
- **Admin**: tudo + governança. O servidor **bloqueia** a governança para não-admins
  (HTTP 403) — ver `docs/PERMISSOES.md`.

---

## 4. Testes

```bash
# backend — 23 testes (motor de similaridade, política, ranking, limite SAP,
# idempotência, auditoria e carga do contexto)
cd JAVA/backend/
./mvnw test

# frontend — 10 testes de interface (combobox, bloqueio de duplicidade, permissões)
cd JAVA/frontend
npm test
```

Resultado esperado: **backend 23/23 · frontend 10/10**, sem falhas.

---

## 5. Solução de problemas

| Sintoma | Causa provável | Solução |
|---|---|---|
| "Backend indisponível" na tela | API fora do ar | `docker compose up --build` / `./mvnw spring-boot:run` |
| Página antiga em cache | cache do navegador | `Ctrl+F5` |
| Porta 8080 ocupada | outro serviço | pare-o ou altere a porta no compose |
| Dados ausentes | seed não executou | confirme `SEED_ENABLED=true` e o diretório das planilhas |
| 403 na governança | perfil não-admin | troque o perfil para Admin |

---

## 6. Limitações conhecidas

- **Identidade por headers** (`X-Usuario-Id`/`X-Usuario-Papel`) é o modo de
  demonstração; em produção deve vir de OIDC (seção 13 do documento técnico).
- Os limiares de similaridade (0,88 / 0,70) são configuráveis e **provisórios** —
  recomenda-se recalibrar com amostra aprovada pela área.
- Integração com **SAP** está fora do escopo (apenas preparada em interfaces).
- O Mongo local do Docker é efêmero por volume; `docker compose down -v` recria tudo
  do zero a partir das planilhas oficiais.
