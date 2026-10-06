# Gerdau Service Code Standardization Project

Sistema de Padronização, Pesquisa Preditiva, Validação Rígida do SAP e Governança de Códigos de Serviço da Gerdau.

---

## 🚀 Como Rodar o Projeto (Docker Compose)

### Pré-requisitos
- Docker e Docker Compose instalados.

### Execução em 1 Passo:
Na raiz da pasta do projeto, execute:

```bash
docker-compose up --build
```

Acesse no seu navegador:
- **Interface Gráfica (Web UI Dark Mode)**: `http://localhost:8000`
- **Documentação Interativa da API (Swagger UI)**: `http://localhost:8000/docs`

---

## 🛠️ Execução Local (Sem Docker)

Caso prefira rodar diretamente no Python:

1. Instale as dependências:
   ```bash
   pip install -r requirements.txt
   ```

2. Inicie o servidor:
   ```bash
   python -m uvicorn app.main:app --reload --port 8000
   ```

3. Abra `http://localhost:8000` no navegador.

---

## 📋 Funcionalidades Implementadas e Testadas

1. **4 Seletores Lado a Lado**:
   - `Grupo de Serviço (G.S)`
   - `Código de Fornecedor (C.F)`
   - `Código de Serviço (C.S)`
   - `Unidade de Medida (U.M)`
2. **Autocompletar e Busca Preditiva em Tempo Real**:
   - Filtragem instantânea conforme a digitação do comprador.
3. **Validação Rígida dos 40 Caracteres do SAP**:
   - Trava visual `[XX/40 Caracteres SAP]` no cadastro com impedimento no servidor.
4. **Motor de Similaridade (*Fuzzy Search / Stemming*)**:
   - Cálculo do *Match %* e acionamento da modal de **Bloqueio por Duplicidade** para itens similares (score >= 90%).
5. **Fluxo de Governança de Dados**:
   - Envio de solicitações com justificativa de negócio obrigatória para validação humana.
