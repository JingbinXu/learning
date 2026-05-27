import logging
import json
from langgraph.graph import StateGraph, END

from app.langgraph.state import WorkflowState, create_initial_context
from app.langgraph.nodes.router import router_node
from app.langgraph.nodes.prompt_enhancer import prompt_enhancer_node
from app.langgraph.nodes.image_collector import image_collector_node
from app.langgraph.nodes.code_generator import code_generator_node
from app.langgraph.nodes.quality_check import code_quality_check_node, should_build
from app.langgraph.nodes.project_builder import project_builder_node

logger = logging.getLogger(__name__)


def build_workflow_graph():
    """构建 LangGraph 工作流"""
    graph = StateGraph(WorkflowState)

    # 添加节点
    graph.add_node("image_collector", image_collector_node)
    graph.add_node("prompt_enhancer", prompt_enhancer_node)
    graph.add_node("router", router_node)
    graph.add_node("code_generator", code_generator_node)
    graph.add_node("code_quality_check", code_quality_check_node)
    graph.add_node("project_builder", project_builder_node)

    # 定义边
    graph.set_entry_point("image_collector")
    graph.add_edge("image_collector", "prompt_enhancer")
    graph.add_edge("prompt_enhancer", "router")
    graph.add_edge("router", "code_generator")
    graph.add_edge("code_generator", "code_quality_check")

    # 条件边：质检结果路由
    graph.add_conditional_edges(
        "code_quality_check",
        should_build,
        {
            "build": "project_builder",
            "skip_build": END,
            "fail": "code_generator",  # 质检失败，回到代码生成
            "error": END,
        },
    )

    graph.add_edge("project_builder", END)

    return graph.compile()


async def execute_workflow(prompt: str) -> dict:
    """执行工作流（同步等待结果）"""
    graph = build_workflow_graph()
    initial_context = create_initial_context(prompt)

    initial_state: WorkflowState = {
        "messages": [],
        "workflow_context": initial_context,
    }

    result = await graph.ainvoke(initial_state)
    return result["workflow_context"]


async def execute_workflow_with_sse(prompt: str):
    """执行工作流，SSE 流式输出"""
    graph = build_workflow_graph()
    initial_context = create_initial_context(prompt)

    initial_state: WorkflowState = {
        "messages": [],
        "workflow_context": initial_context,
    }

    step_number = 0

    # 发送开始事件
    yield _sse_event("workflow_start", {"prompt": prompt})

    try:
        async for event in graph.astream(initial_state):
            for node_name, node_state in event.items():
                step_number += 1
                context = node_state.get("workflow_context", {})
                current_step = context.get("current_step", node_name)

                yield _sse_event("step_completed", {
                    "stepNumber": step_number,
                    "currentStep": current_step,
                    "nodeName": node_name,
                })

                logger.info(f"Workflow step {step_number}: {node_name} ({current_step})")

        # 发送完成事件
        yield _sse_event("workflow_completed", {
            "stepNumber": step_number,
            "generatedCodeDir": initial_context.get("generated_code_dir", ""),
        })

    except Exception as e:
        logger.error(f"Workflow error: {e}")
        yield _sse_event("workflow_error", {"error": str(e)})


def _sse_event(event_type: str, data: dict) -> str:
    """格式化 SSE 事件"""
    return f"event:{event_type}\ndata:{json.dumps(data, ensure_ascii=False)}\n\n"
