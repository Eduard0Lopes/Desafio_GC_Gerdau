import json
from pathlib import Path

import pytest

from fuzzy_engine import IndiceSimilaridade, similaridade
from ranking_policy import classificar

CASOS = json.loads((Path(__file__).parent / "fixtures" / "similaridade_casos.json").read_text(encoding="utf-8"))


@pytest.mark.parametrize("caso", CASOS, ids=lambda c: f"{c['a']}|{c['b']}")
def test_similaridade_igual_a_fixture_compartilhada_com_java(caso):
    assert similaridade(caso["a"], caso["b"]) == pytest.approx(caso["score"], abs=1e-12)
    assert classificar(caso["score"])[0] == caso["status"]


def test_politica_de_ranking_nos_limiares():
    assert classificar(0.88)[0] == "DUPLICIDADE"
    assert classificar(0.8799)[0] == "ALERTA"
    assert classificar(0.70)[0] == "ALERTA"
    assert classificar(0.6999)[0] == "ORIGINAL"


def test_indice_encontra_o_mais_similar_e_ignora_o_proprio():
    itens = {"1": "manutencao mecanica", "2": "mecanica manutencao", "3": "pintura predial"}
    indice = IndiceSimilaridade(itens)
    chave, score = indice.mais_similar("manutencao mecanica", ignorar="1")
    assert chave == "2"
    assert score == pytest.approx(similaridade("manutencao mecanica", "mecanica manutencao"))
