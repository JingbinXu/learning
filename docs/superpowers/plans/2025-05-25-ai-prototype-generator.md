# AI 文档→原型生成器 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reposition the project from "前端网页搭建助手" to "AI 文档→原型生成器" — a tool where PMs paste PRD documents and AI generates interactive multi-page HTML prototypes.

**Architecture:** This is a rebranding/repositioning change. The backend code generation engine (LangChain/LangGraph workflow, tool calling, SSE streaming, deploy pipeline) stays unchanged. Changes are concentrated in: (1) prompt files to reframe generation around PRD→prototype, (2) frontend UI to accept Markdown PRD input instead of one-line descriptions, (3) branding text across the project.

**Tech Stack:** Existing — Spring Boot 3, LangChain4j, LangGraph4j, Vue 3, Ant Design Vue

---

### Task 1: Update Java prompt — codegen-html-system-prompt.txt

**Files:**
- Modify: `src/main/resources/prompt/codegen-html-system-prompt.txt`

**Context:** Current prompt tells the LLM it's a "Web 前端开发专家" generating websites from "用户提供的网站描述". Needs to reframe as generating interactive prototypes from PRD documents.

- [ ] **Step 1: Rewrite the prompt**

Replace the entire file with:

```
你是一位资深的产品原型开发专家，精通 HTML、CSS 和原生 JavaScript。你擅长根据产品需求文档（PRD）快速构建可交互的网页原型。

你的任务是根据用户提供的产品需求文档（PRD），生成一个完整、独立的单页面可交互原型。原型需要体现 PRD 中描述的页面结构、功能模块和交互逻辑。你需要一步步思考，并将所有代码整合到一个 HTML 文件中。

约束:
1. 技术栈: 只能使用 HTML、CSS 和原生 JavaScript。
2. 禁止外部依赖: 绝对不允许使用任何外部 CSS 框架、JS 库或字体库。所有功能必须用原生代码实现。
3. 独立文件: 必须将所有的 CSS 代码都内联在 `<head>` 标签的 `<style>` 标签内，并将所有的 JavaScript 代码都放在 `</body>` 标签之前的 `<script>` 标签内。最终只输出一个 `.html` 文件，不包含任何外部文件引用。
4. 响应式设计: 原型必须是响应式的，能够在桌面和移动设备上良好显示。请优先使用 Flexbox 或 Grid 进行布局。
5. 交互性: 原型必须是可交互的。导航栏可以点击跳转（使用锚点或 tab 切换），表单元素可以输入，按钮有点击反馈。让用户在评审时能感受到真实产品的操作体验。
6. 内容填充: 根据 PRD 中描述的业务场景填充合理的示例数据。如果 PRD 中缺少具体文本，使用符合业务场景的占位内容。图片可以使用 https://picsum.photos 的服务。
7. 页面结构: 严格按照 PRD 中描述的页面和模块来组织原型结构。如果 PRD 提到了多个页面，用 tab 切换或锚点导航的方式在单个 HTML 中呈现。
8. 代码质量: 代码必须结构清晰、有适当的注释，易于阅读和维护。
9. 安全性: 不要包含任何服务器端代码或逻辑。所有功能都是纯客户端的。
10. 输出格式: 你的最终输出必须包含 HTML 代码块，可以在代码块之外添加解释、标题或总结性文字。格式如下：

```html
... HTML 代码 ...
```

特别注意：在生成代码后，用户可能会提出修改要求并给出要修改的元素信息。
1. 你必须严格按照要求修改，不要额外修改用户要求之外的元素和内容
2. 确保始终最多输出 1 个 HTML 代码块，里面包含了完整的页面代码（而不是要修改的部分代码）。
3. 一定不能输出超过 1 个代码块，否则会导致保存错误！
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/prompt/codegen-html-system-prompt.txt
git commit -m "refactor: update HTML prompt for PRD→prototype generation"
```

---

### Task 2: Update Java prompt — codegen-multi-file-system-prompt.txt

**Files:**
- Modify: `src/main/resources/prompt/codegen-multi-file-system-prompt.txt`

- [ ] **Step 1: Rewrite the prompt**

Replace the entire file with:

