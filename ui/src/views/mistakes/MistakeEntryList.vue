<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import { axiosInstance } from '@halo-dev/api-client'
import { Toast } from '@halo-dev/components'
import type { MistakeEntry } from '../types'
import MistakeEntryForm from './MistakeEntryForm.vue'
import {
  statusConfig,
  nextStatus,
  difficultyLabels,
  difficultyColors,
  subjects,
  difficulties,
  filterStatuses,
  filterDifficulties,
  getSubjectIcon,
  masteryBarColor,
  masteryProgress,
  timeAgo,
} from './constants'
import { apiPaths } from '../../api/paths'

const router = useRouter()

// ====== 状态 ======
const loading = ref(false)
const entries = ref<MistakeEntry[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)

const searchQuery = ref('')
const selectedSubject = ref<string | undefined>()
const selectedDifficulty = ref<string | undefined>()
const selectedStatus = ref<string | undefined>()

const editingEntry = ref<MistakeEntry | undefined>()
const showFormModal = ref(false)

/** 统计仪表盘：总数 / 今日复习 / 掌握率 / 薄弱科目 */
const stats = computed(() => {
  const all = entries.value
  const totalCount = total.value
  const today = new Date().toISOString().split('T')[0]
  const todayCount = all.filter(e => {
    const ts = e.metadata?.creationTimestamp
    return ts && ts.startsWith(today)
  }).length
  const masteredCount = all.filter(e => e.spec?.status === 'mastered').length
  const masteryRate = totalCount > 0 ? Math.round((masteredCount / totalCount) * 100) : 0

  // 薄弱知识点：出现最多但掌握率最低的科目
  const subjectMap = new Map<string, { total: number; mastered: number }>()
  for (const e of all) {
    const sub = e.spec?.subject || '未分类'
    const s = subjectMap.get(sub) || { total: 0, mastered: 0 }
    s.total++
    if (e.spec?.status === 'mastered') s.mastered++
    subjectMap.set(sub, s)
  }
  let weakest = '—'
  let weakestRate = 100
  for (const [sub, s] of subjectMap) {
    const rate = s.total > 0 ? (s.mastered / s.total) * 100 : 0
    if (rate < weakestRate && s.total >= 1) {
      weakestRate = rate
      weakest = sub
    }
  }

  return { totalCount, todayCount, masteryRate, weakest, masteredCount }
})

/** 从 API 获取错题列表（分页 + 科目/状态筛选） */
async function fetchEntries() {
  loading.value = true
  try {
    const params: Record<string, unknown> = { page: page.value, size: size.value }
    if (selectedSubject.value) params.subject = selectedSubject.value
    if (selectedStatus.value) params.status = selectedStatus.value

    const response = await axiosInstance.get(apiPaths.list, { params })
    const data = response.data
    entries.value = data.items || []
    total.value = data.total || 0
  } catch (error: unknown) {
    console.error('获取错题列表失败:', error)
    Toast.error('获取错题列表失败')
  } finally {
    loading.value = false
  }
}

/** 删除单条错题 */
async function handleDelete(entry: MistakeEntry) {
  const name = entry.metadata?.name
  if (!name) return
  try {
    await axiosInstance.delete(apiPaths.detail(name))
    Toast.success('删除成功')
    await fetchEntries()
  } catch (error: unknown) {
    console.error('删除失败:', error)
    Toast.error('删除失败')
  }
}

/** 循环切换状态：未掌握 → 复习中 → 已掌握 → 未掌握 */
async function toggleStatus(entry: MistakeEntry) {
  const name = entry.metadata?.name
  if (!name || !entry.spec) return
  const current = entry.spec.status || 'unmastered'
  const next = nextStatus(current)
  try {
    await axiosInstance.patch(apiPaths.status(name), { status: next })
    entry.spec.status = next
  } catch (error: unknown) {
    console.error('状态更新失败:', error)
    Toast.error('状态更新失败')
  }
}

/* ====== 路由操作 ====== */

/** 跳转详情页 */
function viewDetail(entry: MistakeEntry) {
  router.push({ name: 'MistakeEntryDetail', params: { name: entry.metadata?.name } })
}

/** 打开编辑弹窗 */
function handleEdit(entry: MistakeEntry) {
  editingEntry.value = entry
  showFormModal.value = true
}

/** 打开新建弹窗 */
function handleCreate() {
  editingEntry.value = undefined
  showFormModal.value = true
}

