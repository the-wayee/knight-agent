package org.cloudnook.knightagent.core.agent;

import org.cloudnook.knightagent.core.agent.factory.DefaultAgentFactory;
import org.cloudnook.knightagent.core.checkpoint.Checkpointer;
import org.cloudnook.knightagent.core.checkpoint.InMemorySaver;
import org.cloudnook.knightagent.core.middleware.AgentContext;
import org.cloudnook.knightagent.core.middleware.Middleware;
import org.cloudnook.knightagent.core.middleware.builtin.HumanInTheLoopMiddleware;
import org.cloudnook.knightagent.core.middleware.builtin.StateInjectionMiddleware;
import org.cloudnook.knightagent.core.model.ChatModel;
import org.cloudnook.knightagent.core.model.OpenAIChatModel;
import org.cloudnook.knightagent.core.streaming.StreamCallbackAdapter;
import org.cloudnook.knightagent.core.streaming.StreamChunk;
import org.cloudnook.knightagent.core.streaming.StreamCompleteResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ReAct Agent 集成测试
 * <p>
 * 测试中间件和人机交互功能，使用真实的模型 API。
 * <p>
 * 运行测试前需要设置环境变量：
 * <ul>
 *   <li>OPENAI_API_KEY - OpenAI API 密钥</li>
 *   <li>OPENAI_BASE_URL - (可选) API 基础 URL</li>
 *   <li>OPENAI_MODEL - (可选) 模型名称，默认为 gpt-3.5-turbo</li>
 * </ul>
 * <p>
 * 运行测试：
 * <pre>{@code
 * ./mvnw test -Dtest=ReActAgentIntegrationTest
 * }</pre>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
class ReActAgentIntegrationTest {

    private String apiKey;
    private String baseUrl;
    private String modelId;
    private ChatModel model;

    @BeforeEach
    void setUp() {
        apiKey = System.getenv("OPENAI_API_KEY");
        baseUrl = System.getenv("OPENAI_BASE_URL");
        modelId = System.getenv().getOrDefault("OPENAI_MODEL", "gpt-3.5-turbo");

        org.junit.jupiter.api.Assumptions.assumeTrue(
                apiKey != null && !apiKey.isEmpty(),
                "OPENAI_API_KEY environment variable not set, skipping tests"
        );

        model = OpenAIChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelId(modelId)
                .build();
    }

    /**
     * 测试基础对话 - 无中间件
     */
    @Test
    void testBasicConversation() throws AgentExecutionException {
        Agent agent = DefaultAgentFactory.agent()
                .model(model)
                .config(AgentConfig.builder()
                        .systemPrompt("你是一个有帮助的AI助手。")
                        .build())
                .build();

        AgentResponse response = agent.invoke(AgentRequest.of("你好，请简单介绍一下你自己。"));

        assertNotNull(response);
        assertNotNull(response.getOutput());
        assertFalse(response.getOutput().isEmpty());

        System.out.println("=== 基础对话测试 ===");
        System.out.println("响应: " + response.getOutput());
        System.out.println("耗时: " + response.getDurationMs() + "ms");
    }

    /**
     * 测试多轮对话 - 使用 Checkpoint
     */
    @Test
    void testMultiTurnConversation() throws AgentExecutionException {
        Checkpointer checkpointer = new InMemorySaver();

        Agent agent = DefaultAgentFactory.agent()
                .model(model)
                .checkpointer(checkpointer)
                .config(AgentConfig.builder()
                        .systemPrompt("你是一个友好的AI助手，请记住用户的个人信息。")
                        .build())
                .build();

        String threadId = "test-conversation-001";

        // 第一轮：用户介绍自己
        System.out.println("=== 多轮对话测试 - 第一轮 ===");
        AgentRequest request1 = AgentRequest.builder()
                .input("我叫张三，今年25岁，是一名软件工程师。")
                .threadId(threadId)
                .build();

        AgentResponse response1 = agent.invoke(request1);
        System.out.println("AI: " + response1.getOutput());

        // 第二轮：询问用户信息
        System.out.println("\n=== 多轮对话测试 - 第二轮 ===");
        AgentRequest request2 = AgentRequest.builder()
                .input("你还记得我的名字和职业吗？")
                .threadId(threadId)
                .build();

        AgentResponse response2 = agent.invoke(request2);
        System.out.println("AI: " + response2.getOutput());

        // 验证 AI 记住了用户信息
        assertTrue(response2.getOutput().contains("张三") || response2.getOutput().contains("软件工程师"),
                "AI 应该记住用户的名字或职业");
    }

