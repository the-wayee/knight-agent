package org.cloudnook.knightagent.core.multiagent;

import org.cloudnook.knightagent.core.agent.Agent;
import org.cloudnook.knightagent.core.agent.AgentConfig;
import org.cloudnook.knightagent.core.agent.AgentExecutionException;
import org.cloudnook.knightagent.core.agent.AgentRequest;
import org.cloudnook.knightagent.core.agent.AgentResponse;
import org.cloudnook.knightagent.core.agent.factory.DefaultAgentFactory;
import org.cloudnook.knightagent.core.model.ChatModel;
import org.cloudnook.knightagent.core.model.OpenAIChatModel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Multi-Agent 系统真实 API 集成测试
 * <p>
 * 使用真实的 LLM API 测试 Multi-Agent 协作功能。
 *
 * @author KnightAgent
 * @since 1.0.0
 */
@DisplayName("Multi-Agent 真实 API 集成测试")
class OpenAIMultiAgentTest {

    // 使用 Groq API（兼容 OpenAI 格式）
    private static final String API_KEY = "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";
    private static final String BASE_URL = "https://api.groq.com/openai/v1";
    private static final String MODEL = "llama-3.3-70b-versatile";

    private static ChatModel chatModel;
    private static ChatModel supervisorModel;

    @BeforeAll
    static void setUp() {
        chatModel = OpenAIChatModel.builder()
                .apiKey(API_KEY)
                .baseUrl(BASE_URL)
                .modelId(MODEL)
                .build();

        supervisorModel = OpenAIChatModel.builder()
                .apiKey(API_KEY)
                .baseUrl(BASE_URL)
                .modelId(MODEL)
                .build();
    }

    @Test
    @DisplayName("基础双 Agent 协作")
    void testBasicTwoAgentCollaboration() throws AgentExecutionException {
        // 创建研究 Agent
        Agent researchAgent = new DefaultAgentFactory().createAgent()
                .model(chatModel)
                .config(AgentConfig.builder()
                        .systemPrompt(
                                "你是一个研究助手。负责搜索和整理信息。" +
                                "完成后，请用一句话总结你的研究成果，然后添加 [DONE] 标记。"
                        )
                        .maxIterations(3)
                        .build())
                .build();

        // 创建编程 Agent
        Agent codeAgent = new DefaultAgentFactory().createAgent()
                .model(chatModel)
                .config(AgentConfig.builder()
                        .systemPrompt(
                                "你是一个编程助手。负责根据研究结果编写代码。" +
                                "完成后，请输出代码并添加 [DONE] 标记。"
                        )
                        .maxIterations(3)
                        .build())
                .build();

        // 创建多 Agent 系统（不使用策略，直接手动路由）
        MultiAgentSystem multiAgent = MultiAgentSystem.builder()
                .addNode("researcher", researchAgent, "负责搜索和整理信息")
                .addNode("coder", codeAgent, "负责编写代码")
                .entryPoint("researcher")
                .maxHandoffs(3)
                .build();

        // 执行任务
        AgentRequest request = AgentRequest.of(
                "帮我研究一下 Java 8 的 Lambda 表达式语法，然后写一个简单的使用示例"
        );

        long startTime = System.currentTimeMillis();
        AgentResponse response = multiAgent.invoke(request);
        long duration = System.currentTimeMillis() - startTime;

        // 验证结果
        assertNotNull(response);
        assertNotNull(response.getOutput());
        assertFalse(response.getOutput().isEmpty());

        System.out.println("=== 双 Agent 协作测试 ===");
        System.out.println("耗时: " + duration + "ms");
        System.out.println("响应:\n" + response.getOutput());
        System.out.println();
    }

