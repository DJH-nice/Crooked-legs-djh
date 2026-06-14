<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { axiosInstance } from '@halo-dev/api-client'
import { VPageHeader, VButton, VLoading, VCard, Toast, IconArrowLeft } from '@halo-dev/components'
import { useRouter } from 'vue-router'

interface DailyStat {
  date: string
  added: number
  mastered: number
}

const TOTAL_DAYS = 365

const days: { value: number; level: number; date: string; mastered: number }[] = []
const loading = ref(true)
const allStats = ref<DailyStat[]>([])
const router = useRouter()

// 月份名称
const monthNames = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月']

// 填充空日期网格（365 天）
const grid = computed(() => {
  const today = new Date()
  const start = new Date(today)
  start.setDate(start.getDate() - TOTAL_DAYS + 1)

  const cells: { value: number; level: number; date: string; mastered: number }[] = []
  for (let i = 0; i < TOTAL_DAYS; i++) {
    const d = new Date(start)
    d.setDate(d.getDate() + i)
    const ds = d.toISOString().split('T')[0]
    cells.push({ value: 0, level: 0, date: ds, mastered: 0 })
  }
  return cells
})

// 加载统计数据
async function fetchStats() {
  loading.value = true
  try {
    const resp = await axiosInstance.get('/api/plugins/my-plugin/mistakes/stats/daily')
    allStats.value = resp.data
  } catch (e) {
    console.error('Failed to fetch stats', e)
    Toast.error('加载统计数据失败')
  } finally {
    loading.value = false
  }
}

// 合并统计数据到网格
const mergedCells = computed(() => {
  const statsMap = new Map<string, DailyStat>()
  for (const s of allStats.value) {
    statsMap.set(s.date, s)
  }
  return grid.value.map((cell) => {
    const s = statsMap.get(cell.date)
    if (s) {
      cell.value = s.added
      cell.mastered = s.mastered
      cell.level = getLevel(s.added)
    }
    return cell
  })
})

// 根据数值返回热力等级 0-4
function getLevel(count: number): number {
  if (count === 0) return 0
  if (count === 1) return 1
  if (count <= 2) return 2
  if (count <= 4) return 3
  return 4
}

// 统计总览
const totalAdded = computed(() => allStats.value.reduce((s, d) => s + d.added, 0))
const totalMastered = computed(() => allStats.value.reduce((s, d) => s + d.mastered, 0))
const currentStreak = computed(() => {
  let streak = 0
  const today = new Date().toISOString().split('T')[0]
  for (let i = allStats.value.length - 1; i >= 0; i--) {
    if (allStats.value[i].date > today) continue
    if (allStats.value[i].added > 0) streak++
    else break
  }
  return streak
})

// 按排分组的周视图
const weeks = computed(() => {
  const result: { value: number; level: number; date: string; mastered: number }[][] = []
  const cells = mergedCells.value
  // 找到起始日是周几
  const firstDate = new Date(cells[0]?.date || '')
  const startDay = firstDate.getDay() // 0=Sun

  let week: typeof cells = []

  // 补上前面的空位
  for (let i = 0; i < startDay; i++) {
    week.push({ value: -1, level: -1, date: '', mastered: 0 })
  }

  for (const cell of cells) {
    week.push(cell)
    if (week.length === 7) {
      result.push(week)
      week = []
    }
  }
  if (week.length > 0) {
    result.push(week)
  }
  return result
})

// 获取月份的列位置
const monthLabels = computed(() => {
  const labels: { name: string; startCol: number }[] = []
  let col = 0
  let lastMonth = -1
  for (const week of weeks.value) {
    if (week.length > 0) {
      const firstCellDate = week.find((c) => c.date)
      if (firstCellDate) {
        const m = new Date(firstCellDate.date).getMonth()
        if (m !== lastMonth) {
          labels.push({ name: monthNames[m], startCol: col })
          lastMonth = m
        }
      }
    }
    col++
  }
  return labels
})

// 工具提示
const tooltip = ref({ text: '', x: 0, y: 0, visible: false })
function showTooltip(cell: { value: number; date: string; mastered: number }, event: MouseEvent) {
  if (cell.value < 0) return
  const el = event.target as HTMLElement
  const rect = el.getBoundingClientRect()
  tooltip.value = {
    text: `${cell.date}\n新增 ${cell.value} 题 | 已掌握 ${cell.mastered}`,
    x: rect.left + rect.width / 2,
    y: rect.top - 8,
    visible: true,
  }
}
function hideTooltip() {
  tooltip.value.visible = false
}

onMounted(fetchStats)
</script>

