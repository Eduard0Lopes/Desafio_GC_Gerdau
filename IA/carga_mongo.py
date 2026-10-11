"""Carrega saida/*.jsonl no MongoDB com validadores, indices e conferencia contra o lote.
Uso: MONGODB_URI=... python carga_mongo.py [--db gerdau_servicos_hml] [--limpar]
Ambientes (secao 17): use bancos distintos (dev/hml/prod) via --db ou GC_DB. NAO testado contra um cluster real neste ambiente."""
import argparse, json, os
from pathlib import Path
from pymongo import MongoClient

RAIZ = Path(__file__).resolve().parent
COLECOES = ["grupos_servico", "codigos_fornecedor", "unidades_medida", "codigos_servico", "codigos_completos",
            "de_para", "controle_excecoes", "contadores", "lotes_importacao"]
VAZIAS = ["excecoes"]          # preenchida em operacao (12.3)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--limpar", action="store_true"); ap.add_argument("--db", default=os.environ.get("GC_DB", "gerdau_servicos"))
    a = ap.parse_args()
    db = MongoClient(os.environ["MONGODB_URI"])[a.db]
    val = json.loads((RAIZ / "mongo/validadores.json").read_text(encoding="utf-8"))
    cfg = json.loads((RAIZ / "mongo/indices_atlas.json").read_text(encoding="utf-8"))
    lote = json.loads((RAIZ / "saida/lote.json").read_text(encoding="utf-8"))
    if lote["status"] != "LIBERADO":
        raise SystemExit(f"Lote {lote['_id']} esta {lote['status']}: corrija as validacoes antes de carregar.")
    for nome in COLECOES + VAZIAS:
        if a.limpar and nome in db.list_collection_names(): db[nome].drop()
        if nome not in db.list_collection_names():
            db.create_collection(nome, validator=val[nome]) if nome in val else db.create_collection(nome)
        if nome in VAZIAS: continue
        docs = [json.loads(l) for l in (RAIZ / "saida" / f"{nome}.jsonl").read_text(encoding="utf-8").splitlines() if l.strip()]
        for i in range(0, len(docs), 2000): db[nome].insert_many(docs[i:i + 2000], ordered=False)
        print(nome, len(docs))
    for nome, lista in cfg["indices_mongodb"].items():
        for d in lista:
            keys = [(k, v) for k, v in d["keys"].items()]
            kw = {"unique": True} if d.get("unique") else {}
            if d.get("default_language"): kw["default_language"] = d["default_language"]
            db[nome].create_index(keys, **kw)
    try:                                           # indices Atlas Search (pymongo >= 4.5, cluster Atlas)
        from pymongo.operations import SearchIndexModel
        for nome, d in cfg["atlas_search"].items():
            db[nome].create_search_index(SearchIndexModel(definition=d["definition"], name=d["name"]))
    except Exception as e:                         # fora do Atlas isso falha: criar manualmente pela UI/CLI
        print("Atlas Search nao criado automaticamente:", e)
    esperado = lote["metricas"]["volumeImportado"]            # conferencia pos-carga (validacao pos-deploy)
    for nome, n in esperado.items():
        real = db[nome].count_documents({})
        print(f"conferencia {nome}: esperado={n} real={real} {'OK' if real == n else 'DIVERGENTE'}")


if __name__ == "__main__":
    main()
