package org.cloudnook.knightagent.core.state;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 状态上下文
 * <p>
 * 在状态归约过程中传递额外的上下文信息。
 * 用于提供归约决策所需的额外数据，而不修改状态本身。
 * <p>
 * 常见用途：
 * <ul>
 *   <li>传递当前执行的 Agent ID</li>
 *   <li>传递当前步骤/阶段信息</li>
 *   <li>传递用户信息或会话信息</li>
 *   <li>传递临时计算结果</li>
 * </ul>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public class StateContext {

    /**
     * 空上下文
     */
    private static final StateContext EMPTY = new StateContext(new HashMap<>());

    /**
     * 上下文数据
     */
    private final Map<String, Object> data;

    /**
     * 私有构造函数
     */
    private StateContext(Map<String, Object> data) {
        this.data = Map.copyOf(data);
    }

    /**
     * 获取空上下文
     *
     * @return 空的 StateContext 实例
     */
    public static StateContext empty() {
        return EMPTY;
    }

    /**
     * 创建上下文
     *
     * @return Builder 实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 获取值
     *
     * @param key 键
     * @return 值的 Optional 包装
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key) {
        return Optional.ofNullable((T) data.get(key));
    }

    /**
     * 获取值或默认值
     *
     * @param key          键
     * @param defaultValue 默认值
     * @return 值，如果不存在返回默认值
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrDefault(String key, T defaultValue) {
        return (T) data.getOrDefault(key, defaultValue);
    }

    /**
     * 检查是否包含键
     *
     * @param key 键
     * @return 如果包含返回 true
     */
    public boolean contains(String key) {
        return data.containsKey(key);
    }

    /**
     * 获取所有键
     *
     * @return 键集合
     */
    public java.util.Set<String> getKeys() {
        return data.keySet();
    }

    /**
     * 上下文构建器
     */
    public static class Builder {
        private final Map<String, Object> data = new HashMap<>();

        /**
         * 设置值
         *
         * @param key   键
         * @param value 值
         * @return this
         */
        public Builder put(String key, Object value) {
            this.data.put(key, value);
            return this;
        }

        /**
         * 批量设置值
         *
         * @param values 值映射
         * @return this
         */
        public Builder putAll(Map<String, Object> values) {
            this.data.putAll(values);
            return this;
        }

        /**
         * 构建 StateContext
         *
         * @return StateContext 实例
         */
        public StateContext build() {
            return new StateContext(data);
        }
    }
}
