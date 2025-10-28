package io.shareai.joder.services.adapters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.shareai.joder.core.config.ConfigManager;
import io.shareai.joder.domain.Message;
import io.shareai.joder.domain.MessageRole;
import io.shareai.joder.services.model.AbstractModelAdapter;
import io.shareai.joder.services.model.dto.StreamEvent;
import io.shareai.joder.services.model.dto.StreamHandler;
import okhttp3.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

/**
 * OpenAI API 适配器
 * 支持 ChatCompletions API 和流式响应
 */
public class OpenAIAdapter extends AbstractModelAdapter {
    
    private static final String DEFAULT_BASE_URL = "https://api.openai.com";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private final ObjectMapper objectMapper;
    private final String model;
    
    public OpenAIAdapter(ConfigManager configManager, String modelName, String profilePath) {
        super(configManager, modelName, profilePath);
        this.objectMapper = new ObjectMapper();
        this.model = configManager.getString(profilePath + ".model", "gpt-4");
    }
    
    @Override
    protected String getDefaultBaseUrl() {
        return DEFAULT_BASE_URL;
    }
    
    @Override
    public String getProviderName() {
        return "openai";
    }
    
    @Override
    public String sendMessage(List<Message> messages, String systemPrompt) {
        ensureConfigured();
        
        try {
            ObjectNode requestBody = buildRequestBody(messages, systemPrompt, false);
            Request request = buildRequest(requestBody);
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    throw new IOException("Unexpected response " + response.code() + ": " + errorBody);
                }
                
                String responseBody = response.body().string();
                JsonNode jsonResponse = objectMapper.readTree(responseBody);
                
                return extractContent(jsonResponse);
            }
        } catch (IOException e) {
            logger.error("Failed to send message to OpenAI API", e);
            throw new RuntimeException("OpenAI API error: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void sendMessageStream(List<Message> messages, String systemPrompt, StreamHandler handler) {
        ensureConfigured();
        
        try {
            ObjectNode requestBody = buildRequestBody(messages, systemPrompt, true);
            Request request = buildRequest(requestBody);
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    handler.onEvent(StreamEvent.error("API error: " + errorBody));
                    return;
                }
                
                ResponseBody body = response.body();
                if (body == null) {
                    handler.onEvent(StreamEvent.error("Empty response body"));
                    return;
                }
                
                // 处理 SSE 流
                try (BufferedReader reader = new BufferedReader(new StringReader(body.string()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6);
                            
                            if ("[DONE]".equals(data)) {
                                handler.onEvent(StreamEvent.done());
                                break;
                            }
                            
                            try {
                                JsonNode event = objectMapper.readTree(data);
                                JsonNode choices = event.get("choices");
                                if (choices != null && choices.size() > 0) {
                                    JsonNode delta = choices.get(0).get("delta");
                                    if (delta != null && delta.has("content")) {
                                        String content = delta.get("content").asText();
                                        handler.onEvent(StreamEvent.contentDelta(content));
                                    }
                                }
                            } catch (Exception e) {
                                logger.warn("Failed to parse stream event: {}", data, e);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Failed to stream message from OpenAI API", e);
            handler.onEvent(StreamEvent.error(e.getMessage()));
        }
    }
    
    protected ObjectNode buildRequestBody(List<Message> messages, String systemPrompt, boolean stream) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("stream", stream);
        
        ArrayNode messagesArray = body.putArray("messages");
        
        // 添加系统消息
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            ObjectNode systemMessage = messagesArray.addObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
        }
        
        // 添加对话消息
        for (Message msg : messages) {
            if (msg.getRole() != MessageRole.SYSTEM) {
                ObjectNode messageNode = messagesArray.addObject();
                messageNode.put("role", msg.getRole() == MessageRole.USER ? "user" : "assistant");
                messageNode.put("content", msg.getContent());
            }
        }
        
        return body;
    }
    
    protected Request buildRequest(ObjectNode requestBody) throws IOException {
        RequestBody body = RequestBody.create(
            objectMapper.writeValueAsString(requestBody),
            JSON
        );
        
        return new Request.Builder()
            .url(baseUrl + "/v1/chat/completions")
            .addHeader("Authorization", "Bearer " + apiKey)
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build();
    }
    
    private String extractContent(JsonNode response) {
        JsonNode choices = response.get("choices");
        if (choices != null && choices.size() > 0) {
            JsonNode message = choices.get(0).get("message");
            if (message != null && message.has("content")) {
                return message.get("content").asText();
            }
        }
        return "";
    }
}
