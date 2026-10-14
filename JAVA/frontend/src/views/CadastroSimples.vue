<script setup>
import { onMounted, reactive, ref } from 'vue'

import { useDebounce } from '@/composables/useDebounce'
import { useNotificacoes } from '@/composables/useNotificacoes'

/** CRUD genérico para as entidades simples (grupos, fornecedores, unidades). */
const props = defineProps({
  titulo: { type: String, required: true },
  service: { type: Object, required: true },
  campos: { type: Array, required: true },
  colunas: { type: Array, required: true },
})
const emit = defineEmits(['alterado'])

const { notificar, notificarErro } = useNotificacoes()

const itens = ref([])
const filtro = ref('')
const carregando = ref(false)
const salvando = ref(false)
const editandoId = ref(null)
const errosCampos = ref({})
const form = reactive(vazio())

function vazio() {
  return Object.fromEntries(props.campos.map((c) => [c.nome, '']))
}

async function carregar() {
  carregando.value = true
  try {
    itens.value = await props.service.listar({ q: filtro.value || undefined })
  } catch (e) {
    notificarErro(e)
  } finally {
    carregando.value = false
  }
}
const filtrar = useDebounce(carregar)

function limpar() {
  Object.assign(form, vazio())
  editandoId.value = null
  errosCampos.value = {}
}

function editar(item) {
  props.campos.forEach((c) => (form[c.nome] = item[c.nome] ?? ''))
  editandoId.value = item.id
  errosCampos.value = {}
}

async function salvar() {
  salvando.value = true
  errosCampos.value = {}
  try {
    const payload = { ...form }
    const salvo = editandoId.value
      ? await props.service.atualizar(editandoId.value, payload)
      : await props.service.criar(payload)
    notificar(`${props.titulo} ${salvo.id} ${editandoId.value ? 'atualizado(a)' : 'criado(a)'}.`)
    limpar()
    emit('alterado')
    await carregar()
  } catch (e) {
    errosCampos.value = notificarErro(e).campos
  } finally {
    salvando.value = false
  }
}

async function excluir(item) {
  if (!confirm(`Excluir ${props.titulo.toLowerCase()} ${item.id}?`)) return
  try {
    await props.service.excluir(item.id)
    notificar(`${props.titulo} ${item.id} excluído(a).`)
    if (editandoId.value === item.id) limpar()
    emit('alterado')
    await carregar()
  } catch (e) {
    notificarErro(e)
  }
}

onMounted(carregar)
</script>

<template>
  <div class="grade">
    <form class="card" @submit.prevent="salvar">
      <h3>{{ editandoId ? `Editar ${titulo} ${editandoId}` : `Novo(a) ${titulo}` }}</h3>
      <div v-for="campo in campos" :key="campo.nome" class="campo">
        <label :for="campo.nome">{{ campo.rotulo }}</label>
        <input
          :id="campo.nome"
          v-model="form[campo.nome]"
          :type="campo.tipo || 'text'"
          :maxlength="campo.maxlength"
          :class="{ invalido: errosCampos[campo.nome] }"
          required
        />
        <span v-if="errosCampos[campo.nome]" class="erro-campo">{{ errosCampos[campo.nome] }}</span>
      </div>
      <p class="dica">O ID é gerado pela API no mesmo formato da migração.</p>
      <div class="acoes">
        <button type="submit" :disabled="salvando">{{ editandoId ? 'Salvar alterações' : 'Cadastrar' }}</button>
        <button v-if="editandoId" type="button" class="secundario" @click="limpar">Cancelar</button>
      </div>
    </form>

    <section class="card">
      <div class="filtros">
        <input v-model="filtro" type="search" :placeholder="`Filtrar ${titulo.toLowerCase()}…`" @input="filtrar" />
      </div>
      <div class="tabela-wrapper">
        <table>
          <thead>
            <tr>
              <th v-for="col in colunas" :key="col.campo">{{ col.rotulo }}</th>
              <th>Origem</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in itens" :key="item.id">
              <td v-for="col in colunas" :key="col.campo" :class="{ mono: col.mono }">{{ item[col.campo] }}</td>
              <td><span :class="['selo', item.origem]">{{ item.origem }}</span></td>
              <td class="acoes">
                <button class="link" @click="editar(item)">editar</button>
                <button class="link" @click="excluir(item)">excluir</button>
              </td>
            </tr>
          </tbody>
        </table>
        <p v-if="!carregando && !itens.length" class="vazio">Nenhum registro encontrado.</p>
        <p v-if="carregando" class="vazio">Carregando…</p>
      </div>
      <p class="dica">{{ itens.length }} registro(s)</p>
    </section>
  </div>
</template>
