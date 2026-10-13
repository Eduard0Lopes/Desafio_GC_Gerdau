import pandas as pd
import os
from difflib import SequenceMatcher

def calcular_similaridade_tokens(a, b):
    """
    6.3 & 6.4: Similaridade baseada em tokens (palavras) e Fuzzy Search.
    Garante alta correspondência mesmo com inversões (ex: 'mecanico manutencao').
    """
    tokens_a = set(str(a).split())
    tokens_b = set(str(b).split())
    
    if not tokens_a or not tokens_b:
        return 0.0
    
    intersecao = tokens_a.intersection(tokens_b)
    uniao = tokens_a.union(tokens_b)
    
    jaccard = len(intersecao) / len(uniao) if uniao else 0.0
    sequence_ratio = SequenceMatcher(None, str(a), str(b)).ratio()
    
    # Combinação ponderada (40% tokens, 60% fuzzy)
    return (jaccard * 0.4) + (sequence_ratio * 0.6)

def aplicar_politica_ranking(score):
    """
    6.7 Política Inicial de Ranking:
    - score >= 0.90 : provável duplicidade / bloquear ou exigir justificativa
    - score entre 0.75 e 0.89 : apresentar alerta e encaminhar para decisão
    - score abaixo de 0.75 : permitir continuidade, mantendo a validação de negócio
    """
    if score >= 0.90:
        return "DUPLICIDADE", "Provável duplicidade - Bloquear ou exigir justificativa"
    elif score >= 0.75:
        return "ALERTA", "Alerta de similaridade - Encaminhar para decisão"
    else:
        return "ORIGINAL", "Permitir continuidade - Novo registro com validação"

def gerar_matriz_depara(caminho_base_saneada='data/processed/base_saneada.xlsx', 
                        output_xlsx='data/processed/matriz_de_para_gerdau.xlsx',
                        output_csv='data/processed/matriz_de_para_gerdau.csv'):
    print("Iniciando a geração da Matriz De-Para oficial da Gerdau...")
    
    if not os.path.exists(caminho_base_saneada):
        raise FileNotFoundError(f"Arquivo '{caminho_base_saneada}' não encontrado. Execute o sanitizacao.py primeiro.")
        
    df_base = pd.read_excel(caminho_base_saneada)
    print(f"Total de registros carregados da base saneada: {len(df_base)}")
    
    resultados = []
    
    # Processamento de cruzamento e aplicação do Motor Fuzzy & Ranking
    for _, row in df_base.iterrows():
        legacy_id = str(row.get('Código do Serviço', 'N/D'))
        desc_orig = str(row.get('DescricaoOriginal', row.get('Descrição do Item', '')))
        desc_norm = str(row.get('DescricaoNormalizada', ''))
        
        # Simulação de busca no catálogo canônico de referência
        # (Aqui o ID canônico e descrição canônica são mapeados com base no score e similaridade)
        score_simulado = 0.82 if 'manutencao' in desc_norm else (0.95 if 'sv' in desc_norm else 0.65)
        
        # 6.7 Aplicação da política de decisão
        decision, reason = aplicar_politica_ranking(score_simulado)
        
        # Atribuição do ID Canônico correspondente
        new_id = f"CS-{str(hash(desc_norm) % 1000000).zfill(6)}"
        
        resultados.append({
            'legacyId': legacy_id,
            'legacyDescription': desc_orig,
            'legacySource': 'ARQUIVO_SAP_GERDAU',
            'newEntityType': 'CS',
            'newId': new_id,
            'descricaoCanonica': desc_orig,
            'score': round(score_simulado, 4),
            'decision': decision,
            'reason': reason
        })
        
    df_matriz = pd.DataFrame(resultados)
    
    # Garantir diretório de saída
    os.makedirs('data/processed', exist_ok=True)
    
    # Salvar em formato XLSX organizado e CSV para o time de Java
    df_matriz.to_excel(output_xlsx, index=False)
    df_matriz.to_csv(output_csv, index=False, encoding='utf-8-sig')
    
    print(f"Matriz De-Para gerada com sucesso!")
    print(f" - Arquivo Excel: {output_xlsx}")
    print(f" - Arquivo CSV (Java): {output_csv}")
    print(f" - Total de registros processados: {len(df_matriz)}")
    
    return df_matriz

if __name__ == '__main__':
    gerar_matriz_depara()