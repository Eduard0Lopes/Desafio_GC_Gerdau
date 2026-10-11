"""Pipeline de saneamento e migracao (secao 9.1, etapas 1-14).

 1 identificar layout        2 copia imutavel + hash + lote   3 mapear colunas
 4 validar tipos/obrigatorios/tamanho                         5 normalizar textos
 6 duplicidades exatas       7 duplicidades aproximadas       8 agrupar candidatos
 9 definir registro canonico 10 gerar IDs                     11 gravar consolidados
12 gravar de-para            13 validar contagens e amostras  14 liberar a base (lote LIBERADO/BLOQUEADO)
"""
import argparse
import datetime as dt
import json
from collections import Counter, defaultdict
from pathlib import Path

import numpy as np
import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer

from . import entrada, validar_migracao
from .ids import contadores as gerar_contadores, montar_id_completo
from .lote import abrir_lote
from .normalizador import (VERSAO_ALGORITMO, analisar, carregar_dicionario, descricao_sugerida, limpar)
from .similaridade import pontuar
from .stemmer import stem_texto

GS_ORDEM = entrada.GS_VALIDOS            # Eletromecânica=001, Facilities=002, Civil=003 (exemplo do documento)
RETRIEVAL_MIN = 0.55
PISO_AMOSTRA_BAIXA = 0.60                # pares abaixo do alerta guardados so para a amostra de rotulagem


def exibicao(d: str) -> str:
    return " ".join(str(d).replace("\xa0", " ").split())


def carregar(xlsx, layout):
    base = pd.read_excel(xlsx, sheet_name=layout["abaBase"])
    base.columns = entrada.COLUNAS
    um = pd.read_excel(xlsx, sheet_name=layout["abaUnidades"])
    um.columns = ["sigla", "nome"]
    return base, um


class UF:
    def __init__(self, n): self.p = list(range(n))
    def find(self, x):
        while self.p[x] != x:
            self.p[x] = self.p[self.p[x]]; x = self.p[x]
        return x
    def union(self, a, b): self.p[self.find(a)] = self.find(b)


def montar_itens(base, cache):
    base["chave"] = base["desc"].map(lambda d: cache[d]["chave"])
    itens = []
    for chave, g in base.groupby("chave", sort=True):
        longa = max(g["desc"].unique(), key=len)
        trunc = cache[longa]["funcao"][-1] if len(longa) >= 40 and cache[longa]["funcao"] else None
        itens.append({"chave": chave, "linhas": g.index.tolist(), "trunc": trunc,
                      "descs": Counter(g["desc"]), "um": Counter(g["um"]),
                      "gs": sorted(g["gs"].unique()), "analise": cache[g["desc"].iloc[0]]})
    return itens


def candidatos(itens):
    """Pares candidatos por TF-IDF de n-gramas, dentro de blocos (mesmas flags e modificadores)."""
    blocos = defaultdict(list)
    for i, it in enumerate(itens):
        a = it["analise"]
        blocos[("+".join(a["flags"]), "+".join(a["modificadores"]))].append(i)
    pares = []
    for idxs in blocos.values():
        if len(idxs) < 2: continue
        textos = [" ".join(sorted(itens[i]["analise"]["funcao"])) or itens[i]["chave"] for i in idxs]
        X = TfidfVectorizer(analyzer="char_wb", ngram_range=(2, 4), sublinear_tf=True).fit_transform(textos)
        for ini in range(0, len(idxs), 400):
            S = (X[ini:ini + 400] @ X.T).toarray()
            for r in range(S.shape[0]):
                S[r, : ini + r + 1] = 0
            rr, cc = np.where(S >= RETRIEVAL_MIN)
            pares += [(idxs[ini + r], idxs[c]) for r, c in zip(rr, cc)]
    return pares


def pontuar_itens(itens, i, j):
    a, b = itens[i], itens[j]
    um_a, um_b = a["um"].most_common(1)[0][0], b["um"].most_common(1)[0][0]
    return pontuar(a["analise"]["funcao"], b["analise"]["funcao"], a["analise"]["numeros"], b["analise"]["numeros"],
                   um_a, um_b, a["trunc"], b["trunc"])


