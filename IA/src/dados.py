"""Carregamento da base saneada e IDs canônicos compartilhados por todas as etapas."""
import pandas as pd

import config


def carregar_base_saneada() -> pd.DataFrame:
    if not config.BASE_SANEADA.exists():
        raise FileNotFoundError(
            f"{config.BASE_SANEADA} não encontrado. Execute sanitizacao.py com data/raw/base_original.xlsx."
        )
    return pd.read_excel(config.BASE_SANEADA)


def gerar_id(sequencial: int, tipo: str) -> str:
    return str(sequencial).zfill(config.TAMANHO_ID[tipo])


def mapear_por_ordem(valores, tipo: str) -> dict:
    """Atribui IDs sequenciais na ordem da primeira ocorrência (determinístico)."""
    unicos = pd.unique(pd.Series(list(valores)).dropna())
    return {valor: gerar_id(i, tipo) for i, valor in enumerate(unicos, start=1)}


def ids_servico(df: pd.DataFrame) -> dict[str, str]:
    """DescricaoNormalizada -> ID do código de serviço (CS), igual ao ``_id`` no MongoDB."""
    return mapear_por_ordem(df["DescricaoNormalizada"], "cs")
