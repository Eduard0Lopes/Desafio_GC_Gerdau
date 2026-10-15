# Hipótese de Similaridade — critérios e exemplos

## Pergunta

Quando dois códigos de serviço representam o **mesmo** item contratado?

## Critério (motor de similaridade)

Score = **40% Jaccard de tokens + 60% razão de caracteres (Ratcliff/Obershelp)**
— port fiel do motor do Time de IA (`IA/src/fuzzy_engine.py`), com os mesmos
valores de referência validados nos dois lados (testes Java × `difflib` Python).

Sinais combinados (nunca equivalência garantida — são indícios para revisão):

1. **Correspondência exata** de texto normalizado → duplicidade certa;
2. **Alta similaridade textual** entre descrições normalizadas;
3. **Regra numérica**: medidas/valores diferentes limitam o score a 0,85
   (evita `ELETRODUTO 1/2` = `ELETRODUTO 2 1/2`);
4. **ID/código legado idêntico** → score 1.0 (prioridade máxima, seção 6.6).

## Política de decisão (configurável)

| Score | Status | Ação |
|---|---|---|
| >= 0,88 | DUPLICIDADE | Bloquear / exigir justificativa (REVISAR) |
| 0,70 – 0,87 | ALERTA | Encaminhar para decisão humana (ALERTAR) |
| < 0,70 | ORIGINAL | Permitir continuidade (PERMITIR) |

> Os limiares são **provisórios** (calibragem da trilha IA sobre esta base) e
> configuráveis por ambiente (`SIMILARITY_LIMIAR_*`). Recomenda-se recalibração
> com amostra aprovada por especialistas da área.

## Exemplos reais verificados

| Caso | Exemplo | Comportamento |
|---|---|---|
| Texto idêntico normalizado | `SV-EQUIPE TELHADO` × `sv equipe telhado` | 100% — duplicidade |
| Variação de digitação/acento | `Eletromecanica` × `Eletromecânica` | 100% — duplicidade |
| Paráfrase próxima | `Diaria resgatista 8 horas` × `Diaria resgatista turno de ate 8 horas` | ~70% — ALERTA |
| Medidas diferentes | `SV ELETRODUTO PVC 1/2` × `SV ELETRODUTO PVC 2 1/2` | limitado a 0,85 — ALERTA, nunca duplicidade automática |
| Serviços distintos | `pintura predial` × `limpeza de vidros` | ~15% — ORIGINAL |
| Palavras invertidas | `mecanico manutencao` × `manutencao mecanico` | tokens 100%, score ~0,72 — ALERTA |

## Transparência do resultado

Cada candidato vem com **score e o motivo explicado** (ex.: "palavras em comum:
diaria, resgatista; semelhança de caracteres 79%; palavras 50%") — nunca apenas
um percentual (seção 11.8 do documento técnico).
