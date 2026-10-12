import pandas as pd
import json
import os

def gerar_documentos_nosql():
    # Caminho exato do arquivo Excel e da base saneada
    caminho_excel = os.path.abspath('data/raw/base_original.xlsx')
    df_base = pd.read_csv('data/processed/base_saneada.csv')
    
    xls = pd.ExcelFile(caminho_excel)
    df_um = pd.read_excel(caminho_excel, sheet_name='2. Unidades de Medida')
    
    print("[OK] Dados carregados para modelagem NoSQL.")

    # 2. Criar Coleção: grupos_servico (G.S.)
    grupos_unicos = df_base['Grupo de Serviço'].dropna().unique()
    grupos_servico_docs = []
    for idx, grupo in enumerate(grupos_unicos, start=1):
        gs_id = str(idx).zfill(3)
        grupos_servico_docs.append({
            "_id": gs_id,
            "nome": grupo,
            "nomeNormalizado": str(grupo).lower(),
            "origem": "MIGRACAO"
        })

    # 3. Criar Coleção: codigos_fornecedor (C.F.)
    fornecedores_unicos = df_base['Código do Fornecedor'].dropna().unique()
    codigos_fornecedor_docs = []
    for idx, forn in enumerate(fornecedores_unicos, start=1):
        cf_id = str(idx).zfill(3)
        codigos_fornecedor_docs.append({
            "_id": cf_id,
            "codigoFornecedorSAP": int(forn),
            "nome": f"Fornecedor ID {forn}",
            "nomeNormalizado": f"fornecedor id {forn}",
            "origem": "MIGRACAO"
        })

    # 4. Criar Coleção: unidades_medida (U.M.)
    unidades_medida_docs = []
    for idx, row in df_um.iterrows():
        um_id = str(idx + 1).zfill(2)
        sigla = str(row['Sigla Unidade de Medida']).strip()
        desc = str(row['Descrição']).strip()
        unidades_medida_docs.append({
            "_id": um_id,
            "sigla": sigla,
            "nome": desc,
            "nomeNormalizado": desc.lower(),
            "origem": "MIGRACAO"
        })

    map_gs = {doc["nome"]: doc["_id"] for doc in grupos_servico_docs}
    map_cf = {doc["codigoFornecedorSAP"]: doc["_id"] for doc in codigos_fornecedor_docs}

    # 5. Criar Coleção: codigos_servico (C.S.) e codigos_completos
    codigos_servico_docs = []
    codigos_completos_docs = []
    
    cs_unicas = df_base['DescricaoNormalizada'].unique()
    map_cs = {}
    for idx, desc_norm in enumerate(cs_unicas, start=1):
        cs_id = str(idx).zfill(6)
        map_cs[desc_norm] = cs_id
        
        amostra_row = df_base[df_base['DescricaoNormalizada'] == desc_norm].iloc[0]
        
        codigos_servico_docs.append({
            "_id": cs_id,
            "descricao": amostra_row['DescricaoOriginal'],
            "descricaoNormalizada": desc_norm,
            "quantidadeCaracteres": int(amostra_row['QuantidadeCaracteres']),
            "origem": "MIGRACAO",
            "legacyIds": [str(amostra_row['Código do Serviço'])]
        })

    for _, row in df_base.iterrows():
        gs_id = map_gs.get(row['Grupo de Serviço'], "001")
        cf_id = map_cf.get(row['Código do Fornecedor'], "001")
        cs_id = map_cs.get(row['DescricaoNormalizada'], "000001")
        
        um_sigla = str(row['Unidade medida']).strip()
        um_id = "01"
        for doc_um in unidades_medida_docs:
            if doc_um["sigla"].lower() == um_sigla.lower():
                um_id = doc_um["_id"]
                break

        id_completo = f"{gs_id}{cf_id}{cs_id}{um_id}"
        snapshot = f"{row['Grupo de Serviço']} | Fornecedor {row['Código do Fornecedor']} | {row['Descrição do Item']} | {um_sigla}"

        codigos_completos_docs.append({
            "_id": id_completo,
            "idCompleto": id_completo,
            "gsId": gs_id,
            "cfId": cf_id,
            "csId": cs_id,
            "umId": um_id,
            "descricaoSnapshot": snapshot,
            "usuarioCriacaoId": None,
            "createdAt": None,
            "origem": "MIGRACAO",
            "legacyIds": [str(row['Código do Serviço'])]
        })

    os.makedirs('data/processed/nosql', exist_ok=True)
    
    with open('data/processed/nosql/grupos_servico.json', 'w', encoding='utf-8') as f:
        json.dump(grupos_servico_docs, f, ensure_ascii=False, indent=2)
        
    with open('data/processed/nosql/codigos_fornecedor.json', 'w', encoding='utf-8') as f:
        json.dump(codigos_fornecedor_docs, f, ensure_ascii=False, indent=2)

    with open('data/processed/nosql/unidades_medida.json', 'w', encoding='utf-8') as f:
        json.dump(unidades_medida_docs, f, ensure_ascii=False, indent=2)

    with open('data/processed/nosql/codigos_servico.json', 'w', encoding='utf-8') as f:
        json.dump(codigos_servico_docs, f, ensure_ascii=False, indent=2)

    with open('data/processed/nosql/codigos_completos.json', 'w', encoding='utf-8') as f:
        json.dump(codigos_completos_docs, f, ensure_ascii=False, indent=2)

    print("[OK] Todos os documentos NoSQL foram gerados e salvos em 'data/processed/nosql/' com sucesso!")
    print(f"    - Grupos de Serviço: {len(grupos_servico_docs)}")
    print(f"    - Códigos de Fornecedor: {len(codigos_fornecedor_docs)}")
    print(f"    - Unidades de Medida: {len(unidades_medida_docs)}")
    print(f"    - Códigos de Serviço: {len(codigos_servico_docs)}")
    print(f"    - Códigos Completos: {len(codigos_completos_docs)}")

if __name__ == "__main__":
    gerar_documentos_nosql()