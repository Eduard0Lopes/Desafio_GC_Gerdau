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
import { theme } from '../services/tema'

ChartJS.register(ArcElement, Tooltip, Legend)

interface Fatia {
  label: string
  value: number
  color: string
}

const props = withDefaults(defineProps<{
  data: Fatia[]
  title?: string
}>(), { title: '' })

const chartRef = ref<InstanceType<typeof Doughnut> | null>(null)

const textColor = computed(() => {
  void theme.state.theme
  return theme.color('--texto', '#1c2b3a')
})
const titleColor = computed(() => {
  void theme.state.theme
  return theme.color('--azul-escuro', '#00205b')
})
const borderColor = computed(() => {
  void theme.state.theme
  return theme.color('--cartao', '#ffffff')
})

const chartData = computed(() => {

  const slices = props.data.filter((slice) => slice.value > 0)
  return {
    labels: slices.map((slice) => slice.label),
    datasets: [
      {
        data: slices.map((slice) => slice.value),
        backgroundColor: slices.map((slice) => slice.color),
        borderColor: borderColor.value,
        borderWidth: 3,
        hoverOffset: 10
      }
    ]
  }
})

const hasData = computed(() => props.data.some((slice) => slice.value > 0))

const options = computed<ChartOptions<'doughnut'>>(() => ({
  responsive: true,
  maintainAspectRatio: true,
  animation: { animateRotate: true, animateScale: true, duration: 900 },
  layout: { padding: 8 },
  plugins: {
    legend: {
      position: 'bottom',
      labels: { usePointStyle: true, padding: 16, color: textColor.value, font: { size: 11 } }
    },
    title: props.title
      ? { display: true, text: props.title, color: titleColor.value, font: { size: 14, weight: 700 } }
      : undefined,
    tooltip: {
      callbacks: {
        label: (context) => ` ${context.label}: ${Number(context.parsed).toLocaleString('pt-BR')}`
      }
    }
  },

  onClick: (_evento, elements, chart) => {
    if (!elements.length) return
    chart.toggleDataVisibility(elements[0].index)
    chart.update()
  }
}))
</script>

<template>
  <div class="grafico-animado">
    <Doughnut v-if="hasData" :key="theme.state.theme" :data="chartData" :options="options" ref="graficoRef" />
    <div v-else class="grafico-vazio suave">Sem dados para exibir.</div>
  </div>
</template>

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
