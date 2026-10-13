import pandas as pd
import unicodedata
import re
import os

def carregar_e_sanear_base(caminho_excel):
    # 1. Carregar a base anonimizada
    df = pd.read_excel(caminho_excel, sheet_name='1. Base Anonimizada')
    print(f"Total de registros carregados: {len(df)}")
    
    # 2. Função de normalização textual (minúsculas, sem acentos, sem pontuações irrelevantes)
    def normalizar_texto(texto_original):
        if pd.isna(texto_original):
            return {"original": "", "normalizada": "", "tamanho": 0}
        
        texto = str(texto_original).lower()
        
        # Remove acentos (Unicode NFD)
        nfd_form = unicodedata.normalize('NFD', texto)
        texto_sem_acento = "".join([c for c in nfd_form if unicodedata.category(c) != 'Mn'])
        
        # Remove caracteres especiais e espaços duplicados
        texto_limpo = re.sub(r'[^a-z0-9\s]', ' ', texto_sem_acento)
        texto_final = re.sub(r'\s+', ' ', texto_limpo).strip()
        
        return {
            "original": texto_original,
            "normalizada": texto_final,
            "tamanho": len(str(texto_original))
        }

    # Aplicar normalização
    resultados = df['Descrição do Item'].apply(normalizar_texto)
    df['DescricaoOriginal'] = [r['original'] for r in resultados]
    df['DescricaoNormalizada'] = [r['normalizada'] for r in resultados]
    df['QuantidadeCaracteres'] = [r['tamanho'] for r in resultados]
    
    # 3. Validar limite de 40 caracteres do SAP (Ajustado para >= 40 para capturar itens no limite/truncados)
    df['AlertaSAP'] = df['QuantidadeCaracteres'] >= 40
    
    # 4. Identificar duplicidades exatas na base normalizada
    df['DuplicadaExata'] = df.duplicated(subset=['DescricaoNormalizada'], keep=False)
    
    print(f"Itens com alerta de limite do SAP (>= 40 caracteres): {df['AlertaSAP'].sum()}")
    print(f"Itens identificados como duplicados exatos: {df['DuplicadaExata'].sum()}")
    
    return df

if __name__ == "__main__":
    # Garantir que a pasta de destino existe
    os.makedirs('data/processed', exist_ok=True)
    
    # Execução apontando para o arquivo correto da Gerdau
    caminho_excel = os.path.abspath('data/raw/base_original.xlsx')
    
    df_saneado = carregar_e_sanear_base(caminho_excel)
    
    # Salvar em formato Excel (.xlsx) de forma organizada
    output_path = 'data/processed/base_saneada.xlsx'
    df_saneado.to_excel(output_path, index=False)
    print(f"Base saneada ajustada e salva com sucesso em {output_path}!")