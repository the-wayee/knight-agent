package org.cloudnook.knightagent.core.agent.factory;

import org.cloudnook.knightagent.core.agent.Agent;

/**
 * Agent 工厂接口
 * <p>
 * 定义创建 Agent 的工厂方法。
 * 提供统一的 Agent 创建入口，简化 Agent 的构建过程。
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public interface AgentFactory {

    /**
     * 创建 Agent 构建器
     * <p>
     * 返回一个 Fluent Builder API，用于链式配置 Agent。
     *
     * @return Agent 构建器
     */
    AgentBuilder createAgent();
}
