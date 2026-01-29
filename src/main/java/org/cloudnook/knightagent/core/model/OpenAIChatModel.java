package org.cloudnook.knightagent.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.cloudnook.knightagent.core.message.AIMessage;
import org.cloudnook.knightagent.core.message.Message;
import org.cloudnook.knightagent.core.message.ToolCall;
import org.cloudnook.knightagent.core.message.ToolMessage;
import org.cloudnook.knightagent.core.model.base.BaseChatModel;
import org.cloudnook.knightagent.core.streaming.StreamChunk;
import org.cloudnook.knightagent.core.streaming.StreamCallback;
import org.cloudnook.knightagent.core.streaming.StreamCompleteResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * OpenAI 聊天模型实现
 * <p>
 * 支持 OpenAI GPT-4、GPT-3.5 等模型的调用。
 * 使用 Java 11+ HttpClient，支持同步和流式调用。
 * <p>
 * <b>线程安全：</b>此类<b>不是线程安全的</b>。
 * 每次调用 {@link #chat(List, ChatOptions)} 或 {@link #chatStream(List, ChatOptions, StreamCallback)}
 * 时，会使用实例变量来跟踪工具调用状态。
 * 因此，<b>不支持同一个实例的并发调用</b>。
 * <p>
 * 推荐的使用方式：
 * <ul>
 *   <li>为每个请求/线程创建新的 Model 实例</li>
   *   <li>或使用依赖注入框架（如 Spring）为每个请求/作用域注入新的实例</li>
 * </ul>
 * <p>
 * 使用示例：
 * <pre>{@code
 * OpenAIChatModel model = OpenAIChatModel.builder()
 *     .apiKey("sk-xxx")
 *     .modelId("gpt-4")
 *     .build();
 *
 * // 同步调用
 * AIMessage response = model.chat(List.of(HumanMessage.of("你好")));
 *
 * // 流式调用
 * model.chatStream(List.of(HumanMessage.of("你好")), new StreamCallbackAdapter() {
 *     @Override
 *     public void onToken(StreamChunk chunk) {
 *         System.out.print(chunk.getContent());
 *     }
 * });
 * }</pre>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
@Slf4j
public class OpenAIChatModel extends BaseChatModel {

    private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
    private static final String DEFAULT_MODEL = "gpt-3.5-turbo";
    private static final String CHAT_ENDPOINT = "/chat/completions";

    private final String apiKey;
    private final String baseUrl;
    private final String organizationId;

    // ==================== 工具调用增量状态管理 ====================
    // 每次流式调用的临时状态，使用实例变量而非 ThreadLocal
    // 在 chatStream 开始时清理，避免线程池复用时的状态污染
    private final Map<Integer, ToolCallDelta> toolCallDeltas = new HashMap<>();
    private final Set<String> triggeredToolCalls = new HashSet<>();

    private OpenAIChatModel(Builder builder) {
        super(builder);
        this.apiKey = builder.apiKey;
        this.baseUrl = builder.baseUrl != null ? builder.baseUrl : DEFAULT_BASE_URL;
        this.organizationId = builder.organizationId;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public AIMessage chat(List<Message> messages, ChatOptions options) {
        if (options == null) {
            options = ChatOptions.defaults();
        }
        options.validate();

        try {
            ChatRequest request = buildRequest(messages, options, false);
            HttpRequest httpRequest = buildHttpRequest(request, false);

            log.debug("Sending request to OpenAI: model={}, messages={}", modelId, messages.size());

            HttpResponse<String> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString()
            );

            return handleResponse(response.body());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ModelException("Failed to call OpenAI API: " + e.getMessage(), e);
        }
    }

    @Override
    public void chatStream(List<Message> messages, ChatOptions options, StreamCallback callback) {
        // 清理上次调用可能残留的工具调用状态
        toolCallDeltas.clear();
        triggeredToolCalls.clear();

        if (options == null) {
            options = ChatOptions.defaults();
        }
        options.validate();

        try {
            callback.onStart();
        } catch (Exception e) {
            log.error("Error in onStart callback", e);
        }

        try {
            ChatRequest request = buildRequest(messages, options, true);
            HttpRequest httpRequest = buildHttpRequest(request, true);

            log.debug("Sending streaming request to OpenAI: model={}, messages={}", modelId, messages.size());

            HttpResponse<InputStream> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                String errorBody;
                try (InputStream is = response.body()) {
                    errorBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
                throw new ModelException("OpenAI API returned status " + response.statusCode() + ": " + errorBody);
            }

            // 解析 SSE 流，返回完整响应
            StreamCompleteResponse completeResponse = parseSSEStream(response.body(), callback);

            try {
                callback.onCompletion(completeResponse);
            } catch (Exception e) {
                log.error("Error in onCompletion callback", e);
            }

        } catch (Exception e) {
            try {
                callback.onError(e);
            } catch (Exception ignored) {
            }
            throw new ModelException("Failed to call OpenAI API: " + e.getMessage(), e);
        }
    }

    // ==================== Builder ====================

    public static class Builder extends BaseBuilder<Builder> {

        private String apiKey;
        private String baseUrl;
        private String organizationId;

        @Override
        protected Builder self() {
            return this;
        }

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder modelId(String modelId) {
            this.modelId = modelId;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder organizationId(String organizationId) {
            this.organizationId = organizationId;
            return this;
        }

        @Override
        public Builder httpClient(java.net.http.HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        @Override
        public Builder objectMapper(com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            return this;
        }

        public OpenAIChatModel build() {
            if (apiKey == null || apiKey.isEmpty()) {
                throw new IllegalArgumentException("API key is required");
            }
            if (modelId == null || modelId.isEmpty()) {
                this.modelId = DEFAULT_MODEL;
            }
            return new OpenAIChatModel(this);
        }
    }

    // ==================== 内部方法 ====================

    /**
     * 构建请求对象
     */
    private ChatRequest buildRequest(List<Message> messages, ChatOptions options, boolean stream) {
        List<ChatMessage> chatMessages = new ArrayList<>();

        if (options.getSystemPrompt() != null) {
            chatMessages.add(new ChatMessage("system", options.getSystemPrompt(), null, null, null));
        }

        for (Message message : messages) {
            chatMessages.add(convertMessage(message));
        }

        return ChatRequest.builder()
                .model(modelId)
                .messages(chatMessages)
                .stream(stream)
                .temperature(options.getTemperature())
                .topP(options.getTopP())
                .maxTokens(options.getMaxTokens())
                .stop(options.getStopSequences())
                .tools(options.getTools() != null ? convertTools(options.getTools()) : null)
                .toolChoice(options.getTools() != null ? "auto" : null)
                .build();
    }

    /**
     * 转换消息格式
     */
    private ChatMessage convertMessage(Message message) {
        String role = switch (message.getType()) {
            case SYSTEM -> "system";
            case HUMAN -> "user";
            case AI -> "assistant";
            case TOOL -> "tool";
        };

        if (message instanceof AIMessage aiMessage && aiMessage.hasToolCalls()) {
            return new ChatMessage(role, aiMessage.getContent(), null, null, convertToolCalls(aiMessage.getToolCalls()));
        }

        if (message instanceof ToolMessage toolMessage) {
            return new ChatMessage(role, toolMessage.getContent(), null, toolMessage.getToolCallId(), null);
        }

        return new ChatMessage(role, message.getContent(), null, null, null);
    }

    /**
     * 转换工具调用格式
     */
    private List<ChatToolCall> convertToolCalls(List<ToolCall> toolCalls) {
        List<ChatToolCall> result = new ArrayList<>();
        for (ToolCall toolCall : toolCalls) {
            ChatToolCallFunction function = new ChatToolCallFunction(toolCall.getName(), toolCall.getArguments());
            result.add(new ChatToolCall(toolCall.getId(), "function", function));
        }
        return result;
    }

    /**
     * 构建 HTTP 请求
     */
    private HttpRequest buildHttpRequest(ChatRequest request, boolean stream) throws IOException {
        String requestBody = objectMapper.writeValueAsString(request);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(buildUrl(baseUrl, CHAT_ENDPOINT))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(60));

        if (organizationId != null && !organizationId.isEmpty()) {
            builder.header("OpenAI-Organization", organizationId);
        }

        return builder
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
    }

    /**
     * 处理同步响应
     */
    private AIMessage handleResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            if (root.has("error")) {
                JsonNode error = root.get("error");
                String message = error.has("message") ? error.get("message").asText() : "Unknown error";
                String type = error.has("type") ? error.get("type").asText() : "unknown";
                throw new ModelException("OpenAI API error [" + type + "]: " + message);
            }

            JsonNode choice = root.get("choices").get(0);
            JsonNode messageNode = choice.get("message");

            String content = messageNode.has("content") && !messageNode.get("content").isNull()
                    ? messageNode.get("content").asText()
                    : "";

            List<ToolCall> toolCalls = new ArrayList<>();
            if (messageNode.has("tool_calls") && !messageNode.get("tool_calls").isNull()) {
                for (JsonNode toolCallNode : messageNode.get("tool_calls")) {
                    String id = toolCallNode.get("id").asText();
                    String name = toolCallNode.get("function").get("name").asText();
                    String arguments = toolCallNode.get("function").get("arguments").asText();
                    toolCalls.add(ToolCall.of(id, name, arguments));
                }
            }

            Integer usageTokens = null;
            if (root.has("usage")) {
                JsonNode usage = root.get("usage");
                if (usage.has("total_tokens")) {
                    usageTokens = usage.get("total_tokens").asInt();
                }
            }

            return AIMessage.builder()
                    .content(content)
                    .toolCalls(toolCalls)
                    .usageTokens(usageTokens)
                    .build();

        } catch (IOException e) {
            throw new ModelException("Failed to parse OpenAI response: " + e.getMessage(), e);
        }
    }

    /**
     * 解析 SSE (Server-Sent Events) 流
     * <p>
     * 使用 BufferedReader 逐行读取，避免 UTF-8 多字节字符被截断
     *
     * @param inputStream 输入流
     * @param callback    流式回调
     * @return 完整响应对象（包含累积的内容和工具调用）
     * @throws IOException 读取失败
     */
    private StreamCompleteResponse parseSSEStream(InputStream inputStream, StreamCallback callback) throws IOException {
        // 累积器：收集完整内容
        StringBuilder fullContent = new StringBuilder();
        List<org.cloudnook.knightagent.core.message.ToolCall> toolCalls = new ArrayList<>();
        StreamChunk lastChunk = null;

        // 使用 BufferedReader 按行读取，避免 UTF-8 多字节字符截断问题
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                StreamChunk chunk = processSSELine(line, callback, fullContent, toolCalls);
                // 记录最后一个 chunk（用于获取 finish_reason 和 usage）
                if (chunk != null) {
                    lastChunk = chunk;
                }
            }
        }

        // 构建完整响应
        return StreamCompleteResponse.builder()
                .fullContent(fullContent.toString())
                .toolCalls(toolCalls)
                .finishReason(lastChunk != null ? lastChunk.getFinishReason() : "unknown")
                .usage(lastChunk != null ? lastChunk.getUsage() : null)
                .model(lastChunk != null ? lastChunk.getModel() : modelId)
                .id(lastChunk != null ? lastChunk.getId() : null)
                .build();
    }

    /**
     * 处理单行 SSE 数据
     * <p>
     * OpenAI SSE 流格式：
     * <pre>{@code
     * data: {
     *   "id": "chatcmpl-123",
     *   "object": "chat.completion.chunk",
     *   "created": 1694268190,
     *   "model": "gpt-3.5-turbo-0125",
     *   "choices": [{
     *     "index": 0,
     *     "delta": {"content": "Hello"},
     *     "finish_reason": null
     *   }],
     *   "usage": null
     * }
     * }</pre>
     *
     * @param line           一行数据
     * @param callback       流式回调
     * @param fullContent    累积完整内容
     * @param toolCalls      累积工具调用列表
     * @return 构建的 StreamChunk，如果非 data 行返回 null
     */
    private StreamChunk processSSELine(String line, StreamCallback callback,
                                       StringBuilder fullContent,
                                       List<org.cloudnook.knightagent.core.message.ToolCall> toolCalls) {
        // 跳过非 data 开头的行
        if (!line.startsWith("data: ")) {
            return null;
        }

        // 提取 JSON 数据部分
        String data = line.substring(6).trim();

        // [DONE] 表示流结束
        if ("[DONE]".equals(data)) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(data);
            JsonNode choices = root.get("choices");
            if (choices == null || choices.isEmpty()) {
                return null;
            }

            JsonNode choice = choices.get(0);
            JsonNode delta = choice.get("delta");

            // 构建 StreamChunk
            StreamChunk.StreamChunkBuilder chunkBuilder = StreamChunk.builder()
                    .id(root.has("id") ? root.get("id").asText() : null)
                    .model(root.has("model") ? root.get("model").asText() : null)
                    .created(root.has("created") ? root.get("created").asLong() : null)
                    .finishReason(choice.has("finish_reason") && !choice.get("finish_reason").isNull()
                            ? choice.get("finish_reason").asText() : null);

            // 处理 usage（只在最后一个 chunk 存在）
            if (root.has("usage") && !root.get("usage").isNull()) {
                JsonNode usageNode = root.get("usage");
                chunkBuilder.usage(StreamChunk.Usage.builder()
                        .promptTokens(usageNode.has("prompt_tokens") ? usageNode.get("prompt_tokens").asInt() : null)
                        .completionTokens(usageNode.has("completion_tokens") ? usageNode.get("completion_tokens").asInt() : null)
                        .totalTokens(usageNode.has("total_tokens") ? usageNode.get("total_tokens").asInt() : null)
                        .build());
            }

            // 处理内容 token
            if (delta != null && delta.has("content") && !delta.get("content").isNull()) {
                String token = delta.get("content").asText();
                chunkBuilder.content(token);

                // 累积完整内容
                fullContent.append(token);

                try {
                    callback.onToken(chunkBuilder.build());
                } catch (Exception e) {
                    log.error("Error in onToken callback", e);
                }
                return chunkBuilder.build();
            }

            // 处理工具调用
            if (delta != null && delta.has("tool_calls") && !delta.get("tool_calls").isNull()) {
                for (JsonNode toolCallNode : delta.get("tool_calls")) {
                    processToolCallDelta(toolCallNode, callback, chunkBuilder.build(), toolCalls);
                }
                return chunkBuilder.build();
            }

            return chunkBuilder.build();

        } catch (Exception e) {
            // SSE 解析失败，记录日志但不中断流
            log.error("Failed to parse SSE data: {}", data, e);
            return null;
        }
    }


    /**
     * 处理工具调用的增量更新
     * <p>
     * OpenAI SSE 流中，工具调用是分片传输的，需要累积拼接
     *
     * @param toolCallNode 工具调用增量节点
     * @param callback     流式回调
     * @param chunk        当前数据块
     * @param toolCalls    累积工具调用列表
     */
    private void processToolCallDelta(JsonNode toolCallNode, StreamCallback callback, StreamChunk chunk,
                                       List<org.cloudnook.knightagent.core.message.ToolCall> toolCalls) {
        int index = toolCallNode.get("index").asInt();

        // 获取当前索引的增量数据
        String id = toolCallNode.has("id") ? toolCallNode.get("id").asText() : null;
        String name = null;
        String argsDelta = null;

        if (toolCallNode.has("function")) {
            JsonNode function = toolCallNode.get("function");
            if (function.has("name")) {
                name = function.get("name").asText();
            }
            if (function.has("arguments")) {
                argsDelta = function.get("arguments").asText();
            }
        }

        // 累积完整的工具调用信息
        String callId = getToolCallId(index, id);
        String callName = getToolCallName(index, name);
        String callArgs = getToolCallArgs(index, argsDelta);

        // 只有当 arguments 形成完整 JSON 时才触发回调
        if (callArgs != null && isValidJson(callArgs)) {
            ToolCall toolCall = ToolCall.of(callId, callName, callArgs);

            // 检查是否已触发过，避免重复
            if (!triggeredToolCalls.contains(callId)) {
                // 添加到完整响应列表
                toolCalls.add(toolCall);

                try {
                    callback.onToolCall(chunk, toolCall);
                    triggeredToolCalls.add(callId);
                } catch (Exception e) {
                    log.error("Error in onToolCall callback", e);
                }
            }
        }
    }

    /**
     * 工具调用增量数据
     */
    private static class ToolCallDelta {
        String id;
        String name;
        StringBuilder arguments = new StringBuilder();
    }

    /**
     * 获取或创建工具调用增量数据
     */
    private ToolCallDelta getToolCallDelta(int index) {
        return toolCallDeltas.computeIfAbsent(index, k -> new ToolCallDelta());
    }

    /**
     * 获取工具调用 ID
     */
    private String getToolCallId(int index, String id) {
        if (id != null && !id.isEmpty()) {
            ToolCallDelta delta = getToolCallDelta(index);
            delta.id = id;
            return id;
        }
        return getToolCallDelta(index).id;
    }

    /**
     * 获取工具调用名称
     */
    private String getToolCallName(int index, String name) {
        if (name != null && !name.isEmpty()) {
            ToolCallDelta delta = getToolCallDelta(index);
            delta.name = name;
            return name;
        }
        return getToolCallDelta(index).name;
    }

    /**
     * 获取工具调用参数（累积）
     */
    private String getToolCallArgs(int index, String argsDelta) {
        if (argsDelta != null && !argsDelta.isEmpty()) {
            ToolCallDelta delta = getToolCallDelta(index);
            delta.arguments.append(argsDelta);
            return delta.arguments.toString();
        }
        ToolCallDelta delta = getToolCallDelta(index);
        return delta.arguments.length() > 0 ? delta.arguments.toString() : null;
    }

    /**
     * 检查是否为有效的 JSON
     *
     * @param json JSON 字符串
     * @return true 如果有效
     */
    private boolean isValidJson(String json) {
        if (json == null || json.isEmpty()) {
            return false;
        }
        try {
            objectMapper.readTree(json);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 转换工具列表为 OpenAI 格式
     */
    private List<ChatTool> convertTools(List<org.cloudnook.knightagent.core.tool.McpTool> tools) {
        List<ChatTool> result = new ArrayList<>();
        for (org.cloudnook.knightagent.core.tool.McpTool tool : tools) {
            ChatToolFunction function = new ChatToolFunction(
                    tool.getName(),
                    tool.getDescription(),
                    tool.getParameters()
            );
            result.add(new ChatTool("function", function));
        }
        return result;
    }

    // ==================== DTO 类 ====================

    /**
     * 聊天请求 DTO
     */
    @lombok.Data
    @lombok.Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class ChatRequest {
        private final String model;
        private final List<ChatMessage> messages;
        private final Boolean stream;
        private final Double temperature;
        @JsonProperty("top_p")
        private final Double topP;
        @JsonProperty("max_tokens")
        private final Integer maxTokens;
        private final List<String> stop;
        private final List<ChatTool> tools;
        @JsonProperty("tool_choice")
        private final String toolChoice;
    }

    /**
     * 聊天消息 DTO
     */
    @lombok.Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class ChatMessage {
        private final String role;
        private final String content;
        private final String name;
        @JsonProperty("tool_call_id")
        private final String toolCallId;
        @JsonProperty("tool_calls")
        private final List<ChatToolCall> toolCalls;
    }

    /**
     * 工具定义 DTO
     */
    @lombok.Data
    private static class ChatTool {
        private final String type;
        private final ChatToolFunction function;
    }

    /**
     * 工具调用 DTO
     */
    @lombok.Data
    private static class ChatToolCall {
        private final String id;
        private final String type;
        @JsonProperty("function")
        private final ChatToolCallFunction function;
    }

    /**
     * 工具函数定义 DTO
     */
    @lombok.Data
    private static class ChatToolFunction {
        private final String name;
        private final String description;
        private final Object parameters;
    }

    /**
     * 工具调用函数 DTO
     */
    @lombok.Data
    private static class ChatToolCallFunction {
        private final String name;
        private final String arguments;
    }
}
