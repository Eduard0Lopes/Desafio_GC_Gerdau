# 📚 Material de Estudo — Tudo o que foi feito, explicado do zero

> **Para quem é este documento:** para você, que é leigo em Java/Spring, estudar e
> conseguir **explicar o sistema com suas palavras** para o comitê — e para o seu futuro
> profissional.
>
> **Como estudar:** cada parte é independente. Se tiver 1 hora, leia a **Parte 1, 2 e 5**.
> Se tiver um dia inteiro, leia tudo na ordem. No final há um roteiro de 3 dias.

---

## Índice

| Parte | Conteúdo | Tempo |
|---|---|---|
| 0 | O sistema em uma página | 5 min |
| 1 | Java moderno em 10 minutos (para quem nunca viu) | 15 min |
| 2 | Spring Boot descomplicado (anotações, camadas, injeção) | 25 min |
| 3 | Anatomia do projeto (cada pasta, cada classe, por que existe) | 15 min |
| 4 | **As 9 coleções do banco, campo por campo** | 30 min |
| 5 | **A história completa: cada decisão e o PORQUÊ** | 30 min |
| 6 | Código explicado trecho a trecho | 30 min |
| 7 | O fluxo completo de uma requisição (do clique ao banco) | 15 min |
| 8 | Glossário (falar a língua sem errar) | 10 min |
| 9 | Perguntas prováveis do comitê + respostas prontas | 15 min |
| 10 | Roteiro de 3 dias + o que dizer na apresentação | 10 min |

---

# Parte 0 — O sistema em uma página

**Problema:** a Gerdau compra milhares de serviços e o mesmo serviço era cadastrado com
várias descrições diferentes ("DIARIA RESGATISTA...", "RESGATISTA - DIARIA..."). Resultado:
7.961 códigos de serviço onde só 5.965 eram realmente diferentes — **25% de repetição**.

**Solução (nosso sistema):** uma aplicação web onde o usuário monta um **vínculo**
(GS + CF + CS + UM = Grupo de Serviço + Fornecedor + Código de Serviço + Unidade) e o
sistema:

1. **Verifica similaridade** antes de gravar (o texto é igual a algo já cadastrado?)
2. **Bloqueia repetições** oferecendo o item pronto para reutilizar
3. **Manda para governança** o que precisa de decisão humana (admin)
4. **Registra tudo na auditoria** (quem, quando, por quê)
5. **Através de painéis**, mostra o estado da base para a gestão

**Tecnologias:** Java 21 + Spring Boot (backend/API) · Vue 3 + TypeScript (interface) ·
MongoDB Atlas (banco "Mongo web") · Docker (empacotamento).

**Arquitetura em uma imagem mental:**

```
Navegador (Vue 3)  ──HTTP/JSON──►  Spring Boot (Java)  ──MongoDB Driver──►  Atlas
   telas e gráficos                  regras de negócio                      dados
                                     segurança/auditoria
```

---

# Parte 1 — Java moderno em 10 minutos

## 1.1 O que é Java

Java é uma linguagem **tipada** (toda variável tem um tipo declarado) e **orientada a
objetos** (tudo vive dentro de classes). O código compila para bytecode e roda na JVM.

## 1.2 Classe, objeto, método — a base de tudo

```java
public class Pessoa {                  // a CLASSE (a "forma de bolo")
    private String nome;               // um CAMPO (estado)

    public String saudar() {           // um MÉTODO (comportamento)
        return "Olá, " + nome;
    }
}

Pessoa p = new Pessoa();               // o OBJETO (o "bolo" feito com a forma)
```

- **`private`** = só a própria classe enxerga o campo (encapsulamento).
- **`public`** = qualquer código pode chamar.
- **`new`** = cria o objeto na memória.

## 1.3 Os tipos que você vai ver no projeto

| Tipo | O que é | Exemplo |
|---|---|---|
| `String` | texto | `"CS-130835"` |
| `Long`, `Double`, `int`, `long` | números | `999999L`, `0.88` |
| `Boolean` | verdadeiro/falso | `true` |
| `List<String>` | lista de textos | `List.of("CS-1", "CS-2")` |
| `Map<String, Object>` | dicionário chave→valor (parecido com JSON) | `Map.of("total", 42)` |
| `Instant` | data/hora | `Instant.now()` |
| `T` (genérico) | tipo definido depois | `List<MatrizDePara>` |

**`String` vs `string`:** em Java a classe se chama `String` (maiúsculo).

## 1.4 Nulidade — o famoso NullPointerException

Java permite `null` (vazio/ausente). Acessar um `null` derruba a aplicação. Por isso o
projeto tem este padrão em vários lugares:

```java
private static String nvl(String s) { return s == null ? "" : s.trim(); }
```

`nvl` = "null value" — converte nulo em texto vazio. **Por quê?** Dados vindos de
planilha Excel frequentemente têm células vazias.

## 1.5 `final`, `static` e `record`

- **`final`** = o valor não muda depois de atribuído (constante de uso).
- **`static`** = pertence à classe, não ao objeto (ex.: `SequenceService.MAXIMO`).
- **`record`** = classe de dados imutável em 1 linha. Usamos para DTOs:

```java
public record RevisaoMatrizRequest(String revisadoPor, String acao, String observacao) { }
```

Isso equivale a uma classe com 3 campos, construtor e getters — em uma linha.

## 1.6 Exceções (erros)