def escolher_canonico(descs: Counter, cache, dic):
    """Secao 9.4: nao truncada, sem prefixo sem valor semantico (SV-), mais usada, menos abreviada, legivel.
    Devolve (descricao, incerto): incerto = empate nos criterios principais (decisao final humana)."""
    sem_valor = set(dic["prefixos_sem_valor"])
    def k(d):
        a = cache[d]; pri = limpar(d).split()[:1]
        return (not a["possivelTruncamento"], not (pri and pri[0] in sem_valor), descs[d],
                -a["tokensAbreviadosUsados"], len(d), d)
    ordem = sorted(descs, key=k, reverse=True)
    incerto = len(ordem) > 1 and k(ordem[0])[:4] == k(ordem[1])[:4]
    return ordem[0], incerto


def executar(xlsx, saida, dic_path=None, processo=None):
    dic = carregar_dicionario(dic_path) if dic_path else carregar_dicionario()
    lim = dic["limiares"]
    saida = Path(saida); saida.mkdir(parents=True, exist_ok=True)
    agora = dt.datetime.now().astimezone().isoformat(timespec="seconds")

    # ---- etapas 1-2: layout, copia imutavel e lote ----
    layout = entrada.identificar_layout(xlsx)
    lote = abrir_lote(xlsx, saida, VERSAO_ALGORITMO, dic["versao"], processo)
    LID = lote["_id"]
    lote["layoutDetectado"] = layout

    # ---- etapas 3-4: mapear colunas, validar ----
    base_bruta, um_tab = carregar(xlsx, layout)
    base, rejeitadas, val_entrada = entrada.validar_entrada(base_bruta, set(um_tab["sigla"]))
    base["cs"] = base["cs"].astype(int); base["cf"] = base["cf"].astype(int)
    base["desc_raw"] = base["desc"].astype(str)
    base["desc"] = base["desc_raw"].map(exibicao)

    # ---- etapa 5: normalizar ----
    cache = {d: analisar(d, dic) for d in base["desc"].unique()}
    itens = montar_itens(base, cache)                       # etapa 6: exatas = mesma chave semantica

    # ---- etapas 7-8: aproximadas e agrupamento ----
    pares = candidatos(itens)
    pontuados, baixos = {}, []
    for i, j in pares:
        p = pontuar_itens(itens, i, j)
        if p["numerosDiferentes"]:
            if p["scoreBruto"] >= lim["provavel"]: pontuados[(i, j)] = p        # controle negativo
        elif p["score"] >= lim["alerta"]:
            pontuados[(i, j)] = p
        elif p["score"] >= PISO_AMOSTRA_BAIXA:
            baixos.append((i, j, p))
    uf = UF(len(itens))
    for (i, j), p in pontuados.items():
        if p["score"] >= lim["provavel"]: uf.union(i, j)
    comp = defaultdict(list)
    for x in range(len(itens)): comp[uf.find(x)].append(x)
    rejeitados = set()
    for raiz, membros in comp.items():
        if len(membros) < 3: continue
        minimo = min((pontuados.get((min(a, b), max(a, b))) or pontuar_itens(itens, min(a, b), max(a, b)))["score"]
                     for ix, a in enumerate(membros) for b in membros[ix + 1:])
        if minimo < lim["coesao_minima_cluster"]: rejeitados.add(raiz)
    clusters = []
    for raiz, membros in comp.items():
        clusters += [[m] for m in membros] if raiz in rejeitados else [membros]

    # ---- etapa 9: registro canonico ----
    novos = []
    for c in clusters:
        descs = Counter()
        for m in c: descs.update(itens[m]["descs"])
        canon, incerto = escolher_canonico(descs, cache, dic)
        critico = sum(descs.values()) >= dic["canonico_critico_min_linhas"]
        novos.append({"membros": c, "descs": descs, "canon": canon, "analise": cache[canon],
                      "incerto": bool(incerto and critico)})
    clusters = sorted(novos, key=lambda c: (c["analise"]["descricaoNormalizada"], c["canon"]))

    # ---- etapa 10: IDs tecnicos ----
    for n, c in enumerate(clusters, 1): c["csId"] = f"{n:06d}"
    item2cl = {m: c for c in clusters for m in c["membros"]}
    gs_id = {g: f"{i:03d}" for i, g in enumerate(GS_ORDEM, 1)}
    cf_id = {cf: f"{i:03d}" for i, cf in enumerate(sorted(base["cf"].unique()), 1)}
    um_id = {r.sigla: f"{i:02d}" for i, r in enumerate(um_tab.itertuples(), 1)}

    # ---- pendencias entre clusters distintos ----
    pend = {}
    for (i, j), p in pontuados.items():
        ci, cj = item2cl[i], item2cl[j]
        if ci is cj: continue
        tipo = ("CONTROLE_NEGATIVO_NUMEROS" if p["numerosDiferentes"] else
                "PROVAVEL_NAO_FUNDIDO_BAIXA_COESAO" if p["score"] >= lim["provavel"] else "ALERTA")
        k = tuple(sorted((ci["csId"], cj["csId"])))
        if k not in pend or p["score"] > pend[k]["score"]:
            a, b = (ci, cj) if ci["csId"] == k[0] else (cj, ci)
            pend[k] = {"tipo": tipo, "csIdA": k[0], "descricaoA": a["canon"], "csIdB": k[1], "descricaoB": b["canon"],
                       "score": p["score"], "scoreBruto": p["scoreBruto"], "motivo": p["motivo"],
                       "especificidade": a["analise"]["especificidade"],
                       "decisaoSugerida": "MANTER_SEPARADOS" if tipo == "CONTROLE_NEGATIVO_NUMEROS" else "REVISAR"}
    em_rev = defaultdict(set)
    for v in pend.values():
        if v["tipo"] != "CONTROLE_NEGATIVO_NUMEROS":
            em_rev[v["csIdA"]].add(v["csIdB"]); em_rev[v["csIdB"]].add(v["csIdA"])
    amostra_baixa = []
    for i, j, p in baixos:
        ci, cj = item2cl[i], item2cl[j]
        if ci is not cj:
            amostra_baixa.append({"descricaoA": ci["canon"], "descricaoB": cj["canon"], "score": p["score"], "motivo": p["motivo"]})

    # ---- etapa 11: catalogo consolidado ----
    catalogo = []
    for c in clusters:
        a = c["analise"]; incompleto = a["especificidade"].startswith("Mao de obra -")
        pend_tipos = (["SIMILAR"] if c["csId"] in em_rev else []) + (["CANONICO_INCERTO"] if c["incerto"] else []) \
                     + (["INCOMPLETO"] if incompleto else [])
        catalogo.append({
            "_id": c["csId"], "idBusca": c["csId"], "descricao": c["canon"],
            "descricaoNormalizada": a["descricaoNormalizada"], "descricaoExpandida": a["descricaoExpandida"],
            "descricaoStem": " ".join(stem_texto(a["descricaoExpandida"])),
            "quantidadeCaracteres": len(c["canon"]),
            "especificidade": a["especificidade"], "confiancaEspecificidade": a["confiancaEspecificidade"],
            "funcao": " ".join(a["funcao"]), "flags": a["flags"], "modificadores": a["modificadores"], "numeros": a["numeros"],
            "descricaoSugerida": descricao_sugerida(a, dic),
            "variantesLegado": [{"descricao": d, "linhas": n} for d, n in c["descs"].most_common()],
            "qtdVariantes": len(c["descs"]), "qtdLinhasLegado": sum(c["descs"].values()),
            "gsIds": sorted({gs_id[g] for m in c["membros"] for g in itens[m]["gs"]}),
            "legacyIds": sorted({str(base.at[l, "cs"]) for m in c["membros"] for l in itens[m]["linhas"]}),
            "status": "ATIVO", "emRevisao": c["csId"] in em_rev, "canonicoIncerto": c["incerto"], "incompleto": incompleto,
            "pendencias": pend_tipos, "statusRevisao": "EM_REVISAO" if pend_tipos else "OK",
            "candidatosSimilares": sorted(em_rev.get(c["csId"], []))[:5],
            "origem": "MIGRACAO", "usuarioCriacaoId": None, "createdAt": None, "legacyImportedAt": agora,
            "versaoNormalizacao": VERSAO_ALGORITMO, "loteImportacaoId": LID})

    # ---- etapa 12: de-para ----
    depara = []
    for c in clusters:
        canon_item = next(m for m in c["membros"] if c["canon"] in itens[m]["descs"])
        for m in c["membros"]:
            for l in itens[m]["linhas"]:
                r = base.loc[l]
                if r["desc"] == c["canon"]:
                    dec, motivo, sc = "MANTIDO", ("Descricao canonica do grupo" if len(c["descs"]) > 1 else "Sem variacao equivalente"), 1.0
                elif m == canon_item:
                    dec, motivo, sc = "CONSOLIDADO_EXATO", "Mesma descricao apos normalizacao e expansao de abreviacoes", 1.0
                else:
                    p = pontuar_itens(itens, min(m, canon_item), max(m, canon_item))
                    dec, sc = "CONSOLIDADO_PROVAVEL", p["score"]
                    motivo = f"Similaridade {p['score']:.2f} com o canonico; {p['motivo']}"
                depara.append({
                    "legacyKey": f"{r['cs']}|{r['cf']}|{r['um']}", "legacyId": str(r["cs"]), "legacyFornecedor": str(r["cf"]),
                    "legacyGS": r["gs"], "legacyDescricao": r["desc_raw"], "legacyUM": r["um"],
                    "legacySource": Path(xlsx).name, "newEntityType": "CS", "newId": c["csId"],
                    "decision": dec, "reason": motivo, "score": sc, "emRevisao": c["csId"] in em_rev,
                    "reviewedBy": None, "reviewedAt": None, "loteImportacaoId": LID})
    depara.sort(key=lambda x: (x["newId"], x["legacyKey"]))
    cs_novo = {d["legacyKey"]: d["newId"] for d in depara}

    # ---- componentes e codigos completos ----
    comum = {"origem": "MIGRACAO", "loteImportacaoId": LID}
    grupos = [{"_id": gs_id[g], "idBusca": gs_id[g], "nome": g, "nomeNormalizado": limpar(g), **comum} for g in GS_ORDEM]
    forn = [{"_id": v, "idBusca": v, "nome": f"Fornecedor {k}", "codigoLegado": str(k),
             "nomeNormalizado": f"fornecedor {k}", **comum} for k, v in cf_id.items()]
    umed = [{"_id": um_id[r.sigla], "idBusca": um_id[r.sigla], "nome": r.nome, "sigla": r.sigla,
             "nomeNormalizado": limpar(r.nome), **comum} for r in um_tab.itertuples()]
    desc_cs = {c["_id"]: c["descricao"] for c in catalogo}
    gs_nome = {gs_id[g]: g for g in GS_ORDEM}; sig = {v: k for k, v in um_id.items()}
    cf_nome = {v: f"Fornecedor {k}" for k, v in cf_id.items()}
    comp_ids = {}
    for r in base.itertuples():
        cs = cs_novo[f"{r.cs}|{r.cf}|{r.um}"]
        comp_ids.setdefault((gs_id[r.gs], cf_id[r.cf], cs, um_id[r.um]), set()).add(str(r.cs))
    completos = []
    for k, leg in sorted(comp_ids.items()):
        idc = montar_id_completo(*k)
        completos.append({
            "_id": idc, "idCompleto": idc, "idBusca": idc, "gsId": k[0], "cfId": k[1], "csId": k[2], "umId": k[3],
            "descricaoSnapshot": f"{gs_nome[k[0]]} | {cf_nome[k[1]]} | {desc_cs[k[2]]} | {sig[k[3]]}",
            "usuarioCriacaoId": None, "createdAt": None, "origem": "MIGRACAO", "status": "ATIVO",
            "legacyIds": sorted(leg), "legacyImportedAt": agora, "loteImportacaoId": LID})
    ctr = gerar_contadores(len(GS_ORDEM), len(cf_id), len(clusters), len(um_id))

    # ---- colecao de controle (4.1 'camada de controle'; 9.3 duplicata pendente -> revisao humana) ----
    controle, n = [], 0
    def novo_controle(tipo, **kw):
        nonlocal n
        n += 1
        controle.append({"_id": f"P{n:06d}", "tipo": tipo, "status": "PENDENTE", "justificativa": None, "decisao": None,
                         "decididoPor": None, "decididoEm": None, "historico": [], "criadoEm": agora,
                         "criadoPor": lote["usuarioProcesso"], "loteImportacaoId": LID, **kw})
    for v in sorted(pend.values(), key=lambda x: -x["score"]):
        if v["tipo"] != "CONTROLE_NEGATIVO_NUMEROS":
            novo_controle(v["tipo"], csIdA=v["csIdA"], csIdB=v["csIdB"], score=v["score"], motivo=v["motivo"],
                          descricaoA=v["descricaoA"], descricaoB=v["descricaoB"], decisaoSugerida=v["decisaoSugerida"])
    for c in catalogo:
        if c["canonicoIncerto"]:
            novo_controle("CANONICO_INCERTO", csId=c["_id"], score=None, descricaoCanonica=c["descricao"],
                          motivo="Empate entre descricoes candidatas em item de alto uso; escolha final humana (9.4)",
                          variantes=[v["descricao"] for v in c["variantesLegado"]])
    for c in catalogo:
        if c["incompleto"]:
            novo_controle("INCOMPLETO", csId=c["_id"], score=None, descricaoCanonica=c["descricao"],
                          motivo="Mao de obra sem especificidade identificada (9.1 / 4.1 controle de incompletos)")

    res = dict(catalogo=catalogo, depara=depara, controle=controle, contadores=ctr, grupos=grupos, fornecedores=forn,
               unidades=umed, completos=completos, rejeitadas=rejeitadas, base=base, cache=cache,
               pendencias=sorted(pend.values(), key=lambda x: -x["score"]), amostra_baixa=amostra_baixa,
               rejeitados=len(rejeitados), lote=lote, validacaoEntrada=val_entrada)
    res["qualidade"] = qualidade(base, cache, clusters, dic)

    # ---- etapas 13-14: validar e liberar ----
    checks = validar_migracao.validar(res)
    res["checks"] = checks
    dec = Counter(d["decision"] for d in depara)
    lote["metricas"] = {
        "linhasEntrada": val_entrada["linhasEntrada"], "linhasValidas": val_entrada["linhasValidas"],
        "linhasRejeitadas": val_entrada["linhasRejeitadas"], "descricoesDistintas": int(base["desc"].nunique()),
        "itensCatalogo": len(catalogo), "duplicidadesConsolidadas": int(base["desc"].nunique()) - len(catalogo),
        "linhasConsolidadasExato": dec.get("CONSOLIDADO_EXATO", 0), "linhasConsolidadasProvavel": dec.get("CONSOLIDADO_PROVAVEL", 0),
        "pendenciasPorTipo": dict(Counter(c["tipo"] for c in controle)),
        "coberturaDePara": round(sum(1 for d in depara if d["newId"]) / max(1, len(base)), 4),
        "volumeImportado": {"grupos_servico": len(grupos), "codigos_fornecedor": len(forn), "unidades_medida": len(umed),
                            "codigos_servico": len(catalogo), "codigos_completos": len(completos), "de_para": len(depara)},
        "limiares": lim}
    lote["resultadoValidacao"] = {"liberado": validar_migracao.liberado(checks),
                                  "verificacoes": len(checks), "falhas": [c["verificacao"] for c in checks if not c["ok"]]}
    lote["status"] = "LIBERADO" if lote["resultadoValidacao"]["liberado"] else "BLOQUEADO"
    gravar(res, saida)
    return res


