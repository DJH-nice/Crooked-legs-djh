<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { axiosInstance } from '@halo-dev/api-client'
import { Toast } from '@halo-dev/components'
import { useRouter } from 'vue-router'
import { apiPaths } from '../../api/paths'

/** 每日统计数据接口 */
interface DailyStat {
  date: string
  added: number
  mastered: number
}

/** 热力图展示的天数 */
const TOTAL_DAYS = 365

const loading = ref(true)
const allStats = ref<DailyStat[]>([])
const router = useRouter()

const monthNames = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月']

/** 生成 365 天空网格，每格初始值为 0 */
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

/** 从 API 获取每日统计数据 */
async function fetchStats() {
  loading.value = true
  try {
    const resp = await axiosInstance.get(apiPaths.dailyStats)
    allStats.value = resp.data
  } catch (e) {
    console.error('Failed to fetch stats', e)
    Toast.error('加载统计数据失败')
  } finally {
    loading.value = false
  }
}

/**
 * 根据做题数量返回热力等级（0-4）
 * @param count 当日新增题目数
 */
function getLevel(count: number): number {
  if (count === 0) return 0
  if (count === 1) return 1
  if (count <= 2) return 2
  if (count <= 4) return 3
  return 4
}

/** 将 API 数据合并到网格中 */
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

// ====== 统计指标 ======
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
const masteryRate = computed(() =>
  totalAdded.value > 0 ? Math.round((totalMastered.value / totalAdded.value) * 100) : 0
)

/** 按周分组，用于热力图列渲染 */
const weeks = computed(() => {
  const result: { value: number; level: number; date: string; mastered: number }[][] = []
  const cells = mergedCells.value
  const firstDate = new Date(cells[0]?.date || '')
  const startDay = firstDate.getDay()
  let week: typeof cells = []
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
  if (week.length > 0) result.push(week)
  return result
})

/** 生成月份标签位置 */
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

