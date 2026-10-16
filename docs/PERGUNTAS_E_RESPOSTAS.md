# 🎯 Perguntas e Respostas — Reunião com a Gerdau

> **Para o time estudar antes da reunião de segunda.**
> Cada pergunta tem a resposta sugerida + onde no sistema está a prova.
> Agrupadas por frente. As **Partes 8 e 9 são as mais importantes** — são as perguntas
> difíceis e as honestas sobre limitações.

**Como usar:**
- Cada pessoa do time pega uma parte para dominar (e lê as outras por cima)
- Ensaiem em voz alta — a resposta precisa sair com as SUAS palavras
- Se não souberem uma resposta: **"Boa pergunta, vamos confirmar e retornamos"** — nunca inventar

---

## Índice

| Parte | Tema | Qtd |
|---|---|---|
| 1 | Produto e valor | 10 |
| 2 | Dados e a parceria com o Time de IA | 10 |
| 3 | Motor de similaridade | 10 |
| 4 | Arquitetura e tecnologia | 10 |
| 5 | Segurança e governança | 10 |
| 6 | Operação e manutenção | 8 |
| 7 | Integração e evolução | 8 |
| 8 | **Perguntas difíceis / pegadinha** | 10 |
| 9 | **O que NÃO está pronto (e o que diríamos)** | 8 |
| 10 | Processo, time e decisões | 8 |

---

# Parte 1 — Produto e valor

**1.1 Qual problema isso resolve?**
O mesmo serviço era cadastrado com descrições diferentes por pessoas/fornecedores
diferentes. Na base analisada: 7.961 códigos para 5.965 serviços realmente distintos —
**25,4% de repetição**. Isso gera retrabalho de compra, dificulta análise de gasto e
polui o catálogo.

**1.2 Qual é o ganho concreto?**
Menos repetição no catálogo (25,4% identificados), cadastro com verificação automática
antes de gravar, fila de governança para o que precisa de decisão humana e auditoria
completa. Na prática: **o cadastro errado nem entra**.

**1.3 Quem usa o sistema?**
Dois perfis: **usuário comum** (cadastra e vincula serviços) e **admin/governança**
(revisa, decide, acompanha painéis e exportações).

**1.4 O que diferencia de uma planilha bem feita?**
A planilha não bloqueia nada. Aqui: verificação de similaridade em tempo real, bloqueio
com sugestão de reutilização, idempotência (duplo clique não duplica), índice único no
banco, auditoria de cada decisão e reversibilidade.

**1.5 Isso substitui o sistema X da Gerdau?**
Não propomos substituir nada hoje — é um protótipo de padronização. A integração com
sistemas existentes (SAP, catálogo atual) está no roadmap e é conversa com vocês.

**1.6 Quanto tempo leva para um usuário aprender?**
A interface foi desenhada para ser autoexplicativa (busca tipo Google, avisos em
linguagem simples, prevenção de erros). Estimativa: **menos de 30 minutos** para o fluxo
de cadastro; a governança exige um treinamento curto do processo decisório.

**1.7 E se o usuário não gostar do bloqueio?**
O bloqueio sempre oferece caminho: reutilizar o item pronto OU justificar e prosseguir
(com registro). Nada é engessado — tudo passa por decisão registrada.

**1.8 Como medimos o sucesso?**
Métricas do próprio painel: nº de duplicidades evitadas, tempo de cadastro, tamanho da
fila de revisão, redução de novas repetições ao longo do tempo.

**1.9 Qual o custo de operação?**
Como protótipo: um cluster Mongo (Atlas) + containers. Em produção na Gerdau, rodaria
sobre a infra existente — precisa de levantamento com vocês.

**1.10 Por que "padronização de códigos" é prioridade?**
Catálogo limpo é base para análise de gasto por categoria, negociação com fornecedores e
automação de compras — sem padronizar, todo indicador de serviço vem poluído.

---

# Parte 2 — Dados e a parceria com o Time de IA

