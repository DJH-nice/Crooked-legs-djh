<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { axiosInstance } from '@halo-dev/api-client'
import { Toast } from '@halo-dev/components'
import type { MistakeEntry } from '../types'
import MistakeEntryForm from './MistakeEntryForm.vue'
import {
  statusConfig,
  difficultyLabels,
  difficultyBgBorder,
  sourceMap,
  timeAgo,
} from './constants'
import { apiPaths } from '../../api/paths'

const route = useRoute()
const router = useRouter()

/** 加载状态 */
const loading = ref(true)
/** 当前查看的错题数据 */
const entry = ref<MistakeEntry | undefined>(undefined)
/** 编辑弹窗是否可见 */
const showEditModal = ref(false)

/** 从 API 获取错题详情 */
async function fetchDetail() {
  loading.value = true
  try {
    const name = route.params.name as string
    const response = await axiosInstance.get(apiPaths.detail(name))
    entry.value = response.data
  } catch (error: unknown) {
    console.error('获取错题详情失败:', error)
    Toast.error('获取错题详情失败')
  } finally {
    loading.value = false
  }
}

/** 删除当前错题 → 返回列表 */
async function handleDelete() {
  const name = entry.value?.metadata?.name
  if (!name) return
  try {
    await axiosInstance.delete(apiPaths.detail(name))
    Toast.success('删除成功')
    router.push('/mistakes')
  } catch (error: unknown) {
    console.error('删除失败:', error)
    Toast.error('删除失败')
  }
}

/** 返回列表页 */
function goBack() {
  router.push('/mistakes')
}

/** 编辑成功回调：关闭弹窗 → 刷新详情 */
function handleEditSuccess() {
  showEditModal.value = false
  fetchDetail()
}

onMounted(() => {
  fetchDetail()
})
</script>

