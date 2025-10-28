package io.leavesfly.joder.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JSON 工具类
 * 对应 Kode 的 json.ts
 */
public class JsonUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(JsonUtils.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 安全地解析 JSON 字符串
     * 
     * @param json JSON 字符串
     * @return JsonNode 对象，失败时返回 null
     */
    public static JsonNode safeParseJSON(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            logger.error("JSON 解析失败: {}", json, e);
            return null;
        }
    }
    
    /**
     * 安全地解析 JSON 到指定类型
     * 
     * @param <T> 目标类型
     * @param json JSON 字符串
     * @param clazz 目标类
     * @return 解析后的对象，失败时返回 null
     */
    public static <T> T safeParseJSON(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            logger.error("JSON 解析失败: {}", json, e);
            return null;
        }
    }
    
    /**
     * 将对象转换为 JSON 字符串
     * 
     * @param obj 要转换的对象
     * @return JSON 字符串，失败时返回 null
     */
    public static String toJSON(Object obj) {
        if (obj == null) {
            return null;
        }
        
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            logger.error("对象转 JSON 失败", e);
            return null;
        }
    }
    
    /**
     * 将对象转换为格式化的 JSON 字符串
     * 
     * @param obj 要转换的对象
     * @return 格式化的 JSON 字符串，失败时返回 null
     */
    public static String toPrettyJSON(Object obj) {
        if (obj == null) {
            return null;
        }
        
        try {
            return objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            logger.error("对象转 JSON 失败", e);
            return null;
        }
    }
    
    /**
     * 获取 ObjectMapper 实例
     */
    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
