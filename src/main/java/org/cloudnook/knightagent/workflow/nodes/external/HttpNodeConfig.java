package org.cloudnook.knightagent.workflow.nodes.external;

import lombok.Data;
import org.cloudnook.knightagent.workflow.node.NodeConfig;

import java.util.List;
import java.util.Map;

/**
 * HTTP节点配置
 */
@Data
public class HttpNodeConfig extends NodeConfig {

    /**
     * HTTP方法: GET, POST, PUT, DELETE, PATCH
     */
    private String method;

    /**
     * 请求URL（支持变量引用）
     */
    private String url;

    /**
     * 请求头
     */
    private Map<String, String> headers;

    /**
     * 请求体
     */
    private Object body;

    /**
     * 超时时间（秒）
     */
    private Integer timeout;

    /**
     * 是否跟随重定向
     */
    private Boolean followRedirects;

    /**
     * 输出映射
     */
    private OutputMapping outputMapping;

    @Data
    public static class OutputMapping {
        /**
         * 状态码字段名
         */
        private String statusCodeField = "statusCode";

        /**
         * 响应头字段名
         */
        private String headersField = "headers";

        /**
         * 响应体字段名
         */
        private String bodyField = "body";

        /**
         * 数据字段（JSON解析后的数据）
         */
        private String dataField = "data";
    }
}
