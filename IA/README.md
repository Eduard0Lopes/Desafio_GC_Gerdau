# Desafio GC 2026 — IA & Dados

## Objetivo
Apoiar a identificação de códigos de serviço duplicados ou semelhantes e a padronização dos registros, considerando os componentes **G.S.**, **C.F.**, **C.S.** e **U.M.**, além do limite de 40 caracteres do SAP.

## Tecnologias
- Python
- Pandas e OpenPyXL
- Jupyter Notebook
- Git e GitHub

## Estrutura do projeto
```text
.
├── dados/
│   ├── original/
│   └── processada/
├── notebooks/
├── src/
├── resultados/
├── requirements.txt
└── README.md
```

## Executar o projeto
Ative o ambiente virtual e instale as dependências:

```powershell
.\.venv\Scripts\Activate.ps1
python -m pip install -r requirements.txt
```

## Próximo passo
Analisar a base oficial para identificar colunas, dados ausentes, duplicidades e padrões antes de implementar a busca por similaridade.


