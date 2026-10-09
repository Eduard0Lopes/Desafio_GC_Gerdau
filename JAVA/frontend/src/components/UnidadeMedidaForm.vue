<template>
  <section class="card">
    <h2>2. Unidades de Medida</h2>
    <form @submit.prevent="salvar">
      <input v-model="form.sigla" placeholder="Sigla (ex: UN, KG, H)" required />
      <input v-model="form.descricao" placeholder="Descrição" required />
      <button type="submit">Cadastrar Unidade</button>
    </form>
    <ul>
      <li v-for="u in unidades" :key="u.id">
        <strong>{{ u.sigla }}</strong> — {{ u.descricao }}
      </li>
    </ul>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import { UnidadeMedida } from '../models/ServicoModel'

defineProps({ unidades: Array })
const emit = defineEmits(['salvarUnidade'])

const form = ref(new UnidadeMedida())

const salvar = () => {
  emit('salvarUnidade', { ...form.value })
  form.value = new UnidadeMedida()
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