def qualidade(base, cache, clusters, dic):
    q = []
    nb = int(base["desc_raw"].str.contains("\xa0").sum())
    q.append(("TEXTO", "Espacos nao separaveis (NBSP) nas descricoes", nb, "Quebram comparacao exata; tratados pelo normalizador"))
    ep = int((base["desc_raw"] != base["desc_raw"].str.strip()).sum())
    q.append(("TEXTO", "Descricoes com espacos nas pontas", ep, "Tratado na exibicao e na normalizacao"))
    t40 = int((base["desc"].str.len() >= 40).sum())
    q.append(("TRUNCAMENTO", "Descricoes com 40 caracteres (possivel truncamento pelo SAP)", t40, "Revisar: pode haver descricoes diferentes cortadas iguais"))
    g = base.groupby("cs")
    div = [cs for cs, x in g if x["desc"].map(lambda d: cache[d]["chave"]).nunique() > 1]
    q.append(("CODIGO", "Codigos de servico legados com descricoes semanticamente diferentes", len(div), "Provavel erro de cadastro: " + ", ".join(map(str, div[:10]))))
    mg = [cs for cs, x in g if x["gs"].nunique() > 1]
    q.append(("CODIGO", "Codigos de servico legados usados em mais de um Grupo de Servico", len(mg), "Decidir com negocio se o codigo e compartilhado"))
    hor = {"HE", "NOT", "PERIC", "INSAL", "HN"}
    inc = int(sum(1 for r in base.itertuples() if set(cache[r.desc]["flags"]) & hor and r.um != "H"))
    q.append(("UNIDADE", "Linhas de mao de obra por hora com unidade diferente de H (ex.: SV)", inc, "Unidade incoerente com a descricao"))
    nc = sum(1 for c in clusters if c["analise"]["especificidade"].startswith("Mao de obra -"))
    q.append(("TAXONOMIA", "Itens de mao de obra sem especificidade identificada", nc, "Ampliar dicionario/taxonomia"))
    sem = sum(1 for c in clusters if c["analise"]["especificidade"] == "Nao e mao de obra")
    q.append(("TAXONOMIA", "Itens classificados como 'Nao e mao de obra'", sem, "Amostrar para conferir"))
    return q


