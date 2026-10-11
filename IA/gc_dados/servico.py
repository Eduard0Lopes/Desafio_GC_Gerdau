"""Avaliacao de um item NOVO contra o catalogo (contrato para o endpoint /api/v1/itens/similaridade).

Entrada : descricao (+ unidade de medida opcional)
Saida   : normalizacao, validacao dos 40 caracteres, especificidade, decisao
          (PERMITIR | ALERTAR | REVISAR) e similares com score e motivo.
Em producao os candidatos virao do Atlas Search; aqui a busca e em memoria (catalogo em JSONL)
e serve para testar o contrato e calibrar limiares com a mesma funcao de pontuacao.
"""
import json
from pathlib import Path

from .normalizador import LIMITE_SAP, analisar, carregar_dicionario, descricao_sugerida
from .similaridade import decisao, pontuar


def carregar_catalogo(caminho):
    with open(caminho, encoding="utf-8") as f:
        return [json.loads(l) for l in f if l.strip()]


def avaliar_item(descricao: str, catalogo: list, unidade: str = None, top: int = 5, dic=None) -> dict:
    dic = dic or carregar_dicionario()
    a = analisar(descricao, dic)
    flags, mods = a["flags"], a["modificadores"]
    similares = []
    for c in catalogo:
        if c["flags"] != flags or c["modificadores"] != mods:
            continue
        funcao_c = c["funcao"].split()
        # token truncado: so o ultimo termo da descricao candidata, se ela tem 40 caracteres
        trunc_c = funcao_c[-1] if c["quantidadeCaracteres"] >= LIMITE_SAP and funcao_c else None
        trunc_n = a["funcao"][-1] if a["possivelTruncamento"] and a["funcao"] else None
        p = pontuar(a["funcao"], funcao_c, a["numeros"], c["numeros"], None, None, trunc_n, trunc_c)
        if p["score"] >= dic["limiares"]["alerta"] - 0.15:
            similares.append({"csId": c["_id"], "descricao": c["descricao"], "score": p["score"], "motivo": p["motivo"]})
    similares.sort(key=lambda x: -x["score"])
    melhor = similares[0]["score"] if similares else 0.0
    return {
        "descricao": descricao,
        "descricaoNormalizada": a["descricaoNormalizada"],
        "quantidadeCaracteres": a["quantidadeCaracteres"],
        "limiteSapOk": a["quantidadeCaracteres"] <= LIMITE_SAP,
        "especificidade": a["especificidade"], "confiancaEspecificidade": a["confiancaEspecificidade"],
        "descricaoSugerida": descricao_sugerida(a, dic),
        "decisao": decisao(melhor, dic["limiares"]),
        "melhorScore": melhor, "similares": similares[:top],
    }


if __name__ == "__main__":
    import sys
    cat = carregar_catalogo(Path(__file__).resolve().parent.parent / "saida" / "codigos_servico.jsonl")
    print(json.dumps(avaliar_item(" ".join(sys.argv[1:]), cat), ensure_ascii=False, indent=2))
