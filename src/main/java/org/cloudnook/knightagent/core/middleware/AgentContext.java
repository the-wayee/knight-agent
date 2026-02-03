package org.cloudnook.knightagent.core.middleware;

import lombok.Data;
import lombok.Getter;
import org.cloudnook.knightagent.core.agent.AgentRequest;
import org.cloudnook.knightagent.core.agent.AgentResponse;
import org.cloudnook.knightagent.core.agent.AgentStatus;
import org.cloudnook.knightagent.core.state.AgentState;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 中间件上下文
 * <p>
 * 在中间件链中传递的执行上下文信息。
 * <p>
 * 职责划分：
 * <ul>
 *   <li>AgentContext - 单次执行的上下文（请求、迭代次数、停止标志、状态）</li>
 *   <li>AgentState - Thread 的完整状态（消息、自定义数据），跨请求持久化</li>
 * </ul>
 * <p>
 * 关于状态访问：
 * <ul>
 *   <li>中间件可以通过 {@link #getState()} 只读访问持久化状态</li>
 *   <li>中间件修改状态应通过 {@link Middleware#onStateUpdate} 的返回值实现</li>
 *   <li>AgentContext 持有状态引用，不负责状态的生命周期管理</li>
 * </ul>
 * <p>
 * 关于运行时状态：
 * <ul>
 *   <li>AgentStatus 表示当前执行状态（RUNNING, WAITING_FOR_TOOL, WAITING_FOR_APPROVAL 等）</li>
 *   <li>ReActStrategy 在每次迭代时更新此状态</li>
 *   <li>中间件可以读取状态了解执行进展</li>
 * </ul>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
@Data
public class AgentContext {

    /**
     * 当前请求
     */
    private AgentRequest request;

    /**
     * 当前响应（可能为 null）
     */
    private AgentResponse response;

    /**
     * 当前状态（引用，非副本）
     * <p>
     * 中间件可以只读访问此状态。
     * 如需修改状态，应通过 {@link Middleware#onStateUpdate} 的返回值实现。
     */
    private AgentState state;

    /**
     * 当前运行时状态
     * <p>
     * 表示 Agent 的执行状态，在循环迭代时由 ReActStrategy 更新。
     * 中间件可以读取此状态了解当前执行进展。
     */
    private AgentStatus status;

    /**
     * 当前迭代次数
     */
    private int iteration;

    /**
     * 自定义数据
     */
    @Getter
    private final Map<String, Object> data = new HashMap<>();

    /**
     * 创建上下文
     *
     * @param request Agent 请求
     * @return AgentContext 实例
     */
    public static AgentContext of(AgentRequest request) {
        return new AgentContext(request);
    }

    /**
     * 构造函数
     *
     * @param request Agent 请求
     */
    public AgentContext(AgentRequest request) {
        this.request = request;
    }

    // ==================== 快照功能 ====================

    /**
     * 创建上下文的不可变快照
     *
     * @return 不可变的上下文快照
     */
    public ContextSnapshot snapshot() {
        return new ContextSnapshot(
                this.request,
                this.response,
                this.state,
                this.status,
                this.iteration,
                Map.copyOf(this.data)
        );
    }

    /**
     * 从快照恢复上下文
     *
     * @param snapshot 快照
     */
    public void restore(ContextSnapshot snapshot) {
        this.request = snapshot.request();
        this.response = snapshot.response();
        this.state = snapshot.state();
        this.status = snapshot.status();
        this.iteration = snapshot.iteration();
        this.data.clear();
        this.data.putAll(snapshot.data());
    }

    // ==================== 自定义数据便捷方法 ====================

    /**
     * 设置自定义数据
     *
     * @param key   键
     * @param value 值
     */
    public void set(String key, Object value) {
        this.data.put(key, value);
    }

    /**
     * 获取自定义数据
     *
     * @param key 键
     * @return 值的 Optional 包装
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key) {
        return Optional.ofNullable((T) this.data.get(key));
    }

    /**
     * 获取自定义数据或默认值
     *
     * @param key          键
     * @param defaultValue 默认值
     * @return 值，如果不存在返回默认值
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrDefault(String key, T defaultValue) {
        return (T) this.data.getOrDefault(key, defaultValue);
    }

    /**
     * 检查是否包含数据
     *
     * @param key 键
     * @return 如果包含返回 true
     */
    public boolean contains(String key) {
        return this.data.containsKey(key);
    }

    /**
     * 获取所有数据键
     *
     * @return 键集合
     */
    public Set<String> getKeys() {
        return this.data.keySet();
    }

    /**
     * 上下文快照（不可变记录）
     * <p>
     * 包含执行上下文的完整快照，包括状态引用和运行时状态。
     */
    public record ContextSnapshot(
            AgentRequest request,
            AgentResponse response,
            AgentState state,
            AgentStatus status,
            int iteration,
            Map<String, Object> data
    ) {}
}
