package org.cloudnook.knightagent.core.tool;

import org.cloudnook.knightagent.core.message.ToolResult;

/**
 * 工具接口
 * <p>
 * 定义 Agent 可调用的工具规范。
 * 工具是 Agent 与外部世界交互的主要方式，可以是 API 调用、数据库查询、文件操作等。
 * <p>
 * 实现此接口来创建自定义工具：
 * <pre>{@code
 * public class WeatherTool implements Tool {
 *
 *     @Override
 *     public String getName() {
 *         return "get_weather";
 *     }
 *
 *     @Override
 *     public String getDescription() {
 *         return "获取指定城市的当前天气信息";
 *     }
 *
 *     @Override
 *     public String getParametersSchema() {
 *         return """
 *             {
 *                 "type": "object",
 *                 "properties": {
 *                     "city": {
 *                         "type": "string",
 *                         "description": "城市名称"
 *                     }
 *                 },
 *                 "required": ["city"]
 *             }
 *             """;
 *     }
 *
 *     @Override
 *     public ToolResult execute(String arguments) throws ToolExecutionException {
 *         // 解析参数并执行
 *         Map<String, Object> params = parseArguments(arguments);
 *         String city = (String) params.get("city");
 *         String weather = fetchWeather(city);
 *         return ToolResult.success(id, weather);
 *     }
 * }
 * }</pre>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public interface Tool {

    /**
     * 获取工具名称
     * <p>
     * 工具名称必须唯一，用于 LLM 识别和调用工具。
     * 命名建议使用 snake_case 格式，如：get_weather、search_database
     *
     * @return 工具名称
     */
    String getName();

    /**
     * 获取工具描述
     * <p>
     * 向 LLM 描述工具的功能和使用场景。
     * 好的描述应该包含：
     * <ul>
     *   <li>工具做什么</li>
     *   <li>何时使用</li>
     *   <li>输入输出说明</li>
     * </ul>
     * <p>
     * 示例：查询指定日期的股票价格，用于获取实时或历史股票数据
     *
     * @return 工具描述
     */
    String getDescription();

    /**
     * 获取参数 JSON Schema
     * <p>
     * 定义工具接受的参数格式，使用 JSON Schema 标准。
     * LLM 会根据这个 Schema 生成符合要求的参数。
     * <p>
     * 返回的字符串应该是有效的 JSON Schema 格式。
     * <p>
     * 简化示例（无参数）：
     * <pre>{@code
     * return "{\"type\": \"object\", \"properties\": {}}";
     * }</pre>
     * <p>
     * 完整示例：
     * <pre>{@code
     * return """
     *     {
     *         "type": "object",
     *         "properties": {
     *             "city": {
     *                 "type": "string",
     *                 "description": "城市名称"
     *             },
     *             "unit": {
     *                 "type": "string",
     *                 "enum": ["celsius", "fahrenheit"],
     *                 "description": "温度单位"
     *             }
     *         },
     *         "required": ["city"]
     *     }
     *     """;
     * }</pre>
     *
     * @return JSON Schema 格式的参数定义
     */
    String getParametersSchema();

    /**
     * 执行工具
     * <p>
     * 使用给定的参数执行工具逻辑。
     * 参数是 JSON 字符串格式，需要自行解析。
     * <p>
     * 注意：
     * <ul>
     *   <li>此方法应该是线程安全的（如果工具需要被并发调用）</li>
     *   <li>长时间运行的操作应该考虑超时处理</li>
     *   <li>错误应该通过抛出 {@link ToolExecutionException} 来报告</li>
     * </ul>
     *
     * @param arguments JSON 格式的参数字符串
     * @return 工具执行结果
     * @throws ToolExecutionException 执行过程中的错误
     */
    ToolResult execute(String arguments) throws ToolExecutionException;

    /**
     * 判断工具是否需要身份验证
     * <p>
     * 默认不需要，子类可以重写。
     *
     * @return 如果需要身份验证返回 true
     */
    default boolean requiresAuth() {
        return false;
    }

    /**
     * 获取工具版本
     * <p>
     * 可用于工具的版本管理和兼容性检查。
     *
     * @return 工具版本，默认 "1.0.0"
     */
    default String getVersion() {
        return "1.0.0";
    }

    /**
     * 获取工具类别
     * <p>
     * 可用于工具的组织和分类展示。
     *
     * @return 工具类别，默认 "general"
     */
    default String getCategory() {
        return "general";
    }
}
