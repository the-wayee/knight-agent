package org.cloudnook.knightagent.core.multiagent;

import lombok.Builder;
import lombok.Data;
import org.cloudnook.knightagent.core.agent.Agent;

import java.util.List;

/**
 * Agent 节点
 * <p>
 * 包装 Agent 实例，添加多 Agent 协作所需的元数据。
 * 每个 AgentNode 代表 Multi-AgentSystem 中的一个可执行单元。
 *
 * @author KnightAgent
 * @since 1.0.0
 */
@Data
@Builder
public class AgentNode {

    /**
     * 节点名称
     * <p>
     * 在系统中唯一标识此节点，用于路由和手 off。
     */
    private final String name;

    /**
     * Agent 实例
     * <p>
     * 实际执行任务的 Agent。
     */
    private final Agent agent;

    /**
     * 功能描述
     * <p>
     * 描述此 Agent 的功能和能力，用于：
     * <ul>
     *   <li>Supervisor 模式下让 LLM 了解各 Agent 的职责</li>
     *   <li>生成帮助文档</li>
     *   <li>调试和日志记录</li>
     * </ul>
     */
    private final String description;

    /**
     * 标签
     * <p>
     * 用于分类和匹配，例如：
     * <ul>
     *   <li>["research", "search"] - 研究 Agent</li>
     *   <li>["code", "programming"] - 编程 Agent</li>
     *   <li>["review", "quality"] - 审查 Agent</li>
     * </ul>
     */
    @Builder.Default
    private List<String> tags = List.of();

    /**
     * 是否可以直接返回结果
     * <p>
     * 如果为 true，此 Agent 的响应可以直接作为最终结果返回给用户。
     * 如果为 false，Agent 必须显式发起手 off 或返回特殊标记。
     * <p>
     * 默认为 true，表示大多数 Agent 可以直接完成任务。
     */
    @Builder.Default
    private boolean canReturnResult = true;

    /**
     * 优先级
     * <p>
     * 用于多个 Agent 都能处理任务时的选择。
     * 数值越小优先级越高，默认为 100。
     */
    @Builder.Default
    private int priority = 100;

    /**
     * 是否启用
     * <p>
     * 用于临时禁用某个 Agent 而不移除它。
     */
    @Builder.Default
    private boolean enabled = true;

    /**
     * 创建简单节点（只有名称和 Agent）
     *
     * @param name 节点名称
     * @param agent Agent 实例
     * @return AgentNode
     */
    public static AgentNode of(String name, Agent agent) {
        return AgentNode.builder()
                .name(name)
                .agent(agent)
                .build();
    }

    /**
     * 创建带描述的节点
     *
     * @param name        节点名称
     * @param agent       Agent 实例
     * @param description 功能描述
     * @return AgentNode
     */
    public static AgentNode of(String name, Agent agent, String description) {
        return AgentNode.builder()
                .name(name)
                .agent(agent)
                .description(description)
                .build();
    }

    /**
     * 检查是否有指定标签
     *
     * @param tag 标签
     * @return 如果包含返回 true
     */
    public boolean hasTag(String tag) {
        return tags != null && tags.contains(tag);
    }

    /**
     * 检查是否可以处理指定的标签
     *
     * @param tagList 标签列表
     * @return 如果有任何匹配返回 true
     */
    public boolean canHandle(List<String> tagList) {
        if (tags == null || tags.isEmpty()) {
            return true;
        }
        if (tagList == null || tagList.isEmpty()) {
            return true;
        }
        return tags.stream().anyMatch(tagList::contains);
    }
}
