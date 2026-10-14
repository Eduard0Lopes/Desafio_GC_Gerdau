import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  timeout: 15000,
})

const dados = (promise) => promise.then((res) => res.data)

function crud(recurso) {
  return {
    listar: (params) => dados(api.get(recurso, { params })),
    buscar: (id) => dados(api.get(`${recurso}/${id}`)),
    criar: (payload) => dados(api.post(recurso, payload)),
    atualizar: (id, payload) => dados(api.put(`${recurso}/${id}`, payload)),
    excluir: (id) => dados(api.delete(`${recurso}/${id}`)),
  }
}

export const GrupoServicoService = crud('/grupos-servico')
export const UnidadeMedidaService = crud('/unidades-medida')
export const CodigoFornecedorService = crud('/codigos-fornecedor')

export const CodigoServicoService = {
  ...crud('/codigos-servico'),
  similares: (descricao, limite = 5, ignorarId) =>
    dados(api.get('/codigos-servico/similares', { params: { descricao, limite, ignorarId } })),
}

export const CodigoCompletoService = {
  ...crud('/codigos-completos'),
  buscarPorLegado: (legacyId) => dados(api.get(`/codigos-completos/legado/${encodeURIComponent(legacyId)}`)),
}

export const ResumoService = {
  obter: () => dados(api.get('/resumo')),
}

/** Converte o ErrorResponse da API em { mensagem, campos }. */
export function extrairErro(erro) {
  const corpo = erro?.response?.data
  if (corpo?.message) {
    return { mensagem: corpo.message, campos: corpo.campos || {} }
  }
  if (erro?.request && !erro?.response) {
    return { mensagem: 'API indisponível. Verifique se o backend está rodando.', campos: {} }
  }
  return { mensagem: erro?.message || 'Erro inesperado.', campos: {} }
}
