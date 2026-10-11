import sys; sys.path.insert(0, str(__import__("pathlib").Path(__file__).resolve().parent.parent))
import copy, json
from _dados import escrever
from gc_dados.pipeline import executar
from gc_dados import validar_migracao
from gc_dados.ids import montar_id_completo


def _rodar(tmp_path):
    return executar(escrever(tmp_path / "base.xlsx"), tmp_path / "out")


def test_pipeline_ponta_a_ponta(tmp_path):
    r = _rodar(tmp_path)
    assert len(r["depara"]) == 6
    assert len(r["catalogo"]) == 4                       # 3 pintor -> 1, 2 tubos, 1 encarregado
    assert all(len(c["idCompleto"]) == 14 for c in r["completos"])
    assert all(c["quantidadeCaracteres"] <= 40 for c in r["catalogo"])
    assert sum(1 for d in r["depara"] if d["decision"] != "MANTIDO") == 2
    for nome in ("codigos_servico", "de_para", "codigos_completos", "controle_excecoes", "contadores", "lotes_importacao"):
        assert (tmp_path / "out" / f"{nome}.jsonl").exists()


def test_lote_liberado_com_hash_e_copia_imutavel(tmp_path):
    import os, stat
    r = _rodar(tmp_path); l = r["lote"]
    assert l["status"] == "LIBERADO" and len(l["sha256"]) == 64
    copia = tmp_path / "out" / l["copiaImutavel"]
    assert copia.exists() and not (os.stat(copia).st_mode & stat.S_IWRITE)    # somente leitura
    assert l["versaoAlgoritmoNormalizacao"] and l["usuarioProcesso"] and l["dataImportacao"]
    assert l["resultadoValidacao"]["falhas"] == []


def test_canonico_prefere_descricao_sem_prefixo_sv(tmp_path):
    r = _rodar(tmp_path)
    pintor = next(c for c in r["catalogo"] if c["descricao"].upper().endswith("HORA NORMAL)") or "PINTOR" in c["descricao"].upper())
    assert not pintor["descricao"].upper().startswith("SV")


def test_dados_legados_sem_usuario_nem_timestamp(tmp_path):                      # 9.5 / 16.1 campos nulos
    r = _rodar(tmp_path)
    for x in r["catalogo"] + r["completos"]:
        assert x["usuarioCriacaoId"] is None and x["createdAt"] is None and x["origem"] == "MIGRACAO"
        assert x["legacyImportedAt"] and x["loteImportacaoId"] == r["lote"]["_id"]


def test_conversao_de_dados_legados_mantem_codigos(tmp_path):                    # 16.1 conversao legada
    r = _rodar(tmp_path)
    pintor = next(c for c in r["catalogo"] if "PINTOR" in c["descricao"].upper())
    assert set(pintor["legacyIds"]) == {"111", "222"}
    assert {d["legacyDescricao"] for d in r["depara"] if d["newId"] == pintor["_id"]} == {"PINTOR - HORA NORMAL", "PINTOR (HORA NORMAL)", "SV-PINTOR HN"}


def test_validacao_detecta_linha_perdida_no_depara(tmp_path):                    # 16.5 (teste negativo)
    r = _rodar(tmp_path); r2 = dict(r); r2["depara"] = r["depara"][:-1]
    falhas = [c["verificacao"] for c in validar_migracao.validar(r2) if not c["ok"]]
    assert any("linhas de entrada" in f for f in falhas)
    assert not validar_migracao.liberado(validar_migracao.validar(r2))


def test_validacao_detecta_componente_orfao_e_id_invalido(tmp_path):
    r = _rodar(tmp_path); r2 = dict(r); comp = copy.deepcopy(r["completos"])
    comp[0]["csId"] = "999999"                                                    # componente inexistente
    comp[1]["idCompleto"] = "123"                                                 # id invalido
    r2["completos"] = comp
    falhas = " | ".join(c["verificacao"] for c in validar_migracao.validar(r2) if not c["ok"])
    assert "componentes referenciados" in falhas and "invalidos" in falhas


def test_validacao_detecta_perda_de_historico(tmp_path):
    r = _rodar(tmp_path); r2 = dict(r); dp = copy.deepcopy(r["depara"]); dp[0]["legacyDescricao"] = "ADULTERADO"
    r2["depara"] = dp
    assert any("historicos" in c["verificacao"] and not c["ok"] for c in validar_migracao.validar(r2))


def test_validacao_detecta_usuario_fabricado(tmp_path):
    r = _rodar(tmp_path); r2 = dict(r); cat = copy.deepcopy(r["catalogo"]); cat[0]["usuarioCriacaoId"] = "usuario-falso"
    r2["catalogo"] = cat
    assert any("fabricados" in c["verificacao"] and not c["ok"] for c in validar_migracao.validar(r2))


def test_linhas_invalidas_sao_rejeitadas_e_registradas(tmp_path):                # etapa 4
    from _dados import LINHAS
    ruins = LINHAS + [["Civil", 1001, 555, "X" * 41, "H"], ["Civil", 1001, 556, "OK", "ZZZ"], ["Marte", 1001, 557, "OK", "H"]]
    r = executar(escrever(tmp_path / "b.xlsx", ruins), tmp_path / "o")
    assert len(r["depara"]) == 6 and len(r["rejeitadas"]) == 3
    assert r["validacaoEntrada"]["linhasRejeitadas"] == 3 and r["lote"]["status"] == "LIBERADO"
    assert "acima de 40" in " ".join(r["rejeitadas"]["motivoRejeicao"])


def test_id_completo_unico_por_composicao(tmp_path):
    r = _rodar(tmp_path)
    ids = [c["idCompleto"] for c in r["completos"]]
    assert len(ids) == len(set(ids))
    assert all(montar_id_completo(c["gsId"], c["cfId"], c["csId"], c["umId"]) == c["idCompleto"] for c in r["completos"])
