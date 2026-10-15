# Relatório de Impacto — redução de duplicidades

## Números da base oficial

| Métrica | Valor |
|---|---|
| Registros na base histórica | **9.795** |
| Serviços únicos após saneamento | **7.308** |
| **Registros consolidados (redução)** | **2.487** |
| **Redução do catálogo** | **25,4%** |
| Duplicidades identificadas pela matriz | 4.244 (43,3%) |
| Alertas de similaridade | 140 |
| Itens no limite SAP (>= 40 chars) | 1.519 |
| Maior consolidação | até 13 legados → 1 código canônico |

## O que isso significa para o negócio

1. **Spend Analysis confiável** — o mesmo serviço passa a aparecer uma única vez;
   comparar gastos deixa de misturar grafias diferentes do mesmo item.
2. **Negociação por volume** — a consolidação revela o volume real por serviço,
   fortalecendo a negociação com fornecedores.
3. **Filtro de entrada (o anti-ídolo do desafio)** — novos cadastros passam pelo
   motor de similaridade com três saídas: reutilizar o existente, criar com
   justificativa auditada ou enviar para validação humana — o catálogo não volta
   a crescer com duplicidades.
4. **Rastreabilidade total** — cada código canônico preserva os `legacyIds`
   (matriz de-para): o histórico de gastos continua íntegro.

## Governança (pré-carregada com os dados reais)

| Estado | Itens |
|---|---|
| Pendentes de revisão humana (fila inicial) | 4.383 |
| Revisados pela governança | 1 *(em crescimento com o uso)* |
| Exceções auditadas | registradas em `excecoes` + `eventos_auditoria` |

## Indicadores para acompanhar (seção 15.2)

- Redução de novos cadastros duplicados após o filtro de entrada;
- % de consultas resolvidas por reutilização (sem novo cadastro);
- Precisão dos alertas de similaridade (falsos positivos/negativos);
- Tempo médio para localizar um serviço (hoje: ~30–100 ms por consulta fuzzy);
- Cobertura da matriz de-para (hoje: 100% dos legados mapeados).
