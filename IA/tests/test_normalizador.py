import sys; sys.path.insert(0, str(__import__("pathlib").Path(__file__).resolve().parent.parent))
from gc_dados.normalizador import analisar, carregar_dicionario, descricao_sugerida
from gc_dados.similaridade import pontuar

D = carregar_dicionario()
A = lambda s: analisar(s, D)
def sim(x, y, **kw):
    a, b = A(x), A(y)
    return pontuar(a["funcao"], b["funcao"], a["numeros"], b["numeros"], **kw)


def test_variantes_de_escrita_tem_mesma_chave():
    assert len({A(s)["chave"] for s in ["PINTOR - HORA NORMAL", "PINTOR (HORA NORMAL)", "SV-Pintor HN", "Pintor  hora normal\xa0"]}) == 1

def test_he_noturna_domfer_em_formas_diferentes():
    for s in ["MONTADOR (HE NOT DOM.FER)", "SV MONTADOR HORA EXTRA NOTURNA DOM e FER"]:
        assert A(s)["especificidade"] == "Hora extra noturna Dom/Feriado"

def test_prefixos_compactos_do_facilities():
    assert A("H.E.SS-Aux. Adm. Almox.-ADM")["especificidade"] == "Hora extra Seg a Sab"
    assert A("H.P.E.DF-Aux. de Sv. Gerais-ADM")["especificidade"] == "Adicional periculosidade s/ hora extra"
    assert A("Peric.Mês - Of. de Manut. Predial - ADM")["especificidade"] == "Adicional periculosidade (mensal)"

def test_typo_em_palavra_chave():
    assert A("ALOCADOR (PRERICULOSIDADE)")["especificidade"] == "Adicional periculosidade (hora)"

def test_numeros_separam_itens():
    assert A("SV-TUBO PVC 25 MM")["chave"] != A("SV-TUBO PVC 32MM")["chave"]
    assert A("TUBO 50mm")["chave"] == A("TUBO 50 mm")["chave"]
    assert sim("ENCARREGADO HE 50%", "ENCARREGADO HE 100%")["score"] <= 0.60

def test_numero_presente_em_um_so_gera_alerta():
    s = sim("ENCARREGADO HE 50%", "ENCARREGADO HE")["score"]; assert 0.75 <= s <= 0.85

def test_operadores_nao_somem():
    assert A('ARMADURA D > 1/2"')["chave"] != A('ARMADURA D <= 1/2"')["chave"]

def test_mob_e_desmob_sao_diferentes():
    assert A("MOBILIZACAO DE EQUIPE")["chave"] != A("DESMOBILIZACAO DE EQUIPE")["chave"]

def test_negacao_com_sem_nao_funde():
    assert sim("TRECHO TRILHO ACIMA DE 181M c/linha de vida", "TRECHO TRILHO ACIMA DE 181M s/linha de vida")["score"] < 0.75

def test_produtiva_x_improdutiva_nao_casam():
    assert sim("GERADOR 10kVA - HORA PRODUTIVA", "GERADOR 10kVA - HORA IMPRODUTIVA")["score"] < 0.90

def test_termo_extra_vira_alerta_nao_fusao():
    s = sim("OFICIAL MANUT PREDIAL ADM", "OFICIAL MANUT PREDIAL NR35 ADM")["score"]; assert 0.0 < s < 0.90

def test_typo_vira_fusao_provavel():
    assert sim("ENCARRREGADO HE 50%", "ENCARREGADO HE 50%")["score"] >= 0.90

def test_truncamento_so_vale_no_ultimo_termo_de_40_chars():
    a = A("SV-OFICIAL COM MARTELO ROMPEDOR OU ELETR"); b = A("SV-OFICIAL COM MARTELO ROMPEDOR OU ELETRICO")
    ok = pontuar(a["funcao"], b["funcao"], [], [], trunc_a=a["funcao"][-1])["score"]
    no = pontuar(a["funcao"], b["funcao"], [], [])["score"]
    assert ok >= 0.90 and no < 0.90

def test_descricao_sugerida_respeita_40():
    s = descricao_sugerida(A("SOLDADOR - HORA EXTRA 100% DOM e FER"), D)
    assert s and len(s) <= 40
    assert descricao_sugerida(A("TUBO PVC 25 MM"), D) is None