```java
try {
    // código que pode falhar
} catch (Exception e) {
    // tratamento
}
```

`throw new AcessoNegadoException("...")` = **lança um erro de propósito** quando a regra
de negócio é violada (ex.: usuário comum tentando usar a governança). Um "tradutor"
global converte essa exceção em uma resposta HTTP 403 bonitinha para o frontend.

---

# Parte 2 — Spring Boot descomplicado

## 2.1 O que é o Spring Boot?

É um **framework** (conjunto de bibliotecas prontas) que monta a estrutura de uma API
para você. Sem ele, você escreveria ~2.000 linhas de configuração. Com ele, você escreve
**anotações** (aquelas palavras com `@`) e o framework faz o resto.

**Analogia:** Spring é a construtora — você só descreve a casa (as anotações) e a
construtora levanta as paredes, liga a água e a luz.

## 2.2 As anotações que você precisa decorar

| Anotação | Onde | O que significa |
|---|---|---|
| `@SpringBootApplication` | classe principal | "esta é a aplicação, inicie aqui" |
| `@RestController` | classe de API | "esta classe responde requisições HTTP" |
| `@GetMapping("/x")` | método | "responde quando o navegador chama GET /x" |
| `@PostMapping("/x")` | método | "responde quando o navegador chama POST /x" (criar/ação) |
| `@PutMapping`, `@DeleteMapping` | método | atualizar / apagar |
| `@RequestBody` | parâmetro | "o JSON enviado pelo navegador vira este objeto" |
| `@PathVariable` | parâmetro | "pegue o valor da URL (ex.: /{id})" |
| `@RequestParam` | parâmetro | "pegue o valor da query (?decision=X)" |
| `@Service` | classe | "esta classe tem as REGRAS DE NEGÓCIO" |
| `@Repository` | classe | "esta classe conversa com o BANCO" |
| `@Component` | classe | "esta classe é administrada pelo Spring" (genérico) |
| `@Value("${chave:padrão}")` | campo | lê configuração do `application.properties` |
| `@Valid` | parâmetro | "valide os campos antes de processar" |
| `@Operation(...)` | método | documentação no Swagger |
| `@Document(collection=...)` | classe | "esta classe vira esta coleção no Mongo" |

## 2.3 Injeção de dependência — o coração do Spring

Em vez de a classe criar suas "ferramentas" com `new`, ela **declara o que precisa** e o
Spring entrega pronto. Veja `SequenceService`:

```java
public class SequenceService {
    private final MongoTemplate mongoTemplate;
    private final CodigoServicoRepository codigoServicoRepository;

    public SequenceService(MongoTemplate mongoTemplate,
                           CodigoServicoRepository codigoServicoRepository) {
        this.mongoTemplate = mongoTemplate;
        this.codigoServicoRepository = codigoServicoRepository;
    }
}
```

**Tradução:** "quem quiser me usar precisa me dar um `MongoTemplate` e um repositório —
eu não me viro sozinha". O Spring lê esse construtor e **injeta** as peças automaticamente.

**Por que fazer assim (e não `new`)?**
1. Troca de peça sem reescrever o código (ex.: testar com banco fake)
2. Facilita os testes (os 28 testes do backend injetam versões falsas/mock)
3. Mostra explicitamente as dependências de cada classe (documentação viva)

**Termo técnico:** isso se chama **IoC** (Inversão de Controle) — o controle de quem cria
quem é invertido: quem cria é o **container do Spring**, não o seu código.

## 2.4 As camadas — por que o código é dividido assim

```
HTTP chega → CONTROLLER (recebe/valida/entrega)
                 ↓
              SERVICE (aplica as regras de negócio)
                 ↓
              REPOSITORY (lê/grava no MongoDB)
                 ↓
              MongoDB
```

| Camada | Responsabilidade | Exemplo no projeto |
|---|---|---|
| **Controller** | traduz HTTP ↔ objeto; NUNCA tem regra de negócio | `CatalogoController`, `GovernancaController` |
| **Service** | regras: "não pode criar duplicado", "precisa de aprovação" | `CodigoCompletoService`, `GovernancaService` |
| **Repository** | consultas ao banco (Spring gera o código!) | `CodigoCompletoRepository` |
| **Model** | o formato dos dados (vira documento no Mongo) | `CodigoCompleto`, `MatrizDePara` |
| **Config** | configurações globais (índices, segurança) | `MongoIndexConfig`, `AutorizacaoInterceptor` |
| **Seed** | carga inicial dos dados oficiais | `DataSeeder` |

**Por que separar?** Se amanhã a regra "o que é duplicado?" mudar, você mexe **num
arquivo só** (o service). Se trocassemos de banco, mexeríamos só nos repositories.

## 2.5 O ciclo de vida de uma requisição

1. O Vue faz `fetch('/api/v1/catalogo/servicos', {...})`
2. O **filtro de correlação** gera um `correlationId` (rastreador da requisição)
3. O **interceptor de autorização** confere os headers `X-Usuario-Id`/`X-Usuario-Papel`
   (se a rota exige admin e o usuário não é, **403** na hora)
4. O **controller** mapeia a URL para o método certo, valida os campos (`@Valid`)
5. O **service** aplica as regras (similaridade, duplicidade, aprovação)
6. O **repository** grava no Mongo
7. O **AuditoriaService** registra o evento (quem/onde/quando)
8. A resposta volta como JSON com o status HTTP correto (200, 201, 409, 403...)