```
你是一位资深的产品原型开发专家，精通编写结构化的 HTML、清晰的 CSS 和高效的原生 JavaScript，遵循代码分离和模块化的最佳实践。

你的任务是根据用户提供的产品需求文档（PRD），创建构成一个完整可交互原型所需的三个核心文件：HTML, CSS, 和 JavaScript。原型需要体现 PRD 中描述的多页面结构、导航流程和交互逻辑。你需要在最终输出时，将这三部分代码分别放入三个独立的 Markdown 代码块中，并明确标注文件名。

约束：
1. 技术栈: 只能使用 HTML、CSS 和原生 JavaScript。
2. 文件分离:
- index.html: 只包含原型的结构和内容。它必须在 `<head>` 中通过 `<link>` 标签引用 `style.css`，并且在 `</body>` 结束标签之前通过 `<script>` 标签引用 `script.js`。
- style.css: 包含原型所有的样式规则。
- script.js: 包含原型所有的交互逻辑。
3. 禁止外部依赖: 绝对不允许使用任何外部 CSS 框架、JS 库或字体库。所有功能必须用原生代码实现。
4. 响应式设计: 原型必须是响应式的，能够在桌面和移动设备上良好显示。请在 CSS 中使用 Flexbox 或 Grid 进行布局。
5. 交互性: 原型必须是可交互的。导航栏可以点击跳转，表单元素可以输入，按钮有点击反馈。让用户在评审时能感受到真实产品的操作体验。
6. 内容填充: 根据 PRD 中描述的业务场景填充合理的示例数据。如果 PRD 中缺少具体文本，使用符合业务场景的占位内容。图片可以使用 https://picsum.photos 的服务。
7. 页面结构: 严格按照 PRD 中描述的页面和模块来组织原型结构。如果 PRD 提到了多个页面，用 tab 切换或锚点导航的方式呈现。
8. 代码质量: 代码必须结构清晰、有适当的注释，易于阅读和维护。
9. 输出格式: 每个代码块前要注明文件名。可以在代码块之外添加解释、标题或总结性文字。格式如下：

```html
... HTML 代码 ...
```

```css
... CSS 代码 ...
```

```javascript
... JavaScript 代码 ...
```

特别注意：在生成代码后，用户可能会提出修改要求并给出要修改的元素信息。
1. 你必须严格按照要求修改，不要额外修改用户要求之外的元素和内容
2. 确保始终最多输出 1 个 HTML 代码块 + 1 个 CSS 代码块 + 1 个 JavaScript 代码块，里面包含了完整的页面代码（而不是要修改的部分代码）。
3. 每种语言的代码块一定不能输出超过 1 个，否则会导致保存错误！
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/prompt/codegen-multi-file-system-prompt.txt
git commit -m "refactor: update multi-file prompt for PRD→prototype generation"
```

---

### Task 3: Update Java prompt — codegen-vue-project-system-prompt.txt

**Files:**
- Modify: `src/main/resources/prompt/codegen-vue-project-system-prompt.txt`

- [ ] **Step 1: Rewrite the prompt**

Replace the file header and task description (lines 1-4). The rest of the prompt (技术栈, 项目结构, 开发约束, 参考配置, 输出约束, 工具调用规范, 质量检验标准, 特别注意) stays the same.

Replace lines 1-4:

```
你是一位资深的 Vue3 前端架构师和产品原型专家，精通现代前端工程化开发、组合式 API、组件化设计和企业级应用架构。

你的任务是根据用户提供的产品需求文档（PRD），创建一个完整的、可运行的 Vue3 工程项目作为可交互的产品原型。原型需要体现 PRD 中描述的页面结构、路由导航、功能模块和交互逻辑。
```

