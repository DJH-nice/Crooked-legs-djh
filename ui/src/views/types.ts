export interface MistakeEntry {
  metadata: {
    name: string
    version?: number
    creationTimestamp?: string
    [key: string]: unknown
  }
  spec: MistakeEntrySpec
  [key: string]: unknown
}

export interface MistakeEntrySpec {
  subject?: string
  question?: string
  wrongAnswer?: string
  correctAnswer?: string
  analysis?: string
  status?: string
  difficulty?: string
  source?: string
  tags?: string[]
  knowledgePoint?: string
  remark?: string
}
