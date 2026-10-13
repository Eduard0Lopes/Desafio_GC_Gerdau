import pandas as pd
import os
from fuzzy_engine import FuzzyEngine
from ranking_policy import RankingPolicy

def executar_motor_busca(caminho_base_saneada='data/processed/base_saneada.csv', 
                         caminho_catalogo='data/processed/catalogo_padronizado_servicos.xlsx', 
                         output_path='data/processed/resultado_motor_ranking.csv'):
    print("Carregando bases para o Motor de Busca Inteligente...")
    
    if not os.path.exists(caminho_base_saneada):
        raise FileNotFoundError(f"Arquivo base saneada não encontrado em: {caminho_base_saneada}. Execute o sanitizacao.py primeiro.")
    if not os.path.exists(caminho_catalogo):
        raise FileNotFoundError(f"Catálogo padronizado não encontrado em: {caminho_catalogo}.")
        
    # Leitura com fallback de encoding para evitar erros de acentuação (Windows)
    try:
        df_base = pd.read_csv(caminho_base_saneada, encoding='utf-8-sig')
    except Exception:
        df_base = pd.read_csv(caminho_base_saneada, encoding='latin-1')
        
    if caminho_catalogo.endswith('.xlsx'):
        df_catalogo = pd.read_excel(caminho_catalogo)
    else:
        try:
            df_catalogo = pd.read_csv(caminho_catalogo, encoding='utf-8-sig')
        except Exception:
            df_catalogo = pd.read_csv(caminho_catalogo, encoding='latin-1')
    
    resultados = []
    
    print(f"Processando {len(df_base)} registros contra o catálogo com Fuzzy, Tokens e Ranking...")
    
    for _, row in df_base.iterrows():
        # Captura os dados da base saneada
        legacy_id = str(row.get('Código do Serviço', row.get('legacyId', 'N/D')))
        desc_orig = str(row.get('DescricaoOriginal', row.get('Descrição do Item', '')))
        desc_norm = str(row.get('DescricaoNormalizada', ''))
        
        candidatos = []
        
        for _, c_row in df_catalogo.iterrows():
            canonical_id = str(c_row.get('ID_Canônico_CS', c_row.get('newId', 'N/D')))
            canonical_desc_orig = str(c_row.get('Descricao_Padronizada', c_row.get('descricaoCanonica', '')))
            canonical_desc_norm = str(c_row.get('Descricao_Normalizada', canonical_desc_orig))
            
            # 6.6 Índice por ID: Correspondência exata de ID tem prioridade máxima (score 1.0)
            score_id = 1.0 if legacy_id == canonical_id else 0.0
            
            # Similaridade textual por tokens e fuzzy
            score_texto = FuzzyEngine.calcular_similaridade_tokens(desc_norm, canonical_desc_norm)
            
            # Se o ID bater exatamente, o score final é 1.0; caso contrário, vale a similaridade de texto
            score_final = 1.0 if score_id == 1.0 else score_texto
            
            candidatos.append({
                'canonical_id': canonical_id,
                'canonical_desc': canonical_desc_orig,
                'score': score_final
            })
            
        # 6.7 Ordenação do Ranking (maior score primeiro)
        candidatos.sort(key=lambda x: x['score'], reverse=True)
        
        # Seleciona o melhor match (Top 1)
        top_1 = candidatos[0] if candidatos else {'canonical_id': '', 'canonical_desc': '', 'score': 0.0}
        
        # Aplica a política baseada nos limiares (0.90 / 0.75)
        status_ranking, acao_recomendada = RankingPolicy.aplicar_politica_ranking(top_1['score'])
        
        resultados.append({
            'Legacy_ID': legacy_id,
            'DescricaoOriginal': desc_orig,
            'DescricaoNormalizada': desc_norm,
            'MatchCanonical_ID': top_1['canonical_id'],
            'MatchCanonical_Descricao': top_1['canonical_desc'],
            'ScoreSimilaridade': round(top_1['score'], 4),
            'StatusRanking': status_ranking,
            'AcaoRecomendada': acao_recomendada
        })
        
    df_resultado = pd.DataFrame(resultados)
    
    # Salvar na pasta processed para o time de Java consumir
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    df_resultado.to_csv(output_path, index=False, encoding='utf-8-sig')
    
    print(f"Motor executado com sucesso! Resultado salvo em: {output_path}")
    print(f" - {len(df_resultado[df_resultado['StatusRanking'] == 'PROVAVEL_DUPLICIDADE'])} itens com provável duplicidade (>= 0.90).")
    print(f" - {len(df_resultado[df_resultado['StatusRanking'] == 'ALERTA_SIMILARIDADE'])} itens com alerta (0.75 a 0.89).")
    print(f" - {len(df_resultado[df_resultado['StatusRanking'] == 'NOVO_REGISTRO'])} itens permitidos (< 0.75).")
    
    return df_resultado

if __name__ == '__main__':
    executar_motor_busca()