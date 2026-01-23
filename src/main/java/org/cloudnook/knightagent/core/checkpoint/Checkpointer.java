package org.cloudnook.knightagent.core.checkpoint;

import org.cloudnook.knightagent.core.state.AgentState;

import java.util.List;
import java.util.Optional;

/**
 * 检查点接口
 * <p>
 * 定义状态快照的持久化规范。
 * Checkpointer 负责保存和恢复 Agent 的执行状态，实现：
 * <ul>
 *   <li>状态持久化 - 跨会话保存状态</li>
 *   <li>时间旅行 - 回退到任意历史状态</li>
 *   <li>容错恢复 - 从检查点恢复执行</li>
 *   <li>人机协作 - 在检查点暂停等待人工干预</li>
 * </ul>
 * <p>
 * 核心概念：
 * <ul>
 *   <li><b>Thread（线程）</b> - 一个独立的对话会话，类似聊天窗口</li>
 *   <li><b>Checkpoint（检查点）</b> - Thread 内的某个状态快照</li>
 *   <li><b>CheckpointId</b> - 检查点的唯一标识</li>
 * </ul>
 * <p>
 * 使用示例：
 * <pre>{@code
 * Checkpointer checkpointer = new InMemorySaver();
 *
 * // 保存状态
 * String threadId = "conversation-123";
 * String checkpointId = checkpointer.save(threadId, state);
 *
 * // 加载最新状态
 * Optional<AgentState> latest = checkpointer.loadLatest(threadId);
 *
 * // 列出所有检查点
 * List<CheckpointInfo> checkpoints = checkpointer.list(threadId);
 *
 * // 回退到指定检查点
 * Optional<AgentState> earlier = checkpointer.load(threadId, "checkpoint-456");
 * }</pre>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public interface Checkpointer {

    /**
     * 保存状态检查点
     * <p>
     * 将当前状态保存为一个新的检查点。
     * 如果是 Thread 的第一个检查点，会自动初始化 Thread。
     *
     * @param threadId Thread 标识符，不能为 null
     * @param state    要保存的状态，不能为 null
     * @return 生成的检查点 ID，用于后续加载
     * @throws CheckpointException 保存失败
     */
    String save(String threadId, AgentState state) throws CheckpointException;

    /**
     * 保存状态检查点（指定 ID）
     * <p>
     * 使用指定的 ID 保存检查点。
     * 如果该 ID 已存在，会覆盖原有的检查点。
     *
     * @param threadId    Thread 标识符
     * @param checkpointId 检查点 ID
     * @param state       要保存的状态
     * @throws CheckpointException 保存失败
     */
    void save(String threadId, String checkpointId, AgentState state) throws CheckpointException;

    /**
     * 加载指定检查点
     *
     * @param threadId    Thread 标识符
     * @param checkpointId 检查点 ID
     * @return 状态的 Optional 包装，如果不存在返回空
     * @throws CheckpointException 加载失败
     */
    Optional<AgentState> load(String threadId, String checkpointId) throws CheckpointException;

    /**
     * 加载最新的检查点
     * <p>
     * 返回指定 Thread 中最近保存的检查点。
     *
     * @param threadId Thread 标识符
     * @return 最新状态的 Optional 包装，如果 Thread 不存在返回空
     * @throws CheckpointException 加载失败
     */
    Optional<AgentState> loadLatest(String threadId) throws CheckpointException;

    /**
     * 列出 Thread 的所有检查点
     * <p>
     * 返回按时间倒序排列的检查点列表（最新的在前）。
     *
     * @param threadId Thread 标识符
     * @return 检查点信息列表，如果 Thread 不存在返回空列表
     * @throws CheckpointException 查询失败
     */
    List<CheckpointInfo> list(String threadId) throws CheckpointException;

    /**
     * 删除指定检查点
     *
     * @param threadId    Thread 标识符
     * @param checkpointId 检查点 ID
     * @return 如果删除成功返回 true，如果检查点不存在返回 false
     * @throws CheckpointException 删除失败
     */
    boolean delete(String threadId, String checkpointId) throws CheckpointException;

    /**
     * 删除整个 Thread
     * <p>
     * 删除 Thread 及其所有检查点。
     *
     * @param threadId Thread 标识符
     * @return 如果删除成功返回 true，如果 Thread 不存在返回 false
     * @throws CheckpointException 删除失败
     */
    boolean deleteThread(String threadId) throws CheckpointException;

    /**
     * 检查 Thread 是否存在
     *
     * @param threadId Thread 标识符
     * @return 如果 Thread 存在返回 true
     */
    boolean exists(String threadId);

    /**
     * 获取所有 Thread ID
     *
     * @return Thread ID 列表
     * @throws CheckpointException 查询失败
     */
    List<String> listThreads() throws CheckpointException;

    /**
     * 生成新的检查点 ID
     * <p>
     * 实现类可以自定义 ID 生成策略。
     *
     * @return 新的检查点 ID
     */
    default String generateCheckpointId() {
        return "chk_" + System.currentTimeMillis() + "_" + Integer.toHexString((int) (Math.random() * 0xFFFF));
    }

    /**
     * 获取检查点数量
     *
     * @param threadId Thread 标识符
     * @return 检查点数量
     * @throws CheckpointException 查询失败
     */
    default int getCheckpointCount(String threadId) throws CheckpointException {
        return list(threadId).size();
    }

    /**
     * 清理旧检查点
     * <p>
     * 保留最新的 N 个检查点，删除其余的。
     *
     * @param threadId    Thread 标识符
     * @param keepCount  保留数量
     * @return 被删除的检查点数量
     * @throws CheckpointException 清理失败
     */
    default int cleanup(String threadId, int keepCount) throws CheckpointException {
        List<CheckpointInfo> checkpoints = list(threadId);
        if (checkpoints.size() <= keepCount) {
            return 0;
        }

        int deletedCount = 0;
        for (int i = keepCount; i < checkpoints.size(); i++) {
            CheckpointInfo info = checkpoints.get(i);
            if (delete(threadId, info.getId())) {
                deletedCount++;
            }
        }
        return deletedCount;
    }
}