<template>
  <div class="stats-record">
    <VPageHeader title="学习记录">
      <template #left>
        <VButton type="secondary" @click="router.push('/mistakes')">
          <template #icon><IconArrowLeft /></template>
          返回错题本
        </VButton>
      </template>
    </VPageHeader>

    <div v-if="loading" class="flex justify-center py-20">
      <VLoading />
    </div>

    <div v-else class="p-6 max-w-5xl mx-auto">
      <!-- 概览卡片 -->
      <div class="grid grid-cols-3 gap-4 mb-8">
        <VCard>
          <div class="text-center py-4">
            <div class="text-3xl font-bold text-blue-600">{{ totalAdded }}</div>
            <div class="text-sm text-gray-500 mt-1">总记录数</div>
          </div>
        </VCard>
        <VCard>
          <div class="text-center py-4">
            <div class="text-3xl font-bold text-green-600">{{ totalMastered }}</div>
            <div class="text-sm text-gray-500 mt-1">已掌握</div>
          </div>
        </VCard>
        <VCard>
          <div class="text-center py-4">
            <div class="text-3xl font-bold text-orange-500">{{ currentStreak }}</div>
            <div class="text-sm text-gray-500 mt-1">连续打卡天数</div>
          </div>
        </VCard>
      </div>

      <!-- 热力图 -->
      <VCard>
        <div class="flex items-center justify-between mb-4">
          <span class="text-sm text-gray-500">
            {{ totalAdded }} 条记录（过去 365 天）
          </span>
          <div class="flex items-center gap-1">
            <span class="text-xs text-gray-400">少</span>
            <div class="heatmap-legend level-0"></div>
            <div class="heatmap-legend level-1"></div>
            <div class="heatmap-legend level-2"></div>
            <div class="heatmap-legend level-3"></div>
            <div class="heatmap-legend level-4"></div>
            <span class="text-xs text-gray-400">多</span>
          </div>
        </div>

        <div class="overflow-x-auto">
          <div class="heatmap-grid">
            <!-- 月份标签行 -->
            <div class="month-labels">
              <span
                v-for="(label, i) in monthLabels"
                :key="i"
                :style="{ gridColumnStart: label.startCol + 1 }"
                class="month-label"
              >
                {{ label.name }}
              </span>
            </div>

            <!-- 热力图表格 -->
            <div class="heatmap-body">
              <div
                v-for="(week, wi) in weeks"
                :key="wi"
                class="heatmap-column"
              >
                <div
                  v-for="(cell, di) in week"
                  :key="di"
                  :class="['heatmap-cell', cell.level >= 0 ? `level-${cell.level}` : 'level-empty']"
                  @mouseenter="showTooltip(cell, $event)"
                  @mouseleave="hideTooltip"
                ></div>
              </div>
            </div>

            <!-- 星期标签 -->
            <div class="weekday-labels">
              <span></span><span>一</span><span></span><span>三</span><span></span><span>五</span><span></span>
            </div>
          </div>
        </div>
      </VCard>

      <!-- 浮动提示 -->
      <div
        v-if="tooltip.visible"
        class="tooltip-popup"
        :style="{ left: tooltip.x + 'px', top: tooltip.y + 'px' }"
      >
        <pre>{{ tooltip.text }}</pre>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.stats-record {
  min-height: 100vh;
  background: #f8fafc;
}
.heatmap-grid {
  display: inline-block;
}
.month-labels {
  display: flex;
  margin-left: 0;
  margin-bottom: 4px;
  span { font-size: 11px; color: #6b7280; min-width: 28px; }
}
.heatmap-body {
  display: flex;
  gap: 3px;
}
.heatmap-column {
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.heatmap-cell {
  width: 14px;
  height: 14px;
  border-radius: 3px;
  cursor: pointer;
  transition: outline 0.1s;
}
.heatmap-cell:hover { outline: 2px solid #3b82f6; outline-offset: 1px; }
.heatmap-cell.level-empty { background: transparent; }
.heatmap-cell.level-0 { background: #ebedf0; }
.heatmap-cell.level-1 { background: #9be9a8; }
.heatmap-cell.level-2 { background: #40c463; }
.heatmap-cell.level-3 { background: #30a14e; }
.heatmap-cell.level-4 { background: #216e39; }
.heatmap-legend {
  width: 14px; height: 14px; border-radius: 3px;
  &.level-0 { background: #ebedf0; }
  &.level-1 { background: #9be9a8; }
  &.level-2 { background: #40c463; }
  &.level-3 { background: #30a14e; }
  &.level-4 { background: #216e39; }
}
.weekday-labels {
  display: flex;
  margin-top: 4px;
  span { font-size: 10px; color: #9ca3af; min-width: 14px; text-align: center; }
}
.tooltip-popup {
  position: fixed;
  transform: translate(-50%, -100%);
  background: #1f2937;
  color: #f9fafb;
  padding: 6px 10px;
  border-radius: 6px;
  font-size: 12px;
  z-index: 999;
  pointer-events: none;
  white-space: pre;
  line-height: 1.5;
}
.flex { display: flex; }
.items-center { align-items: center; }
.justify-between { justify-content: space-between; }
.gap-4 { gap: 1rem; }
.mb-4 { margin-bottom: 1rem; }
.mb-8 { margin-bottom: 2rem; }
.w-full { width: 100%; }
</style>
