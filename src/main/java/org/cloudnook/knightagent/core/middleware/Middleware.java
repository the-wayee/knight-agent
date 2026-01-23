package org.cloudnook.knightagent.core.middleware;

import org.cloudnook.knightagent.core.agent.AgentRequest;
import org.cloudnook.knightagent.core.agent.AgentResponse;
import org.cloudnook.knightagent.core.message.Message;
import org.cloudnook.knightagent.core.message.ToolCall;
import org.cloudnook.knightagent.core.state.AgentState;

/**
 * 中间件接口
 * <p>
 * 定义 Agent 执行过程中的拦截点。
 * 中间件可以修改输入/输出、注入上下文、实现横切关注点。
 * <p>
 * 拦截点：
 * <ul>
 *   <li>{@link #beforeInvoke} - Agent 调用前</li>
 *   <li>{@link #afterInvoke} - Agent 调用后</li>
 *   <li>{@link #beforeToolCall} - 工具调用前</li>
 *   <li>{@link #afterToolCall} - 工具调用后</li>
 *   <li>{@link #onStateUpdate} - 状态更新时</li>
 * </ul>
 * <p>
 * 使用示例：
 * <pre>{@code
 * public class LoggingMiddleware implements Middleware {
 *     @Override
 *     public void beforeInvoke(AgentRequest request, AgentContext context) {
 *         log.info("Agent 调用: {}", request.getInput());
 *     }
 *
 *     @Override
 *     public void afterInvoke(AgentResponse response, AgentContext context) {
 *         log.info("Agent 完成: {}", response.getOutput());
 *     }
 * }
 * }</pre>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public interface Middleware {

    /**
     * Agent 调用前拦截
     * <p>
     * 在 Agent 开始执行前调用。
     * 可以用于：
     * <ul>
     *   <li>日志记录</li>
     *   <li>请求验证</li>
     *   <li>注入上下文</li>
     *   <li>修改请求参数</li>
     * </ul>
     *
     * @param request Agent 请求
     * @param context 执行上下文
     * @throws MiddlewareException 处理失败
     */
    default void beforeInvoke(AgentRequest request, AgentContext context) throws MiddlewareException {
        // 默认空实现
    }

    /**
     * Agent 调用后拦截
     * <p>
     * 在 Agent 完成执行后调用。
     * 可以用于：
     * <ul>
     *   <li>日志记录</li>
     *   <li>响应修改</li>
     *   <li>指标收集</li>
     *   <li>错误处理</li>
     * </ul>
     *
     * @param response Agent 响应
     * @param context 执行上下文
     * @throws MiddlewareException 处理失败
     */
    default void afterInvoke(AgentResponse response, AgentContext context) throws MiddlewareException {
        // 默认空实现
    }

    /**
     * 工具调用前拦截
     * <p>
     * 在执行工具调用前调用。
     * 可以用于：
     * <ul>
     *   <li>权限检查</li>
     *   <li>参数验证</li>
     *   <li>调用日志</li>
     *   <li>阻止调用</li>
     * </ul>
     *
     * @param toolCall 工具调用信息
     * @param context  执行上下文
     * @return 如果返回 false，阻止该工具调用
     * @throws MiddlewareException 处理失败
     */
    default boolean beforeToolCall(ToolCall toolCall, AgentContext context) throws MiddlewareException {
        // 默认允许所有工具调用
        return true;
    }

    /**
     * 工具调用后拦截
     * <p>
     * 在工具调用完成后调用。
     * 可以用于：
     * <ul>
     *   <li>结果修改</li>
     *   <li>错误处理</li>
     *   <li>调用日志</li>
     * </ul>
     *
     * @param toolCall      工具调用信息
     * @param toolResult    工具执行结果
     * @param context       执行上下文
     * @throws MiddlewareException 处理失败
     */
    default void afterToolCall(ToolCall toolCall, org.cloudnook.knightagent.core.message.ToolResult toolResult, AgentContext context) throws MiddlewareException {
        // 默认空实现
    }

    /**
     * 状态更新拦截
     * <p>
     * 在状态更新时调用。
     * 可以用于：
     * <ul>
     *   <li>状态验证</li>
     *   <li>状态修改</li>
     *   <li>状态审计</li>
     * </ul>
     *
     * @param oldState 旧状态
     * @param newState 新状态（候选）
     * @param context  执行上下文
     * @return 最终的状态，如果返回 null 则使用 newState
     * @throws MiddlewareException 处理失败
     */
    default AgentState onStateUpdate(AgentState oldState, AgentState newState, AgentContext context) throws MiddlewareException {
        // 默认不修改状态
        return newState;
    }

    /**
     * 获取中间件名称
     * <p>
     * 用于日志和调试。
     *
     * @return 中间件名称
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * 获取中间件优先级
     * <p>
     * 值越小优先级越高，越先执行。
     *
     * @return 优先级，默认 100
     */
    default int getPriority() {
        return 100;
    }
}
