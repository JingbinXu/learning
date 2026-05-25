from pptx import Presentation
from pptx.util import Inches, Pt, Emu
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR
from pptx.enum.shapes import MSO_SHAPE

prs = Presentation()
prs.slide_width = Inches(13.333)
prs.slide_height = Inches(7.5)

# Color scheme
DARK_BG = RGBColor(0x1A, 0x1A, 0x2E)
ACCENT_BLUE = RGBColor(0x00, 0x96, 0xFF)
ACCENT_CYAN = RGBColor(0x00, 0xD4, 0xAA)
WHITE = RGBColor(0xFF, 0xFF, 0xFF)
LIGHT_GRAY = RGBColor(0xCC, 0xCC, 0xCC)
CARD_BG = RGBColor(0x25, 0x25, 0x40)
ORANGE = RGBColor(0xFF, 0x8C, 0x00)
GREEN = RGBColor(0x00, 0xE6, 0x76)
PURPLE = RGBColor(0xBB, 0x86, 0xFC)

def set_slide_bg(slide, color):
    bg = slide.background
    fill = bg.fill
    fill.solid()
    fill.fore_color.rgb = color

def add_shape(slide, left, top, width, height, fill_color, border_color=None):
    shape = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, left, top, width, height)
    shape.fill.solid()
    shape.fill.fore_color.rgb = fill_color
    if border_color:
        shape.line.color.rgb = border_color
        shape.line.width = Pt(1.5)
    else:
        shape.line.fill.background()
    return shape

def add_text_box(slide, left, top, width, height, text, font_size=18, color=WHITE, bold=False, alignment=PP_ALIGN.LEFT):
    txBox = slide.shapes.add_textbox(left, top, width, height)
    tf = txBox.text_frame
    tf.word_wrap = True
    p = tf.paragraphs[0]
    p.text = text
    p.font.size = Pt(font_size)
    p.font.color.rgb = color
    p.font.bold = bold
    p.alignment = alignment
    return txBox

def add_bullet_text(slide, left, top, width, height, items, font_size=16, color=WHITE, bullet_color=ACCENT_CYAN):
    txBox = slide.shapes.add_textbox(left, top, width, height)
    tf = txBox.text_frame
    tf.word_wrap = True
    for i, item in enumerate(items):
        if i == 0:
            p = tf.paragraphs[0]
        else:
            p = tf.add_paragraph()
        p.text = f"  {item}"
        p.font.size = Pt(font_size)
        p.font.color.rgb = color
        p.space_after = Pt(8)
    return txBox

# ========== Slide 1: Title ==========
slide = prs.slides.add_slide(prs.slide_layouts[6])  # blank
set_slide_bg(slide, DARK_BG)

add_text_box(slide, Inches(1), Inches(1.5), Inches(11), Inches(1.2),
             "AI 文档→原型生成器", font_size=44, color=WHITE, bold=True, alignment=PP_ALIGN.CENTER)
add_text_box(slide, Inches(1), Inches(3.0), Inches(11), Inches(0.8),
             "AI-Powered Frontend Web Page Builder", font_size=24, color=ACCENT_CYAN, alignment=PP_ALIGN.CENTER)

# Decorative line
shape = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(4.5), Inches(4.2), Inches(4.3), Pt(3))
shape.fill.solid()
shape.fill.fore_color.rgb = ACCENT_BLUE
shape.line.fill.background()

add_text_box(slide, Inches(1), Inches(4.8), Inches(11), Inches(0.6),
             "基于 LangChain4j + LangGraph4j 的智能代码生成平台", font_size=20, color=LIGHT_GRAY, alignment=PP_ALIGN.CENTER)
add_text_box(slide, Inches(1), Inches(5.6), Inches(11), Inches(0.5),
             "Java 21 · Spring Boot · DeepSeek · Vue 3", font_size=16, color=ACCENT_BLUE, alignment=PP_ALIGN.CENTER)

# ========== Slide 2: 项目目标 ==========
slide = prs.slides.add_slide(prs.slide_layouts[6])
set_slide_bg(slide, DARK_BG)

add_text_box(slide, Inches(0.8), Inches(0.4), Inches(5), Inches(0.8),
             "项目目标", font_size=36, color=ACCENT_BLUE, bold=True)

shape = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0.8), Inches(1.2), Inches(2), Pt(3))
shape.fill.solid(); shape.fill.fore_color.rgb = ACCENT_CYAN; shape.line.fill.background()

