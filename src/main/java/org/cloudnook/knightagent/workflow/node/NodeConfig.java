package org.cloudnook.knightagent.workflow.node;

import lombok.Data;

import java.util.Map;

/**
 * 节点配置基类
 */
@Data
public class NodeConfig {

    /**
     * 配置参数
     */
    protected Map<String, Object> parameters;

    /**
     * 获取配置参数
     */
    public Object getParameter(String key) {
        return parameters != null ? parameters.get(key) : null;
    }

    /**
     * 获取配置参数（带默认值）
     */
    @SuppressWarnings("unchecked")
    public <T> T getParameter(String key, Class<T> type, T defaultValue) {
        Object value = getParameter(key);
        if (value == null) {
            return defaultValue;
        }
        return (T) value;
    }

    /**
     * 设置配置参数
     */
    public void setParameter(String key, Object value) {
        if (parameters == null) {
            parameters = new java.util.HashMap<>();
        }
        parameters.put(key, value);
    }
}
