package com.bing.bingaicode.rag.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.bing.bingaicode.annotation.AuthCheck;
import com.bing.bingaicode.common.BaseResponse;
import com.bing.bingaicode.common.ResultUtils;
import com.bing.bingaicode.constant.UserConstant;
import com.bing.bingaicode.exception.BusinessException;
import com.bing.bingaicode.exception.ErrorCode;
import com.bing.bingaicode.model.entity.User;
import com.bing.bingaicode.rag.dto.KnowledgeBaseDocSaveRequest;
import com.bing.bingaicode.rag.dto.RAGSearchRequest;
import com.bing.bingaicode.rag.dto.RAGSearchResponse;
import com.bing.bingaicode.rag.model.KnowledgeBaseChunk;
import com.bing.bingaicode.rag.model.KnowledgeBaseDoc;
import com.bing.bingaicode.rag.service.KnowledgeBaseService;
import com.bing.bingaicode.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识库管理 Controller
 */
@RestController
@RequestMapping("/knowledge-base")
@Slf4j
@Tag(name = "知识库管理", description = "OpenSpec 知识库的 CRUD 和 RAG 检索接口")
public class KnowledgeBaseController {

    @Resource
    private KnowledgeBaseService knowledgeBaseService;

    @Resource
    private UserService userService;

    /**
     * 保存知识库文档（新增或更新）
     */
    @PostMapping("/doc/save")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @Operation(summary = "保存知识库文档")
    public BaseResponse<KnowledgeBaseDoc> saveDoc(
            @RequestBody KnowledgeBaseDocSaveRequest request,
            HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 参数校验
        if (StrUtil.isBlank(request.getDocName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文档名称不能为空");
        }
        if (StrUtil.isBlank(request.getContent())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文档内容不能为空");
        }
        if (StrUtil.isBlank(request.getDocId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文档标识不能为空");
        }
        // 构建实体
        KnowledgeBaseDoc doc = KnowledgeBaseDoc.builder()
                .id(request.getId())
                .docName(request.getDocName())
                .docId(request.getDocId())
                .category(StrUtil.isNotBlank(request.getCategory()) ? request.getCategory() : "custom")
                .description(request.getDescription())
                .tags(StrUtil.isNotBlank(request.getTags()) ?
                        JSONUtil.toJsonStr(request.getTags().split(",")) : "[]")
                .frameworks(StrUtil.isNotBlank(request.getFrameworks()) ?
                        JSONUtil.toJsonStr(request.getFrameworks().split(",")) : "[]")
                .content(request.getContent())
                .version(StrUtil.isNotBlank(request.getVersion()) ? request.getVersion() : "1.0.0")
                .status(1)
                .build();
        KnowledgeBaseDoc saved = knowledgeBaseService.saveDoc(doc, loginUser.getId());
        return ResultUtils.success(saved);
    }

    /**
     * 获取文档详情
     */
    @GetMapping("/doc/get")
    @Operation(summary = "获取知识库文档详情")
    public BaseResponse<KnowledgeBaseDoc> getDoc(@RequestParam Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无效的文档ID");
        }
        KnowledgeBaseDoc doc = knowledgeBaseService.getDocById(id);
        if (doc == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文档不存在");
        }
        return ResultUtils.success(doc);
    }

    /**
     * 查询文档列表
     */
    @GetMapping("/doc/list")
    @Operation(summary = "查询知识库文档列表")
    public BaseResponse<List<KnowledgeBaseDoc>> listDocs(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        List<KnowledgeBaseDoc> docs = knowledgeBaseService.listDocs(category, keyword, status);
        return ResultUtils.success(docs);
    }

    /**
     * 删除文档
     */
    @PostMapping("/doc/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @Operation(summary = "删除知识库文档")
    public BaseResponse<Boolean> deleteDoc(@RequestParam Long id, HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        boolean result = knowledgeBaseService.deleteDoc(id, loginUser.getId());
        return ResultUtils.success(result);
    }

    /**
     * 启用/禁用文档
     */
    @PostMapping("/doc/toggle")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @Operation(summary = "启用/禁用知识库文档")
    public BaseResponse<Boolean> toggleDoc(@RequestParam Long id, @RequestParam Integer status) {
        boolean result = knowledgeBaseService.toggleDocStatus(id, status);
        return ResultUtils.success(result);
    }

    /**
     * RAG 检索（测试接口，生产环境由工作流内部调用）
     */
    @PostMapping("/search")
    @Operation(summary = "RAG 知识库检索")
    public BaseResponse<RAGSearchResponse> search(@RequestBody RAGSearchRequest request) {
        if (StrUtil.isBlank(request.getQuery())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "检索查询不能为空");
        }
        int topK = request.getTopK() != null && request.getTopK() > 0 ? request.getTopK() : 5;
        List<KnowledgeBaseChunk> chunks = knowledgeBaseService.search(
                request.getQuery(), request.getCategory(), request.getFramework(), topK);
        String context = knowledgeBaseService.assembleChunksToContext(chunks);
        return ResultUtils.success(RAGSearchResponse.of(request.getQuery(), chunks, context));
    }
}
