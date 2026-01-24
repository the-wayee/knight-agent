package org.cloudnook.knightagent.core.middleware.builtin;

import lombok.extern.slf4j.Slf4j;
import org.cloudnook.knightagent.core.agent.AgentRequest;
import org.cloudnook.knightagent.core.agent.AgentResponse;
import org.cloudnook.knightagent.core.message.ToolCall;
import org.cloudnook.knightagent.core.message.ToolResult;
import org.cloudnook.knightagent.core.middleware.AgentContext;
import org.cloudnook.knightagent.core.middleware.Middleware;
import org.cloudnook.knightagent.core.state.AgentState;

import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 状态注入中间件
 * <p>
 * 从 AgentState 中提取自定义数据，动态注入到系统提示词中。
 * 支持多种注入模式和变量替换，实现上下文增强。
 * <p>
 * 注入模式：
 * <ul>
 *   <li>PREFIX - 在系统提示词前注入</li>
 *   <li>SUFFIX - 在系统提示词后注入</li>
 *   <li>REPLACE - 替换系统提示词中的变量</li>
 * </ul>
 * <p>
 * 变量格式：
 * <ul>
 *   <li>{@code ${state:key}} - 从 AgentState.data 中获取值</li>
 *   <li>{@code ${request:key}} - 从 AgentRequest.parameters 中获取值</li>
 *   <li>{@code ${context:key}} - 从 AgentContext.data 中获取值</li>
 * </ul>
 * <p>
 * 使用示例：
 * <pre>{@code
 * // 示例 1：基本注入
 * Agent agent = AgentBuilder.builder()
 *     .model(chatModel)
 *     .middleware(StateInjectionMiddleware.builder()
 *         .injectionMode(InjectionMode.SUFFIX)
 *         .template("当前用户：${state:userName}")
 *         .build())
 *     .build();
 *
 * // 示例 2：变量替换
 * Agent agent = AgentBuilder.builder()
 *     .model(chatModel)
 *     .systemPrompt("你正在为用户 ${state:userName} 提供服务，" +
 *                    "他的偏好是 ${state:userPreferences}")
 *     .middleware(StateInjectionMiddleware.builder()
 *         .injectionMode(InjectionMode.REPLACE)
 *         .build())
 *     .build();
 *
 * // 示例 3：自定义注入逻辑
 * Agent agent = AgentBuilder.builder()
 *     .model(chatModel)
 *     .middleware(StateInjectionMiddleware.builder()
 *         .customInjector((state, request) -> {
 *             String userId = state.get("userId", String.class);
 *             return "用户 ID: " + userId + "\n对话轮数: " + state.getMessages().size();
 *         })
 *         .build())
 *     .build();
 * }</pre>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
@Slf4j
public class StateInjectionMiddleware implements Middleware {

    /**
     * 注入模式
     */
    public enum InjectionMode {
        /**
         * 在系统提示词前注入
         */
        PREFIX,
        /**
         * 在系统提示词后注入
         */
        SUFFIX,
        /**
         * 替换系统提示词中的变量（不改变原结构）
         */
        REPLACE,
        /**
         * 完全替换系统提示词
         */
        OVERRIDE
    }

    /**
     * 变量解析模式
     */
    public enum VariableMode {
        /**
         * 不解析变量，直接使用模板
         */
        NONE,
        /**
         * 只解析 ${state:xxx} 变量
         */
        STATE_ONLY,
        /**
         * 解析所有变量类型
         */
        ALL
    }

    private final InjectionMode injectionMode;
    private final VariableMode variableMode;
    private final String template;
    private final Function<InjectionContext, String> customInjector;
    private final boolean injectOnlyWhenPresent;
    private final boolean trimInjectedContent;

    private StateInjectionMiddleware(Builder builder) {
        this.injectionMode = builder.injectionMode;
        this.variableMode = builder.variableMode;
        this.template = builder.template;
        this.customInjector = builder.customInjector;
        this.injectOnlyWhenPresent = builder.injectOnlyWhenPresent;
        this.trimInjectedContent = builder.trimInjectedContent;
    }

    @Override
    public void beforeInvoke(AgentRequest request, AgentContext context) {
        // 只在第一次迭代时注入，避免重复注入
        if (context.getIteration() > 0) {
            return;
        }

        String injectedContent = buildInjectedContent(request, context);

        // 如果没有内容可注入，且配置为"仅在存在时注入"，则跳过
        if (injectedContent == null || injectedContent.trim().isEmpty()) {
            if (injectOnlyWhenPresent) {
                return;
            }
            return;
        }

        if (trimInjectedContent) {
            injectedContent = injectedContent.trim();
        }

        String currentSystemPrompt = request.getSystemPrompt();

        String newSystemPrompt = switch (injectionMode) {
            case PREFIX -> {
                if (currentSystemPrompt == null || currentSystemPrompt.isEmpty()) {
                    yield injectedContent;
                }
                yield injectedContent + "\n\n" + currentSystemPrompt;
            }
            case SUFFIX -> {
                if (currentSystemPrompt == null || currentSystemPrompt.isEmpty()) {
                    yield injectedContent;
                }
                yield currentSystemPrompt + "\n\n" + injectedContent;
            }
            case REPLACE -> {
                if (currentSystemPrompt == null || currentSystemPrompt.isEmpty()) {
                    yield injectedContent;
                }
                // 解析变量并替换
                yield replaceVariables(currentSystemPrompt, request, context);
            }
            case OVERRIDE -> injectedContent;
        };

        // 更新请求
        AgentRequest updatedRequest = request.toBuilder()
                .systemPrompt(newSystemPrompt)
                .build();
        context.setRequest(updatedRequest);

        log.debug("状态注入: 模式={}, 新提示词长度={}",
                injectionMode, newSystemPrompt.length());
    }

