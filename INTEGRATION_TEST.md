# OpenAI 真实 API 集成测试指南

本文档说明如何运行使用真实 OpenAI API 的集成测试。

## 测试文件

- `OpenAIIntegrationTest.java` - 使用真实 OpenAI API 的集成测试
- `AgentIntegrationTest.java` - 使用 Mock 模型的单元测试

## 运行方式

### 方式一：使用环境变量

```bash
# 设置 API key
export OPENAI_API_KEY=sk-your-api-key-here

# 可选：设置自定义 API 地址（默认：https://api.openai.com/v1）
export OPENAI_BASE_URL=https://api.openai.com/v1

# 可选：设置模型（默认：gpt-3.5-turbo）
export OPENAI_MODEL=gpt-4

# 运行测试
./mvnw test -Dtest=OpenAIIntegrationTest
```

### 方式二：使用 Maven 属性

```bash
./mvnw test -Dtest=OpenAIIntegrationTest \
  -DOPENAI_API_KEY=sk-your-api-key-here \
  -DOPENAI_BASE_URL=https://api.openai.com/v1 \
  -DOPENAI_MODEL=gpt-3.5-turbo
```

### 方式三：在 IDE 中运行

在 IDE（如 IntelliJ IDEA 或 Eclipse）中：

1. 打开运行配置
2. 设置环境变量：
   - `OPENAI_API_KEY=sk-your-api-key-here`
   - `OPENAI_BASE_URL=https://api.openai.com/v1`（可选）
   - `OPENAI_MODEL=gpt-3.5-turbo`（可选）
3. 运行 `OpenAIIntegrationTest` 类

## 测试用例说明

### 基础对话测试
- `testSimpleQA` - 简单问答测试
- `testWithSystemPrompt` - 系统提示词测试

### 多轮对话测试
- `testMultiTurnConversation` - 带历史的多轮对话
- `testContextAccumulation` - 上下文累积测试

### 工具调用测试
- `testSingleToolCall` - 单次工具调用（计算器）
- `testMultipleToolCalls` - 多个工具调用

### 中间件测试
- `testLoggingMiddleware` - 日志中间件
- `testStateInjectionMiddleware` - 状态注入中间件

### 异常处理测试
- `testInvalidApiKey` - 无效 API key
- `testLongInput` - 超长输入

### 流式输出测试
- `testStreamingOutput` - 流式输出测试

## 注意事项

1. **API 费用**：运行真实 API 测试会产生费用，请谨慎使用
2. **网络连接**：确保能够访问 OpenAI API 服务器
3. **跳过条件**：如果没有设置 API key，测试会自动跳过
4. **并发限制**：注意 OpenAI API 的速率限制

## 使用 Mock 模型测试

如果不想使用真实 API，可以运行 Mock 测试：

```bash
./mvnw test -Dtest=AgentIntegrationTest
```

Mock 测试不需要 API key，可以离线运行。
