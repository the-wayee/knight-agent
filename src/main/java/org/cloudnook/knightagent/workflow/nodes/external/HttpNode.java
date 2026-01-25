package org.cloudnook.knightagent.workflow.nodes.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.cloudnook.knightagent.workflow.node.AbstractNode;
import org.cloudnook.knightagent.workflow.node.ExecutionStatus;
import org.cloudnook.knightagent.workflow.node.NodeContext;
import org.cloudnook.knightagent.workflow.node.NodeExecutionResult;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * HTTP节点
 * 发送HTTP请求
 */
@Slf4j
public class HttpNode extends AbstractNode<HttpNodeConfig> {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public HttpNode() {
        super(null, null, null);
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public HttpNode(String id, String name, HttpNodeConfig config) {
        super(id, name, config);
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public org.cloudnook.knightagent.workflow.node.NodeType getType() {
        return org.cloudnook.knightagent.workflow.node.NodeType.HTTP;
    }

    @Override
    protected NodeExecutionResult doExecute(NodeContext context) {
        HttpNodeConfig config = getConfig();

        try {
            // 解析URL中的变量
            String url = resolveVariables(config.getUrl(), context);

            // 构建请求
            Map<String, Object> response = sendRequest(url, config.getMethod(), config);

            return NodeExecutionResult.builder()
                    .nodeId(context.getNodeId())
                    .status(ExecutionStatus.COMPLETED)
                    .output(response)
                    .build();

        } catch (Exception e) {
            log.error("HTTP request failed", e);
            return NodeExecutionResult.failure(context.getNodeId(), e.getMessage());
        }
    }

    /**
     * 发送HTTP请求
     */
    private Map<String, Object> sendRequest(String url, String method, HttpNodeConfig config) {
        Map<String, Object> result = new HashMap<>();

        try {
            Object response = switch (method.toUpperCase()) {
                case "GET" -> restTemplate.getForEntity(url, String.class);
                case "POST" -> restTemplate.postForEntity(url, config.getBody(), String.class);
                case "PUT" -> restTemplate.exchange(url, org.springframework.http.HttpMethod.PUT,
                        new org.springframework.http.HttpEntity<>(config.getBody()), String.class).getBody();
                case "DELETE" -> restTemplate.exchange(url, org.springframework.http.HttpMethod.DELETE,
                        null, String.class).getBody();
                default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
            };

            // 解析响应
            if (response instanceof org.springframework.http.ResponseEntity<?> responseEntity) {
                HttpNodeConfig.OutputMapping mapping = config.getOutputMapping();
                if (mapping == null) {
                    mapping = new HttpNodeConfig.OutputMapping();
                }

                result.put(mapping.getStatusCodeField(), responseEntity.getStatusCode().value());
                result.put(mapping.getHeadersField(), responseEntity.getHeaders());

                Object body = responseEntity.getBody();
                result.put(mapping.getBodyField(), body);

                // 尝试解析JSON
                try {
                    if (body instanceof String) {
                        Object jsonData = objectMapper.readValue((String) body, Object.class);
                        result.put(mapping.getDataField(), jsonData);
                    }
                } catch (Exception e) {
                    log.debug("Failed to parse response as JSON", e);
                }
            }

        } catch (Exception e) {
            result.put("error", e.getMessage());
            result.put("success", false);
        }

        return result;
    }

    /**
     * 解析变量引用
     */
    private String resolveVariables(String text, NodeContext context) {
        if (text == null) {
            return null;
        }

        // 简单的变量替换
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\{\\{([^}]+)\\}\\}");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String varRef = matcher.group(1).trim();
            Object value = resolveVariable(varRef, context);
            matcher.appendReplacement(result, value != null ? value.toString() : "");
        }
        matcher.appendTail(result);

        return result.toString();
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