# Goals cards
goals = [
    ("智能代码生成", "用户通过自然语言描述需求，AI 自动分析\n并选择最优生成策略（HTML/多文件/Vue 项目），\n通过工具调用生成完整可运行的前端项目。", ACCENT_BLUE),
    ("可视化编辑", "生成的应用实时渲染预览，支持编辑模式\n下选择网页元素，与 AI 对话迭代修改页面，\n所见即所得的开发体验。", ACCENT_CYAN),
    ("一键部署分享", "应用一键部署到云端，自动截取封面图，\n获得可访问地址进行分享，同时支持\n完整项目源码下载。", GREEN),
    ("企业级管理", "用户管理、应用管理、系统监控、\nAI 调用追踪，Prometheus + Grafana\n可视化监控面板。", PURPLE),
]

for i, (title, desc, accent) in enumerate(goals):
    x = Inches(0.8 + i * 3.1)
    card = add_shape(slide, x, Inches(1.8), Inches(2.8), Inches(4.5), CARD_BG, accent)
    add_text_box(slide, x + Inches(0.2), Inches(2.0), Inches(2.4), Inches(0.6),
                 title, font_size=22, color=accent, bold=True, alignment=PP_ALIGN.CENTER)
    # Accent bar
    bar = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, x + Inches(0.8), Inches(2.7), Inches(1.2), Pt(2))
    bar.fill.solid(); bar.fill.fore_color.rgb = accent; bar.line.fill.background()
    add_text_box(slide, x + Inches(0.15), Inches(3.0), Inches(2.5), Inches(3.0),
                 desc, font_size=14, color=LIGHT_GRAY)

# ========== Slide 3: 项目内容 - 架构概览 ==========
slide = prs.slides.add_slide(prs.slide_layouts[6])
set_slide_bg(slide, DARK_BG)

add_text_box(slide, Inches(0.8), Inches(0.4), Inches(5), Inches(0.8),
             "项目内容 — 技术架构", font_size=36, color=ACCENT_BLUE, bold=True)

shape = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0.8), Inches(1.2), Inches(2), Pt(3))
shape.fill.solid(); shape.fill.fore_color.rgb = ACCENT_CYAN; shape.line.fill.background()

# Architecture layers
layers = [
    ("前端层", "Vue 3 + Vite + TypeScript + Ant Design Vue + Pinia", ACCENT_BLUE, Inches(1.8)),
    ("网关层", "Spring Boot 3.5 + SSE 流式响应 + 限流 + 鉴权", ACCENT_CYAN, Inches(2.7)),
    ("AI 编排层", "LangChain4j + LangGraph4j 工作流编排 + Tool Calling", GREEN, Inches(3.6)),
    ("模型层", "DeepSeek (代码生成) + Qwen-Turbo (路由) + DashScope (图像)", ORANGE, Inches(4.5)),
    ("数据层", "MySQL + MyBatis-Flex + Redis + 腾讯云 COS", PURPLE, Inches(5.4)),
]

for title, desc, accent, y in layers:
    bar = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0.8), y, Inches(0.15), Inches(0.7))
    bar.fill.solid(); bar.fill.fore_color.rgb = accent; bar.line.fill.background()
    add_text_box(slide, Inches(1.2), y, Inches(2.2), Inches(0.5), title, font_size=20, color=accent, bold=True)
    add_text_box(slide, Inches(3.5), y, Inches(8.5), Inches(0.5), desc, font_size=16, color=LIGHT_GRAY)

# Right side: design patterns
add_shape(slide, Inches(8.5), Inches(1.8), Inches(4.2), Inches(4.8), CARD_BG, ACCENT_BLUE)
add_text_box(slide, Inches(8.8), Inches(1.9), Inches(3.6), Inches(0.5),
             "设计模式", font_size=20, color=ACCENT_BLUE, bold=True, alignment=PP_ALIGN.CENTER)

patterns = [
    "  工厂模式 — AI 服务实例管理",
    "  策略模式 — 三种代码生成策略",
    "  模板方法 — 代码文件保存",
    "  外观模式 — 统一代码生成入口",
    "  工作流图模式 — LangGraph4j 编排",
    "  并发扇出/扇入 — 图像并行采集",
    "  工具调用模式 — AI 自主文件操作",
    "  防护栏模式 — 输入安全检测",
]
add_bullet_text(slide, Inches(8.8), Inches(2.5), Inches(3.8), Inches(4.0),
                patterns, font_size=13, color=LIGHT_GRAY)

# ========== Slide 4: 项目内容 — 记忆、规划、工具 ==========
slide = prs.slides.add_slide(prs.slide_layouts[6])
set_slide_bg(slide, DARK_BG)