    @Override
    public void afterInvoke(AgentResponse response, AgentContext context) {
        // 响应完成后不需要处理
    }

    @Override
    public boolean beforeToolCall(ToolCall toolCall, AgentContext context) {
        // 不拦截工具调用
        return true;
    }

    @Override
    public void afterToolCall(ToolCall toolCall, ToolResult toolResult, AgentContext context) {
        // 工具调用完成后不需要处理
    }

    @Override
    public AgentState onStateUpdate(AgentState oldState, AgentState newState, AgentContext context) {
        return newState;
    }

    /**
     * 构建注入内容
     */
    private String buildInjectedContent(AgentRequest request, AgentContext context) {
        if (customInjector != null) {
            // 使用自定义注入器
            InjectionContext injectionContext = new InjectionContext(
                    context.getState(),
                    request,
                    context
            );
            return customInjector.apply(injectionContext);
        }

        if (template != null && !template.isEmpty()) {
            // 使用模板，解析变量
            return replaceVariables(template, request, context);
        }

        return "";
    }

    /**
     * 替换变量
     */
    private String replaceVariables(String text, AgentRequest request, AgentContext context) {
        if (variableMode == VariableMode.NONE) {
            return text;
        }

        Pattern pattern = Pattern.compile("\\$\\{(\\w+):([^}]+)\\}");
        Matcher matcher = pattern.matcher(text);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String source = matcher.group(1);
            String key = matcher.group(2);
            String replacement = resolveVariable(source, key, request, context);

            if (replacement != null) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            } else {
                // 保留原始变量
                matcher.appendReplacement(result, matcher.group(0));
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * 解析变量值
     */
    private String resolveVariable(String source, String key,
                                   AgentRequest request, AgentContext context) {
        return switch (source) {
            case "state" -> {
                if (context.getState() != null) {
                    Object value = context.getState().getData().get(key);
                    yield value != null ? value.toString() : null;
                }
                yield null;
            }
            case "request" -> {
                if (request.getParameters() != null) {
                    Object value = request.getParameters().get(key);
                    yield value != null ? value.toString() : null;
                }
                yield null;
            }
            case "context" -> {
                Object value = context.get(key).orElse(null);
                yield value != null ? value.toString() : null;
            }
            default -> null;
        };
    }

    /**
     * 创建构建器
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 注入上下文
     */
    public record InjectionContext(
            AgentState state,
            AgentRequest request,
            AgentContext context
    ) {
    }

    /**
     * 构建器
     */
    public static class Builder {

        private InjectionMode injectionMode = InjectionMode.SUFFIX;
        private VariableMode variableMode = VariableMode.ALL;
        private String template;
        private Function<InjectionContext, String> customInjector;
        private boolean injectOnlyWhenPresent = true;
        private boolean trimInjectedContent = true;

        /**
         * 设置注入模式（默认 SUFFIX）
         */
        public Builder injectionMode(InjectionMode injectionMode) {
            this.injectionMode = injectionMode;
            return this;
        }

        /**
         * 设置变量解析模式（默认 ALL）
         */
        public Builder variableMode(VariableMode variableMode) {
            this.variableMode = variableMode;
            return this;
        }

        /**
         * 设置注入模板
         */
        public Builder template(String template) {
            this.template = template;
            return this;
        }

        /**
         * 设置自定义注入器
         */
        public Builder customInjector(Function<InjectionContext, String> customInjector) {
            this.customInjector = customInjector;
            return this;
        }

        /**
         * 是否只在有内容时注入（默认 true）
         */
        public Builder injectOnlyWhenPresent(boolean injectOnlyWhenPresent) {
            this.injectOnlyWhenPresent = injectOnlyWhenPresent;
            return this;
        }

        /**
         * 是否修剪注入的内容（默认 true）
         */
        public Builder trimInjectedContent(boolean trimInjectedContent) {
            this.trimInjectedContent = trimInjectedContent;
            return this;
        }

        /**
         * 构建中间件实例
         */
        public StateInjectionMiddleware build() {
            if (template == null && customInjector == null) {
                throw new IllegalStateException(
                        "Either template or customInjector must be provided");
            }
            return new StateInjectionMiddleware(this);
        }
    }
}
