# 错题本 - Halo 插件

一个功能完善的错题本插件，帮助记录、管理和复习错题。

## 功能特性

- 📝 **错题记录**：完整记录题目、错误答案、正确答案和分析
- 📂 **科目分类**：支持数学、语文、英语、物理、化学等多科目分类
- 🎯 **掌握状态**：未掌握 → 复习中 → 已掌握，三阶段状态追踪
- ⭐ **难度标注**：简单、中等、困难三级难度
- 🏷️ **标签系统**：自定义标签，灵活组织错题
- 📖 **错题分析**：记录解题思路和易错点
- 🔍 **快速筛选**：按科目、状态筛选错题

## API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/plugins/my-plugin/mistakes | 分页查询错题列表 |
| GET | /api/plugins/my-plugin/mistakes/{name} | 获取错题详情 |
| POST | /api/plugins/my-plugin/mistakes | 创建错题 |
| PUT | /api/plugins/my-plugin/mistakes/{name} | 更新错题 |
| DELETE | /api/plugins/my-plugin/mistakes/{name} | 删除错题 |
| PATCH | /api/plugins/my-plugin/mistakes/{name}/status | 更新掌握状态 |

## 开发环境

- Java 21+
- Node.js 18+
- pnpm

## 开发

```bash
# 启用插件
./gradlew haloServer
# 开发前端
cd ui
pnpm install
pnpm dev
```

## 构建

```bash
./gradlew build
```

构建完成后，可以在 `build/libs` 目录找到插件 jar 文件。

## 许可证

[GPL-3.0](./LICENSE) © dongjiahao
