package com.bing.bingaicode.rag.node;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.bing.bingaicode.langgraph4j.state.WorkflowContext;
import com.bing.bingaicode.rag.ai.PRDParserService;
import com.bing.bingaicode.rag.model.PRDParserResult;
import com.bing.bingaicode.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * Step 1: PRD 解析节点（Planner Agent）
 *
 * 职责：将用户的 PRD 文本解析为扁平化的 JSON，
 * 提取页面列表、组件需求、表单字段和 RAG 检索关键词。
 *
 * 使用手动 JSON 解析（而非 LangChain4j 自动反序列化）提高容错性。
 */
@Slf4j
public class PRDParserNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: PRD 解析 (Planner Agent)");
            String originalPrompt = context.getOriginalPrompt();
            log.info("开始解析 PRD，长度: {} 字符", originalPrompt.length());
            try {
                // 调用 AI 服务，获取原始 JSON 字符串
                PRDParserService prdParserService = SpringContextUtil.getBean(PRDParserService.class);
                String rawJson = prdParserService.parsePRD(originalPrompt);
                // 清理 LLM 输出中可能的 markdown 包裹
                rawJson = cleanLLMJsonOutput(rawJson);
                log.info("PRD 解析原始输出长度: {} 字符", rawJson.length());
                // 手动解析 JSON，容错处理
                PRDParserResult result = parseJsonTolerant(rawJson);
                // 提取 RAG 关键词
                String ragKeywords = "";
                if (result.getRagKeywords() != null && !result.getRagKeywords().isEmpty()) {
                    ragKeywords = String.join(" ", result.getRagKeywords());
                }
                log.info("PRD 解析完成 - 项目: {}, 页面数: {}, RAG关键词: {}",
                        result.getProjectName(),
                        result.getPages() != null ? result.getPages().size() : 0,
                        ragKeywords);
                // 更新上下文
                context.setCurrentStep("PRD 解析");
                context.setPrdParserResult(result);
                context.setPrdParserResultJson(rawJson);
                context.setRagQueryKeywords(ragKeywords);
            } catch (Exception e) {
                log.error("PRD 解析异常，降级使用原始 prompt: {}", e.getMessage());
                context.setCurrentStep("PRD 解析(降级)");
                context.setRagQueryKeywords(originalPrompt.length() > 200
                        ? originalPrompt.substring(0, 200) : originalPrompt);
            }
            return WorkflowContext.saveContext(context);
        });
    }

    /**
     * 清理 LLM 输出中的 markdown 代码块包裹
     */
    private static String cleanLLMJsonOutput(String raw) {
        if (raw == null) return "";
        String trimmed = raw.trim();
        // 移除 ```json ... ``` 包裹
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        }
        return trimmed.trim();
    }

    /**
     * 手动解析 JSON 为 PRDParserResult，容错处理缺失字段
     */
    private static PRDParserResult parseJsonTolerant(String json) {
        JSONObject root = JSONUtil.parseObj(json);
        PRDParserResult result = PRDParserResult.builder()
                .projectName(root.getStr("projectName", ""))
                .description(root.getStr("description", ""))
                .businessDomain(root.getStr("businessDomain", ""))
                .complexityLevel(root.getStr("complexityLevel", "medium"))
                .ragKeywords(parseStringList(root.getJSONArray("ragKeywords")))
                .pages(parsePages(root.getJSONArray("pages")))
                .build();
        return result;
    }

    private static List<PRDParserResult.PageSpec> parsePages(JSONArray pagesArray) {
        if (pagesArray == null || pagesArray.isEmpty()) {
            return Collections.emptyList();
        }
        List<PRDParserResult.PageSpec> pages = new ArrayList<>();
        for (int i = 0; i < pagesArray.size(); i++) {
            JSONObject pageObj = pagesArray.getJSONObject(i);
            if (pageObj == null) continue;
            pages.add(PRDParserResult.PageSpec.builder()
                    .name(pageObj.getStr("name", "页面" + (i + 1)))
                    .route(pageObj.getStr("route", ""))
                    .description(pageObj.getStr("description", ""))
                    .components(pageObj.getStr("components", ""))
                    .formFields(pageObj.getStr("formFields", ""))
                    .tableColumns(pageObj.getStr("tableColumns", ""))
                    .apiEndpoints(pageObj.getStr("apiEndpoints", ""))
                    .interactions(pageObj.getStr("interactions", ""))
                    .build());
        }
        return pages;
    }

    private static List<String> parseStringList(JSONArray array) {
        if (array == null) return Collections.emptyList();
        List<String> list = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            String val = array.getStr(i);
            if (StrUtil.isNotBlank(val)) list.add(val);
        }
        return list;
    }
}
