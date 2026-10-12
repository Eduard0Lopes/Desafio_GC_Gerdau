# Projeto Gerdau — Saneamento de Dados e Catálogo Padronizado de Serviços

Este repositório contém a solução desenvolvida para a trilha de **IA & Dados** do Desafio Gerdau, focada no saneamento, padronização e estruturação dos códigos de serviço contratuais.

## 📋 Visão Geral do Problema
O cadastro descentralizado de serviços no SAP gerou milhares de registros redundantes, divergentes e com o limite de 40 caracteres mal aproveitado. A ausência de um padrão unificava serviços idênticos sob descrições e códigos diferentes, dificultando a análise de gastos (*Spend Analysis*) e a negociação por volume.

## 🛠️ Solução Implementada
1. **Saneamento Textual:** Normalização de descrições (remoção de acentos, padronização de maiúsculas/minúsculas e pontuações).
2. **Matriz De-Para:** Mapeamento unívoco entre os códigos legados e os novos identificadores canônicos (`CS-000001`).
3. **Deduplicação Inteligente:** Separação entre registros exclusivos (`ORIGINAL`) e duplicidades exatas tratadas (`CONSOLIDADO`).
4. **Catálogo Padronizado XLSX:** Exportação de uma planilha corporativa estruturada, estilizada e pronta para consumo.
5. **Modelagem NoSQL:** Estruturação em coleções JSON para integração futura com API e motores de busca (*Fuzzy Search*).

---

## 📁 Estrutura de Arquivos
- `gerar_catalogo_xlsx.py` — Script principal em Python que executa todo o pipeline de saneamento, agrupamento e formatação da planilha.
- `requirements.txt` — Lista de dependências e bibliotecas necessárias.
- `.gitignore` — Configuração para exclusão de dados brutos sensíveis e arquivos temporários.
- `data/processed/catalogo_padronizado_servicos.xlsx` — Catálogo final gerado e formatado.

---

## 🚀 Como Executar o Projeto

1. **Pré-requisitos:**
   Certifique-se de ter o Python 3.8+ instalado em sua máquina.

2. **Instalar Dependências:**
   No terminal integrado do VS Code, execute:
   ```bash
   pip install -r requirements.txt
   ```

3. **Executar o Pipeline:**
   Certifique-se de que a base original (`base_original.xlsx`) está na raiz do projeto e execute:
   ```bash
   python gerar_catalogo_xlsx.py
   ```

4. **Resultado:**
   O script gerará a planilha formatada em `data/processed/catalogo_padronizado_servicos.xlsx` e os arquivos de suporte na pasta de processados.
