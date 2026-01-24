package org.cloudnook.knightagent.core.agent.factory;

import lombok.Getter;
import lombok.Setter;
import org.cloudnook.knightagent.core.agent.Agent;
import org.cloudnook.knightagent.core.agent.AgentConfig;
import org.cloudnook.knightagent.core.agent.AgentExecutor;
import org.cloudnook.knightagent.core.agent.strategy.ExecutionContext;
import org.cloudnook.knightagent.core.agent.strategy.ReActStrategy;
import org.cloudnook.knightagent.core.checkpoint.Checkpointer;
import org.cloudnook.knightagent.core.checkpoint.InMemorySaver;
import org.cloudnook.knightagent.core.middleware.Middleware;
import org.cloudnook.knightagent.core.middleware.MiddlewareChain;
import org.cloudnook.knightagent.core.model.ChatModel;
import org.cloudnook.knightagent.core.tool.Tool;
import org.cloudnook.knightagent.core.tool.ToolInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent 构建器
 * <p>
 * 提供 Fluent API 用于构建 Agent 实例。
 * 支持链式调用，简化 Agent 的创建过程。
 * <p>
 * 使用示例：
 * <pre>{@code
 * Agent agent = new AgentBuilder()
 *     .model(chatModel)
 *     .tools(List.of(weatherTool, searchTool))
 *     .checkpointer(checkpointer)
 *     .config(AgentConfig.defaults())
 *     .build();
 * }</pre>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public class AgentBuilder {

    private static final Logger log = LoggerFactory.getLogger(AgentBuilder.class);

    @Getter
    @Setter
    private ChatModel model;

    @Getter
    @Setter
    private List<Tool> tools;

    @Getter
    @Setter
    private Checkpointer checkpointer;

    @Getter
    @Setter
    private List<Middleware> middlewares;

    @Getter
    @Setter
    private AgentConfig config;

    @Getter
    @Setter
    private org.cloudnook.knightagent.core.agent.strategy.ExecutionStrategy strategy;

    /**
     * 设置 LLM 模型
     *
     * @param model LLM 模型
     * @return this
     */
    public AgentBuilder model(ChatModel model) {
        this.model = model;
        return this;
    }

    /**
     * 设置工具列表
     *
     * @param tools 工具列表
     * @return this
     */
    public AgentBuilder tools(List<Tool> tools) {
        this.tools = tools;
        return this;
    }

    /**
     * 添加单个工具
     *
     * @param tool 工具
     * @return this
     */
    public AgentBuilder tool(Tool tool) {
        if (this.tools == null) {
            this.tools = new ArrayList<>();
        }
        this.tools.add(tool);
        return this;
    }

    /**
     * 设置检查点存储
     *
     * @param checkpointer 检查点存储
     * @return this
     */
    public AgentBuilder checkpointer(Checkpointer checkpointer) {
        this.checkpointer = checkpointer;
        return this;
    }

    /**
     * 设置中间件列表
     *
     * @param middlewares 中间件列表
     * @return this
     */
    public AgentBuilder middlewares(List<Middleware> middlewares) {
        this.middlewares = middlewares;
        return this;
    }

    /**
     * 添加单个中间件
     *
     * @param middleware 中间件
     * @return this
     */
    public AgentBuilder middleware(Middleware middleware) {
        if (this.middlewares == null) {
            this.middlewares = new ArrayList<>();
        }
        this.middlewares.add(middleware);
        return this;
    }

    /**
     * 设置 Agent 配置
     *
     * @param config Agent 配置
     * @return this
     */
    public AgentBuilder config(AgentConfig config) {
        this.config = config;
        return this;
    }

    /**
     * 设置执行策略
     *
     * @param strategy 执行策略
     * @return this
     */
    public AgentBuilder strategy(org.cloudnook.knightagent.core.agent.strategy.ExecutionStrategy strategy) {
        this.strategy = strategy;
        return this;
    }

    /**
     * 构建 Agent 实例
     *
     * @return Agent 实例
     */
    public Agent build() {
        // 验证必填字段
        if (model == null) {
            throw new IllegalStateException("model is required");
        }

        // 构建默认配置
        AgentConfig finalConfig = config != null ? config : AgentConfig.defaults();

        // 构建默认策略
        org.cloudnook.knightagent.core.agent.strategy.ExecutionStrategy finalStrategy =
                strategy != null ? strategy : new ReActStrategy();

        // 构建工具执行器
        ToolInvoker toolInvoker = new ToolInvoker();
        if (tools != null) {
            toolInvoker.registerAll(tools);
            log.debug("注册了 {} 个工具", tools.size());
        }

        // 构建中间件链
        List<Middleware> middlewareList = finalConfig.getMiddlewares() != null
                ? finalConfig.getMiddlewares()
                : (middlewares != null ? middlewares : List.of());
        MiddlewareChain middlewareChain = new MiddlewareChain(middlewareList);

        // 构建检查点存储
        Checkpointer finalCheckpointer = checkpointer != null
                ? checkpointer
                : new InMemorySaver();

        // 构建执行上下文
        ExecutionContext context = ExecutionContext.builder()
                .model(model)
                .toolInvoker(toolInvoker)
                .checkpointer(finalCheckpointer)
                .config(finalConfig)
                .middlewareChain(middlewareChain)
                .build();

        // 创建并返回 Agent
        return new AgentExecutor(finalStrategy, context);
    }
}
