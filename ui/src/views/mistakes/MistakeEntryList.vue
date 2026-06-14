<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { axiosInstance } from '@halo-dev/api-client'
import {
  VPageHeader,
  VButton,
  VModal,
  VStatusDot,
  VTag,
  VEmpty,
  VLoading,
  VPagination,
  VCard,
  VDropdown,
  VDropdownItem,
  IconMore,
  IconAddCircle,
  IconDeleteBin,
  Toast,
} from '@halo-dev/components'
import type { MistakeEntry } from '../types'
import MistakeEntryForm from './MistakeEntryForm.vue'

const router = useRouter()

// 状态
const loading = ref(false)
const entries = ref<MistakeEntry[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const selectedSubject = ref<string | undefined>()
const selectedStatus = ref<string | undefined>()

// 弹窗控制
const editingEntry = ref<MistakeEntry | undefined>()
const showFormModal = ref(false)

// 科目选项
const subjectOptions = [
  { label: '全部科目', value: undefined },
  { label: '数学', value: '数学' },
  { label: '语文', value: '语文' },
  { label: '英语', value: '英语' },
  { label: '物理', value: '物理' },
  { label: '化学', value: '化学' },
  { label: '生物', value: '生物' },
  { label: '历史', value: '历史' },
  { label: '地理', value: '地理' },
  { label: '政治', value: '政治' },
  { label: '其他', value: '其他' },
]

// 状态映射
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

// 获取错题列表
async function fetchEntries() {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      page: page.value,
      size: size.value,
    }
    if (selectedSubject.value) {
      params.subject = selectedSubject.value
    }
    if (selectedStatus.value) {
      params.status = selectedStatus.value
    }

    const response = await axiosInstance.get('/api/plugins/my-plugin/mistakes', { params })
    const data = response.data
    entries.value = data.items || []
    total.value = data.total || 0
  } catch (error) {
    console.error('获取错题列表失败:', error)
    Toast.error('获取错题列表失败')
  } finally {
    loading.value = false
  }
}

// 删除错题
async function handleDelete(entry: MistakeEntry) {
  const name = entry.metadata?.name
  if (!name) return
  try {
    await axiosInstance.delete(`/api/plugins/my-plugin/mistakes/${name}`)
    Toast.success('删除成功')
    await fetchEntries()
  } catch (error) {
    console.error('删除失败:', error)
    Toast.error('删除失败')
  }
}

// 切换掌握状态（快速切换）
async function toggleStatus(entry: MistakeEntry) {
  const name = entry.metadata?.name
  if (!name || !entry.spec) return

  const currentStatus = entry.spec.status
  let nextStatus: string
  if (currentStatus === 'unmastered' || !currentStatus) {
    nextStatus = 'reviewing'
  } else if (currentStatus === 'reviewing') {
    nextStatus = 'mastered'
  } else {
    nextStatus = 'unmastered'
  }

  try {
    await axiosInstance.patch(`/api/plugins/my-plugin/mistakes/${name}/status`, { status: nextStatus })
    Toast.success('状态已更新')
    entry.spec.status = nextStatus
  } catch (error) {
    console.error('状态更新失败:', error)
    Toast.error('状态更新失败')
  }
}

// 查看详情
function viewDetail(entry: MistakeEntry) {
  router.push({
    name: 'MistakeEntryDetail',
    params: { name: entry.metadata?.name },
  })
}

// 编辑
function handleEdit(entry: MistakeEntry) {
  editingEntry.value = entry
  showFormModal.value = true
}

// 新建
function handleCreate() {
  editingEntry.value = undefined
  showFormModal.value = true
}

// 表单成功回调
function handleFormSuccess() {
  showFormModal.value = false
  editingEntry.value = undefined
  fetchEntries()
}

// 监听筛选条件变化
watch([selectedSubject, selectedStatus], () => {
  page.value = 1
  fetchEntries()
})

onMounted(() => {
  fetchEntries()
})
</script>