**2.1 De onde vieram os dados?**
Exclusivamente das planilhas oficiais do Time de IA: `base_saneada.xlsx` ×
`matriz_de_para_gerdau.xlsx`. **Nenhum dado foi inventado.** A pasta do Time de IA é
somente leitura no projeto — nunca alterada.

**2.2 O que exatamente está no banco hoje?**
`matriz_depara` 9.795 análises · `codigos_servico` ~7.316 · `codigos_completos` ~7.563 ·
`codigos_fornecedor` 113 · `unidades_medida` 18 · `grupos_servico` ~5 — mais auditoria,
contadores e exceções. (Números exatos: painel Detalhes em tempo real.)

**2.3 Como o merge das planilhas foi feito?**
Um processo único e documentado (`DataSeeder`): cada linha das planilhas vira um
documento com os campos mapeados um a um (documentado em `docs/FLUXO_DE_DADOS.md`).
O processo é **idempotente** — rodar de novo não duplica nada.

**2.4 E se um dado oficial estiver errado?**
A governança existe para isso: o item entra na fila, o admin decide (aceitar/apagar) e a
decisão fica registrada. O dado original nunca é destruído — fica no histórico.

**2.5 Os dados são reais da Gerdau?**
São os dados que o Time de IA nos entregou (base saneada real do domínio). Se para
produção forem necessários outros recortes, a carga se ajusta — o processo de merge é
parametrizado.

**2.6 Como atualizar a base com novas planilhas?**
O seeder é incremental (só insere o que falta) e as APIs de importação estão previstas.
Para um processo recorrente de carga, propomos um job agendado — item de roadmap.

**2.7 O que é "natureza" (Mão de obra, Material...)?**
Classificação por tipo de gasto, gerada por heurística de vocabulário sobre as
descrições. Responde a uma necessidade de análise por categoria. Pode ser refinada com
regras de negócio de vocês.

**2.8 Por que 25,4% de redução?**
É a razão entre códigos canônicos e o total de descrições analisadas pela matriz
(5.965 ÷ 7.961). É o número da análise oficial do Time de IA, não nossa.

**2.9 E as descrições que estouram o limite do SAP?**
O sistema sinaliza (alerta SAP) conforme o tamanho do texto — visível na matriz e na
tela de cadastro, com contador de caracteres.

**2.10 Os dados pessoais (matrículas) — LGPD?**
Guardamos apenas matrícula (identificador funcional) e ações realizadas — trilha de
auditoria, sem dados pessoais sensíveis. A política completa de retenção é um alinhamento
a fazer com o jurídico de vocês.

---

# Parte 3 — Motor de similaridade

**3.1 Como o sistema sabe que dois textos são o mesmo serviço?**
Motor de similaridade: **40% Jaccard de tokens** (palavras em comum, ignorando
acentos/palavras vazias) + **60% Ratcliff/Obershelp** (semelhança caractere a caractere).
Resultado de 0 a 1.

**3.2 De onde vêm os limiares 0,88 e 0,70?**
Da calibração oficial do Time de IA (o motor em Python usa os mesmos). Não inventamos:
o motor Java é um **port fiel** do `fuzzy_engine.py` e os limiares são configuráveis.

**3.3 Por que 40/60 e não 50/50?**
A ponderação foi a que melhor reproduziu as decisões da análise oficial. Testável:
comparamos o resultado do motor com a matriz do Time de IA.

**3.4 O que acontece com números diferentes (17/40 vs 34/40)?**
Regra numérica: se a diferença for só de números, o score é **limitado a 0,85** — nunca
vira duplicidade automática. Por quê? Números mudam o serviço (quantidade, tamanho,
modelo) — decisão arriscada demais para automação.

**3.5 E se o motor errar (falso positivo/negativo)?**
Falso positivo → o usuário justifica e prossegue (registrado). Falso negativo → entra
normalmente e pode ser pego na governança. Nenhuma decisão automática é definitiva:
**tudo tem recurso e auditoria**.

