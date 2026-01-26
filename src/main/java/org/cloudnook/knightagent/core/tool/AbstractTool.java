package org.cloudnook.knightagent.core.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.cloudnook.knightagent.core.message.ToolResult;

import java.util.Map;

/**
 * 工具抽象基类
 * <p>
 * 提供工具接口的通用实现，简化自定义工具的开发。
 * 子类只需实现核心的 {@link #executeInternal(Map)} 方法。
 * <p>
 * 使用示例：
 * <pre>{@code
 * public class SimpleWeatherTool extends AbstractTool {
 *
 *     @Override
 *     public String getName() {
 *         return "get_weather";
 *     }
 *
 *     @Override
 *     public String getDescription() {
 *         return "获取指定城市的天气信息";
 *     }
 *
 *     @Override
 *     public Map<String, Object> getParameters() {
 *         return Map.of(
 *             "type", "object",
 *             "properties", Map.of(
 *                 "city", Map.of(
 *                     "type", "string",
 *                     "description", "城市名称"
 *                 )
 *             ),
 *             "required", List.of("city")
 *         );
 *     }
 *
 *     @Override
 *     protected ToolResult executeInternal(Map<String, Object> arguments) {
 *         String city = (String) arguments.get("city");
 *         // 直接使用解析后的参数
 *         String weather = fetchWeather(city);
 *         return ToolResult.success(generateCallId(), weather);
 *     }
 * }
 * }</pre>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public abstract class AbstractTool implements McpTool {

    /**
     * JSON 序列化器
     * <p>
     * 用于参数解析和结果序列化。
     * 使用 Jackson ObjectMapper，支持复杂类型。
     */
    protected final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 默认构造函数
     */
    protected AbstractTool() {
    }

    @Override
    public final ToolResult execute(String arguments) throws ToolExecutionException {
        try {
            // 解析 JSON 参数
            Map<String, Object> params = parseArguments(arguments);

            // 调用子类实现
            return executeInternal(params);

        } catch (JsonProcessingException e) {
            throw new ToolExecutionException("参数解析失败: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ToolExecutionException("工具执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 子类实现：执行工具核心逻辑
     * <p>
     * 参数已经被解析为 Map，可以直接使用。
     * <p>
     * 注意：返回的 ToolResult 中的 toolCallId 应该是本次调用的唯一标识，
     * 可以使用 {@link #generateCallId()} 生成。
     *
     * @param arguments 解析后的参数 Map
     * @return 工具执行结果
     * @throws Exception 执行过程中的任何异常都会被包装为 ToolExecutionException
     */
    protected abstract ToolResult executeInternal(Map<String, Object> arguments) throws Exception;

    /**
     * 解析 JSON 参数
     *
     * @param arguments JSON 字符串
     * @return 解析后的参数 Map
     * @throws JsonProcessingException 解析失败
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> parseArguments(String arguments) throws JsonProcessingException {
        if (arguments == null || arguments.isBlank()) {
            return Map.of();
        }
        return objectMapper.readValue(arguments, Map.class);
    }

    /**
     * 将对象序列化为 JSON 字符串
     *
     * @param obj 要序列化的对象
     * @return JSON 字符串
     */
    protected String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return String.valueOf(obj);
        }
    }

    /**
     * 生成工具调用 ID
     * <p>
     * 用于标识本次工具调用，格式：call_ + 8位随机字符
     *
     * @return 工具调用 ID
     */
    protected String generateCallId() {
        return "call_" + java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 获取字符串参数
     *
     * @param arguments 参数 Map
     * @param key       参数键
     * @return 参数值，如果不存在返回 null
     */
    protected String getStringParam(Map<String, Object> arguments, String key) {
        Object value = arguments.get(key);
        return value != null ? String.valueOf(value) : null;
    }

    /**
     * 获取字符串参数（带默认值）
     *
     * @param arguments 参数 Map
     * @param key       参数键
     * @param defaultValue 默认值
     * @return 参数值，如果不存在返回默认值
     */
    protected String getStringParam(Map<String, Object> arguments, String key, String defaultValue) {
        String value = getStringParam(arguments, key);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取整数参数
     *
     * @param arguments 参数 Map
     * @param key       参数键
     * @return 参数值，如果不存在返回 null
     */
    protected Integer getIntParam(Map<String, Object> arguments, String key) {
        Object value = arguments.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 获取布尔参数
     *
     * @param arguments 参数 Map
     * @param key       参数键
     * @return 参数值，如果不存在返回 false
     */
    protected boolean getBooleanParam(Map<String, Object> arguments, String key) {
        Object value = arguments.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return false;
    }
}
