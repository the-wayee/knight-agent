package org.cloudnook.knightagent.core.checkpoint;

import java.time.Instant;

/**
 * 检查点信息
 * <p>
 * 描述一个检查点的元数据，不包含完整的状态数据。
 * 用于列出检查点时提供摘要信息。
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public class CheckpointInfo {

    /**
     * 检查点 ID
     */
    private final String id;

    /**
     * Thread ID
     */
    private final String threadId;

    /**
     * 创建时间
     */
    private final Instant createdAt;

    /**
     * 状态版本号
     */
    private final long version;

    /**
     * 检查点标签（可选）
     * <p>
     * 用于标记重要的检查点，如 "before_tool_call"。
     */
    private final String tag;

    /**
     * 检查点序号
     * <p>
     * 在 Thread 中的位置，从 1 开始。
     */
    private final int sequence;

    /**
     * 附加元数据
     */
    private final java.util.Map<String, String> metadata;

    private CheckpointInfo(Builder builder) {
        this.id = builder.id;
        this.threadId = builder.threadId;
        this.createdAt = builder.createdAt != null ? builder.createdAt : Instant.now();
        this.version = builder.version;
        this.tag = builder.tag;
        this.sequence = builder.sequence;
        this.metadata = builder.metadata != null ? java.util.Map.copyOf(builder.metadata) : java.util.Map.of();
    }

    /**
     * 创建 Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 创建简化的检查点信息
     *
     * @param id        检查点 ID
     * @param threadId  Thread ID
     * @param sequence  序号
     * @return CheckpointInfo 实例
     */
    public static CheckpointInfo of(String id, String threadId, int sequence) {
        return builder()
                .id(id)
                .threadId(threadId)
                .sequence(sequence)
                .build();
    }

    // Getters

    public String getId() {
        return id;
    }

    public String getThreadId() {
        return threadId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public long getVersion() {
        return version;
    }

    public String getTag() {
        return tag;
    }

    public int getSequence() {
        return sequence;
    }

    public java.util.Map<String, String> getMetadata() {
        return metadata;
    }

    /**
     * 获取元数据值
     *
     * @param key 键
     * @return 值，如果不存在返回 null
     */
    public String getMetadata(String key) {
        return metadata.get(key);
    }

    /**
     * 检查是否有指定标签
     *
     * @param tag 标签
     * @return 如果有该标签返回 true
     */
    public boolean hasTag(String tag) {
        return tag != null && tag.equals(this.tag);
    }

    /**
     * 是否为最新检查点
     * <p>
     * 简单判断：如果序号为 -1，表示是最新。
     */
    public boolean isLatest() {
        return sequence == -1;
    }

    @Override
    public String toString() {
        return "CheckpointInfo{" +
                "id='" + id + '\'' +
                ", threadId='" + threadId + '\'' +
                ", sequence=" + sequence +
                ", version=" + version +
                ", createdAt=" + createdAt +
                (tag != null ? ", tag='" + tag + '\'' : "") +
                '}';
    }

    /**
     * 检查点信息构建器
     */
    public static class Builder {
        private String id;
        private String threadId;
        private Instant createdAt;
        private long version;
        private String tag;
        private int sequence;
        private java.util.Map<String, String> metadata = new java.util.HashMap<>();

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder threadId(String threadId) {
            this.threadId = threadId;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder version(long version) {
            this.version = version;
            return this;
        }

        public Builder tag(String tag) {
            this.tag = tag;
            return this;
        }

        public Builder sequence(int sequence) {
            this.sequence = sequence;
            return this;
        }

        public Builder metadata(java.util.Map<String, String> metadata) {
            this.metadata = metadata != null ? new java.util.HashMap<>(metadata) : new java.util.HashMap<>();
            return this;
        }

        public Builder putMetadata(String key, String value) {
            this.metadata.put(key, value);
            return this;
        }

        public CheckpointInfo build() {
            return new CheckpointInfo(this);
        }
    }
}
