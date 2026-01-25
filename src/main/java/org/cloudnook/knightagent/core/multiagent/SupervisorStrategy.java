package org.cloudnook.knightagent.core.multiagent;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.cloudnook.knightagent.core.agent.AgentRequest;
import org.cloudnook.knightagent.core.agent.AgentResponse;
import org.cloudnook.knightagent.core.message.AIMessage;
import org.cloudnook.knightagent.core.message.HumanMessage;
import org.cloudnook.knightagent.core.message.Message;
import org.cloudnook.knightagent.core.model.ChatModel;
import org.cloudnook.knightagent.core.model.ChatOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Supervisor 策略
 * <p>
 * 使用 LLM 作为 Supervisor，根据各 Agent 的描述和当前状态，
 * 决定下一步应该调用哪个 Agent。
 * <p>
 * 这是 LangChain Multi-Agent 中最常用的模式，称为 "Supervisor Agent"。
 * <p>
 * 工作流程：
 * <ol>
 *   <li>Supervisor 接收用户请求</li>
 *   <li>Supervisor 分析请求，选择合适的 Agent</li>
 *   <li>被选中的 Agent 执行任务</li>
 *   <li>Agent 返回结果给 Supervisor</li>
 *   <li>Supervisor 判断是否需要继续或结束</li>
 * </ol>
 * <p>
 * 示例提示词：
 * <pre>{@code
 * 你是一个任务分发器。根据用户请求，选择合适的 Agent 来处理。
 *
 * 可用的 Agent:
 * - researcher: 搜索和整理信息
 * - coder: 编写代码
 * - reviewer: 审查代码质量
 *
 * 当前任务: {userInput}
 * 上一个 Agent: {previousAgent} 返回: {lastResponse}
 *
 * 请输出下一个要调用的 Agent 名称（仅输出名称，或 FINAL 表示结束）。
 * }</pre>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
@Slf4j
public class SupervisorStrategy implements HandoffStrategy {

    private final ChatModel supervisorModel;
    private final String systemPrompt;
    private final boolean verbose;

    /**
     * 默认系统提示词
     */
    private static final String DEFAULT_SYSTEM_PROMPT = """
            你是一个任务协调器（Supervisor）。你的职责是根据用户的请求和当前进度，
            决定下一步应该调用哪个 Agent，或者任务已经完成需要结束。

            请严格按照以下格式输出：
            - Agent 名称：直接输出 Agent 的名称
            - 结束任务：输出 "FINAL"

            只输出一个单词，不要添加任何额外说明。
            """;

    @Builder
    public SupervisorStrategy(ChatModel supervisorModel) {
        this(supervisorModel, DEFAULT_SYSTEM_PROMPT, false);
    }

    public SupervisorStrategy(ChatModel supervisorModel, String systemPrompt, boolean verbose) {
        this.supervisorModel = supervisorModel;
        this.systemPrompt = systemPrompt;
        this.verbose = verbose;
    }

    @Override
    public Optional<String> nextAgent(AgentRequest request,
                                      AgentResponse currentResponse,
                                      AgentNode currentAgent,
                                      List<AgentNode> availableAgents,
                                      int handoffCount) {

        // 构建提示词
        String prompt = buildSupervisorPrompt(request, currentResponse, currentAgent, availableAgents, handoffCount);

        if (verbose) {
            log.debug("Supervisor 提示词:\n{}", prompt);
        }

        try {
            // 调用 LLM 做决策
            List<Message> messages = List.of(
                    org.cloudnook.knightagent.core.message.SystemMessage.of(systemPrompt),
                    HumanMessage.of(prompt)
            );

            AIMessage decision = supervisorModel.chat(messages, ChatOptions.builder()
                    .temperature(0.0)  // 使用低温度确保输出稳定
                    .maxTokens(50)     // 只需要简短回应
                    .build());

            String decisionText = decision.getContent().trim();
            log.debug("Supervisor 决策: {}", decisionText);

            // 解析决策
            return parseDecision(decisionText, availableAgents);

        } catch (Exception e) {
            log.error("Supervisor 决策失败", e);
            return Optional.empty();
        }
    }

