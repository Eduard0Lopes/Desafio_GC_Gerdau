# Modelo de Dados

O banco é construído pelo **merge dos dados oficiais do Time de IA**:

```
IA/data/processed/base_saneada.xlsx         (dados tratados)
IA/data/processed/matriz_de_para_gerdau.xlsx (IDs canônicos + score/decisão)
                        │
                        ▼  DataSeeder (backend, leitura apenas)
        ┌───────────────┴────────────────────────┐
        │  matriz_depara  (1 doc por legado)     │  ← documento MESCLADO + governança
        │  + 5 coleções de catálogo (seção 7)    │
        └────────────────────────────────────────┘
```

## `matriz_depara` — documento mesclado (1 por registro legado)

```json
{
  "_id": "ObjectId(...)",
  "newId": "CS-130835",
  "descricaoCanonica": "SV-EQUIPE TELHADO",
  "grupoServico": "Civil",
  "unidadeMedida": "SV",
  "decision": "DUPLICIDADE",
  "score": 0.95,
  "reason": "Provável duplicidade - Bloquear ou exigir justificativa",
  "legacyData": {
    "legacyId": "4033776",
    "legacyDescription": "SV-EQUIPE TELHADO",
    "legacySource": "ARQUIVO_SAP_GERDAU",
    "codigoFornecedor": 200982674,
    "alertaSap": false,
    "quantidadeCaracteres": 17
  },
  "statusRevisao": "PENDENTE",
  "reviewedBy": null,
  "reviewedAt": null,
  "observacaoRevisao": null
}
```

- `statusRevisao`: `PENDENTE` (decisão DUPLICIDADE/ALERTA — fila de governança pré-carregada)
  | `REVISADO` (com `reviewedBy`/`reviewedAt` — seção 9.6) | `SEM_PENDENCIA` (ORIGINAL)

## Coleções de catálogo (seção 7) — derivadas do merge

| Coleção | _id (identidade oficial) | Origem | Volume |
|---|---|---|---|
| `grupos_servico` | slug do nome (ex.: `CIVIL`) | MIGRACAO | 3 |
| `codigos_fornecedor` | código SAP (ex.: `200982674`) | MIGRACAO | 111 |
| `codigos_servico` | **newId oficial** (ex.: `CS-130835`) | MIGRACAO | 7.308 |
| `unidades_medida` | sigla (ex.: `SV`) | MIGRACAO | 17 |
| `codigos_completos` | composição `GS_CF_CS_UM` (única) | MIGRACAO/APLICACAO | 7.554 |

**codigos_servico** (seção 7.3):
```json
{ "_id": "CS-130835", "descricao": "SV-EQUIPE TELHADO",
  "descricaoNormalizada": "sv equipe telhado", "quantidadeCaracteres": 17,
  "origem": "MIGRACAO", "legacyIds": ["4033776"] }
```

**codigos_completos** (seção 7.5):
```json
{ "_id": "CIVIL_200982674_CS-130835_SV", "idCompleto": "…",
  "gsId": "CIVIL", "cfId": "200982674", "csId": "CS-130835", "umId": "SV",
  "descricaoSnapshot": "Civil | Fornecedor 200982674 | SV-EQUIPE TELHADO | SV",
  "status": "ATIVO", "decisaoSimilaridade": "DUPLICIDADE", "melhorScore": 0.95,
  "usuarioCriacaoId": null, "createdAt": null, "origem": "MIGRACAO", "legacyIds": ["4033776"] }
```

> **IDs (seção 8.3):** os quatro componentes usam as **identidades oficiais** do Time de IA
> (newId `CS-130835`, código SAP do fornecedor, sigla da unidade, slug do grupo),
> compostas em `idCompleto` na ordem G.S._C.F._C.S._U.M.. Os códigos legados são
> preservados em `legacyIds`/`legacyData` — rastreabilidade completa.

## Índices (seção 14.3)

| Coleção | Índices |
|---|---|
| `codigos_completos` | **único** `idCompleto` · `gsId` · `cfId` · `csId` · `umId` · `origem` · `status` |
| `codigos_servico` | `descricaoNormalizada` · `origem` · **text** (`descricao`, `descricaoNormalizada` — português) |
| `matriz_depara` | `newId` · `statusRevisao+score` · `legacyData.legacyId` · `decision` |

## Coleções de governança

- `excecoes` — auditoria de justificativas (seção 12.3): solicitante, data/hora, payload,
  similares encontrados, justificativa, decisão tomada.
- `codigos_completos.status` — `ATIVO` | `PENDENTE_VALIDACAO` | `REJEITADO`.