def amostra_depara_manual(r, n=150, seed=11):
    """Etapa 13 / 16.5 'amostra manual do de-para': estratificada por decisao, para conferencia humana."""
    import random
    rnd = random.Random(seed); cat = {c["_id"]: c["descricao"] for c in r["catalogo"]}
    por = defaultdict(list)
    for d in r["depara"]: por[d["decision"]].append(d)
    cota = {"CONSOLIDADO_PROVAVEL": 40, "CONSOLIDADO_EXATO": 50, "MANTIDO": n - 90}
    out = []
    for dec, k in cota.items():
        L = por.get(dec, []); rnd.shuffle(L)
        out += [{"decision": x["decision"], "legacyId": x["legacyId"], "legacyGS": x["legacyGS"],
                 "legacyDescricao": x["legacyDescricao"], "newId": x["newId"], "descricaoNova": cat[x["newId"]],
                 "score": x["score"], "reason": x["reason"]} for x in L[:k]]
    return out


def gravar(r, saida: Path):
    def jl(nome, linhas):
        with open(saida / f"{nome}.jsonl", "w", encoding="utf-8") as f:
            for x in linhas: f.write(json.dumps(x, ensure_ascii=False, default=str) + "\n")
    for nome, chave in (("grupos_servico", "grupos"), ("codigos_fornecedor", "fornecedores"), ("unidades_medida", "unidades"),
                        ("codigos_servico", "catalogo"), ("codigos_completos", "completos"), ("de_para", "depara"),
                        ("controle_excecoes", "controle"), ("contadores", "contadores")):
        jl(nome, r[chave])
    jl("lotes_importacao", [r["lote"]])
    (saida / "lote.json").write_text(json.dumps(r["lote"], ensure_ascii=False, indent=2, default=str), encoding="utf-8")
    pend = pd.DataFrame(r["pendencias"])
    if len(pend):
        neg = pend["tipo"] == "CONTROLE_NEGATIVO_NUMEROS"
        pend[~neg].to_csv(saida / "pendencias_revisao.csv", index=False, encoding="utf-8-sig")
        pend[neg].to_csv(saida / "controle_negativo_numeros.csv", index=False, encoding="utf-8-sig")
    pd.DataFrame(amostra_depara_manual(r)).to_csv(saida / "amostra_depara_manual.csv", index=False, encoding="utf-8-sig")
    pd.DataFrame(r["checks"]).to_csv(saida / "validacao_migracao.csv", index=False, encoding="utf-8-sig")
    r["rejeitadas"].to_csv(saida / "linhas_rejeitadas.csv", index=False, encoding="utf-8-sig")


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("xlsx"); ap.add_argument("--saida", default="saida")
    ap.add_argument("--relatorio", default=None, help="caminho do .xlsx de relatorio")
    a = ap.parse_args()
    r = executar(a.xlsx, a.saida)
    if a.relatorio:
        from .relatorio import gerar
        gerar(r, a.relatorio)
    m = r["lote"]["metricas"]
    print(f"lote={r['lote']['_id']} status={r['lote']['status']} catalogo={m['itensCatalogo']} depara={m['volumeImportado']['de_para']} "
          f"completos={m['volumeImportado']['codigos_completos']} controle={sum(m['pendenciasPorTipo'].values())} "
          f"validacoes={r['lote']['resultadoValidacao']['verificacoes']} falhas={len(r['lote']['resultadoValidacao']['falhas'])}")


if __name__ == "__main__":
    main()
