"""Executor minimo (sem pytest): python tests/rodar_testes.py"""
import sys, tempfile, traceback
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent)); sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
import test_normalizador, test_pipeline, test_modulos, test_busca, test_avaliacao

falhas = total = 0
for mod in (test_normalizador, test_modulos, test_busca, test_avaliacao, test_pipeline):
    for nome in sorted(n for n in dir(mod) if n.startswith("test_")):
        fn = getattr(mod, nome); total += 1
        try:
            if "tmp_path" in fn.__code__.co_varnames[:fn.__code__.co_argcount]:
                with tempfile.TemporaryDirectory() as t: fn(Path(t))
            else: fn()
            print("OK  ", mod.__name__, nome)
        except Exception:
            falhas += 1; print("FALHA", mod.__name__, nome); traceback.print_exc()
print(f"\n{total} testes: {'TUDO OK' if not falhas else str(falhas) + ' FALHA(S)'}")
sys.exit(1 if falhas else 0)
