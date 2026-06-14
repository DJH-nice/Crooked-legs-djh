<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { axiosInstance } from '@halo-dev/api-client'
import {
  VButton,
  VTag,
  Toast,
} from '@halo-dev/components'
import type { MistakeEntry } from '../types'

const props = defineProps<{ entry?: MistakeEntry }>()
const emit = defineEmits<{ success: []; cancel: [] }>()

const submitting = ref(false)

// ====== 难度选项 ======
const difficulties = [
  { label: '简单', value: 'easy' },
  { label: '中等', value: 'medium' },
  { label: '困难', value: 'hard' },
]

// ====== 来源选项 ======
const sources = [
  { label: '考试', value: 'exam' },
  { label: '作业', value: 'homework' },
  { label: '测验', value: 'quiz' },
  { label: '其他', value: 'other' },
]

// ====== 表单数据 ======
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

const newTagInput = ref('')

// ====== 初始化（编辑时回填） ======
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

// ====== 标签操作 ======
function addTag() {
  const tag = newTagInput.value.trim()
  if (tag && !form.tags.includes(tag)) {
    form.tags.push(tag)
  }
  newTagInput.value = ''
}

function removeTag(tag: string) {
  const idx = form.tags.indexOf(tag)
  if (idx >= 0) form.tags.splice(idx, 1)
}