    /**
     * 测试自定义中间件 - 日志记录
     */
    @Test
    void testCustomLoggingMiddleware() throws AgentExecutionException {
        AtomicInteger invokeCount = new AtomicInteger(0);

        Middleware customLogging = new Middleware() {
            @Override
            public void beforeInvoke(AgentRequest request, AgentContext context) {
                System.out.println("[LOG] 开始执行 - Iteration: " + context.getIteration());
                System.out.println("[LOG] 输入: " + request.getInput());
                System.out.println("[LOG] 状态: " + context.getStatus());
                invokeCount.incrementAndGet();
            }

            @Override
            public void afterInvoke(AgentResponse response, AgentContext context) {
                System.out.println("[LOG] 执行完成");
                System.out.println("[LOG] 输出: " + response.getOutput());
                System.out.println("[LOG] 耗时: " + response.getDurationMs() + "ms");
                System.out.println("[LOG] 最终状态: " + context.getStatus());
            }
        };

        Agent agent = DefaultAgentFactory.agent()
                .model(model)
                .middleware(customLogging)
                .build();

        System.out.println("=== 自定义日志中间件测试 ===");
        AgentResponse response = agent.invoke(AgentRequest.of("2 + 2 等于多少？"));

        assertNotNull(response);
        assertEquals(1, invokeCount.get());
        assertTrue(response.getOutput().contains("4"));
    }

    /**
     * 测试 StateInjectionMiddleware - 状态注入
     */
    @Test
    void testStateInjectionMiddleware() throws AgentExecutionException {
        Middleware stateInjection = StateInjectionMiddleware.builder()
                .injectionMode(StateInjectionMiddleware.InjectionMode.SUFFIX)
                .template("""
                        === 重要提示 ===
                        当前用户：测试用户
                        请用简短的语言回答。
                        ====================
                        """)
                .build();

        Agent agent = DefaultAgentFactory.agent()
                .model(model)
                .middleware(stateInjection)
                .build();

        System.out.println("=== StateInjectionMiddleware 测试 ===");
        AgentResponse response = agent.invoke(AgentRequest.of("什么是Java？"));

        assertNotNull(response);
        System.out.println("响应: " + response.getOutput());
        // 注意：LLM 输出长度不可预测，不强制验证长度
        assertTrue(response.getOutput().length() > 0, "响应不应该为空");
    }

    /**
     * 测试 HumanInTheLoopMiddleware - 人机交互
     */
    @Test
    void testHumanInTheLoop() throws AgentExecutionException {
        // 创建人机交互中间件 - 审核所有工具调用
        Middleware humanInTheLoop = HumanInTheLoopMiddleware.builder()
                .mode(HumanInTheLoopMiddleware.ReviewMode.ALWAYS)
                .build();

        Checkpointer checkpointer = new InMemorySaver();

        Agent agent = DefaultAgentFactory.agent()
                .model(model)
                .checkpointer(checkpointer)
                .middleware(humanInTheLoop)
                .build();

        String threadId = "test-approval-001";

        System.out.println("=== HumanInTheLoopMiddleware 测试 ===");
        System.out.println("发送请求: 请帮我计算 25 * 4");

        AgentRequest request = AgentRequest.builder()
                .input("请帮我计算 25 * 4")
                .threadId(threadId)
                .build();

        AgentResponse response = agent.invoke(request);

        // 应该返回等待审批的响应
        assertNotNull(response);
        assertTrue(response.requiresApproval(), "应该需要审批");

        ApprovalRequest approval = response.getApprovalRequest();
        assertNotNull(approval);
        assertNotNull(approval.getCheckpointId());

        System.out.println("需要审批: " + approval.getToolName());
        System.out.println("Checkpoint ID: " + approval.getCheckpointId());

        // 模拟用户审批通过
        System.out.println("\n用户批准了操作...");
        approval.setDecision(ApprovalRequest.ApprovalDecision.ALLOW);

        AgentResponse finalResponse = agent.resume(approval.getCheckpointId(), approval);

        assertNotNull(finalResponse);
        assertFalse(finalResponse.requiresApproval(), "最终响应不应该需要审批");
        System.out.println("最终响应: " + finalResponse.getOutput());
        assertTrue(finalResponse.getOutput().contains("100"), "应该包含正确答案 100");
    }

