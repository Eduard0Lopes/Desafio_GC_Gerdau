"""Consolida as coleções NoSQL em um único arquivo (data/processed/db_simulado.json) para inspeção offline."""
import json

import config


def main():
    banco = {}
    for colecao in config.COLECOES:
        caminho = config.NOSQL / f"{colecao}.json"
        with open(caminho, encoding="utf-8") as f:
            banco[colecao] = json.load(f)
        print(f"     {colecao}: {len(banco[colecao])} documentos")
    with open(config.DB_SIMULADO, "w", encoding="utf-8") as f:
        json.dump(banco, f, ensure_ascii=False, indent=2)
    print(f"[OK] Banco simulado salvo em {config.DB_SIMULADO}")


if __name__ == "__main__":
    main()