## 2.6 Maven (`pom.xml`) — de onde vêm as bibliotecas

O `pom.xml` é a "lista de compras" do projeto: Spring Web, Spring Data MongoDB, Apache
POI (ler Excel), SpringDoc (Swagger), JUnit (testes)... Comandos que você usa:

```bash
mvn test        # roda os 28 testes do backend
mvn spring-boot:run   # sobe o backend localmente
```

---

# Parte 3 — Anatomia do projeto

```
Desafio_GC_Gerdau/
├── IA/                       # DO TIME DE IA — SOMENTE LEITURA
│   └── data/processed/       # base_saneada.xlsx + matriz_de_para_gerdau.xlsx
├── JAVA/
│   ├── backend/desafio/      # API Spring Boot
│   │   └── src/main/java/com/gerdau/desafio/
│   │       ├── DesafioApplication.java      # ponto de partida
│   │       ├── controller/   # 4 controllers (API REST)
│   │       ├── service/      # 7 services (regras)
│   │       ├── repository/   # 8 repositórios (banco)
│   │       ├── model/        # 9 entidades (documentos do Mongo)
│   │       ├── seed/         # DataSeeder (carga dos xlsx)
│   │       ├── config/       # índices, autorização, filtros
│   │       └── util/         # NaturezaUtil, normalização
│   └── frontend/             # Vue 3 + TypeScript
│       └── src/
│           ├── views/        # 4 telas (Vinculação, Cadastro, Governança, Detalhes)
│           ├── components/   # gráficos, modais, combobox, toasts
│           ├── services/api.ts   # todas as chamadas HTTP
│           └── stores/       # tema (claro/escuro)
├── docker-compose.yml        # sobe TUDO com um comando
└── docs/                     # toda a documentação
```

**O que cada classe de serviço faz (e por que existe):**

| Classe | Existe para... |
|---|---|
| `DataSeeder` | transformar as 2 planilhas oficiais nas coleções do banco (só roda se o banco estiver vazio — idempotente) |
| `SequenceService` | gerar o próximo `CS-XXXXXX` sem colisão (vira `Contador` no banco) |
| `SimilaridadeService` | calcular "quão parecido é" um texto com a base (o motor) |
| `PreviewService` | o "semáforo": decidir PERMITIR / ALERTAR / REVISAR antes de cada gravação |
| `CodigoCompletoService` | criar/listar os vínculos (G.C.C.) com idempotência e exceções |
| `CatalogoService` | CRUD dos 4 componentes (GS/CF/CS/UM) — cria itens novos |
| `GovernancaService` | a fila de revisão, decisões humanas, estatísticas |
| `AuditoriaService` | gravar cada acontecimento na trilha de auditoria |

---

# Parte 4 — As 9 coleções do banco, campo por campo

> No MongoDB, um **banco** tem **coleções** (≈ tabelas) com **documentos** (≈ linhas,
> mas em JSON). Cada documento pode ter campos diferentes — aqui padronizamos tudo.

**Resumo rápido:**

| Coleção | Qtd | O que guarda |
|---|---|---|
| `matriz_depara` | 9.795 | histórico de similaridade do Time de IA (o "de-para") |
| `codigos_servico` | 7.313 | os códigos de serviço (CS) |
| `codigos_fornecedor` | 113 | os fornecedores (CF) |
| `grupos_servico` | 4 | os grupos (GS) |
| `unidades_medida` | 18 | as unidades (UM) |
| `codigos_completos` | 7.563 | os vínculos GS+CF+CS+UM (o produto final!) |
| `eventos_auditoria` | cresce | quem fez o quê, quando |
| `contadores` | 4 | os números de sequência dos IDs |
| `excecoes` | raro | pedidos formais de exceção (justificativas) |

---

## 4.1 `matriz_depara` — o histórico do Time de IA (9.795 documentos)

**Para que serve:** guarda o trabalho de análise do Time de IA: para cada descrição
legada, qual é o código canônico (`newId`) e se é ORIGINAL, DUPLICIDADE ou ALERTA.
É a **fila da governança** (os 4.384 itens "em revisão" vêm daqui).

**Como nasceu:** merge (junção) das duas planilhas:
- `base_saneada.xlsx` → a base saneada (descrições, fornecedor, SAP)
- `matriz_de_para_gerdau.xlsx` → as decisões de similaridade (scores, decisões)

| Campo | Tipo | Significado |
|---|---|---|
| `_id` | string | id interno do Mongo |
| `newId` | string | o código canônico, ex.: `CS-130835` |
| `descricaoCanonica` | string | descrição do serviço canônico |
| `grupoServico` | string | grupo ao qual pertence (`GS-001`) |
| `unidadeMedida` | string | unidade (`UM-07`) |
| `decision` | string | `ORIGINAL` / `DUPLICIDADE` / `ALERTA` (decisão técnica) |
| `natureza` | string | Mão de obra / Material / Equipamento / Locação / Outros |
| `score` | número | similaridade (0 a 1) entre o legado e o canônico |
| `reason` | string | por que o motor decidiu (ex.: "iguais após normalização") |
| `legacyData.legacyId` | string | id do registro antigo (legado) |
| `legacyData.legacyDescription` | string | descrição antiga |
| `legacyData.legacySource` | string | de onde veio (a planilha) |
| `legacyData.codigoFornecedor` | número | fornecedor SAP do legado |
| `legacyData.alertaSap` | boolean | descrição estoura o limite do SAP (40/70) |
| `legacyData.quantidadeCaracteres` | número | tamanho do texto |
| `statusRevisao` | string | `PENDENTE` / `REVISADO` (a governança mexe aqui) |
| `reviewedBy` | string | quem decidiu (matrícula) |
| `reviewedAt` | data | quando decidiu |
| `observacaoRevisao` | string | justificativa da decisão humana |

