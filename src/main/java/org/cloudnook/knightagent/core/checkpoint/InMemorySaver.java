package org.cloudnook.knightagent.core.checkpoint;

import org.cloudnook.knightagent.core.state.AgentState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存检查点存储
 * <p>
 * 基于 ConcurrentHashMap 的内存实现，用于：
 * <ul>
 *   <li>单元测试</li>
 *   <li>快速原型开发</li>
 *   <li>无状态服务</li>
 *   <li>演示和示例</li>
 * </ul>
 * <p>
 * 注意：
 * <ul>
 *   <li>数据存储在内存中，应用重启后丢失</li>
 *   <li>不适合生产环境</li>
 *   <li>没有持久化保证</li>
 *   <li>适合单线程或并发读场景</li>
 * </ul>
 * <p>
 * 数据结构：
 * <pre>
 * ThreadStorage {
 *     String threadId
 *     List<CheckpointEntry> checkpoints  // 按时间顺序排列
 * }
 *
 * CheckpointEntry {
 *     String checkpointId
 *     AgentState state
 *     Instant createdAt
 *     int sequence
 *     String tag
 * }
 * </pre>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public class InMemorySaver implements Checkpointer {

    private static final Logger log = LoggerFactory.getLogger(InMemorySaver.class);

    /**
     * Thread 存储
     * <p>
     * Key: threadId
     * Value: ThreadStorage
     */
    private final ConcurrentHashMap<String, ThreadStorage> threads;

    /**
     * 默认构造函数
     */
    public InMemorySaver() {
        this.threads = new ConcurrentHashMap<>();
    }

    @Override
    public String save(String threadId, AgentState state) throws CheckpointException {
        String checkpointId = generateCheckpointId();
        save(threadId, checkpointId, state);
        return checkpointId;
    }

    @Override
    public void save(String threadId, String checkpointId, AgentState state) throws CheckpointException {
        try {
            ThreadStorage storage = threads.computeIfAbsent(threadId, k -> new ThreadStorage(k));
            int sequence = storage.checkpoints.size() + 1;

            CheckpointEntry entry = new CheckpointEntry(
                    checkpointId,
                    state,
                    Instant.now(),
                    sequence
            );

            storage.checkpoints.add(entry);
            log.debug("保存检查点: thread={}, checkpoint={}, sequence={}", threadId, checkpointId, sequence);

        } catch (Exception e) {
            throw new CheckpointException("保存检查点失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<AgentState> load(String threadId, String checkpointId) throws CheckpointException {
        try {
            ThreadStorage storage = threads.get(threadId);
            if (storage == null) {
                return Optional.empty();
            }

            return storage.checkpoints.stream()
                    .filter(e -> e.checkpointId.equals(checkpointId))
                    .findFirst()
                    .map(e -> e.state);

        } catch (Exception e) {
            throw new CheckpointException("加载检查点失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<AgentState> loadLatest(String threadId) throws CheckpointException {
        try {
            ThreadStorage storage = threads.get(threadId);
            if (storage == null || storage.checkpoints.isEmpty()) {
                return Optional.empty();
            }

            // 返回最后一个检查点
            CheckpointEntry latest = storage.checkpoints.get(storage.checkpoints.size() - 1);
            return Optional.of(latest.state);

        } catch (Exception e) {
            throw new CheckpointException("加载最新检查点失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<CheckpointInfo> list(String threadId) throws CheckpointException {
        try {
            ThreadStorage storage = threads.get(threadId);
            if (storage == null || storage.checkpoints.isEmpty()) {
                return List.of();
            }

            // 按时间倒序返回（Java 17 兼容）
            List<CheckpointInfo> result = new ArrayList<>();
            for (int i = storage.checkpoints.size() - 1; i >= 0; i--) {
                CheckpointEntry e = storage.checkpoints.get(i);
                result.add(CheckpointInfo.builder()
                        .id(e.checkpointId)
                        .threadId(threadId)
                        .sequence(e.sequence)
                        .createdAt(e.createdAt)
                        .version(e.state.getVersion())
                        .build());
            }
            return result;

        } catch (Exception e) {
            throw new CheckpointException("列出检查点失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(String threadId, String checkpointId) throws CheckpointException {
        try {
            ThreadStorage storage = threads.get(threadId);
            if (storage == null) {
                return false;
            }

            boolean removed = storage.checkpoints.removeIf(e -> e.checkpointId.equals(checkpointId));
            if (removed) {
                log.debug("删除检查点: thread={}, checkpoint={}", threadId, checkpointId);
            }

            // 如果没有检查点了，删除 Thread
            if (storage.checkpoints.isEmpty()) {
                threads.remove(threadId);
            }

            return removed;

        } catch (Exception e) {
            throw new CheckpointException("删除检查点失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteThread(String threadId) throws CheckpointException {
        try {
            ThreadStorage removed = threads.remove(threadId);
            if (removed != null) {
                log.debug("删除 Thread: thread={}, checkpoints={}", threadId, removed.checkpoints.size());
                return true;
            }
            return false;

        } catch (Exception e) {
            throw new CheckpointException("删除 Thread 失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean exists(String threadId) {
        return threads.containsKey(threadId);
    }

    @Override
    public List<String> listThreads() throws CheckpointException {
        try {
            return new ArrayList<>(threads.keySet());
        } catch (Exception e) {
            throw new CheckpointException("列出 Thread 失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取 Thread 数量
     *
     * @return Thread 数量
     */
    public int getThreadCount() {
        return threads.size();
    }

    /**
     * 获取总检查点数量
     *
     * @return 所有 Thread 的检查点总数
     */
    public int getTotalCheckpointCount() {
        return threads.values().stream()
                .mapToInt(s -> s.checkpoints.size())
                .sum();
    }

    /**
     * 清空所有数据
     */
    public void clear() {
        threads.clear();
        log.debug("已清空所有检查点数据");
    }

    /**
     * 获取统计信息
     *
     * @return 统计信息字符串
     */
    public String getStats() {
        return String.format("InMemorySaver{threads=%d, checkpoints=%d}",
                threads.size(),
                getTotalCheckpointCount());
    }

    // ==================== 内部类 ====================

    /**
     * Thread 存储结构
     */
    private static class ThreadStorage {
        final String threadId;
        final List<CheckpointEntry> checkpoints;

        ThreadStorage(String threadId) {
            this.threadId = threadId;
            this.checkpoints = new ArrayList<>();
        }
    }

    /**
     * 检查点条目
     */
    private static class CheckpointEntry {
        final String checkpointId;
        final AgentState state;
        final Instant createdAt;
        final int sequence;

        CheckpointEntry(String checkpointId, AgentState state, Instant createdAt, int sequence) {
            this.checkpointId = checkpointId;
            this.state = state;
            this.createdAt = createdAt;
            this.sequence = sequence;
        }
    }
}