**3.6 Vocês usam IA/LLM para isso?**
Não é necessário para o problema (similaridade textual é bem resolvida com os algoritmos
acima, auditáveis e determinísticos). LLM entra como evolução possível — ex.: sugerir
natureza ou agrupar sinônimos — mas sempre com decisão humana.

**3.7 O motor é o mesmo do Time de IA?**
Sim, em dois sentidos: mesmos algoritmos (port fiel) e mesmos limiares. Há ainda a opção
de usar o serviço Python do Time de IA em runtime (configuração `SEARCH_ENGINE=python`).

**3.8 Por que não usar embeddings/vetores?**
Poderíamos (evolução prevista). Hoje: algoritmos clássicos são leves, explicáveis e
já calibrados. Para 7 mil textos, a busca atual responde em milissegundos.

**3.9 Quanto tempo leva uma verificação?**
Milissegundos — a busca usa índices no Mongo e o cálculo roda em memória.

**3.10 Como recalibrar se os limiares não servirem para a realidade de vocês?**
São variáveis de configuração (`SIMILARITY_LIMIAR_DUPLICIDADE`, `SIMILARITY_LIMIAR_ALERTA`)
— mudam sem alterar código, e podemos revalidar com uma amostra da base real.

---

# Parte 4 — Arquitetura e tecnologia

**4.1 Qual a stack e por quê?**
Java 21 + Spring Boot (padrão corporativo, robusto) · MongoDB (NoSQL, flexível para o
documento de código) · Vue 3 (interface moderna) · Docker (execução em qualquer lugar).
Tudo alinhado ao documento técnico do desafio.

**4.2 Por que MongoDB e não relacional?**
O documento técnico prevê NoSQL; o modelo é documental (o documento do código completo
carrega histórico, similares e decisões juntos — leitura de um documento só). E os dados
de origem são planilhas semiestruturadas.

**4.3 O banco aguenta a base inteira da Gerdau?**
A base de teste tem ~10 mil documentos; os índices do Mongo (seção 14.3 do documento
técnico) cobrem as consultas quentes. Para volumes maiores, o Atlas escala horizontalmente
— seria um dimensionamento a fazer com vocês.

**4.4 E se dois usuários cadastrarem ao mesmo tempo?**
Duas proteções: sequência atômica de IDs (`findAndModify`) e índice único em
`idCompleto`/`idempotencyKey`. Concorrência não gera duplicata — testado.

**4.5 O que acontece se o servidor cair no meio de um cadastro?**
Ou grava tudo, ou não grava nada (transação de documento). E a idempotência protege o
retry: reenviar não duplica.

**4.6 Por que Docker?**
"Rodar em qualquer lugar" com um comando: `docker compose up`. Sem instalar Java, Node
ou Mongo. Facilita a avaliação de vocês e a futura homologação.

**4.7 Onde isso roda hoje?**
Localmente com um comando e no banco "Mongo web" (Atlas) compartilhado do projeto — é
para onde todos os cadastros vão.

**4.8 Tem API documentada?**
Sim: Swagger/OpenAPI automático (`/swagger-ui.html`) + `docs/CONTRATO_API.md` com todos
os endpoints, exemplos de corpo e respostas.

**4.9 Por que frontend separado (e não tudo no Java)?**
Separação de responsabilidades: a interface evolui sem tocar na API; permite reuso da API
por outros consumidores (portais, automações) — importante para a evolução com vocês.

**4.10 Como testaram?**
28 testes de backend (regras de negócio: duplicidade, idempotência, sequência,
permissões) + 10 de frontend + validação manual do fluxo completo ponta a ponta.

---

# Parte 5 — Segurança e governança

**5.1 Como está a segurança hoje?**
Perfis (usuário/admin) validados **no servidor** (rotas de governança retornam 403 sem
admin), trilha de auditoria completa e proteção contra duplicação. É um protótipo: o
login real (SSO) é a primeira evolução.

**5.2 Por que o login é por header?**
Modo demonstração — permite avaliar os dois perfis sem depender de infra de identidade.
A evolução documentada é OIDC/SSO com o provedor corporativo de vocês.

