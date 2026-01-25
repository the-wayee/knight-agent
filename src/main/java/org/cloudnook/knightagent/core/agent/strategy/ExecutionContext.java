package org.cloudnook.knightagent.core.agent.strategy;

import lombok.Builder;
import lombok.Getter;
import org.cloudnook.knightagent.core.agent.AgentConfig;
import org.cloudnook.knightagent.core.checkpoint.Checkpointer;
import org.cloudnook.knightagent.core.middleware.MiddlewareChain;
import org.cloudnook.knightagent.core.model.ChatModel;
import org.cloudnook.knightagent.core.tool.ToolInvoker;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Agent 执行上下文
 * <p>
 * 封装执行策略所需的所有依赖和配置。
 * 上下文对象在执行期间保持不变，确保线程安全。
 *
 * @author KnightAgent
 * @since 1.0.0
 */
@Getter
@Builder
public class ExecutionContext {

    /**
     * LLM 模型
     */
    private final ChatModel model;

    /**
     * 工具执行器
     */
    private final ToolInvoker toolInvoker;

    /**
     * 检查点存储
     */
    private final Checkpointer checkpointer;

    /**
     * Agent 配置
     */
    private final AgentConfig config;

    /**
     * 中间件链
     */
    private final MiddlewareChain middlewareChain;

    /**
     * 附加属性
     * <p>
     * 用于存储执行过程中的动态数据。
     */
    @Builder.Default
    private final Map<String, Object> attributes = new HashMap<>();

    /**
     * 获取附加属性值
     *
     * @param key 属性键
     * @return 属性值的 Optional 包装
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getAttribute(String key) {
        if (attributes == null) {
            return Optional.empty();
        }
        Object value = attributes.get(key);
        return Optional.ofNullable((T) value);
    }

    /**
     * 创建默认上下文
     * <p>
     * 使用提供的必填依赖，其他使用默认值。
     *
     * @param model    LLM 模型
     * @param toolInvoker 工具执行器
     * @return 执行上下文
     */
    public static ExecutionContext of(ChatModel model, ToolInvoker toolInvoker) {
        return ExecutionContext.builder()
                .model(model)
                .toolInvoker(toolInvoker)
                .checkpointer(null)
                .config(org.cloudnook.knightagent.core.agent.AgentConfig.defaults())
                .middlewareChain(new MiddlewareChain(java.util.List.of()))
                .build();
    }
}