    /**
     * 测试流式执行
     */
    @Test
    void testStreamingExecution() throws InterruptedException, AgentExecutionException {
        CountDownLatch latch = new CountDownLatch(1);
        StringBuilder fullContent = new StringBuilder();
        AtomicInteger tokenCount = new AtomicInteger(0);

        Agent agent = DefaultAgentFactory.agent()
                .model(model)
                .config(AgentConfig.builder()
                        .systemPrompt("你是一个简洁的AI助手。")
                        .build())
                .build();

        System.out.println("=== 流式执行测试 ===");
        System.out.println("发送请求: 用三个词描述春天");

        agent.stream(AgentRequest.of("用三个词描述春天"), new StreamCallbackAdapter() {
            @Override
            public void onStart() {
                System.out.println("[流式开始]");
            }

            @Override
            public void onToken(StreamChunk chunk) {
                String token = chunk.getContent();
                if (token != null && !token.isEmpty()) {
                    System.out.print(token);
                    fullContent.append(token);
                    tokenCount.incrementAndGet();
                }
            }

            @Override
            public void onCompletion(StreamCompleteResponse response) {
                System.out.println("\n[流式完成]");
                System.out.println("完整内容: " + response.getFullContent());
                System.out.println("Token 数量: " + tokenCount.get());
                latch.countDown();
            }

            @Override
            public void onError(Throwable error) {
                System.err.println("[流式错误]: " + error.getMessage());
                latch.countDown();
            }
        });

        assertTrue(latch.await(30, TimeUnit.SECONDS), "流式执行应在 30 秒内完成");
        assertTrue(fullContent.length() > 0, "应该收到完整内容");
        assertTrue(tokenCount.get() > 0, "应该收到至少一个 token");
    }

    /**
     * 测试中间件链 - 多个中间件组合
     */
    @Test
    void testMiddlewareChain() throws AgentExecutionException {
        AtomicInteger step = new AtomicInteger(0);

        Middleware middleware1 = new Middleware() {
            @Override
            public void beforeInvoke(AgentRequest request, AgentContext context) {
                System.out.println("[中间件1] beforeInvoke - 步骤: " + step.incrementAndGet());
            }

            @Override
            public void afterInvoke(AgentResponse response, AgentContext context) {
                System.out.println("[中间件1] afterInvoke");
            }
        };

        Middleware middleware2 = new Middleware() {
            @Override
            public void beforeInvoke(AgentRequest request, AgentContext context) {
                System.out.println("[中间件2] beforeInvoke - 步骤: " + step.incrementAndGet());
            }

            @Override
            public void afterInvoke(AgentResponse response, AgentContext context) {
                System.out.println("[中间件2] afterInvoke");
            }
        };

        Agent agent = DefaultAgentFactory.agent()
                .model(model)
                .middlewares(List.of(middleware1, middleware2))
                .build();

        System.out.println("=== 中间件链测试 ===");
        AgentResponse response = agent.invoke(AgentRequest.of("1+1=?"));

        assertNotNull(response);
        assertEquals(2, step.get(), "应该执行了两个中间件的 beforeInvoke");
        assertTrue(response.getOutput().contains("2"));
    }

    /**
     * 测试 AgentContext 状态更新
     */
    @Test
    void testAgentContextStatusUpdate() throws AgentExecutionException {
        List<AgentStatus.StatusType> statusTransitions = new java.util.ArrayList<>();

        Middleware statusTracker = new Middleware() {
            @Override
            public void beforeInvoke(AgentRequest request, AgentContext context) {
                statusTransitions.add(context.getStatus().getStatusType());
                System.out.println("[状态跟踪] beforeInvoke: " + context.getStatus().getStatusType());
            }

            @Override
            public void afterInvoke(AgentResponse response, AgentContext context) {
                statusTransitions.add(context.getStatus().getStatusType());
                System.out.println("[状态跟踪] afterInvoke: " + context.getStatus().getStatusType());
            }
        };

        Agent agent = DefaultAgentFactory.agent()
                .model(model)
                .middleware(statusTracker)
                .build();

        System.out.println("=== AgentContext 状态更新测试 ===");
        AgentResponse response = agent.invoke(AgentRequest.of("你好"));

        assertNotNull(response);
        assertTrue(statusTransitions.contains(AgentStatus.StatusType.RUNNING),
                "应该有 RUNNING 状态");
        assertTrue(statusTransitions.contains(AgentStatus.StatusType.IDLE),
                "应该有 IDLE 状态");

        System.out.println("状态转换序列: " + statusTransitions);
    }
}
