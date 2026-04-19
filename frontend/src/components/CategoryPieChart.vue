<script setup lang="ts">
import { PieChart, type PieSeriesOption } from 'echarts/charts'
import { LegendComponent, TooltipComponent, type LegendComponentOption, type TooltipComponentOption } from 'echarts/components'
import { init, use, type ComposeOption, type ECharts } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

type PieChartOption = ComposeOption<PieSeriesOption | TooltipComponentOption | LegendComponentOption>

use([PieChart, TooltipComponent, LegendComponent, CanvasRenderer])

const props = defineProps<{
  title: string
  items: Array<{
    category: string
    value: number
  }>
}>()

const chartRef = ref<HTMLDivElement | null>(null)
const themeTick = ref(0)
let chart: ECharts | null = null
let themeObserver: MutationObserver | null = null

const chartItems = computed(() => props.items.filter((item) => item.value > 0))
const total = computed(() => chartItems.value.reduce((sum, item) => sum + item.value, 0))
const hasData = computed(() => total.value > 0)

const palette = [
  '#1f5f6b',
  '#c9772e',
  '#5e8f7b',
  '#d9a15f',
  '#6a8fbf',
  '#b46f5c',
  '#8a9a5b',
  '#4e7c8d',
  '#cf8f43',
  '#7f6f9f',
]

const getCssVar = (name: string, fallback: string) => {
  const value = getComputedStyle(document.documentElement).getPropertyValue(name).trim()
  return value || fallback
}

const renderChart = async () => {
  await nextTick()

  if (!chartRef.value || !hasData.value) {
    chart?.dispose()
    chart = null
    return
  }

  if (!chart) {
    chart = init(chartRef.value)
  }

  const ink = getCssVar('--sl-ink', '#223043')
  const inkSoft = getCssVar('--sl-ink-soft', '#5d6877')
  const surface = getCssVar('--sl-paper-strong', '#fffdf8')
  const line = getCssVar('--sl-line', 'rgba(34, 48, 67, 0.12)')

  const option: PieChartOption = {
    color: palette,
    tooltip: {
      trigger: 'item',
      formatter: '{b}<br />数量：{c}<br />占比：{d}%',
      backgroundColor: surface,
      borderColor: line,
      textStyle: {
        color: ink,
      },
    },
    legend: {
      bottom: 0,
      left: 'center',
      icon: 'circle',
      itemWidth: 9,
      itemHeight: 9,
      textStyle: {
        color: inkSoft,
      },
    },
    series: [
      {
        name: props.title,
        type: 'pie',
        radius: ['42%', '68%'],
        center: ['50%', '42%'],
        minAngle: 8,
        avoidLabelOverlap: true,
        itemStyle: {
          borderRadius: 8,
          borderColor: surface,
          borderWidth: 3,
        },
        label: {
          color: ink,
          formatter: '{b}\n{d}%',
          lineHeight: 16,
        },
        labelLine: {
          lineStyle: {
            color: inkSoft,
          },
        },
        data: chartItems.value.map((item) => ({
          name: item.category,
          value: item.value,
        })),
      },
    ],
  }

  chart.setOption(option)
}

const handleResize = () => {
  chart?.resize()
}

watch(
  () => [props.title, props.items, themeTick.value],
  () => {
    renderChart()
  },
  { deep: true },
)

onMounted(() => {
  renderChart()
  window.addEventListener('resize', handleResize)
  themeObserver = new MutationObserver(() => {
    themeTick.value += 1
  })
  themeObserver.observe(document.documentElement, {
    attributes: true,
    attributeFilter: ['data-theme'],
  })
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  themeObserver?.disconnect()
  chart?.dispose()
})
</script>

<template>
  <article class="category-pie-card">
    <header class="category-pie-card__head">
      <div>
        <h3>{{ title }}</h3>
        <p>共 {{ total }} 项</p>
      </div>
    </header>

    <div v-if="hasData" ref="chartRef" class="category-pie-card__chart" />
    <div v-else class="category-pie-card__empty">
      <strong>暂无数据</strong>
      <span>还没有形成可展示的分类分布。</span>
    </div>
  </article>
</template>

<style scoped>
.category-pie-card {
  display: grid;
  gap: 14px;
  min-height: 420px;
  padding: 16px;
  border: 1px solid var(--sl-line);
  border-radius: 22px;
  background: var(--sl-soft-panel-bg);
}

.category-pie-card__head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.category-pie-card__head h3,
.category-pie-card__head p {
  margin: 0;
}

.category-pie-card__head h3 {
  font-size: 1.08rem;
}

.category-pie-card__head p {
  margin-top: 6px;
  color: var(--sl-ink-soft);
  font-size: 0.92rem;
}

.category-pie-card__chart {
  min-height: 330px;
}

.category-pie-card__empty {
  display: grid;
  place-items: center;
  align-content: center;
  gap: 8px;
  min-height: 330px;
  border-radius: 18px;
  border: 1px dashed var(--sl-line);
  color: var(--sl-ink-soft);
  text-align: center;
}

.category-pie-card__empty strong {
  color: var(--sl-ink);
}
</style>
