package org.cloudnook.knightagent.core.model.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.cloudnook.knightagent.core.message.AIMessage;
import org.cloudnook.knightagent.core.message.Message;
import org.cloudnook.knightagent.core.model.ChatModel;
import org.cloudnook.knightagent.core.model.ChatOptions;
import org.cloudnook.knightagent.core.streaming.StreamCallback;

import java.net.http.HttpClient;
import java.net.URI;
import java.time.Duration;
import java.util.List;

/**
 * ChatModel 抽象基类
 * <p>
 * 提供模型实现的通用功能：
 * <ul>
 *   <li>HTTP 客户端管理</li>
 *   <li>ObjectMapper 配置</li>
 *   <li>消息类型转换</li>
 * </ul>
 * <p>
 * 子类只需实现 {@link #chat(List, ChatOptions)} 和
 * {@link #chatStream(List, ChatOptions, StreamCallback)} 方法。
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public abstract class BaseChatModel implements ChatModel {

    protected final String modelId;
    protected final HttpClient httpClient;
    protected final ObjectMapper objectMapper;

    /**
     * 受保护的构造函数
     *
     * @param builder 构建器
     */
    protected BaseChatModel(BaseBuilder<?> builder) {
        this.modelId = builder.modelId;
        this.httpClient = builder.httpClient != null
                ? builder.httpClient
                : createDefaultHttpClient();
        this.objectMapper = builder.objectMapper != null
                ? builder.objectMapper
                : createDefaultObjectMapper();
    }

    // ==================== 抽象方法（子类必须实现）====================

    @Override
    public abstract AIMessage chat(List<Message> messages, ChatOptions options) throws org.cloudnook.knightagent.core.model.ModelException;

    @Override
    public abstract void chatStream(List<Message> messages, ChatOptions options, StreamCallback callback) throws org.cloudnook.knightagent.core.model.ModelException;

    // ==================== 通用实现 ====================

    @Override
    public AIMessage chat(List<Message> messages) throws org.cloudnook.knightagent.core.model.ModelException {
        return chat(messages, ChatOptions.defaults());
    }

    @Override
    public void chatStream(List<Message> messages, StreamCallback callback) throws org.cloudnook.knightagent.core.model.ModelException {
        chatStream(messages, ChatOptions.defaults(), callback);
    }

    @Override
    public String getModelId() {
        return modelId;
    }

    @Override
    public boolean isAvailable() {
        return true; // 默认可用
    }

    @Override
    public void close() {
        // 默认不关闭任何资源
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    // ==================== 工具方法 ====================

    /**
     * 获取消息对应模型的角色名称
     *
     * @param type 消息类型
     * @return 角色名称
     */
    protected String getRoleName(Message.MessageType type) {
        return switch (type) {
            case SYSTEM -> "system";
            case HUMAN -> "user";
            case AI -> "assistant";
            case TOOL -> "tool";
        };
    }

    /**
     * 创建默认 HTTP 客户端
     *
     * @return HTTP 客户端
     */
    protected HttpClient createDefaultHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * 创建默认 ObjectMapper
     *
     * @return ObjectMapper
     */
    protected ObjectMapper createDefaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    /**
     * 构建完整的 API URL
     *
     * @param baseUrl 基础 URL
     * @param endpoint 端点路径
     * @return 完整 URL
     */
    protected URI buildUrl(String baseUrl, String endpoint) {
        String url = baseUrl;
        if (!url.endsWith("/") && !endpoint.startsWith("/")) {
            url += "/";
        }
        return URI.create(url + endpoint);
    }

    // ==================== Builder 基类 ====================

    /**
     * Builder 基类
     *
     * @param <T> Builder 子类型
     */
    @SuppressWarnings("unchecked")
    protected abstract static class BaseBuilder<T extends BaseBuilder<T>> {

        protected String modelId;
        protected HttpClient httpClient;
        protected ObjectMapper objectMapper;

        /**
         * 获取 this 的类型安全引用
         *
         * @return this
         */
        protected abstract T self();

        /**
         * 设置模型 ID
         *
         * @param modelId 模型 ID
         * @return this
         */
        public T modelId(String modelId) {
            this.modelId = modelId;
            return self();
        }

        /**
         * 设置 HTTP 客户端
         *
         * @param httpClient HTTP 客户端
         * @return this
         */
        public T httpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
            return self();
        }

        /**
         * 设置 ObjectMapper
         *
         * @param objectMapper ObjectMapper
         * @return this
         */
        public T objectMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            return self();
        }
    }
}
