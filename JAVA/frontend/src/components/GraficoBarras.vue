<script setup lang="ts">
import { computed } from 'vue'
import { Bar } from 'vue-chartjs'
import {
  BarElement,
  CategoryScale,
  Chart as ChartJS,
  Legend,
  LinearScale,
  Tooltip,
  type ChartOptions
} from 'chart.js'
import { theme } from '../services/tema'

ChartJS.register(BarElement, CategoryScale, LinearScale, Tooltip, Legend)

interface Item {
  nome: string
  value: number
}

const props = withDefaults(defineProps<{
  data: Item[]
  title?: string
  color?: string
  label?: string
}>(), { title: '', color: '#005eb8', label: 'Registros' })

const height = computed(() => Math.max(200, props.data.length * 42 + 70))

const textColor = computed(() => {
  void theme.state.theme
  return theme.color('--texto', '#1c2b3a')
})
const titleColor = computed(() => {
  void theme.state.theme
  return theme.color('--azul-escuro', '#00205b')
})
const gridColor = computed(() => {
  void theme.state.theme
  return theme.state.theme === 'escuro' ? 'rgba(157, 176, 195, 0.18)' : 'rgba(128, 148, 168, 0.14)'
})
const barColor = computed(() => {
  void theme.state.theme
  return theme.state.theme === 'escuro' && props.color.toLowerCase() === '#00205b' ? '#4fa3e8' : props.color
})

const chartData = computed(() => ({
  labels: props.data.map((entry) => entry.nome.length > 22 ? entry.nome.slice(0, 21) + '…' : entry.nome),
  datasets: [
    {
      label: props.label,
      data: props.data.map((entry) => entry.value),
      backgroundColor: props.data.map((_, index) => (index % 2 === 0 ? barColor.value : '#ffc72c')),
      borderRadius: 7,
      barThickness: 22
    }
  ]
}))

const options = computed<ChartOptions<'bar'>>(() => ({
  responsive: true,
  maintainAspectRatio: false,
  animation: { duration: 900, easing: 'easeOutQuart' },
  layout: { padding: { top: 6, right: 16, bottom: 6, left: 4 } },
  plugins: {
    legend: { display: false },
    title: props.title
      ? { display: true, text: props.title, color: titleColor.value, font: { size: 14, weight: 700 } }
      : undefined,
    tooltip: {
      callbacks: {

        label: (context) => {
          const value = Number(context.dataset.data[context.dataIndex])
          return ` ${props.label}: ${value.toLocaleString('pt-BR')}`
        }
      }
    }
  },
  indexAxis: 'y' as const,
  scales: {
    x: {
      beginAtZero: true,
      ticks: { color: tickColor(), precision: 0 },
      grid: { color: gridColor.value }
    },
    y: {
      ticks: { color: textColor.value, font: { size: 11 }, autoSkip: false },
      grid: { display: false }
    }
  },

  onClick: (_evento, elements, chart) => {
    if (!elements.length) return
    const meta = chart.getDatasetMeta(0)
    const barra = meta.data[elements[0].index] as unknown as { hidden?: boolean }
    barra.hidden = !barra.hidden
    chart.update()
  }
}))

function tickColor(): string {
  return theme.color('--texto-suave', '#5c6f81')
}
</script>

<template>
  <div class="grafico-animado" :style="{ minHeight: height + 'px' }">
    <Bar :key="theme.state.theme" :data="chartData" :options="options" />
  </div>
</template>

<style scoped>
.grafico-animado {
  width: 100%;
  position: relative;
}
</style>
