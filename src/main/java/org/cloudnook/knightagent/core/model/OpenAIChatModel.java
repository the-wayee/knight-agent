package org.cloudnook.knightagent.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.cloudnook.knightagent.core.message.AIMessage;
import org.cloudnook.knightagent.core.message.HumanMessage;
import org.cloudnook.knightagent.core.message.Message;
import org.cloudnook.knightagent.core.message.SystemMessage;
import org.cloudnook.knightagent.core.message.ToolCall;
import org.cloudnook.knightagent.core.streaming.StreamCallback;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * OpenAI 聊天模型实现
 * <p>
 * 支持 OpenAI GPT-4、GPT-3.5 等模型的调用。
 * 使用 Java 11+ HttpClient，支持同步和流式调用。
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
 *     public void onToken(String token) {
 *         System.out.print(token);
 *     }
 * });
 * }</pre>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
@Slf4j
public class OpenAIChatModel implements ChatModel {

    private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
    private static final String DEFAULT_MODEL = "gpt-3.5-turbo";
    private static final String CHAT_ENDPOINT = "/chat/completions";

    private final String apiKey;
    private final String modelId;
    private final String baseUrl;
    private final String organizationId;
    private final ModelCapabilities capabilities;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private OpenAIChatModel(Builder builder) {
        this.apiKey = builder.apiKey;
        this.modelId = builder.modelId != null ? builder.modelId : DEFAULT_MODEL;
        this.baseUrl = builder.baseUrl != null ? builder.baseUrl : DEFAULT_BASE_URL;
        this.organizationId = builder.organizationId;
        this.capabilities = builder.capabilities != null ? builder.capabilities : inferCapabilities(this.modelId);
        this.httpClient = builder.httpClient != null ? builder.httpClient : createDefaultHttpClient();
        this.objectMapper = builder.objectMapper != null ? builder.objectMapper : createDefaultObjectMapper();
    }

