import sys; sys.path.insert(0, str(__import__("pathlib").Path(__file__).resolve().parent.parent))
import os, stat
from _dados import escrever, CAB
from gc_dados import entrada
from gc_dados.ids import montar_id_completo, separar_id_completo, formatar_id, contadores
from gc_dados.lote import abrir_lote, sha256_arquivo
from gc_dados.normalizador import limpar, analisar, carregar_dicionario
from gc_dados.similaridade import decisao
from gc_dados.stemmer import stem


# ---------- stemming (6.4)
def test_stemming_aproxima_variacoes_morfologicas():
    assert stem(limpar("manutenção")) == stem(limpar("manutenções")) == stem("manutencao")
    assert stem("mecanico") == stem("mecanica") == stem("mecanicos")
    assert stem("soldador") == stem("soldadores") == stem("soldadora")

def test_stemming_nao_e_correcao_ortografica():                                  # 6.4: precisa de fuzzy junto
    assert stem("manutencao") != stem("manutencoa")


# ---------- IDs (8) e unitarios (16.1: montagem do ID completo)
def test_montagem_do_id_completo():
    assert montar_id_completo("001", "001", "000001", "01") == "00100100000101"       # exemplo do documento
    assert separar_id_completo("00100100000101") == {"gsId": "001", "cfId": "001", "csId": "000001", "umId": "01"}

def test_id_completo_rejeita_larguras_erradas():
    for args in (("1", "001", "000001", "01"), ("001", "001", "1", "01"), ("001", "001", "000001", "1"), ("001", "00A", "000001", "01")):
        try: montar_id_completo(*args); assert False, args
        except ValueError: pass

def test_ids_nunca_estouram_silenciosamente_e_contadores_ok():
    assert formatar_id(7, 3) == "007"
    try: formatar_id(1000, 3); assert False
    except OverflowError: pass
    assert {c["_id"]: c["seq"] for c in contadores(3, 111, 6940, 17)} == {"gs": 3, "cf": 111, "cs": 6940, "um": 17}


# ---------- 40 caracteres (16.1: contagem de caracteres)
def test_contagem_de_caracteres_e_truncamento():
    d = carregar_dicionario()
    assert analisar("A" * 40, d)["quantidadeCaracteres"] == 40 and analisar("A" * 40, d)["possivelTruncamento"]
    assert analisar("Manutenção mecânica normal", d)["quantidadeCaracteres"] == 26          # o exemplo do doc dizia 29


# ---------- classificacao por score (16.1)
def test_classificacao_por_score_segue_a_politica_do_documento():
    lim = carregar_dicionario()["limiares"]
    assert decisao(0.95, lim) == "REVISAR" and decisao(0.90, lim) == "REVISAR"
    assert decisao(0.89, lim) == "ALERTAR" and decisao(0.75, lim) == "ALERTAR"
    assert decisao(0.74, lim) == "PERMITIR" and decisao(0.0, lim) == "PERMITIR"

def test_limiares_podem_vir_de_variavel_de_ambiente():                            # secao 17
    os.environ["GC_LIMIAR_PROVAVEL"] = "0.95"
    try: assert carregar_dicionario()["limiares"]["provavel"] == 0.95
    finally: del os.environ["GC_LIMIAR_PROVAVEL"]


# ---------- layout e validacao de entrada (9.1 etapas 1, 3, 4)
def test_layout_reconhecido(tmp_path):
    l = entrada.identificar_layout(escrever(tmp_path / "a.xlsx"))
    assert l["abaBase"] == "1. Base" and l["mapeamento"]["gs"] == "Grupo de Serviço"

def test_layout_invalido_e_recusado(tmp_path):
    try: entrada.identificar_layout(escrever(tmp_path / "b.xlsx", cab=["a", "b", "c", "d", "e"])); assert False
    except ValueError as e: assert "Cabecalhos" in str(e)

def test_validacao_de_entrada_regras():
    import pandas as pd
    b = pd.DataFrame([["Civil", 1, 10, "OK", "H"], ["Civil", 1, 10, "DUP", "H"], ["Civil", "x", 11, "OK", "H"],
                      ["Civil", 1, 12, "", "H"], ["Civil", 1, 13, "OK", "??"], ["Civil", 1, 14, "Y" * 41, "H"]], columns=entrada.COLUNAS)
    ok, rej, rel = entrada.validar_entrada(b, {"H"})
    assert len(ok) == 1 and len(rej) == 5 and rel["status"] == "COM_REJEICOES"


# ---------- lote (9.2)
def test_lote_guarda_hash_copia_somente_leitura(tmp_path):
    f = escrever(tmp_path / "o.xlsx")
    l = abrir_lote(f, tmp_path / "s", "1.0.0", "1.0.0", "teste")
    assert l["sha256"] == sha256_arquivo(f)
    copia = tmp_path / "s" / l["copiaImutavel"]
    assert sha256_arquivo(copia) == l["sha256"] and not (os.stat(copia).st_mode & stat.S_IWRITE)
    assert l["usuarioProcesso"] == "teste" and l["status"] == "EM_PROCESSAMENTO"

def test_mesmo_arquivo_gera_mesmo_hash(tmp_path):
    a, b = escrever(tmp_path / "a.xlsx"), escrever(tmp_path / "b.xlsx")
    assert sha256_arquivo(a) != "" and abrir_lote(a, tmp_path / "s", "1", "1")["sha256"] == sha256_arquivo(a)
