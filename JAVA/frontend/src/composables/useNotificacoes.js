import { reactive } from 'vue'

import { extrairErro } from '@/services/apiService'

const estado = reactive({ itens: [] })
let proximoId = 1

export function useNotificacoes() {
  function notificar(texto, tipo = 'sucesso', duracao = 4500) {
    const id = proximoId++
    estado.itens.push({ id, texto, tipo })
    setTimeout(() => remover(id), duracao)
  }

  function remover(id) {
    const i = estado.itens.findIndex((n) => n.id === id)
    if (i >= 0) estado.itens.splice(i, 1)
  }

  function notificarErro(erro) {
    const { mensagem } = extrairErro(erro)
    notificar(mensagem, 'erro', 7000)
    return extrairErro(erro)
  }

  return { estado, notificar, notificarErro, remover }
}
