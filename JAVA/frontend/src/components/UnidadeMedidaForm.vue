<template>
  <section class="card">
    <h2>2. Unidades de Medida</h2>
    <form @submit.prevent="salvar">
      <input v-model.trim="form.sigla" maxlength="10" placeholder="Sigla (ex: UN, KG, H)" required />
      <input v-model.trim="form.nome" maxlength="100" placeholder="Nome da unidade" required />
      <button type="submit" :disabled="salvando">{{ salvando ? 'Salvando…' : 'Cadastrar Unidade' }}</button>
      <p v-if="erro" class="error" role="alert">{{ erro }}</p>
    </form>
    <ul><li v-for="u in unidades" :key="u.id"><strong>{{ u.sigla }}</strong> — {{ u.nome }}</li></ul>
  </section>
</template>
<script setup>
import { ref } from 'vue'
import { UnidadeMedida } from '../models/ServicoModel'
defineProps({ unidades: { type: Array, default: () => [] } })
const emit = defineEmits(['salvarUnidade'])
const form = ref(new UnidadeMedida())
const salvando = ref(false)
const erro = ref('')
const salvar = async () => {
  erro.value = ''
  salvando.value = true
  try {
    await emit('salvarUnidade', { sigla: form.value.sigla, nome: form.value.nome })
    form.value = new UnidadeMedida()
  } catch (e) {
    erro.value = e?.response?.data?.message || 'Não foi possível salvar a unidade.'
  } finally { salvando.value = false }
}
</script>
<style scoped>
.card { background-color: #282a36; border-radius: 8px; padding: 20px; border: 1px solid #44475a; }
form { display: flex; flex-direction: column; gap: 10px; margin-bottom: 15px; }
input, button { padding: 10px; border-radius: 4px; border: 1px solid #6272a4; background-color: #44475a; color: #f8f9fa; }
button { background-color: #bd93f9; color: #282a36; font-weight: bold; cursor: pointer; border: none; }
button:disabled { opacity: .6; cursor: wait; }
.error { color: #ff8080; }
</style>
