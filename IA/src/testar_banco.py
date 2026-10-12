import os
import json

def testar_e_popular_banco_local():
    print("=" * 60)
    print("INICIANDO SIMULAÇÃO DO BANCO NOSQL LOCAL - GERDAU")
    print("=" * 60)

    # 1. Diretórios de origem e destino simulados
    caminho_nosql = 'data/processed/nosql/'
    caminho_db_local = 'data/processed/db_simulado.json'
    
    colecoes_arquivos = {
        'grupos_servico': 'grupos_servico.json',
        'codigos_fornecedor': 'codigos_fornecedor.json',
        'unidades_medida': 'unidades_medida.json',
        'codigos_servico': 'codigos_servico.json',
        'codigos_completos': 'codigos_completos.json'
    }

    print("\n[1/3] Consolidando coleções NoSQL em um banco local estruturado...")
    banco_dados = {}
    
    for nome_col, arquivo_json in colecoes_arquivos.items():
        caminho_completo = os.path.join(caminho_nosql, arquivo_json)
        
        if not os.path.exists(caminho_completo):
            print(f"   [AVISO] Arquivo {arquivo_json} não encontrado. Pulando...")
            banco_dados[nome_col] = []
            continue
            
        with open(caminho_completo, 'r', encoding='utf-8') as f:
            dados = json.load(f)
            banco_dados[nome_col] = dados
            print(f"   - Coleção '{nome_col}': {len(dados) if isinstance(dados, list) else 1} documentos carregados.")

    # Salva em um arquivo de banco simulado unificado
    os.makedirs(os.path.dirname(caminho_db_local), exist_ok=True)
    with open(caminho_db_local, 'w', encoding='utf-8') as f:
        json.dump(banco_dados, f, ensure_ascii=False, indent=4)
    print(f"   [SUCESSO] Banco NoSQL salvo localmente em: {caminho_db_local}")

    print("\n[2/3] Validação de Consulta no Banco Local:")
    total_completos = len(banco_dados.get('codigos_completos', []))
    print(f"   - Total de registros na coleção canônica 'codigos_completos': {total_completos}")
    
    # Amostra de consulta textual simulada
    amostra = banco_dados.get('codigos_completos', [])
    if amostra:
        primeiro_item = amostra[0]
        print(f"   - Exemplo de documento recuperado (ID): {primeiro_item.get('_id')}")
        print(f"   - Descrição Snapshot: {primeiro_item.get('descricaoSnapshot')}")

    print("\n[3/3] Integração Pronta para a Trilha Java:")
    print("   - Os dados saneados estão estruturados e prontos para consumo.")

    print("\n" + "=" * 60)
    print("PROCESSO CONCLUÍDO COM SUCESSO! BANCO LOCAL DA GERDAU ATIVO.")
    print("=" * 60)

if __name__ == "__main__":
    testar_e_popular_banco_local()