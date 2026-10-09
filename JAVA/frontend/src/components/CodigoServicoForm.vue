<template>
  <section class="card full-width">
    <h2>3. Cadastro de Código de Serviço (Regra SAP 40 Caracteres)</h2>
    <form @submit.prevent="salvar">
      <div class="form-group">
        <label>Grupo de Serviço:</label>
        <select v-model="form.grupoServicoId" required>
          <option value="" disabled>Selecione um Grupo</option>
          <option v-for="g in grupos" :key="g.id" :value="g.id">{{ g.nome }}</option>
        </select>
      </div>

      <div class="form-group">
        <label>Unidade de Medida:</label>
        <select v-model="form.unidadeMedidaId" required>
          <option value="" disabled>Selecione uma Unidade</option>
          <option v-for="u in unidades" :key="u.id" :value="u.id">{{ u.sigla }} - {{ u.descricao }}</option>
        </select>
      </div>

      <div class="form-group">
        <label>Descrição Breve do Serviço (Max 40 Caracteres):</label>
        <input 
          v-model="form.descricao" 
          maxlength="40" 
          placeholder="Ex: Manutenção Preventiva Ponte Rolante" 
          required 
        />
        <small :class="{ error: form.descricao.length >= 40 }">
          {{ form.descricao.length }} / 40 caracteres
        </small>
      </div>

      <button type="submit">Gerar e Salvar Código SAP</button>
    </form>

    <hr />

    <h3>Serviços Cadastrados</h3>
    <table>
      <thead>
        <tr>
          <th>Código</th>
          <th>Descrição (SAP)</th>
          <th>Grupo</th>
          <th>Unidade</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in codigos" :key="item.id">
        <td><code>{{ item.codigoOriginal || item.id }}</code></td>  
        <td>{{ item.descricao }}</td>
          <td>{{ item.grupoServico?.nome || '-' }}</td>
          <td>{{ item.unidadeMedida?.sigla || '-' }}</td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import { CodigoServico } from '../models/ServicoModel'

defineProps({
  grupos: Array,
  unidades: Array,
  codigos: Array
})

const emit = defineEmits(['salvarCodigo'])
const form = ref(new CodigoServico())

const salvar = () => {
  if (form.value.descricao.length > 40) {
    alert('A descrição excede o limite de 40 caracteres exigido pelo SAP.')
    return
  }

  const payload = {
    descricao: form.value.descricao,
    grupoServicoId: form.value.grupoServicoId,
    unidadeMedidaId: form.value.unidadeMedidaId,
    grupoServico: { id: form.value.grupoServicoId },
    unidadeMedida: { id: form.value.unidadeMedidaId }
  }

  emit('salvarCodigo', payload)
  form.value = new CodigoServico()
}
</script>

<style scoped>
.full-width { grid-column: 1 / -1; }
.card { background-color: #282a36; border-radius: 8px; padding: 20px; border: 1px solid #44475a; }
form { display: flex; flex-direction: column; gap: 10px; margin-bottom: 15px; }
.form-group { display: flex; flex-direction: column; gap: 5px; }
input, select, button { padding: 10px; border-radius: 4px; border: 1px solid #6272a4; background-color: #44475a; color: #f8f9fa; }
button { background-color: #bd93f9; color: #282a36; font-weight: bold; cursor: pointer; border: none; }
button:hover { background-color: #ff79c6; }
table { width: 100%; border-collapse: collapse; margin-top: 10px; }
th, td { padding: 10px; text-align: left; border-bottom: 1px solid #44475a; }
th { color: #8be9fd; }
small { color: #50fa7b; }
small.error { color: #ff5555; font-weight: bold; }
</style>