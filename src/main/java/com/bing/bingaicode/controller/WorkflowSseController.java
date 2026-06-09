package com.bing.bingaicode.controller;

import com.bing.bingaicode.langgraph4j.CodeGenWorkflow;
import com.bing.bingaicode.langgraph4j.RAGCodeGenWorkflow;
import com.bing.bingaicode.langgraph4j.state.WorkflowContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

/**
 * 工作流 SSE 控制器
 */
@RestController
@RequestMapping("/workflow")
@Slf4j
public class WorkflowSseController {

    @Resource
    private CodeGenWorkflow codeGenWorkflow;

    @Resource
    private RAGCodeGenWorkflow ragCodeGenWorkflow;

    /**
     * 同步执行工作流
     */
    @PostMapping("/execute")
    public WorkflowContext executeWorkflow(@RequestParam String prompt) {
        log.info("收到同步工作流执行请求: {}", prompt);
        return codeGenWorkflow.executeWorkflow(prompt);
    }

    /**
     * Flux 流式执行工作流
     */
    @GetMapping(value = "/execute-flux", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> executeWorkflowWithFlux(@RequestParam String prompt) {
        log.info("收到 Flux 工作流执行请求: {}", prompt);
        return codeGenWorkflow.executeWorkflowWithFlux(prompt);
    }

    /**
     * SSE 流式执行工作流
     */
    @GetMapping(value = "/execute-sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter executeWorkflowWithSse(@RequestParam String prompt) {
        log.info("收到 SSE 工作流执行请求: {}", prompt);
        return codeGenWorkflow.executeWorkflowWithSse(prompt);
    }

    // ========== RAG 增强工作流 ==========

    @PostMapping("/rag/execute")
    public WorkflowContext executeRAGWorkflow(@RequestParam String prompt) {
        log.info("收到 RAG 工作流同步执行请求: {}", prompt);
        return ragCodeGenWorkflow.executeWorkflow(prompt);
    }

    @GetMapping(value = "/rag/execute-flux", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> executeRAGWorkflowWithFlux(@RequestParam String prompt) {
        log.info("收到 RAG 工作流 Flux 执行请求: {}", prompt);
        return ragCodeGenWorkflow.executeWorkflowWithFlux(prompt);
    }

    @GetMapping(value = "/rag/execute-sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter executeRAGWorkflowWithSse(@RequestParam String prompt) {
        log.info("收到 RAG 工作流 SSE 执行请求: {}", prompt);
        return ragCodeGenWorkflow.executeWorkflowWithSse(prompt);
    }
}