// ====== 热力提示框 ======
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
  <div class="stats-record min-h-screen" style="background: linear-gradient(135deg, #1a103c 0%, #0f0c29 50%, #0a0518 100%)">
    <!-- ===== 顶栏 ===== -->
    <header class="sticky top-0 z-50 glass border-b border-white/10" style="backdrop-filter: blur(20px);">
      <div class="max-w-5xl mx-auto px-4 sm:px-6">
        <div class="flex items-center justify-between h-16">
          <button @click="router.push('/mistakes')" class="glass-btn-secondary glass-btn-sm flex items-center gap-2">
            <span>←</span>
            <span class="hidden sm:inline">返回错题本</span>
          </button>
          <h1 class="text-lg font-bold text-white">📊 学习统计</h1>
          <div class="w-20"></div>
        </div>
      </div>
    </header>

    <!-- ===== 加载状态 ===== -->
    <div v-if="loading" class="flex justify-center py-20">
      <div class="flex flex-col items-center gap-3">
        <div class="w-10 h-10 border-2 border-purple-500/30 border-t-purple-500 rounded-full animate-spin"></div>
        <span class="text-sm text-gray-400">加载统计数据...</span>
      </div>
    </div>

    <div v-else class="max-w-5xl mx-auto px-4 sm:px-6 py-6 space-y-6">
      <!-- ===== 统计卡片 ===== -->
      <div class="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <!-- 总记录数 -->
        <div class="glass-stat animate-fade-in-up animate-delay-100">
          <div class="flex items-center justify-between mb-2">
            <span class="text-sm text-gray-400">总记录数</span>
            <div class="w-8 h-8 rounded-lg bg-purple-500/20 flex items-center justify-center text-sm glow-purple" style="color:#a855f7">📚</div>
          </div>
          <div class="text-3xl font-bold text-white">{{ totalAdded }}</div>
          <div class="text-xs text-gray-500 mt-1">过去一年累计</div>
        </div>

        <!-- 已掌握 -->
        <div class="glass-stat animate-fade-in-up animate-delay-200">
          <div class="flex items-center justify-between mb-2">
            <span class="text-sm text-gray-400">已掌握</span>
            <div class="w-8 h-8 rounded-lg bg-emerald-500/20 flex items-center justify-center text-sm glow-emerald" style="color:#10b981">🎯</div>
          </div>
          <div class="text-3xl font-bold text-white">{{ totalMastered }}</div>
          <div class="text-xs text-gray-500 mt-1">{{ masteryRate }}% 掌握率</div>
        </div>

        <!-- 掌握率 -->
        <div class="glass-stat animate-fade-in-up animate-delay-300">
          <div class="flex items-center justify-between mb-2">
            <span class="text-sm text-gray-400">掌握率</span>
            <div class="w-8 h-8 rounded-lg bg-cyan-500/20 flex items-center justify-center text-sm glow-cyan" style="color:#06b6d4">📈</div>
          </div>
          <div class="text-3xl font-bold text-white">{{ masteryRate }}<span class="text-lg text-gray-400">%</span></div>
          <div class="h-1.5 rounded-full bg-white/5 mt-2 overflow-hidden">
            <div class="h-full rounded-full bg-gradient-to-r from-cyan-500 to-cyan-400" :style="{ width: masteryRate + '%' }"></div>
          </div>
        </div>

        <!-- 连续打卡 -->
        <div class="glass-stat animate-fade-in-up animate-delay-400">
          <div class="flex items-center justify-between mb-2">
            <span class="text-sm text-gray-400">连续打卡</span>
            <div class="w-8 h-8 rounded-lg bg-amber-500/20 flex items-center justify-center text-sm" style="color:#f59e0b">🔥</div>
          </div>
          <div class="text-3xl font-bold text-white">{{ currentStreak }}</div>
          <div class="text-xs text-gray-500 mt-1">天</div>
        </div>
      </div>

      <!-- ===== 学习热力图 ===== -->
      <div class="glass-card animate-fade-in-up animate-delay-500">
        <div class="flex items-center justify-between mb-6">
          <div>
            <h2 class="text-base font-semibold text-white">学习热力图</h2>
            <p class="text-xs text-gray-500 mt-0.5">{{ totalAdded }} 条记录（过去 365 天）</p>
          </div>
          <!-- 图例 -->
          <div class="flex items-center gap-1.5">
            <span class="text-xs text-gray-500">少</span>
            <div class="w-3 h-3 rounded-sm" style="background: rgba(255,255,255,0.06)"></div>
            <div class="w-3 h-3 rounded-sm" style="background: rgba(16,185,129,0.3)"></div>
            <div class="w-3 h-3 rounded-sm" style="background: rgba(16,185,129,0.5)"></div>
            <div class="w-3 h-3 rounded-sm" style="background: rgba(16,185,129,0.7)"></div>
            <div class="w-3 h-3 rounded-sm" style="background: rgba(16,185,129,0.9)"></div>
            <span class="text-xs text-gray-500">多</span>
          </div>
        </div>

        <div class="overflow-x-auto pb-2">
          <div class="inline-block">
            <!-- 月份标签 -->
            <div class="flex mb-1 ml-8 gap-px">
              <span
                v-for="(label, i) in monthLabels"
                :key="i"
                class="text-[10px] text-gray-500"
                :style="{ minWidth: '14px', marginLeft: label.startCol > 0 ? (label.startCol - (monthLabels[i-1]?.startCol || 0) - 1) * 17 + 'px' : '0' }"
              >{{ label.name }}</span>
            </div>

            <!-- 热力网格 -->
            <div class="flex gap-px">
              <!-- 星期标签 -->
              <div class="flex flex-col gap-px mr-1">
                <span class="text-[8px] text-gray-600 h-3.5 flex items-center"></span>
                <span class="text-[8px] text-gray-600 h-3.5 flex items-center">一</span>
                <span class="text-[8px] text-gray-600 h-3.5 flex items-center"></span>
                <span class="text-[8px] text-gray-600 h-3.5 flex items-center">三</span>
                <span class="text-[8px] text-gray-600 h-3.5 flex items-center"></span>
                <span class="text-[8px] text-gray-600 h-3.5 flex items-center">五</span>
                <span class="text-[8px] text-gray-600 h-3.5 flex items-center"></span>
              </div>

              <!-- 数据列 -->
              <div
                v-for="(week, wi) in weeks"
                :key="wi"
                class="flex flex-col gap-px"
              >
                <div
                  v-for="(cell, di) in week"
                  :key="di"
                  class="w-3.5 h-3.5 rounded-sm cursor-pointer transition-all hover:ring-1 hover:ring-white/30"
                  :class="cell.level >= 0 ? '' : 'opacity-0'"
                  :style="{
                    background: cell.level === 0 ? 'rgba(255,255,255,0.04)' :
                              cell.level === 1 ? 'rgba(16,185,129,0.25)' :
                              cell.level === 2 ? 'rgba(16,185,129,0.45)' :
                              cell.level === 3 ? 'rgba(16,185,129,0.65)' :
                              cell.level === 4 ? 'rgba(16,185,129,0.9)' :
                              'transparent'
                  }"
                  @mouseenter="showTooltip(cell, $event)"
                  @mouseleave="hideTooltip"
                ></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ===== 悬浮提示 ===== -->
    <Teleport to="body">
      <div
        v-if="tooltip.visible"
        class="fixed z-[999] pointer-events-none"
        :style="{ left: tooltip.x + 'px', top: tooltip.y + 'px', transform: 'translate(-50%, -100%)' }"
      >
        <div class="glass rounded-lg px-3 py-2 text-xs text-gray-200 whitespace-pre text-center leading-relaxed">
          {{ tooltip.text }}
        </div>
      </div>
    </Teleport>
  </div>
</template>
