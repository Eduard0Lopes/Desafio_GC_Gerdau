import os
import pandas as pd
import unicodedata
import re
import openpyxl
from openpyxl.styles import Font, PatternFill, Alignment, Border, Side
from openpyxl.utils import get_column_letter

def normalizar_texto(texto):
    if pd.isna(texto):
        return ""
    nfkd = unicodedata.normalize('NFKD', str(texto))
    sem_acento = "".join([c for c in nfkd if not unicodedata.combining(c)])
    limpo = re.sub(r'[^a-zA-Z0-9\s]', ' ', sem_acento)
    limpo = re.sub(r'\s+', ' ', limpo).strip().lower()
    return limpo

def gerar_planilha_formatada():
    print("=" * 60)
    print("GERANDO CATÁLOGO PADRONIZADO EM FORMATO XLSX - GERDAU")
    print("=" * 60)

    # 1. Carregar base original
    caminho_excel = 'data/raw/base_original.xlsx'
    if not os.path.exists(caminho_excel):
        print(f"[ERRO] Arquivo {caminho_excel} não encontrado.")
        return

    df = pd.read_excel(caminho_excel, sheet_name='1. Base Anonimizada')
    
    # 2. Normalizar e agrupar
    df['DescricaoNormalizada'] = df['Descrição do Item'].apply(normalizar_texto)
    df['QuantidadeCaracteres'] = df['Descrição do Item'].astype(str).str.len()

    catalogo_unicos = df.groupby('DescricaoNormalizada').agg({
        'Grupo de Serviço': 'first',
        'Descrição do Item': 'first',
        'Unidade medida': 'first',
        'QuantidadeCaracteres': 'first',
        'Código do Serviço': lambda x: list(x)
    }).reset_index()

    catalogo_unicos['ID_Canônico_CS'] = [f"CS-{str(i+1).zfill(6)}" for i in range(len(catalogo_unicos))]
    catalogo_unicos['Total_Ocorrencias_Legado'] = catalogo_unicos['Código do Serviço'].apply(len)
    catalogo_unicos['Codigos_Legados_Associados'] = catalogo_unicos['Código do Serviço'].apply(lambda l: ", ".join(map(str, l[:5])) + ("..." if len(l)>5 else ""))

    catalogo_export = catalogo_unicos[[
        'ID_Canônico_CS', 'Grupo de Serviço', 'Descrição do Item', 
        'DescricaoNormalizada', 'Unidade medida', 'QuantidadeCaracteres', 
        'Total_Ocorrencias_Legado', 'Codigos_Legados_Associados'
    ]]
    catalogo_export.columns = [
        'ID_Canônico_CS', 'Grupo_Servico', 'Descricao_Padronizada', 
        'Descricao_Normalizada', 'Unidade_Medida', 'Qtd_Caracteres', 
        'Total_Ocorrencias_Legado', 'Codigos_Legados_Associados'
    ]

    # 3. Construção da Planilha com Openpyxl
    wb = openpyxl.Workbook()
    ws = wb.active
    ws.title = "Catálogo Padronizado"
    ws.views.sheetView[0].showGridLines = True

    header_fill = PatternFill(start_color="34495E", end_color="34495E", fill_type="solid")
    header_font = Font(name="Arial", size=11, bold=True, color="FFFFFF")
    row_font = Font(name="Arial", size=10)
    zebra_fill = PatternFill(start_color="F8F9F9", end_color="F8F9F9", fill_type="solid")

    thin_border = Border(
        left=Side(style='thin', color='D5DBDB'), right=Side(style='thin', color='D5DBDB'),
        top=Side(style='thin', color='D5DBDB'), bottom=Side(style='thin', color='D5DBDB')
    )

    align_center = Alignment(horizontal='center', vertical='center')
    align_left = Alignment(horizontal='left', vertical='center')

    # Título do Relatório
    ws.merge_cells('A1:H1')
    title_cell = ws['A1']
    title_cell.value = "GERDAU — Catálogo Padronizado de Serviços (Saneado)"
    title_cell.font = Font(name="Arial", size=14, bold=True, color="2C3E50")
    title_cell.alignment = Alignment(horizontal='left', vertical='center')
    ws.row_dimensions[1].height = 30

    ws.merge_cells('A2:H2')
    subtitle_cell = ws['A2']
    subtitle_cell.value = f"Total de Itens Canônicos Únicos: {len(catalogo_export)} | Base Saneada e Deduplicada"
    subtitle_cell.font = Font(name="Arial", size=10, italic=True, color="7F8C8D")
    subtitle_cell.alignment = Alignment(horizontal='left', vertical='center')
    ws.row_dimensions[2].height = 20

    headers = [
        "ID Canônico", "Grupo de Serviço", "Descrição Padronizada", 
        "Descrição Normalizada", "Unidade de Medida", "Qtd Caracteres", 
        "Total Ocorrências", "Códigos Legados Associados"
    ]

    header_row_idx = 4
    ws.row_dimensions[header_row_idx].height = 25

    for col_idx, header_text in enumerate(headers, 1):
        cell = ws.cell(row=header_row_idx, column=col_idx, value=header_text)
        cell.fill = header_fill
        cell.font = header_font
        cell.alignment = align_center
        cell.border = thin_border

    for row_idx, row_data in enumerate(catalogo_export.values, start=5):
        ws.row_dimensions[row_idx].height = 20
        is_zebra = (row_idx % 2 == 0)
        
        for col_idx, val in enumerate(row_data, 1):
            cell = ws.cell(row=row_idx, column=col_idx, value=val)
            cell.font = row_font
            cell.border = thin_border
            if is_zebra:
                cell.fill = zebra_fill
                
            if col_idx in [1, 5, 6, 7]:
                cell.alignment = align_center
                if col_idx in [6, 7]:
                    cell.number_format = '#,##0'
            else:
                cell.alignment = align_left

    # Larguras personalizadas
    ws.column_dimensions['A'].width = 16
    ws.column_dimensions['B'].width = 18
    ws.column_dimensions['C'].width = 45
    ws.column_dimensions['D'].width = 35
    ws.column_dimensions['E'].width = 18
    ws.column_dimensions['F'].width = 16
    ws.column_dimensions['G'].width = 18
    ws.column_dimensions['H'].width = 30

    ws.freeze_panes = 'A5'

    os.makedirs('data/processed', exist_ok=True)
    output_xlsx = 'data/processed/catalogo_padronizado_servicos.xlsx'
    wb.save(output_xlsx)
    print(f"[SUCESSO] Planilha formatada salva em: {output_xlsx}")

if __name__ == "__main__":
    gerar_planilha_formatada()