// ====== 提交保存 ======
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
      // 更新已有记录
      await axiosInstance.put(
        `/api/plugins/my-plugin/mistakes/${props.entry.metadata.name}`,
        payload
      )
      Toast.success('更新成功')
    } else {
      // 新建记录
      await axiosInstance.post('/api/plugins/my-plugin/mistakes', payload)
      Toast.success('添加成功')
    }
    emit('success')
  } catch (error: any) {
    console.error('保存失败:', error)
    const msg = error?.response?.data?.detail || error?.message || '保存失败'
    Toast.error(msg.length > 50 ? '服务器内部错误，请稍后再试' : msg)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="mistake-form" style="padding:16px;max-height:70vh;overflow-y:auto">

    <!-- 1. 科目 —— 自由文本输入 -->
    <div style="margin-bottom:16px">
      <label style="display:block;font-size:14px;font-weight:500;color:#374151;margin-bottom:6px">
        科目
      </label>
      <input
        v-model="form.subject"
        style="width:100%;padding:8px 12px;border:1px solid #d1d5db;border-radius:8px;font-size:14px;outline:none"
        placeholder="自由输入科目，如：高等数学、CPA会计、日语N1..."
      />
    </div>

    <!-- 2. 题目 -->
    <div style="margin-bottom:16px">
      <label style="display:block;font-size:14px;font-weight:500;color:#374151;margin-bottom:6px">
        题目 <span style="color:#ef4444">*</span>
      </label>
      <textarea
        v-model="form.question"
        rows="3"
        style="width:100%;padding:8px 12px;border:1px solid #d1d5db;border-radius:8px;font-size:14px;outline:none;resize:vertical"
        placeholder="请输入题目内容..."
      />
    </div>

    <!-- 3. 错误答案 -->
    <div style="margin-bottom:16px">
      <label style="display:block;font-size:14px;font-weight:500;color:#374151;margin-bottom:6px">错误答案</label>
      <textarea
        v-model="form.wrongAnswer"
        rows="2"
        style="width:100%;padding:8px 12px;border:1px solid #fca5a5;border-radius:8px;font-size:14px;outline:none;resize:vertical;background:#fff5f5"
        placeholder="你的错误答案..."
      />
    </div>

    <!-- 4. 正确答案 -->
    <div style="margin-bottom:16px">
      <label style="display:block;font-size:14px;font-weight:500;color:#374151;margin-bottom:6px">正确答案</label>
      <textarea
        v-model="form.correctAnswer"
        rows="2"
        style="width:100%;padding:8px 12px;border:1px solid #86efac;border-radius:8px;font-size:14px;outline:none;resize:vertical;background:#f0fdf4"
        placeholder="正确答案..."
      />
    </div>

    <!-- 5. 难度 + 来源（并排） -->
    <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px;margin-bottom:16px">
      <!-- 难度 -->
      <div>
        <label style="display:block;font-size:14px;font-weight:500;color:#374151;margin-bottom:6px">难度</label>
        <div style="display:flex;gap:8px">
          <button
            v-for="d in difficulties"
            :key="d.value"
            type="button"
            @click="form.difficulty = d.value"
            :style="{
              padding: '6px 16px',
              border: form.difficulty === d.value ? '2px solid #f97316' : '1px solid #d1d5db',
              borderRadius: '8px',
              background: form.difficulty === d.value ? '#fff7ed' : '#fff',
              color: form.difficulty === d.value ? '#ea580c' : '#374151',
              fontWeight: form.difficulty === d.value ? '600' : '400',
              cursor: 'pointer',
              fontSize: '13px',
              transition: 'all 0.15s'
            }"
          >
            {{ d.label }}
          </button>
        </div>
      </div>

      <!-- 来源 -->
      <div>
        <label style="display:block;font-size:14px;font-weight:500;color:#374151;margin-bottom:6px">来源</label>
        <div style="display:flex;gap:8px;flex-wrap:wrap">
          <button
            v-for="s in sources"
            :key="s.value"
            type="button"
            @click="form.source = s.value"
            :style="{
              padding: '6px 16px',
              border: form.source === s.value ? '2px solid #3b82f6' : '1px solid #d1d5db',
              borderRadius: '8px',
              background: form.source === s.value ? '#eff6ff' : '#fff',
              color: form.source === s.value ? '#1d4ed8' : '#374151',
              fontWeight: form.source === s.value ? '600' : '400',
              cursor: 'pointer',
              fontSize: '13px',
              transition: 'all 0.15s'
            }"
          >
            {{ s.label }}
          </button>
        </div>
      </div>
    </div>

    <!-- 6. 知识点 -->
    <div style="margin-bottom:16px">
      <label style="display:block;font-size:14px;font-weight:500;color:#374151;margin-bottom:6px">知识点 / 考点</label>
      <input
        v-model="form.knowledgePoint"
        style="width:100%;padding:8px 12px;border:1px solid #d1d5db;border-radius:8px;font-size:14px;outline:none"
        placeholder="例如：拉格朗日中值定理、存货跌价准备..."
      />
    </div>

    <!-- 7. 错题分析 -->
    <div style="margin-bottom:16px">
      <label style="display:block;font-size:14px;font-weight:500;color:#374151;margin-bottom:6px">错题分析 / 解析</label>
      <textarea
        v-model="form.analysis"
        rows="3"
        style="width:100%;padding:8px 12px;border:1px solid #d1d5db;border-radius:8px;font-size:14px;outline:none;resize:vertical"
        placeholder="分析错误原因、解题思路..."
      />
    </div>

    <!-- 8. 标签 -->
    <div style="margin-bottom:16px">
      <label style="display:block;font-size:14px;font-weight:500;color:#374151;margin-bottom:6px">标签</label>
      <div style="display:flex;flex-wrap:wrap;gap:6px;margin-bottom:8px">
        <span
          v-for="tag in form.tags"
          :key="tag"
          style="display:inline-flex;align-items:center;gap:4px;padding:4px 10px;background:#f3f4f6;border-radius:20px;font-size:12px;color:#374151"
        >
          {{ tag }}
          <span
            @click="removeTag(tag)"
            style="cursor:pointer;color:#9ca3af;font-size:14px;line-height:1"
            title="删除"
          >&times;</span>
        </span>
      </div>
      <div style="display:flex;gap:8px">
        <input
          v-model="newTagInput"
          style="flex:1;padding:8px 12px;border:1px solid #d1d5db;border-radius:8px;font-size:14px;outline:none"
          placeholder="输入标签后按回车添加"
          @keyup.enter="addTag"
        />
        <VButton type="secondary" @click="addTag">添加</VButton>
      </div>
    </div>

    <!-- 9. 备注 -->
    <div style="margin-bottom:16px">
      <label style="display:block;font-size:14px;font-weight:500;color:#374151;margin-bottom:6px">备注</label>
      <input
        v-model="form.remark"
        style="width:100%;padding:8px 12px;border:1px solid #d1d5db;border-radius:8px;font-size:14px;outline:none"
        placeholder="其他备注信息..."
      />
    </div>

    <!-- 10. 操作按钮 -->
    <div style="display:flex;justify-content:flex-end;gap:12px;padding-top:16px;border-top:1px solid #e5e7eb">
      <VButton type="secondary" @click="emit('cancel')">取消</VButton>
      <VButton type="primary" :loading="submitting" @click="handleSubmit">
        {{ props.entry?.metadata?.name ? '更新' : '添加' }}
      </VButton>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.mistake-form button:hover {
  opacity: 0.85;
}
.mistake-form input:focus,
.mistake-form textarea:focus {
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.15);
}
</style>