> **Detalhe técnico:** existem também `legacyId`, `legacyDescription` etc. **espelhados**
> no nível raiz — é uma redundância proposital para facilitar consultas e exportações
> (o documento técnico prevê os dois formatos).

**Exemplo (resumido):**

```json
{
  "newId": "CS-130835",
  "descricaoCanonica": "SV EQUIPE TELHADO",
  "decision": "DUPLICIDADE",
  "score": 0.94,
  "legacyData": {
    "legacyDescription": "SV-EQUIPE TELHADO",
    "codigoFornecedor": 200400010,
    "quantidadeCaracteres": 17
  },
  "statusRevisao": "REVISADO",
  "reviewedBy": "403921",
  "observacaoRevisao": "Consolidado em massa (DUPLICIDADE)"
}
```

---

## 4.2 `codigos_servico` — os códigos de serviço (7.313)

**Para que serve:** o catálogo canônico de serviços. É daqui que sai o `CS-XXXXXX`.

| Campo | Tipo | Significado |
|---|---|---|
| `_id` | string | o próprio `CS-XXXXXX` (id de negócio!) |
| `descricao` | string | descrição padronizada |
| `descricaoNormalizada` | string | versão minúscula/sem acento usada na busca |
| `quantidadeCaracteres` | number | tamanho (para o alerta do SAP) |
| `origem` | string | `BASE_SANEADA` / `MATRIZ` / `NOVO` (de onde veio) |
| `legacyIds` | lista | ids legados absorvidos (o "histórico" da consolidação) |
| `natureza` | string | Mão de obra / Material / Equipamento / Locação / Outros |
| `active` | boolean | `false` = excluído da lista ativa (nunca se apaga de verdade) |
| `createdAt` / `updatedAt` | data | auditoria de datas |

**Por que `_id` é o `CS-XXXXXX`?** Decisão nossa: o ID de negócio deve ser o próprio
identificador do documento — assim, `findById("CS-130835")` é direto e não existe
"chave técnica" escondida.

## 4.3 `codigos_fornecedor` — os fornecedores (113)

| Campo | Significado |
|---|---|
| `_id` | `CF-0443` |
| `codigoFornecedorSAP` | o número do SAP (ex.: `200400010`) |
| `nome` / `nomeNormalizado` | nome do fornecedor |
| `origem`, `active`, `createdAt`, `updatedAt` | igual ao anterior |

## 4.4 `grupos_servico` — os grupos (4)

| Campo | Significado |
|---|---|
| `_id` | `GS-001` |
| `nome` | ex.: "Civil", "Facilities", "Eletromecânica" |
| `nomeNormalizado`, `origem`, `active`, datas | apoio |

## 4.5 `unidades_medida` — as unidades (18)

| Campo | Significado |
|---|---|
| `_id` | `UM-07` |
| `sigla` | ex.: "H" (hora), "UN" (unidade) |
| `nome` | ex.: "Hora" |

## 4.6 `codigos_completos` — os VÍNCULOS (7.563) ★ o coração

**Para que serve:** cada documento é **um vínculo GS + CF + CS + UM** criado por alguém.
É o que a tela "Vinculação" monta.

| Campo | Significado |
|---|---|
| `_id` | id interno |
| `idCompleto` | `GS-001_CF-0443_CS-130835_UM-07` (único no banco!) |
| `gsId`, `cfId`, `csId`, `umId` | os 4 componentes referenciados |
| `descricaoSnapshot` | descrição no momento da criação (não muda se o catálogo mudar) |
| `usuarioCriacaoId` | matrícula de quem criou |
| `createdAt` | quando |
| `origem` | `SEED` (base oficial) ou `CADASTRO` (usuário) |
| `legacyIds` | legados que este vínculo absorveu |
| `status` | `ATIVO` / `PENDENTE` (aguardando validação) / `REJEITADO` |
| `decisaoSimilaridade` | `PERMITIR` / `ALERTAR` / `REVISAR` |
| `melhorScore` | o maior score de similaridade encontrado |
| `justificativa` | texto explicando a decisão automática |
| `similaresRegistrados[]` | quais itens parecidos existiam (id + score) |
| `aprovadoPor`, `aprovadoEm`, `observacaoAprovacao` | aprovação humana (governança) |
| `idempotencyKey` | chave única enviada pelo frontend |
| `idempotencyHash` | SHA-256 do conteúdo — impede o "duplo clique" |

**A regra de ouro:** o índice único em `idCompleto` é a **garantia física** de que a
mesma composição nunca será gravada duas vezes — mesmo que a regra de negócio falhasse,
o banco recusaria.

## 4.7 `eventos_auditoria` — a trilha (cresce a cada ação)

| Campo | Significado |
|---|---|
| `actorId` | quem (matrícula) |
| `action` | o quê (`CRIAR_VINCULO`, `REVISAO_HISTORICA`, `APROVAR_NOVO...`) |
| `entityType` / `entityId` | onde (`CODIGO_COMPLETO`, `CS-130835`) |
| `occurredAt` | quando |
| `decision` / `reason` | decisão e justificativa |
| `candidateIds[]` / `similarScores[]` | os parecidos encontrados (prova) |
| `correlationId` | id da requisição (liga o evento ao log do servidor) |

