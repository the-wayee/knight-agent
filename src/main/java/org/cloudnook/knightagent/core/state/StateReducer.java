package org.cloudnook.knightagent.core.state;

/**
 * 状态归约器接口
 * <p>
 * 定义如何从旧状态生成新状态的规则。
 * 这是函数式编程中的常见模式，确保状态更新的可预测性和可追踪性。
 * <p>
 * 使用场景：
 * <ul>
 *   <li>控制状态更新逻辑</li>
 *   <li>实现状态的撤销/重做</li>
 *   <li>记录状态变更历史</li>
 *   <li>实现中间件拦截状态变更</li>
 * </ul>
 * <p>
 * 使用示例：
 * <pre>{@code
 * StateReducer reducer = (oldState, newState) -> {
 *     // 限制消息历史长度
 *     if (newState.getMessages().size() > 100) {
 *         List<Message> trimmed = newState.getMessages().subList(0, 100);
 *         return AgentState.builder()
 *             .from(newState)
 *             .messages(trimmed)
 *             .build();
 *     }
 *     return newState;
 * };
 * }</pre>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
@FunctionalInterface
public interface StateReducer {

    /**
     * 归约状态
     * <p>
     * 根据旧状态和候选新状态，生成最终的新状态。
     * <p>
     * 注意：
     * <ul>
     *   <li>此方法应该是纯函数，不应有副作用</li>
     *   <li>返回的状态应该是新实例，不应修改输入状态</li>
     *   <li>可以直接返回 newState，表示不进行归约</li>
     * </ul>
     *
     * @param oldState     旧状态
     * @param newState    候选新状态
     * @param stateContext 状态上下文（可选的额外信息）
     * @return 最终的新状态
     */
    AgentState reduce(AgentState oldState, AgentState newState, StateContext stateContext);

    /**
     * 归约状态（无上下文）
     * <p>
     * 默认实现，调用带上下文的方法。
     *
     * @param oldState  旧状态
     * @param newState 候选新状态
     * @return 最终的新状态
     */
    default AgentState reduce(AgentState oldState, AgentState newState) {
        return reduce(oldState, newState, StateContext.empty());
    }

    // ==================== 内置归约器 ====================

    /**
     * 不做任何改变的归约器
     */
    static StateReducer identity() {
        return (oldState, newState, ctx) -> newState;
    }

    /**
     * 限制消息历史长度的归约器
     *
     * @param maxMessages 最大消息数量
     * @return 状态归约器
     */
    static StateReducer limitMessages(int maxMessages) {
        return (oldState, newState, ctx) -> {
            if (newState.getMessages().size() <= maxMessages) {
                return newState;
            }
            return new AgentState.Builder(newState)
                    .messages(newState.getMessages().subList(0, maxMessages))
                    .build();
        };
    }

    /**
     * 合并多个归约器
     * <p>
     * 按顺序应用每个归约器，前一个的输出是后一个的输入。
     *
     * @param reducers 要合并的归约器
     * @return 合并后的归约器
     */
    static StateReducer compose(StateReducer... reducers) {
        return (oldState, newState, ctx) -> {
            AgentState result = newState;
            for (StateReducer reducer : reducers) {
                result = reducer.reduce(oldState, result, ctx);
            }
            return result;
        };
    }
}
