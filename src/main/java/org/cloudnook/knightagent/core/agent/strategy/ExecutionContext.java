package org.cloudnook.knightagent.core.agent.strategy;

import lombok.Builder;
import lombok.Getter;
import org.cloudnook.knightagent.core.agent.AgentConfig;
import org.cloudnook.knightagent.core.checkpoint.Checkpointer;
import org.cloudnook.knightagent.core.middleware.MiddlewareChain;
import org.cloudnook.knightagent.core.model.ChatModel;
import org.cloudnook.knightagent.core.tool.ToolInvoker;

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