## 4.8 `contadores` — as sequências (4 documentos)

```json
{ "_id": "codigos_servico", "valor": 7253 }
```

**Para que serve:** o próximo `CS-` será `7254`. O `SequenceService` usa uma operação
atômica do Mongo (`findAndModify` com `$inc`) — se duas pessoas cadastrarem ao mesmo
tempo, **nunca sai o mesmo número**.

## 4.9 `excecoes` — pedidos formais de exceção

Quando alguém cria algo com justificativa (ex.: duplicidade aceita por motivo de força
maior), o pedido é guardado aqui: quem pediu, o que era, os similares, a justificativa e
a decisão tomada.

---

# Parte 5 — A história completa: cada decisão e o PORQUÊ

Aqui está **tudo que decidimos juntos**, na ordem em que aconteceu.

## 5.1 A stack (Java + Vue + MongoDB + Docker)

**Decisão:** backend Java/Spring, frontend Vue 3, banco MongoDB Atlas, tudo em Docker.
**Por quê?** O desafio pede Java + NoSQL; o Vue dá interatividade de SPA; o Docker faz
"rodar em qualquer lugar" com um comando só (o comitê pode testar).

## 5.2 A fonte dos dados — merge dos xlsx do Time de IA

**Decisão:** o banco nasce do **merge** de `base_saneada.xlsx` × `matriz_de_para_gerdau.xlsx`
(o `DataSeeder` faz isso). A pasta `IA/` é **somente leitura**.
**Por quê?** A análise oficial do Time de IA é a fonte da verdade; inventar dados seria
trapaça. O `nosql/` era material de teste e foi **ignorado**.

## 5.3 IDs de negócio: `CS-130835` (e não o formato 3/3/6/2)

**Decisão:** usar o `newId` oficial como ID dos documentos.
**Por quê?** O documento técnico traz os dois formatos; o `newId` é o que a equipe já
usa no dia a dia — menos ruído cognitivo. O formato antigo continua documentado.

## 5.4 `SequenceService` — IDs sem colisão

**Problema:** como gerar `CS-7254`, `CS-7255`... sem repetir se dois usuários
cadastrarem juntos?
**Solução:** coleção `contadores` + operação atômica `findAndModify` + `synchronized`
(a chave do método `proximoCodigoServico()`).
**Detalhe importante:** ao subir, o serviço **varre os IDs já existentes** e pula os
ocupados — foi uma correção quando a sequência "esgotou" após o seed oficial.

## 5.5 O motor de similaridade (40% + 60%)

**Decisão:** port fiel do `fuzzy_engine.py` do Time de IA para Java:

```
score final = 0,40 × Jaccard(tokens) + 0,60 × Ratcliff/Obershelp
```

- **Jaccard de tokens:** quantas palavras em comum (após limpar acentos, "de/da/do")
- **Ratcliff/Obershelp:** semelhança dos textos caractere a caractere (o algoritmo do
  `difflib` do Python)
- **Limiares:** `≥ 0,88` = DUPLICIDADE · `≥ 0,70` = ALERTA · abaixo = ORIGINAL
- **Regra numérica:** se a diferença for só de números (17/40 vs 34/40), o score é
  **limitado a 0,85** (nunca vira duplicidade automática) — números mudam o serviço

**Por quê esses números?** Calibração oficial da trilha do Time de IA. Estão no
`application.properties` — podem ser ajustados sem mudar código.

## 5.6 `SearchClient` pluggável (local | python)

**Decisão:** o motor vive atrás de uma interface; `local` (Java, padrão) ou `python`
(o serviço do Time de IA, se estiver no ar).
**Por quê?** O protótipo precisa rodar sozinho (sem depender do serviço Python), mas
mostrar que a integração está prevista.

## 5.7 `idCompleto` = `GS_CF_CS_UM`

**Decisão:** a chave única do vínculo é a concatenação dos 4 IDs.
**Por quê?** É legível ("de cara" se vê a composição) e o índice único impede duplicatas
**fisicamente**.

## 5.8 Anti-duplicidade em TODOS os componentes (o PreviewService)

**Decisão:** antes de gravar qualquer coisa (novo GS, CF, CS, UM ou vínculo), o sistema
faz um **preview** com o semáforo: PERMITIR / ALERTAR / REVISAR.
**Por quê?** O bug que você reportou — "não quero ver alerta de duplicidade ao vincular,
só no cadastro" — ensinou a separar os contextos: **criar** componente avisa; **vincular**
o que já existe mostra "Este vínculo já existe" (modal próprio).

## 5.9 Idempotência com SHA-256 (o "duplo clique")

**Decisão:** cada criação envia uma `idempotencyKey`; o backend guarda o **hash SHA-256**
do conteúdo. Reenviar o mesmo conteúdo = a mesma resposta, sem duplicar nada.
**Por quê?** Na vida real, rede instável e cliques repetidos acontecem; banco sério
não pode criar lixo por causa disso.

## 5.10 Governança — fila, decisões e reversão

**Decisão:** fila pré-carregada com os **4.384 itens DUPLICIDADE/ALERTA** da matriz +
novos cadastros; decisões MANTER / EXCLUIR / APROVAR / REJEITAR, todas com **confirmação
e auditoria**; qualquer decisão humana pode ser **revertida** (o item volta à fila).
**Por quê?** Dado oficial é sagrado: "excluir" **consolida** (tira da lista ativa), nunca
apaga — e todo arrependimento tem volta.

