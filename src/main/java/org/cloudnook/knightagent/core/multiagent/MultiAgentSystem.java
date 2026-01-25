package org.cloudnook.knightagent.core.multiagent;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.cloudnook.knightagent.core.agent.Agent;
import org.cloudnook.knightagent.core.agent.AgentConfig;
import org.cloudnook.knightagent.core.agent.AgentExecutionException;
import org.cloudnook.knightagent.core.agent.AgentRequest;
import org.cloudnook.knightagent.core.agent.AgentResponse;
import org.cloudnook.knightagent.core.streaming.StreamCallback;

import java.time.Instant;
import java.util.*;

/**
 * 多 Agent 系统
 * <p>
 * 协调多个 Agent 协作完成复杂任务的系统。
 * 实现了 LangChain 风格的 Multi-Agent 模式，支持：
 * <ul>
 *   <li>Supervisor 模式 - 由 LLM 决定路由</li>
 *   <li>手 off 机制 - Agent 之间转交控制权</li>
 *   <li>状态共享 - 所有 Agent 共享对话历史</li>
 *   <li>动态路由 - 根据响应内容选择下一个 Agent</li>
 * </ul>
 * <p>
 * 使用示例：
 * <pre>{@code
 * MultiAgentSystem system = MultiAgentSystem.builder()
 *     .addNode("researcher", researchAgent, "负责搜索信息")
 *     .addNode("coder", codeAgent, "负责编写代码")
 *     .addNode("reviewer", reviewAgent, "负责审查代码")
 *     .entryPoint("researcher")
 *     .strategy(new SupervisorStrategy(chatModel))
 *     .maxHandoffs(5)
 *     .build();
 *
 * AgentResponse response = system.invoke(
 *     AgentRequest.of("研究 Java 新特性并写个示例")
 * );
 * }</pre>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
@Slf4j
public class MultiAgentSystem implements Agent {

    private final Map<String, AgentNode> agents;
    private final String entryPoint;
    private final HandoffStrategy strategy;
    private final int maxHandoffs;
    private final AgentConfig config;
    private final boolean verbose;

    private MultiAgentSystem(Builder builder) {
        this.agents = Collections.unmodifiableMap(new LinkedHashMap<>(builder.agents));
        this.entryPoint = builder.entryPoint;
        this.strategy = builder.strategy;
        this.maxHandoffs = builder.maxHandoffs;
        this.config = builder.config;
        this.verbose = builder.verbose;
    }

    @Override
    public AgentResponse invoke(AgentRequest request) throws AgentExecutionException {
        log.debug("MultiAgentSystem 开始执行，入口: {}, 可用 Agent: {}", entryPoint, agents.keySet());

        // 验证入口节点存在
        AgentNode currentAgent = agents.get(entryPoint);
        if (currentAgent == null) {
            throw new AgentExecutionException("入口节点不存在: " + entryPoint);
        }

        AgentResponse finalResponse = null;
        AgentNode previousAgent = null;
        int handoffCount = 0;

        // 执行循环
        while (handoffCount < maxHandoffs) {
            log.debug("执行 Agent: {} (第 {} 轮)", currentAgent.getName(), handoffCount + 1);

            // 调用当前 Agent
            AgentResponse response = executeAgent(currentAgent, request, previousAgent, handoffCount);
            finalResponse = response;

            // 检查是否需要继续手 off
            if (!shouldContinue(response, currentAgent, handoffCount)) {
                log.debug("Agent {} 完成任务，结束流程", currentAgent.getName());
                break;
            }

            // 使用策略决定下一个 Agent
            Optional<String> nextAgentName = strategy.nextAgent(
                    request, response, currentAgent,
                    new ArrayList<>(agents.values()),
                    handoffCount
            );

            if (nextAgentName.isEmpty()) {
                // 策略无法决定，使用默认行为
                log.debug("策略无法决定下一个 Agent，使用当前 Agent 的结果");
                break;
            }

            String nextName = nextAgentName.get();
            if ("FINAL".equals(nextName) || isResponseComplete(response)) {
                // 流程结束
                log.debug("流程结束，Agent: {}, 原因: FINAL/complete",
                        currentAgent.getName());
                break;
            }

            // 查找下一个 Agent
            AgentNode nextAgent = agents.get(nextName);
            if (nextAgent == null) {
                log.warn("策略返回的 Agent 不存在: {}，结束流程", nextName);
                break;
            }

            if (!nextAgent.isEnabled()) {
                log.warn("Agent {} 未启用，结束流程", nextName);
                break;
            }

            // 记录手 off
            log.info("手 off: {} -> {}, 原因: {}",
                    currentAgent.getName(), nextAgent.getName(),
                    response.getOutput() != null && response.getOutput().length() > 100
                            ? response.getOutput().substring(0, 100) + "..."
                            : response.getOutput());

            previousAgent = currentAgent;
            currentAgent = nextAgent;
            handoffCount++;
        }

        if (handoffCount >= maxHandoffs) {
            log.warn("达到最大手 off 次数: {}", maxHandoffs);
        }

        // 构建最终响应，包含执行路径
        return enrichResponse(finalResponse, handoffCount);
    }

    @Override
    public void stream(AgentRequest request, StreamCallback callback) throws AgentExecutionException {
        // 简化实现：使用同步调用 + 分块推送
        AgentResponse response = invoke(request);
        String output = response.getOutput();

        if (output != null) {
            callback.onStart();
            // 模拟流式输出，每 10 个字符一个 token
            for (int i = 0; i < output.length(); i += 10) {
                int end = Math.min(i + 10, output.length());
                String token = output.substring(i, end);
                callback.onToken(token);
            }
            callback.onComplete();
        }
    }

    @Override
    public List<AgentResponse> batch(List<AgentRequest> requests) throws AgentExecutionException {
        List<AgentResponse> responses = new ArrayList<>();
        for (AgentRequest request : requests) {
            responses.add(invoke(request));
        }
        return responses;
    }

    @Override
    public AgentConfig getConfig() {
        return config;
    }

    /**
     * 执行单个 Agent
     */
    private AgentResponse executeAgent(AgentNode agentNode,
                                      AgentRequest request,
                                      AgentNode previousAgent,
                                      int iteration) {
        try {
            // 构建上下文信息
            AgentRequest enrichedRequest = enrichRequest(request, agentNode, previousAgent, iteration);

            // 执行 Agent
            return agentNode.getAgent().invoke(enrichedRequest);

        } catch (AgentExecutionException e) {
            log.error("Agent {} 执行失败", agentNode.getName(), e);
            return AgentResponse.builder()
                    .output(String.format("[Agent %s 执行失败: %s]",
                            agentNode.getName(), e.getMessage()))
                    .error(e.getMessage())
                    .startTime(Instant.now())
                    .endTime(Instant.now())
                    .build();
        }
    }

    /**
     * 丰富请求，添加上下文信息
     */
    private AgentRequest enrichRequest(AgentRequest original,
                                      AgentNode currentAgent,
                                      AgentNode previousAgent,
                                      int iteration) {
        var builder = original.toBuilder();

        // 如果有系统提示词，添加 Agent 上下文
        if (original.getSystemPrompt() == null || original.getSystemPrompt().isEmpty()) {
            StringBuilder systemPrompt = new StringBuilder();

            // 添加 Agent 自身的描述
            if (currentAgent.getDescription() != null) {
                systemPrompt.append("你的角色: ").append(currentAgent.getDescription()).append("\n\n");
            }

            // 添加前一个 Agent 的信息
            if (previousAgent != null) {
                systemPrompt.append("上一个处理者: ").append(previousAgent.getName());
                if (previousAgent.getDescription() != null) {
                    systemPrompt.append(" (").append(previousAgent.getDescription()).append(")");
                }
                systemPrompt.append("\n\n");
            }

            if (systemPrompt.length() > 0) {
                builder.systemPrompt(systemPrompt.toString());
            }
        }

        return builder.build();
    }

    /**
     * 检查响应是否完成（包含结束标记）
     */
    private boolean isResponseComplete(AgentResponse response) {
        if (response == null) {
            return false;
        }
        String output = response.getOutput();
        if (output == null) {
            return false;
        }
        // 检查是否包含完成标记
        return output.contains("[DONE]") ||
               output.contains("[完成]") ||
               output.contains("[FINISH]");
    }

    /**
     * 检查是否应该继续手 off
     */
    private boolean shouldContinue(AgentResponse response, AgentNode currentAgent, int handoffCount) {
        // 1. 检查响应是否明确表示完成
        if (isResponseComplete(response)) {
            return false;
        }

        // 2. 检查是否有错误
        if (!response.isSuccess()) {
            return false;
        }

        // 3. 检查当前 Agent 是否可以直接返回结果
        if (currentAgent.isCanReturnResult()) {
            // 检查响应中是否包含手 off 指令
            Optional<AgentHandoff> handoff = AgentHandoff.parseFromResponse(response.getOutput());
            if (handoff.isEmpty()) {
                // 没有手 off 指令，可以返回
                return false;
            }
        }

        // 4. 询问策略
        return strategy.shouldContinue(response);
    }

    /**
     * 丰富最终响应，添加执行路径信息
     */
    private AgentResponse enrichResponse(AgentResponse response, int handoffCount) {
        if (response == null) {
            return AgentResponse.builder()
                    .output("[无响应]")
                    .startTime(Instant.now())
                    .endTime(Instant.now())
                    .build();
        }

        // 使用现有响应的值，只添加 metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("handoffCount", handoffCount);
        metadata.put("multiAgent", true);
        metadata.put("agentCount", agents.size());

        // 由于 AgentResponse 没有 toBuilder，我们创建一个新的响应
        return AgentResponse.builder()
                .output(response.getOutput())
                .messages(response.getMessages())
                .toolCalls(response.getToolCalls())
                .state(response.getState())
                .threadId(response.getThreadId())
                .checkpointId(response.getCheckpointId())
                .durationMs(response.getDurationMs())
                .tokensUsed(response.getTokensUsed())
                .startTime(response.getStartTime())
                .endTime(response.getEndTime())
                .error(response.getError())
                .build();
    }

    /**
     * 获取所有 Agent 节点
     */
    public Map<String, AgentNode> getAgents() {
        return agents;
    }

    /**
     * 获取指定的 Agent 节点
     */
    public Optional<AgentNode> getAgent(String name) {
        return Optional.ofNullable(agents.get(name));
    }

    /**
     * 获取入口节点名称
     */
    public String getEntryPoint() {
        return entryPoint;
    }

    /**
     * 获取手 off 策略
     */
    public HandoffStrategy getStrategy() {
        return strategy;
    }

    /**
     * 创建构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 构建器
     */
    public static class Builder {
        private final Map<String, AgentNode> agents = new LinkedHashMap<>();
        private String entryPoint;
        private HandoffStrategy strategy;
        private int maxHandoffs = 10;
        private AgentConfig config = AgentConfig.defaults();
        private boolean verbose = false;

        /**
         * 添加 Agent 节点
         */
        public Builder addNode(String name, Agent agent, String description) {
            AgentNode node = AgentNode.builder()
                    .name(name)
                    .agent(agent)
                    .description(description)
                    .build();
            agents.put(name, node);
            return this;
        }

        /**
         * 添加 Agent 节点（完整配置）
         */
        public Builder addNode(AgentNode node) {
            agents.put(node.getName(), node);
            return this;
        }

        /**
         * 批量添加 Agent 节点
         */
        public Builder addNodes(List<AgentNode> nodes) {
            nodes.forEach(node -> agents.put(node.getName(), node));
            return this;
        }

        /**
         * 批量添加 Agent（简化版）
         */
        public Builder addAgents(Map<String, Agent> agentMap) {
            agentMap.forEach((name, agent) -> {
                AgentNode node = AgentNode.of(name, agent);
                agents.put(name, node);
            });
            return this;
        }

        /**
         * 设置入口节点
         */
        public Builder entryPoint(String entryPoint) {
            this.entryPoint = entryPoint;
            return this;
        }

        /**
         * 设置手 off 策略
         */
        public Builder strategy(HandoffStrategy strategy) {
            this.strategy = strategy;
            return this;
        }

        /**
         * 设置最大手 off 次数
         */
        public Builder maxHandoffs(int maxHandoffs) {
            this.maxHandoffs = maxHandoffs;
            return this;
        }

        /**
         * 设置配置
         */
        public Builder config(AgentConfig config) {
            this.config = config;
            return this;
        }

        /**
         * 设置是否输出详细日志
         */
        public Builder verbose(boolean verbose) {
            this.verbose = verbose;
            return this;
        }

        /**
         * 构建 MultiAgentSystem
         */
        public MultiAgentSystem build() {
            // 验证
            if (agents.isEmpty()) {
                throw new IllegalStateException("至少需要一个 Agent 节点");
            }

            // 默认入口为第一个添加的 Agent
            if (entryPoint == null || entryPoint.isEmpty()) {
                entryPoint = agents.keySet().iterator().next();
            }

            // 验证入口存在
            if (!agents.containsKey(entryPoint)) {
                throw new IllegalStateException("入口节点不存在: " + entryPoint);
            }

            return new MultiAgentSystem(this);
        }
    }
}
