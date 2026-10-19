<template>
  <div class="grafico-area">
    <svg :width="tamanho" :height="tamanho" :viewBox="`0 0 ${tamanho} ${tamanho}`" role="img"
         :aria-label="`Gráfico de pizza: ${total} registros`">
      
      <g :transform="`translate(${tamanho / 2}, ${tamanho / 2})`">
        <path
          v-for="(fatia, i) in fatias"
          :key="i"
          :d="caminho(fatia.inicio, fatia.fim)"
          :fill="fatia.cor"
          :stroke="corCentro"
          stroke-width="2"
        />
        
        <circle r="52" :fill="corCentro" />
        <text y="-4" text-anchor="middle" font-size="26" font-weight="800" :fill="corTitulo">
          {{ totalFormatado }}
        </text>
        <text y="18" text-anchor="middle" font-size="11" :fill="corSubtitulo">{{ rotuloCentro }}</text>
      </g>
    </svg>

    <div class="grafico-legenda">
      <div v-for="(fatia, i) in fatias" :key="i" class="item">
        <span class="cor" :style="{ background: fatia.cor }"></span>
        <span>{{ fatia.rotulo }}</span>
        <span class="valor">{{ fatia.valor.toLocaleString('pt-BR') }} ({{ porcentagem(fatia.valor) }}%)</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { tema } from '../services/tema'

interface Fatia {
  rotulo: string
  valor: number
  cor: string
}

const props = withDefaults(defineProps<{
  dados: Fatia[]
  tamanho?: number
  rotuloCentro?: string
}>(), {
  tamanho: 190,
  rotuloCentro: 'registros'
})

const total = computed(() => props.dados.reduce((s, f) => s + f.valor, 0))
const totalFormatado = computed(() =>
  total.value >= 1000 ? (total.value / 1000).toFixed(1).replace('.', ',') + ' mil' : String(total.value))

const escuro = computed(() => tema.estado.tema === 'escuro')
const corCentro = computed(() => (escuro.value ? tema.cor('--cartao', '#0a264e') : '#ffffff'))
const corTitulo = computed(() => (escuro.value ? tema.cor('--texto', '#e8f1fb') : '#00205b'))
const corSubtitulo = computed(() => (escuro.value ? tema.cor('--texto-suave', '#a3c0e0') : '#5c6f81'))

const fatias = computed(() => {
  const t = total.value || 1
  let angulo = -Math.PI / 2
  return props.dados
    .filter((f) => f.valor > 0)
    .map((f) => {
      const inicio = angulo
      angulo += (f.valor / t) * 2 * Math.PI
      return { ...f, inicio, fim: angulo }
    })
})

function polar(raio: number, angulo: number): [number, number] {
  return [raio * Math.cos(angulo), raio * Math.sin(angulo)]
}

function caminho(inicio: number, fim: number): string {
  const raioExterno = props.tamanho / 2 - 4
  const raioInterno = 52
  const [x1, y1] = polar(raioExterno, inicio)
  const [x2, y2] = polar(raioExterno, fim)
  const [x3, y3] = polar(raioInterno, fim)
  const [x4, y4] = polar(raioInterno, inicio)
  const grande = fim - inicio > Math.PI ? 1 : 0
  return `M ${x1} ${y1} A ${raioExterno} ${raioExterno} 0 ${grande} 1 ${x2} ${y2}
          L ${x3} ${y3} A ${raioInterno} ${raioInterno} 0 ${grande} 0 ${x4} ${y4} Z`
}

function porcentagem(valor: number): string {
  if (!total.value) return '0'
  return ((valor / total.value) * 100).toFixed(1).replace('.', ',')
}
</script>
