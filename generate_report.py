from docx import Document
from docx.shared import Inches, Pt, Cm, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT

doc = Document()

# Set default font
style = doc.styles['Normal']
font = style.font
font.name = '宋体'
font.size = Pt(12)

# ===== Title =====
title = doc.add_heading('', level=0)
run = title.add_run('AI 文档→原型生成器\n项目报告')
run.font.size = Pt(22)
run.font.color.rgb = RGBColor(0x00, 0x52, 0xCC)
title.alignment = WD_ALIGN_PARAGRAPH.CENTER

doc.add_paragraph('')

# Project info table
table = doc.add_table(rows=4, cols=4)
table.style = 'Light Grid Accent 1'
info = [
    ['项目名称', 'AI 文档→原型生成器', '项目类型', 'Web 应用'],
    ['技术栈', 'Java 21 + Spring Boot + Vue 3', '开发周期', '迭代开发'],
    ['开发人员', '许敬彬', '项目仓库', 'bing-ai-code'],
    ['核心框架', 'LangChain4j + LangGraph4j', 'AI 模型', 'DeepSeek + Qwen'],
]
for i, row_data in enumerate(info):
    for j, cell_text in enumerate(row_data):
        table.cell(i, j).text = cell_text

doc.add_paragraph('')

# ===== 1. 项目目标 =====
doc.add_heading('一、项目目标', level=1)

doc.add_heading('1.1 项目背景', level=2)
doc.add_paragraph(
    '随着大语言模型（LLM）技术的快速发展，AI 辅助编程已成为软件开发的重要趋势。'
    '传统前端开发需要开发者掌握 HTML、CSS、JavaScript 等多种技术，学习曲线较陡。'
    '本项目旨在构建一个 AI 驱动的前端网页搭建平台，用户只需通过自然语言描述需求，'
    '系统即可自动生成完整的、可运行的前端应用，大幅降低前端开发门槛。'
)

doc.add_heading('1.2 核心目标', level=2)
goals = [
    '智能代码生成：用户输入自然语言描述，AI 自动分析需求并选择最优生成策略（单文件 HTML / 多文件静态页面 / Vue 项目），通过工具调用生成完整代码文件，采用 SSE 流式输出让用户实时看到 AI 执行过程。',
    '可视化编辑：生成的应用实时渲染展示，支持编辑模式下选择网页元素，与 AI 对话迭代修改页面，实现所见即所得的开发体验。',
    '一键部署分享：将生成的应用一键部署到云端，自动截取封面图，获得可访问地址进行分享，同时支持完整项目源码 ZIP 下载。',
    '企业级管理：提供用户管理、应用管理、系统监控（Prometheus + Grafana）、AI 调用追踪等后台功能，管理员可设置精选应用、监控系统性能。',
]
for g in goals:
    doc.add_paragraph(g, style='List Bullet')

# ===== 2. 项目内容 =====
doc.add_heading('二、项目内容', level=1)

doc.add_heading('2.1 系统架构', level=2)
doc.add_paragraph(
    '本系统采用前后端分离架构，后端基于 Java 21 + Spring Boot 3.5.4 构建，'
    '前端使用 Vue 3.5 + Vite 7 + TypeScript 开发。整体架构分为五层：'
)
layers = [
    '前端展示层：Vue 3 + Ant Design Vue + Pinia 状态管理',
    '网关服务层：Spring Boot REST API + SSE 流式响应 + 限流鉴权',
    'AI 编排层：LangChain4j + LangGraph4j 工作流编排 + Tool Calling',
    '模型服务层：DeepSeek (代码生成) + Qwen-Turbo (智能路由) + DashScope (图像生成)',
    '数据存储层：MySQL + MyBatis-Flex + Redis + 腾讯云 COS 对象存储',
]
for l in layers:
    doc.add_paragraph(l, style='List Number')

