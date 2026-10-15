# EDA — Análise Exploratória da Base

## Volume e consolidação

| Métrica | Valor |
|---|---|
| Registros originais (legados) | **9.795** |
| Serviços únicos após saneamento | **7.308** |
| **Redução potencial do catálogo** | **25,4%** |

## Distribuição das decisões (matriz de-para)

| Decisão | Registros | % |
|---|---|---|
| ORIGINAL | 5.411 | 55,2% |
| DUPLICIDADE | 4.244 | 43,3% |
| ALERTA | 140 | 1,4% |

## Uso por Grupo de Serviço (categorias-piloto)

| Grupo | Registros | % |
|---|---|---|
| Civil | 4.530 | 46,2% |
| Facilities | 2.781 | 28,4% |
| Eletromecânica | 2.484 | 25,4% |

## Dimensões do catálogo

| Dimensão | Quantidade |
|---|---|
| Grupos de Serviço | 3 |
| Códigos de Fornecedor distintos | 111 |
| Unidades de Medida distintas | 17 |
| Itens com alerta SAP (>= 40 chars) | 1.519 (15,5%) |
| Duplicidades exatas detectadas | 3.967 (40,5%) |
| Maior consolidação por código canônico | até 13 legados em um único newId |

## Padrões observados nas descrições

- Prefixos recorrentes (`SV`, `DIARIA`, `MANUTENCAO`) — vocabulário de categoria.
- Medidas/valores embutidos no texto (`1/2`, `8h`, `12 x 36`) — sensíveis à regra
  numérica do motor (evita casar `ELETRODUTO 1/2` com `ELETRODUTO 2 1/2`).
- Nomes de unidades abreviados na descrição (HR, UN, KG) — separados no modelo
  como componente U.M., conforme o documento técnico.

## Conclusão exploratória

O problema é estrutural: quase metade da base (43,3%) são duplicidades reconhecidas.
A consolidação por descrição normalizada + similaridade reduz o catálogo em ~25%
mantendo 100% de rastreabilidade — base para o filtro de entrada que impede o
crescimento recorrente.
