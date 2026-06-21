/**
 * 错题本模块共享常量
 *
 * 集中维护展示层映射，避免组件间重复定义。
 * 所有展示文本（状态标签、难度颜色、科目图标等）统一管理于此，
 * 确保列表、详情、表单三视图的视觉一致性。
 */

// ====== 状态配置 ======
export const STATUS_ORDER = ['unmastered', 'reviewing', 'mastered'] as const
export type MistakeStatus = (typeof STATUS_ORDER)[number]

export interface StatusConfigItem {
  label: string
  badgeClass: string
  icon: string
}

export const statusConfig: Record<string, StatusConfigItem> = {
  unmastered: { label: '未掌握', badgeClass: 'badge-unmastered', icon: '●' },
  reviewing: { label: '复习中', badgeClass: 'badge-reviewing', icon: '◐' },
  mastered: { label: '已掌握', badgeClass: 'badge-mastered', icon: '●' },
}

/** 循环切换状态：unmastered → reviewing → mastered → unmastered */
export function nextStatus(current: string): string {
  const idx = STATUS_ORDER.indexOf(current as MistakeStatus)
  if (idx === -1) return 'unmastered'
  return STATUS_ORDER[(idx + 1) % STATUS_ORDER.length]
}

// ====== 难度配置 ======
export const difficultyLabels: Record<string, string> = {
  easy: '简单',
  medium: '中等',
  hard: '困难',
}

export const difficultyColors: Record<string, string> = {
  easy: 'text-emerald-400 border-emerald-500/30',
  medium: 'text-amber-400 border-amber-500/30',
  hard: 'text-rose-400 border-rose-500/30',
}

export const difficultyBgBorder: Record<string, string> = {
  easy: 'text-emerald-400 border-emerald-500/30 bg-emerald-500/10',
  medium: 'text-amber-400 border-amber-500/30 bg-amber-500/10',
  hard: 'text-rose-400 border-rose-500/30 bg-rose-500/10',
}

export const difficulties = [
  { label: '简单', value: 'easy', color: 'emerald' },
  { label: '中等', value: 'medium', color: 'amber' },
  { label: '困难', value: 'hard', color: 'rose' },
]

// ====== 科目 ======
export const subjects = ['数学', '语文', '英语', '物理', '化学', '生物', '历史', '地理', '政治', '其他']

export function getSubjectIcon(subject?: string): string {
  const map: Record<string, string> = {
    '数学': '∑',
    '语文': '文',
    '英语': 'A',
    '物理': '⚛',
    '化学': '⚗',
    '生物': '🧬',
    '历史': '📜',
    '地理': '🌍',
    '政治': '🏛',
  }
  return subject ? map[subject] || '📝' : '📝'
}

// ====== 来源 ======
export const sourceMap: Record<string, string> = {
  exam: '考试',
  homework: '作业',
  quiz: '测验',
  other: '其他',
}

export const sources = [
  { label: '考试', value: 'exam', icon: '📝' },
  { label: '作业', value: 'homework', icon: '📚' },
  { label: '测验', value: 'quiz', icon: '✏️' },
  { label: '其他', value: 'other', icon: '📌' },
]

// ====== 掌握度进度条 ======
export function masteryBarColor(status?: string): string {
  if (status === 'mastered') return 'bg-gradient-to-r from-emerald-500 to-emerald-400'
  if (status === 'reviewing') return 'bg-gradient-to-r from-amber-500 to-amber-400'
  return 'bg-gradient-to-r from-rose-500 to-rose-400'
}

export function masteryProgress(status?: string): number {
  if (status === 'mastered') return 100
  if (status === 'reviewing') return 60
  return 20
}

// ====== 筛选选项 ======
export const filterStatuses = [
  { label: '全部状态', value: undefined },
  { label: '未掌握', value: 'unmastered', color: 'rose' },
  { label: '复习中', value: 'reviewing', color: 'amber' },
  { label: '已掌握', value: 'mastered', color: 'emerald' },
]

export const filterDifficulties = [
  { label: '全部难度', value: undefined },
  { label: '简单', value: 'easy' },
  { label: '中等', value: 'medium' },
  { label: '困难', value: 'hard' },
]

// ====== 时间工具 ======
export function timeAgo(dateStr?: string): string {
  if (!dateStr) return ''
  const now = Date.now()
  const then = new Date(dateStr).getTime()
  const diff = now - then
  const days = Math.floor(diff / 86400000)
  if (days === 0) return '今天'
  if (days === 1) return '昨天'
  if (days < 30) return `${days} 天前`
  return `${Math.floor(days / 30)} 个月前`
}