doc.add_heading('2.2 记忆（Memory）', level=2)
doc.add_paragraph(
    '系统实现了多层次的记忆机制，确保 AI 在代码生成过程中能够充分利用上下文信息：'
)
memory_items = [
    'Redis 聊天记忆：每个应用拥有独立的 MessageWindowChatMemory，存储最近 20 条对话上下文，支持多轮对话的连续性。AI 能够记住用户之前的需求和修改，实现连贯的代码生成体验。',
    '数据库持久化：chat_history 表存储全量对话历史记录，服务重启后自动从数据库加载历史消息到 Redis 内存，确保对话不丢失。',
    'Caffeine 本地缓存：AI 服务实例使用 Caffeine 缓存管理（最大 1000 实例，30 分钟写过期），避免重复创建 AI 服务实例，提升响应速度。',
    '工作流状态传递：WorkflowContext 在 LangGraph4j 工作流的各节点间传递上下文，包括用户提示词、图像资源列表、代码生成结果等，确保整个生成流程的状态一致性。',
]
for m in memory_items:
    doc.add_paragraph(m, style='List Bullet')

doc.add_heading('2.3 规划（Planning）', level=2)
doc.add_paragraph(
    '系统采用多层规划机制，从需求分析到代码生成的全流程都由 AI 智能规划驱动：'
)
planning_items = [
    '智能路由规划：使用 Qwen-Turbo 轻量模型分析用户需求复杂度，自动选择最优的代码生成策略——简单页面走单文件 HTML，中等复杂度走多文件静态页面，复杂应用走完整 Vue 项目。',
    'LangGraph4j 工作流编排：基于有向图的工作流引擎，节点依次为：图像采集 → 提示词增强 → 智能路由 → 代码生成 → 质量检查 → 项目构建。每个节点都有明确的职责和条件分支。',
    '图像采集规划：AI 根据用户需求自动制定图像采集方案，确定需要哪些类型的图像资源（配图、插画、架构图、Logo），并通过并发扇出/扇入模式并行采集。',
    '质量检查闭环：代码生成后自动进行质量检查，如果检查失败则携带错误信息回退到代码生成节点重新生成，形成闭环直到代码通过质量检查。',
]
for p in planning_items:
    doc.add_paragraph(p, style='List Bullet')

doc.add_heading('2.4 工具（Tools）', level=2)
doc.add_paragraph(
    '系统为 AI Agent 提供了丰富的工具集，使其能够自主完成从需求分析到项目部署的全流程：'
)
tools_items = [
    '文件操作工具集：writeFile（创建文件）、readFile（读取文件）、modifyFile（修改文件）、deleteFile（删除文件）、dirRead（读取目录）、exit（结束生成）。AI 通过调用这些工具直接在文件系统中创建真实项目。',
    'Pexels 图像搜索工具：按关键词搜索高质量图片资源，自动为生成的网页匹配合适的配图，提升页面视觉效果。',
    'Logo 生成器：AI 驱动的品牌 Logo 生成工具，根据用户描述自动生成品牌标识。',
    'Mermaid 图表工具：自动生成架构图、流程图等 Mermaid 图表，通过 CLI 渲染为 PNG 并上传至腾讯云 COS。',
    'UnDraw 插画工具：获取风格统一的矢量插画资源，为网页增添专业的视觉元素。',
    'Selenium 截图工具：应用部署后自动使用 Selenium WebDriver 截取页面截图，作为应用封面图。',
]
for t in tools_items:
    doc.add_paragraph(t, style='List Bullet')

doc.add_heading('2.5 核心技术栈', level=2)

