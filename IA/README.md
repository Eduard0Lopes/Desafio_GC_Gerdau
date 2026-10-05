# Desafio GC 2026 — Padronização de Códigos de Serviço

Projeto da trilha de **IA & Dados**, voltado à identificação de possíveis duplicidades e inconsistências no catálogo de códigos de serviço da Gerdau.

## Objetivo

Desenvolver uma solução baseada em dados e inteligência de busca para identificar serviços potencialmente duplicados, melhorar a padronização das descrições e reduzir a criação de novos códigos desnecessários.

A proposta é apoiar a identificação de serviços existentes antes do cadastro de novos itens, preservando diferenças legítimas entre serviços.

## Problema

O cadastro manual de serviços pode gerar descrições inconsistentes, variações de formatação, múltiplos códigos associados a descrições iguais e dificuldades para localizar itens existentes.

Essas situações podem prejudicar a qualidade do catálogo e dificultar a consolidação e a análise dos dados.

## Tecnologias e ferramentas

* **Python:** desenvolvimento das análises e regras de validação.
* **Pandas:** manipulação, agrupamento e análise dos dados.
* **Jupyter Notebook:** execução documentada das análises exploratórias.
* **Git e GitHub:** controle de versão e organização do projeto.

Tecnologias para a solução integrada serão definidas e validadas durante as próximas etapas.

## Estrutura dos dados

A base analisada contém **9.795 registros e 5 colunas**:

* `Grupo de Serviço`
* `Código do Fornecedor`
* `Código do Serviço`
* `Descrição do Item`
* `Unidade medida`

## Análise exploratória

Foram realizadas análises para compreender a estrutura do catálogo e identificar padrões que possam indicar problemas de qualidade.

Principais resultados:

* **10.925 pares de descrições semelhantes** identificados na análise textual.
* **410 pares prioritários** selecionados para investigação mais detalhada.
* **375 descrições** associadas a mais de um código de serviço.
* **320 combinações** de fornecedor, descrição e unidade associadas a múltiplos códigos.
* Entre essas 320 combinações, **68 (21,25%) cruzam faixas numéricas de códigos**, enquanto 252 (78,75%) permanecem na mesma faixa.
* Também foram identificados códigos compartilhados entre fornecedores com divergências de atributos, incluindo casos que precisam de validação adicional.

Esses resultados representam candidatos e padrões exploratórios. Não comprovam, isoladamente, a existência de duplicidades reais, pois códigos diferentes podem representar condições ou serviços legítimos.

## Próxima etapa

Construir a primeira versão de um **detector de possíveis duplicidades**, capaz de:

1. Identificar descrições iguais associadas a códigos diferentes.
2. Comparar descrições semelhantes por similaridade textual.
3. Preservar diferenças relevantes, como números, medidas, sinais e operadores.
4. Considerar fornecedor, grupo de serviço e unidade de medida na análise.
5. Gerar uma tabela de candidatos com os códigos envolvidos, o motivo do alerta e a prioridade de revisão.

A solução deverá apoiar a revisão humana, sem excluir ou unificar códigos automaticamente.

## Status do projeto

* [x] Preparação e exploração inicial da base.
* [x] Análise de descrições iguais e códigos múltiplos.
* [x] Análise de similaridade textual.
* [x] Investigação de padrões de inconsistência.
* [ ] Implementação do detector de possíveis duplicidades.
* [ ] Validação dos alertas e revisão dos critérios.
* [ ] Avaliação dos resultados e preparação da demonstração.

## Observação

Os resultados são exploratórios e dependem de validação das regras de negócio. Os critérios de similaridade e prioridade ainda deverão ser testados antes de serem utilizados como regras definitivas.
