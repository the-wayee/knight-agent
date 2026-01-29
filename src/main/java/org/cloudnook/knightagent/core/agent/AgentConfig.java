package org.cloudnook.knightagent.core.agent;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Agent 配置
 * <p>
 * 封装 Agent 的所有配置参数。
 * 使用 Builder 模式构建，支持链式调用。
 *
 * @author KnightAgent
 * @since 1.0.0
 */
@Data
public class AgentConfig {

    /**
     * Agent 名称
     */
    private String name;

    /**
     * Agent 描述
     */
    private String description;

    /**
     * 系统提示词
     * <p>
     * 设置 Agent 的行为和角色。
     */
    private String systemPrompt;

    /**
     * 最大的迭代次数
     * <p>
     * 防止 Agent 进入无限循环。
     * 默认值：10
     */
    private int maxIterations = 10;

    /**
     * 是否启用流式输出
     * <p>
     * 默认值：true
     */
    private boolean streamEnabled = true;

    /**
     * 是否启用检查点
     * <p>
     * 如果启用，会在每次迭代后保存状态。
     * 默认值：true
     */
    private boolean checkpointEnabled = true;

    /**
     * 超时时间（秒）
     * <p>
     * 单次执行的最大等待时间。
     * 默认值：120 秒
     */
    private int timeoutSeconds = 120;

    /**
     * 调用选项
     * <p>
     * 传递给 LLM 的调用选项。
     */
    private org.cloudnook.knightagent.core.model.ChatOptions chatOptions;

    /**
     * 中间件列表
     * <p>
     * 按顺序执行的中间件链。
     */
    private List<org.cloudnook.knightagent.core.middleware.Middleware> middlewares;

    /**
     * 状态归约器
     * <p>
     * 用于控制状态更新的规则。
     */
    private org.cloudnook.knightagent.core.state.StateReducer stateReducer;

    /**
     * Thread ID
     * <p>
     * 指定对话会话的 ID，用于加载历史状态。
     * 如果为 null，则每次都是新对话。
     */
    private String threadId;

    /**
     * 附加配置
     * <p>
     * 其他自定义配置参数。
     */
    private Map<String, Object> additionalConfig;

    /**
     * 默认构造函数
     */
    public AgentConfig() {
    }

    /**
     * 获取默认配置
     *
     * @return 默认的 AgentConfig
     */
    public static AgentConfig defaults() {
        return new AgentConfig();
    }

    /**
     * 创建 Builder
     *
     * @return Builder 实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 获取附加配置值
     *
     * @param key 配置键
     * @return 配置值的 Optional 包装
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getAdditionalConfig(String key) {
        if (additionalConfig == null) {
            return Optional.empty();
        }
        Object value = additionalConfig.get(key);
        return Optional.ofNullable((T) value);
    }

    /**
     * 获取附加配置值或默认值
     *
     * @param key          配置键
     * @param defaultValue 默认值
     * @return 配置值，如果不存在返回默认值
     */
    @SuppressWarnings("unchecked")
    public <T> T getAdditionalConfigOrDefault(String key, T defaultValue) {
        Object value = additionalConfig != null ? additionalConfig.get(key) : null;
        return value != null ? (T) value : defaultValue;
    }

    /**
     * 设置附加配置
     *
     * @param key   键
     * @param value 值
     * @return 新的配置实例
     */
    public AgentConfig withAdditionalConfig(String key, Object value) {
        Map<String, Object> newConfig = additionalConfig != null
                ? new HashMap<>(additionalConfig)
                : new HashMap<>();
        newConfig.put(key, value);
        AgentConfig copy = copy();
        copy.additionalConfig = newConfig;
        return copy;
    }

    /**
     * 复制配置
     */
    public AgentConfig copy() {
        AgentConfig copy = new AgentConfig();
        copy.name = this.name;
        copy.description = this.description;
        copy.systemPrompt = this.systemPrompt;
        copy.maxIterations = this.maxIterations;
        copy.streamEnabled = this.streamEnabled;
        copy.checkpointEnabled = this.checkpointEnabled;
        copy.timeoutSeconds = this.timeoutSeconds;
        copy.threadId = this.threadId;
        copy.chatOptions = this.chatOptions;
        copy.middlewares = this.middlewares != null ? new ArrayList<>(this.middlewares) : null;
        copy.stateReducer = this.stateReducer;
        copy.additionalConfig = this.additionalConfig != null ? new HashMap<>(this.additionalConfig) : null;
        return copy;
    }

    /**
     * 验证配置
     *
     * @throws IllegalArgumentException 如果配置不合法
     */
    public void validate() {
        if (maxIterations < 1) {
            throw new IllegalArgumentException("maxIterations 必须大于 0");
        }
        if (maxIterations > 100) {
            throw new IllegalArgumentException("maxIterations 不能超过 100");
        }
        if (timeoutSeconds < 1) {
            throw new IllegalArgumentException("timeoutSeconds 必须大于 0");
        }
        if (chatOptions != null) {
            chatOptions.validate();
        }
    }

    /**
     * Builder 类
     */
    public static class Builder {
        private final AgentConfig config = new AgentConfig();

        public Builder name(String name) {
            config.name = name;
            return this;
        }

        public Builder description(String description) {
            config.description = description;
            return this;
        }

        public Builder systemPrompt(String systemPrompt) {
            config.systemPrompt = systemPrompt;
            return this;
        }

        public Builder maxIterations(int maxIterations) {
            config.maxIterations = maxIterations;
            return this;
        }

        public Builder streamEnabled(boolean streamEnabled) {
            config.streamEnabled = streamEnabled;
            return this;
        }

        public Builder checkpointEnabled(boolean checkpointEnabled) {
            config.checkpointEnabled = checkpointEnabled;
            return this;
        }

        public Builder timeoutSeconds(int timeoutSeconds) {
            config.timeoutSeconds = timeoutSeconds;
            return this;
        }

        public Builder chatOptions(org.cloudnook.knightagent.core.model.ChatOptions chatOptions) {
            config.chatOptions = chatOptions;
            return this;
        }

        public Builder middlewares(List<org.cloudnook.knightagent.core.middleware.Middleware> middlewares) {
            config.middlewares = middlewares;
            return this;
        }

        public Builder stateReducer(org.cloudnook.knightagent.core.state.StateReducer stateReducer) {
            config.stateReducer = stateReducer;
            return this;
        }

        public Builder threadId(String threadId) {
            config.threadId = threadId;
            return this;
        }

        public Builder additionalConfig(Map<String, Object> additionalConfig) {
            config.additionalConfig = additionalConfig;
            return this;
        }

        public AgentConfig build() {
            return config;
        }
    }
}