<template>
  <div class="mistake-entry-list">
    <VPageHeader title="错题本">
      <template #actions>
        <VButton type="secondary" @click="router.push('/mistakes/stats')">
          📊 学习统计
        </VButton>
        <VButton type="primary" @click="handleCreate">
          <template #icon>
            <IconAddCircle />
          </template>
          添加错题
        </VButton>
      </template>
    </VPageHeader>

    <div class="p-4">
      <!-- 筛选栏 -->
      <VCard class="mb-4">
        <div class="flex items-center gap-4">
          <div class="flex items-center gap-2">
            <span class="text-sm text-gray-600">📋 筛选：</span>
          </div>
          <div class="flex items-center gap-2">
            <span class="text-sm text-gray-500">科目</span>
            <select
              v-model="selectedSubject"
              class="rounded border border-gray-300 px-3 py-1.5 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            >
              <option
                v-for="opt in subjectOptions"
                :key="String(opt.value)"
                :value="opt.value"
              >
                {{ opt.label }}
              </option>
            </select>
          </div>
          <div class="flex items-center gap-2">
            <span class="text-sm text-gray-500">状态</span>
            <select
              v-model="selectedStatus"
              class="rounded border border-gray-300 px-3 py-1.5 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            >
              <option value="">全部状态</option>
              <option value="unmastered">未掌握</option>
              <option value="reviewing">复习中</option>
              <option value="mastered">已掌握</option>
            </select>
          </div>
          <div class="text-sm text-gray-400">
            共 {{ total }} 条记录
          </div>
        </div>
      </VCard>

      <!-- 加载中 -->
      <VLoading v-if="loading" />

      <!-- 空状态 -->
      <VEmpty
        v-else-if="entries.length === 0"
        title="还没有错题记录"
        description="点击右上角「添加错题」开始记录吧！"
      >
        <template #actions>
          <VButton type="primary" @click="handleCreate">
            <template #icon>
              <IconAddCircle />
            </template>
            添加第一条错题
          </VButton>
        </template>
      </VEmpty>

      <!-- 错题列表 -->
      <div v-else class="space-y-3">
        <div
          v-for="entry in entries"
          :key="entry.metadata.name"
          class="bg-white rounded-lg border border-gray-100 shadow-sm hover:shadow-md transition-shadow cursor-pointer"
          @click="viewDetail(entry)"
        >
          <div class="p-4">
            <div class="flex items-start justify-between">
              <div class="flex-1 min-w-0">
                <!-- 标题栏：科目 + 状态 + 难度 -->
                <div class="flex items-center gap-2 mb-2">
                  <VTag>{{ entry.spec?.subject || '未分类' }}</VTag>
                  <VStatusDot
                    v-if="entry.spec?.status"
                    :color="statusMap[entry.spec.status]?.color || 'gray'"
                    :text="statusMap[entry.spec.status]?.label || entry.spec.status"
                    @click.stop="toggleStatus(entry)"
                  />
                  <VTag
                    v-if="entry.spec?.difficulty"
                    :color="difficultyMap[entry.spec.difficulty]?.color"
                  >
                    {{ difficultyMap[entry.spec.difficulty]?.label || entry.spec.difficulty }}
                  </VTag>
                  <VTag v-if="entry.spec?.source" color="blue">
                    {{ entry.spec.source === 'exam' ? '考试' : entry.spec.source === 'homework' ? '作业' : entry.spec.source === 'quiz' ? '测验' : '其他' }}
                  </VTag>
                </div>

                <!-- 题目 -->
                <p class="text-base font-medium text-gray-900 mb-2 line-clamp-2">
                  {{ entry.spec?.question || '无题目' }}
                </p>

                <!-- 知识点 -->
                <p v-if="entry.spec?.knowledgePoint" class="text-sm text-gray-500">
                  📌 知识点：{{ entry.spec.knowledgePoint }}
                </p>

                <!-- 标签 -->
                <div v-if="entry.spec?.tags && entry.spec.tags.length > 0" class="flex gap-1 mt-2">
                  <VTag v-for="tag in entry.spec.tags" :key="tag" size="small">
                    {{ tag }}
                  </VTag>
                </div>
              </div>

              <!-- 操作菜单 -->
              <div class="ml-4" @click.stop>
                <VDropdown>
                  <VButton type="secondary" size="sm">
                    <template #icon>
                      <IconMore />
                    </template>
                  </VButton>
                  <template #popper>
                    <VDropdownItem @click="handleEdit(entry)">
                      <template #icon>
                        <IconAddCircle />
                      </template>
                      编辑
                    </VDropdownItem>
                    <VDropdownItem type="danger" @click="handleDelete(entry)">
                      <template #icon>
                        <IconDeleteBin />
                      </template>
                      删除
                    </VDropdownItem>
                  </template>
                </VDropdown>
              </div>
            </div>
          </div>
        </div>

        <!-- 分页 -->
        <div class="mt-4">
          <VPagination
            v-model:page="page"
            v-model:size="size"
            :total="total"
            @change="fetchEntries"
          />
        </div>
      </div>
    </div>

    <!-- 创建/编辑弹窗 -->
    <VModal
      v-model:visible="showFormModal"
      :title="editingEntry ? '编辑错题' : '添加错题'"
      :width="800"
    >
      <MistakeEntryForm
        :entry="editingEntry"
        @success="handleFormSuccess"
        @cancel="showFormModal = false"
      />
    </VModal>
  </div>
</template>

<style lang="scss" scoped>
.mistake-entry-list {
  :deep(.v-status-dot) {
    cursor: pointer;
  }
}
</style>
