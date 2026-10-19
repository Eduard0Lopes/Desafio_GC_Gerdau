<template>
  <div class="grafico-animado" :style="{ minHeight: altura + 'px' }">
    <Bar :key="tema.estado.tema" :data="dadosGrafico" :options="opcoes" />
  </div>
</template>

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
import { tema } from '../services/tema'

ChartJS.register(BarElement, CategoryScale, LinearScale, Tooltip, Legend)

interface Item {
  nome: string
  valor: number
}

const props = withDefaults(defineProps<{
  dados: Item[]
  titulo?: string
  cor?: string
  rotulo?: string
}>(), { titulo: '', cor: '#005eb8', rotulo: 'Registros' })

const altura = computed(() => Math.max(200, props.dados.length * 42 + 70))


const corTexto = computed(() => {
  void tema.estado.tema
  return tema.cor('--texto', '#1c2b3a')
})
const corTitulo = computed(() => {
  void tema.estado.tema
  return tema.cor('--azul-escuro', '#00205b')
})
const corGrade = computed(() => {
  void tema.estado.tema
  return tema.estado.tema === 'escuro' ? 'rgba(157, 176, 195, 0.18)' : 'rgba(128, 148, 168, 0.14)'
})
const corBarra = computed(() => {
  void tema.estado.tema
  // o azul-marinho da marca some no fundo escuro; lá usamos o azul Gerdau claro
  return tema.estado.tema === 'escuro' && props.cor.toLowerCase() === '#00205b' ? '#4fa3e8' : props.cor
})

const dadosGrafico = computed(() => ({
  labels: props.dados.map((i) => i.nome.length > 22 ? i.nome.slice(0, 21) + '…' : i.nome),
  datasets: [
    {
      label: props.rotulo,
      data: props.dados.map((i) => i.valor),
      backgroundColor: props.dados.map((_, idx) => (idx % 2 === 0 ? corBarra.value : '#ffc72c')),
      borderRadius: 7,
      barThickness: 22
    }
  ]
}))

const opcoes = computed<ChartOptions<'bar'>>(() => ({
  responsive: true,
  maintainAspectRatio: false,
  animation: { duration: 900, easing: 'easeOutQuart' },
  layout: { padding: { top: 6, right: 16, bottom: 6, left: 4 } },
  plugins: {
    legend: { display: false },
    title: props.titulo
      ? { display: true, text: props.titulo, color: corTitulo.value, font: { size: 14, weight: 700 } }
      : undefined,
    tooltip: {
      callbacks: {
        
        label: (ctx) => {
          const valor = Number(ctx.dataset.data[ctx.dataIndex])
          return ` ${props.rotulo}: ${valor.toLocaleString('pt-BR')}`
        }
      }
    }
  },
  indexAxis: 'y' as const,
  scales: {
    x: {
      beginAtZero: true,
      ticks: { color: corSuaveTick(), precision: 0 },
      grid: { color: corGrade.value }
    },
    y: {
      ticks: { color: corTexto.value, font: { size: 11 }, autoSkip: false },
      grid: { display: false }
    }
  },
  
  onClick: (_evento, elementos, grafico) => {
    if (!elementos.length) return
    const meta = grafico.getDatasetMeta(0)
    const barra = meta.data[elementos[0].index] as unknown as { hidden?: boolean }
    barra.hidden = !barra.hidden
    grafico.update()
  }
}))

function corSuaveTick(): string {
  return tema.cor('--texto-suave', '#5c6f81')
}
</script>

<style scoped>
.grafico-animado {
  width: 100%;
  position: relative;
}
</style>
