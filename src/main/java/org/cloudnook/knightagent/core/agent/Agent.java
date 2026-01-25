package org.cloudnook.knightagent.core.agent;

import org.cloudnook.knightagent.core.message.AIMessage;
import org.cloudnook.knightagent.core.message.Message;
import org.cloudnook.knightagent.core.streaming.StreamCallback;

import java.util.List;

/**
 * Agent 接口
 * <p>
 * 定义 Agent 的核心行为，作为框架的主要入口点。
 * Agent 负责协调 LLM 调用、工具执行和状态管理。
 * <p>
 * 核心方法：
 * <ul>
 *   <li>{@link #invoke(AgentRequest)} - 同步执行，返回完整响应</li>
 *   <li>{@link #stream(AgentRequest, StreamCallback)} - 流式执行，实时返回结果</li>
 *   <li>{@link #batch(List)} - 批量执行多个请求</li>
 * </ul>
 * <p>
 * 使用示例：
 * <pre>{@code
 * Agent agent = agentFactory.createAgent()
 *     .model(chatModel)
 *     .tools(List.of(weatherTool, searchTool))
 *     .checkpointer(checkpointer)
 *     .build();
 *
 * // 同步调用
 * AgentResponse response = agent.invoke(AgentRequest.of("今天北京天气怎么样？"));
 * System.out.println(response.getFinalMessage().getContent());
 *
 * // 流式调用
 * agent.stream(AgentRequest.of("讲个笑话"), new StreamCallbackAdapter() {
 *     @Override
 *     public void onToken(String token) {
 *         System.out.print(token);
 *     }
 * });
 * }</pre>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public interface Agent {

    /**
     * 同步执行 Agent
     * <p>
     * 发送请求到 Agent，等待完整响应后返回。
     * <p>
     * 执行流程：
     * <ol>
     *   <li>加载指定 Thread 的历史状态（如果有配置 checkpointer）</li>
     *   <li>通过中间件链处理输入</li>
     *   <li>调用 LLM 获取响应</li>
     *   <li>如果需要，执行工具调用</li>
     *   <li>更新状态并保存检查点</li>
     *   <li>通过中间件链处理输出</li>
     *   <li>返回最终响应</li>
     * </ol>
     *
     * @param request Agent 请求
     * @return Agent 响应
     * @throws AgentExecutionException 执行失败
     */
    AgentResponse invoke(AgentRequest request) throws AgentExecutionException;

    /**
     * 流式执行 Agent
     * <p>
     * 发送请求到 Agent，通过回调实时接收响应。
     * <p>
     * 流式输出包含：
     * <ul>
     *   <li>增量 Token - 通过 {@link StreamCallback#onToken(String)}</li>
     *   <li>工具调用事件 - 通过 {@link StreamCallback#onToolCall}</li>
     *   <li>思考过程 - 通过 {@link StreamCallback#onReasoning}</li>
     *   <li>完成/错误 - 通过 {@link StreamCallback#onComplete()}/ onError</li>
     * </ul>
     *
     * @param request  Agent 请求
     * @param callback 流式回调
     * @return Agent 响应
     * @throws AgentExecutionException 执行失败
     */
    AgentResponse stream(AgentRequest request, StreamCallback callback) throws AgentExecutionException;

    /**
     * 批量执行 Agent
     * <p>
     * 批量处理多个请求，每个请求独立执行。
     * 适用于需要同时处理多个用户输入的场景。
     *
     * @param requests Agent 请求列表
     * @return Agent 响应列表
     * @throws AgentExecutionException 执行失败
     */
    List<AgentResponse> batch(List<AgentRequest> requests) throws AgentExecutionException;

    /**
     * 获取 Agent 配置
     *
     * @return Agent 配置
     */
    AgentConfig getConfig();

    /**
     * 获取 Agent 状态
     * <p>
     * 返回 Agent 的当前运行时状态信息。
     *
     * @return Agent 状态
     */
    default AgentStatus getStatus() {
        return AgentStatus.idle();
    }
}
