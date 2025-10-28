package io.leavesfly.joder.services.adapters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.leavesfly.joder.core.config.ConfigManager;
import io.leavesfly.joder.domain.Message;
import io.leavesfly.joder.domain.MessageRole;
import io.leavesfly.joder.services.model.AbstractModelAdapter;
import io.leavesfly.joder.services.model.dto.StreamEvent;
import io.leavesfly.joder.services.model.dto.StreamHandler;
import okhttp3.*;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Claude (Anthropic) API 适配器
 * 支持 Anthropic Messages API 和流式响应
 */
public class ClaudeAdapter extends AbstractModelAdapter {
    
    private static final String DEFAULT_BASE_URL = "https://api.anthropic.com";
    private static final String API_VERSION = "2023-06-01";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private final ObjectMapper objectMapper;
    private final String model;
    
    public ClaudeAdapter(ConfigManager configManager, String modelName, String profilePath) {
        super(configManager, modelName, profilePath);
        this.objectMapper = new ObjectMapper();
        this.model = configManager.getString(profilePath + ".model", "claude-3-5-sonnet-20241022");
    }
    
    @Override
    protected String getDefaultBaseUrl() {
        return DEFAULT_BASE_URL;
    }
    
    @Override
    public String getProviderName() {
        return "anthropic";
    }
    
    @Override
    public String sendMessage(List<Message> messages, String systemPrompt) {
        ensureConfigured();
        
        try {
            ObjectNode requestBody = buildRequestBody(messages, systemPrompt, false);
            Request request = buildRequest(requestBody);
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected response: " + response);
                }
                
                String responseBody = response.body().string();
                JsonNode jsonResponse = objectMapper.readTree(responseBody);
                
                return extractContent(jsonResponse);
            }
        } catch (IOException e) {
            logger.error("Failed to send message to Claude API", e);
            throw new RuntimeException("Claude API error: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void sendMessageStream(List<Message> messages, String systemPrompt, StreamHandler handler) {
        ensureConfigured();
        
        try {
            ObjectNode requestBody = buildRequestBody(messages, systemPrompt, true);
            Request request = buildRequest(requestBody);
            
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Exception> error = new AtomicReference<>();
            
            EventSourceListener listener = new EventSourceListener() {
                @Override
                public void onEvent(EventSource eventSource, String id, String type, String data) {
                    try {
                        if ("content_block_delta".equals(type)) {
                            JsonNode event = objectMapper.readTree(data);
                            JsonNode delta = event.get("delta");
                            if (delta != null && delta.has("text")) {
                                String text = delta.get("text").asText();
                                handler.onEvent(StreamEvent.contentDelta(text));
                            }
                        } else if ("message_stop".equals(type)) {
                            handler.onEvent(StreamEvent.done());
                            latch.countDown();
                        }
                    } catch (Exception e) {
                        error.set(e);
                        handler.onEvent(StreamEvent.error(e.getMessage()));
                        latch.countDown();
                    }
                }
                
                @Override
                public void onFailure(EventSource eventSource, Throwable t, Response response) {
                    error.set(new RuntimeException(t));
                    handler.onEvent(StreamEvent.error(t.getMessage()));
                    latch.countDown();
                }
            };
            
            EventSource eventSource = EventSources.createFactory(httpClient)
                .newEventSource(request, listener);
            
            latch.await();
            eventSource.cancel();
            
            if (error.get() != null) {
                throw error.get();
            }
        } catch (Exception e) {
            logger.error("Failed to stream message from Claude API", e);
            throw new RuntimeException("Claude API stream error: " + e.getMessage(), e);
        }
    }
    
    private ObjectNode buildRequestBody(List<Message> messages, String systemPrompt, boolean stream) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("max_tokens", 4096);
        body.put("stream", stream);
        
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            body.put("system", systemPrompt);
        }
        
        ArrayNode messagesArray = body.putArray("messages");
        for (Message msg : messages) {
            if (msg.getRole() != MessageRole.SYSTEM) {
                ObjectNode messageNode = messagesArray.addObject();
                messageNode.put("role", msg.getRole() == MessageRole.USER ? "user" : "assistant");
                messageNode.put("content", msg.getContent());
            }
        }
        
        return body;
    }
    
    private Request buildRequest(ObjectNode requestBody) throws IOException {
        RequestBody body = RequestBody.create(
            objectMapper.writeValueAsString(requestBody),
            JSON
        );
        
        return new Request.Builder()
            .url(baseUrl + "/v1/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", API_VERSION)
            .addHeader("content-type", "application/json")
            .post(body)
            .build();
    }
    
    private String extractContent(JsonNode response) {
        JsonNode content = response.get("content");
        if (content != null && content.isArray() && content.size() > 0) {
            JsonNode firstContent = content.get(0);
            if (firstContent.has("text")) {
                return firstContent.get("text").asText();
            }
        }
        return "";
    }
}
