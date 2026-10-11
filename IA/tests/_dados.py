"""Auxiliares de teste: monta uma planilha minima no layout real."""
import pandas as pd

CAB = ["Grupo de Serviço", "Código do Fornecedor", "Código do Serviço", "Descrição do Item", "Unidade medida"]
LINHAS = [
    ["Civil", 1001, 111, "PINTOR - HORA NORMAL", "H"], ["Civil", 1002, 111, "PINTOR (HORA NORMAL)", "H"],
    ["Eletromecânica", 1001, 222, "SV-PINTOR HN", "H"],
    ["Civil", 1001, 333, "TUBO PVC 25 MM", "ST"], ["Civil", 1002, 334, "TUBO PVC 32 MM", "ST"],
    ["Facilities", 1003, 444, "ENCARREGADO HE 50%", "H"],
]


def escrever(caminho, linhas=None, cab=None):
    base = pd.DataFrame(linhas if linhas is not None else LINHAS, columns=cab or CAB)
    um = pd.DataFrame([["H", "Hora"], ["ST", "Unidade"]], columns=["Sigla Unidade de Medida", "Descrição"])
    mo = pd.DataFrame([["Hora normal", "x"]], columns=["Especificidades Mão de Obra", "Descrição"])
    with pd.ExcelWriter(caminho) as w:
        base.to_excel(w, sheet_name="1. Base", index=False)
        um.to_excel(w, sheet_name="2. Unidades", index=False)
        mo.to_excel(w, sheet_name="3. Mao de Obra", index=False)
    return caminho
