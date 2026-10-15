# Alinhamento com as respostas do comitê (Q&A)

> Comparação entre as respostas da área (comitê avaliador) e o que o sistema entrega:
> **dentro do esperado · acima · ou oportunidade de melhoria** (e o que foi feito).

---

## Pergunta 1 — Quem solicita, cadastra e aprova novos códigos? Mais de um perfil?

**Resposta da área:** hoje o **próprio comprador** cadastra após fechar a negociação
(~20 compradores), **sem consultar** se o código já existe — daí as duplicidades.

| Aspecto | O que a área descreveu | O que o sistema faz | Status |
|---|---|---|---|
| Quem cadastra | Comprador | Perfil **Usuário** (comprador) cadastra itens e vincula serviços | ✅ dentro |
| Consulta de existentes | **Não existe** hoje | **Automática**: a cada digitação o sistema busca similares e mostra % Match + motivo | ⬆ acima |
| Mais de um perfil | Não definido | **Usuário** (comprador) × **Admin** (governança), com bloqueio real no servidor | ⬆ acima |

**Síntese para a apresentação:** "O comprador continua no comando do cadastro — a
diferença é que agora o sistema consulta a base **por ele**, antes de criar duplicidade."

---

## Pergunta 2 — Tipos/naturezas de serviços: o que deve entrar no saneamento?

**Resposta da área:** maior ganho em **mão de obra** (funções: mecânico, supervisor,
soldador…); mas materiais, equipamentos e locações também devem ser contemplados,
ainda que de forma menos detalhada — desde que fiquem **"identificáveis"** (ex.: somar
o total de **"mobilização"** entre contratos, hoje impossível por falta de padrão).

| Necessidade expressada | O que o sistema faz | Status |
|---|---|---|
| Similaridade forte em mão de obra | Motor fuzzy (40% tokens + 60% caracteres) com tolerância a typos/acentos/ordem | ✅ |
| Materiais, equipamentos, locações "identificáveis" | Classificação automática por **natureza do serviço** (Mão de obra · Material · Equipamento · Locação · Outros) em cada código | ✅ **novo** |
| Somar um conceito entre contratos (ex.: "mobilização") | Busca por conceito retorna **todos** os registros ligados ao código canônico (`legacyIds`) + contagens agregadas por natureza/grupo | ✅ |
| Padronização textual/de código | `descricaoNormalizada` + `newId` canônico + matriz de-para | ✅ |

**Melhorias implementadas nesta rodada:**
- campo `natureza` em `codigos_servico` e `matriz_depara` (classificador por vocabulário
  da categoria — heurística configurável, sinal e não equivalência garantida);
- gráfico **"Natureza dos serviços"** no Painel Admin → Detalhes;
- campo **"Natureza do serviço"** visível em cada card da fila de governança.

---

## Pergunta 3 — Quem decide manter/substituir/criar quando há duplicidade?

**Resposta da área:** o **próprio comprador** analisa a sugestão e decide (reutilizar ou
criar novo se julgar diferente apesar da similaridade); sugere-se uma **segunda pessoa**
para aprovar o novo cadastro com possível duplicidade, **sem engessar** o processo.
"Aceitamos sugestões de governança" — não há resposta definitiva.

| Necessidade expressada | O que o sistema faz | Status |
|---|---|---|
| Comprador decide | Três saídas claras: **Reutilizar** (com escolha do item) · **Criar com justificativa** · **Enviar para governança** | ✅ |
| Segunda pessoa para o caso de duplicidade | Fila de **Governança** (Painel Admin) com evidências: similares, % Match, justificativa — aprovar/rejeitar com registro | ✅ **novo** |
| "Sem engessar o processo" | **Criar com justificativa** não espera aprovação: o comprador segue o trabalho e a decisão fica auditada | ✅ **novo** |
| Sugestão de governança (eles pediram ideias) | Modo gradual: `observe` (só registra) → `enforce` (bloqueia sem justificativa) — implantável por ambiente | ✅ **novo** |

**Síntese para a apresentação:** "O comitê disse que não tinha resposta definitiva para
governança — o sistema oferece uma: o comprador decide, e quando insiste em uma
duplicidade, uma segunda pessoa revisa **sem travar** o dia a dia."

---

## Extra — correção de usabilidade apontada

> *"Quando tento vincular um serviço, ele bate que já existe e dá o alerta de
> duplicidade — isso deveria ser só no cadastro."*

**Corrigido:** em **Vincular**, composição repetida agora mostra um aviso simples
**"Este vínculo já existe"** (com o código existente e ação de voltar/limpar). O
**Bloqueio de Duplicidade** (com seleção de item) aparece apenas na **hora de cadastrar**
um item novo — onde a regra faz sentido.

---

## Nota de transparência (para a apresentação)

- A classificação de **natureza** é por vocabulário (heurística) — recomenda-se validação
  com a área para refinar os termos de cada categoria;
- Os limiares de similaridade (0,88/0,70) são provisórios e configuráveis;
- O modo `enforce` (bloqueio) pode começar em `observe` para adoção gradual.
