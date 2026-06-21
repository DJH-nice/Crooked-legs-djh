import { rsbuildConfig } from '@halo-dev/ui-plugin-bundler-kit'
import Icons from 'unplugin-icons/rspack'
import { pluginSass } from '@rsbuild/plugin-sass'
import type { RsbuildConfig } from '@rsbuild/core'

export default rsbuildConfig({
  rsbuild: {
    resolve: {
      alias: {
        '@': './src',
      },
    },
    plugins: [pluginSass()],
    tools: {
      postcss: {
        // Rsbuild auto-detects postcss.config.cjs by default.
        // This explicit config ensures PostCSS + Tailwind + Autoprefixer are
        // always wired, even if the bundler-kit preset changes.
        postcssOptions: {
          config: true,
        },
      },
      rspack: {
        plugins: [Icons({ compiler: 'vue3' })],
      },
    },
  },
}) as RsbuildConfig
