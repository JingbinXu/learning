package com.bing.bingaicode.rag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * PRD 解析结果（Step 1 的输出）
 *
 * 设计原则：从 4 层嵌套简化为 2 层，用扁平化字符串描述复杂结构。
 * LLM 生成简单字符串比生成深层嵌套 JSON 可靠 10 倍。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PRDParserResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 项目名称 */
    private String projectName;

    /** 项目描述 */
    private String description;

    /** 业务领域 */
    private String businessDomain;

    /** 复杂度: simple | medium | complex */
    private String complexityLevel;

    /** RAG 检索关键词 */
    private List<String> ragKeywords;

    /** 页面列表（扁平结构） */
    private List<PageSpec> pages;

    /**
     * 页面定义 - 扁平结构，所有嵌套信息用语义化字符串描述
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageSpec implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /** 页面名称 */
        private String name;

        /** 路由路径 */
        private String route;

        /** 页面描述 */
        private String description;

        /**
         * 组件需求（语义化字符串，每行一个组件）
         * 格式：「组件类型」组件名称 | 描述
         * 示例：
         *   table 用户列表表格 | 展示用户数据，支持分页和搜索
         *   form 用户编辑表单 | 包含姓名、邮箱、角色字段
         *   modal 新增用户弹窗 | 弹窗表单，包含必填校验
         */
        private String components;

        /**
         * 表单字段定义（语义化字符串）
         * 格式：字段名:标签:类型:必填:占位文字;...
         * 示例：name:姓名:text:true:请输入姓名;email:邮箱:email:true:请输入邮箱;role:角色:select:false:请选择角色
         */
        private String formFields;

        /**
         * 表格列定义（语义化字符串）
         * 格式：字段名:标题:类型:宽度;...
         * 示例：name:姓名:text:120px;email:邮箱:text:200px;status:状态:status:100px;createTime:创建时间:date:160px
         */
        private String tableColumns;

        /**
         * API 接口列表（语义化字符串，每行一个接口）
         * 格式：METHOD /path - 描述
         * 示例：
         *   GET /api/users - 获取用户列表
         *   POST /api/users - 创建用户
         *   PUT /api/users/{id} - 更新用户
         */
        private String apiEndpoints;

        /**
         * 交互逻辑（语义化字符串，每行一个交互）
         * 格式：触发元素 → 动作 → 目标
         * 示例：
         *   新增按钮 → 点击 → 打开新增用户弹窗
         *   编辑按钮 → 点击 → 打开编辑弹窗并回填数据
         */
        private String interactions;
    }
}
