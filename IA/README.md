# Desafio GC - IA e Dados: padronizacao dos codigos de servico

Escopo: tudo que o documento atribui a **IA e Dados** (bloco verde da secao 4.1). API Spring Boot, telas Vue/JavaFX,
autenticacao, perfis, auditoria de usuario e integracao SAP sao da equipe de Java; aqui ficam o modelo de dados, a
migracao, a busca, a similaridade e os contratos que eles consomem. Materiais textuais/visuais (relatorio, mapa mental,
diagramas, video) nao fazem parte desta entrega.

## Rodar
```bash
pip install -r requirements.txt
./ci.sh BASE.xlsx                     # compila, 58 testes, saneia a base, valida o lote (~30 s)
python -m gc_dados.pipeline BASE.xlsx --saida saida --relatorio saida/relatorio_saneamento.xlsx
python -m gc_dados.busca mec          # busca (autocomplete/fuzzy/stemming/ID) sobre saida/
python -m gc_dados.servico "Eletricista HE 50%"     # avalia item novo (contrato para o Java)
python -m gc_dados.avaliacao relatorio_saneamento.xlsx   # precisao/revocacao apos rotular a amostra
MONGODB_URI=... python carga_mongo.py --db gerdau_servicos_hml
```

## Matriz documento -> implementacao
| Secao | Exigencia | Onde / como |
|---|---|---|
| 1.3, 10.4 | limite de 40 caracteres, validado | `entrada.py` (rejeita >40), `quantidadeCaracteres` calculado, validador Mongo `maxLength: 40` |
| 2.2 | busca por ID, nome ou descricao; pesquisa aproximada | `busca.py` (ID, ID legado, texto, fuzzy) |
| 2.2, 9 | duplicidades historicas, de-para, relacionamento antigo-novo | `pipeline.py`, colecao `de_para`, `legacyIds` |
| 3 | legado importado sem usuario/timestamp | `usuarioCriacaoId`/`createdAt` nulos, `origem: MIGRACAO` (verificado em `validar_migracao`) |
| 5 | MongoDB + Atlas Search; busca atras de uma abstracao | `mongo/`, `carga_mongo.py`, `BuscaMemoria`/`BuscaAtlas` (mesmo contrato) |
| 6.1 | busca exata (IDs, codigos completos, HR/UN/M2) | tiers `ID_EXATO`, `ID_LEGADO_EXATO`, `VALOR_EXATO` |
| 6.2 | prefix search e autocomplete | `PREFIXO_ID`, `TOKENS_PREFIXO`, `TOKENS_SUBSTRING`; Atlas `autocomplete` (nGram) |
| 6.3 | fuzzy, ordem invertida, `unid medida` -> `unidade de medida` | `FUZZY`; tokens sem ordem; abreviacoes no dicionario |
| 6.4 | stemming | `stemmer.py` (leve, sem dependencias); campo `descricaoStem`; analisador `lucene.portuguese` |
| 6.5 | normalizacao + `descricaoOriginal`/`descricaoNormalizada` | `normalizador.py`; `descricao`, `descricaoNormalizada`, `descricaoExpandida` |
| 6.6 | indices por ID e por texto, pesos | `mongo/indices_atlas.json` (idBusca / texto separados); boosts em `pipeline_atlas` |
| 6.7 | ranking: ID exato, prefixo, texto exato, tokens, fuzzy, uso/ativo | `busca.py` (pesos em `dicionario.json > ranking`) |
| 6.7 | limiares 0,90 / 0,75, calibrar com amostra rotulada | `dicionario.json > limiares` (ou `GC_LIMIAR_*`), `avaliacao.py`, aba "Amostra p rotular" |
| 7 | cinco colecoes + `descricaoSnapshot`, `legacyIds` | `saida/*.jsonl`, `mongo/validadores.json` |
| 8 | IDs estaveis, 3+3+6+2, unicos, nunca reutilizados | `ids.py`, colecao `contadores`, indice unico composto |
| 9.1 | 14 etapas do saneamento | `pipeline.py` (docstring numera as etapas) |
| 9.2 | copia de origem, data/lote, hash, versao do algoritmo, responsavel, resultado | `lote.py`, `saida/origem/` (somente leitura), `lote.json`, colecao `lotes_importacao` |
| 9.3 | duplicata exata / provavel / pendente (nada e apagado) | `decision` no de-para; colecao `controle_excecoes` |
| 9.4 | descricao canonica (<=40, legivel, sem prefixo sem valor); humano nos casos criticos | `escolher_canonico`; `CANONICO_INCERTO` |
| 9.5-9.6 | dados ausentes e matriz de-para | idem; campos de revisao (`reviewedBy/At`) |
| 10.2 | previa/similaridade (parte de dados) | `servico.avaliar_item` -> `PERMITIR/ALERTAR/REVISAR`, score, motivo |
| 12.3 | registro de excecoes | colecao `excecoes` (validador); a escrita e da API |
| 14.1-14.3 | resposta enxuta, paginacao, indices | `busca.py`, `mongo/indices_atlas.json` (sem indice em `createdAt`) |
| 15.2 | metricas de sucesso (parte de dados) | `lote.json > metricas` (cobertura do de-para, duplicidades consolidadas, volume) |
| 16.1 | unitarios: normalizacao, caracteres, ID, score, nulos, legado | `tests/` (58 testes) |
| 16.3 | precisao, revocacao, falsos bloqueios | `avaliacao.py` (precisa de rotulos humanos) |
| 16.5 | testes de migracao | `validar_migracao.py` (19 verificacoes; lote so e `LIBERADO` se passam) + aba "Amostra de-para" |
| 17 | ambientes, config fora do codigo, pipeline | `GC_*` env, `--db`, `ci.sh` |
| 20 | mitigacoes (calibrar, faixas, sinonimos, reindexacao, de-para) | idem; `dicionario.json > sinonimos` (vazio: definir com especialistas) |

