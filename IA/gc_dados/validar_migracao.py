"""Validacao da migracao: etapa 13 (secao 9.1) e testes de migracao da secao 16.5.
Cada verificacao informa a secao do documento, se e bloqueante e o detalhe. O lote so e LIBERADO (etapa 14)
se todas as verificacoes bloqueantes passam."""
import re
from collections import Counter

from .ids import LARGURAS, montar_id_completo


def validar(r: dict) -> list:
    cks = []
    def chk(secao, nome, ok, detalhe="", bloqueante=True):
        cks.append({"secao": secao, "verificacao": nome, "ok": bool(ok), "bloqueante": bloqueante, "detalhe": str(detalhe)})

    base, cat, dp, comp = r["base"], r["catalogo"], r["depara"], r["completos"]
    # --- contagem antes e depois
    chk("16.5", "Contagem: linhas de entrada = linhas no de-para", len(base) == len(dp), f"{len(base)} x {len(dp)}")
    chk("16.5", "Linhas legado preservadas nos itens (soma de qtdLinhasLegado)",
        sum(c["qtdLinhasLegado"] for c in cat) == len(base), f"{sum(c['qtdLinhasLegado'] for c in cat)} x {len(base)}")
    distintas = base["desc"].nunique()
    chk("16.5", "Descricoes distintas = soma das variantes dos itens",
        sum(c["qtdVariantes"] for c in cat) == distintas, f"{sum(c['qtdVariantes'] for c in cat)} x {distintas}")
    chk("16.5", "Catalogo nao tem mais itens que descricoes distintas", len(cat) <= distintas, f"{len(cat)} <= {distintas}")
    dec = Counter(d["decision"] for d in dp)
    chk("16.5", "Quantidade de duplicatas encontradas (informativo)", True,
        f"exatas={dec.get('CONSOLIDADO_EXATO', 0)} provaveis={dec.get('CONSOLIDADO_PROVAVEL', 0)} "
        f"pendentes={sum(1 for c in r['controle'] if c['tipo'] in ('ALERTA', 'PROVAVEL_NAO_FUNDIDO_BAIXA_COESAO'))}", False)
    # --- componentes referenciados
    ids = {k: {x["_id"] for x in r[k2]} for k, k2 in (("gs", "grupos"), ("cf", "fornecedores"), ("um", "unidades"), ("cs", "catalogo"))}
    falta = [c["_id"] for c in comp if c["gsId"] not in ids["gs"] or c["cfId"] not in ids["cf"]
             or c["csId"] not in ids["cs"] or c["umId"] not in ids["um"]]
    chk("16.5", "Todos os componentes referenciados pelos codigos completos existem", not falta, f"{len(falta)} orfaos")
    chk("16.5", "Todo newId do de-para existe no catalogo", all(d["newId"] in ids["cs"] for d in dp))
    refs = {x for c in r["controle"] for x in (c.get("csIdA"), c.get("csIdB"), c.get("csId")) if x}
    chk("9.3", "Todo item citado em pendencias existe no catalogo", refs <= ids["cs"], f"{len(refs - ids['cs'])} inexistentes")
    # --- codigos completos validos e unicos
    invalidos = []
    for c in comp:
        try:
            if not (montar_id_completo(c["gsId"], c["cfId"], c["csId"], c["umId"]) == c["idCompleto"] == c["_id"]):
                invalidos.append(c["_id"])
        except ValueError:
            invalidos.append(c["_id"])
    chk("16.5", "Ausencia de codigos completos invalidos (14 digitos = gs+cf+cs+um)", not invalidos, f"{len(invalidos)} invalidos")
    chk("8.4", "idCompleto unico", len({c["idCompleto"] for c in comp}) == len(comp))
    chk("8.4", "Combinacao dos quatro componentes unica",
        len({(c["gsId"], c["cfId"], c["csId"], c["umId"]) for c in comp}) == len(comp))
    chk("8.3", "IDs de componentes com largura fixa",
        all(re.fullmatch(rf"\d{{{LARGURAS[k + 'Id']}}}", x["_id"]) for k, kk in (("gs", "grupos"), ("cf", "fornecedores"), ("um", "unidades"), ("cs", "catalogo")) for x in r[kk]))
    # --- historico preservado
    orig = Counter((str(x.cs), str(x.cf), x.gs, x.um, x.desc_raw) for x in base.itertuples())
    novo = Counter((d["legacyId"], d["legacyFornecedor"], d["legacyGS"], d["legacyUM"], d["legacyDescricao"]) for d in dp)
    chk("16.5", "Dados historicos preservados no de-para (linha a linha, texto original)", orig == novo)
    leg_cat = {l for c in cat for l in c["legacyIds"]}
    sem = {str(x) for x in base["cs"]} - leg_cat
    chk("9.3", "Nada foi apagado: todo codigo legado esta em legacyIds de algum item", not sem, f"{len(sem)} sem destino")
    # --- dados ausentes (9.5) e descricao (1.3)
    chk("9.5", "Legado sem usuario/timestamp fabricados (nulos + origem MIGRACAO)",
        all(c["usuarioCriacaoId"] is None and c["createdAt"] is None and c["origem"] == "MIGRACAO" for c in cat + comp))
    chk("9.5", "legacyImportedAt e loteImportacaoId preenchidos", all(c.get("legacyImportedAt") and c.get("loteImportacaoId") for c in cat + comp))
    chk("1.3", "Descricoes canonicas <= 40 caracteres e quantidadeCaracteres correto",
        all(len(c["descricao"]) <= 40 and c["quantidadeCaracteres"] == len(c["descricao"]) for c in cat))
    chk("8.4", "csId unico", len(ids["cs"]) == len(cat))
    cont = {c["_id"]: c["seq"] for c in r["contadores"]}
    chk("8.2", "Contadores de sequencia cobrem o maior ID usado (IDs nunca reutilizados)",
        cont["cs"] >= max(int(x) for x in ids["cs"]) and cont["cf"] >= max(int(x) for x in ids["cf"]))
    return cks


def liberado(checks) -> bool:
    return all(c["ok"] for c in checks if c["bloqueante"])
