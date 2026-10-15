<template>
  <div class="grafico-animado">
    <Doughnut v-if="temDados" :key="tema.estado.tema" :data="dadosGrafico" :options="opcoes" ref="graficoRef" />
    <div v-else class="grafico-vazio suave">Sem dados para exibir.</div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { Doughnut } from 'vue-chartjs'
import {
  ArcElement,
  Chart as ChartJS,
  Legend,
  Tooltip,
  type ChartOptions
} from 'chart.js'
import { tema } from '../services/tema'

ChartJS.register(ArcElement, Tooltip, Legend)

interface Fatia {
  rotulo: string
  valor: number
  cor: string
}

const props = withDefaults(defineProps<{
  dados: Fatia[]
  titulo?: string
}>(), { titulo: '' })

const graficoRef = ref<InstanceType<typeof Doughnut> | null>(null)

// dependem do tema (reativo) — garantem re-render correto ao troclar claro/escuro
const corTexto = computed(() => {
  void tema.estado.tema
  return tema.cor('--texto', '#1c2b3a')
})
const corTitulo = computed(() => {
  void tema.estado.tema
  return tema.cor('--azul-escuro', '#00205b')
})
const corBorda = computed(() => {
  void tema.estado.tema
  return tema.cor('--cartao', '#ffffff')
})

const dadosGrafico = computed(() => {
  // fatias com valor 0 criam arcos fantasmas no Chart.js — são filtradas
  const fatias = props.dados.filter((f) => f.valor > 0)
  return {
    labels: fatias.map((f) => f.rotulo),
    datasets: [
      {
        data: fatias.map((f) => f.valor),
        backgroundColor: fatias.map((f) => f.cor),
        borderColor: corBorda.value,
        borderWidth: 3,
        hoverOffset: 10
      }
    ]
  }
})

const temDados = computed(() => props.dados.some((f) => f.valor > 0))

const opcoes = computed<ChartOptions<'doughnut'>>(() => ({
  responsive: true,
  maintainAspectRatio: true,
  animation: { animateRotate: true, animateScale: true, duration: 900 },
  layout: { padding: 8 },
  plugins: {
    legend: {
      position: 'bottom',
      labels: { usePointStyle: true, padding: 16, color: corTexto.value, font: { size: 11 } }
    },
    title: props.titulo
      ? { display: true, text: props.titulo, color: corTitulo.value, font: { size: 14, weight: 700 } }
      : undefined,
    tooltip: {
      callbacks: {
        label: (ctx) => ` ${ctx.label}: ${Number(ctx.parsed).toLocaleString('pt-BR')}`
      }
    }
  },
  // clique na fatia oculta/mostra — mesmo mecanismo da legenda (a "bolinha" restaura)
  onClick: (_evento, elementos, grafico) => {
    if (!elementos.length) return
    grafico.toggleDataVisibility(elementos[0].index)
    grafico.update()
  }
}))
</script>

<style scoped>
.grafico-animado {
  width: 100%;
  max-width: 340px;
}
.grafico-vazio {
  display: grid;
  place-items: center;
  min-height: 190px;
  border: 1px dashed var(--borda);
  border-radius: 12px;
}
</style>