<template>
  <div class="mistake-detail min-h-screen" style="background: linear-gradient(135deg, #1a103c 0%, #0f0c29 50%, #0a0518 100%)">
    <!-- Header -->
    <header class="sticky top-0 z-50 glass border-b border-white/10" style="backdrop-filter: blur(20px);">
      <div class="max-w-4xl mx-auto px-4 sm:px-6">
        <div class="flex items-center justify-between h-16">
          <button @click="goBack" class="glass-btn-secondary glass-btn-sm flex items-center gap-2">
            <span>←</span>
            <span class="hidden sm:inline">返回列表</span>
          </button>
          <div class="flex items-center gap-2">
            <button @click="showEditModal = true" class="glass-btn-secondary glass-btn-sm">✎ 编辑</button>
            <button @click="handleDelete" class="glass-btn-danger glass-btn-sm flex items-center gap-1">
              <span>🗑</span>
              <span class="hidden sm:inline">删除</span>
            </button>
          </div>
        </div>
      </div>
    </header>

    <!-- Content -->
    <div class="max-w-4xl mx-auto px-4 sm:px-6 py-6">
      <!-- Loading -->
      <div v-if="loading" class="flex justify-center py-20">
        <div class="flex flex-col items-center gap-3">
          <div class="w-10 h-10 border-2 border-purple-500/30 border-t-purple-500 rounded-full animate-spin"></div>
          <span class="text-sm text-gray-400">加载中...</span>
        </div>
      </div>

      <!-- Detail -->
      <div v-else-if="entry" class="space-y-6 animate-fade-in-up">
        <!-- Meta Tags -->
        <div class="flex flex-wrap items-center gap-2">
          <span class="glass-pill text-sm flex items-center gap-1.5">
            <span>{{ entry.spec?.subject || '未分类' }}</span>
          </span>
          <span
            v-if="entry.spec?.status"
            :class="statusConfig[entry.spec.status]?.badgeClass || 'badge-unmastered'"
            class="text-xs px-3 py-1 rounded-full inline-flex items-center gap-1.5"
          >
            <span class="text-[8px]">{{ statusConfig[entry.spec.status]?.icon }}</span>
            {{ statusConfig[entry.spec.status]?.label }}
          </span>
          <span
            v-if="entry.spec?.difficulty"
            class="text-xs px-3 py-1 rounded-full border"
            :class="difficultyBgBorder[entry.spec.difficulty] || 'text-gray-400'"
          >
            {{ difficultyLabels[entry.spec.difficulty] || entry.spec.difficulty }}
          </span>
          <span
            v-if="entry.spec?.source"
            class="text-xs px-3 py-1 rounded-full border border-cyan-500/30 bg-cyan-500/10 text-cyan-400"
          >
            {{ sourceMap[entry.spec.source] || '其他' }}
          </span>
          <span class="text-xs text-gray-500 ml-auto">
            {{ entry.metadata?.creationTimestamp ? new Date(entry.metadata.creationTimestamp).toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' }) : '' }}
          </span>
        </div>

        <!-- Question -->
        <div class="glass-card">
          <div class="flex items-center gap-2 mb-4">
            <div class="w-8 h-8 rounded-lg bg-purple-500/20 flex items-center justify-center text-sm glow-purple" style="color:#a855f7">📝</div>
            <h2 class="text-lg font-semibold text-white">题目</h2>
          </div>
          <div class="text-gray-200 whitespace-pre-wrap leading-relaxed text-base pl-10">
            {{ entry.spec?.question || '无题目' }}
          </div>
        </div>

        <!-- Wrong vs Correct (side by side) -->
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div class="glass-card" style="border-color: rgba(244, 63, 94, 0.2);">
            <div class="flex items-center gap-2 mb-3">
              <div class="w-8 h-8 rounded-lg bg-rose-500/20 flex items-center justify-center text-sm glow-coral" style="color:#f43f5e">✕</div>
              <h3 class="font-semibold text-rose-400">错误答案</h3>
            </div>
            <div class="text-gray-300 whitespace-pre-wrap leading-relaxed pl-10 text-sm">
              {{ entry.spec?.wrongAnswer || '未填写' }}
            </div>
          </div>
          <div class="glass-card" style="border-color: rgba(16, 185, 129, 0.2);">
            <div class="flex items-center gap-2 mb-3">
              <div class="w-8 h-8 rounded-lg bg-emerald-500/20 flex items-center justify-center text-sm glow-emerald" style="color:#10b981">✓</div>
              <h3 class="font-semibold text-emerald-400">正确答案</h3>
            </div>
            <div class="text-gray-300 whitespace-pre-wrap leading-relaxed pl-10 text-sm">
              {{ entry.spec?.correctAnswer || '未填写' }}
            </div>
          </div>
        </div>

        <!-- Analysis -->
        <div v-if="entry.spec?.analysis" class="glass-card">
          <div class="flex items-center gap-2 mb-4">
            <div class="w-8 h-8 rounded-lg bg-amber-500/20 flex items-center justify-center text-sm" style="color:#f59e0b">📖</div>
            <h2 class="text-lg font-semibold text-white">错题分析</h2>
          </div>
          <div class="text-gray-300 whitespace-pre-wrap leading-relaxed pl-10 text-sm">
            {{ entry.spec.analysis }}
          </div>
        </div>

        <!-- Knowledge Point + Tags (side by side) -->
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div v-if="entry.spec?.knowledgePoint" class="glass-card">
            <div class="flex items-center gap-2 mb-3">
              <div class="w-8 h-8 rounded-lg bg-cyan-500/20 flex items-center justify-center text-sm glow-cyan" style="color:#06b6d4">📌</div>
              <h3 class="font-semibold text-gray-200">知识点</h3>
            </div>
            <p class="text-gray-300 pl-10 text-sm">{{ entry.spec.knowledgePoint }}</p>
          </div>
          <div v-if="entry.spec?.tags && entry.spec.tags.length > 0" class="glass-card">
            <div class="flex items-center gap-2 mb-3">
              <div class="w-8 h-8 rounded-lg bg-purple-500/20 flex items-center justify-center text-sm glow-purple" style="color:#a855f7">🏷</div>
              <h3 class="font-semibold text-gray-200">标签</h3>
            </div>
            <div class="flex flex-wrap gap-2 pl-10">
              <span
                v-for="tag in entry.spec.tags"
                :key="tag"
                class="text-xs px-3 py-1 rounded-full bg-purple-500/15 text-purple-300 border border-purple-500/25"
              >#{{ tag }}</span>
            </div>
          </div>
        </div>

        <!-- Remark -->
        <div v-if="entry.spec?.remark" class="glass-card">
          <div class="flex items-center gap-2 mb-3">
            <div class="w-8 h-8 rounded-lg bg-white/10 flex items-center justify-center text-sm">📋</div>
            <h3 class="font-semibold text-gray-200">备注</h3>
          </div>
          <p class="text-gray-400 pl-10 text-sm">{{ entry.spec.remark }}</p>
        </div>
      </div>

      <!-- Not Found -->
      <div v-else class="glass-card text-center py-12">
        <div class="text-4xl mb-4">🔍</div>
        <p class="text-gray-400">错题不存在或已被删除</p>
        <button @click="goBack" class="glass-btn-secondary glass-btn-sm mt-4">返回列表</button>
      </div>
    </div>

    <!-- Edit Modal -->
    <Teleport to="body">
      <div v-if="showEditModal" class="fixed inset-0 z-[100] flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" @click="showEditModal = false"></div>
        <div class="relative w-full max-w-2xl max-h-[90vh] overflow-y-auto glass rounded-2xl p-6" @click.stop>
          <div class="flex items-center justify-between mb-6">
            <h2 class="text-lg font-semibold text-white">编辑错题</h2>
            <button @click="showEditModal = false" class="w-8 h-8 rounded-lg bg-white/5 hover:bg-white/10 flex items-center justify-center text-gray-400 hover:text-white transition-all">✕</button>
          </div>
          <MistakeEntryForm :entry="entry" @success="handleEditSuccess" @cancel="showEditModal = false" />
        </div>
      </div>
    </Teleport>
  </div>
</template>
