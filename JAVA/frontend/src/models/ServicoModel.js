export class GrupoServico {
  constructor(id = null, nome = '') { this.id = id; this.nome = nome }
}
export class UnidadeMedida {
  constructor(id = null, sigla = '', nome = '') { this.id = id; this.sigla = sigla; this.nome = nome }
}
export class CodigoServico {
  constructor(id = null, descricao = '', grupoServicoId = '', unidadeMedidaId = '') {
    this.id = id; this.descricao = descricao; this.grupoServicoId = grupoServicoId; this.unidadeMedidaId = unidadeMedidaId
  }
}