add_text_box(slide, Inches(0.8), Inches(0.4), Inches(8), Inches(0.8),
             "项目内容 — 记忆 · 规划 · 工具", font_size=36, color=ACCENT_BLUE, bold=True)

shape = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0.8), Inches(1.2), Inches(2), Pt(3))
shape.fill.solid(); shape.fill.fore_color.rgb = ACCENT_CYAN; shape.line.fill.background()

# Memory
add_shape(slide, Inches(0.8), Inches(1.8), Inches(3.7), Inches(5.0), CARD_BG, ACCENT_BLUE)
add_text_box(slide, Inches(1.1), Inches(1.9), Inches(3.1), Inches(0.5),
             "记忆 Memory", font_size=24, color=ACCENT_BLUE, bold=True, alignment=PP_ALIGN.CENTER)
memory_items = [
    "  Redis 聊天记忆：每应用独立 MessageWindowChatMemory",
    "    存储最近 20 条对话上下文",
    "  数据库持久化：chat_history 表存储全量对话",
    "    服务重启后自动加载历史到内存",
    "  Caffeine 本地缓存：AI 服务实例缓存",
    "    最大 1000 实例，30 分钟写过期",
    "  工作流状态：WorkflowContext 在图节点间",
    "    传递上下文（提示词、图像资源、代码结果）",
]
add_bullet_text(slide, Inches(1.0), Inches(2.6), Inches(3.3), Inches(4.0),
                memory_items, font_size=13, color=LIGHT_GRAY)

# Planning
add_shape(slide, Inches(4.8), Inches(1.8), Inches(3.7), Inches(5.0), CARD_BG, ACCENT_CYAN)
add_text_box(slide, Inches(5.1), Inches(1.9), Inches(3.1), Inches(0.5),
             "规划 Planning", font_size=24, color=ACCENT_CYAN, bold=True, alignment=PP_ALIGN.CENTER)
planning_items = [
    "  智能路由规划：Qwen-Turbo 分析用户需求",
    "    自动选择 HTML / 多文件 / Vue 项目策略",
    "  LangGraph4j 工作流：有向图节点编排",
    "    图像采集→提示词增强→路由→生成→质检",
    "  图像采集规划：AI 制定图像采集方案",
    "    并发采集 Pexels / UnDraw / Mermaid / Logo",
    "  质量检查闭环：质检失败自动回退重新生成",
    "    最多循环直到代码通过质量检查",
]
add_bullet_text(slide, Inches(5.0), Inches(2.6), Inches(3.3), Inches(4.0),
                planning_items, font_size=13, color=LIGHT_GRAY)

# Tools
add_shape(slide, Inches(8.8), Inches(1.8), Inches(3.7), Inches(5.0), CARD_BG, GREEN)
add_text_box(slide, Inches(9.1), Inches(1.9), Inches(3.1), Inches(0.5),
             "工具 Tools", font_size=24, color=GREEN, bold=True, alignment=PP_ALIGN.CENTER)
tools_items = [
    "  文件操作工具集：writeFile / readFile /",
    "    modifyFile / deleteFile / dirRead / exit",
    "  Pexels 图像搜索：按关键词搜索高质量图片",
    "    自动为生成的网页匹配配图",
    "  Logo 生成器：AI 驱动的品牌 Logo 生成",
    "  Mermaid 图表：自动生成架构图 / 流程图",
    "    CLI 渲染为 PNG 并上传 COS",
    "  UnDraw 插画：获取风格统一的矢量插画",
    "  Selenium 截图：自动截取部署页面封面图",
]
add_bullet_text(slide, Inches(9.0), Inches(2.6), Inches(3.3), Inches(4.0),
                tools_items, font_size=13, color=LIGHT_GRAY)

# ========== Slide 5: 系统演示 ==========
slide = prs.slides.add_slide(prs.slide_layouts[6])
set_slide_bg(slide, DARK_BG)

add_text_box(slide, Inches(0.8), Inches(0.4), Inches(5), Inches(0.8),
             "系统演示", font_size=36, color=ACCENT_BLUE, bold=True)

shape = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0.8), Inches(1.2), Inches(2), Pt(3))
shape.fill.solid(); shape.fill.fore_color.rgb = ACCENT_CYAN; shape.line.fill.background()

