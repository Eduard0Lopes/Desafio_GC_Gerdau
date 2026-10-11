"""Etapa 2 do saneamento e secao 9.2: copia imutavel da origem + metadados do lote de importacao."""
import datetime as dt
import getpass
import hashlib
import os
import shutil
import stat
from pathlib import Path


def sha256_arquivo(caminho) -> str:
    h = hashlib.sha256()
    with open(caminho, "rb") as f:
        for bloco in iter(lambda: f.read(1 << 20), b""):
            h.update(bloco)
    return h.hexdigest()


def abrir_lote(xlsx, saida, versao_algoritmo: str, versao_dicionario: str, processo: str = None) -> dict:
    """Copia o arquivo para saida/origem/ (somente leitura), calcula o hash e devolve o manifesto base."""
    xlsx, saida = Path(xlsx), Path(saida)
    agora = dt.datetime.now(dt.timezone.utc)
    h = sha256_arquivo(xlsx)
    destino_dir = saida / "origem"; destino_dir.mkdir(parents=True, exist_ok=True)
    destino = destino_dir / f"{h[:12]}_{xlsx.name}"
    if destino.exists():
        os.chmod(destino, stat.S_IWRITE | stat.S_IREAD)
    shutil.copy2(xlsx, destino)
    os.chmod(destino, stat.S_IREAD | stat.S_IRGRP | stat.S_IROTH)       # imutavel: so leitura
    assert sha256_arquivo(destino) == h, "copia de origem nao confere com o hash"
    return {
        "_id": f"L{agora.strftime('%Y%m%dT%H%M%SZ')}-{h[:8]}",
        "arquivoOrigem": xlsx.name, "copiaImutavel": str(destino.relative_to(saida)),
        "tamanhoBytes": xlsx.stat().st_size, "sha256": h,
        "dataImportacao": agora.isoformat(timespec="seconds"),
        "versaoAlgoritmoNormalizacao": versao_algoritmo, "versaoDicionario": versao_dicionario,
        "usuarioProcesso": processo or f"pipeline:{getpass.getuser()}",
        "status": "EM_PROCESSAMENTO", "resultadoValidacao": None, "metricas": None,
    }