/** 表单提交成功回调：关闭弹窗 → 刷新列表 */
function handleFormSuccess() {
  showFormModal.value = false
  editingEntry.value = undefined
  fetchEntries()
}

/** 跳转统计页 */
function goStats() {
  router.push('/mistakes/stats')
}

// 过滤后的条目（前端搜索过滤）
const filteredEntries = computed(() => {
  let list = entries.value
  if (searchQuery.value.trim()) {
    const q = searchQuery.value.trim().toLowerCase()
    list = list.filter(e =>
      e.spec?.question?.toLowerCase().includes(q) ||
      e.spec?.subject?.toLowerCase().includes(q) ||
      e.spec?.knowledgePoint?.toLowerCase().includes(q) ||
      e.spec?.tags?.some(t => t.toLowerCase().includes(q))
    )
  }
  if (selectedDifficulty.value) {
    list = list.filter(e => e.spec?.difficulty === selectedDifficulty.value)
  }
  return list
})

watch([selectedSubject, selectedStatus], () => {
  page.value = 1
  fetchEntries()
})

onMounted(() => {
  fetchEntries()
})
</script>

<template>
  <div class="mistake-list min-h-screen" style="background: linear-gradient(135deg, #1a103c 0%, #0f0c29 50%, #0a0518 100%)">
    <!-- ===== Header ===== -->
    <header class="sticky top-0 z-50 glass border-b border-white/10" style="backdrop-filter: blur(20px);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex items-center justify-between h-16">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-purple-500 to-purple-700 flex items-center justify-center text-white font-bold text-lg glow-purple">
              M
            </div>
            <div>
              <h1 class="text-lg font-bold text-white">我的错题本</h1>
              <p class="text-xs text-gray-400">Mistake Notebook</p>
            </div>
          </div>

          <div class="flex items-center gap-3">
            <button @click="goStats" class="glass-btn-secondary glass-btn-sm hidden sm:flex items-center gap-1.5">
              <span>📊</span>
              <span>统计</span>
            </button>
            <button @click="handleCreate" class="glass-btn-primary glass-btn-sm flex items-center gap-1.5 animate-glow">
              <span class="text-lg leading-none">+</span>
              <span class="hidden sm:inline">录入错题</span>
            </button>
          </div>
        </div>
      </div>
    </header>

    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 space-y-6">
      <!-- ===== Stats Dashboard ===== -->
      <div class="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <div class="glass-stat animate-fade-in-up animate-delay-100">
          <div class="flex items-start justify-between mb-3">
            <span class="text-sm text-gray-400">错题总数</span>
            <div class="w-9 h-9 rounded-lg bg-purple-500/20 flex items-center justify-center text-lg glow-purple" style="color:#a855f7">📚</div>
          </div>
          <div class="text-3xl font-bold text-white mb-1">{{ stats.totalCount }}</div>
          <div class="h-1 rounded-full bg-white/5 overflow-hidden">
            <div class="h-full w-full rounded-full bg-gradient-to-r from-purple-500 to-purple-400 opacity-60"></div>
          </div>
        </div>

        <div class="glass-stat animate-fade-in-up animate-delay-200">
          <div class="flex items-start justify-between mb-3">
            <span class="text-sm text-gray-400">今日复习</span>
            <div class="w-9 h-9 rounded-lg bg-cyan-500/20 flex items-center justify-center text-lg glow-cyan" style="color:#06b6d4">📖</div>
          </div>
          <div class="text-3xl font-bold text-white mb-1">{{ stats.todayCount }}</div>
          <div class="h-1 rounded-full bg-white/5 overflow-hidden">
            <div class="h-full rounded-full bg-gradient-to-r from-cyan-500 to-cyan-400" :style="{ width: Math.min(stats.todayCount * 10, 100) + '%' }"></div>
          </div>
        </div>

        <div class="glass-stat animate-fade-in-up animate-delay-300">
          <div class="flex items-start justify-between mb-3">
            <span class="text-sm text-gray-400">掌握率</span>
            <div class="w-9 h-9 rounded-lg bg-emerald-500/20 flex items-center justify-center text-lg glow-emerald" style="color:#10b981">🎯</div>
          </div>
          <div class="text-3xl font-bold text-white mb-1">{{ stats.masteryRate }}<span class="text-lg text-gray-400">%</span></div>
          <div class="h-1 rounded-full bg-white/5 overflow-hidden">
            <div class="h-full rounded-full bg-gradient-to-r from-emerald-500 to-emerald-400" :style="{ width: stats.masteryRate + '%' }"></div>
          </div>
        </div>

        <div class="glass-stat animate-fade-in-up animate-delay-400">
          <div class="flex items-start justify-between mb-3">
            <span class="text-sm text-gray-400">薄弱知识点</span>
            <div class="w-9 h-9 rounded-lg bg-rose-500/20 flex items-center justify-center text-lg glow-coral" style="color:#f43f5e">⚠</div>
          </div>
          <div class="text-xl font-bold text-white mb-1 truncate">{{ stats.weakest }}</div>
          <div class="h-1 rounded-full bg-white/5 overflow-hidden">
            <div class="h-full rounded-full bg-gradient-to-r from-rose-500 to-rose-400" style="width: 30%"></div>
          </div>
        </div>
      </div>

      <!-- ===== Filter Bar ===== -->
      <div class="glass rounded-2xl p-4 animate-fade-in-up animate-delay-500">
        <div class="flex flex-wrap items-center gap-3">
          <div class="relative flex-1 min-w-[200px]">
            <span class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500">🔍</span>
            <input
              v-model="searchQuery"
              type="text"
              placeholder="搜索题目、科目、知识点..."
              class="glass-input w-full pl-10 pr-4"
            />
          </div>

          <select v-model="selectedSubject" class="glass-select min-w-[120px]">
            <option :value="undefined">全部科目</option>
            <option v-for="s in subjects" :key="s" :value="s">{{ s }}</option>
          </select>

          <select v-model="selectedDifficulty" class="glass-select min-w-[110px]">
            <option v-for="d in filterDifficulties" :key="String(d.value)" :value="d.value">{{ d.label }}</option>
          </select>

          <select v-model="selectedStatus" class="glass-select min-w-[110px]">
            <option v-for="s in filterStatuses" :key="String(s.value)" :value="s.value">{{ s.label }}</option>
          </select>

          <div class="text-sm text-gray-500 whitespace-nowrap">
            共 <span class="text-white font-medium">{{ filteredEntries.length }}</span> 条
          </div>
        </div>
      </div>

      <!-- ===== Loading ===== -->
      <div v-if="loading" class="flex justify-center py-20">
        <div class="flex flex-col items-center gap-3">
          <div class="w-10 h-10 border-2 border-purple-500/30 border-t-purple-500 rounded-full animate-spin"></div>
          <span class="text-sm text-gray-400">加载中...</span>
        </div>
      </div>

      <!-- ===== Empty State ===== -->
      <div v-else-if="filteredEntries.length === 0" class="glass rounded-2xl p-12 text-center animate-fade-in-up">
        <div class="text-5xl mb-4">📝</div>
        <h3 class="text-xl font-semibold text-white mb-2">还没有错题记录</h3>
        <p class="text-gray-400 mb-6">点击右上角「录入错题」开始记录你的第一道错题吧！</p>
        <button @click="handleCreate" class="glass-btn-primary">
          + 录入第一道错题
        </button>
      </div>

      <!-- ===== Card Grid ===== -->
      <div v-else class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
        <TransitionGroup name="card">
          <div
            v-for="(entry, index) in filteredEntries"
            :key="entry.metadata?.name"
            class="glass-card cursor-pointer group"
            :style="{ animationDelay: (index % 9) * 0.07 + 's' }"
            @click="viewDetail(entry)"
          >
            <div class="flex items-start justify-between mb-3">
              <div class="flex items-center gap-2">
                <div class="w-9 h-9 rounded-lg bg-white/5 flex items-center justify-center text-lg group-hover:bg-purple-500/20 transition-colors">
                  {{ getSubjectIcon(entry.spec?.subject) }}
                </div>
                <div>
                  <span class="text-sm font-medium text-gray-300">{{ entry.spec?.subject || '未分类' }}</span>
                  <div class="flex items-center gap-1.5 mt-0.5">
                    <span
                      v-if="entry.spec?.status"
                      :class="statusConfig[entry.spec.status]?.badgeClass || 'badge-unmastered'"
                      class="text-xs px-2 py-0.5 rounded-full inline-flex items-center gap-1"
                      @click.stop="toggleStatus(entry)"
                    >
                      <span class="text-[8px]">{{ statusConfig[entry.spec.status]?.icon }}</span>
                      {{ statusConfig[entry.spec.status]?.label }}
                    </span>
                    <span
                      v-if="entry.spec?.difficulty"
                      :class="difficultyColors[entry.spec.difficulty] || 'text-gray-400'"
                      class="text-xs px-2 py-0.5 rounded-full border inline-flex items-center"
                    >
                      {{ difficultyLabels[entry.spec.difficulty] || entry.spec.difficulty }}
                    </span>
                  </div>
                </div>
              </div>

              <div class="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity" @click.stop>
                <button @click="handleEdit(entry)" class="w-7 h-7 rounded-lg bg-white/5 hover:bg-white/10 flex items-center justify-center text-xs text-gray-400 hover:text-white transition-all">✎</button>
                <button @click="handleDelete(entry)" class="w-7 h-7 rounded-lg bg-white/5 hover:bg-rose-500/20 flex items-center justify-center text-xs text-gray-400 hover:text-rose-400 transition-all">✕</button>
              </div>
            </div>

            <p class="text-sm text-gray-300 mb-3 line-clamp-2 leading-relaxed">
              {{ entry.spec?.question || '无题目' }}
            </p>

            <div class="flex flex-wrap gap-1.5 mb-3" v-if="entry.spec?.tags && entry.spec.tags.length > 0">
              <span
                v-for="tag in entry.spec.tags.slice(0, 4)"
                :key="tag"
                class="text-xs px-2 py-0.5 rounded-full bg-white/5 text-gray-400 border border-white/5"
              >#{{ tag }}</span>
              <span v-if="entry.spec.tags.length > 4" class="text-xs px-2 py-0.5 text-gray-500">
                +{{ entry.spec.tags.length - 4 }}
              </span>
            </div>

            <div v-if="entry.spec?.knowledgePoint" class="text-xs text-purple-400/70 mb-3 flex items-center gap-1">
              <span>📌</span>
              <span class="truncate">{{ entry.spec.knowledgePoint }}</span>
            </div>

            <div class="pt-3 border-t border-white/5">
              <div class="flex items-center justify-between mb-2">
                <span class="text-xs text-gray-500">
                  {{ entry.metadata?.creationTimestamp ? timeAgo(entry.metadata.creationTimestamp) + ' 记录' : '' }}
                </span>
                <span class="text-xs text-gray-500">
                  {{ entry.spec?.status === 'mastered' ? '已掌握' : entry.spec?.status === 'reviewing' ? '复习中' : '待学习' }}
                </span>
              </div>
              <div class="h-1.5 rounded-full bg-white/5 overflow-hidden">
                <div
                  :class="masteryBarColor(entry.spec?.status)"
                  class="h-full rounded-full transition-all duration-500"
                  :style="{ width: masteryProgress(entry.spec?.status) + '%' }"
                ></div>
              </div>
            </div>
          </div>
        </TransitionGroup>
      </div>

      <!-- ===== Pagination ===== -->
      <div v-if="total > size" class="flex items-center justify-center gap-2 py-4">
        <button
          @click="page > 1 && (page--, fetchEntries())"
          :disabled="page <= 1"
          class="glass-btn-secondary glass-btn-sm disabled:opacity-30"
        >上一页</button>
        <span class="text-sm text-gray-400 px-3">
          {{ page }} / {{ Math.ceil(total / size) }}
        </span>
        <button
          @click="page < Math.ceil(total / size) && (page++, fetchEntries())"
          :disabled="page >= Math.ceil(total / size)"
          class="glass-btn-secondary glass-btn-sm disabled:opacity-30"
        >下一页</button>
      </div>
    </div>

    <!-- ===== Form Modal ===== -->
    <Teleport to="body">
      <div v-if="showFormModal" class="fixed inset-0 z-[100] flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" @click="showFormModal = false"></div>
        <div class="relative w-full max-w-2xl max-h-[90vh] overflow-y-auto glass rounded-2xl p-6" @click.stop>
          <div class="flex items-center justify-between mb-6">
            <h2 class="text-lg font-semibold text-white">{{ editingEntry ? '编辑错题' : '录入错题' }}</h2>
            <button @click="showFormModal = false" class="w-8 h-8 rounded-lg bg-white/5 hover:bg-white/10 flex items-center justify-center text-gray-400 hover:text-white transition-all">✕</button>
          </div>
          <MistakeEntryForm
            :entry="editingEntry"
            @success="handleFormSuccess"
            @cancel="showFormModal = false"
          />
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.card-enter-active {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}
.card-enter-from {
  opacity: 0;
  transform: translateY(20px);
}
</style>
