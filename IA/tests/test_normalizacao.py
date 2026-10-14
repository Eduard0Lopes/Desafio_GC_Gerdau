import pytest

from normalizacao import normalizar_texto


@pytest.mark.parametrize("entrada, esperado", [
    ("SV-EQUIPE TELHADO", "sv equipe telhado"),
    ("Manutenção  Mecânica!!", "manutencao mecanica"),
    ("  Pavimentação/Segurança (NR-10) ", "pavimentacao seguranca nr 10"),
    ("Centímetro cúbico", "centimetro cubico"),
    ("", ""),
    (None, ""),
    (float("nan"), ""),
])
def test_normalizar_texto(entrada, esperado):
    assert normalizar_texto(entrada) == esperado
