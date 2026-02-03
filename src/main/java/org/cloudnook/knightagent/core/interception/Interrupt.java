package org.cloudnook.knightagent.core.interception;

import java.time.Instant;

/**
 * 中断接口
 * <p>
 * 当 Middleware 需要暂停执行并等待外部干预时，返回此接口的实现。
 * <p>
 * 中断类型包括：
 * <ul>
 *   <li>工具审批中断（ToolApprovalInterrupt）- 等待人工审批工具调用</li>
 *   <li>限流中断（RateLimitInterrupt）- 等待限流恢复</li>
 *   <li>自定义中断（CustomInterrupt）- 用户自定义的中断场景</li>
 * </ul>
 * <p>
 * 中断生命周期：
 * <pre>
 * 1. Middleware.beforeToolCall() 返回 InterceptionResult.interrupt(interrupt)
 * 2. AgentExecutor 检测到中断，保存 checkpoint，返回包含 interrupt 的 AgentResponse
 * 3. 调用者处理中断（例如：展示审批界面）
 * 4. 调用者使用 agent.resume(interrupt.toCommand(resumeValue)) 恢复执行
 * </pre>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public sealed interface Interrupt permits
        ToolApprovalInterrupt,
        RateLimitInterrupt {

    /**
     * 获取中断 ID
     * <p>
     * 唯一标识此次中断，用于恢复时匹配。
     *
     * @return 中断 ID
     */
    String interruptId();

    /**
     * 获取线程 ID
     * <p>
     * 关联的 conversation thread ID。
     *
     * @return 线程 ID
     */
    String threadId();

    /**
     * 获取中断时间戳
     *
     * @return 中断发生的时间
     */
    Instant timestamp();

    /**
     * 获取中断描述
     * <p>
     * 人类可读的中断原因描述。
     *
     * @return 中断描述
     */
    String description();

    /**
     * 创建恢复命令
     * <p>
     * 将中断转换为恢复命令，用于传递给 Agent.resume()。
     *
     * @param resumeValue 恢复时传递的值（例如：审批决策）
     * @return 恢复命令
     */
    InterruptCommand toCommand(Object resumeValue);
}