    /**
     * 构建 Supervisor 提示词
     */
    private String buildSupervisorPrompt(AgentRequest request,
                                         AgentResponse currentResponse,
                                         AgentNode currentAgent,
                                         List<AgentNode> availableAgents,
                                         int handoffCount) {

        StringBuilder prompt = new StringBuilder();

        // 添加可用 Agent 列表
        prompt.append("可用的 Agent:\n");
        for (AgentNode node : availableAgents) {
            if (node.isEnabled()) {
                prompt.append(String.format("- %s: %s%n",
                        node.getName(),
                        node.getDescription() != null ? node.getDescription() : "无描述"));
            }
        }
        prompt.append("\n");

        // 添加用户原始请求
        prompt.append("用户请求: ").append(request.getInput()).append("\n\n");

        // 添加当前进度
        if (handoffCount > 0) {
            prompt.append("当前进度:\n");
            prompt.append("- 已执行 ").append(handoffCount).append(" 个 Agent\n");
            if (currentAgent != null) {
                prompt.append("- 最后一个 Agent: ").append(currentAgent.getName()).append("\n");
            }
            if (currentResponse != null && currentResponse.getOutput() != null) {
                String output = currentResponse.getOutput();
                if (output.length() > 500) {
                    output = output.substring(0, 500) + "...";
                }
                prompt.append("- 上一个输出: ").append(output).append("\n");
            }
            prompt.append("\n");
        }

        // 添加决策指令
        prompt.append("请根据以上信息，决定下一步操作：\n");
        prompt.append("1. 如果任务已完成，输出: FINAL\n");
        prompt.append("2. 如果需要继续，输出下一个 Agent 的名称\n");
        prompt.append("\n只输出一个单词，不要添加任何额外说明。");

        return prompt.toString();
    }

    /**
     * 解析 LLM 的决策
     */
    private Optional<String> parseDecision(String decisionText, List<AgentNode> availableAgents) {
        if (decisionText == null || decisionText.isEmpty()) {
            return Optional.empty();
        }

        // 清理输出
        String cleaned = decisionText.trim()
                .replaceAll("^['\"]", "")
                .replaceAll("['\"]$", "")
                .toUpperCase();

        // 检查是否为结束指令
        if ("FINAL".equals(cleaned) || "END".equals(cleaned) || "DONE".equals(cleaned)) {
            return Optional.of("FINAL");
        }

        // 在可用 Agent 中查找匹配
        String finalDecision = cleaned;
        List<String> availableNames = availableAgents.stream()
                .filter(AgentNode::isEnabled)
                .map(AgentNode::getName)
                .map(String::toUpperCase)
                .toList();

        // 精确匹配
        for (String name : availableNames) {
            if (name.equals(finalDecision)) {
                // 返回原始大小写的名称
                return availableAgents.stream()
                        .filter(n -> n.getName().equalsIgnoreCase(finalDecision))
                        .map(AgentNode::getName)
                        .findFirst();
            }
        }

        // 模糊匹配
        for (String name : availableNames) {
            if (name.contains(finalDecision) || finalDecision.contains(name)) {
                return availableAgents.stream()
                        .filter(n -> n.getName().equalsIgnoreCase(name))
                        .map(AgentNode::getName)
                        .findFirst();
            }
        }

        log.warn("Supervisor 决策的 Agent '{}' 不在可用列表中: {}",
                finalDecision, availableNames);

        return Optional.empty();
    }

    /**
     * 创建默认的 SupervisorStrategy
     */
    public static SupervisorStrategy create(ChatModel supervisorModel) {
        return new SupervisorStrategy(supervisorModel);
    }

    /**
     * 创建带自定义提示词的 SupervisorStrategy
     */
    public static SupervisorStrategy create(ChatModel supervisorModel, String customPrompt) {
        return new SupervisorStrategy(supervisorModel, customPrompt, false);
    }
}
