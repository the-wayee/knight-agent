package org.cloudnook.knightagent.core.agent;

import lombok.Builder;
import lombok.Data;
import org.cloudnook.knightagent.core.state.AgentState;

import java.util.Map;
import java.util.Optional;

/**
 * Agent 请求
 * <p>
 * 封装发送给 Agent 的所有输入信息。
 *
 * @author KnightAgent
 * @since 1.0.0
 */
@Data
@Builder(toBuilder = true)
public class AgentRequest {

    /**
     * 用户输入内容
     * <p>
     * 用户发送给 Agent 的文本消息。
     */
    private final String input;

    /**
     * 初始状态（用于恢复执行）
     * <p>
     * 如果提供此字段，Strategy 将使用此状态而不是从 checkpoint 加载。
     * 主要用于审批恢复等场景。
     */
    private AgentState state;

    /**
     * Thread ID
     * <p>
     * 指定对话会话的 ID，用于加载历史状态。
     * 如果为 null，则开始新的对话。
     */
    private String threadId;

    /**
     * 用户 ID
     * <p>
     * 发送请求的用户标识，用于多用户场景。
     */
    private String userId;

    /**
     * 会话 ID
     * <p>
     * 当前会话的唯一标识，可用于追踪单次调用。
     */
    private String sessionId;

    /**
     * 附加参数
     * <p>
     * 额外的配置参数，可以传递给中间件或执行器。
     */
    private Map<String, Object> parameters;

    /**
     * 系统提示词（可选）
     * <p>
     * 覆盖 Agent 默认的系统提示词。
     */
    private String systemPrompt;

    /**
     * 是否启用流式输出
     * <p>
     * 覆盖 Agent 的默认配置。
     */
    private Boolean streamEnabled;

    /**
     * 最大迭代次数
     * <p>
     * 限制 Agent 的执行轮次，防止无限循环。
     */
    private Integer maxIterations;

    /**
     * 创建请求（仅输入）
     *
     * @param input 用户输入
     * @return Agent 请求
     */
    public static AgentRequest of(String input) {
        return AgentRequest.builder()
                .input(input)
                .build();
    }

    /**
     * 创建请求（输入 + Thread ID）
     *
     * @param input    用户输入
     * @param threadId Thread ID
     * @return Agent 请求
     */
    public static AgentRequest of(String input, String threadId) {
        return AgentRequest.builder()
                .input(input)
                .threadId(threadId)
                .build();
    }

    /**
     * 获取参数值
     *
     * @param key 参数键
     * @return 参数值的 Optional 包装
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getParameter(String key) {
        if (parameters == null) {
            return Optional.empty();
        }
        Object value = parameters.get(key);
        return Optional.ofNullable((T) value);
    }

    /**
     * 获取参数值或默认值
     *
     * @param key          参数键
     * @param defaultValue 默认值
     * @return 参数值，如果不存在返回默认值
     */
    @SuppressWarnings("unchecked")
    public <T> T getParameterOrDefault(String key, T defaultValue) {
        Object value = parameters != null ? parameters.get(key) : null;
        return value != null ? (T) value : defaultValue;
    }

    /**
     * 检查是否有参数
     *
     * @param key 参数键
     * @return 如果存在返回 true
     */
    public boolean hasParameter(String key) {
        return parameters != null && parameters.containsKey(key);
    }

    /**
     * 生成会话 ID（如果不存在）
     *
     * @return 会话 ID
     */
    public String getSessionId() {
        if (sessionId == null) {
            sessionId = java.util.UUID.randomUUID().toString();
        }
        return sessionId;
    }
}
