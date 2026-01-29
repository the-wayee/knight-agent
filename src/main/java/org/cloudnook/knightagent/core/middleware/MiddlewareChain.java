package org.cloudnook.knightagent.core.middleware;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 中间件链
 * <p>
 * 管理中间件的执行顺序和调用。
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public class MiddlewareChain {

    private static final Logger log = LoggerFactory.getLogger(MiddlewareChain.class);

    /**
     * 中间件列表（按优先级排序）
     */
    private final List<Middleware> middlewares;

    /**
     * 构造函数
     *
     * @param middlewares 中间件列表
     */
    public MiddlewareChain(List<Middleware> middlewares) {
        // 按优先级排序（值越小越先执行）
        this.middlewares = new ArrayList<>(middlewares);
        this.middlewares.sort(Comparator.comparingInt(Middleware::getPriority));
        log.debug("中间件链初始化: {}", this.middlewares.stream()
                .map(Middleware::getName)
                .toList());
    }

    /**
     * 执行 beforeInvoke
     */
    public void beforeInvoke(org.cloudnook.knightagent.core.agent.AgentRequest request, AgentContext context)
            throws MiddlewareException {
        for (Middleware middleware : middlewares) {
            try {
                log.debug("执行 beforeInvoke: {}", middleware.getName());
                middleware.beforeInvoke(request, context);
                if (context.isStopped()) {
                    log.debug("中间件停止执行: {}", middleware.getName());
                    break;
                }
            } catch (MiddlewareException e) {
                throw e;
            } catch (Exception e) {
                throw new MiddlewareException(
                        "beforeInvoke 失败: " + e.getMessage(),
                        middleware.getName(),
                        e
                );
            }
        }
    }

    /**
     * 执行 afterInvoke
     */
    public void afterInvoke(org.cloudnook.knightagent.core.agent.AgentResponse response, AgentContext context)
            throws MiddlewareException {
        // 反向执行 afterInvoke
        for (int i = middlewares.size() - 1; i >= 0; i--) {
            Middleware middleware = middlewares.get(i);
            try {
                log.debug("执行 afterInvoke: {}", middleware.getName());
                middleware.afterInvoke(response, context);
                if (context.isStopped()) {
                    log.debug("中间件停止执行: {}", middleware.getName());
                    break;
                }
            } catch (MiddlewareException e) {
                throw e;
            } catch (Exception e) {
                throw new MiddlewareException(
                        "afterInvoke 失败: " + e.getMessage(),
                        middleware.getName(),
                        e
                );
            }
        }
    }

    /**
     * 执行 beforeToolCall
     * <p>
     * 中间件通过 context 控制行为：
     * <ul>
     *   <li>{@link AgentContext#stop()} - 停止执行</li>
     *   <li> - 触发审批</li>
     * </ul>
     */
    public void beforeToolCall(org.cloudnook.knightagent.core.message.ToolCall toolCall, AgentContext context)
            throws MiddlewareException {
        for (Middleware middleware : middlewares) {
            try {
                log.debug("执行 beforeToolCall: {}", middleware.getName());
                middleware.beforeToolCall(toolCall, context);
                if (context.isStopped()) {
                    log.debug("中间件停止执行: {}", middleware.getName());
                    break;
                }
                // 检查是否触发审批，如果有则停止后续中间件执行
                if (context.hasPendingApproval()) {
                    break;
                }
            } catch (MiddlewareException e) {
                throw e;
            } catch (Exception e) {
                throw new MiddlewareException(
                        "beforeToolCall 失败: " + e.getMessage(),
                        middleware.getName(),
                        e
                );
            }
        }
    }

    /**
     * 执行 afterToolCall
     */
    public void afterToolCall(
            org.cloudnook.knightagent.core.message.ToolCall toolCall,
            org.cloudnook.knightagent.core.message.ToolResult toolResult,
            AgentContext context) throws MiddlewareException {
        // 反向执行 afterToolCall
        for (int i = middlewares.size() - 1; i >= 0; i--) {
            Middleware middleware = middlewares.get(i);
            try {
                log.debug("执行 afterToolCall: {}", middleware.getName());
                middleware.afterToolCall(toolCall, toolResult, context);
            } catch (MiddlewareException e) {
                throw e;
            } catch (Exception e) {
                throw new MiddlewareException(
                        "afterToolCall 失败: " + e.getMessage(),
                        middleware.getName(),
                        e
                );
            }
        }
    }

    /**
     * 执行 onStateUpdate
     */
    public org.cloudnook.knightagent.core.state.AgentState onStateUpdate(
            org.cloudnook.knightagent.core.state.AgentState state,
            AgentContext context) throws MiddlewareException {
        org.cloudnook.knightagent.core.state.AgentState result = state;
        for (Middleware middleware : middlewares) {
            try {
                log.debug("执行 onStateUpdate: {}", middleware.getName());
                org.cloudnook.knightagent.core.state.AgentState modified = middleware.onStateUpdate(result, context);
                if (modified != null) {
                    result = modified;
                }
                if (context.isStopped()) {
                    break;
                }
            } catch (MiddlewareException e) {
                throw e;
            } catch (Exception e) {
                throw new MiddlewareException(
                        "onStateUpdate 失败: " + e.getMessage(),
                        middleware.getName(),
                        e
                );
            }
        }
        return result;
    }

    /**
     * 执行 onError
     * <p>
     * 当中间件链执行过程中发生异常时调用。
     * 反向执行，与 afterInvoke 顺序一致。
     *
     * @param error   发生的异常
     * @param context 执行上下文
     */
    public void onError(Throwable error, AgentContext context) {
        // 反向执行 onError
        for (int i = middlewares.size() - 1; i >= 0; i--) {
            Middleware middleware = middlewares.get(i);
            try {
                log.debug("执行 onError: {}", middleware.getName());
                middleware.onError(error, context);
            } catch (MiddlewareException e) {
                // onError 中再抛出异常，记录但不中断
                log.warn("onError 失败: {} - {}", middleware.getName(), e.getMessage());
            } catch (Exception e) {
                log.warn("onError 异常: {} - {}", middleware.getName(), e.getMessage());
            }
        }
    }

    /**
     * 执行 onFinally
     * <p>
     * 无论执行成功或失败都会调用。
     * 反向执行，与 afterInvoke 顺序一致。
     *
     * @param context 执行上下文
     * @param error   执行过程中的异常（如果有），null 表示执行成功
     */
    public void onFinally(AgentContext context, Throwable error) {
        // 反向执行 onFinally
        for (int i = middlewares.size() - 1; i >= 0; i--) {
            Middleware middleware = middlewares.get(i);
            try {
                log.debug("执行 onFinally: {}", middleware.getName());
                middleware.onFinally(context, error);
            } catch (MiddlewareException e) {
                // onFinally 中抛出异常，记录但不中断
                log.warn("onFinally 失败: {} - {}", middleware.getName(), e.getMessage());
            } catch (Exception e) {
                log.warn("onFinally 异常: {} - {}", middleware.getName(), e.getMessage());
            }
        }
    }

    /**
     * 获取中间件数量
     */
    public int size() {
        return middlewares.size();
    }

    /**
     * 检查是否为空
     */
    public boolean isEmpty() {
        return middlewares.isEmpty();
    }

    /**
     * 获取中间件列表
     */
    public List<Middleware> getMiddlewares() {
        return new ArrayList<>(middlewares);
    }
}
