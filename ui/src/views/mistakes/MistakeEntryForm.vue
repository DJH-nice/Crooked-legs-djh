<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { axiosInstance } from '@halo-dev/api-client'
import { Toast } from '@halo-dev/components'
import type { MistakeEntry } from '../types'
import { difficulties, sources } from './constants'
import { apiPaths } from '../../api/paths'

/** 组件属性：entry 为待编辑的错题（undefined 表示新建） */
const props = defineProps<{ entry?: MistakeEntry }>()
const emit = defineEmits<{ success: []; cancel: [] }>()

/** 提交中状态，防止重复提交 */
const submitting = ref(false)

/** 表单数据模型 */
const form = reactive({
  subject: '',
  question: '',
  wrongAnswer: '',
  correctAnswer: '',
  analysis: '',
  difficulty: 'medium',
  source: 'exam',
  knowledgePoint: '',
  tags: [] as string[],
  remark: '',
})

/** 新增标签的临时输入框 */
const newTagInput = ref('')

/** 编辑模式下，将 entry 数据填充到表单 */
watch(
  () => props.entry,
  (entry) => {
    if (entry?.spec) {
      form.subject = entry.spec.subject || ''
      form.question = entry.spec.question || ''
      form.wrongAnswer = entry.spec.wrongAnswer || ''
      form.correctAnswer = entry.spec.correctAnswer || ''
      form.analysis = entry.spec.analysis || ''
      form.difficulty = entry.spec.difficulty || 'medium'
      form.source = entry.spec.source || 'exam'
      form.knowledgePoint = entry.spec.knowledgePoint || ''
      form.tags = entry.spec.tags ? [...entry.spec.tags] : []
      form.remark = entry.spec.remark || ''
    }
  },
  { immediate: true }
)

/** 添加标签 */
function addTag() {
  const tag = newTagInput.value.trim()
  if (tag && !form.tags.includes(tag)) {
    form.tags.push(tag)
  }
  newTagInput.value = ''
}

/** 移除指定标签 */
function removeTag(tag: string) {
  const idx = form.tags.indexOf(tag)
  if (idx >= 0) form.tags.splice(idx, 1)
}

