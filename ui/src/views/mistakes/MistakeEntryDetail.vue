<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { axiosInstance } from '@halo-dev/api-client'
import {
  VPageHeader,
  VButton,
  VStatusDot,
  VTag,
  VLoading,
  VCard,
  Toast,
  IconArrowLeft,
  IconDeleteBin,
} from '@halo-dev/components'
import type { MistakeEntry } from '../types'
import MistakeEntryForm from './MistakeEntryForm.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const entry = ref<MistakeEntry | undefined>(undefined)
const showEditModal = ref(false)

const statusMap: Record<string, { label: string; color: string }> = {
  unmastered: { label: '未掌握', color: 'red' },
  reviewing: { label: '复习中', color: 'orange' },
  mastered: { label: '已掌握', color: 'green' },
}

const difficultyMap: Record<string, { label: string; color: string }> = {
  easy: { label: '简单', color: 'green' },
  medium: { label: '中等', color: 'orange' },
  hard: { label: '困难', color: 'red' },
}

const sourceMap: Record<string, string> = {
  exam: '考试',
  homework: '作业',
  quiz: '测验',
  other: '其他',
}

async function fetchDetail() {
  loading.value = true
  try {
    const name = route.params.name as string
    const response = await axiosInstance.get(`/api/plugins/my-plugin/mistakes/${name}`)
    entry.value = response.data
  } catch (error) {
    console.error('获取错题详情失败:', error)
    Toast.error('获取错题详情失败')
  } finally {
    loading.value = false
  }
}

async function handleDelete() {
  const name = entry.value?.metadata?.name
  if (!name) return
  try {
    await axiosInstance.delete(`/api/plugins/my-plugin/mistakes/${name}`)
    Toast.success('删除成功')
    router.push('/mistakes')
  } catch (error) {
    console.error('删除失败:', error)
    Toast.error('删除失败')
  }
}

function goBack() {
  router.push('/mistakes')
}

function handleEditSuccess() {
  showEditModal.value = false
  fetchDetail()
}

onMounted(() => {
  fetchDetail()
})
</script>

<template>
  <div class="mistake-entry-detail">
    <!-- 页面头部 -->
    <VPageHeader title="错题详情">
      <template #left>
        <VButton type="secondary" @click="goBack">
          <template #icon><IconArrowLeft /></template>
          返回列表
        </VButton>
      </template>
      <template #right>
        <VButton type="secondary" @click="showEditModal = true">
          ✏️ 编辑
        </VButton>
        <VButton type="danger" @click="handleDelete">
          <template #icon><IconDeleteBin /></template>
          删除
        </VButton>
      </template>
    </VPageHeader>

    <!-- 加载状态 -->
    <div v-if="loading" class="flex justify-center py-20">
      <VLoading />
    </div>

    <!-- 错题详情内容 -->
    <div v-else-if="entry" class="p-6 max-w-4xl mx-auto">
      <!-- 元信息栏 -->
      <div class="flex items-center gap-3 mb-6">
        <VTag>{{ entry.spec?.subject || '未分类' }}</VTag>
        <VStatusDot
          v-if="entry.spec?.status"
          :color="statusMap[entry.spec.status]?.color || 'gray'"
          :text="statusMap[entry.spec.status]?.label || entry.spec.status"
        />
        <VTag
          v-if="entry.spec?.difficulty"
          :color="difficultyMap[entry.spec.difficulty]?.color"
        >
          {{ difficultyMap[entry.spec.difficulty]?.label || entry.spec.difficulty }}
        </VTag>
        <VTag v-if="entry.spec?.source" color="blue">
          {{ sourceMap[entry.spec.source] || '其他' }}
        </VTag>
      </div>

      <!-- 题目 -->
      <VCard class="mb-6">
        <template #header>
          <div class="font-semibold text-lg">📝 题目</div>
        </template>
        <div class="text-gray-900 whitespace-pre-wrap leading-relaxed">
          {{ entry.spec?.question || '无题目' }}
        </div>
      </VCard>

      <!-- 错误答案 vs 正确答案 对比 -->
      <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
        <VCard>
          <template #header>
            <div class="font-semibold text-lg text-red-600">❌ 错误答案</div>
          </template>
          <div class="text-gray-900 whitespace-pre-wrap leading-relaxed">
            {{ entry.spec?.wrongAnswer || '未填写' }}
          </div>
        </VCard>
        <VCard>
          <template #header>
            <div class="font-semibold text-lg text-green-600">✅ 正确答案</div>
          </template>
          <div class="text-gray-900 whitespace-pre-wrap leading-relaxed">
            {{ entry.spec?.correctAnswer || '未填写' }}
          </div>
        </VCard>
      </div>

      <!-- 错题分析 -->
      <VCard class="mb-6" v-if="entry.spec?.analysis">
        <template #header>
          <div class="font-semibold text-lg">📖 错题分析</div>
        </template>
        <div class="text-gray-900 whitespace-pre-wrap leading-relaxed">
          {{ entry.spec.analysis }}
        </div>
      </VCard>

      <!-- 知识点和标签 -->
      <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
        <VCard v-if="entry.spec?.knowledgePoint">
          <template #header>
            <div class="font-semibold text-lg">📌 知识点</div>
          </template>
          <div class="text-gray-900">{{ entry.spec.knowledgePoint }}</div>
        </VCard>
        <VCard v-if="entry.spec?.tags && entry.spec.tags.length > 0">
          <template #header>
            <div class="font-semibold text-lg">🏷️ 标签</div>
          </template>
          <div class="flex flex-wrap gap-2">
            <VTag v-for="tag in entry.spec.tags" :key="tag">{{ tag }}</VTag>
          </div>
        </VCard>
      </div>

      <!-- 备注 -->
      <VCard v-if="entry.spec?.remark" class="mb-6">
        <template #header>
          <div class="font-semibold text-lg">📋 备注</div>
        </template>
        <div class="text-gray-900 whitespace-pre-wrap leading-relaxed">
          {{ entry.spec.remark }}
        </div>
      </VCard>
    </div>

    <!-- 空状态 -->
    <div v-else class="flex justify-center py-20 text-gray-500">
      错题不存在或已被删除
    </div>

    <!-- 编辑弹窗 -->
    <VModal
      v-model:visible="showEditModal"
      :title="'编辑错题'"
      :width="800"
    >
      <MistakeEntryForm
        :entry="entry"
        @success="handleEditSuccess"
        @cancel="showEditModal = false"
      />
    </VModal>
  </div>
</template>

<style lang="scss" scoped>
.mistake-entry-detail {
  min-height: 100vh;
  background-color: #f8fafc;
}
</style>
