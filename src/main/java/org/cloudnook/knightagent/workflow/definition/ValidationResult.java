package org.cloudnook.knightagent.workflow.definition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 验证结果
 */
@Data
@Builder
public class ValidationResult {

    /**
     * 是否验证通过
     */
    private boolean valid;

    /**
     * 错误信息列表
     */
    @Builder.Default
    private List<String> errors = new ArrayList<>();

    /**
     * 警告信息列表
     */
    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    /**
     * 创建成功结果
     */
    public static ValidationResult success() {
        return ValidationResult.builder()
                .valid(true)
                .build();
    }

    /**
     * 创建失败结果
     */
    public static ValidationResult failure(String error) {
        return ValidationResult.builder()
                .valid(false)
                .errors(List.of(error))
                .build();
    }

    /**
     * 创建失败结果（多个错误）
     */
    public static ValidationResult failure(List<String> errors) {
        return ValidationResult.builder()
                .valid(false)
                .errors(errors)
                .build();
    }

    /**
     * 添加错误
     */
    public void addError(String error) {
        this.errors.add(error);
        this.valid = false;
    }

    /**
     * 添加警告
     */
    public void addWarning(String warning) {
        this.warnings.add(warning);
    }
}
