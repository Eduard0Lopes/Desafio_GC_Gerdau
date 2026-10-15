# Diagnóstico de Qualidade — Base de Códigos de Serviço

_Origem: `IA/data/processed/base_saneada.xlsx` (dados tratados pelo Time de IA) ×
`matriz_de_para_gerdau.xlsx` (IDs canônicos e similaridade)._

## Visão geral

| Métrica | Valor |
|---|---|
| Registros legados (base histórica) | **9.795** |
| Códigos canônicos únicos (newId) | **7.308** |
| Colunas analisadas | Grupo de Serviço · Código do Fornecedor · Código do Serviço · Descrição do Item · Unidade medida · DescricaoOriginal · DescricaoNormalizada · QuantidadeCaracteres · AlertaSAP · DuplicadaExata (+ score/decision/reason na matriz) |

## Qualidade por dimensão

| Verificação | Resultado |
|---|---|
| Integridade do join base × matriz (por `legacyId`) | **100%** — 9.795 = 9.795, nenhuma linha órfã |
| Divergência de descrição entre base e matriz | **0** (amostra de 500 conferida) |
| Duplicidades exatas (mesma descrição normalizada) | **3.967** registros |
| Limite SAP (>= 40 caracteres) | **1.519** itens |
| Campos nulos críticos (legado, descrição, grupo, unidade) | **0** |
| Cobertura de fornecedores distintos | 111 |
| Unidades de medida distintas | 17 (SV, KG, H, M3, M2, CM, M, ST, MON, L, TO, TAG, MM, ZCJ, ML, CDM, KM) |

## Problemas identificados (causa-raiz do desafio)

1. **Cadastro descentralizado no SAP** — cada comprador cria descrições próprias, sem
   validação de similaridade nem aprovação prévia.
2. **Duplicidades exatas e aproximadas** — 3.967 registros com descrição normalizada
   repetida; 4.244 classificados como DUPLICIDADE pela matriz.
3. **Limite SAP mal aproveitado** — 1.519 itens na faixa de risco (>= 40 caracteres),
   com abreviações e códigos internos de fornecedor embutidos na descrição.
4. **Vocabulário irregular** — prefixos (`SV`, `DIARIA`), medidas na descrição e
   pontuação inconsistente.

## Decisões de tratamento aplicadas

1. Normalização textual única (minúsculas, sem acentos, sem pontuação, espaços
   simples) — implementada no backend (`TextoUtil`) e na trilha IA (`sanitizacao.py`).
2. Chave canônica = descrição normalizada → consolidação em `newId`.
3. Duplicidade aproximada tratada pelo motor de similaridade (0,88 / 0,70).
4. Rastreabilidade preservada: nenhum registro é apagado (`legacyData`/`legacyIds`).

## Limitações

- Base anonimizada de uma única extração SAP (sem histórico temporal).
- Limiares provisórios — recalibrar com amostra aprovada pela área.
- Sinonímia de categoria (vocabulário de compras) ainda não tratada.
