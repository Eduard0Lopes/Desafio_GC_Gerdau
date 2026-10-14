"""Executa o pipeline completo de IA/Dados na ordem correta.

Uso (a partir de IA/): python src/pipeline.py
A etapa de saneamento só roda se data/raw/base_original.xlsx existir; caso contrário,
reutiliza data/processed/base_saneada.xlsx já versionada.
"""
import config
import gerar_catalogo
import gerar_matriz_depara
import migration
import sanitizacao
import testar_banco
import validar_prova


def main():
    if config.BASE_ORIGINAL.exists():
        print("== 1/6 Saneamento"); sanitizacao.main()
    else:
        print(f"== 1/6 Saneamento ignorado ({config.BASE_ORIGINAL.name} ausente; usando base saneada existente)")
    print("== 2/6 Coleções NoSQL"); migration.main()
    print("== 3/6 Matriz De-Para"); gerar_matriz_depara.main()
    print("== 4/6 Catálogo padronizado"); gerar_catalogo.main()
    print("== 5/6 Banco simulado"); testar_banco.main()
    print("== 6/6 Auditoria"); validar_prova.main()


if __name__ == "__main__":
    main()
