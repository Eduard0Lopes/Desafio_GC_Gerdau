"""Stemmer leve para portugues (secao 6.4 do documento).

Aproxima variacoes morfologicas (manutencao/manutencoes, mecanico/mecanica, eletricista/eletrica).
USO: recall de BUSCA (autocomplete/ranking). NAO e usado para fundir itens: para deduplicar,
'mecanica' e 'mecanico' sao itens diferentes e quem decide e a similaridade com suas guardas.
Entrada: palavra ja sem acento e em minusculas (saida de normalizador.limpar).
"""
_PLURAL = (("coes", "cao"), ("caes", "cao"), ("oes", "ao"), ("aes", "ao"), ("ais", "al"),
           ("eis", "el"), ("ois", "ol"), ("ores", "or"), ("res", "r"), ("ns", "m"))
_FEMININO = (("eira", "eiro"), ("ora", "or"), ("esa", "es"))
_SUFIXOS = ("amente", "mente", "amento", "imento", "mento", "acao", "icao", "ucao", "cao", "sao",
            "agem", "idade", "ismo", "ista", "avel", "ivel", "vel", "dor", "tor", "ico", "ica", "oso", "osa")
_MIN = 3


def stem(p: str) -> str:
    if len(p) <= 3 or not p.isalpha():
        return p
    for suf, rep in _PLURAL:                      # 1) plural
        if p.endswith(suf) and len(p) - len(suf) >= 2:
            p = p[: -len(suf)] + rep; break
    else:
        if p.endswith("s") and not p.endswith(("ss", "us", "is")) and len(p) > 4:
            p = p[:-1]
    for suf, rep in _FEMININO:                     # 2) feminino
        if p.endswith(suf) and len(p) - len(suf) >= 3:
            p = p[: -len(suf)] + rep; break
    for suf in _SUFIXOS:                           # 3) sufixos derivacionais
        if p.endswith(suf) and len(p) - len(suf) >= _MIN + 1:
            p = p[: -len(suf)]; break
    if len(p) > 4 and p[-1] in "aeo":              # 4) vogal final
        p = p[:-1]
    return p


def stem_texto(texto: str) -> list:
    return [stem(t) for t in texto.split()]
