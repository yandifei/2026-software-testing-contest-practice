# AGENTS.md

## 宪法规则（最高优先级，任何情况不得违反）
1. **严禁超范围执行。** 只做用户本轮提示词明确要求的事，绝不擅自扩大到提示词边界之外。
   - 反例（真实事故）：用户要求“把某条提示词写入项目系统提示词”，AI 却擅自开始生成代码文件。
   - 判断方法：动手前先问自己——用户要我做的到底是哪一件事？产出物在他的要求里吗？
   - 若提示词可作多种理解、或需要额外动作才能完成，**先停下来向用户确认**，不要自行脑补任务。
2. **命令/文件操作前先说明再执行**，涉及删除、覆盖、提交、推送时尤其要先确认。
3. **先运行验证再宣布完成**，不谎报“已经修好了”。

## 这个仓库是什么
课程实验：用 AI 编程助手从零搭建一个**番茄钟待办清单**。

**当前真实进度**：步骤 0–5.4、**步骤 7（单元测试）**均已完成——四个代码文件
（`index.html`/`styles.css`/`pomodoro.js`/`app.js`）、`docs/brainstorm.md`/`requirements.md`/`design.md`/`prompts.md`/`AI对话记录.md`/`review.md`
均已产出，`feat: 番茄钟待办清单 v0.1` 已提交，并已修复「双击 `index.html` 时 ESM 不加载」的运行问题；
`pomodoro.js` 已按 **superpowers 的 TDD 流程**删除重建，`test/pomodoro.test.js` 共 **27 个用例**（含 8 个边界/异常）全绿；
第 19 轮已补齐两处 P0 缺口（「继续」按钮 + 列表显示预计番茄数）。
**尚未完成**：步骤 8 集成测试与验收（`scripts/e2e.js` + `docs/acceptance.md`）、`docs/report.md`、
`docs/review.md` 的只读代码审查部分、`git tag v1.0`；
以及第一期清单里未做的 FR-P1-1（自定义时长——引擎支持但界面无入口）、FR-P1-2（编辑任务内容）、FR-P2-1（历史统计）。

> **工具变更**：步骤 0–5.4 由 OpenCode 完成；**从第 17 轮起改用 DeepSeek Harness 桌面端继续开发**，
> 后续所有步骤（单元测试、集成测试、评审、报告）都在 DeepSeek Harness 里做。

实验指导书（`docs/老师的资料/P2.实验指导书_用AI辅助编程_opencode.docx`）是需求、步骤顺序、
提示词和验收标准的唯一依据（**文件名里的 `opencode` 是老师原始文件名，保持不动**）。
不懂就先把 docx 解压读 `word/document.xml`（UTF-8 编码）。

## 不可动摇的架构（实验会检查这一点）
技术栈固定：**原生 HTML/CSS/JS + localStorage，ESM，不用框架，无构建步骤。**
- `index.html` — 页面结构；用 `<script type="module" src="app.js"></script>` 引入脚本
- `styles.css` — 样式
- `pomodoro.js` — **纯业务逻辑**（任务增删改查 + 计时状态机 + 统计）。
  **绝对不能**操作 DOM 或读写 `localStorage`。
- `app.js` — DOM 渲染、`setInterval` 驱动、`localStorage` 持久化。
- **`pomodoro.js` 里所有和时间相关的计算都必须接收外部传入的 `nowMs` 参数；绝不能在内部调用 `Date.now()`。**
  这是单元测试能跑起来的前提——破坏了它，测试就没法写。
- localStorage 的 key：`pomodoro-data`。

