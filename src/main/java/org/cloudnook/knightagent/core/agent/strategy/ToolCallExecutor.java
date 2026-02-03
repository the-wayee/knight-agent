package org.cloudnook.knightagent.core.agent.strategy;

import org.cloudnook.knightagent.core.agent.AgentExecutionException;
import org.cloudnook.knightagent.core.agent.AgentResponse;
import org.cloudnook.knightagent.core.message.ToolCall;

/**
 * 工具调用执行器
 * <p>
 * 封装完整的工具调用执行流程：
 * <ol>
 *   <li>中间件拦截 ({@link org.cloudnook.knightagent.core.middleware.Middleware#beforeToolCall})</li>
 *   <li>中断处理 - 如果中间件返回中断结果</li>
 *   <li>实际工具执行</li>
 *   <li>后置处理 ({@link org.cloudnook.knightagent.core.middleware.Middleware#afterToolCall})</li>
 *   <li>状态更新</li>
 * </ol>
 * <p>
 * 所有策略都可以使用此接口来执行工具调用，
 * 自动获得人机交互、限流等中间件能力的支持。
 * <p>
 * 使用示例：
 * <pre>{@code
 * public class MyStrategy implements ExecutionStrategy {
 *     private final ToolCallExecutor toolCallExecutor;
 *
 *     public MyStrategy(ExecutionContext context) {
 *         this.toolCallExecutor = new DefaultToolCallExecutor(
 *             context.getMiddlewareChain(),
 *             context.getToolInvoker(),
 *             context.getCheckpointer()
 *         );
 *     }
 *
 *     private AgentResponse executeTools(List<ToolCall> toolCalls, ExecutionContext context) {
 *         for (ToolCall toolCall : toolCalls) {
 *             AgentResponse interruptResponse = toolCallExecutor.execute(toolCall, context);
 *             if (interruptResponse != null) {
 *                 return interruptResponse; // 被中断，返回给上层
 *             }
 *         }
 *         return null; // 没有中断，继续执行
 *     }
 * }
 * }</pre>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public interface ToolCallExecutor {

    /**
     * 执行单个工具调用
     * <p>
     * 执行流程：
     * <ol>
     *   <li>调用中间件链的 beforeToolCall</li>
     *   <li>如果被中断，保存 checkpoint 并返回中断响应</li>
     *   <li>否则执行工具</li>
     *   <li>调用中间件链的 afterToolCall</li>
     *   <li>更新状态</li>
     * </ol>
     *
     * @param toolCall 工具调用
     * @param context 执行上下文
     * @return 如果被中断返回中断响应，否则返回 null
     * @throws AgentExecutionException 执行失败
     */
    AgentResponse execute(ToolCall toolCall, ExecutionContext context)
            throws AgentExecutionException;
}
