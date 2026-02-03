package org.cloudnook.knightagent.core.agent.strategy;

import org.cloudnook.knightagent.core.agent.AgentConfig;
import org.cloudnook.knightagent.core.agent.AgentExecutionException;
import org.cloudnook.knightagent.core.agent.AgentResponse;
import org.cloudnook.knightagent.core.agent.AgentStatus;
import org.cloudnook.knightagent.core.checkpoint.CheckpointException;
import org.cloudnook.knightagent.core.checkpoint.Checkpointer;
import org.cloudnook.knightagent.core.interception.Interrupt;
import org.cloudnook.knightagent.core.interception.InterceptionResult;
import org.cloudnook.knightagent.core.message.ToolCall;
import org.cloudnook.knightagent.core.message.ToolResult;
import org.cloudnook.knightagent.core.middleware.AgentContext;
import org.cloudnook.knightagent.core.middleware.MiddlewareChain;
import org.cloudnook.knightagent.core.middleware.MiddlewareException;
import org.cloudnook.knightagent.core.state.AgentState;
import org.cloudnook.knightagent.core.tool.ToolExecutionException;
import org.cloudnook.knightagent.core.tool.ToolInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 默认工具调用执行器
 * <p>
 * 实现完整的工具调用执行流程，包括：
 * <ul>
 *   <li>中间件拦截</li>
 *   <li>中断处理（保存 checkpoint）</li>
 *   <li>工具执行</li>
 *   <li>状态更新</li>
 * </ul>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public class DefaultToolCallExecutor implements ToolCallExecutor {

    private static final Logger log = LoggerFactory.getLogger(DefaultToolCallExecutor.class);

    private final MiddlewareChain middlewareChain;
    private final ToolInvoker toolInvoker;
    private final Checkpointer checkpointer;

    /**
     * 构造函数
     *
     * @param middlewareChain 中间件链
     * @param toolInvoker     工具调用器
     * @param checkpointer     检查点存储（可选）
     */
    public DefaultToolCallExecutor(
            MiddlewareChain middlewareChain,
            ToolInvoker toolInvoker,
            Checkpointer checkpointer) {
        this.middlewareChain = middlewareChain;
        this.toolInvoker = toolInvoker;
        this.checkpointer = checkpointer;
    }

    @Override
    public AgentResponse execute(ToolCall toolCall, ExecutionContext context)
            throws AgentExecutionException {

        // 获取 AgentContext
        AgentContext agentContext = context.getAgentContext()
            .orElseThrow(() -> new AgentExecutionException("AgentContext 未设置"));

        // 1. 中间件拦截
        InterceptionResult result;
        try {
            result = middlewareChain.beforeToolCall(toolCall, agentContext);
        } catch (MiddlewareException e) {
            log.error("中间件 beforeToolCall 失败: {}", e.getMessage(), e);
            // 中间件失败，继续执行（或者可以中断）
            result = InterceptionResult.continueExec();
        }

        // 2. 处理拦截结果
        if (result.isInterrupted()) {
            return handleInterrupt(result.interrupt(), context, toolCall);
        }

        if (result.shouldStop()) {
            log.debug("工具调用被中间件停止: {}", toolCall.getName());
            return null;
        }

        // 3. 执行工具
        ToolResult toolResult;
        try {
            toolResult = toolInvoker.invoke(toolCall);
        } catch (ToolExecutionException e) {
            log.error("工具执行失败: {}", e.getMessage(), e);
            toolResult = ToolResult.error(
                toolCall.getId(),
                "执行失败: " + e.getMessage()
            );
        }

        // 4. 后置处理
        try {
            middlewareChain.afterToolCall(toolCall, toolResult, agentContext);
        } catch (MiddlewareException e) {
            log.warn("中间件 afterToolCall 处理失败: {}", e.getMessage());
        }

        // 5. 更新状态
        AgentState currentState = agentContext.getState();
        if (currentState != null) {
            AgentState newState = currentState.addMessage(toolResult.toMessage());
            agentContext.setState(newState);
        }

        return null; // 没有中断，继续执行
    }

    /**
     * 处理中断
     * <p>
     * 保存 checkpoint 并构建中断响应。
     *
     * @param interrupt 中断对象
     * @param context  执行上下文
     * @param toolCall 触发中断的工具调用
     * @return 中断响应
     */
    private AgentResponse handleInterrupt(
            Interrupt interrupt,
            ExecutionContext context,
            ToolCall toolCall) throws AgentExecutionException {

        // 获取 AgentContext
        AgentContext agentContext = context.getAgentContext()
            .orElseThrow(() -> new AgentExecutionException("AgentContext 未设置"));

        // 获取 AgentConfig
        AgentConfig config = context.getConfig();
        if (config == null) {
            config = AgentConfig.defaults();
        }

        log.info("工具调用被中断: 工具={}, 中断类型={}",
                toolCall.getName(), interrupt.getClass().getSimpleName());

        // 保存 checkpoint
        String checkpointId = null;
        if (checkpointer != null && config.getThreadId() != null && agentContext.getState() != null) {
            try {
                checkpointId = checkpointer.save(config.getThreadId(), agentContext.getState());
                log.debug("已保存 checkpoint: {}", checkpointId);
            } catch (CheckpointException e) {
                throw new AgentExecutionException("保存 checkpoint 失败", e, "CHECKPOINT_ERROR");
            }
        }

        // 设置中断状态
        agentContext.setStatus(AgentStatus.builder()
                .statusType(AgentStatus.StatusType.WAITING_FOR_APPROVAL)
                .description("等待中断处理: " + interrupt.description())
                .currentThreadId(config.getThreadId())
                .currentIteration(agentContext.getIteration())
                .build());

        // 构建中断响应
        return AgentResponse.builder()
                .interrupt(interrupt)
                .output(interrupt.description())
                .messages(agentContext.getState() != null ? agentContext.getState().getMessages() : List.of())
                .state(agentContext.getState())
                .threadId(config.getThreadId())
                .checkpointId(checkpointId)
                .durationMs(0) // 中断响应不计算耗时
                .startTime(Instant.now())
                .endTime(Instant.now())
                .build();
    }
}
