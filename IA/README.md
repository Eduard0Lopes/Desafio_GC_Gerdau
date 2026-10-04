# Desafio GC 2026 — IA & Dados

## 1. Sobre o projeto

O projeto busca apoiar a identificação de códigos de serviço duplicados ou semelhantes, facilitando a consulta e a padronização dos registros utilizados pela empresa.

Atualmente, a criação manual desses códigos pode gerar problemas como:

* Cadastro de serviços que já existem;
* Criação de vários códigos para serviços com descrições iguais ou semelhantes;
* Diferenças na escrita das descrições;
* Dificuldade para encontrar um serviço já cadastrado;
* Descrições que ultrapassam o limite de 40 caracteres do SAP.

A proposta é utilizar Python e técnicas de análise de dados para localizar serviços semelhantes e apresentar possíveis correspondências antes que um novo código seja criado.

**Importante:** a ferramenta deve sugerir candidatos para análise, e não considerar automaticamente que duas descrições semelhantes representam o mesmo serviço.

## 2. Objetivo

Desenvolver uma solução que ajude os compradores a encontrar serviços existentes e identificar possíveis duplicidades, considerando quatro componentes:

* **G.S. — Grupo de Serviço**
* **C.F. — Código do Fornecedor**
* **C.S. — Código do Serviço**
* **U.M. — Unidade de medida**

A descrição do item também será utilizada na busca. O limite de 40 caracteres do SAP será considerado na etapa de padronização das descrições.

## 3. Tecnologias utilizadas

* **Python:** desenvolvimento da lógica de análise e busca;
* **Pandas:** manipulação e análise da base de dados;
* **OpenPyXL:** leitura e processamento de arquivos Excel;
* **RapidFuzz:** comparação de similaridade entre textos;
* **Jupyter Notebook:** exploração dos dados e testes;
* **Git e GitHub:** versionamento e organização do projeto.

## 4. Estrutura do projeto

```text
.
├── dados/
│   ├── original/
│   └── processada/
├── notebooks/
│   └── 01_analise_exploratoria.ipynb
├── src/
├── resultados/
├── requirements.txt
└── README.md
```

* `dados/original/`: arquivos originais, preservados sem alterações.
* `dados/processada/`: dados preparados para análise, quando necessário.
* `notebooks/`: análises exploratórias e experimentos.
* `src/`: funções e módulos reutilizáveis da aplicação.
* `resultados/`: relatórios e resultados gerados pelas análises.
* `requirements.txt`: dependências Python do projeto.

## 5. Análise exploratória da base

A primeira análise foi realizada sobre uma base com **9.795 registros**.

| Indicador                                                                                            | Resultado |
| ---------------------------------------------------------------------------------------------------- | --------: |
| Registros analisados                                                                                 |     9.795 |
| Descrições originais distintas                                                                       |     7.468 |
| Descrições distintas após normalização                                                               |     7.431 |
| Códigos de serviço distintos                                                                         |     7.937 |
| Códigos presentes em mais de um registro                                                             |     1.234 |
| Grupos com uma mesma descrição normalizada, mesmo grupo, fornecedor e unidade, mas múltiplos códigos |       341 |

A base analisada não apresentou valores ausentes nas cinco colunas utilizadas.

Esses indicadores ajudam a identificar situações que merecem investigação. Entretanto, registros repetidos não representam necessariamente erros: um mesmo código pode aparecer em vários registros, e descrições iguais com códigos diferentes precisam ser avaliadas conforme as regras do negócio.

## 6. O que já foi desenvolvido

### Normalização de descrições

Foi criada uma função para padronizar textos antes da comparação. Ela:

* Converte letras para minúsculas;
* Remove acentos;
* Elimina espaços excedentes;
* Preserva pontuação, sinais e números.

A normalização reduziu de 7.468 para 7.431 a quantidade de descrições distintas. Isso indica que algumas diferenças de escrita eram apenas variações de acentuação, maiúsculas ou espaços.

### Busca por similaridade

Foi utilizada a biblioteca RapidFuzz para encontrar descrições textualmente semelhantes, mesmo quando a escrita não é idêntica.

A busca pode considerar filtros de contexto, como grupo de serviço, fornecedor e unidade de medida, para tornar os resultados mais relevantes.

### Comparação numérica

Foi implementada uma análise dos números presentes nas descrições.

Essa etapa é importante porque textos parecidos podem representar serviços diferentes. Por exemplo, um material de 0,5 mm não deve ser considerado equivalente a outro de 0,65 mm apenas porque as descrições são semelhantes.

### Classificação inicial dos resultados

Os candidatos encontrados são classificados para facilitar a revisão:

* **Correspondência textual exata:** descrição normalizada e números correspondentes;
* **Revisar diferenças numéricas:** existem diferenças nos números identificados;
* **Candidato para revisão:** existe semelhança textual, mas é necessária uma avaliação adicional.

Essas classificações são indicativas. Elas não comprovam que dois serviços sejam equivalentes ou diferentes em todos os aspectos técnicos.

### Investigação de possíveis duplicidades

A análise identificou 341 grupos com mais de um código de serviço para a mesma descrição normalizada, grupo, fornecedor e unidade de medida.

Entre os casos encontrados, há um grupo com 13 códigos associados à descrição `SV - FURAÇÃO EM CONCRETO COM COROA DIAM`.

Esses casos foram separados para investigação. A quantidade de códigos é utilizada como critério inicial de priorização, não como confirmação de erro.

## 7. Testes iniciais da busca

Foram realizados testes com descrições existentes na base, variações textuais e uma descrição sem correspondência aparente.

Nos testes com dez descrições existentes selecionadas para avaliação, a busca encontrou pelo menos um dos códigos esperados em todas as dez consultas.

Entretanto, também foram retornados candidatos adicionais. Isso demonstra a importância de avaliar a precisão dos resultados e reduzir sugestões irrelevantes.

**Limitação atual:** os testes ainda são iniciais e não representam uma avaliação completa da qualidade da solução. Encontrar o código esperado não significa que todos os demais candidatos estejam incorretos, nem comprova a equivalência técnica dos serviços.

## 8. Próximas etapas

1. Avaliar diferentes limites de similaridade e a qualidade dos resultados;
2. Organizar as funções validadas em módulos dentro de `src/`;
3. Criar testes para os principais comportamentos da busca;
4. Definir regras de negócio para tratar possíveis duplicidades;
5. Desenvolver uma forma de disponibilizar a busca para a aplicação Java;
6. Avaliar a integração entre Python e Java;
7. Implementar a validação do limite de 40 caracteres do SAP;
8. Preparar uma avaliação com casos representativos e resultados verificáveis.

## 9. Execução do projeto

Crie e ative um ambiente virtual no Windows PowerShell:

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
```

Instale as dependências:

```powershell
python -m pip install -r requirements.txt
```

Abra o notebook `notebooks/01_analise_exploratoria.ipynb` no VS Code para acompanhar as análises já desenvolvidas.

Os caminhos e nomes dos arquivos de dados devem ser ajustados conforme a organização local do repositório.

## 10. Cuidados com os dados

Não publique bases internas, informações confidenciais ou credenciais no GitHub. Mantenha os arquivos originais preservados e fora do versionamento quando contiverem informações restritas.

Os resultados da análise devem ser tratados como indicadores para investigação, e não como confirmação automática de duplicidade.
