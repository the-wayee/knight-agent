package org.cloudnook.knightagent.core.multiagent;

import org.cloudnook.knightagent.core.agent.AgentRequest;
import org.cloudnook.knightagent.core.agent.AgentResponse;

import java.util.List;
import java.util.Optional;

/**
 * 手 off 策略接口
 * <p>
 * 定义 Multi-AgentSystem 中如何决定下一个调用哪个 Agent 的策略。
 * <p>
 * 常见实现：
 * <ul>
 *   <li>{@link SupervisorStrategy} - 由 LLM 根据各 Agent 描述决定路由</li>
 *   <li>{@link KeywordMatchStrategy} - 根据关键词匹配选择 Agent</li>
 *   <li>{@link PriorityStrategy} - 根据优先级选择</li>
 *   <li>{@link RoundRobinStrategy} - 轮询选择</li>
 * </ul>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public interface HandoffStrategy {

    /**
     * 决定下一个调用哪个 Agent
     * <p>
     * 根据当前请求、响应和可用 Agent 列表，返回下一个应该调用的 Agent 名称。
     * <p>
     * 返回值说明：
     * <ul>
     *   <li>Optional.of("agentName") - 转交给指定的 Agent</li>
     *   <li>Optional.of("FINAL") - 流程结束，返回当前结果</li>
     *   <li>Optional.empty() - 无法决定，由系统使用默认策略</li>
     * </ul>
     *
     * @param request         当前请求
     * @param currentResponse 当前 Agent 的响应
     * @param currentAgent    当前执行的 Agent 节点
     * @param availableAgents 所有可用的 Agent 节点
     * @param handoffCount    已执行的手 off 次数
     * @return 下一个 Agent 的名称，或空表示使用默认策略
     */
    Optional<String> nextAgent(AgentRequest request,
                               AgentResponse currentResponse,
                               AgentNode currentAgent,
                               List<AgentNode> availableAgents,
                               int handoffCount);

    /**
     * 检查是否应该发起手 off
     * <p>
     * 在调用 Agent 之前检查，如果返回 false，则直接使用当前 Agent 的结果，
     * 不再继续手 off 流程。
     * <p>
     * 默认实现为始终允许手 off。
     *
     * @param response Agent 响应
     * @return 如果应该继续手 off 返回 true
     */
    default boolean shouldContinue(AgentResponse response) {
        return true;
    }

    /**
     * 获取策略名称
     *
     * @return 策略名称
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }
}
