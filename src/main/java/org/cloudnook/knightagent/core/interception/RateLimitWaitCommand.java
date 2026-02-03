package org.cloudnook.knightagent.core.interception;

import java.time.Duration;

/**
 * 限流等待命令（示例扩展）
 * <p>
 * 用于从限流中断恢复执行。
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public record RateLimitWaitCommand(
    String interruptId,
    String threadId,
    String checkpointId,
    Duration retryAfter
) implements InterruptCommand {}