/** 提交表单：校验 → POST（新建）或 PUT（更新） */
async function handleSubmit() {
  if (!form.question.trim()) {
    Toast.warning('请输入题目')
    return
  }

  submitting.value = true
  try {
    const payload = {
      spec: {
        subject: form.subject.trim(),
        question: form.question.trim(),
        wrongAnswer: form.wrongAnswer.trim(),
        correctAnswer: form.correctAnswer.trim(),
        analysis: form.analysis.trim(),
        difficulty: form.difficulty,
        source: form.source,
        knowledgePoint: form.knowledgePoint.trim(),
        tags: [...form.tags],
        remark: form.remark.trim(),
      },
    }

    if (props.entry?.metadata?.name) {
      await axiosInstance.put(apiPaths.detail(props.entry.metadata.name), payload)
      Toast.success('更新成功')
    } else {
      await axiosInstance.post(apiPaths.create, payload)
      Toast.success('添加成功')
    }
    emit('success')
  } catch (error: unknown) {
    console.error('保存失败:', error)
    Toast.error('保存失败，请稍后再试')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="space-y-5">
    <!-- 科目 -->
    <div>
      <label class="block text-sm font-medium text-gray-300 mb-2">科目</label>
      <input v-model="form.subject" class="glass-input w-full" placeholder="自由输入科目，如：高等数学、CPA会计、日语N1..." />
    </div>

    <!-- 题目 -->
    <div>
      <label class="block text-sm font-medium text-gray-300 mb-2">题目 <span class="text-rose-400">*</span></label>
      <textarea v-model="form.question" rows="3" class="glass-textarea w-full" placeholder="请输入题目内容..." />
    </div>

    <!-- 错误答案 vs 正确答案（并排） -->
    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
      <div>
        <label class="block text-sm font-medium text-gray-300 mb-2">❌ 错误答案</label>
        <textarea
          v-model="form.wrongAnswer"
          rows="2"
          class="glass-textarea w-full"
          style="border-color: rgba(244, 63, 94, 0.3);"
          placeholder="你的错误答案..."
        />
      </div>
      <div>
        <label class="block text-sm font-medium text-gray-300 mb-2">✅ 正确答案</label>
        <textarea
          v-model="form.correctAnswer"
          rows="2"
          class="glass-textarea w-full"
          style="border-color: rgba(16, 185, 129, 0.3);"
          placeholder="正确答案..."
        />
      </div>
    </div>

    <!-- 难度 + 来源 -->
    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
      <div>
        <label class="block text-sm font-medium text-gray-300 mb-2">难度</label>
        <div class="flex gap-2">
          <button
            v-for="d in difficulties"
            :key="d.value"
            type="button"
            @click="form.difficulty = d.value"
            :class="[
              'px-4 py-2 rounded-xl text-sm font-medium transition-all border',
              form.difficulty === d.value
                ? 'bg-white/10 border-purple-500/50 text-purple-300'
                : 'bg-white/5 border-white/10 text-gray-400 hover:bg-white/10'
            ]"
          >
            {{ d.label }}
          </button>
        </div>
      </div>
      <div>
        <label class="block text-sm font-medium text-gray-300 mb-2">来源</label>
        <div class="flex gap-2 flex-wrap">
          <button
            v-for="s in sources"
            :key="s.value"
            type="button"
            @click="form.source = s.value"
            :class="[
              'px-4 py-2 rounded-xl text-sm font-medium transition-all border',
              form.source === s.value
                ? 'bg-white/10 border-purple-500/50 text-purple-300'
                : 'bg-white/5 border-white/10 text-gray-400 hover:bg-white/10'
            ]"
          >
            {{ s.icon }} {{ s.label }}
          </button>
        </div>
      </div>
    </div>

    <!-- 知识点 -->
    <div>
      <label class="block text-sm font-medium text-gray-300 mb-2">知识点 / 考点</label>
      <input v-model="form.knowledgePoint" class="glass-input w-full" placeholder="例如：拉格朗日中值定理、存货跌价准备..." />
    </div>

    <!-- 错题分析 -->
    <div>
      <label class="block text-sm font-medium text-gray-300 mb-2">错题分析 / 解析</label>
      <textarea v-model="form.analysis" rows="3" class="glass-textarea w-full" placeholder="分析错误原因、解题思路..." />
    </div>

    <!-- 标签 -->
    <div>
      <label class="block text-sm font-medium text-gray-300 mb-2">标签</label>
      <div class="flex flex-wrap gap-2 mb-3">
        <span
          v-for="tag in form.tags"
          :key="tag"
          class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs bg-purple-500/20 text-purple-300 border border-purple-500/30"
        >
          #{{ tag }}
          <button @click="removeTag(tag)" class="text-purple-400/60 hover:text-purple-300">&times;</button>
        </span>
      </div>
      <div class="flex gap-2">
        <input
          v-model="newTagInput"
          class="glass-input flex-1"
          placeholder="输入标签后按回车添加"
          @keyup.enter="addTag"
        />
        <button @click="addTag" class="glass-btn-secondary glass-btn-sm">添加</button>
      </div>
    </div>

    <!-- 备注 -->
    <div>
      <label class="block text-sm font-medium text-gray-300 mb-2">备注</label>
      <input v-model="form.remark" class="glass-input w-full" placeholder="其他备注信息..." />
    </div>

    <!-- 操作按钮 -->
    <div class="flex justify-end gap-3 pt-4 border-t border-white/10">
      <button @click="emit('cancel')" class="glass-btn-secondary glass-btn-sm">取消</button>
      <button @click="handleSubmit" :disabled="submitting" class="glass-btn-primary glass-btn-sm flex items-center gap-2">
        <span v-if="submitting" class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></span>
        {{ props.entry?.metadata?.name ? '更新' : '添加' }}
      </button>
    </div>
  </div>
</template>