# Backend table
doc.add_paragraph('后端技术栈：')
backend_table = doc.add_table(rows=11, cols=3)
backend_table.style = 'Light Grid Accent 1'
backend_table.cell(0, 0).text = '技术'
backend_table.cell(0, 1).text = '版本'
backend_table.cell(0, 2).text = '用途'
backend_data = [
    ['Java', '21', '开发语言'],
    ['Spring Boot', '3.5.4', 'Web 框架'],
    ['LangChain4j', '1.1.0', 'AI 框架集成'],
    ['LangGraph4j', '1.6.0-rc2', 'AI 工作流编排'],
    ['DeepSeek', 'deepseek-chat/reasoner', '代码生成模型'],
    ['MySQL + MyBatis-Flex', '8.x', '数据持久化'],
    ['Redis + Redisson', '7.x', '缓存与限流'],
    ['Selenium', '4.x', '页面截图'],
    ['腾讯云 COS', '-', '对象存储'],
    ['Prometheus + Grafana', '-', '系统监控'],
]
for i, row_data in enumerate(backend_data):
    for j, cell_text in enumerate(row_data):
        backend_table.cell(i + 1, j).text = cell_text

doc.add_paragraph('')
doc.add_paragraph('前端技术栈：')
frontend_table = doc.add_table(rows=7, cols=3)
frontend_table.style = 'Light Grid Accent 1'
frontend_table.cell(0, 0).text = '技术'
frontend_table.cell(0, 1).text = '版本'
frontend_table.cell(0, 2).text = '用途'
frontend_data = [
    ['Vue 3', '3.5', '前端框架'],
    ['Vite', '7', '构建工具'],
    ['TypeScript', '5.8', '类型安全'],
    ['Ant Design Vue', '4', 'UI 组件库'],
    ['Pinia', '3', '状态管理'],
    ['Axios', '-', 'HTTP 客户端'],
]
for i, row_data in enumerate(frontend_data):
    for j, cell_text in enumerate(row_data):
        frontend_table.cell(i + 1, j).text = cell_text

doc.add_heading('2.6 设计模式', level=2)
patterns = [
    '工厂模式（Factory Pattern）：AiCodeGeneratorServiceFactory 使用 Caffeine 缓存创建和管理 AI 服务实例。',
    '策略模式（Strategy Pattern）：CodeGenTypeEnum 定义三种代码生成策略，AI 路由服务自动选择最优策略。',
    '模板方法模式（Template Method Pattern）：CodeFileSaverTemplate 定义文件保存流程骨架，子类实现具体保存逻辑。',
    '外观模式（Facade Pattern）：AiCodeGeneratorFacade 提供统一的代码生成入口，屏蔽底层复杂性。',
    '工作流图模式（Graph/Workflow Pattern）：LangGraph4j 实现有向图节点编排，串联整个代码生成流程。',
    '并发扇出/扇入模式：实现图像资源的并行采集（Pexels + UnDraw + Mermaid + Logo），提升生成效率。',
    '工具调用模式（Tool Calling）：AI 通过调用工具直接操作文件系统，而非在对话中输出代码文本。',
    '护栏模式（Guardrail Pattern）：PromptSafetyInputGuardrail 实现输入安全检测，防止 Prompt 注入攻击。',
]
for p in patterns:
    doc.add_paragraph(p, style='List Bullet')

# ===== 3. 系统演示 =====
doc.add_heading('三、系统演示', level=1)

doc.add_heading('3.1 核心流程', level=2)
doc.add_paragraph(
    '系统的代码生成流程如下：用户在聊天界面输入自然语言需求描述 → AI 智能路由分析需求复杂度并选择生成策略 → '
    'LangGraph 工作流启动，首先进行图像资源采集（并发采集 Pexels 配图、Logo、插画、架构图） → '
    '提示词增强，将图像 URL 注入用户提示词 → DeepSeek-Reasoner 模型通过工具调用生成代码文件 → '
    '自动质量检查，失败则回退重新生成 → Vue 项目自动执行 npm install 和 npm run build → '
    '部署上线并使用 Selenium 自动截图生成封面 → 用户获得可访问链接进行分享。'
)

