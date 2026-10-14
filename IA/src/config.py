"""Caminhos e constantes compartilhados pelo pipeline (independentes do diretório atual)."""
from pathlib import Path

RAIZ_IA = Path(__file__).resolve().parent.parent
DATA = RAIZ_IA / "data"
RAW = DATA / "raw"
PROCESSED = DATA / "processed"
REFERENCE = DATA / "reference"
NOSQL = PROCESSED / "nosql"

BASE_ORIGINAL = RAW / "base_original.xlsx"
ABA_BASE = "1. Base Anonimizada"
ABA_UNIDADES = "2. Unidades de Medida"
UNIDADES_REFERENCIA = REFERENCE / "unidades_medida.csv"

BASE_SANEADA = PROCESSED / "base_saneada.xlsx"
CATALOGO = PROCESSED / "catalogo_padronizado_servicos.xlsx"
MATRIZ_XLSX = PROCESSED / "matriz_de_para_gerdau.xlsx"
MATRIZ_CSV = PROCESSED / "matriz_de_para_gerdau.csv"
DB_SIMULADO = PROCESSED / "db_simulado.json"

COLECOES = (
    "grupos_servico",
    "unidades_medida",
    "codigos_fornecedor",
    "codigos_servico",
    "codigos_completos",
)

TAMANHO_ID = {"gs": 3, "cf": 3, "cs": 6, "um": 2}
LEGACY_SOURCE = "ARQUIVO_SAP_GERDAU"
