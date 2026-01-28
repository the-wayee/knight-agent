package org.cloudnook.knightagent.core.middleware;

import lombok.Data;
import lombok.Getter;
import org.cloudnook.knightagent.core.agent.AgentRequest;
import org.cloudnook.knightagent.core.agent.AgentResponse;
import org.cloudnook.knightagent.core.agent.ApprovalRequest;
import org.cloudnook.knightagent.core.state.AgentState;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 中间件上下文
 * <p>
 * 在中间件链中传递的上下文信息。
 * 包含请求、响应、状态等数据。
 * <p>
 * 支持快照功能，可以创建不可变的时间点副本。
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
     * 当前状态
     */
    private AgentState state;

    /**
     * 当前迭代次数
     */
    private int iteration;

    /**
     * 是否已停止
     */
    private boolean stopped;

    /**
     * 待审批的请求
     * <p>
     * 当 HumanInTheLoopMiddleware 检测到需要审批的工具时，
     * 会设置此字段，ReActStrategy 检测到此字段后会保存 checkpoint 并返回。
     */
    private ApprovalRequest pendingApproval;

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

    /**
     * 停止执行
     */
    public void stop() {
        this.stopped = true;
    }

    /**
     * 是否有待审批的请求
     */
    public boolean hasPendingApproval() {
        return pendingApproval != null;
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
                this.iteration,
                this.stopped,
                this.pendingApproval,
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
        this.iteration = snapshot.iteration();
        this.stopped = snapshot.stopped();
        this.pendingApproval = snapshot.pendingApproval();
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
     */
    public record ContextSnapshot(
            AgentRequest request,
            AgentResponse response,
            AgentState state,
            int iteration,
            boolean stopped,
            ApprovalRequest pendingApproval,
            Map<String, Object> data
    ) {}
}
