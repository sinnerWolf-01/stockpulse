package com.stockpulse.suggestion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class HttpLLMGateway implements LLMGateway {
    private final RestClient restClient;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final String product;
    private final String cookie;

    @Autowired
    public HttpLLMGateway(RestClient.Builder builder,
                          @Value("${llm.base-url:}") String baseUrl,
                          @Value("${llm.api-key:}") String apiKey,
                          @Value("${llm.model:}") String model,
                          @Value("${llm.product:}") String product,
                          @Value("${llm.cookie:}") String cookie) {
        this(createClient(builder), baseUrl, apiKey, model, product, cookie);
    }

    HttpLLMGateway(RestClient restClient, String baseUrl, String apiKey,
                   String model, String product, String cookie) {
        this.restClient = restClient;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.product = product;
        this.cookie = cookie;
    }

    private static RestClient createClient(RestClient.Builder builder) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(15));
        requestFactory.setReadTimeout(Duration.ofSeconds(15));
        return builder.requestFactory(requestFactory).build();
    }

    @Override
    public String complete(String prompt) {
        if (baseUrl.isBlank() || apiKey.isBlank() || model.isBlank() || product.isBlank()) {
            throw new AdvisorException("LLM configuration is unavailable; base URL, API key, model, and product are required");
        }
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(Map.of("role", "user", "content", prompt)));
            var request = restClient.post().uri(baseUrl + "/v1/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey);
            if (!product.isBlank()) request.header("product", product);
            if (!cookie.isBlank()) request.header("Cookie", cookie);
            Map<?, ?> response = request.body(body).retrieve().body(Map.class);
            Object choices = response == null ? null : response.get("choices");
            if (!(choices instanceof List<?> list) || list.isEmpty()
                    || !(list.get(0) instanceof Map<?, ?> choice)
                    || !(choice.get("message") instanceof Map<?, ?> message)
                    || !(message.get("content") instanceof String content)
                    || content.isBlank()) {
                throw new AdvisorException("LLM response content is missing");
            }
            return content;
        } catch (AdvisorException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AdvisorException("LLM request failed", exception);
        }
    }
}