## `pomodoro.js` 的公开 API 约定
导出工厂函数 `createApp(options)`，默认值 `{ workMinutes: 25, breakMinutes: 5 }`。
方法：`addTask(text, estimatedPomodoros)`、`toggleTask(id)`、`deleteTask(id)`、`getTasks()`、
`getTodayStats()`、`startTimer(nowMs)`、`pauseTimer()`、`resumeTimer(nowMs)`、`resetTimer()`、
`tick(nowMs)`、`getTimerState()`。id 用 `crypto.randomUUID()`。
统计：`focusMinutes = completedPomodoros * workMinutes`；`tasksDone` = 已勾选完成的任务数。
计时阶段：`idle` / `running` / `break` / `paused`；一个工作番茄结束后 `completedPomodoros` 加一，
并自动切换到休息。
**状态迁移守卫**：`startTimer` 只在 `idle` 生效（非 idle 时点击不重开本轮）；
`pauseTimer` 只在 `running` / `break` 生效；`resumeTimer` 只在 `paused` 生效。
**「今日统计」目前是累计语义**（跨天不清零，界面标题仍写「今日统计」，见 `docs/review.md` 偏差 A）。

## 命令
- `npm init -y && npm pkg set type=module` — 让 ESM 在 Node 里能用，必须先做（之后任何 `node -e` 导入都依赖它）。
- `npx vitest run`（等价于 `npm test`）— 单元测试在 `test/pomodoro.test.js`（27 个用例，含 8 个边界/异常）。
  - **受限沙箱里必须加 `--pool=threads`**：vitest 默认的 forks 池要给子进程建管道，沙箱下会 `EPERM`；
    也不要改用 `vitest.config.js`——vite 解析配置文件时同样会 `spawn EPERM`。
  - 跑测试时若 npm 报缓存目录无权写入，把缓存改到临时区：`$env:npm_config_cache = "$env:TEMP\dsh-npm-cache"`。
- `pwsh -File scripts/mutation-check.ps1` — 变异检查：往实现里注入 10 个已知 bug，确认每个都被测试抓住（当前 10/10）。
- `node scripts/e2e.js` — 集成测试；用 Node 内置 `node:assert`，不装额外依赖。
- **运行应用必须用本地静态服务器**：`npm run dev`，然后浏览器访问 `http://localhost:5173`。
  原因：技术栈用的是原生 ES modules，而 Chrome/Edge **禁止 `file://` 加载外部模块脚本**，直接双击 `index.html` 会导致 `app.js` 完全不执行、界面全部失灵（这是已踩过的坑）。
- 不想干等时间就能验证计时逻辑：`createApp({ workMinutes: 1, breakMinutes: 1 })` 并传入伪造的 `nowMs`。

## 工作流约定（来自实验指导书）
- 按实验步骤一步一步来；每完成一步就 `git commit`，让 AI 做大改动前也要先提交一次。
- 用过的**每一条**提示词都要追加到 `docs/prompts.md`（实验会按这个评分）。
- **每轮对话结束后，都要把本轮“用户请求 / AI 操作 / 涉及文件”追加到 `docs/AI对话记录.md`。**
- 先运行验证再宣布完成——指导书明确警告 AI 可能谎称“已经修好了”。
- 必须产出的文档：`docs/brainstorm.md`、`requirements.md`、`design.md`、`acceptance.md`、
  `review.md`、`report.md`；最终版本打标签 `git tag v1.0`。
- 代码评审环节必须是**只读**的（切到 Plan 代理，或在提示词里写明“只审查，不要修改任何文件”）。
- 写测试遵循 **superpowers 的 `test-driven-development` 技能**（已装在 `~\.claude\skills\`）：
  先写失败的测试并亲眼看到它失败，再写最小实现；完成后跑 `scripts/mutation-check.ps1` 变异检查。

## 已下发的开发提示词（归档，需遵守）
### 步骤 5.1 创建项目骨架
> 你是前端开发工程师。请为「番茄钟待办清单」创建项目骨架，技术栈为原生 HTML/CSS/JavaScript + localStorage，不引入任何框架。
> 任务：生成 index.html、styles.css、pomodoro.js、app.js 四个文件；pomodoro.js 只放纯业务逻辑（不操作 DOM、不读写 localStorage），app.js 负责界面交互。
> 产出格式：先列出文件清单和各文件职责，再逐个给出文件代码。

现状：**已执行**（第 13 轮创建骨架，第 15 轮补全实现）。运行方式另见「命令」一节的本地静态服务器要求。