    private static HttpClient createDefaultHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    private static ObjectMapper createDefaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
    }

    private static ModelCapabilities inferCapabilities(String modelId) {
        if (modelId.contains("gpt-4")) {
            if (modelId.contains("vision") || modelId.contains("o1")) {
                return ModelCapabilities.builder()
                        .maxContextTokens(128000)
                        .maxOutputTokens(8192)
                        .supportsStreaming(true)
                        .supportsToolCalling(true)
                        .supportsParallelToolCalling(true)
                        .supportsSystemPrompt(true)
                        .supportsMultimodal(true)
                        .family("gpt")
                        .build();
            }
            return ModelCapabilities.gpt4();
        }
        return ModelCapabilities.gpt35();
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

            StringBuilder contentBuilder = new StringBuilder();
            List<ToolCall> toolCalls = new ArrayList<>();

            HttpResponse<InputStream> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                String errorBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                throw new ModelException("OpenAI API returned status " + response.statusCode() + ": " + errorBody);
            }

            parseStream(response.body(), callback, contentBuilder, toolCalls);

            try {
                callback.onComplete();
            } catch (Exception e) {
                log.error("Error in onComplete callback", e);
            }

        } catch (Exception e) {
            try {
                callback.onError(e);
            } catch (Exception ignored) {
            }
            throw new ModelException("Failed to call OpenAI API: " + e.getMessage(), e);
        }
    }

    @Override
    public int countTokens(String text) {
        int charCount = text.length();
        int nonAscii = 0;
        for (char c : text.toCharArray()) {
            if (c > 127) {
                nonAscii++;
            }
        }
        return (charCount - nonAscii) / 4 + nonAscii / 2 + 1;
    }

    @Override
    public ModelCapabilities getCapabilities() {
        return capabilities;
    }

    @Override
    public String getModelId() {
        return modelId;
    }

    // ==================== Builder ====================

    public static class Builder {
        private String apiKey;
        private String modelId;
        private String baseUrl;
        private String organizationId;
        private ModelCapabilities capabilities;
        private HttpClient httpClient;
        private ObjectMapper objectMapper;

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

        public Builder capabilities(ModelCapabilities capabilities) {
            this.capabilities = capabilities;
            return this;
        }

        public Builder httpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public Builder objectMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            return this;
        }

        public OpenAIChatModel build() {
            if (apiKey == null || apiKey.isEmpty()) {
                throw new IllegalArgumentException("API key is required");
            }
            return new OpenAIChatModel(this);
        }
    }

    // ==================== 内部方法 ====================

    private ChatRequest buildRequest(List<Message> messages, ChatOptions options, boolean stream) {
        List<ChatMessage> chatMessages = new ArrayList<>();

        if (options.getSystemPrompt() != null) {
            chatMessages.add(new ChatMessage("system", options.getSystemPrompt(), null, null, null));
        }

        for (Message message : messages) {
            chatMessages.add(convertMessage(message));
        }

        return new ChatRequest(
                modelId,
                chatMessages,
                stream,
                options.getTemperature(),
                options.getTopP(),
                options.getMaxTokens(),
                options.getStopSequences()
        );
    }

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

        return new ChatMessage(role, message.getContent(), null, null, null);
    }

    private List<ChatToolCall> convertToolCalls(List<ToolCall> toolCalls) {
        List<ChatToolCall> result = new ArrayList<>();
        for (ToolCall toolCall : toolCalls) {
            ChatToolFunction function = new ChatToolFunction(toolCall.getName(), toolCall.getArguments());
            result.add(new ChatToolCall(toolCall.getId(), "function", function));
        }
        return result;
    }

    private HttpRequest buildHttpRequest(ChatRequest request, boolean stream) throws IOException {
        String requestBody = objectMapper.writeValueAsString(request);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + CHAT_ENDPOINT))
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

    private void parseStream(InputStream inputStream, StreamCallback callback,
                             StringBuilder contentBuilder, List<ToolCall> toolCalls) throws IOException {
        StringBuilder lineBuilder = new StringBuilder();
        byte[] buffer = new byte[8192];
        int bytesRead;

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            String chunk = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
            for (int i = 0; i < chunk.length(); i++) {
                char c = chunk.charAt(i);

                if (c == '\n') {
                    processStreamLine(lineBuilder.toString(), callback, contentBuilder, toolCalls);
                    lineBuilder.setLength(0);
                } else if (c != '\r') {
                    lineBuilder.append(c);
                }
            }
        }

        if (lineBuilder.length() > 0) {
            processStreamLine(lineBuilder.toString(), callback, contentBuilder, toolCalls);
        }
    }

    private void processStreamLine(String line, StreamCallback callback,
                                    StringBuilder contentBuilder, List<ToolCall> toolCalls) {
        if (line.isEmpty() || !line.startsWith("data: ")) {
            return;
        }

        String data = line.substring(6).trim();
        if ("[DONE]".equals(data)) {
            return;
        }

        try {
            JsonNode root = objectMapper.readTree(data);
            JsonNode choices = root.get("choices");
            if (choices == null || choices.isEmpty()) {
                return;
            }

            JsonNode delta = choices.get(0).get("delta");

            if (delta.has("content") && !delta.get("content").isNull()) {
                String token = delta.get("content").asText();
                contentBuilder.append(token);
                try {
                    callback.onToken(token);
                } catch (Exception e) {
                    log.error("Error in onToken callback", e);
                }
            }

            if (delta.has("tool_calls") && !delta.get("tool_calls").isNull()) {
                for (JsonNode toolCallNode : delta.get("tool_calls")) {
                    processToolCallDelta(toolCallNode, toolCalls, callback);
                }
            }

        } catch (Exception e) {
            log.error("Failed to parse SSE data: {}", data, e);
        }
    }

    private void processToolCallDelta(JsonNode toolCallNode, List<ToolCall> toolCalls, StreamCallback callback) {
        int index = toolCallNode.get("index").asInt();

        while (toolCalls.size() <= index) {
            toolCalls.add(ToolCall.of("", "", ""));
        }

        ToolCall toolCall = toolCalls.get(index);

        if (toolCallNode.has("id")) {
            toolCall.setId(toolCallNode.get("id").asText());
        }

        if (toolCallNode.has("function")) {
            JsonNode function = toolCallNode.get("function");
            if (function.has("name")) {
                toolCall.setName(function.get("name").asText());
            }
            if (function.has("arguments")) {
                String currentArgs = toolCall.getArguments();
                String newArgs = function.get("arguments").asText();
                toolCall.setArguments(currentArgs != null ? currentArgs + newArgs : newArgs);
            }

            String args = toolCall.getArguments();
            if (args != null && isValidJson(args)) {
                try {
                    callback.onToolCall(toolCall);
                } catch (Exception e) {
                    log.error("Error in onToolCall callback", e);
                }
            }
        }
    }

    private boolean isValidJson(String json) {
        try {
            objectMapper.readTree(json);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== DTO 类 ====================

    @Data
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
    }

    @Data
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

    @Data
    private static class ChatToolCall {
        private final String id;
        private final String type;
        @JsonProperty("function")
        private final ChatToolFunction function;
    }

    @Data
    private static class ChatToolFunction {
        private final String name;
        private final String arguments;
    }
}
