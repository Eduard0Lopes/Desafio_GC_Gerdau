"""Busca por similaridade no catálogo gerado (mesmo resultado de GET /api/codigos-servico/similares).

Uso: python src/motor_busca.py "SV MANUTENCAO MECANICA" --top 5
"""
import argparse
import json

import config
from fuzzy_engine import similaridade
from normalizacao import normalizar_texto
from ranking_policy import classificar


def carregar_servicos() -> list[dict]:
    with open(config.NOSQL / "codigos_servico.json", encoding="utf-8") as f:
        return json.load(f)


def buscar(descricao: str, top: int = 5, servicos: list[dict] | None = None) -> list[dict]:
    normalizada = normalizar_texto(descricao)
    if not normalizada:
        return []
    servicos = servicos if servicos is not None else carregar_servicos()
    resultados = []
    for cs in servicos:
        score = round(similaridade(normalizada, cs["descricaoNormalizada"]), 4)
        status, acao = classificar(score)
        resultados.append({"id": cs["_id"], "descricao": cs["descricao"], "score": score,
                           "status": status, "acaoRecomendada": acao})
    resultados.sort(key=lambda r: (-r["score"], r["id"]))
    return resultados[:top]


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("descricao")
    parser.add_argument("--top", type=int, default=5)
    args = parser.parse_args()
    for r in buscar(args.descricao, args.top):
        print(f"{r['id']}  {r['score']:.4f}  {r['status']:<11}  {r['descricao']}")


if __name__ == "__main__":
    main()