**5.3 O que impede um usuário comum de revisar a governança?**
O servidor recusa (403) — não é escondida a tela, é negada a operação. Testado.

**5.4 Toda decisão fica registrada?**
Sim: quem, quando, o quê, decisão, justificativa e os candidatos parecidos encontrados
(a prova da decisão automática). Auditoria com correlação de requisição.

**5.5 E se alguém decidir errado na governança?**
Toda decisão humana pode ser **revertida** — o item volta para a fila e a reversão também
fica registrada. Nada é definitivo nem silencioso.

**5.6 "Apagar" apaga mesmo?**
Não. É exclusão lógica (sai da lista ativa) — o registro original permanece no histórico.
O banco nunca perde dado oficial.

**5.7 Como funciona a proteção contra duplo clique/envio repetido?**
Idempotência: cada criação tem uma chave; o conteúdo é "assinado" com SHA-256. Reenvio =
mesma resposta, sem duplicar.

**5.8 E auditoria da auditoria?**
Os eventos são append-only (inserção única) — não há API de edição/apagamento de eventos.

**5.9 Conformidade LGPD?**
Guardamos matrícula + ação (dados funcionais mínimos). Para produção, alinhar retenção e
direito de exclusão com o jurídico de vocês — item de próximo passo.

**5.10 Como controlamos acesso ao banco?**
Credenciais por variável de ambiente (não hardcoded como regra), usuário dedicado no
Atlas. Em produção: rede privada/VPN e gestão de segredos (vault) — recomendação.

---

# Parte 6 — Operação e manutenção

**6.1 Como se sobe o sistema?**
Um comando: `docker compose up` — ou via JAR/Node para quem prefere rodar fora de
container. Documentado em `docs/COMO-RODAR.md` e `docs/INSTALACAO_E_USO.md`.

**6.2 Tem como saber se está saudável?**
Sim: endpoints `/health` e `/ready` (usados inclusive pelo Docker para monitorar).

**6.3 E os logs?**
Log estruturado com correlation id — dá para rastrear uma requisição do frontend ao banco.

**6.4 Como faz backup?**
Hoje: o Atlas tem backup gerenciado. Para produção, política de retenção e teste de
restore — item a combinar com a infra de vocês.

**6.5 E se a memória/espaco acabar?**
O Atlas escala armazenamento; a aplicação é stateless (pode reiniciar sem perda).
Monitoramento é item de produção (ver Parte 9).

**6.6 Atualizações quebram dados antigos?**
O schema tem controle de versão e o seed é idempotente; mudanças de modelo seguem
migração documentada (padrão Schema Versioning do documento técnico).

**6.7 Quem mantém?**
Como projeto: nosso time. Em produção: transição para o time de vocês com documentação
completa (arquitetura, contrato de API, modelo de dados, fluxo linha a linha).

**6.8 Tem relatórios/exportação?**
Sim: exportação CSV de resumo, detalhamento e decisões — direto pelo painel de detalhes.

---

# Parte 7 — Integração e evolução

**7.1 Integra com o SAP?**
Hoje consumimos os códigos de fornecedor SAP como dado. A integração em tempo real
(consulta/gravação) é evolução natural — depende do ambiente e APIs de vocês.

**7.1.2 Quais as próximas funcionalidades previstas?**
1) SSO/identidade real · 2) notificações (fila por e-mail/Teams) · 3) importação
recorrente de planilhas · 4) busca vetorial/sinônimos com IA · 5) integração SAP ·
6) relatórios gerenciais.

