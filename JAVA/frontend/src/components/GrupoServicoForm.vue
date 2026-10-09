<template>
  <section class="card">
    <h2>1. Grupos de Serviço</h2>
    <form @submit.prevent="salvar">
      <input v-model="form.nome" placeholder="Nome do Grupo" required />
      <button type="submit">Cadastrar Grupo</button>
    </form>
    <ul>
      <li v-for="grupo in grupos" :key="grupo.id">
        <strong>{{ grupo.nome }}</strong>
      </li>
    </ul>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import { GrupoServico } from '../models/ServicoModel'

defineProps({ grupos: Array })
const emit = defineEmits(['salvarGrupo'])

const form = ref(new GrupoServico())

const salvar = () => {
  emit('salvarGrupo', { ...form.value })
  form.value = new GrupoServico()
}
</script>

<style scoped>
.card { background-color: #282a36; border-radius: 8px; padding: 20px; border: 1px solid #44475a; }
form { display: flex; flex-direction: column; gap: 10px; margin-bottom: 15px; }
input, button { padding: 10px; border-radius: 4px; border: 1px solid #6272a4; background-color: #44475a; color: #f8f9fa; }
button { background-color: #bd93f9; color: #282a36; font-weight: bold; cursor: pointer; border: none; }
button:hover { background-color: #ff79c6; }
ul { list-style: none; padding: 0; }
li { padding: 6px 0; border-bottom: 1px solid #44475a; }
</style>