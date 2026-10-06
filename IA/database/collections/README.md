# Banco de Dados - MongoDB

Banco de dados utilizado no protótipo do projeto de IA e Dados.

## Banco

Nome do banco:

`gerdau_prototipo`

O banco foi desenvolvido em MongoDB, utilizando uma estrutura não relacional baseada em coleções e documentos.

## Estrutura

O banco possui cinco coleções principais:

### 1. grupos_servico

Armazena os grupos aos quais os serviços pertencem.

Principais campos:

- `_id`: identificador do grupo
- `nome`: nome do grupo
- `nomeNormalizado`: nome sem acentuação e padronizado
- `origem`: origem do registro

Exemplo:

```json
{
  "_id": "001",
  "nome": "Eletromecânica",
  "nomeNormalizado": "eletromecanica",
  "origem": "MIGRACAO"
}