**7.2 Como entra a sugestão de novos sinônimos?**
Hoje pelo motor textual; evolução: aprendizado com as decisões humanas (os "aceitar/
apagar" alimentam o recalibrar) e embeddings para sinônimos ("resgatista" ≈ "socorrista").

**7.3 Dá para multi-unidade/multi-empresa?**
O modelo já guarda grupo/fornecedor/unidade — a granularidade organizacional (empresa,
centro de custo) seria um campo novo — trivial de adicionar, conversa de requisito.

**7.4 Dá para rodar on-premise na Gerdau?**
Sim — Docker/JAR padrão; o Mongo pode ser gerenciado ou on-premise. Nada amarra à nuvem.

**7.5 E APIs para outros sistemas consumirem?**
Já existe: a API REST é a mesma usada pela interface — outros sistemas podem consumir já
hoje (com os mesmos controles de perfil).

**7.6 Tem roadmap com prazos?**
Proposta de piloto: validar com um time real por algumas semanas, coletar métricas do
painel e decidir as integrações — prazos a combinar com vocês segunda.

**7.7 Aceita customização de regras (limiares, naturezas, campos)?**
Sim — limiares e classificações são configuração/regra isolada, não código espalhado.
Regras novas = mudança pontual em um service.

**7.8 Como fica a migração do catálogo atual da Gerdau?**
Processo igual ao que fizemos: análise de similaridade sobre a base real → matriz de
de-para → fila de governança para exceções. O caminho já está provado.

---

# Parte 8 — Perguntas difíceis / pegadinha ⚠️

**8.1 "Isso parece bom demais — onde está o truque?"**
Sem truque: os 25,4% vêm da análise oficial do Time de IA, o motor é auditável e as
decisões são registradas. Convidamos a testar qualquer fluxo na hora.

**8.2 "E se eu cadastrar algo agora, aparece no banco de verdade?"**
Sim — é a nossa aposta de transparência: mostramos o cadastro e o documento no banco ao
vivo.

**8.3 "Vocês confiam 100% no motor automático?"**
Não — por isso a governança humana. Automação propõe, humano decide, auditoria registra.
Esse é o desenho, não um remendo.

**8.4 "Por que tem itens 'em atenção' ainda?"**
Escopo proposital: os 140 casos ambíguos são exatamente os que precisam de olho humano.
Os 4.244 claros foram consolidados em massa (registrado) — mostramos como.

**8.5 "Isso é uma prova de conceito de fim de semana?"**
Não: modelo de dados versionado, testes automatizados, segurança no servidor, auditoria,
idempotência, índices, Docker e documentação técnica completa. É um MVP de engenharia.

**8.6 "Como vocês sabem que o número de redução está certo?"**
Reproduzível: a matriz do Time de IA está no banco e o cálculo (5.965/7.961) pode ser
conferido por qualquer pessoa.

**8.7 "E se dois admins decidirem diferente sobre o mesmo item?"**
A última decisão vence e TODAS ficam registradas (com reversibilidade) — o conflito é
visível na auditoria.

**8.8 "Por que não aproveitaram o sistema X que já temos?"**
Não tivemos acesso a ele no desafio — mas o desenho (APIs abertas) foi pensado para
integrar, não substituir. Segunda queremos ouvir vocês sobre os pontos de integração.

**8.9 "O que vocês fariam diferente?"**
(Sejam honestos — sugestões:) Testar mais cedo com usuários reais; ter definido o modelo
de permissões definitivo (SSO) desde o início; automatizar a carga recorrente desde a
primeira versão.

**8.10 "Dá para confiar para operação real amanhã?"**
Para piloto com supervisão: sim. Para operação crítica: falta o pacote de produção
(Parte 9) — e preferimos dizer isso antes de prometer.

---

# Parte 9 — O que NÃO está pronto (e como responder) ⚠️

> **Princípio: honestidade estratégica.** Dizer o que falta com plano vale mais que
> prometer tudo. Cada item tem a resposta sugerida.

**9.1 Falta login real (SSO)?**
"Correto — hoje é modo demonstração. Primeiro item do roadmap: OIDC com o provedor
corporativo de vocês. A estrutura de perfis/permissoes já está pronta no servidor."

**9.2 Falta testes de carga?**
"Sim — dimensionamos com a base de ~10 mil registros. Para o volume real de vocês,
fazemos um teste de carga antes do piloto. Precisamos dos números de transação diária."

**9.3 Falta monitoramento/alertas (observabilidade)?**
"Hoje temos health checks e logs correlacionados. Para produção: métricas/alertas
(Prometheus/Grafana ou a stack de vocês) — integração padrão de mercado."

**9.4 Falta CI/CD?**
"O projeto tem testes automatizados prontos para pipeline; a montagem do pipeline
corporativo é rápida com o time de vocês."

**9.5 Falta backup testado?**
"O Atlas tem backup gerenciado. Políticas de retenção e teste de restore serão alinhados
com a infra de vocês antes do piloto."

**9.6 Falta avaliação de segurança (pentest)?**
"Não fizemos — recomendamos antes de qualquer operação com dado real. Fazemos parte
com a equipe de segurança de vocês."

**9.7 Os dados são os do Time de IA, não o catálogo completo da Gerdau?**
"Exato — o desafio usou a base do Time de IA. Para produção, rodamos o mesmo processo
sobre a base real: já temos o caminho provado (matriz → governança)."

**9.8 Falta treinamento/usabilidade validada com usuários?**
"Fizemos a interface com linguagem simples e prevenção de erros, mas queremos um teste
com usuários reais de vocês no piloto — é o próximo passo natural."

---

# Parte 10 — Processo, time e decisões

**10.1 Quanto tempo levou?**
Ciclo átil de desenvolvimento do desafio — com o que mostra: sistema completo, testes,
documentação e dados oficiais integrados.

**10.2 Como se organizaram?**
Divisão por frentes (dados/motor, backend, frontend, documentação) + decisões registradas
em documentação técnica — tudo versionado.

**10.3 Qual a maior decisão técnica?**
Usar a decisão oficial do Time de IA como fonte da verdade (em vez de reinventar o
motor) e dar ao humano a última palavra na governança.

**10.4 E a maior dificuldade?**
Integrar análise (planilhas) com operação (cadastro) sem perder fidelidade dos dados —
resolvido com merge documentado campo a campo e carga idempotente.

**10.5 Como garantiram qualidade?**
Testes automatizados das regras críticas, validação ponta a ponta do fluxo e documentação
de cada decisão (o "porquê" está tudo escrito).

**10.6 Como será a transição para o time de vocês?**
Documentação completa já existe (arquitetura, dados, API, fluxo, permissões, material de
estudo) — propomos workshops de handover + período de sombra no piloto.

**10.7 O que esperam de nós na reunião?**
Prioridades reais: quais integrações doem mais, volume real de cadastro, quem são os
usuários, e o que o processo de compra precisa hoje que não tem.

**10.8 E se pedirem mudanças grandes?**
"Ótimo — o desenho é modular (regras isoladas em serviços, configuração separada de
código). Vamos priorizar juntos: o que entra no piloto e o que vai para a próxima fase."

