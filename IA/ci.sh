#!/bin/sh
# Pipeline de entrega local (secao 17): compilacao, testes, execucao e validacao pos-processamento.
set -e
cd "$(dirname "$0")"
BASE="${1:-base_legada.xlsx}"
echo "1/4 compilacao";           python3 -m compileall -q gc_dados carga_mongo.py
echo "2/4 testes";               python3 tests/rodar_testes.py | tail -1
echo "3/4 saneamento da base";   python3 -m gc_dados.pipeline "$BASE" --saida saida --relatorio saida/relatorio_saneamento.xlsx
echo "4/4 validacao pos-processamento"
python3 - <<'PY'
import json, sys
l = json.load(open("saida/lote.json"))
print("lote", l["_id"], l["status"], "| verificacoes:", l["resultadoValidacao"]["verificacoes"], "| falhas:", l["resultadoValidacao"]["falhas"])
sys.exit(0 if l["status"] == "LIBERADO" else 1)
PY
