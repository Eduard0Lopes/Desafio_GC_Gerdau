"""Montagem e validacao do ID completo (secao 8): gs(3) + cf(3) + cs(6) + um(2) = 14, sem delimitador."""
import re

LARGURAS = {"gsId": 3, "cfId": 3, "csId": 6, "umId": 2}
TAM_ID_COMPLETO = sum(LARGURAS.values())


def montar_id_completo(gs: str, cf: str, cs: str, um: str) -> str:
    for nome, v in zip(LARGURAS, (gs, cf, cs, um)):
        if not (isinstance(v, str) and re.fullmatch(rf"\d{{{LARGURAS[nome]}}}", v)):
            raise ValueError(f"{nome} invalido: {v!r} (esperado {LARGURAS[nome]} digitos)")
    return gs + cf + cs + um


def separar_id_completo(idc: str) -> dict:
    """Uso diagnostico/validacao. O documento recomenda NAO depender de parsing em operacao."""
    if not re.fullmatch(rf"\d{{{TAM_ID_COMPLETO}}}", idc):
        raise ValueError(f"idCompleto invalido: {idc!r}")
    out, pos = {}, 0
    for k, w in LARGURAS.items():
        out[k] = idc[pos:pos + w]; pos += w
    return out


def formatar_id(seq: int, largura: int) -> str:
    if seq < 1 or len(str(seq)) > largura:
        raise OverflowError(f"sequencia {seq} nao cabe em {largura} digitos")
    return f"{seq:0{largura}d}"


def contadores(n_gs: int, n_cf: int, n_cs: int, n_um: int) -> list:
    """Documentos da colecao 'contadores': ultima sequencia usada. IDs nunca sao reutilizados (8.2)."""
    return [{"_id": k, "seq": n, "largura": LARGURAS[k + "Id"]}
            for k, n in (("gs", n_gs), ("cf", n_cf), ("cs", n_cs), ("um", n_um))]