---

# 🧭 Guia rápido para a reunião

## A abertura (30 segundos)
"Esse é o protótipo de padronização de códigos de serviço — ele bloqueia a duplicidade
**na hora do cadastro**, usa a calibração do Time de IA e deixa com vocês a decisão do
que for ambíguo. Hoje vamos mostrar funcionando e ouvir onde precisamos ajustar."

## Os 3 números para decorar
1. **25,4%** de repetição encontrada na base (análise oficial)
2. **0,88 / 0,70** — limiares calibrados pelo Time de IA
3. **4.384** itens tratados pela governança (4.244 em massa + 140 decisão humana)

## Se travar, diga
- "O banco nunca apaga — consolida; toda decisão tem volta e auditoria."
- "Automação propõe, humano decide."
- "Isso está documentado — posso mostrar no projeto."

## Nunca diga
- "Acho que..." para dados (mostramos ou confirmamos)
- "Está tudo pronto" para produção (está pronto para **piloto**)
- Nada negativo sobre o Time de IA — os dados e a calibração são deles

---

*Suporte: cada resposta detalhada está nos documentos em `docs/` — sobretudo
`ESTUDO_JAVA_SPRING.md` (estudo completo), `ALINHAMENTO_COMITE.md` (Q1/Q2/Q3) e
`FLUXO_DE_DADOS.md` (dados linha a linha).*
