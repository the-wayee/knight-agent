package org.cloudnook.knightagent.workflow.nodes.logic;

import lombok.extern.slf4j.Slf4j;
import org.cloudnook.knightagent.workflow.node.AbstractNode;
import org.cloudnook.knightagent.workflow.node.ExecutionStatus;
import org.cloudnook.knightagent.workflow.node.NodeContext;
import org.cloudnook.knightagent.workflow.node.NodeExecutionResult;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 条件节点
 * 根据条件表达式决定是否继续执行
 */
@Slf4j
public class ConditionNode extends AbstractNode<ConditionNodeConfig> {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");

    public ConditionNode() {
        super(null, null, null);
    }

    public ConditionNode(String id, String name, ConditionNodeConfig config) {
        super(id, name, config);
    }

    @Override
    public org.cloudnook.knightagent.workflow.node.NodeType getType() {
        return org.cloudnook.knightagent.workflow.node.NodeType.CONDITION;
    }

    @Override
    protected NodeExecutionResult doExecute(NodeContext context) {
        ConditionNodeConfig config = getConfig();

        try {
            // 解析并评估条件
            boolean conditionMet = evaluateCondition(config.getCondition(), context);

            Map<String, Object> output = new HashMap<>();
            output.put("conditionMet", conditionMet);

            return NodeExecutionResult.builder()
                    .nodeId(context.getNodeId())
                    .status(conditionMet ? ExecutionStatus.COMPLETED : ExecutionStatus.SKIPPED)
                    .output(output)
                    .build();

        } catch (Exception e) {
            log.error("Condition evaluation failed", e);
            return NodeExecutionResult.failure(context.getNodeId(), e.getMessage());
        }
    }

    /**
     * 评估条件表达式
     */
    private boolean evaluateCondition(String condition, NodeContext context) {
        if (condition == null || condition.isBlank()) {
            return true; // 默认通过
        }

        // 替换变量引用
        String evaluated = replaceVariables(condition, context);

        // 简单的条件评估
        // 支持格式: value > 10, value == "test", value != null
        return evaluateSimpleCondition(evaluated);
    }

    /**
     * 替换变量引用
     */
    private String replaceVariables(String expression, NodeContext context) {
        Matcher matcher = VARIABLE_PATTERN.matcher(expression);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String varRef = matcher.group(1).trim();
            Object value = resolveVariable(varRef, context);
            String valueStr = value != null ? formatValue(value) : "null";
            matcher.appendReplacement(result, valueStr);
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * 格式化值用于表达式
     */
    private String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String) {
            return "\"" + value + "\"";
        }
        return value.toString();
    }

    /**
     * 评估简单条件
     */
    private boolean evaluateSimpleCondition(String condition) {
        condition = condition.trim();

        // 检查是否为真
        if (condition.equals("true")) {
            return true;
        }
        if (condition.equals("false")) {
            return false;
        }
        if (condition.equals("null")) {
            return false;
        }

        // 比较操作
        String[] operators = {">=", "<=", "!=", "==", ">", "<"};
        for (String op : operators) {
            if (condition.contains(op)) {
                String[] parts = condition.split(op, 2);
                if (parts.length == 2) {
                    Object left = parseValue(parts[0].trim());
                    Object right = parseValue(parts[1].trim());
                    return compare(left, right, op);
                }
            }
        }

        // 默认返回true
        return true;
    }

    /**
     * 解析值
     */
    private Object parseValue(String value) {
        if (value == null || value.equals("null")) {
            return null;
        }
        if (value.equals("true")) {
            return true;
        }
        if (value.equals("false")) {
            return false;
        }
        if (value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            }
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return value;
        }
    }

    /**
     * 比较两个值
     */
    @SuppressWarnings("unchecked")
    private boolean compare(Object left, Object right, String operator) {
        return switch (operator) {
            case "==" -> left == null ? right == null : left.equals(right);
            case "!=" -> left == null ? right != null : !left.equals(right);
            case ">" -> compareNumber(left, right, (a, b) -> a > b);
            case "<" -> compareNumber(left, right, (a, b) -> a < b);
            case ">=" -> compareNumber(left, right, (a, b) -> a >= b);
            case "<=" -> compareNumber(left, right, (a, b) -> a <= b);
            default -> false;
        };
    }

    /**
     * 比较数字
     */
    private boolean compareNumber(Object left, Object right, java.util.function.BiFunction<Double, Double, Boolean> comparator) {
        double leftNum = left instanceof Number ? ((Number) left).doubleValue() : Double.parseDouble(left.toString());
        double rightNum = right instanceof Number ? ((Number) right).doubleValue() : Double.parseDouble(right.toString());
        return comparator.apply(leftNum, rightNum);
    }

    /**
     * 解析变量引用
     */
    private Object resolveVariable(String ref, NodeContext context) {
        String[] parts = ref.split("\\.");
        if (parts.length < 2) {
            return context.getInput().get(ref);
        }

        String source = parts[0];
        String field = parts[1];

        return switch (source) {
            case "input" -> context.getWorkflowInput().get(field);
            case "context" -> context.getVariable(field);
            default -> {
                Map<String, Object> nodeOutput = context.getNodeOutput(source);
                yield nodeOutput != null ? nodeOutput.get(field) : null;
            }
        };
    }
}
