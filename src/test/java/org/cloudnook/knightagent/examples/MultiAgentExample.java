package org.cloudnook.knightagent.examples;

import org.cloudnook.knightagent.core.agent.Agent;
import org.cloudnook.knightagent.core.agent.AgentConfig;
import org.cloudnook.knightagent.core.agent.AgentExecutionException;
import org.cloudnook.knightagent.core.agent.AgentRequest;
import org.cloudnook.knightagent.core.agent.AgentResponse;
import org.cloudnook.knightagent.core.agent.factory.DefaultAgentFactory;
import org.cloudnook.knightagent.core.message.AIMessage;
import org.cloudnook.knightagent.core.message.Message;
import org.cloudnook.knightagent.core.model.ChatModel;
import org.cloudnook.knightagent.core.model.ChatOptions;
import org.cloudnook.knightagent.core.model.ModelException;
import org.cloudnook.knightagent.core.multiagent.AgentNode;
import org.cloudnook.knightagent.core.multiagent.MultiAgentSystem;
import org.cloudnook.knightagent.core.multiagent.SupervisorStrategy;

import java.util.ArrayList;
import java.util.List;

/**
 * Multi-Agent 系统使用示例
 * <p>
 * 演示如何使用 MultiAgentSystem 构建协作式多 Agent 应用。
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public class MultiAgentExample {

    /**
     * 示例 1：基础的多 Agent 协作
     * <p>
     * 场景：用户提问 -> 研究 Agent -> 编码 Agent -> 完成
     */
    public static void example1_BasicCollaboration() throws AgentExecutionException {
        System.out.println("=== 示例 1：基础多 Agent 协作 ===\n");

        ChatModel model = createMockModel();

        // 创建专业化的 Agent
        Agent researchAgent = new DefaultAgentFactory().createAgent()
                .model(model)
                .config(AgentConfig.builder()
                        .systemPrompt("你是一个研究助手，负责搜索和整理信息。")
                        .build())
                .build();

        Agent codeAgent = new DefaultAgentFactory().createAgent()
                .model(model)
                .config(AgentConfig.builder()
                        .systemPrompt("你是一个编程助手，负责编写代码。")
                        .build())
                .build();

        // 创建多 Agent 系统
        MultiAgentSystem multiAgent = MultiAgentSystem.builder()
                .addNode("researcher", researchAgent, "负责搜索和整理信息")
                .addNode("coder", codeAgent, "负责编写代码")
                .entryPoint("researcher")
                .maxHandoffs(3)
                .build();

        // 执行
        AgentRequest request = AgentRequest.of("帮我研究一下 Java 的 Stream API，然后写一个示例");
        AgentResponse response = multiAgent.invoke(request);

        System.out.println("最终响应:");
        System.out.println(response.getOutput());
        System.out.println();
    }

    /**
     * 示例 2：使用 Supervisor 模式
     * <p>
     * 场景：LLM 决定路由，自动选择合适的 Agent
     */
    public static void example2_SupervisorMode() throws AgentExecutionException {
        System.out.println("=== 示例 2：Supervisor 模式 ===\n");

        ChatModel model = createMockModel();
        ChatModel supervisorModel = createSupervisorModel();  // 专门用于路由的模型

        // 创建多个专业 Agent
        Agent searchAgent = new DefaultAgentFactory().createAgent()
                .model(model)
                .config(AgentConfig.builder()
                        .systemPrompt("你是搜索专家，负责查找网络信息。")
                        .build())
                .build();

        Agent mathAgent = new DefaultAgentFactory().createAgent()
                .model(model)
                .config(AgentConfig.builder()
                        .systemPrompt("你是数学专家，负责计算和解决数学问题。")
                        .build())
                .build();

        Agent writingAgent = new DefaultAgentFactory().createAgent()
                .model(model)
                .config(AgentConfig.builder()
                        .systemPrompt("你是写作专家，负责撰写文章和报告。")
                        .build())
                .build();

        // 创建多 Agent 系统（使用 Supervisor 策略）
        MultiAgentSystem multiAgent = MultiAgentSystem.builder()
                .addNode("searcher", searchAgent, "搜索专家，负责查找信息")
                .addNode("math", mathAgent, "数学专家，负责计算")
                .addNode("writer", writingAgent, "写作专家，负责撰写")
                .entryPoint("searcher")
                .strategy(new SupervisorStrategy(supervisorModel))
                .maxHandoffs(5)
                .verbose(true)
                .build();

        // 执行不同类型的任务
        System.out.println("--- 任务 1：数学问题 ---");
        AgentRequest request1 = AgentRequest.of("计算 123 + 456 等于多少？");
        AgentResponse response1 = multiAgent.invoke(request1);
        System.out.println("响应: " + response1.getOutput());

        System.out.println("\n--- 任务 2：搜索问题 ---");
        AgentRequest request2 = AgentRequest.of("搜索最新的 AI 技术发展");
        AgentResponse response2 = multiAgent.invoke(request2);
        System.out.println("响应: " + response2.getOutput());
        System.out.println();
    }

    /**
     * 示例 3：完整的代码生成流程
     * <p>
     * 场景：需求分析 -> 编码 -> 测试 -> 审查 -> 完成
     */
    public static void example3_CodeGenerationWorkflow() throws AgentExecutionException {
        System.out.println("=== 示例 3：代码生成工作流 ===\n");

        ChatModel model = createMockModel();
        ChatModel supervisorModel = createSupervisorModel();

        // 创建工作流 Agent
        Agent analystAgent = new DefaultAgentFactory().createAgent()
                .model(model)
                .config(AgentConfig.builder()
                        .systemPrompt("你是需求分析师，负责分析用户需求并产出技术规格。")
                        .build())
                .build();

        Agent developerAgent = new DefaultAgentFactory().createAgent()
                .model(model)
                .config(AgentConfig.builder()
                        .systemPrompt("你是开发工程师，负责根据规格编写高质量代码。")
                        .build())
                .build();

        Agent testerAgent = new DefaultAgentFactory().createAgent()
                .model(model)
                .config(AgentConfig.builder()
                        .systemPrompt("你是测试工程师，负责编写测试用例并验证代码质量。")
                        .build())
                .build();

        Agent reviewerAgent = new DefaultAgentFactory().createAgent()
                .model(model)
                .config(AgentConfig.builder()
                        .systemPrompt("你是代码审查员，负责审查代码并提供改进建议。")
                        .build())
                .build();

        // 构建多 Agent 系统
        MultiAgentSystem multiAgent = MultiAgentSystem.builder()
                .addNode("analyst", analystAgent, "需求分析")
                .addNode("developer", developerAgent, "代码开发")
                .addNode("tester", testerAgent, "测试验证")
                .addNode("reviewer", reviewerAgent, "代码审查")
                .entryPoint("analyst")
                .strategy(new SupervisorStrategy(supervisorModel))
                .maxHandoffs(10)
                .build();

        // 执行完整流程
        AgentRequest request = AgentRequest.of(
                "我需要一个 Java 工具类，用于解析 JSON 数据，支持嵌套对象和数组查询"
        );

        System.out.println("开始处理请求...");
        AgentResponse response = multiAgent.invoke(request);

        System.out.println("\n=== 最终结果 ===");
        System.out.println(response.getOutput());
        System.out.println("\n=== 执行统计 ===");
        System.out.println("响应: " + response.getOutput());
    }

    /**
     * 示例 4：带优先级的 Agent 选择
     * <p>
     * 演示如何使用节点优先级和标签
     */
    public static void example4_PriorityAndTags() throws AgentExecutionException {
        System.out.println("=== 示例 4：优先级和标签 ===\n");

        ChatModel model = createMockModel();

        // 创建不同优先级的 Agent
        AgentNode seniorAgent = AgentNode.builder()
                .name("senior")
                .agent(new DefaultAgentFactory().createAgent()
                        .model(model)
                        .config(AgentConfig.builder()
                                .systemPrompt("你是高级专家，只处理复杂问题。")
                                .build())
                        .build())
                .description("高级专家")
                .tags(List.of("expert", "complex"))
                .priority(1)  // 高优先级
                .build();

        AgentNode juniorAgent = AgentNode.builder()
                .name("junior")
                .agent(new DefaultAgentFactory().createAgent()
                        .model(model)
                        .config(AgentConfig.builder()
                                .systemPrompt("你是初级助手，负责简单问题。")
                                .build())
                        .build())
                .description("初级助手")
                .tags(List.of("basic", "simple"))
                .priority(100)  // 低优先级
                .build();

        MultiAgentSystem multiAgent = MultiAgentSystem.builder()
                .addNode(seniorAgent)
                .addNode(juniorAgent)
                .entryPoint("junior")  // 从初级助手开始
                .maxHandoffs(2)
                .build();

        AgentRequest request = AgentRequest.of("解释什么是多态性");
        AgentResponse response = multiAgent.invoke(request);

        System.out.println("响应: " + response.getOutput());
        System.out.println();
    }

    /**
     * 示例 5：通过响应内容触发手 off
     * <p>
     * 演示 Agent 如何通过特定格式发起手 off
     */
    public static void example5_ResponseBasedHandoff() throws AgentExecutionException {
        System.out.println("=== 示例 5：基于响应的手 off ===\n");

        ChatModel model = createHandoffAwareModel();

        // Agent 1：处理完成后发起手 off
        Agent step1Agent = new DefaultAgentFactory().createAgent()
                .model(model)
                .config(AgentConfig.builder()
                        .systemPrompt("你是第一步处理器。完成后输出 HANDOFF:step2:完成第一步")
                        .build())
                .build();

        // Agent 2：最终处理器
        Agent step2Agent = new DefaultAgentFactory().createAgent()
                .model(model)
                .config(AgentConfig.builder()
                        .systemPrompt("你是第二步处理器。输出最终结果。")
                        .build())
                .build();

        MultiAgentSystem multiAgent = MultiAgentSystem.builder()
                .addNode("step1", step1Agent, "第一步")
                .addNode("step2", step2Agent, "第二步")
                .entryPoint("step1")
                .maxHandoffs(3)
                .build();

        AgentRequest request = AgentRequest.of("执行两步流程");
        AgentResponse response = multiAgent.invoke(request);

        System.out.println("最终响应: " + response.getOutput());
        System.out.println("最终响应: " + response.getOutput());
        System.out.println();
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建 Mock 模型
     */
    private static ChatModel createMockModel() {
        return new ChatModel() {
            @Override
            public AIMessage chat(List<Message> messages, ChatOptions options) throws ModelException {
                String lastUserMessage = "";
                for (Message msg : messages) {
                    if (msg.getType() == Message.MessageType.HUMAN) {
                        lastUserMessage = msg.getContent();
                    }
                }

                String response;
                if (lastUserMessage.contains("研究") || lastUserMessage.contains("搜索")) {
                    response = "我已经完成了信息研究，Java Stream API 是一个强大的数据处理工具...";
                } else if (lastUserMessage.contains("代码") || lastUserMessage.contains("编写")) {
                    response = "我已经编写了完整的代码示例，使用 Stream API 处理数据...";
                } else if (lastUserMessage.contains("计算") || lastUserMessage.contains("数学")) {
                    response = "计算结果: 123 + 456 = 579";
                } else if (lastUserMessage.contains("写") || lastUserMessage.contains("文章")) {
                    response = "我已经撰写了完整的文章...";
                } else {
                    response = "任务已完成";
                }

                return AIMessage.of(response);
            }

            @Override
            public void chatStream(List<Message> messages, ChatOptions options,
                                   org.cloudnook.knightagent.core.streaming.StreamCallback callback) {
                try {
                    AIMessage response = chat(messages, options);
                    callback.onToken(response.getContent());
                    callback.onComplete();
                } catch (ModelException e) {
                    callback.onError(e);
                }
            }

            @Override
            public int countTokens(String text) {
                return text.length();
            }

            @Override
            public org.cloudnook.knightagent.core.model.ModelCapabilities getCapabilities() {
                return org.cloudnook.knightagent.core.model.ModelCapabilities.builder().build();
            }

            @Override
            public String getModelId() {
                return "mock-model";
            }

            @Override
            public boolean isAvailable() {
                return true;
            }
        };
    }

    /**
     * 创建 Supervisor 专用模型（模拟路由决策）
     */
    private static ChatModel createSupervisorModel() {
        return new ChatModel() {
            private int callCount = 0;

            @Override
            public AIMessage chat(List<Message> messages, ChatOptions options) {
                // 简单模拟：根据输入关键词决定下一个 Agent
                String lastMessage = "";
                for (Message msg : messages) {
                    if (msg.getType() == Message.MessageType.HUMAN) {
                        lastMessage = msg.getContent();
                    }
                }

                String decision;
                if (lastMessage.contains("计算") || lastMessage.contains("数学")) {
                    decision = "math";
                } else if (lastMessage.contains("写") || lastMessage.contains("文章")) {
                    decision = "writer";
                } else if (lastMessage.contains("代码") || lastMessage.contains("开发")) {
                    decision = "developer";
                } else if (callCount < 2) {
                    decision = "searcher";
                    callCount++;
                } else {
                    decision = "FINAL";
                }

                return AIMessage.of(decision);
            }

            @Override
            public void chatStream(List<Message> messages, ChatOptions options,
                                   org.cloudnook.knightagent.core.streaming.StreamCallback callback) {
                AIMessage response = chat(messages, options);
                callback.onToken(response.getContent());
                callback.onComplete();
            }

            @Override
            public int countTokens(String text) {
                return text.length();
            }

            @Override
            public org.cloudnook.knightagent.core.model.ModelCapabilities getCapabilities() {
                return org.cloudnook.knightagent.core.model.ModelCapabilities.builder().build();
            }

            @Override
            public String getModelId() {
                return "supervisor-model";
            }

            @Override
            public boolean isAvailable() {
                return true;
            }
        };
    }

    /**
     * 创建能识别手 off 指令的模型
     */
    private static ChatModel createHandoffAwareModel() {
        return new ChatModel() {
            @Override
            public AIMessage chat(List<Message> messages, ChatOptions options) {
                return AIMessage.of("步骤完成");
            }

            @Override
            public void chatStream(List<Message> messages, ChatOptions options,
                                   org.cloudnook.knightagent.core.streaming.StreamCallback callback) {
                AIMessage response = chat(messages, options);
                callback.onToken(response.getContent());
                callback.onComplete();
            }

            @Override
            public int countTokens(String text) {
                return text.length();
            }

            @Override
            public org.cloudnook.knightagent.core.model.ModelCapabilities getCapabilities() {
                return org.cloudnook.knightagent.core.model.ModelCapabilities.builder().build();
            }

            @Override
            public String getModelId() {
                return "handoff-aware-model";
            }

            @Override
            public boolean isAvailable() {
                return true;
            }
        };
    }

    /**
     * 主方法
     */
    public static void main(String[] args) throws AgentExecutionException {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║           Multi-Agent 系统使用示例                            ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();

        example1_BasicCollaboration();
        example2_SupervisorMode();
        example3_CodeGenerationWorkflow();
        example4_PriorityAndTags();
        example5_ResponseBasedHandoff();

        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║                    所有示例运行完成                             ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
    }
}