# Demo flow steps
demo_steps = [
    ("01", "输入需求", "用户在聊天界面输入自然语言描述\n如：\"帮我做一个个人简历网站\"", ACCENT_BLUE),
    ("02", "AI 智能路由", "Qwen-Turbo 分析需求复杂度\n自动选择 Vue 项目生成策略", ACCENT_CYAN),
    ("03", "图像资源采集", "LangGraph 工作流并发采集\nPexels 配图 + Logo + 插画", GREEN),
    ("04", "代码生成", "DeepSeek-Reasoner 通过工具调用\n流式输出，实时创建项目文件", ORANGE),
    ("05", "质量检查", "自动质检 + 修复循环\nnpm install & build 构建", PURPLE),
    ("06", "部署分享", "一键部署 + Selenium 截图\n生成可访问链接和封面图", ACCENT_BLUE),
]

for i, (num, title, desc, accent) in enumerate(demo_steps):
    col = i % 3
    row = i // 3
    x = Inches(0.8 + col * 4.1)
    y = Inches(1.6 + row * 2.9)

    add_shape(slide, x, y, Inches(3.8), Inches(2.5), CARD_BG, accent)
    # Number circle
    circle = slide.shapes.add_shape(MSO_SHAPE.OVAL, x + Inches(0.15), y + Inches(0.15), Inches(0.5), Inches(0.5))
    circle.fill.solid(); circle.fill.fore_color.rgb = accent; circle.line.fill.background()
    tf = circle.text_frame; tf.paragraphs[0].text = num
    tf.paragraphs[0].font.size = Pt(16); tf.paragraphs[0].font.color.rgb = WHITE
    tf.paragraphs[0].font.bold = True; tf.paragraphs[0].alignment = PP_ALIGN.CENTER

    add_text_box(slide, x + Inches(0.8), y + Inches(0.15), Inches(2.8), Inches(0.5),
                 title, font_size=20, color=accent, bold=True)
    add_text_box(slide, x + Inches(0.2), y + Inches(0.9), Inches(3.4), Inches(1.4),
                 desc, font_size=14, color=LIGHT_GRAY)

# ========== Slide 6: 成员分工 ==========
slide = prs.slides.add_slide(prs.slide_layouts[6])
set_slide_bg(slide, DARK_BG)

add_text_box(slide, Inches(0.8), Inches(0.4), Inches(5), Inches(0.8),
             "成员分工", font_size=36, color=ACCENT_BLUE, bold=True)

shape = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0.8), Inches(1.2), Inches(2), Pt(3))
shape.fill.solid(); shape.fill.fore_color.rgb = ACCENT_CYAN; shape.line.fill.background()

members = [
    ("许敬彬 (JingbinXu)", "项目负责人 / 全栈开发", [
        "整体架构设计与技术选型",
        "AI 工作流编排（LangGraph4j）",
        "核心代码生成引擎开发",
        "系统集成与部署",
    ], ACCENT_BLUE),
    ("许敬彬 (bingbingedu)", "前端开发 / 后端开发", [
        "Vue 3 前端应用开发",
        "可视化编辑功能实现",
        "Spring Boot 后端 API",
        "数据库设计与用户管理",
    ], ACCENT_CYAN),
]

for i, (name, role, tasks, accent) in enumerate(members):
    x = Inches(0.8 + i * 6.2)
    add_shape(slide, x, Inches(1.8), Inches(5.8), Inches(5.0), CARD_BG, accent)

    # Avatar circle
    circle = slide.shapes.add_shape(MSO_SHAPE.OVAL, x + Inches(0.3), Inches(2.0), Inches(0.8), Inches(0.8))
    circle.fill.solid(); circle.fill.fore_color.rgb = accent; circle.line.fill.background()
    tf = circle.text_frame; tf.paragraphs[0].text = name[0]
    tf.paragraphs[0].font.size = Pt(24); tf.paragraphs[0].font.color.rgb = WHITE
    tf.paragraphs[0].font.bold = True; tf.paragraphs[0].alignment = PP_ALIGN.CENTER

    add_text_box(slide, x + Inches(1.3), Inches(2.0), Inches(4.2), Inches(0.4),
                 name, font_size=20, color=WHITE, bold=True)
    add_text_box(slide, x + Inches(1.3), Inches(2.5), Inches(4.2), Inches(0.4),
                 role, font_size=16, color=accent)

    bar = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, x + Inches(0.3), Inches(3.2), Inches(5.2), Pt(1))
    bar.fill.solid(); bar.fill.fore_color.rgb = accent; bar.line.fill.background()

    task_items = [f"  {t}" for t in tasks]
    add_bullet_text(slide, x + Inches(0.3), Inches(3.5), Inches(5.2), Inches(3.0),
                    task_items, font_size=15, color=LIGHT_GRAY)

