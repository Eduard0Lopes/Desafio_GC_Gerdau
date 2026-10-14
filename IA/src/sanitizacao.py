"""Etapa 1: carrega a base original anonimizada e gera data/processed/base_saneada.xlsx."""
import pandas as pd

import config
from normalizacao import LIMITE_SAP, normalizar_texto


def sanear(df: pd.DataFrame) -> pd.DataFrame:
    df = df.copy()
    df["DescricaoOriginal"] = df["Descrição do Item"].fillna("").astype(str)
    df["DescricaoNormalizada"] = df["DescricaoOriginal"].map(normalizar_texto)
    df["QuantidadeCaracteres"] = df["DescricaoOriginal"].str.len()
    # >= 40 captura também descrições que bateram no limite e provavelmente foram truncadas no SAP.
    df["AlertaSAP"] = df["QuantidadeCaracteres"] >= LIMITE_SAP
    df["DuplicadaExata"] = df.duplicated(subset=["DescricaoNormalizada"], keep=False)
    return df


def main():
    if not config.BASE_ORIGINAL.exists():
        raise FileNotFoundError(f"Base original não encontrada em {config.BASE_ORIGINAL}.")
    df = sanear(pd.read_excel(config.BASE_ORIGINAL, sheet_name=config.ABA_BASE))
    config.PROCESSED.mkdir(parents=True, exist_ok=True)
    df.to_excel(config.BASE_SANEADA, index=False)
    print(f"[OK] {len(df)} registros saneados -> {config.BASE_SANEADA}")
    print(f"     Alerta SAP (>= {LIMITE_SAP} caracteres): {int(df['AlertaSAP'].sum())}")
    print(f"     Duplicidades exatas: {int(df['DuplicadaExata'].sum())}")


if __name__ == "__main__":
    main()
