import { definePlugin } from '@halo-dev/ui-shared'
import { IconBookRead } from '@halo-dev/components'
import { markRaw } from 'vue'

export default definePlugin({
  components: {},
  routes: [
    {
      parentName: 'Root',
      route: {
        path: '/mistakes',
        name: 'MistakeEntryList',
        component: () =>
          import(
            /* webpackChunkName: "MistakeEntryList" */ './views/mistakes/MistakeEntryList.vue'
          ),
        meta: {
          title: '错题本',
          searchable: true,
          menu: {
            name: '错题本',
            group: '学习工具',
            icon: markRaw(IconBookRead),
            priority: 0,
          },
        },
      },
    },
    {
      parentName: 'Root',
      route: {
        path: '/mistakes/stats',          // 必须在 /:name 之前，避免stats被当成name参数
        name: 'StatsRecord',
        component: () =>
          import(
            /* webpackChunkName: "StatsRecord" */ './views/mistakes/StatsRecord.vue'
          ),
        meta: {
          title: '学习统计',
          searchable: false,
        },
      },
    },
    {
      parentName: 'Root',
      route: {
        path: '/mistakes/:name',
        name: 'MistakeEntryDetail',
        component: () =>
          import(
            /* webpackChunkName: "MistakeEntryDetail" */ './views/mistakes/MistakeEntryDetail.vue'
          ),
        meta: {
          title: '错题详情',
          searchable: false,
        },
      },
    },
  ],
  extensionPoints: {},
})