# ========== Slide 7: 项目总结 ==========
slide = prs.slides.add_slide(prs.slide_layouts[6])
set_slide_bg(slide, DARK_BG)

add_text_box(slide, Inches(0.8), Inches(0.4), Inches(5), Inches(0.8),
             "项目总结", font_size=36, color=ACCENT_BLUE, bold=True)

shape = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0.8), Inches(1.2), Inches(2), Pt(3))
shape.fill.solid(); shape.fill.fore_color.rgb = ACCENT_CYAN; shape.line.fill.background()

# Tech highlights
add_shape(slide, Inches(0.8), Inches(1.8), Inches(5.8), Inches(5.0), CARD_BG, ACCENT_BLUE)
add_text_box(slide, Inches(1.1), Inches(1.9), Inches(5.2), Inches(0.5),
             "技术亮点", font_size=22, color=ACCENT_BLUE, bold=True)
highlights = [
    "  基于 LangGraph4j 的有向图工作流编排，实现复杂的多步骤 AI 生成流程",
    "  并发扇出/扇入模式，实现图像资源的并行采集，提升生成效率",
    "  工具调用模式（Tool Calling），让 AI 直接操作文件系统生成真实项目",
    "  多模型协作：DeepSeek 生成代码、Qwen-Turbo 智能路由、DashScope 生图",
    "  SSE 流式响应 + Reactor Flux，为用户提供实时的代码生成体验",
    "  质量检查闭环：自动质检失败回退重试，确保生成代码质量",
    "  防护栏机制：Prompt 注入检测 + 敏感词过滤，保障系统安全",
]
add_bullet_text(slide, Inches(1.0), Inches(2.5), Inches(5.4), Inches(4.0),
                highlights, font_size=13, color=LIGHT_GRAY)

# Future & summary
add_shape(slide, Inches(6.9), Inches(1.8), Inches(5.6), Inches(2.3), CARD_BG, GREEN)
add_text_box(slide, Inches(7.2), Inches(1.9), Inches(5.0), Inches(0.5),
             "项目成果", font_size=22, color=GREEN, bold=True)
results = [
    "  完整实现四大核心功能：智能生成、可视化编辑、一键部署、企业管理",
    "  前后端分离架构，涵盖 40+ 核心类，支持三种代码生成策略",
    "  集成监控体系（Prometheus + Grafana），支持 AI 调用指标追踪",
]
add_bullet_text(slide, Inches(7.1), Inches(2.5), Inches(5.2), Inches(1.5),
                results, font_size=13, color=LIGHT_GRAY)

add_shape(slide, Inches(6.9), Inches(4.4), Inches(5.6), Inches(2.4), CARD_BG, ORANGE)
add_text_box(slide, Inches(7.2), Inches(4.5), Inches(5.0), Inches(0.5),
             "未来展望", font_size=22, color=ORANGE, bold=True)
futures = [
    "  支持更多前端框架（React / Next.js / Nuxt.js）",
    "  引入更多 AI 模型，支持用户自选模型",
    "  增强可视化编辑能力，支持拖拽式布局",
    "  增加团队协作功能和应用市场生态",
]
add_bullet_text(slide, Inches(7.1), Inches(5.1), Inches(5.2), Inches(1.5),
                futures, font_size=13, color=LIGHT_GRAY)

# ========== Slide 8: Thank You ==========
slide = prs.slides.add_slide(prs.slide_layouts[6])
set_slide_bg(slide, DARK_BG)

add_text_box(slide, Inches(1), Inches(2.5), Inches(11), Inches(1.2),
             "感谢聆听", font_size=48, color=WHITE, bold=True, alignment=PP_ALIGN.CENTER)
add_text_box(slide, Inches(1), Inches(4.0), Inches(11), Inches(0.8),
             "Thank You", font_size=28, color=ACCENT_CYAN, alignment=PP_ALIGN.CENTER)

shape = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(4.5), Inches(5.2), Inches(4.3), Pt(3))
shape.fill.solid(); shape.fill.fore_color.rgb = ACCENT_BLUE; shape.line.fill.background()

add_text_box(slide, Inches(1), Inches(5.6), Inches(11), Inches(0.5),
             "AI 文档→原型生成器 — 粘贴 PRD，分钟级生成可交互原型", font_size=16, color=LIGHT_GRAY, alignment=PP_ALIGN.CENTER)

# Save
output_path = r"C:\Users\许敬彬\Desktop\AI原型生成器_项目汇报.pptx"
prs.save(output_path)
print(f"PPT saved to: {output_path}")