    @Test
    @DisplayName("三 Agent 工作流：分析→开发→审查")
    void testThreeAgentWorkflow() throws AgentExecutionException {
        // 需求分析师
        Agent analystAgent = new DefaultAgentFactory().createAgent()
                .model(chatModel)
                .config(AgentConfig.builder()
                        .systemPrompt(
                                "你是一个需求分析师。负责理解用户需求并产出技术规格说明。" +
                                "请简洁地输出需求分析结果，不超过 100 字。"
                        )
                        .maxIterations(3)
                        .build())
                .build();

        // 开发工程师
        Agent developerAgent = new DefaultAgentFactory().createAgent()
                .model(chatModel)
                .config(AgentConfig.builder()
                        .systemPrompt(
                                "你是一个开发工程师。根据需求规格编写简洁的代码示例。" +
                                "只输出核心代码，不要太多解释。"
                        )
                        .maxIterations(3)
                        .build())
                .build();

        // 代码审查员
        Agent reviewerAgent = new DefaultAgentFactory().createAgent()
                .model(chatModel)
                .config(AgentConfig.builder()
                        .systemPrompt(
                                "你是一个代码审查员。审查代码质量，指出潜在问题和改进建议。" +
                                "保持简短，不超过 80 字。"
                        )
                        .maxIterations(3)
                        .build())
                .build();

        // 构建工作流
        MultiAgentSystem workflow = MultiAgentSystem.builder()
                .addNode("analyst", analystAgent, "需求分析")
                .addNode("developer", developerAgent, "代码开发")
                .addNode("reviewer", reviewerAgent, "代码审查")
                .entryPoint("analyst")
                .maxHandoffs(5)
                .build();

        AgentRequest request = AgentRequest.of(
                "我需要一个 Java 工具类，用于计算两个日期之间的天数差"
        );

        long startTime = System.currentTimeMillis();
        AgentResponse response = workflow.invoke(request);
        long duration = System.currentTimeMillis() - startTime;

        System.out.println("=== 三 Agent 工作流测试 ===");
        System.out.println("耗时: " + duration + "ms");
        System.out.println("最终响应:\n" + response.getOutput());
        System.out.println();
    }

    @Test
    @DisplayName("专业分工：搜索专家→写作专家")
    void testSpecializedAgents() throws AgentExecutionException {
        // 搜索专家
        Agent searchAgent = new DefaultAgentFactory().createAgent()
                .model(chatModel)
                .config(AgentConfig.builder()
                        .systemPrompt(
                                "你是一个搜索专家。模拟搜索功能，提供关于 Java Stream API 的准确信息。" +
                                "输出要简洁，不超过 150 字。"
                        )
                        .maxIterations(3)
                        .build())
                .build();

        // 写作专家
        Agent writerAgent = new DefaultAgentFactory().createAgent()
                .model(chatModel)
                .config(AgentConfig.builder()
                        .systemPrompt(
                                "你是一个技术写作专家。将搜索结果整理成易于理解的教程。" +
                                "输出要清晰简洁，不超过 200 字。"
                        )
                        .maxIterations(3)
                        .build())
                .build();

        MultiAgentSystem system = MultiAgentSystem.builder()
                .addNode("searcher", searchAgent, "搜索专家")
                .addNode("writer", writerAgent, "写作专家")
                .entryPoint("searcher")
                .maxHandoffs(2)
                .build();

        AgentRequest request = AgentRequest.of(
                "搜索 Java Stream 的 filter 方法用法，并写一个简短的教程"
        );

        long startTime = System.currentTimeMillis();
        AgentResponse response = system.invoke(request);
        long duration = System.currentTimeMillis() - startTime;

        System.out.println("=== 专业分工测试 ===");
        System.out.println("耗时: " + duration + "ms");
        System.out.println("响应:\n" + response.getOutput());
        System.out.println();
    }

    @Test
    @DisplayName("带优先级的 Agent 选择")
    void testPriorityBasedSelection() throws AgentExecutionException {
        // 高级专家（优先级 10）
        AgentNode seniorAgent = AgentNode.builder()
                .name("senior")
                .agent(new DefaultAgentFactory().createAgent()
                        .model(chatModel)
                        .config(AgentConfig.builder()
                                .systemPrompt(
                                        "你是高级技术专家，只处理复杂的技术问题。" +
                                        "回答要专业但简洁，不超过 100 字。"
                                )
                                .maxIterations(3)
                                .build())
                        .build())
                .description("高级技术专家，处理复杂问题")
                .tags(List.of("expert", "complex"))
                .priority(10)
                .canReturnResult(true)
                .build();

        // 初级助手（优先级 100）
        AgentNode juniorAgent = AgentNode.builder()
                .name("junior")
                .agent(new DefaultAgentFactory().createAgent()
                        .model(chatModel)
                        .config(AgentConfig.builder()
                                .systemPrompt(
                                        "你是初级助手，负责回答简单的技术问题。" +
                                        "回答要友好简洁，不超过 80 字。"
                                )
                                .maxIterations(3)
                                .build())
                        .build())
                .description("初级助手，处理简单问题")
                .tags(List.of("basic", "simple"))
                .priority(100)
                .canReturnResult(true)
                .build();

        // 从初级助手开始，简单问题不需要升级
        MultiAgentSystem system = MultiAgentSystem.builder()
                .addNode(seniorAgent)
                .addNode(juniorAgent)
                .entryPoint("junior")
                .maxHandoffs(1)
                .build();

        AgentRequest request = AgentRequest.of("什么是 Java 的接口？");

        long startTime = System.currentTimeMillis();
        AgentResponse response = system.invoke(request);
        long duration = System.currentTimeMillis() - startTime;

        System.out.println("=== 优先级测试 ===");
        System.out.println("耗时: " + duration + "ms");
        System.out.println("响应:\n" + response.getOutput());
        System.out.println();

        // 简单问题应该由初级助手直接回答
        assertTrue(response.isSuccess());
    }

