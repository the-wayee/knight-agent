package org.cloudnook.knightagent.core.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cloudnook.knightagent.core.message.Message;

import java.time.Instant;
import java.util.*;

/**
 * Agent 状态
 * <p>
 * 管理 Agent 执行过程中的所有状态数据。
 * 状态是 Agent 与 LLM 交互的"记忆"，包括：
 * <ul>
 *   <li>消息历史 - 对话记录</li>
 *   <li>自定义数据 - 用户扩展字段</li>
 *   <li>元数据 - 时间戳、计数等</li>
 * </ul>
 * <p>
 * 状态设计原则：
 * <ul>
 *   <li>不可变性 - 每次更新返回新的状态实例</li>
 *   <li>可序列化 - 支持持久化存储</li>
 *   <li>类型安全 - 支持自定义状态 Schema</li>
 * </ul>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public class AgentState {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 消息历史
     * <p>
     * 按时间顺序存储所有对话消息。
     */
    private final List<Message> messages;

    /**
     * 自定义状态数据
     * <p>
     * 用于存储 Agent 特定的状态信息。
     * 例如：用户信息、上下文变量、中间计算结果等。
     */
    private final Map<String, Object> data;

    /**
     * 创建时间
     */
    private final Instant createdAt;

    /**
     * 最后更新时间
     */
    private final Instant updatedAt;

    /**
     * 状态版本号
     * <p>
     * 每次更新递增，用于检测并发修改。
     */
    private final long version;

    /**
     * 私有构造函数
     * <p>
     * 使用 Builder 创建实例。
     */
    private AgentState(Builder builder) {
        this.messages = builder.messages != null ? List.copyOf(builder.messages) : List.of();
        this.data = builder.data != null ? Map.copyOf(builder.data) : Map.of();
        this.createdAt = builder.createdAt != null ? builder.createdAt : Instant.now();
        this.updatedAt = builder.updatedAt != null ? builder.updatedAt : Instant.now();
        this.version = builder.version;
    }

    /**
     * 创建初始状态
     *
     * @return 空的 AgentState 实例
     */
    public static AgentState initial() {
        return new Builder().build();
    }

    /**
     * 创建带初始消息的状态
     *
     * @param messages 初始消息列表
     * @return AgentState 实例
     */
    public static AgentState of(List<Message> messages) {
        return new Builder().messages(messages).build();
    }

    // ==================== Getters ====================

    /**
     * 获取消息历史（不可变视图）
     */
    public List<Message> getMessages() {
        return messages;
    }

    /**
     * 获取自定义数据（不可变视图）
     */
    public Map<String, Object> getData() {
        return data;
    }

    /**
     * 获取创建时间
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * 获取最后更新时间
     */
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 获取版本号
     */
    public long getVersion() {
        return version;
    }

    // ==================== 状态操作 ====================

    /**
     * 添加消息
     *
     * @param message 要添加的消息
     * @return 新的状态实例
     */
    public AgentState addMessage(Message message) {
        return new Builder(this)
                .addMessage(message)
                .updatedAt(Instant.now())
                .version(version + 1)
                .build();
    }

    /**
     * 添加多条消息
     *
     * @param newMessages 要添加的消息列表
     * @return 新的状态实例
     */
    public AgentState addMessages(List<Message> newMessages) {
        return new Builder(this)
                .addAllMessages(newMessages)
                .updatedAt(Instant.now())
                .version(version + 1)
                .build();
    }

    /**
     * 设置数据字段
     *
     * @param key   键
     * @param value 值
     * @return 新的状态实例
     */
    public AgentState put(String key, Object value) {
        Map<String, Object> newData = new HashMap<>(this.data);
        newData.put(key, value);
        return new Builder(this)
                .data(newData)
                .updatedAt(Instant.now())
                .version(version + 1)
                .build();
    }

    /**
     * 批量设置数据
     *
     * @param newData 要设置的数据
     * @return 新的状态实例
     */
    public AgentState putAll(Map<String, Object> newData) {
        Map<String, Object> combinedData = new HashMap<>(this.data);
        combinedData.putAll(newData);
        return new Builder(this)
                .data(combinedData)
                .updatedAt(Instant.now())
                .version(version + 1)
                .build();
    }

    /**
     * 移除数据字段
     *
     * @param key 要移除的键
     * @return 新的状态实例
     */
    public AgentState remove(String key) {
        if (!data.containsKey(key)) {
            return this;
        }
        Map<String, Object> newData = new HashMap<>(this.data);
        newData.remove(key);
        return new Builder(this)
                .data(newData)
                .updatedAt(Instant.now())
                .version(version + 1)
                .build();
    }

    /**
     * 获取数据字段
     *
     * @param key 键
     * @return 值，如果不存在返回 null
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) data.get(key);
    }

    /**
     * 获取数据字段（带默认值）
     *
     * @param key          键
     * @param defaultValue 默认值
     * @return 值，如果不存在返回默认值
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        Object value = data.get(key);
        return value != null ? (T) value : defaultValue;
    }

    /**
     * 检查是否包含数据字段
     *
     * @param key 键
     * @return 如果包含返回 true
     */
    public boolean contains(String key) {
        return data.containsKey(key);
    }

    // ==================== 序列化 ====================

    /**
     * 将状态序列化为 JSON 字节数组
     * <p>
     * 用于持久化存储。
     *
     * @return JSON 字节数组
     * @throws StateSerializationException 序列化失败
     */
    public byte[] toBytes() throws StateSerializationException {
        try {
            Map<String, Object> stateMap = new HashMap<>();
            stateMap.put("messages", messages);
            stateMap.put("data", data);
            stateMap.put("createdAt", createdAt.toString());
            stateMap.put("updatedAt", updatedAt.toString());
            stateMap.put("version", version);
            return objectMapper.writeValueAsBytes(stateMap);
        } catch (Exception e) {
            throw new StateSerializationException("状态序列化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从 JSON 字节数组反序列化状态
     *
     * @param bytes JSON 字节数组
     * @return AgentState 实例
     * @throws StateSerializationException 反序列化失败
     */
    @SuppressWarnings("unchecked")
    public static AgentState fromBytes(byte[] bytes) throws StateSerializationException {
        try {
            Map<String, Object> stateMap = objectMapper.readValue(bytes, Map.class);

            Builder builder = new Builder()
                    .createdAt(Instant.parse((String) stateMap.get("createdAt")))
                    .updatedAt(Instant.parse((String) stateMap.get("updatedAt")))
                    .version(((Number) stateMap.getOrDefault("version", 0)).longValue());

            // 恢复消息列表（简化处理，实际需要根据类型反序列化）
            if (stateMap.containsKey("messages")) {
                // TODO: 实现消息的反序列化
            }

            // 恢复数据
            if (stateMap.containsKey("data")) {
                builder.data((Map<String, Object>) stateMap.get("data"));
            }

            return builder.build();
        } catch (Exception e) {
            throw new StateSerializationException("状态反序列化失败: " + e.getMessage(), e);
        }
    }

    // ==================== Builder ====================

    /**
     * 状态构建器
     */
    public static class Builder {
        private List<Message> messages = new ArrayList<>();
        private Map<String, Object> data = new HashMap<>();
        private Instant createdAt;
        private Instant updatedAt;
        private long version = 0;

        public Builder() {
        }

        /**
         * 基于现有状态创建 Builder
         *
         * @param state 现有状态
         */
        public Builder(AgentState state) {
            this.messages = new ArrayList<>(state.messages);
            this.data = new HashMap<>(state.data);
            this.createdAt = state.createdAt;
            this.updatedAt = state.updatedAt;
            this.version = state.version;
        }

        public Builder messages(List<Message> messages) {
            this.messages = messages != null ? new ArrayList<>(messages) : new ArrayList<>();
            return this;
        }

        public Builder addMessage(Message message) {
            this.messages.add(message);
            return this;
        }

        public Builder addAllMessages(List<Message> messages) {
            if (messages != null) {
                this.messages.addAll(messages);
            }
            return this;
        }

        public Builder data(Map<String, Object> data) {
            this.data = data != null ? new HashMap<>(data) : new HashMap<>();
            return this;
        }

        public Builder put(String key, Object value) {
            this.data.put(key, value);
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder version(long version) {
            this.version = version;
            return this;
        }

        public AgentState build() {
            if (this.createdAt == null) {
                this.createdAt = Instant.now();
            }
            if (this.updatedAt == null) {
                this.updatedAt = Instant.now();
            }
            return new AgentState(this);
        }
    }

    @Override
    public String toString() {
        return "AgentState{" +
                "messageCount=" + messages.size() +
                ", dataKeys=" + data.keySet() +
                ", version=" + version +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
