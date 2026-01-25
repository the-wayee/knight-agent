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
import org.cloudnook.knightagent.core.middleware.Middleware;
import org.cloudnook.knightagent.core.middleware.builtin.StateInjectionMiddleware;
import org.cloudnook.knightagent.core.middleware.builtin.LoggingMiddleware;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * StateInjectionMiddleware 使用示例
 * <p>
 * 演示如何使用状态注入中间件动态增强系统提示词。
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public class StateInjectionExample {

    /**
     * 示例 1：基础的用户信息注入（SUFFIX 模式）
     */
    public static void example1_BasicUserInjection() throws AgentExecutionException {
        System.out.println("=== 示例 1：基础用户信息注入 ===\n");

        // 创建模型
        ChatModel model = createMockModel();

        // 创建 Agent，使用 SUFFIX 模式追加用户信息
        Agent agent = new DefaultAgentFactory().createAgent()
                .model(model)
                .config(AgentConfig.builder()
                        .systemPrompt("你是一个友好的 AI 助手。")
                        .build())
                .middleware(StateInjectionMiddleware.builder()
                        .injectionMode(StateInjectionMiddleware.InjectionMode.SUFFIX)
                        .template("""
                            === 用户信息 ===
                            用户名：${request:userName}
                            用户级别：${request:userLevel}
                            =====================
                            """)
                        .build())
                .build();

        // 通过 parameters 传递用户信息
        Map<String, Object> params = Map.of(
                "userName", "张三",
                "userLevel", "VIP会员"
        );

        // 执行调用
        AgentRequest request = AgentRequest.builder()
                .input("你好，请介绍一下你自己")
                .parameters(params)
                .build();

        AgentResponse response = agent.invoke(request);
        System.out.println("响应: " + response.getOutput());
        System.out.println();
    }

    /**
     * 示例 2：变量替换模式（REPLACE 模式）
     */
    public static void example2_VariableReplacement() throws AgentExecutionException {
        System.out.println("=== 示例 2：变量替换模式 ===\n");

        ChatModel model = createMockModel();

        // 在系统提示词中使用变量占位符
        Agent agent = new DefaultAgentFactory().createAgent()
                .model(model)
                .config(AgentConfig.builder()
                        .systemPrompt("""
                            你是一个智能客服助手，正在为用户 ${request:userName} 提供服务。
                            该用户的偏好是：${request:language} 风格，${request:responseStyle} 回复。
                            用户积分：${request:points} 分
                            """)
                        .build())
                .middleware(StateInjectionMiddleware.builder()
                        .injectionMode(StateInjectionMiddleware.InjectionMode.REPLACE)
                        .template("")  // REPLACE 模式使用空模板，直接替换系统提示词中的变量
                        .build())
                .build();

        // 设置参数
        Map<String, Object> params = Map.of(
                "userName", "李四",
                "language", "中文",
                "responseStyle", "简洁",
                "points", "5000"
        );

        AgentRequest request = AgentRequest.builder()
                .input("查询我的积分")
                .parameters(params)
                .build();

        AgentResponse response = agent.invoke(request);
        System.out.println("响应: " + response.getOutput());
        System.out.println();
    }

    /**
     * 示例 3：前缀注入（PREFIX 模式）
     */
    public static void example3_PrefixInjection() throws AgentExecutionException {
        System.out.println("=== 示例 3：前缀注入 ===\n");

        ChatModel model = createMockModel();

        Agent agent = new DefaultAgentFactory().createAgent()
                .model(model)
                .config(AgentConfig.builder()
                        .systemPrompt("你是一个Java技术专家。")
                        .build())
                .middleware(StateInjectionMiddleware.builder()
                        .injectionMode(StateInjectionMiddleware.InjectionMode.PREFIX)
                        .template("""
                            === 会话上下文 ===
                            会话ID：${request:sessionId}
                            开始时间：${request:startTime}
                            当前时间：${request:currentTime}
                            ===============
                            """)
                        .build())
                .build();

        Map<String, Object> params = Map.of(
                "sessionId", "SESSION-2024-001",
                "startTime", "2024-01-25 10:30:00",
                "currentTime", java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );

        AgentRequest request = AgentRequest.builder()
                .input("Java 17 有什么新特性？")
                .parameters(params)
                .build();

        AgentResponse response = agent.invoke(request);
        System.out.println("响应: " + response.getOutput());
        System.out.println();
    }

    /**
     * 示例 4：自定义注入逻辑
     */
    public static void example4_CustomInjector() throws AgentExecutionException {
        System.out.println("=== 示例 4：自定义注入逻辑 ===\n");

        ChatModel model = createMockModel();

        Agent agent = new DefaultAgentFactory().createAgent()
                .model(model)
                .config(AgentConfig.builder()
                        .systemPrompt("你是一个智能助手。")
                        .build())
                .middleware(StateInjectionMiddleware.builder()
                        .customInjector((ctx) -> {
                            // 自定义复杂的注入逻辑
                            StringBuilder context = new StringBuilder();

                            // 从请求参数获取用户名
                            Object userNameObj = ctx.request().getParameters() != null
                                    ? ctx.request().getParameters().get("userName")
                                    : null;
                            if (userNameObj != null && !userNameObj.toString().isEmpty()) {
                                context.append("用户：").append(userNameObj).append("\n");
                            }

                            // 获取其他参数
                            Object perms = ctx.request().getParameters() != null
                                    ? ctx.request().getParameters().get("permissions")
                                    : null;
                            if (perms instanceof List<?> list) {
                                context.append("权限：").append(list).append("\n");
                            }

                            // 当前时间
                            context.append("时间：")
                                .append(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

                            return context.toString();
                        })
                        .build())
                .build();

        Map<String, Object> params = Map.of(
                "userName", "王五",
                "permissions", List.of("read", "write", "delete")
        );

        AgentRequest request = AgentRequest.builder()
                .input("我能做什么？")
                .parameters(params)
                .build();

        AgentResponse response = agent.invoke(request);
        System.out.println("响应: " + response.getOutput());
        System.out.println();
    }

    /**
     * 示例 5：多数据源组合
     */
    public static void example5_MultipleDataSources() throws AgentExecutionException {
        System.out.println("=== 示例 5：多数据源组合 ===\n");

        ChatModel model = createMockModel();

        Agent agent = new DefaultAgentFactory().createAgent()
                .model(model)
                .config(AgentConfig.builder()
                        .systemPrompt("""
                            用户：${request:userName}
                            来源：${request:source}
                            平台：${request:platform}
                            设备：${request:device}
                            """)
                        .build())
                .middleware(StateInjectionMiddleware.builder()
                        .injectionMode(StateInjectionMiddleware.InjectionMode.REPLACE)
                        .template("")  // REPLACE 模式使用空模板，直接替换系统提示词中的变量
                        .build())
                .build();

        Map<String, Object> params = Map.of(
                "userName", "赵六",
                "source", "移动应用",
                "platform", "iOS",
                "device", "iPhone 15"
        );

        AgentRequest request = AgentRequest.builder()
                .input("你好")
                .parameters(params)
                .build();

        AgentResponse response = agent.invoke(request);
        System.out.println("响应: " + response.getOutput());
        System.out.println();
    }

    /**
     * 示例 6：完整的电商客服 Agent
     */
    public static void example6_ECommerceAgent() throws AgentExecutionException {
        System.out.println("=== 示例 6：电商客服 Agent（完整示例）===\n");

        ChatModel model = createMockModel();

        // 组合多个中间件
        List<Middleware> middlewares = new ArrayList<>();
        middlewares.add(StateInjectionMiddleware.builder()
                .injectionMode(StateInjectionMiddleware.InjectionMode.SUFFIX)
                .template("""
                    === 会员信息 ===
                    用户名：${request:userName}
                    会员等级：${request:memberLevel}
                    积分：${request:points} 分
                    优惠券：${request:couponCount} 张
                    购物车：${request:cartItems} 件商品
                    ====================
                    """)
                .build());
        middlewares.add(LoggingMiddleware.defaults());

        Agent agent = new DefaultAgentFactory().createAgent()
                .model(model)
                .config(AgentConfig.builder()
                        .systemPrompt("你是一个专业的电商客服助手，友好热情。")
                        .build())
                .middlewares(middlewares)
                .build();

        // 多轮对话
        System.out.println("--- 第一轮对话 ---");
        Map<String, Object> params1 = Map.of(
                "userName", "钱七",
                "memberLevel", "钻石会员",
                "points", 25000,
                "couponCount", 5,
                "cartItems", 2
        );
        AgentRequest request1 = AgentRequest.builder()
                .input("我的积分有什么用？")
                .parameters(params1)
                .build();
        AgentResponse response1 = agent.invoke(request1);
        System.out.println("用户: 我的积分有什么用？");
        System.out.println("助手: " + response1.getOutput() + "\n");

        System.out.println("--- 第二轮对话 ---");
        Map<String, Object> params2 = Map.of(
                "userName", "钱七",
                "memberLevel", "钻石会员",
                "points", 25000,
                "couponCount", 5,
                "cartItems", 2
        );
        AgentRequest request2 = AgentRequest.builder()
                .input("我有优惠券吗")
                .parameters(params2)
                .build();
        AgentResponse response2 = agent.invoke(request2);
        System.out.println("用户: 我有优惠券吗");
        System.out.println("助手: " + response2.getOutput() + "\n");

        System.out.println("--- 第三轮对话 ---");
        // 模拟购物车变化
        Map<String, Object> params3 = Map.of(
                "userName", "钱七",
                "memberLevel", "钻石会员",
                "points", 25000,
                "couponCount", 5,
                "cartItems", 5  // 购物车商品变化
        );
        AgentRequest request3 = AgentRequest.builder()
                .input("帮我推荐商品")
                .parameters(params3)
                .build();
        AgentResponse response3 = agent.invoke(request3);
        System.out.println("用户: 帮我推荐商品");
        System.out.println("助手: " + response3.getOutput() + "\n");

        System.out.println();
    }

    /**
     * 示例 7：动态更新参数后的注入
     */
    public static void example7_DynamicParameterUpdate() throws AgentExecutionException {
        System.out.println("=== 示例 7：动态参数更新 ===\n");

        ChatModel model = createMockModel();

        Agent agent = new DefaultAgentFactory().createAgent()
                .model(model)
                .config(AgentConfig.builder()
                        .systemPrompt("你是一个任务提醒助手。")
                        .build())
                .middleware(StateInjectionMiddleware.builder()
                        .injectionMode(StateInjectionMiddleware.InjectionMode.SUFFIX)
                        .template("""
                            === 当前任务列表 ===
                            ${request:taskList}
                            ====================
                            """)
                        .build())
                .build();

        // 初始参数：没有任务
        Map<String, Object> params1 = Map.of("taskList", "暂无任务");
        AgentRequest request1 = AgentRequest.builder()
                .input("有什么任务？")
                .parameters(params1)
                .build();
        AgentResponse response1 = agent.invoke(request1);
        System.out.println("助手: " + response1.getOutput());

        // 更新参数：添加任务
        System.out.println("\n--- 添加任务后 ---");
        Map<String, Object> params2 = Map.of("taskList", "1. 完成报告\n2. 回复邮件\n3. 准备会议");
        AgentRequest request2 = AgentRequest.builder()
                .input("现在有什么任务？")
                .parameters(params2)
                .build();
        AgentResponse response2 = agent.invoke(request2);
        System.out.println("助手: " + response2.getOutput());
        System.out.println();
    }

    /**
     * 示例 8：多轮对话中的动态参数注入
     */
    public static void example8_DynamicParamsInConversation() throws AgentExecutionException {
        System.out.println("=== 示例 8：多轮对话中的动态参数注入 ===\n");

        ChatModel model = createMockModel();

        Agent agent = new DefaultAgentFactory().createAgent()
                .model(model)
                .config(AgentConfig.builder()
                        .systemPrompt("你是一个 AI 助手。")
                        .build())
                .middleware(StateInjectionMiddleware.builder()
                        .injectionMode(StateInjectionMiddleware.InjectionMode.SUFFIX)
                        .template("用户备注：${request:userNotes}")
                        .build())
                .build();

        // 第一轮：没有用户备注
        System.out.println("--- 第一轮：无备注 ---");
        Map<String, Object> params1 = Map.of("userNotes", "（无）");
        AgentRequest request1 = AgentRequest.builder()
                .input("你好")
                .parameters(params1)
                .build();
        AgentResponse response1 = agent.invoke(request1);
        System.out.println("助手: " + response1.getOutput());

        // 第二轮：有用户备注
        System.out.println("\n--- 第二轮：有备注 ---");
        Map<String, Object> params2 = Map.of(
                "userNotes", "用户偏好简洁回复，避免使用emoji"
        );
        AgentRequest request2 = AgentRequest.builder()
                .input("介绍一下Java")
                .parameters(params2)
                .build();
        AgentResponse response2 = agent.invoke(request2);
        System.out.println("助手: " + response2.getOutput());
        System.out.println();
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建 Mock 模型用于测试
     */
    private static ChatModel createMockModel() {
        return new ChatModel() {
            @Override
            public AIMessage chat(List<Message> messages, ChatOptions options) throws ModelException {
                // 简单的 mock 逻辑
                String lastUserMessage = "";
                for (Message msg : messages) {
                    if (msg.getType() == Message.MessageType.HUMAN) {
                        lastUserMessage = msg.getContent();
                    }
                }

                // 根据输入生成简单响应
                String response;
                if (lastUserMessage.contains("介绍")) {
                    response = "我是 AI 助手，很高兴为您服务！";
                } else if (lastUserMessage.contains("积分")) {
                    response = "您的积分可以兑换优惠券、抵扣现金、参与会员活动等。";
                } else if (lastUserMessage.contains("优惠券")) {
                    response = "您有 5 张优惠券可用，包括满减券和折扣券。";
                } else if (lastUserMessage.contains("推荐")) {
                    response = "为您推荐：最新款智能手表、蓝牙耳机、移动电源等热门商品。";
                } else if (lastUserMessage.contains("任务")) {
                    response = "您当前有任务待办。";
                } else if (lastUserMessage.contains("你好")) {
                    response = "您好！有什么可以帮助您的吗？";
                } else {
                    response = "我明白了，还有其他问题吗？";
                }

                return AIMessage.of(response);
            }

            @Override
            public void chatStream(List<Message> messages, ChatOptions options,
                                   org.cloudnook.knightagent.core.streaming.StreamCallback callback) throws ModelException {
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
                return "mock-model";
            }

            @Override
            public boolean isAvailable() {
                return true;
            }
        };
    }

    /**
     * 主方法 - 运行所有示例
     */
    public static void main(String[] args) throws AgentExecutionException {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║     StateInjectionMiddleware 使用示例                       ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();

        example1_BasicUserInjection();
        example2_VariableReplacement();
        example3_PrefixInjection();
        example4_CustomInjector();
        example5_MultipleDataSources();
        example6_ECommerceAgent();
        example7_DynamicParameterUpdate();
        example8_DynamicParamsInConversation();

        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║                    所有示例运行完成                             ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
    }
}