## Decisoes de leitura do documento
- **Prefixo no ranking (6.7)**: lido como prefixo de **ID** (6.6). Prefixo de texto fica logo abaixo do texto exato.
- **CS global**: o catalogo de servicos nao e separado por GS; o GS so entra no codigo completo.
- **IDs tecnicos**: gs, cf, cs, um sequenciais (3+3+6+2); codigos legados em `legacyIds` / `codigoLegado`.
- **Stemming so na busca**: nao funde itens (mecanica e mecanico sao itens diferentes).

## Contrato para a equipe de Java
`servico.avaliar_item` -> `{descricaoNormalizada, quantidadeCaracteres, limiteSapOk, especificidade, confiancaEspecificidade,
descricaoSugerida, decisao, melhorScore, similares[{csId, descricao, score, motivo}]}`. Busca: `{total, resultados[{id, nome,
descricao, score, tipoMatch}]}`. Colecoes e indices em `mongo/`. Candidatos reais virao do Atlas Search (`pipeline_atlas`) e
devem ser reavaliados por `similaridade.pontuar`.

## Limites conhecidos
- **Nao testado em Atlas/MongoDB reais** (sem acesso aqui): `carga_mongo.py`, validadores, indices e `pipeline_atlas`. A
  carga confere contagens contra o lote ao final; rode em homologacao antes de producao.
- Limiares 0,90/0,75 **nao calibrados**: dependem da rotulagem humana. Sinonimos de negocio: vazio.
- `dicionario.json > a_confirmar`: abreviacoes deduzidas (SS, DF, PER, EV, H.Oc...).
- Stemmer proprio e leve (RSLP/NLTK indisponiveis offline); o Atlas usa o seu analisador portugues.
- Itens fora da secao de dados (perfis, OAuth, endpoints, observabilidade de runtime, integracao SAP) sao da equipe de Java.
