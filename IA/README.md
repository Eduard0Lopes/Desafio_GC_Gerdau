# IA & Dados — Saneamento e catálogo de serviços

Pipeline Python que saneia a base SAP anonimizada e gera os artefatos consumidos pela API Java.

```bash
pip install -r requirements.txt pytest
python src/pipeline.py          # roda todas as etapas (a partir de IA/)
python -m pytest tests
python src/motor_busca.py "SV EQUIPE DE TELHADO" --top 5
```

| Etapa | Script | Saída |
|---|---|---|
| 1. Saneamento (só se `data/raw/base_original.xlsx` existir) | `sanitizacao.py` | `data/processed/base_saneada.xlsx` |
| 2. Coleções NoSQL | `migration.py` | `data/processed/nosql/*.json` (importadas pelo `DadosIaSeeder`) |
| 3. Matriz De-Para com score real | `gerar_matriz_depara.py` | `matriz_de_para_gerdau.{xlsx,csv}` |
| 4. Catálogo padronizado | `gerar_catalogo.py` | `catalogo_padronizado_servicos.xlsx` |
| 5. Banco simulado offline | `testar_banco.py` | `db_simulado.json` |
| 6. Auditoria | `validar_prova.py` | falha (exit 1) se alguma verificação não passar |

Módulos compartilhados: `config.py` (caminhos), `normalizacao.py`, `fuzzy_engine.py`, `ranking_policy.py`,
`dados.py` (IDs canônicos). A base original não é versionada (LGPD); sem ela, as unidades de medida vêm de
`data/reference/unidades_medida.csv`.

**IDs canônicos**: o código de serviço (`000001`) é o mesmo na matriz, no catálogo e no `_id` do MongoDB
(ordem da primeira ocorrência da descrição normalizada). A matriz traz, por linha legada: `newId`,
`decision` (`ORIGINAL`/`CONSOLIDADO`), o serviço canônico mais parecido (`similarId`, `score`) e o status da
política de ranking (≥ 0,88 duplicidade, ≥ 0,70 alerta). O motor e a normalização são idênticos aos da API
(ver [docs/ARQUITETURA.md](../docs/ARQUITETURA.md)).
