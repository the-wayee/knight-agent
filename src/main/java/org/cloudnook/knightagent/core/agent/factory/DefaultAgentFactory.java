package org.cloudnook.knightagent.core.agent.factory;

/**
 * 默认 Agent 工厂实现
 * <p>
 * 提供标准的 Agent 构建器创建方法。
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public class DefaultAgentFactory implements AgentFactory {

    @Override
    public AgentBuilder createAgent() {
        return new AgentBuilder();
    }

    /**
     * 静态工厂方法
     * <p>
     * 提供更简洁的创建方式。
     *
     * @return Agent 构建器
     */
    public static AgentBuilder agent() {
        return new AgentBuilder();
    }
}
