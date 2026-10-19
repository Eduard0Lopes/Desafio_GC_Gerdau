# Casos Especiais — badge `(!)` e unicidade de Códigos de Serviço

Resumo executivo da regra de negócio implementada — material de apoio para apresentação.

## 1. O problema

A base migrada tinha **códigos de serviço repetidos**: o mesmo serviço registrado com
nomes diferentes (`SV-ALVENARIA DE BLOCOS...` × `SV-ALVENARIA BLOCOS...`), cortes de
40 caracteres do SAP (`…COROA DIAMA` × `…COROA DIAMANT`), abreviações
(`TÉC. SEGURANÇA` × `TECNICO(A) DE SEG`) e até o mesmo nome duas vezes
(`TELHADO` × `telhado`). Isso fere a regra de ouro: **códigos de serviço únicos**.

## 2. A regra: código de serviço único

- **Cadastro bloqueia** duplicatas (409 com mensagem clara) — vale para criação avulsa,
  fluxo de código completo e importação de CSV;
- **Edição de pedido** também valida nome único + limite SAP de 40 caracteres;
- **Preview reutiliza** o serviço existente com o mesmo nome (como já fazia com
  Grupo/Fornecedor) em vez de criar um paralelo.

<!-- ============================================================
     SEÇÃO OCULTA PARA A APRESENTAÇÃO — descomentar se precisar.
     ============================================================
## 3. O badge `(!)` — caso especial

O `(!)` marca o item que merece atenção humana e **persegue o item em toda a tela**
(catálogo, seletores, cards de % match, governança), com o **motivo no hover**.

| Situação | O que acontece |
|---|---|
| **Exceção aprovada** — a similaridade sinalizou e a governança aprovou com justificativa | `(!)` + motivo obrigatório registrado |
| **Aceite de repetição** — "Aceitar" item do histórico com DUPLICIDADE/ALERTA | `(!)` + motivo |
| **Duplicidade tratada** na varredura | `(!)` no item mantido (e nos envolvidos) explicando a unificação |
| **Marca manual** — o "!" tracejado ao lado do status | o usuário registra o motivo |

**Ciclo de vida:** `marcar → revisar (filtro "Casos especiais" + hover) →
resolver (limpa o `(!)`) ou excluir (definitivo, com transferência de vínculos)`.
============================================================ -->

## 3. Exclusão com transferência de vínculos

Item com vínculos não pode ser apagado direto — o sistema abre o modal
**"Transferir vínculos para excluir"**: escolhe-se o item que recebe os vínculos e o de
origem é excluído. Composições que já existiam no destino são **mescladas** (redundantes),
nunca sobrescritas.

## 4. Varredura de duplicidades — critérios

Um par é considerado o **mesmo serviço** quando:
1. nomes **exatamente** iguais (ignorando caixa/acentos/pontuação);
2. iguais tirando **palavras de ligação** (de/em/e/…); `com`, `sem`, `para` e os
   **sinais `>`/`<=`** contam — mudam o produto;
3. **truncagem/abreviação** (corte dos 40 chars do SAP, `TEC SEG` × `TÉCNICO DE SEGURANÇA`);
4. marcador **`SV`** ignorado (`SV RETROESCAVADEIRA` × `Retroescavadeira`);
5. **`H` = `HORA`** e espaçamento de compostos (`RETROESCAVADEIRA` × `RETRO ESCAVADEIRA`).

E **não** é duplicidade (só recebe `(!)` para revisão): variações de negócio —
`SUPERVISOR TST` × `SUPERVISOR ADM`, `H.E.SS` × `H.P.E.SS`, `CONDULETE COM/SEM ROSCA`,
turnos (`08:00-20:00` × `20:00-08:00`), regimes (`SENIOR` × `SENIOR HE`).

**Resultado:** 195 duplicados eliminados (vínculos transferidos antes), 146 vínculos
transferidos, 43 composições mescladas, **0 repetições restantes** e **0 vínculos quebrados**.

## 5. Auditoria

Toda ação fica registrada e é consultável no app pelo botão **📋 Alterações** do catálogo:
itens **excluídos, transferidos, mesclados, desativados e reativados**, com motivo e data.

<!-- ============================================================
     SEÇÃO OCULTA PARA A APRESENTAÇÃO — descomentar se precisar.
     ============================================================
## 7. Mapa de código (onde vive cada peça)

| Peça | Onde |
|---|---|
| Campos `casoIsolado`/`motivoCasoIsolado` | `model/CodigoServico.java` |
| Marcar/limpar marca, transferir vínculos, contagem | `service/CatalogoService.java` |
| Regra de aprovação (motivo obrigatório + marca) | `service/GovernancaService.java` · `aprovarNovo` |
| Aceitar/Apagar do histórico | `service/GovernancaService.java` · `revisarHistorico` |
| Retroativos (exceções aprovadas, órfãos) | `seed/SchemaBackfill.java` |
| Nome único + limite SAP | `CatalogoService.exigirServicoLivre` · `PreviewService.buscarServicoExistente` |
| Endpoint de marca/limpeza/vínculos | `controller/CatalogoController.java` (`/marcar-excecao`, `/vinculos`, `/transferir-vinculos`) |
| Badge, filtro, modais de resolver/excluir/marcar | `views/CatalogoView.vue` + `style.css` |
| Badge nos candidatos similares | `components/ModalBloqueio.vue`, `views/CadastrarItemView.vue`, `views/GovernancaView.vue`, `components/ComboboxBusca.vue` |
| Normalização/plural (busca e similaridade) | `util/TextoUtil.java` · `services/texto.ts` |
============================================================ -->
