package org.cloudnook.knightagent.core.interception;

/**
 * 拦截结果
 * <p>
 * Middleware 通过返回此接口显式声明对执行流程的控制意图，
 * 而不是通过修改 AgentContext 的副作用。
 * <p>
 * 使用示例：
 * <pre>{@code
 * // 继续执行
 * return InterceptionResult.continueExec();
 *
 * // 中断执行（需要人工干预）
 * return InterceptionResult.interrupt(interrupt);
 *
 * // 停止执行
 * return InterceptionResult.stop("用户取消");
 * }</pre>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public sealed interface InterceptionResult permits
        InterceptionResult.ContinueResult,
        InterceptionResult.InterruptResult,
        InterceptionResult.StopResult {

    /**
     * 继续执行
     */
    record ContinueResult() implements InterceptionResult {}

    /**
     * 中断执行（等待外部恢复）
     * <p>
     * 例如：等待人工审批、等待限流恢复、等待外部事件等
     */
    record InterruptResult(Interrupt interrupt) implements InterceptionResult {}

    /**
     * 停止执行（不再继续）
     */
    record StopResult(String reason) implements InterceptionResult {}

    // ==================== 便捷判断方法 ====================

    /**
     * 是否应该继续执行
     */
    default boolean shouldContinue() {
        return this instanceof ContinueResult;
    }

    /**
     * 是否被中断（需要外部恢复）
     */
    default boolean isInterrupted() {
        return this instanceof InterruptResult;
    }

    /**
     * 是否应该停止执行
     */
    default boolean shouldStop() {
        return this instanceof StopResult;
    }

    // ==================== 便捷访问方法 ====================

    /**
     * 获取中断对象（如果是中断结果）
     * <p>
     * 无需类型转换，直接调用此方法即可获取 Interrupt。
     *
     * @return 中断对象，如果不是中断结果则返回 null
     */
    default Interrupt interrupt() {
        if (this instanceof InterruptResult ir) {
            return ir.interrupt();
        }
        return null;
    }

    /**
     * 获取停止原因（如果是停止结果）
     *
     * @return 停止原因，如果不是停止结果则返回 null
     */
    default String stopReason() {
        if (this instanceof StopResult sr) {
            return sr.reason();
        }
        return null;
    }

    // ==================== 工厂方法 ====================

    /**
     * 创建继续执行结果
     */
    static InterceptionResult continueExec() {
        return new ContinueResult();
    }

    /**
     * 创建中断结果
     *
     * @param interrupt 中断对象
     * @return 中断结果
     */
    static InterceptionResult interrupt(Interrupt interrupt) {
        return new InterruptResult(interrupt);
    }

    /**
     * 创建停止结果
     *
     * @param reason 停止原因
     * @return 停止结果
     */
    static InterceptionResult stop(String reason) {
        return new StopResult(reason);
    }
}
