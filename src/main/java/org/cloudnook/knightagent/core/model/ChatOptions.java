package org.cloudnook.knightagent.core.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 聊天选项
 * <p>
 * 封装调用 LLM 时的各种配置参数。
 * 所有选项都是可选的，未指定的选项将使用模型默认值。
 * <p>
 * 使用示例：
 * <pre>{@code
 * // 使用默认选项
 * ChatOptions options = ChatOptions.defaults();
 *
 * // 自定义选项
 * ChatOptions options = ChatOptions.builder()
 *     .temperature(0.7)
 *     .maxTokens(2000)
 *     .stopSequences(List.of("END", "STOP"))
 *     .build();
 * }</pre>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
@Data
@Builder(toBuilder = true)
public class ChatOptions {

    /**
     * 采样温度
     * <p>
     * 控制输出的随机性：
     * <ul>
     *   <li>0.0 - 完全确定性，总是选择最可能的下一个 Token</li>
     *   <li>1.0 - 标准随机性</li>
     *   <li>>1.0 - 更高的随机性和创造性</li>
     * </ul>
     * <p>
     * 建议值：
     * <ul>
     *   <li>创意写作：0.8 - 1.0</li>
     *   <li>代码生成：0.2 - 0.4</li>
     *   <li>事实问答：0.0 - 0.2</li>
     * </ul>
     * <p>
     * 默认值：0.7
     */
    @Builder.Default
    private Double temperature = 0.7;

    /**
     * 核采样参数
     * <p>
     * 与 temperature 类似，控制输出的随机性。
     * 只考虑累积概率达到 topP 的 Token。
     * <p>
     * 范围：0.0 - 1.0
     * <p>
     * 默认值：1.0（不限制）
     */
    @Builder.Default
    private Double topP = 1.0;

    /**
     * 最大生成 Token 数
     * <p>
     * 限制单次响应的最大长度。
     * <p>
     * 注意：
     * <ul>
     *   <li>设置为 null 表示使用模型默认值</li>
     *   <li>设置为 0 表示不限制（取决于模型的硬限制）</li>
     *   <li>实际生成的 Token 数可能少于这个值</li>
     * </ul>
     */
    private Integer maxTokens;

    /**
     * 停止序列
     * <p>
     * 当生成内容遇到这些字符串时停止生成。
     * <p>
     * 示例：
     * <pre>{@code
     * .stopSequences(List.of("\n\n", "END:", "###"))
     * }</pre>
     */
    private List<String> stopSequences;

    /**
     * Top-K 采样
     * <p>
     * 只考虑概率最高的 K 个 Token。
     * <p>
     * 范围：1 - ∞
     * <p>
     * 默认值：null（使用模型默认值）
     */
    private Integer topK;

    /**
     * 频率惩罚
     * <p>
     * 降低重复内容的出现频率。
     * <p>
     * 范围：-2.0 - 2.0
     * <ul>
     *   <li>正值：减少重复</li>
     *   <li>负值：增加重复</li>
     *   <li>0：不惩罚</li>
     * </ul>
     * <p>
     * 默认值：0
     */
    @Builder.Default
    private Double frequencyPenalty = 0.0;

    /**
     * 存在惩罚
     * <p>
     * 惩罚已经出现过的 Token。
     * <p>
     * 范围：-2.0 - 2.0
     * <p>
     * 默认值：0
     */
    @Builder.Default
    private Double presencePenalty = 0.0;

    /**
     * 系统提示词
     * <p>
     * 覆盖消息列表中的系统提示词。
     * 如果为 null，使用消息列表中的系统提示词。
     */
    private String systemPrompt;

    /**
     * 超时时间（秒）
     * <p>
     * 单次调用的最大等待时间。
     * <p>
     * 默认值：60 秒
     */
    @Builder.Default
    private Integer timeoutSeconds = 60;

    /**
     * 是否启用流式输出
     * <p>
     * 即使使用非流式方法，也可以强制启用流式并自动收集结果。
     * <p>
     * 默认值：true
     */
    @Builder.Default
    private boolean streamEnabled = true;

    /**
     * 可用工具列表
     * <p>
     * 传递给模型的可调用工具定义。
     */
    private List<org.cloudnook.knightagent.core.tool.McpTool> tools;

    /**
     * 获取默认选项
     *
     * @return 默认的 ChatOptions 实例
     */
    public static ChatOptions defaults() {
        return ChatOptions.builder().build();
    }

    /**
     * 创建低温度选项（更确定性）
     * <p>
     * 适用于需要精确输出的场景。
     *
     * @return ChatOptions 实例
     */
    public static ChatOptions deterministic() {
        return ChatOptions.builder()
                .temperature(0.1)
                .topP(0.9)
                .build();
    }

    /**
     * 创建高温度选项（更创造性）
     * <p>
     * 适用于创意写作、头脑风暴等场景。
     *
     * @return ChatOptions 实例
     */
    public static ChatOptions creative() {
        return ChatOptions.builder()
                .temperature(0.9)
                .topP(1.0)
                .build();
    }

    /**
     * 创建代码生成选项
     *
     * @return ChatOptions 实例
     */
    public static ChatOptions forCode() {
        return ChatOptions.builder()
                .temperature(0.2)
                .frequencyPenalty(0.3)  // 减少代码重复
                .build();
    }

    /**
     * 验证选项是否合法
     *
     * @throws IllegalArgumentException 如果选项值超出合法范围
     */
    public void validate() {
        if (temperature != null && (temperature < 0 || temperature > 2)) {
            throw new IllegalArgumentException("temperature 必须在 0-2 之间");
        }
        if (topP != null && (topP < 0 || topP > 1)) {
            throw new IllegalArgumentException("topP 必须在 0-1 之间");
        }
        if (maxTokens != null && maxTokens < 0) {
            throw new IllegalArgumentException("maxTokens 不能为负数");
        }
        if (frequencyPenalty != null && (frequencyPenalty < -2 || frequencyPenalty > 2)) {
            throw new IllegalArgumentException("frequencyPenalty 必须在 -2 到 2 之间");
        }
        if (presencePenalty != null && (presencePenalty < -2 || presencePenalty > 2)) {
            throw new IllegalArgumentException("presencePenalty 必须在 -2 到 2 之间");
        }
    }

    /**
     * 创建此选项的副本
     *
     * @return 副本实例
     */
    public ChatOptions copy() {
        return ChatOptions.builder()
                .temperature(temperature)
                .topP(topP)
                .maxTokens(maxTokens)
                .stopSequences(stopSequences != null ? List.copyOf(stopSequences) : null)
                .topK(topK)
                .frequencyPenalty(frequencyPenalty)
                .presencePenalty(presencePenalty)
                .systemPrompt(systemPrompt)
                .timeoutSeconds(timeoutSeconds)
                .streamEnabled(streamEnabled)
                .build();
    }
}