**Evolução final:** a **consolidação em massa** — os 4.244 "possíveis repetidos" (confiança
alta) foram consolidados com um clique (registrado na auditoria); sobraram os **140
"em atenção"** para decisão humana individual. **Por quê?** Rever 4.384 itens um a um é
impossível na prática; a decisão técnica da matriz já existe — o humano olha só o que é
ambíguo.

## 5.11 Perfis ADMIN × USUÁRIO

**Decisão:** headers `X-Usuario-Id` / `X-Usuario-Papel` (modo demonstração) + **proteção
no servidor** (rotas de governança retornam 403 sem ADMIN). OIDC/SSO está documentado
como evolução.
**Por quê?** Demo precisa ser simples de mostrar; mas a segurança real **não pode** viver
só no frontend.

## 5.12 Natureza (Mão de obra / Material / Equipamento / Locação)

**Decisão:** classificação por heurística de vocabulário (`NaturezaUtil`) aplicada na carga.
**Por quê?** O comitê perguntou "como vocês agrupam por tipo de gasto?" — a resposta
precisava existir nos dados, não só na conversa.

## 5.13 Docker Compose

**Decisão:** `docker compose up` sobe mongo + backend + frontend; `MONGODB_URI` aponta
para o Atlas por padrão.
**Por quê?** "Rodar em qualquer lugar" sem instalar Java, Node ou Mongo.

## 5.14 As correções de bugs (e o que cada uma ensinou)

| Bug | Causa | Correção |
|---|---|---|
| Textos com "Ã§" (mojibake) | o PowerShell gravava arquivos em ANSI | passamos a gravar UTF-8 via Node |
| Sequência esgotada (CS-999999) | contador abaixo dos IDs do seed | ao subir, o serviço pula IDs ocupados |
| Duplicado sobrescrito na criação | falta de índice único + verificação | índice único + idempotência |
| Donut com fatia zerada | Chart.js quebrava com valor 0 | filtro de fatias > 0 |
| Dark mode ilegível | faltava a variável `--texto-forte` | criada a variável de texto forte |
| Modal errado ao vincular | vínculo repetido era tratado como "item novo parecido" | modal "Este vínculo já existe" |
| Faltava aviso no "Novo registro" | botão só limpava | toast de confirmação |
| Gráficos "meh" | mostravam saneamento (uso único) | trocados por métricas de operação |
| Grade 3+1 desarmônica | `auto-fit` com 4 itens | colunas fixas (4/2/1) e alturas iguais |
| Scroll feia | padrão do navegador | scrollbar temática fina (claro/escuro) |
| IDs gritando mais que os nomes | estilos sem hierarquia | ID minimalista, nome em negrito |

---

# Parte 6 — Código explicado trecho a trecho

## 6.1 `application.properties` — o painel de configuração

```properties
spring.mongodb.uri=${MONGODB_URI:mongodb+srv://...mongodb.net/gerdau_prototipo...}
```

**Ler assim:** "use a variável de ambiente `MONGODB_URI`; se não existir, use este valor".
Isso permite trocar de banco **sem mudar código** (só variável de ambiente).

```properties
similaridade.limiar-duplicidade=${SIMILARITY_LIMIAR_DUPLICIDADE:0.88}
similaridade.limiar-alerta=${SIMILARITY_LIMIAR_ALERTA:0.70}
```

Os limiares — mesmos números do Time de IA — ajustáveis por configuração.

```properties
app.seed.enabled=${SEED_ENABLED:true}
```

Liga/desliga a carga inicial das planilhas.

## 6.2 `@Document` — a classe vira coleção

```java
@Document(collection = "codigos_completos")
public class CodigoCompleto {
    @Id
    private String id;
    private String idCompleto;
    ...
}
```

**Tradução:** "esta classe é gravada na coleção `codigos_completos`; o campo `id` é o
`_id` do documento". Cada `private` vira um campo do JSON.

## 6.3 Repository — consultas sem escrever consultas

```java
public interface CodigoCompletoRepository extends MongoRepository<CodigoCompleto, String> {
    Optional<CodigoCompleto> findByIdempotencyKey(String key);
    List<CodigoCompleto> findByStatusOrderByCreatedAtDesc(String status);
    long countByStatus(String status);
}
```

**Como funciona:** o Spring Data **lê o nome do método** e escreve a consulta para você!
`findByStatusOrderByCreatedAtDesc` = `WHERE status = ? ORDER BY createdAt DESC`.
`countByStatus` = `COUNT(*) WHERE status = ?`. Nenhuma linha de SQL/Mongo escrita.

**Por quê `Optional`?** O resultado pode não existir — `Optional` força o programador a
tratar o caso "não achei" (evita NullPointerException).

## 6.4 `SequenceService` — o gerador de IDs

```java
public static final long MAXIMO = 999999L;   // o formato CS- é de 6 dígitos

public synchronized String proximoCodigoServico() {
    // 1. pergunta ao banco o próximo número (operação atômica)
    // 2. se o CS- correspondente já existe, pula (varredura)
    // 3. formata: "CS-" + número com zeros à esquerda
}

private long proximoIncremento(String nome) {
    // findAndModify({ _id: nome }, { $inc: { valor: 1 } })
}
```

