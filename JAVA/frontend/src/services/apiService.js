import axios from 'axios'

const API = axios.create({
  baseURL: '/api'
})

export const GrupoServicoService = {
  listarTodos: () => API.get('/grupos-servico'),
  salvar: (grupo) => API.post('/grupos-servico', grupo)
}

export const UnidadeMedidaService = {
  listarTodos: () => API.get('/unidades-medida'),
  salvar: (unidade) => API.post('/unidades-medida', unidade)
}

export const CodigoServicoService = {
  listarTodos: () => API.get('/codigo-servico'),
  salvar: (codigo) => API.post('/codigo-servico', codigo)
}