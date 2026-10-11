import sys; sys.path.insert(0, str(__import__("pathlib").Path(__file__).resolve().parent.parent))
from gc_dados.avaliacao import carregar_rotulos, metricas, recomendar, varrer


def _pares():
    dup = [0.99, 0.97, 0.95, 0.93, 0.91, 0.88, 0.82, 0.60]
    dif = [0.92, 0.86, 0.79, 0.70, 0.62, 0.55, 0.40, 0.30]
    return ([{"rotulo": "DUPLICATA", "score": s} for s in dup] + [{"rotulo": "DIFERENTE", "score": s} for s in dif]
            + [{"rotulo": "DUVIDA", "score": 0.9}])


def test_metricas_precisao_revocacao_falsos_bloqueios():
    m = metricas(_pares(), 0.90)
    assert (m["tp"], m["fp"], m["fn"], m["tn"]) == (5, 1, 3, 7)
    assert abs(m["precisao"] - 5 / 6) < 1e-9 and abs(m["revocacao"] - 5 / 8) < 1e-9
    assert abs(m["taxaFalsosBloqueios"] - 1 / 8) < 1e-9

def test_duvida_nao_entra_na_conta():
    assert metricas(_pares(), 0.0)["tp"] + metricas(_pares(), 0.0)["fp"] == 16

def test_varredura_e_recomendacao():
    assert len(varrer(_pares())) > 40
    rec = recomendar(_pares(), precisao_minima=0.99, falsos_bloqueios_max=0.0)
    assert rec["limiarProvavel"] is not None and rec["limiarProvavel"] > 0.92
    assert rec["pares"] == 16

def test_leitura_da_aba_preenchida(tmp_path):
    from openpyxl import Workbook
    wb = Workbook(); ws = wb.active; ws.title = "Amostra p rotular"
    ws.append(["legenda"]); ws.append(["ID", "Origem", "A", "B", "Score", "Motivo", "Rotulo"])
    ws.append([1, "x", "a", "b", 0.95, "m", "duplicata"]); ws.append([2, "x", "a", "c", 0.80, "m", "DIFERENTE"])
    ws.append([3, "x", "a", "d", 0.80, "m", None]); ws.append([4, "x", "a", "e", 0.80, "m", "dúvida"])
    wb.save(tmp_path / "r.xlsx")
    p = carregar_rotulos(tmp_path / "r.xlsx")
    assert [x["rotulo"] for x in p] == ["DUPLICATA", "DIFERENTE", "DUVIDA"]
