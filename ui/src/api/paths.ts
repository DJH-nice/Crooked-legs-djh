/**
 * API 路径常量
 * 集中管理 REST 端点，单点修改。
 * 如果插件重命名，只需更改 PLUGIN_NAME 一处即可。
 */

/** Halo 插件名称，对应 plugin.yaml 中定义的 name */
const PLUGIN_NAME = 'my-plugin'

export const API_PREFIX = `/api/plugins/${PLUGIN_NAME}/mistakes`

export const apiPaths = {
  /** GET 列表（分页 + 筛选） */
  list: API_PREFIX,
  /** POST 创建 */
  create: API_PREFIX,
  /** GET / PUT / DELETE 单条 */
  detail: (name: string) => `${API_PREFIX}/${name}`,
  /** PATCH 更新状态 */
  status: (name: string) => `${API_PREFIX}/${name}/status`,
  /** GET 每日统计 */
  dailyStats: `${API_PREFIX}/stats/daily`,
} as const
