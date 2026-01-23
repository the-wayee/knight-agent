package org.cloudnook.knightagent.core.checkpoint;

import org.cloudnook.knightagent.core.state.AgentState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * PostgreSQL 检查点存储
 * <p>
 * 基于 PostgreSQL + JSONB 的生产级持久化实现。
 * <p>
 * 特性：
 * <ul>
 *   <li>持久化存储 - 数据库持久化，重启不丢失</li>
 *   <li>事务支持 - 保证数据一致性</li>
 *   <li>JSONB 存储 - 灵活的状态结构，支持索引查询</li>
 *   <li>并发安全 - 数据库级别的并发控制</li>
 * </ul>
 * <p>
 * 表结构：
 * <pre>
 * CREATE TABLE agent_threads (
 *     thread_id VARCHAR(255) PRIMARY KEY,
 *     created_at TIMESTAMP NOT NULL DEFAULT NOW(),
 *     updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
 *     metadata JSONB DEFAULT '{}'::jsonb
 * );
 *
 * CREATE TABLE agent_checkpoints (
 *     checkpoint_id VARCHAR(255) NOT NULL,
 *     thread_id VARCHAR(255) NOT NULL,
 *     sequence INT NOT NULL,
 *     state_data JSONB NOT NULL,
 *     version BIGINT NOT NULL,
 *     tag VARCHAR(100),
 *     created_at TIMESTAMP NOT NULL DEFAULT NOW(),
 *     PRIMARY KEY (thread_id, checkpoint_id),
 *     FOREIGN KEY (thread_id) REFERENCES agent_threads(thread_id) ON DELETE CASCADE
 * );
 *
 * CREATE INDEX idx_checkpoints_thread_sequence ON agent_checkpoints(thread_id, sequence DESC);
 * CREATE INDEX idx_checkpoints_created_at ON agent_checkpoints(created_at DESC);
 * </pre>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public class PostgresSaver implements Checkpointer {

    private static final Logger log = LoggerFactory.getLogger(PostgresSaver.class);

    private final DataSource dataSource;

    /**
     * 构造函数
     *
     * @param dataSource 数据源
     */
    public PostgresSaver(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public String save(String threadId, AgentState state) throws CheckpointException {
        String checkpointId = generateCheckpointId();
        save(threadId, checkpointId, state);
        return checkpointId;
    }

    @Override
    public void save(String threadId, String checkpointId, AgentState state) throws CheckpointException {
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);

            // 确保 Thread 存在
            ensureThreadExists(conn, threadId);

            // 获取下一个序号
            int sequence = getNextSequence(conn, threadId);

            // 序列化状态
            byte[] stateBytes = state.toBytes();
            String stateJson = new String(stateBytes);

            // 插入检查点
            String sql = """
                    INSERT INTO agent_checkpoints (checkpoint_id, thread_id, sequence, state_data, version, created_at)
                    VALUES (?, ?, ?, ?::jsonb, ?, NOW())
                    ON CONFLICT (thread_id, checkpoint_id)
                    DO UPDATE SET
                        state_data = EXCLUDED.state_data,
                        version = EXCLUDED.version,
                        created_at = EXCLUDED.created_at
                    """;

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, checkpointId);
                ps.setString(2, threadId);
                ps.setInt(3, sequence);
                ps.setString(4, stateJson);
                ps.setLong(5, state.getVersion());

                int updated = ps.executeUpdate();

                if (updated <= 0) {
                    throw new CheckpointException("插入检查点失败");
                }
            }

            // 更新 Thread 的 updated_at
            updateThreadTimestamp(conn, threadId);

            conn.commit();
            log.debug("保存检查点: thread={}, checkpoint={}, sequence={}, version={}",
                    threadId, checkpointId, sequence, state.getVersion());

        } catch (CheckpointException e) {
            rollbackQuietly(conn);
            throw e;
        } catch (Exception e) {
            rollbackQuietly(conn);
            throw new CheckpointException("保存检查点失败: " + e.getMessage(), e);
        } finally {
            closeQuietly(conn);
        }
    }

    @Override
    public Optional<AgentState> load(String threadId, String checkpointId) throws CheckpointException {
        Connection conn = null;
        try {
            conn = dataSource.getConnection();

            String sql = """
                    SELECT state_data
                    FROM agent_checkpoints
                    WHERE thread_id = ? AND checkpoint_id = ?
                    """;

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, threadId);
                ps.setString(2, checkpointId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String stateJson = rs.getString("state_data");
                        AgentState state = AgentState.fromBytes(stateJson.getBytes());
                        return Optional.of(state);
                    }
                }
            }

            return Optional.empty();

        } catch (Exception e) {
            throw new CheckpointException("加载检查点失败: " + e.getMessage(), e);
        } finally {
            closeQuietly(conn);
        }
    }

    @Override
    public Optional<AgentState> loadLatest(String threadId) throws CheckpointException {
        Connection conn = null;
        try {
            conn = dataSource.getConnection();

            String sql = """
                    SELECT state_data
                    FROM agent_checkpoints
                    WHERE thread_id = ?
                    ORDER BY sequence DESC
                    LIMIT 1
                    """;

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, threadId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String stateJson = rs.getString("state_data");
                        AgentState state = AgentState.fromBytes(stateJson.getBytes());
                        return Optional.of(state);
                    }
                }
            }

            return Optional.empty();

        } catch (Exception e) {
            throw new CheckpointException("加载最新检查点失败: " + e.getMessage(), e);
        } finally {
            closeQuietly(conn);
        }
    }

    @Override
    public List<CheckpointInfo> list(String threadId) throws CheckpointException {
        Connection conn = null;
        try {
            conn = dataSource.getConnection();

            String sql = """
                    SELECT checkpoint_id, sequence, version, tag, created_at
                    FROM agent_checkpoints
                    WHERE thread_id = ?
                    ORDER BY sequence DESC
                    """;

            List<CheckpointInfo> result = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, threadId);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        result.add(CheckpointInfo.builder()
                                .id(rs.getString("checkpoint_id"))
                                .threadId(threadId)
                                .sequence(rs.getInt("sequence"))
                                .version(rs.getLong("version"))
                                .tag(rs.getString("tag"))
                                .createdAt(rs.getTimestamp("created_at").toInstant())
                                .build());
                    }
                }
            }

            return result;

        } catch (Exception e) {
            throw new CheckpointException("列出检查点失败: " + e.getMessage(), e);
        } finally {
            closeQuietly(conn);
        }
    }

    @Override
    public boolean delete(String threadId, String checkpointId) throws CheckpointException {
        Connection conn = null;
        try {
            conn = dataSource.getConnection();

            String sql = "DELETE FROM agent_checkpoints WHERE thread_id = ? AND checkpoint_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, threadId);
                ps.setString(2, checkpointId);

                int deleted = ps.executeUpdate();

                if (deleted > 0) {
                    log.debug("删除检查点: thread={}, checkpoint={}", threadId, checkpointId);

                    // 检查是否还有检查点
                    long count = getCheckpointCount(conn, threadId);
                    if (count == 0) {
                        deleteThread(threadId);
                    }

                    return true;
                }

                return false;
            }

        } catch (Exception e) {
            throw new CheckpointException("删除检查点失败: " + e.getMessage(), e);
        } finally {
            closeQuietly(conn);
        }
    }

    @Override
    public boolean deleteThread(String threadId) throws CheckpointException {
        Connection conn = null;
        try {
            conn = dataSource.getConnection();

            try {
                // 先删除检查点
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM agent_checkpoints WHERE thread_id = ?")) {
                    ps.setString(1, threadId);
                    ps.executeUpdate();
                }

                // 删除 Thread
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM agent_threads WHERE thread_id = ?")) {
                    ps.setString(1, threadId);
                    int deleted = ps.executeUpdate();

                    if (deleted > 0) {
                        log.debug("删除 Thread: thread={}", threadId);
                        return true;
                    }
                }

            } finally {
                conn.commit();
            }

            return false;

        } catch (Exception e) {
            throw new CheckpointException("删除 Thread 失败: " + e.getMessage(), e);
        } finally {
            closeQuietly(conn);
        }
    }

    @Override
    public boolean exists(String threadId) {
        Connection conn = null;
        try {
            conn = dataSource.getConnection();

            String sql = "SELECT COUNT(*) FROM agent_threads WHERE thread_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, threadId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1) > 0;
                    }
                }
            }

            return false;

        } catch (Exception e) {
            return false;
        } finally {
            closeQuietly(conn);
        }
    }

    @Override
    public List<String> listThreads() throws CheckpointException {
        Connection conn = null;
        try {
            conn = dataSource.getConnection();

            List<String> result = new ArrayList<>();
            String sql = "SELECT thread_id FROM agent_threads ORDER BY created_at DESC";

            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getString("thread_id"));
                }
            }

            return result;

        } catch (Exception e) {
            throw new CheckpointException("列出 Thread 失败: " + e.getMessage(), e);
        } finally {
            closeQuietly(conn);
        }
    }

    // ==================== 私有辅助方法 ====================

    private void ensureThreadExists(Connection conn, String threadId) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM agent_threads WHERE thread_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setString(1, threadId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 0) {
                    String insertSql = """
                            INSERT INTO agent_threads (thread_id, created_at, updated_at)
                            VALUES (?, NOW(), NOW())
                            """;
                    try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
                        insertPs.setString(1, threadId);
                        insertPs.executeUpdate();
                        log.debug("创建新 Thread: {}", threadId);
                    }
                }
            }
        }
    }

    private int getNextSequence(Connection conn, String threadId) throws SQLException {
        String sql = "SELECT COALESCE(MAX(sequence), 0) FROM agent_checkpoints WHERE thread_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, threadId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) + 1;
                }
            }
        }
        return 1;
    }

    private void updateThreadTimestamp(Connection conn, String threadId) throws SQLException {
        String sql = "UPDATE agent_threads SET updated_at = NOW() WHERE thread_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, threadId);
            ps.executeUpdate();
        }
    }

    private long getCheckpointCount(Connection conn, String threadId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM agent_checkpoints WHERE thread_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, threadId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return 0;
    }

    private void rollbackQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException e) {
                // 忽略
            }
        }
    }

    private void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                // 忽略
            }
        }
    }

    // ==================== 初始化表结构 ====================

    /**
     * 初始化数据库表
     *
     * @throws CheckpointException 初始化失败
     */
    public void initializeTables() throws CheckpointException {
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            Statement stmt = conn.createStatement();

            // 创建 Thread 表
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS agent_threads (
                        thread_id VARCHAR(255) PRIMARY KEY,
                        created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                        updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
                        metadata JSONB DEFAULT '{}'::jsonb
                    )
                    """);

            // 创建 Checkpoint 表
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS agent_checkpoints (
                        checkpoint_id VARCHAR(255) NOT NULL,
                        thread_id VARCHAR(255) NOT NULL,
                        sequence INT NOT NULL,
                        state_data JSONB NOT NULL,
                        version BIGINT NOT NULL,
                        tag VARCHAR(100),
                        created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                        PRIMARY KEY (thread_id, checkpoint_id),
                        FOREIGN KEY (thread_id) REFERENCES agent_threads(thread_id) ON DELETE CASCADE
                    )
                    """);

            // 创建索引
            stmt.execute("""
                    CREATE INDEX IF NOT EXISTS idx_checkpoints_thread_sequence
                    ON agent_checkpoints(thread_id, sequence DESC)
                    """);

            stmt.execute("""
                    CREATE INDEX IF NOT EXISTS idx_checkpoints_created_at
                    ON agent_checkpoints(created_at DESC)
                    """);

            log.info("PostgreSQL 检查点表初始化完成");

        } catch (Exception e) {
            throw new CheckpointException("初始化表结构失败: " + e.getMessage(), e);
        } finally {
            closeQuietly(conn);
        }
    }
}
