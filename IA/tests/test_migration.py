import pandas as pd
import pytest

from migration import gerar_documentos


@pytest.fixture
def base():
    return pd.DataFrame({
        "Grupo de Serviço": ["Civil", "Civil", "Elétrica", "Civil"],
        "Código do Fornecedor": [111, 111, 222, 111],
        "Código do Serviço": [10, 11, 12, 13],
        "DescricaoOriginal": ["SV PINTURA", "SV-PINTURA", "SV CABO", "SV PINTURA"],
        "DescricaoNormalizada": ["sv pintura", "sv pintura", "sv cabo", "sv pintura"],
        "Unidade medida": ["SV", "SV", "M", "SV"],
    })


@pytest.fixture
def unidades():
    return pd.DataFrame({"Sigla Unidade de Medida": ["SV", "M"], "Descrição": ["Serviço", "Metro"]})


def test_consolida_ids_compostos_repetidos_em_legacy_ids(base, unidades):
    docs = gerar_documentos(base, unidades)
    completos = docs["codigos_completos"]
    assert [c["_id"] for c in completos] == ["00100100000101", "00200200000202"]
    assert completos[0]["legacyIds"] == ["10", "11", "13"]
    assert completos[0]["descricaoSnapshot"] == "Civil | Fornecedor 111 | SV PINTURA | SV"


def test_ids_sequenciais_e_nomes_normalizados(base, unidades):
    docs = gerar_documentos(base, unidades)
    assert [s["_id"] for s in docs["codigos_servico"]] == ["000001", "000002"]
    assert docs["codigos_servico"][0]["legacyIds"] == ["10", "11", "13"]
    assert docs["unidades_medida"][0] == {"_id": "01", "sigla": "SV", "nome": "Serviço",
                                          "nomeNormalizado": "servico", "origem": "MIGRACAO"}
    assert docs["grupos_servico"][1]["nomeNormalizado"] == "eletrica"


def test_unidade_desconhecida_falha(base, unidades):
    base.loc[0, "Unidade medida"] = "XYZ"
    with pytest.raises(ValueError, match="XYZ"):
        gerar_documentos(base, unidades)
