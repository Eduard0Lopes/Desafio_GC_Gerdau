# Arquitetura

Solução de **Saneamento, Padronização e Filtro de Códigos de Serviço** (Desafio GC — Gerdau),
conforme a Documentação Técnica do projeto.

```
┌──────────────────────────────────────┐
│ Frontend (Vue 3 + Vite)              │  seletores · cadastro · similaridade
│                                      │  governança · de-para
└────────────────┬─────────────────────┘
                 │ HTTPS / JSON (/api/v1)
┌────────────────▼─────────────────────┐
│ Backend (Spring Boot)                │
│  Controllers → Services → Repos      │
│  Preview · Governança · Auditoria    │
│  Idempotência · Validações           │
│           │                          │
│   SearchClient (seção 4.2)           │
│    └─ local (motor Java = port fiel  │
│       do fuzzy engine do Time de IA) │
└───────┬──────────────────────────────┘
        │
┌───────▼──────────────────────────────┐
│ MongoDB                              │
│ matriz_depara (mesclada + governança)│
│ + 5 coleções de catálogo (seção 7)   │
│ + excecoes / contadores              │
└───────▲──────────────────────────────┘
        │  DataSeeder (leitura apenas)
┌───────┴──────────────────────────────┐
│ Time de IA (IA/data/processed)       │
│ base_saneada.xlsx × matriz_de_para…  │
└──────────────────────────────────────┘
```

## Decisões principais

| Tema | Decisão | Fundamento |
|---|---|---|
| Banco | MongoDB (Atlas ou local) | Seção 5 — catálogos documentais + busca textual |
| Dados | merge `base_saneada.xlsx` × `matriz_de_para_gerdau.xlsx` | dados oficiais do Time de IA, sem cópia manual |
| Identidades | newId oficial (`CS-130835`), código SAP, sigla, slug | seção 8 — IDs estáveis + legados preservados |
| Similaridade | 40% Jaccard tokens + 60% Ratcliff/Obershelp | port fiel de `IA/src/fuzzy_engine.py` (valores de referência testados) |
| Política | 0,88 duplicidade / 0,70 alerta | calibragem da trilha IA (seção 6.7) |
| Anti-duplicidade | **todos os componentes** (G.S., C.F., C.S., U.M.) | filtro de entrada do TAP |
| Governança | fila pré-carregada (4.384 do histórico) + novos cadastros | seções 9.6 e 12 — reviewedBy/reviewedAt |
| Busca | `SearchClient` abstraído | seção 4.2 — trocar o motor não muda contratos |
| Índices | seção 14.3 (único em idCompleto, text em descrição) | performance e unicidade |

## Fluxo de governança (filtro anti-duplicidade)

1. **Prévia** (`POST /codigos-completos/preview`): composição, duplicidade, análise de
   similaridade em todos os componentes e decisão **PERMITIR | ALERTAR | REVISAR**.
2. **Decisão**:
   - **Reutilizar** o item sugerido (recomendado);
   - **Criar com justificativa** → gravado em `excecoes` (auditável);
   - **Enviar para validação** → fila humana (`PENDENTE_VALIDACAO`).
3. **Modo** (`SIMILARITY_ENFORCEMENT`): `enforce` bloqueia sem justificativa (HTTP 409);
   `observe` apenas registra (implantação gradual, seção 12.4).
4. **Idempotência**: header `Idempotency-Key` evita duplicação por retentativa (seção 10.3).

## Revisão histórica (seção 9.6)

Os 4.384 registros com decisão DUPLICIDADE/ALERTA entram **pré-carregados** na fila de
governança com os scores reais da matriz como evidência. A decisão registra
`reviewedBy`/`reviewedAt`/`observacaoRevisao` — rastreabilidade completa.