doc.add_heading('3.2 功能展示', level=2)
features = [
    '智能代码生成页面：用户输入需求后，实时看到 AI 的执行过程，包括文件创建、代码生成、工具调用等流式消息。',
    '可视化编辑页面：生成的应用在 iframe 中实时渲染，用户可进入编辑模式选择元素，通过对话修改页面。',
    '应用管理首页：展示精选应用和所有已生成的应用，支持搜索、查看详情、下载源码。',
    '管理后台：用户管理、应用管理、对话管理、Prometheus + Grafana 监控面板。',
]
for f in features:
    doc.add_paragraph(f, style='List Bullet')

# ===== 4. 成员分工 =====
doc.add_heading('四、成员分工', level=1)

member_table = doc.add_table(rows=2, cols=3)
member_table.style = 'Light Grid Accent 1'
member_table.cell(0, 0).text = '成员'
member_table.cell(0, 1).text = '角色'
member_table.cell(0, 2).text = '主要职责'
member_table.cell(1, 0).text = '许敬彬'
member_table.cell(1, 1).text = '项目负责人 / 全栈开发'
member_table.cell(1, 2).text = (
    '整体架构设计与技术选型；'
    'AI 工作流编排（LangGraph4j）核心开发；'
    '代码生成引擎（LangChain4j Tool Calling）开发；'
    '前端 Vue 3 应用开发；'
    '后端 Spring Boot API 开发；'
    '数据库设计与系统集成部署。'
)

# ===== 5. 项目总结 =====
doc.add_heading('五、项目总结', level=1)

doc.add_heading('5.1 技术亮点', level=2)
highlights = [
    '基于 LangGraph4j 的有向图工作流编排，实现复杂的多步骤 AI 生成流程，节点间支持条件分支和循环。',
    '并发扇出/扇入模式实现图像资源的并行采集，显著提升生成效率。',
    '工具调用模式（Tool Calling）让 AI 直接操作文件系统生成真实项目，而非简单的代码片段输出。',
    '多模型协作架构：DeepSeek 负责代码生成、Qwen-Turbo 负责智能路由、DashScope 负责图像生成，各司其职。',
    'SSE 流式响应 + Reactor Flux，为用户提供实时的代码生成体验。',
    '质量检查闭环机制：自动质检 + 失败回退重试，确保生成代码的质量。',
    '防护栏机制：Prompt 注入检测 + 敏感词过滤 + 输入长度限制，保障系统安全。',
]
for h in highlights:
    doc.add_paragraph(h, style='List Bullet')

doc.add_heading('5.2 项目成果', level=2)
results = [
    '完整实现了四大核心功能：智能代码生成、可视化编辑、一键部署分享、企业级管理。',
    '前后端分离架构，后端涵盖 40+ 核心类，支持三种代码生成策略（HTML / 多文件 / Vue 项目）。',
    '集成了完整的监控体系（Prometheus + Grafana），支持 AI 模型调用指标追踪和系统性能监控。',
    '实现了基于角色的权限控制（RBAC），支持普通用户和管理员两种角色。',
    '采用 Redis + Caffeine 双层缓存架构，有效提升系统响应速度。',
]
for r in results:
    doc.add_paragraph(r, style='List Bullet')

doc.add_heading('5.3 未来展望', level=2)
futures = [
    '扩展前端框架支持：除 Vue 3 外，增加 React、Next.js、Nuxt.js 等框架的支持。',
    '更多 AI 模型接入：支持用户自选 AI 模型，接入 Claude、GPT-4 等更多大语言模型。',
    '增强可视化编辑：引入拖拽式布局编辑器，进一步降低页面定制门槛。',
    '团队协作功能：支持多人协作编辑同一应用，引入版本管理和冲突解决机制。',
    '应用市场生态：构建应用模板市场，用户可以分享和复用优质模板。',
]
for f in futures:
    doc.add_paragraph(f, style='List Bullet')

# Save
output_path = r"C:\Users\许敬彬\Desktop\AI原型生成器_项目报告.docx"
doc.save(output_path)
print(f"Report saved to: {output_path}")
