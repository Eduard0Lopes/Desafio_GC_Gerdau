<script setup lang="ts">
import { computed } from 'vue'
import { theme } from '../services/tema'

interface Fatia {
  label: string
  value: number
  color: string
}

const props = withDefaults(defineProps<{
  data: Fatia[]
  size?: number
  centerLabel?: string
}>(), {
  size: 190,
  centerLabel: 'registros'
})

const total = computed(() => props.data.reduce((s, slice) => s + slice.value, 0))
const formattedTotal = computed(() =>
  total.value >= 1000 ? (total.value / 1000).toFixed(1).replace('.', ',') + ' mil' : String(total.value))

const escuro = computed(() => theme.state.theme === 'escuro')
const centerColor = computed(() => (escuro.value ? theme.color('--cartao', '#0a264e') : '#ffffff'))
const titleColor = computed(() => (escuro.value ? theme.color('--texto', '#e8f1fb') : '#00205b'))
const subtitleColor = computed(() => (escuro.value ? theme.color('--texto-suave', '#a3c0e0') : '#5c6f81'))

const slices = computed(() => {
  const baseTotal = total.value || 1
  let angle = -Math.PI / 2
  return props.data
    .filter((slice) => slice.value > 0)
    .map((slice) => {
      const start = angle
      angle += (slice.value / baseTotal) * 2 * Math.PI
      return { ...slice, start, end: angle }
    })
})

function polar(radius: number, angle: number): [number, number] {
  return [radius * Math.cos(angle), radius * Math.sin(angle)]
}

function path(start: number, end: number): string {
  const outerRadius = props.size / 2 - 4
  const innerRadius = 52
  const [outerStartX, outerStartY] = polar(outerRadius, start)
  const [outerEndX, outerEndY] = polar(outerRadius, end)
  const [innerEndX, innerEndY] = polar(innerRadius, end)
  const [innerStartX, innerStartY] = polar(innerRadius, start)
  const large = end - start > Math.PI ? 1 : 0
  return `M ${outerStartX} ${outerStartY} A ${outerRadius} ${outerRadius} 0 ${large} 1 ${outerEndX} ${outerEndY}
          L ${innerEndX} ${innerEndY} A ${innerRadius} ${innerRadius} 0 ${large} 0 ${innerStartX} ${innerStartY} Z`
}

function percentage(value: number): string {
  if (!total.value) return '0'
  return ((value / total.value) * 100).toFixed(1).replace('.', ',')
}
</script>

<template>
  <div class="grafico-area">
    <svg :width="size" :height="size" :viewBox="`0 0 ${size} ${size}`" role="img"
         :aria-label="`Gráfico de pizza: ${total} registros`">

      <g :transform="`translate(${size / 2}, ${size / 2})`">
        <path
          v-for="(slice, i) in slices"
          :key="i"
          :d="path(slice.start, slice.end)"
          :fill="slice.color"
          :stroke="centerColor"
          stroke-width="2"
        />

        <circle r="52" :fill="centerColor" />
        <text y="-4" text-anchor="middle" font-size="26" font-weight="800" :fill="titleColor">
          {{ formattedTotal }}
        </text>
        <text y="18" text-anchor="middle" font-size="11" :fill="subtitleColor">{{ centerLabel }}</text>
      </g>
    </svg>

    <div class="grafico-legenda">
      <div v-for="(slice, i) in slices" :key="i" class="item">
        <span class="cor" :style="{ background: slice.color }"></span>
        <span>{{ slice.label }}</span>
        <span class="valor">{{ slice.value.toLocaleString('pt-BR') }} ({{ percentage(slice.value) }}%)</span>
      </div>
    </div>
  </div>
</template>
