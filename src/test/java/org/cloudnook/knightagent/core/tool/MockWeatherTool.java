package org.cloudnook.knightagent.core.tool;

import org.cloudnook.knightagent.core.message.ToolResult;

import java.util.Map;

/**
 * 模拟天气查询工具
 * <p>
 * 用于测试工具调用功能。
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public class MockWeatherTool extends AbstractTool {

    @Override
    public String getName() {
        return "get_weather";
    }

    @Override
    public String getDescription() {
        return "获取指定城市的天气信息";
    }

    @Override
    public String getParametersSchema() {
        return """
            {
                "type": "object",
                "properties": {
                    "city": {
                        "type": "string",
                        "description": "城市名称"
                    }
                },
                "required": ["city"]
            }
            """;
    }

    @Override
    protected ToolResult executeInternal(Map<String, Object> arguments) {
        String city = getStringParam(arguments, "city");
        // 模拟天气数据
        return ToolResult.success(
                generateCallId(),
                String.format(
                        "{\"city\": \"%s\", \"temperature\": 25, \"condition\": \"晴\", \"humidity\": 60}",
                        city
                )
        );
    }
}
