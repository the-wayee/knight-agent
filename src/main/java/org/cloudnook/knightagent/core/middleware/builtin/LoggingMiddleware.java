package org.cloudnook.knightagent.core.middleware.builtin;

import lombok.extern.slf4j.Slf4j;
import org.cloudnook.knightagent.core.agent.AgentRequest;
import org.cloudnook.knightagent.core.agent.AgentResponse;
import org.cloudnook.knightagent.core.agent.AgentStatus;
import org.cloudnook.knightagent.core.message.ToolCall;
import org.cloudnook.knightagent.core.message.ToolResult;
import org.cloudnook.knightagent.core.middleware.AgentContext;
import org.cloudnook.knightagent.core.middleware.Middleware;
import org.cloudnook.knightagent.core.state.AgentState;

import java.time.Duration;
import java.time.Instant;

/**
 * 日志记录中间件
 * <p>
 * 记录 Agent 执行过程中的关键事件，便于调试和监控。
 * <p>
 * 记录内容：
 * <ul>
 *   <li>请求输入</li>
 *   <li>响应输出</li>
 *   <li>工具调用和结果</li>
 *   <li>执行时间</li>
 *   <li>Token 消耗</li>
 *   <li>错误信息</li>
 * </ul>
 * <p>
 * 使用示例：
 * <pre>{@code
 * Agent agent = AgentBuilder.builder()
 *     .model(chatModel)
 *     .middleware(new LoggingMiddleware())
 *     .build();
 * }</pre>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
@Slf4j
public class LoggingMiddleware implements Middleware {

    private final boolean logRequests;
    private final boolean logResponses;
    private final boolean logToolCalls;
    private final boolean logTokenUsage;
    private final boolean logErrors;
    private final boolean logStateChanges;

    private LoggingMiddleware(Builder builder) {
        this.logRequests = builder.logRequests;
        this.logResponses = builder.logResponses;
        this.logToolCalls = builder.logToolCalls;
        this.logTokenUsage = builder.logTokenUsage;
        this.logErrors = builder.logErrors;
        this.logStateChanges = builder.logStateChanges;
    }

    @Override
    public void beforeInvoke(AgentRequest request, AgentContext context) {
        if (logRequests) {
            log.info("===== Agent 请求开始 =====");
            if (context.getStatus() != null && context.getStatus().getCurrentThreadId() != null) {
                log.info("Thread ID: {}", context.getStatus().getCurrentThreadId());
            }
            log.info("User ID: {}", request.getUserId());
            log.info("输入: {}", request.getInput());
            log.info("最大迭代次数: {}", request.getMaxIterations());
            if (request.getSystemPrompt() != null) {
                log.debug("系统提示词: {}", request.getSystemPrompt());
            }
        }
    }

    @Override
    public void afterInvoke(AgentResponse response, AgentContext context) {
        if (logResponses) {
            log.info("===== Agent 请求完成 =====");
            log.info("输出: {}", response.getOutput());
            log.info("消息数: {}", response.getMessages().size());
            log.info("工具调用数: {}", response.getToolCalls().size());
            log.info("执行时间: {}ms", response.getDurationMs());
        }
    }

    @Override
    public void beforeToolCall(ToolCall toolCall, AgentContext context) {
        if (logToolCalls) {
            log.info("→ 工具调用: {} [{}]", toolCall.getName(), toolCall.getId());
            log.debug("   参数: {}", toolCall.getArguments());
        }
        // 不修改 context，允许工具正常执行
    }

    @Override
    public void afterToolCall(ToolCall toolCall, ToolResult toolResult, AgentContext context) {
        if (logToolCalls) {
            if (toolResult.isError()) {
                log.warn("← 工具调用失败: {} - {}",
                        toolCall.getName(), toolResult.getErrorMessage());
            } else {
                log.info("← 工具调用完成: {} [{}]",
                        toolCall.getName(), toolCall.getId());
                log.debug("   结果长度: {} 字符",
                        toolResult.getResult() != null ? toolResult.getResult().length() : 0);
            }
        }
    }

    @Override
    public AgentState onStateUpdate(AgentState state, AgentContext context) {
        if (logStateChanges) {
            log.debug("状态更新: 消息数 {}, 版本 {}",
                    state.getMessages().size(), state.getVersion());
        }
        return state;
    }

    /**
     * 创建构建器
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 创建默认配置的中间件
     *
     * @return 日志中间件
     */
    public static LoggingMiddleware defaults() {
        return new Builder().build();
    }

    /**
     * 构建器
     */
    public static class Builder {

        private boolean logRequests = true;
        private boolean logResponses = true;
        private boolean logToolCalls = true;
        private boolean logTokenUsage = true;
        private boolean logErrors = true;
        private boolean logStateChanges = false;

        /**
         * 是否记录请求（默认 true）
         */
        public Builder logRequests(boolean logRequests) {
            this.logRequests = logRequests;
            return this;
        }

        /**
         * 是否记录响应（默认 true）
         */
        public Builder logResponses(boolean logResponses) {
            this.logResponses = logResponses;
            return this;
        }

        /**
         * 是否记录工具调用（默认 true）
         */
        public Builder logToolCalls(boolean logToolCalls) {
            this.logToolCalls = logToolCalls;
            return this;
        }

        /**
         * 是否记录 Token 使用（默认 true）
         */
        public Builder logTokenUsage(boolean logTokenUsage) {
            this.logTokenUsage = logTokenUsage;
            return this;
        }

        /**
         * 是否记录错误（默认 true）
         */
        public Builder logErrors(boolean logErrors) {
            this.logErrors = logErrors;
            return this;
        }

        /**
         * 是否记录状态变化（默认 false）
         */
        public Builder logStateChanges(boolean logStateChanges) {
            this.logStateChanges = logStateChanges;
            return this;
        }

        /**
         * 构建中间件实例
         */
        public LoggingMiddleware build() {
            return new LoggingMiddleware(this);
        }
    }
}