**Três defesas contra ID repetido:**
1. `$inc` atômico do Mongo (duas pessoas não puxam o mesmo número)
2. `synchronized` (uma chamada por vez dentro do servidor)
3. varredura dos IDs ocupados (correção nossa pós-seed)

## 6.5 `CodigoCompletoService.criar` — o método mais importante

Passo a passo do que este método faz (é o "Vincular" da tela):

1. **Idempotência:** se a `idempotencyKey` já existe → devolve o cadastro original
2. **Preview:** chama o `PreviewService` (semáforo de similaridade nos 4 componentes)
3. **Se REVISAR (bloqueio):** não grava; joga exceção com os candidatos parecidos —
   o frontend abre o modal "Já existe algo parecido, quer reutilizar?"
4. **Se PERMITIR/ALERTAR:** grava o `CodigoCompleto` com status `ATIVO` (ou `PENDENTE`
   se ALERTAR), guarda os similares encontrados e a decisão
5. **Exceções:** se o usuário criou mesmo com justificativa, grava em `excecoes`
6. **Auditoria:** registra tudo com `correlationId`
7. **Resposta:** devolve o documento criado (o frontend mostra o cartão de sucesso)

```java
public static String hashDoConteudo(CriarCodigoCompletoRequest r) {
    // SHA-256 de "gs|cf|cs|um|usuario" → 64 caracteres hex
}
```

**Por quê?** Se a mesma pessoa enviar a mesma composição duas vezes, o hash bate e o
sistema responde "já foi criado" — em vez de criar lixo.

## 6.6 `GovernancaService.consolidarEmMassa` — a ação em massa

```java
// 1. busca todos os PENDENTES de uma decisão (ex.: DUPLICIDADE)
// 2. marca cada um como REVISADO com reviewedBy/reviewedAt/observacao
// 3. salva tudo em lote (saveAll)
// 4. registra UM evento de auditoria "CONSOLIDACAO_EM_MASSA"
```

**Por quê `saveAll`?** Uma única ida ao banco para milhares de documentos (muito mais
rápido que salvar um a um).

## 6.7 `AutorizacaoInterceptor` — a segurança no servidor

```java
// antes de cada requisição em rotas protegidas:
// se X-Usuario-Papel != ADMIN → throw new AcessoNegadoException()  (vira HTTP 403)
```

**Por quê interceptor e não código dentro de cada método?** A regra vale para **todas**
as rotas de governança — escrita uma vez, aplicada em todas.

## 6.8 O semáforo do `PreviewService`

```
decisão = PERMITIR   → pode gravar (nada parecido)
decisão = ALERTAR    → grava, mas sinaliza (similaridade média ≥ 0,70)
decisão = REVISAR    → bloqueia (similaridade alta ≥ 0,88) e oferece os parecidos
```

Regra numérica: se a diferença for só de números, o score é limitado a **0,85** — ou
seja, **nunca** bloqueia sozinho.

---

# Parte 7 — O fluxo completo de uma requisição

**Cenário:** você seleciona GS + CF + CS + UM e clica **Vincular**.

```
1. Vue (VinculacaoView)
   └─ monta o corpo { gsId, cfId, csId, umId, usuarioCriacaoId }
   └─ gera idempotencyKey (timestamp + aleatório)
   └─ fetch POST /api/v1/codigos-completos  (headers X-Usuario-*)

2. Nginx (frontend) ──proxy──► backend:8080

3. CorrelationIdFilter
   └─ cria o correlationId e coloca no log

4. AutorizacaoInterceptor
   └─ confere o perfil (rota é de usuário comum? ok, segue)

5. CodigosCompletosController.criar
   └─ @Valid confere os campos obrigatórios

6. CodigoCompletoService.criar
   ├─ idempotencyKey já existe? → devolve o original
   ├─ PreviewService.avaliar(4 componentes)
   │    ├─ SimilaridadeService → scores (Jaccard 40% + Ratcliff 60%)
   │    └─ semáforo PERMITIR/ALERTAR/REVISAR
   ├─ REVISAR? → exceção com candidatos → modal no frontend
   └─ grava codigos_completos + eventos_auditoria

7. Resposta JSON (201) → Vue mostra o cartão de sucesso + "Novo registro"
```

E quando aparece o modal de bloqueio e você escolhe **Reutilizar**? O frontend não cria
nada — apenas **vincula o item existente** (gravando o vínculo com os IDs escolhidos),
com a mensagem "Este vínculo já existe" se a composição for idêntica.

---

# Parte 8 — Glossário

