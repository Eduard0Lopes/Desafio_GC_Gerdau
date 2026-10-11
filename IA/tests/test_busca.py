import sys; sys.path.insert(0, str(__import__("pathlib").Path(__file__).resolve().parent.parent))
from gc_dados.busca import BuscaMemoria, pipeline_atlas
from gc_dados.normalizador import analisar, carregar_dicionario, limpar
from gc_dados.stemmer import stem_texto

D = carregar_dicionario()


def _cs(i, desc, uso=1, legado=(), status="ATIVO"):
    a = analisar(desc, D)
    return {"_id": f"{i:06d}", "descricao": desc, "descricaoNormalizada": a["descricaoNormalizada"],
            "descricaoExpandida": a["descricaoExpandida"], "legacyIds": list(legado), "qtdLinhasLegado": uso, "status": status}


CATALOGO = [
    _cs(1, "MECÂNICA INDUSTRIAL", uso=50), _cs(2, "MECÂNICO DE MANUTENÇÃO", uso=200), _cs(3, "MANUTENÇÃO ELETROMECÂNICA", uso=20),
    _cs(4, "PINTOR - HORA NORMAL"), _cs(5, "ENCARREGADO HE 50%", legado=("4521", "MEC.MANUT.NORM")),
    _cs(6, "UNIDADE DE MEDIDA"), _cs(7, "MANUTENÇÃO MECÂNICA NORMAL", uso=0, status="INATIVO"),
]
B = BuscaMemoria({"codigos_servico": CATALOGO, "unidades_medida": [
    {"_id": "01", "nome": "Hora", "sigla": "HR"}, {"_id": "02", "nome": "Unidade", "sigla": "UN"}, {"_id": "03", "nome": "Metro Quadrado", "sigla": "M2"}],
    "grupos_servico": [{"_id": "001", "nome": "Eletromecânica"}, {"_id": "002", "nome": "Facilities"}]}, D)
def busca(q, col="codigos_servico", **kw): return B.buscar(col, q, **kw)


def test_ranking_id_exato_acima_de_tudo():                                                # 6.7 (1)
    r = busca("000004")["resultados"][0]
    assert r["id"] == "000004" and r["tipoMatch"] == "ID_EXATO"

def test_busca_por_id_legado():                                                           # 2.2 busca por ID
    assert busca("4521")["resultados"][0]["id"] == "000005"
    assert busca("mec.manut.norm")["resultados"][0]["tipoMatch"] == "ID_LEGADO_EXATO"

def test_prefixo_de_id():                                                                 # 6.7 (2)
    assert busca("0000")["resultados"][0]["tipoMatch"] == "PREFIXO_ID"

def test_autocomplete_do_exemplo_do_documento():                                          # 6.2: mec -> 3 sugestoes
    ids = [x["id"] for x in busca("mec", limite=10)["resultados"]]
    assert {"000001", "000002", "000003"} <= set(ids)                                     # inclui Eletromecanica (meio da palavra)
    assert ids.index("000003") > ids.index("000001")                                      # prefixo de palavra vem antes de substring

def test_texto_exato_vence_tokens_e_fuzzy():                                              # 6.7 (3) > (4) > (5)
    r = busca("pintor hora normal")["resultados"]
    assert r[0]["id"] == "000004" and r[0]["tipoMatch"] == "TEXTO_EXATO"
    tipos = {x["tipoMatch"] for x in busca("manutencao")["resultados"]}
    assert "TOKENS" in tipos

def test_ordem_invertida_das_palavras():                                                  # 6.3
    assert busca("manutencao mecanico")["resultados"][0]["id"] == "000002"
    assert busca("mecanico manutencao")["resultados"][0]["id"] == "000002"

def test_fuzzy_erro_de_digitacao():                                                       # 6.3
    r = busca("manutencao mecnico")["resultados"][0]
    assert r["id"] == "000002" and r["tipoMatch"] in ("FUZZY", "TOKENS_PREFIXO")
    assert busca("eletromecanica")["resultados"][0]["id"] == "000003"

def test_acento_e_caixa_nao_importam():                                                   # 6.5
    assert busca("MANUTENÇÃO MECÂNICO")["resultados"][0]["id"] == busca("manutencao mecanico")["resultados"][0]["id"]

def test_stemming_na_busca():                                                             # 6.4
    r = busca("manutencoes mecanicos")["resultados"]
    assert r and r[0]["id"] in ("000002", "000007") and r[0]["tipoMatch"] in ("TOKENS_STEM", "TOKENS")

def test_unid_medida_do_exemplo_do_documento():                                           # 6.3
    assert busca("unid medida")["resultados"][0]["id"] == "000006"

def test_valores_padronizados_exatos_HR_UN_M2():                                          # 6.1
    r = busca("m2", "unidades_medida")["resultados"][0]; assert r["id"] == "03" and r["tipoMatch"] == "VALOR_EXATO"
    assert busca("un", "unidades_medida")["resultados"][0]["id"] == "02"

def test_relevancia_por_uso_e_status_ativo():                                             # 6.7 (6)
    r = busca("mecanica", limite=10)["resultados"]
    ordem = [x["id"] for x in r]
    assert ordem.index("000001") < ordem.index("000007")                                  # ativo/uso acima do inativo sem uso

def test_resposta_enxuta_e_paginacao():                                                   # 14.1
    r = busca("manutencao", limite=2)
    assert len(r["resultados"]) == 2 and r["total"] >= 3
    assert set(r["resultados"][0]) == {"id", "nome", "descricao", "score", "tipoMatch"}
    assert busca("manutencao", limite=2, offset=2)["resultados"][0]["id"] != r["resultados"][0]["id"]
    assert busca("   ")["total"] == 0 and busca("zzzzzzzz")["total"] == 0

def test_busca_em_componentes():
    assert busca("eletro", "grupos_servico")["resultados"][0]["id"] == "001"

def test_pipeline_atlas_tem_os_mesmos_niveis_do_ranking():
    p = pipeline_atlas("mec", limite=5); should = p[0]["$search"]["compound"]["should"]
    boosts = [list(s.values())[0]["score"]["boost"]["value"] for s in should]
    assert boosts[0] == max(boosts) and "limit" in str(p[2]).lower() and p[2]["$limit"] == 5   # ID exato pesa mais
    assert p[0]["$search"]["compound"]["filter"][0]["equals"]["value"] == "ATIVO"


def test_sinonimos_na_busca_secao_20():
    import copy
    dic = copy.deepcopy(D); dic["sinonimos"] = {"conserto": "reparo"}
    b = BuscaMemoria({"codigos_servico": [_cs(1, "REPARO DE BOMBA"), _cs(2, "PINTURA PREDIAL")]}, dic)
    assert b.buscar("codigos_servico", "conserto bomba")["resultados"][0]["id"] == "000001"
    assert BuscaMemoria({"codigos_servico": [_cs(1, "REPARO DE BOMBA")]}, D).buscar("codigos_servico", "conserto")["total"] == 0
