import pandas as pd

def gerar_matriz_depara():
    # 1. Carregar a base saneada gerada no passo anterior
    caminho_base = 'data/processed/base_saneada.csv'
    df = pd.read_csv(caminho_base)
    
    print(f"[{'OK'}] Base saneada carregada com {len(df)} registros.")
    
    # 2. Criar IDs técnicos estáveis para cada DescricaoNormalizada única (Chave Canônica)
    # Conforme o documento, IDs padronizados facilitam consultas e relatórios (ex: CS-000001)
    descricoes_unicas = df['DescricaoNormalizada'].unique()
    mapeamento_canonico = {desc: f"CS-{str(idx+1).zfill(6)}" for idx, desc in enumerate(descricoes_unicas)}
    
    # Atribuir o ID padronizado a cada linha do DataFrame
    df['NewId'] = df['DescricaoNormalizada'].map(mapeamento_canonico)
    
    # 3. Construir a Matriz De-Para (ligando o legado ao novo padrão)
    matriz_depara = []
    for _, row in df.iterrows():
        matriz_depara.append({
            "legacyId": row['Código do Serviço'],
            "legacyDescription": row['Descrição do Item'],
            "legacySource": "ARQUIVO_SAP_GERDAU",
            "newEntityType": "CS",
            "newId": row['NewId'],
            "descricaoCanonica": row['DescricaoOriginal'],
            "decision": "CONSOLIDADO" if row['DuplicadaExata'] else "ORIGINAL",
            "reason": "Duplicidade exata tratada e consolidada" if row['DuplicadaExata'] else "Registro exclusivo padronizado"
        })
        
    df_depara = pd.DataFrame(matriz_depara)
    
    # 4. Salvar o resultado na pasta de processados
    caminho_depara = 'data/processed/matriz_de_para.csv'
    df_depara.to_csv(caminho_depara, index=False)
    
    print(f"[{'OK'}] Matriz De-Para gerada com sucesso!")
    print(f"    - Total de registros na matriz: {len(df_depara)}")
    print(f"    - Arquivo salvo em: {caminho_depara}")
    
    # Exibir uma amostra da Matriz De-Para
    print("\nAmostra da Matriz De-Para gerada:")
    print(df_depara[['legacyId', 'legacyDescription', 'newId', 'decision']].head(5))
    
    return df_depara

if __name__ == "__main__":
    gerar_matriz_depara()