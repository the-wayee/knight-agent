package org.cloudnook.knightagent.core.model;

import lombok.Builder;
import lombok.Data;

/**
 * 模型能力描述
 * <p>
     * 描述 LLM 支持的功能特性和限制。
     * 用于 Agent 框架判断模型是否支持某些特性。
 *
 * @author KnightAgent
 * @since 1.0.0
 */
@Data
@Builder
public class ModelCapabilities {

    /**
     * 最大上下文窗口（Token 数）
     * <p>
     * 模型能处理的最大输入+输出 Token 总数。
     * <p>
     * 常见值：
     * <ul>
     *   <li>GPT-3.5: 4096 / 16385</li>
     *   <li>GPT-4: 8192 / 32768 / 128000</li>
     *   <li>Claude 3: 200000</li>
     * </ul>
     */
    private final int maxContextTokens;

    /**
     * 最大输出 Token 数
     * <p>
     * 单次响应的最大 Token 数。
     */
    private final int maxOutputTokens;

    /**
     * 是否支持流式输出
     */
    @Builder.Default
    private final boolean supportsStreaming = true;

    /**
     * 是否支持工具调用
     * <p>
     * 也称为函数调用 (Function Calling)
     */
    @Builder.Default
    private final boolean supportsToolCalling = true;

    /**
     * 是否支持并行工具调用
     * <p>
     * 是否可以在一次响应中调用多个工具。
     */
    @Builder.Default
    private final boolean supportsParallelToolCalling = false;

    /**
     * 是否支持系统提示词
     */
    @Builder.Default
    private final boolean supportsSystemPrompt = true;

    /**
     * 是否支持多模态（图像、音频等）
     */
    @Builder.Default
    private final boolean supportsMultimodal = false;

    /**
     * 是否支持思考过程输出
     * <p>
     * 如 Claude 的 extended thinking 模式。
     */
    @Builder.Default
    private final boolean supportsReasoning = false;

    /**
     * 模型家族
     * <p>
     * 如 "gpt", "claude", "gemini", "llama" 等
     */
    private final String family;

    /**
     * 创建 GPT-3.5 能力描述
     */
    public static ModelCapabilities gpt35() {
        return ModelCapabilities.builder()
                .maxContextTokens(16385)
                .maxOutputTokens(4096)
                .supportsStreaming(true)
                .supportsToolCalling(true)
                .supportsParallelToolCalling(true)
                .supportsSystemPrompt(true)
                .family("gpt")
                .build();
    }

    /**
     * 创建 GPT-4 能力描述
     */
    public static ModelCapabilities gpt4() {
        return ModelCapabilities.builder()
                .maxContextTokens(128000)
                .maxOutputTokens(8192)
                .supportsStreaming(true)
                .supportsToolCalling(true)
                .supportsParallelToolCalling(true)
                .supportsSystemPrompt(true)
                .supportsMultimodal(true)
                .family("gpt")
                .build();
    }

    /**
     * 创建 Claude 3 Opus 能力描述
     */
    public static ModelCapabilities claude3Opus() {
        return ModelCapabilities.builder()
                .maxContextTokens(200000)
                .maxOutputTokens(4096)
                .supportsStreaming(true)
                .supportsToolCalling(true)
                .supportsParallelToolCalling(false)
                .supportsSystemPrompt(true)
                .supportsMultimodal(true)
                .supportsReasoning(true)
                .family("claude")
                .build();
    }

    /**
     * 创建 Claude 3.5 Sonnet 能力描述
     */
    public static ModelCapabilities claude35Sonnet() {
        return ModelCapabilities.builder()
                .maxContextTokens(200000)
                .maxOutputTokens(8192)
                .supportsStreaming(true)
                .supportsToolCalling(true)
                .supportsParallelToolCalling(false)
                .supportsSystemPrompt(true)
                .supportsMultimodal(true)
                .supportsReasoning(true)
                .family("claude")
                .build();
    }

    /**
     * 创建通用模型能力描述
     * <p>
     * 使用保守的默认值，适用于未知模型。
     */
    public static ModelCapabilities generic() {
        return ModelCapabilities.builder()
                .maxContextTokens(4096)
                .maxOutputTokens(2048)
                .supportsStreaming(true)
                .supportsToolCalling(false)
                .supportsParallelToolCalling(false)
                .supportsSystemPrompt(true)
                .family("generic")
                .build();
    }

    /**
     * 检查是否为 GPT 系列模型
     */
    public boolean isGpt() {
        return "gpt".equals(family);
    }

    /**
     * 检查是否为 Claude 系列模型
     */
    public boolean isClaude() {
        return "claude".equals(family);
    }

    /**
     * 检查是否为 Gemini 系列模型
     */
    public boolean isGemini() {
        return "gemini".equals(family);
    }

    /**
     * 计算剩余可用 Token 数
     *
     * @param usedTokens 已使用的 Token 数
     * @return 剩余可用 Token 数
     */
    public int getRemainingTokens(int usedTokens) {
        return Math.max(0, maxContextTokens - usedTokens);
    }
}