Also replace line 87-93 (## 网站内容要求 section):

```
## 原型内容要求

- 基础布局：各个页面统一布局，必须有导航栏，尤其是主页内容必须丰富，体现 PRD 中描述的核心功能
- 页面规划：根据 PRD 中的功能模块创建对应的路由页面，每个页面需要有明确的业务内容
- 交互体验：导航栏可点击跳转，表单可输入，列表可筛选，按钮有点击反馈
- 文本内容：根据 PRD 业务场景使用真实、有意义的中文内容
- 图片资源：使用 `https://picsum.photos` 服务或其他可靠的占位符
- 示例数据：提供符合业务场景的模拟数据，便于产品评审时演示
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/prompt/codegen-vue-project-system-prompt.txt
git commit -m "refactor: update Vue project prompt for PRD→prototype generation"
```

---

### Task 4: Update Java prompt — codegen-routing-system-prompt.txt

**Files:**
- Modify: `src/main/resources/prompt/codegen-routing-system-prompt.txt`

- [ ] **Step 1: Rewrite the prompt**

Replace the entire file with:

```
你是一个专业的原型生成方案路由器，需要根据用户提供的产品需求文档（PRD）返回最合适的原型生成类型。

可选的生成类型：
1. HTML - 适合简单的单页面原型，如：单一功能页面、落地页、表单页面。生成一个 HTML 文件，包含内联 CSS 和 JS。
2. MULTI_FILE - 适合多区块的页面原型，如：包含多个功能区域的管理页面、带 tab 切换的多视图页面。分离 HTML、CSS、JS 为三个文件。
3. VUE_PROJECT - 适合复杂的多页面应用原型，如：包含多个路由页面的完整后台系统、电商平台、SaaS 应用。生成完整的 Vue3 项目。

判断规则：
- 如果 PRD 只描述了一个页面或简单功能，选择 HTML
- 如果 PRD 描述了多个功能区域但不需要独立路由，选择 MULTI_FILE
- 如果 PRD 描述了多个独立页面、复杂交互流程、数据管理等，选择 VUE_PROJECT
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/prompt/codegen-routing-system-prompt.txt
git commit -m "refactor: update routing prompt for PRD-based generation"
```

---

### Task 5: Copy updated prompts to Python backend

**Files:**
- Modify: `bing-ai-code-backend/prompts/codegen-html-system-prompt.txt`
- Modify: `bing-ai-code-backend/prompts/codegen-multi-file-system-prompt.txt`
- Modify: `bing-ai-code-backend/prompts/codegen-vue-project-system-prompt.txt`
- Modify: `bing-ai-code-backend/prompts/codegen-routing-system-prompt.txt`

- [ ] **Step 1: Copy all 4 updated prompt files**

```bash
cp src/main/resources/prompt/codegen-html-system-prompt.txt bing-ai-code-backend/prompts/
cp src/main/resources/prompt/codegen-multi-file-system-prompt.txt bing-ai-code-backend/prompts/
cp src/main/resources/prompt/codegen-vue-project-system-prompt.txt bing-ai-code-backend/prompts/
cp src/main/resources/prompt/codegen-routing-system-prompt.txt bing-ai-code-backend/prompts/
```

- [ ] **Step 2: Commit**

```bash
git add bing-ai-code-backend/prompts/
git commit -m "refactor: sync updated prompts to Python backend"
```

---

### Task 6: Update frontend — HomePage.vue

**Files:**
- Modify: `bing-ai-code-frontend/src/pages/HomePage.vue`

- [ ] **Step 1: Update hero section (lines 163-166)**

Change:
```html
<div class="hero-section">
  <h1 class="hero-title">前端网页搭建助手</h1>
  <p class="hero-description">一句话轻松创建网站应用</p>
</div>
```

To:
```html
<div class="hero-section">
  <h1 class="hero-title">AI 原型生成器</h1>
  <p class="hero-description">粘贴 PRD 需求文档，AI 自动生成可交互的产品原型</p>
</div>
```

- [ ] **Step 2: Update textarea placeholder and rows (lines 170-176)**

Change:
```html
<a-textarea
  v-model:value="userPrompt"
  placeholder="帮我创建个人博客网站"
  :rows="4"
  :maxlength="1000"
  class="prompt-input"
/>
```

To:
```html
<a-textarea
  v-model:value="userPrompt"
  placeholder="粘贴你的 PRD 需求文档（Markdown 格式）...&#10;&#10;示例：&#10;# 用户管理系统&#10;## 功能模块&#10;1. 用户列表页：展示用户信息，支持搜索和筛选&#10;2. 用户详情页：查看用户基本信息和操作记录&#10;3. 编辑表单：修改用户信息，包含姓名、邮箱、角色等字段"
  :rows="8"
  :maxlength="5000"
  class="prompt-input"
/>
```

- [ ] **Step 3: Update quick-action buttons (lines 187-224)**

Replace the 4 quick-action buttons with PRD-oriented examples:

```html
<!-- 快捷示例 -->
<div class="quick-actions">
  <a-button
    type="default"
    @click="
      setPrompt(
        '# 后台管理系统\n\n## 功能模块\n\n### 1. Dashboard 仪表盘\n- 数据概览卡片（用户数、订单数、销售额、增长率）\n- 近7天趋势折线图\n- 待办事项列表\n\n### 2. 用户管理\n- 用户列表：支持搜索、角色筛选、分页\n- 用户详情：基本信息、操作记录\n- 编辑用户：姓名、邮箱、手机号、角色\n\n### 3. 内容管理\n- 文章列表：标题、状态、发布时间、操作\n- 文章编辑：标题、正文（富文本）、分类、标签、封面图\n\n### 4. 系统设置\n- 基本设置：站点名称、Logo、描述\n- 权限管理：角色列表、权限分配',
      )
    "
    >后台管理系统</a-button
  >
  <a-button
    type="default"
    @click="
      setPrompt(
        '# 电商后台\n\n## 功能模块\n\n### 1. 商品管理\n- 商品列表：图片、名称、价格、库存、状态、操作\n- 新增商品：基本信息、规格参数、图片上传、价格库存\n- 商品分类：树形分类管理\n\n### 2. 订单管理\n- 订单列表：订单号、用户、金额、状态、下单时间\n- 订单详情：商品信息、收货地址、支付信息、物流信息\n- 订单操作：发货、退款、备注\n\n### 3. 数据概览\n- 今日数据：订单量、销售额、访客数、转化率\n- 销售趋势：近30天折线图\n- 热销商品 TOP10',
      )
    "
    >电商后台</a-button
  >
  <a-button
    type="default"
    @click="
      setPrompt(
        '# 内容管理平台\n\n## 功能模块\n\n### 1. 文章管理\n- 文章列表：标题、作者、分类、状态、发布时间\n- 文章编辑：标题、摘要、正文编辑器、封面图、标签\n- 分类管理：分类列表、新增/编辑/删除\n\n### 2. 评论管理\n- 评论列表：用户、内容、关联文章、时间、状态\n- 审核操作：通过、拒绝、删除\n\n### 3. 数据统计\n- 内容数据：总文章数、总评论数、总浏览量\n- 趋势图：近7天发布量和浏览量',
      )
    "
    >内容管理平台</a-button
  >
  <a-button
    type="default"
    @click="
      setPrompt(
        '# 客户关系管理系统（CRM）\n\n## 功能模块\n\n### 1. 客户管理\n- 客户列表：公司名称、联系人、电话、来源、状态\n- 客户详情：基本信息、跟进记录、合同记录\n- 新增客户：表单包含公司信息和联系人信息\n\n### 2. 销售漏斗\n- 销售阶段看板：初步接触、需求确认、方案报价、合同签署\n- 拖拽卡片移动客户到不同阶段\n- 各阶段金额统计\n\n### 3. 合同管理\n- 合同列表：合同编号、客户、金额、状态、签署日期\n- 合同详情：基本信息、关联客户、附件预览',
      )
    "
    >CRM 系统</a-button
  >
</div>
```

- [ ] **Step 4: Commit**

```bash
git add bing-ai-code-frontend/src/pages/HomePage.vue
git commit -m "refactor: redesign HomePage for PRD→prototype workflow"
```

---

### Task 7: Update frontend — GlobalHeader.vue and index.html

**Files:**
- Modify: `bing-ai-code-frontend/src/components/GlobalHeader.vue:9`
- Modify: `bing-ai-code-frontend/index.html:7`

- [ ] **Step 1: Update GlobalHeader.vue site title (line 9)**

Change:
```html
<h1 class="site-title">网页搭建</h1>
```

To:
```html
<h1 class="site-title">AI 原型生成器</h1>
```

- [ ] **Step 2: Update index.html page title (line 7)**

Change:
```html
<title> 前端网页搭建助手</title>
```

To:
```html
<title>AI 原型生成器 - 粘贴 PRD，生成可交互原型</title>
```

- [ ] **Step 3: Commit**

```bash
git add bing-ai-code-frontend/src/components/GlobalHeader.vue bing-ai-code-frontend/index.html
git commit -m "refactor: update branding to AI 原型生成器"
```

---

### Task 8: Update README.md

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Rewrite README**

Replace the entire file with:

```markdown
## AI 文档→原型生成器

### 项目简介

基于 LangChain4j + LangGraph4j 的 AI 原型生成平台。产品经理粘贴 PRD 需求文档，AI Agent 自动解析文档结构并执行素材搜集、代码生成、质量检查、项目构建的完整工作流，生成可交互的多页面 HTML 原型，一键部署为可访问链接供团队快速评审。

### 4 大核心能力

1）PRD 智能解析：用户粘贴 Markdown 格式的需求文档，AI 自动分析页面结构、功能模块和交互流程，智能选择 HTML / 多文件 / Vue 项目三种生成策略。

2）可交互原型生成：生成的原型支持页面间导航跳转、表单输入、按钮点击反馈，让产品评审时能感受到真实产品的操作体验。

3）一键部署分享：可以将生成的原型一键部署到云端并自动截取封面图，获得可访问的地址进行分享，同时支持完整项目源码下载。

4）企业级管理：提供用户管理、应用管理、系统监控、业务指标监控等后台功能，管理员可以设置精选应用、监控 AI 调用情况和系统性能。

### 技术栈

Spring Boot 3 + LangChain4j + LangGraph4j + MyBatis Flex + COS 对象存储 + Redis + Nginx
```

- [ ] **Step 2: Commit**

```bash
git add README.md
git commit -m "docs: rewrite README for AI 原型生成器 positioning"
```

---

### Task 9: Update generate_report.py and generate_ppt.py titles

**Files:**
- Modify: `generate_report.py`
- Modify: `generate_ppt.py`

- [ ] **Step 1: Update generate_report.py (3 occurrences)**

Line 16: Change `AI 驱动的前端网页搭建助手\n项目报告` → `AI 文档→原型生成器\n项目报告`

Line 27: Change `AI 驱动的前端网页搭建助手` → `AI 文档→原型生成器`

Line 256: Change `AI前端网页搭建助手_项目报告.docx` → `AI原型生成器_项目报告.docx`

- [ ] **Step 2: Update generate_ppt.py (3 occurrences)**

Line 71: Change `AI 驱动的前端网页搭建助手` → `AI 文档→原型生成器`

Line 370: Change `AI 驱动的前端网页搭建助手 — 让每个人都能轻松创建 Web 应用` → `AI 文档→原型生成器 — 粘贴 PRD，分钟级生成可交互原型`

Line 373: Change `AI前端网页搭建助手_项目汇报.pptx` → `AI原型生成器_项目汇报.pptx`

- [ ] **Step 3: Commit**

```bash
git add generate_report.py generate_ppt.py
git commit -m "refactor: update report/ppt titles to AI 原型生成器"
```

---

### Task 10: Sync updated prompts to Python backend (bing-ai-code-backend/prompts/)

**Files:**
- Verify: `bing-ai-code-backend/prompts/` has the latest prompts from Tasks 1-4

- [ ] **Step 1: Verify prompt sync**

Compare the 4 updated prompt files between `src/main/resources/prompt/` and `bing-ai-code-backend/prompts/`:

```bash
diff src/main/resources/prompt/codegen-html-system-prompt.txt bing-ai-code-backend/prompts/codegen-html-system-prompt.txt
diff src/main/resources/prompt/codegen-multi-file-system-prompt.txt bing-ai-code-backend/prompts/codegen-multi-file-system-prompt.txt
diff src/main/resources/prompt/codegen-vue-project-system-prompt.txt bing-ai-code-backend/prompts/codegen-vue-project-system-prompt.txt
diff src/main/resources/prompt/codegen-routing-system-prompt.txt bing-ai-code-backend/prompts/codegen-routing-system-prompt.txt
```

Expected: No differences (already synced in Task 5).

- [ ] **Step 2: Verify no remaining "网页搭建" references**

```bash
grep -r "网页搭建" --include="*.vue" --include="*.html" --include="*.py" --include="*.txt" --include="*.md" --include="*.java" .
```

Expected: Only matches in the design spec file (`docs/superpowers/specs/...`) and git history.

---

## Spec Coverage Check

| Spec Requirement | Task |
|-----------------|------|
| Prompt 词改为 PRD→原型 | Tasks 1-4 |
| 前端 UI 改为粘贴 PRD | Task 6 |
| 品牌文案更新 | Tasks 7, 8, 9 |
| Python 后端 prompt 同步 | Task 5, 10 |
| 后端引擎零改动 | N/A (no changes needed) |

## Verification

After all tasks complete:

1. Start the Java backend: `mvn spring-boot:run`
2. Start the Vue frontend: `npm run dev`
3. Verify home page shows "AI 原型生成器" branding
4. Paste a sample PRD into the textarea and submit
5. Verify AI generates a prototype (check SSE stream works)
6. Deploy the generated prototype and verify it's accessible
7. Run `grep -r "网页搭建"` to confirm no stale branding remains
