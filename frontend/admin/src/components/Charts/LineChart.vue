<template>
  <div ref="chartRef" class="chart-container"></div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import { useThemeStore } from '@/stores/theme'

const props = defineProps({
  data: { type: Object, required: true },
  smooth: { type: Boolean, default: true },
  showArea: { type: Boolean, default: true }
})

const themeStore = useThemeStore()
const chartRef = ref()
let chart = null
let isDisposed = false

function initChart() {
  if (!chartRef.value || isDisposed) return
  chart = echarts.init(chartRef.value)
  updateChart()
}

function updateChart() {
  if (!chart || isDisposed) return
  
  const isDark = themeStore.isDark
  const textColor = isDark ? '#a1a1aa' : '#71717a'
  const gridColor = isDark ? '#333' : '#e5e7eb'
  
  const option = {
    backgroundColor: 'transparent',
    tooltip: { 
      trigger: 'axis',
      backgroundColor: isDark ? '#27272a' : '#ffffff',
      borderColor: isDark ? '#3f3f46' : '#e5e7eb',
      textStyle: { color: isDark ? '#fafafa' : '#18181b' }
    },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
    xAxis: {
      type: 'category',
      data: props.data.xAxis || [],
      axisLine: { lineStyle: { color: gridColor } },
      axisLabel: { color: textColor },
      boundaryGap: false
    },
    yAxis: {
      type: 'value',
      axisLine: { lineStyle: { color: gridColor } },
      axisLabel: { color: textColor },
      splitLine: { lineStyle: { color: gridColor } }
    },
    series: [{
      data: props.data.series || [],
      type: 'line',
      smooth: props.smooth,
      areaStyle: props.showArea ? {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(168, 85, 247, 0.3)' },
          { offset: 1, color: 'rgba(168, 85, 247, 0)' }
        ])
      } : undefined,
      lineStyle: { color: '#a855f7', width: 2 },
      itemStyle: { color: '#a855f7' },
      symbol: 'circle',
      symbolSize: 6
    }]
  }
  chart.setOption(option)
}

function handleResize() {
  if (chart && !isDisposed) {
    chart.resize()
  }
}

watch(() => props.data, () => {
  nextTick(updateChart)
}, { deep: true })

watch(() => themeStore.isDark, () => {
  nextTick(updateChart)
})

onMounted(() => {
  nextTick(initChart)
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  isDisposed = true
  window.removeEventListener('resize', handleResize)
  if (chart) {
    chart.dispose()
    chart = null
  }
})
</script>

<style scoped>
.chart-container {
  width: 100%;
  height: 300px;
}
</style>
