package org.cloudnook.knightagent.core.middleware;

import org.cloudnook.knightagent.core.agent.AgentRequest;
import org.cloudnook.knightagent.core.agent.AgentResponse;
import org.cloudnook.knightagent.core.interception.InterceptionResult;
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
     *   <li>触发审批 - 返回 {@link InterceptionResult#interrupt(Interrupt)}</li>
     *   <li>阻止调用 - 返回 {@link InterceptionResult#stop(String)}</li>
     * </ul>
     * <p>
     * 中间件通过返回 {@link InterceptionResult} 显式声明对执行流程的控制意图：
     * <ul>
     *   <li>继续执行 - 返回 {@link InterceptionResult#continueExec()}</li>
     *   <li>中断执行 - 返回 {@link InterceptionResult#interrupt(Interrupt)}</li>
     *   <li>停止执行 - 返回 {@link InterceptionResult#stop(String)}</li>
     * </ul>
     *
     * @param toolCall 工具调用信息
     * @param context  执行上下文
     * @return 拦截结果
     * @throws MiddlewareException 处理失败
     */
    default InterceptionResult beforeToolCall(ToolCall toolCall, AgentContext context) throws MiddlewareException {
        // 默认实现：继续执行
        return InterceptionResult.continueExec();
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
     * @param state    当前状态
     * @param context  执行上下文
     * @return 最终的状态，如果返回 null 则使用原状态
     * @throws MiddlewareException 处理失败
     */
    default AgentState onStateUpdate(AgentState state, AgentContext context) throws MiddlewareException {
        // 默认不修改状态
        return state;
    }

    /**
     * 发生异常时拦截
     * <p>
     * 当中间件链执行过程中发生异常时调用。
     * 可以用于：
     * <ul>
     *   <li>异常日志记录</li>
     *   <li>异常转换</li>
     *   <li>错误恢复</li>
     *   <li>资源清理</li>
     * </ul>
     * <p>
     * 注意：此方法按反向顺序调用（与 afterInvoke 一致）。
     *
     * @param error   发生的异常
     * @param context 执行上下文
     * @throws MiddlewareException 处理失败时抛出新异常
     */
    default void onError(Throwable error, AgentContext context) throws MiddlewareException {
        // 默认空实现
    }

    /**
     * 最终清理拦截
     * <p>
     * 无论执行成功或失败，都会调用此方法。
     * 可以用于：
     * <ul>
     *   <li>资源释放</li>
     *   <li>指标上报</li>
     *   <li>事务清理</li>
     * </ul>
     * <p>
     * 注意：此方法按反向顺序调用（与 afterInvoke 一致）。
     * 即使 onError 中抛出异常，此方法也会被调用。
     *
     * @param context 执行上下文
     * @param error   执行过程中的异常（如果有），null 表示执行成功
     */
    default void onFinally(AgentContext context, Throwable error) throws MiddlewareException {
        // 默认空实现
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
