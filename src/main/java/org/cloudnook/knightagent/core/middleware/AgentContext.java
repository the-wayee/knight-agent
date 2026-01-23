package org.cloudnook.knightagent.core.middleware;

import org.cloudnook.knightagent.core.agent.AgentRequest;
import org.cloudnook.knightagent.core.agent.AgentResponse;
import org.cloudnook.knightagent.core.message.ToolCall;
import org.cloudnook.knightagent.core.state.AgentState;

/**
 * 中间件上下文
 * <p>
 * 在中间件链中传递的上下文信息。
 * 包含请求、响应、状态等数据。
 *
 * @author KnightAgent
 * @since 1.0.0
 */
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
     * 自定义数据
     */
    private final java.util.Map<String, Object> data;

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
        this.data = new java.util.HashMap<>();
    }

    // Getters and Setters

    public AgentRequest getRequest() {
        return request;
    }

    public void setRequest(AgentRequest request) {
        this.request = request;
    }

    public AgentResponse getResponse() {
        return response;
    }

    public void setResponse(AgentResponse response) {
        this.response = response;
    }

    public AgentState getState() {
        return state;
    }

    public void setState(AgentState state) {
        this.state = state;
    }

    public int getIteration() {
        return iteration;
    }

    public void setIteration(int iteration) {
        this.iteration = iteration;
    }

    public boolean isStopped() {
        return stopped;
    }

    public void setStopped(boolean stopped) {
        this.stopped = stopped;
    }

    /**
     * 停止执行
     */
    public void stop() {
        this.stopped = true;
    }

    // 自定义数据操作

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
    public <T> java.util.Optional<T> get(String key) {
        return java.util.Optional.ofNullable((T) this.data.get(key));
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
    public java.util.Set<String> getKeys() {
        return this.data.keySet();
    }
}
