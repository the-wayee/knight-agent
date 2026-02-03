package org.cloudnook.knightagent.core.interception;

import java.time.Duration;
import java.time.Instant;

/**
 * 限流中断（示例扩展）
 * <p>
 * 当触发限流时使用，等待限流恢复后继续执行。
 * <p>
 * 使用示例：
 * <pre>{@code
 * public class RateLimitMiddleware implements Middleware {
 *     private final RateLimiter rateLimiter;
 *
 *     @Override
 *     public InterceptionResult beforeToolCall(ToolCall toolCall, AgentContext context) {
 *         if (rateLimiter.isExceeded()) {
 *             return InterceptionResult.interrupt(
 *                 RateLimitInterrupt.of(
 *                     context.getStatus().getCurrentThreadId(),
 *                     rateLimiter.getRetryAfter()
 *                 )
 *             );
 *         }
 *         return InterceptionResult.continueExec();
 *     }
 * }
 * }</pre>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public record RateLimitInterrupt(
    String interruptId,
    String threadId,
    Instant timestamp,
    String description,
    Duration retryAfter
) implements Interrupt {

    /**
     * 创建限流中断
     *
     * @param threadId 线程 ID
     * @param retryAfter 重试等待时间
     * @return 限流中断
     */
    public static RateLimitInterrupt of(String threadId, Duration retryAfter) {
        return new RateLimitInterrupt(
            "interrupt_" + System.currentTimeMillis(),
            threadId,
            Instant.now(),
            "触发限流，请等待 " + retryAfter.getSeconds() + " 秒后重试",
            retryAfter
        );
    }

    @Override
    public InterruptCommand toCommand(Object resumeValue) {
        return new RateLimitWaitCommand(
            interruptId,
            threadId,
            null,  // checkpointId 由外部设置
            retryAfter
        );
    }
}
