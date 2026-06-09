package com.bing.bingaicode.rag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 代码审查结果（Step 4 的输出）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeReviewResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 是否通过审查 */
    private Boolean isValid;

    /** 得分（0-100） */
    private Integer score;

    /** 错误列表 */
    private List<ReviewError> errors;

    /** 合规检查结果 */
    private List<ComplianceCheck> complianceChecks;

    /** 改进建议 */
    private List<String> suggestions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewError implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String ruleId;
        private String severity;
        private String file;
        private String line;
        private String description;
        private String fixSuggestion;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComplianceCheck implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String checkName;
        private Boolean passed;
        private String detail;
    }
}
