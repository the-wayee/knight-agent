package org.cloudnook.knightagent.core.middleware.builtin;

import lombok.extern.slf4j.Slf4j;
import org.cloudnook.knightagent.core.agent.AgentRequest;
import org.cloudnook.knightagent.core.agent.AgentResponse;
import org.cloudnook.knightagent.core.message.ToolCall;
import org.cloudnook.knightagent.core.message.ToolResult;
import org.cloudnook.knightagent.core.middleware.AgentContext;
import org.cloudnook.knightagent.core.middleware.Middleware;
import org.cloudnook.knightagent.core.state.AgentState;

import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 人机协作中间件
 * <p>
 * 在执行敏感工具调用前暂停，等待人工审核。
 * 支持三种审核模式：ALLOW（允许）、REJECT（拒绝）、EDIT（编辑）。
 * <p>
 * 审核模式：
 * <ul>
 *   <li>ALWAYS - 所有工具调用都需要审核</li>
   *   <li>WHITELIST - 只有白名单中的工具需要审核</li>
 *   <li>BLACKLIST - 除了黑名单中的工具外都需要审核</li>
 *   <li>NEVER - 不进行审核（直接放行）</li>
 * </ul>
 * <p>
 * 使用示例：
 * <pre>{@code
 * Agent agent = AgentBuilder.builder()
 *     .model(chatModel)
 *     .middleware(HumanInTheLoopMiddleware.builder()
 *         .mode(ReviewMode.WHITELIST)
 *     .toolsToReview(List.of("delete_file", "send_email"))
 *     .build())
 *     .build();
 * }</pre>
 * <p>
 * <b>注意：</b>当使用终端审核模式时，使用完毕后应调用 {@link #close()} 方法释放资源。
 *
 * @author KnightAgent
 * @since 1.0.0
 */
@Slf4j
public class HumanInTheLoopMiddleware implements Middleware, AutoCloseable {

    /**
     * 审核模式
     */
    public enum ReviewMode {
        /**
         * 审核所有工具调用
         */
        ALWAYS,
        /**
         * 只审核白名单中的工具
         */
        WHITELIST,
        /**
         * 审核除黑名单外的所有工具
         */
        BLACKLIST,
        /**
         * 不进行审核
         */
        NEVER
    }

    /**
     * 审核决策
     */
    public enum ReviewDecision {
        /**
         * 允许执行
         */
        ALLOW,
        /**
         * 拒绝执行
         */
        REJECT,
        /**
         * 修改后执行
         */
        EDIT
    }

    /**
     * 审核回调接口
     */
    @FunctionalInterface
    public interface ReviewCallback {
        ReviewDecision review(ToolCall toolCall, AgentContext context);
    }

    private final ReviewMode mode;
    private final List<String> whitelist;
    private final List<String> blacklist;
    private final ReviewCallback callback;
    private final boolean promptInTerminal;
    private final Map<String, ReviewDecision> cache;
    private Scanner scanner; // 延迟初始化的 Scanner

    private HumanInTheLoopMiddleware(Builder builder) {
        this.mode = builder.mode;
        this.whitelist = builder.whitelist;
        this.blacklist = builder.blacklist;
        this.callback = builder.callback;
        this.promptInTerminal = builder.promptInTerminal;
        this.cache = new ConcurrentHashMap<>();
    }

    @Override
    public boolean beforeToolCall(ToolCall toolCall, AgentContext context) {
        // 检查是否需要审核
        if (!needsReview(toolCall)) {
            return true;
        }

        log.info("人机协作审核: 工具={}, 调用={}", toolCall.getName(), toolCall.getId());

        ReviewDecision decision = getDecision(toolCall, context);

        // 处理审核结果
        return switch (decision) {
            case ALLOW -> {
                log.info("审核结果: 允许");
                yield true;
            }
            case REJECT -> {
                log.info("审核结果: 拒绝");
                yield false;
            }
            case EDIT -> {
                // 编辑模式暂不支持，需要实现参数编辑逻辑
                log.warn("编辑模式暂不支持，转为允许");
                yield true;
            }
        };
    }

    /**
     * 检查是否需要审核
     */
    private boolean needsReview(ToolCall toolCall) {
        return switch (mode) {
            case ALWAYS -> true;
            case NEVER -> false;
            case WHITELIST -> whitelist.contains(toolCall.getName());
            case BLACKLIST -> !blacklist.contains(toolCall.getName());
        };
    }

    /**
     * 获取审核决策
     */
    private ReviewDecision getDecision(ToolCall toolCall, AgentContext context) {
        // 检查缓存
        String cacheKey = toolCall.getName() + ":" + toolCall.getArguments();
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }

        ReviewDecision decision;

        if (callback != null) {
            // 使用自定义回调
            decision = callback.review(toolCall, context);
        } else if (promptInTerminal) {
            // 终端提示
            decision = promptInTerminal(toolCall);
        } else {
            // 默认允许
            decision = ReviewDecision.ALLOW;
        }

        // 缓存决策
        cache.put(cacheKey, decision);
        return decision;
    }

    /**
     * 获取或创建 Scanner 实例
     */
    private synchronized Scanner getScanner() {
        if (scanner == null) {
            scanner = new Scanner(System.in);
        }
        return scanner;
    }

    /**
     * 在终端中提示用户审核
     */
    private ReviewDecision promptInTerminal(ToolCall toolCall) {
        Scanner scanner = getScanner();

        System.out.println("\n===== 工具调用审核 ======");
        System.out.println("工具: " + toolCall.getName());
        System.out.println("参数: " + toolCall.getArguments());
        System.out.println("========================");
        System.out.println("请选择 (a=允许, r=拒绝, e=编辑): ");

        while (true) {
            String input = scanner.nextLine().trim().toLowerCase();

            ReviewDecision decision = switch (input) {
                case "a", "allow", "y", "yes" -> {
                    cache.put(toolCall.getName(), ReviewDecision.ALLOW);
                    yield ReviewDecision.ALLOW;
                }
                case "r", "reject", "n", "no" -> {
                    cache.put(toolCall.getName(), ReviewDecision.REJECT);
                    yield ReviewDecision.REJECT;
                }
                case "e", "edit" -> {
                    System.out.println("编辑功能暂未实现，已自动允许");
                    cache.put(toolCall.getName(), ReviewDecision.ALLOW);
                    yield ReviewDecision.EDIT;
                }
                default -> {
                    System.out.println("无效输入，请重新选择: ");
                    yield null; // 继续循环
                }
            };

            if (decision != null) {
                return decision;
            }
        }
    }

    /**
     * 关闭中间件，释放资源
     * <p>
     * 当使用终端审核模式时，应调用此方法关闭 Scanner。
     */
    @Override
    public void close() {
        if (scanner != null) {
            scanner.close();
            scanner = null;
        }
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
     * 构建器
     */
    public static class Builder {

        private ReviewMode mode = ReviewMode.WHITELIST;
        private List<String> whitelist = List.of();
        private List<String> blacklist = List.of();
        private ReviewCallback callback;
        private boolean promptInTerminal = false;

        /**
         * 设置审核模式（默认 WHITELIST）
         */
        public Builder mode(ReviewMode mode) {
            this.mode = mode;
            return this;
        }

        /**
         * 设置白名单
         */
        public Builder whitelist(List<String> whitelist) {
            this.whitelist = whitelist != null ? whitelist : List.of();
            return this;
        }

        /**
         * 设置黑名单
         */
        public Builder blacklist(List<String> blacklist) {
            this.blacklist = blacklist != null ? blacklist : List.of();
            return this;
        }

        /**
         * 设置审核回调
         */
        public Builder callback(ReviewCallback callback) {
            this.callback = callback;
            return this;
        }

        /**
         * 是否在终端中提示（默认 false）
         */
        public Builder promptInTerminal(boolean promptInTerminal) {
            this.promptInTerminal = promptInTerminal;
            return this;
        }

        /**
         * 构建中间件实例
         */
        public HumanInTheLoopMiddleware build() {
            return new HumanInTheLoopMiddleware(this);
        }
    }
}