| Termo | Significado em português claro |
|---|---|
| **Spring Boot** | framework que monta a API para você |
| **Bean** | objeto administrado pelo Spring (criado/injetado por ele) |
| **IoC / Inversão de Controle** | quem cria os objetos é o framework, não você |
| **Injeção de dependência** | o Spring "entrega" as ferramentas que a classe precisa |
| **Controller** | camada que fala HTTP (recebe requisição, devolve resposta) |
| **Service** | camada das regras de negócio |
| **Repository** | camada de acesso ao banco |
| **DTO** | objeto de transporte (formato de entrada/saída da API) |
| **Record** | classe de dados imutável resumida em 1 linha |
| **Endpoint / rota** | um endereço da API (ex.: `POST /api/v1/codigos-completos`) |
| **REST** | estilo de API onde tudo é recurso + verbos HTTP |
| **JSON** | formato de dados em texto (o que a API troca) |
| **Idempotência** | fazer 1x ou Nx tem o mesmo efeito |
| **SHA-256** | impressão digital de um conteúdo (hash) |
| **Índice (banco)** | estrutura que deixa consultas rápidas; o `unique` impede duplicatas |
| **Interceptador** | código que roda antes das rotas (segurança, logs) |
| **Auditoria** | registro de quem fez o quê e quando |
| **Seed** | carga inicial de dados |
| **Maven** | gerenciador de dependências do Java (`pom.xml`) |
| **JVM** | a "máquina virtual" que executa o Java |
| **Swagger/OpenAPI** | documentação automática da API (`/swagger-ui.html`) |
| **SPA** | aplicação de página única (o Vue, sem recarregar a página) |
| **Container Docker** | "máquina virtual leve" com o sistema empacotado |
| **MongoDB / Atlas** | banco NoSQL em documentos JSON / o Atlas é a nuvem dele |
| **CRUD** | Criar, Ler, Atualizar, Apagar |

---

# Parte 9 — Perguntas prováveis do comitê + respostas

**P: Como vocês evitam duplicidade?**
R: Em três camadas: (1) **preview de similaridade** antes de gravar (bloqueia ≥ 0,88);
(2) **índice único** em `idCompleto` e `idempotencyKey` no banco; (3) **idempotência**
com hash — reenvio não duplica.

**P: Por que 0,88 e 0,70?**
R: Calibração oficial do motor do Time de IA (40% Jaccard + 60% Ratcliff). Estão em
configuração e podem ser reajustados sem mudar código.

**P: E se o sistema bloquear algo indevido?**
R: O usuário justifica (exceção registrada) e o admin decide na governança — e toda
decisão humana pode ser **revertida**.

**P: O banco "apaga" dados?**
R: Nunca. Exclusão é lógica (`active: false` / `status: REJEITADO`); a matriz mantém os
legados e a auditoria guarda o histórico completo.

**P: Como escala para milhares de usuários?**
R: Os índices do Mongo (seção 14.3 do documento técnico) cobrem as consultas quentes;
a aplicação é stateless (dá para colocar várias instâncias atrás de um load balancer);
o Atlas já escala horizontalmente.

**P: E a segurança?**
R: Hoje: perfis por headers (demo) + 403 no servidor. Evolução documentada: OIDC/SSO
com o provedor corporativo.

**P: Qual foi a maior dificuldade?**
R: (Escolha uma real sua — sugestão:) Integrar a análise do Time de IA (planilhas) com
o modelo operacional sem perder a fidelidade dos dados — resolveu-se com o merge
documentado campo a campo e a carga idempotente.

**P: Vocês inventaram dados?**
R: Não. A base é o merge oficial `base_saneada.xlsx` × `matriz_de_para_gerdau.xlsx`;
a pasta `IA/` é somente leitura.

---

# Parte 10 — Roteiro de 3 dias + o que dizer na apresentação

## Roteiro de estudo

**Dia 1 (2h):** Parte 0, 1 e 2 — entenda a arquitetura e a linguagem. Depois abra
`CodigoCompletoService.java` e tente reconhecer as anotações.

**Dia 2 (2h):** Parte 4 e 5 — decore as coleções (sobretudo `codigos_completos` e
`matriz_depara`) e as **decisões com seus porquês**. É o que mais cai em avaliação.

**Dia 3 (2h):** Parte 6, 7 e 9 — o fluxo do clique ao banco e as perguntas do comitê.
Ensaiando em voz alta o fluxo da Parte 7.

## O mapa mental da apresentação (fale esta ordem)

1. **Problema:** 25% de repetição no catálogo de serviços
2. **Solução:** vínculo GS+CF+CS+UM com anti-duplicidade em 3 camadas
3. **Dado oficial:** merge do Time de IA (9.795 análises → 7.308 serviços)
4. **Motor:** 40% Jaccard + 60% Ratcliff, limiares 0,88/0,70 (calibrados pela IA)
5. **Governança:** fila técnica + decisão humana + reversão + auditoria
6. **Resultado:** 25,4% menos repetições; 4.244 consolidados em massa; 140 para revisão
7. **Extra que diferencia:** natureza por heurística, idempotência SHA-256, Docker,
   documentação completa

## Frases de ouro (se travar, diga uma dessas)

- "O banco nunca apaga — consolida. Toda decisão tem volta e auditoria."
- "A garantia final é física: índice único no banco, não só regra de código."
- "A decisão técnica veio do Time de IA; o humano decide só o que é ambíguo."
- "Os limiares são configuração, não código — dá para recalibrar sem redesenvolver."

---

## Leituras complementares (nesta pasta)

| Documento | Para quê |
|---|---|
| `ARQUITETURA.md` | visão técnica dos componentes |
| `MODELO_DADOS.md` | o modelo do banco em detalhe |
| `CONTRATO_API.md` | todos os endpoints |
| `FLUXO_DE_DADOS.md` | onde cada dado grava, linha por linha |
| `PERMISSOES.md` | regras de perfil |
| `DEMO.md` | roteiro da demonstração |
| `ALINHAMENTO_COMITE.md` | respostas Q1/Q2/Q3 do comitê |

---

**Boa sorte no Caldeira e na apresentação!** 🚀
Você construiu um sistema de verdade — com dados oficiais, regras claras, segurança,
auditoria e documentação. Qualquer pergunta do comitê, você tem a resposta aqui.