    @Test
    @DisplayName("错误处理和恢复")
    void testErrorHandling() throws AgentExecutionException {
        // 正常工作的 Agent
        Agent normalAgent = new DefaultAgentFactory().createAgent()
                .model(chatModel)
                .config(AgentConfig.builder()
                        .systemPrompt("你是一个正常的助手，简单回答问题即可。")
                        .maxIterations(3)
                        .build())
                .build();

        // 多 Agent 系统应该能正常处理单个 Agent 的情况
        MultiAgentSystem system = MultiAgentSystem.builder()
                .addNode("normal", normalAgent, "正常助手")
                .entryPoint("normal")
                .maxHandoffs(1)
                .build();

        AgentRequest request = AgentRequest.of("你好");

        long startTime = System.currentTimeMillis();
        AgentResponse response = system.invoke(request);
        long duration = System.currentTimeMillis() - startTime;

        System.out.println("=== 错误处理测试 ===");
        System.out.println("耗时: " + duration + "ms");
        System.out.println("响应:\n" + response.getOutput());
        System.out.println();

        assertNotNull(response);
        assertTrue(response.isSuccess());
    }

    @Test
    @DisplayName("多轮对话中的 Agent 保持")
    void testAgentConsistency() throws AgentExecutionException {
        // 创建一个专门的 Agent
        Agent mathAgent = new DefaultAgentFactory().createAgent()
                .model(chatModel)
                .config(AgentConfig.builder()
                        .systemPrompt("你是一个数学计算专家。只进行计算，不要做其他事情。")
                        .maxIterations(3)
                        .build())
                .build();

        MultiAgentSystem system = MultiAgentSystem.builder()
                .addNode("math", mathAgent, "数学计算专家")
                .entryPoint("math")
                .maxHandoffs(1)
                .build();

        // 第一轮
        AgentRequest request1 = AgentRequest.of("100 + 200 等于多少？");
        AgentResponse response1 = system.invoke(request1);
        System.out.println("第一轮: " + response1.getOutput());

        // 第二轮
        AgentRequest request2 = AgentRequest.of("50 * 3 等于多少？");
        AgentResponse response2 = system.invoke(request2);
        System.out.println("第二轮: " + response2.getOutput());

        System.out.println();

        assertNotNull(response1);
        assertNotNull(response2);
        assertTrue(response1.isSuccess());
        assertTrue(response2.isSuccess());
    }

    @Test
    @DisplayName("不同 systemPrompt 的 Agent 协作")
    void testDifferentSystemPrompts() throws AgentExecutionException {
        // 总结 Agent
        Agent summarizer = new DefaultAgentFactory().createAgent()
                .model(chatModel)
                .config(AgentConfig.builder()
                        .systemPrompt(
                                "你是一个内容总结专家。" +
                                "将长内容总结成 3-5 个要点，每个要点不超过 20 字。"
                        )
                        .maxIterations(3)
                        .build())
                .build();

        // 翻译 Agent
        Agent translator = new DefaultAgentFactory().createAgent()
                .model(chatModel)
                .config(AgentConfig.builder()
                        .systemPrompt(
                                "你是一个技术翻译专家。" +
                                "将技术内容翻译成通俗易懂的语言，不超过 100 字。"
                        )
                        .maxIterations(3)
                        .build())
                .build();

        MultiAgentSystem system = MultiAgentSystem.builder()
                .addNode("summarizer", summarizer, "内容总结")
                .addNode("translator", translator, "技术翻译")
                .entryPoint("summarizer")
                .maxHandoffs(2)
                .build();

        AgentRequest request = AgentRequest.of(
                "Lambda 表达式是 Java 8 引入的一个重要特性，它允许我们将函数作为方法参数传递，" +
                "使代码更加简洁和灵活。Lambda 表达式的语法是 (参数) -> { 方法体 }，" +
                "可以用于替代匿名内部类，特别是在使用集合框架的 Stream API 时非常有用。"
        );

        long startTime = System.currentTimeMillis();
        AgentResponse response = system.invoke(request);
        long duration = System.currentTimeMillis() - startTime;

        System.out.println("=== 不同 SystemPrompt 测试 ===");
        System.out.println("耗时: " + duration + "ms");
        System.out.println("最终响应:\n" + response.getOutput());
        System.out.println();
    }
}
