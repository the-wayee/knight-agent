package org.cloudnook.knightagent.core.tool;

import org.cloudnook.knightagent.core.message.ToolCall;
import org.cloudnook.knightagent.core.message.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 工具执行器
 * <p>
 * 负责管理和执行工具调用。
 * 提供工具注册、调用、并发控制等功能。
 * <p>
 * 使用示例：
 * <pre>{@code
 * // 创建工具执行器
 * ToolInvoker invoker = new ToolInvoker();
 *
 * // 注册工具
 * invoker.register(new WeatherTool());
 * invoker.register(new SearchTool());
 *
 * // 执行工具调用
 * ToolCall call = ToolCall.of("get_weather", "{\"city\": \"北京\"}");
 * ToolResult result = invoker.invoke(call);
 *
 * if (result.isError()) {
 *     System.err.println("工具调用失败: " + result.getErrorMessage());
 * } else {
 *     System.out.println("工具返回: " + result.getResult());
 * }
 * }</pre>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public class ToolInvoker {

    private static final Logger log = LoggerFactory.getLogger(ToolInvoker.class);

    /**
     * 已注册的工具映射
     * <p>
     * Key: 工具名称
     * Value: 工具实例
     */
    private final Map<String, Tool> tools;

    /**
     * 默认构造函数
     */
    public ToolInvoker() {
        this.tools = new ConcurrentHashMap<>();
    }

    /**
     * 构造函数（初始化工具集合）
     *
     * @param tools 初始工具集合
     */
    public ToolInvoker(Collection<Tool> tools) {
        this();
        if (tools != null) {
            tools.forEach(this::register);
        }
    }

    /**
     * 注册工具
     * <p>
     * 如果已存在同名工具，将被覆盖。
     *
     * @param tool 要注册的工具
     * @return this，支持链式调用
     */
    public ToolInvoker register(Tool tool) {
        if (tool == null) {
            throw new IllegalArgumentException("工具不能为 null");
        }
        String name = tool.getName();
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("工具名称不能为空");
        }
        tools.put(name, tool);
        log.debug("注册工具: {} ({})", name, tool.getClass().getSimpleName());
        return this;
    }

    /**
     * 批量注册工具
     *
     * @param tools 工具集合
     * @return this，支持链式调用
     */
    public ToolInvoker registerAll(Collection<Tool> tools) {
        if (tools != null) {
            tools.forEach(this::register);
        }
        return this;
    }

    /**
     * 注销工具
     *
     * @param toolName 工具名称
     * @return 被移除的工具，如果不存在返回 null
     */
    public Tool unregister(String toolName) {
        Tool removed = tools.remove(toolName);
        if (removed != null) {
            log.debug("注销工具: {}", toolName);
        }
        return removed;
    }

    /**
     * 检查工具是否存在
     *
     * @param toolName 工具名称
     * @return 如果工具已注册返回 true
     */
    public boolean hasTool(String toolName) {
        return tools.containsKey(toolName);
    }

    /**
     * 获取已注册的工具名称集合
     *
     * @return 工具名称集合的不可修改视图
     */
    public Set<String> getToolNames() {
        return Set.copyOf(tools.keySet());
    }

    /**
     * 获取已注册的工具数量
     *
     * @return 工具数量
     */
    public int getToolCount() {
        return tools.size();
    }

    /**
     * 执行工具调用
     * <p>
     * 根据工具调用信息查找对应工具并执行。
     * <p>
     * 注意：此方法是同步的，长时间运行的工具会阻塞调用线程。
     * 如需异步执行，请使用 {@link #invokeAsync(ToolCall)}。
     *
     * @param toolCall 工具调用信息
     * @return 执行结果
     * @throws ToolExecutionException 如果工具不存在或执行失败
     */
    public ToolResult invoke(ToolCall toolCall) throws ToolExecutionException {
        if (toolCall == null) {
            throw new IllegalArgumentException("工具调用不能为 null");
        }

        String toolName = toolCall.getName();
        Tool tool = tools.get(toolName);

        if (tool == null) {
            log.warn("工具不存在: {}", toolName);
            return ToolResult.error(
                    toolCall.getId(),
                    "工具 '" + toolName + "' 不存在"
            );
        }

        log.debug("执行工具: {} ({})", toolName, toolCall.getId());

        long startTime = System.nanoTime();
        try {
            ToolResult result = tool.execute(toolCall.getArguments());
            // 确保结果中的 toolCallId 与调用匹配
            if (result != null && result.getToolCallId() == null) {
                // 如果工具没有设置 ID，使用调用中的 ID
                return new ToolResult(
                        toolCall.getId(),
                        result.getResult(),
                        result.isError(),
                        result.getErrorMessage()
                );
            }
            return result;

        } catch (ToolExecutionException e) {
            log.error("工具执行失败: {} - {}", toolName, e.getMessage());
            return ToolResult.error(toolCall.getId(), e.getMessage());

        } catch (Exception e) {
            log.error("工具执行异常: {} - {}", toolName, e.getMessage(), e);
            return ToolResult.error(
                    toolCall.getId(),
                    "工具执行异常: " + e.getMessage()
            );

        } finally {
            long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            log.debug("工具执行完成: {} (耗时: {}ms)", toolName, duration);
        }
    }

    /**
     * 异步执行工具调用
     * <p>
     * 在新的线程中执行工具，立即返回 Future。
     *
     * @param toolCall 工具调用信息
     * @return Future，可用于获取异步执行结果
     */
    public java.util.concurrent.Future<ToolResult> invokeAsync(ToolCall toolCall) {
        return java.util.concurrent.Executors.newSingleThreadExecutor().submit(() -> invoke(toolCall));
    }

    /**
     * 批量执行工具调用
     * <p>
     * 按顺序执行多个工具调用，返回结果列表。
     *
     * @param toolCalls 工具调用列表
     * @return 执行结果列表（与输入顺序一致）
     */
    public java.util.List<ToolResult> invokeAll(java.util.List<ToolCall> toolCalls) {
        if (toolCalls == null || toolCalls.isEmpty()) {
            return java.util.List.of();
        }

        return toolCalls.stream()
                .map(call -> {
                    try {
                        return invoke(call);
                    } catch (ToolExecutionException e) {
                        return ToolResult.error(call.getId(), e.getMessage());
                    }
                })
                .toList();
    }

    /**
     * 清空所有已注册的工具
     */
    public void clear() {
        tools.clear();
        log.debug("已清空所有工具");
    }

    @Override
    public String toString() {
        return "ToolInvoker{" +
                "toolCount=" + tools.size() +
                ", tools=" + tools.keySet() +
                '}';
    }
}
