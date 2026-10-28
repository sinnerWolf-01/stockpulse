package com.stockpulse.suggestion;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HttpLLMGatewayTest {
    @Test
    void sendsTheExpectedOpenAiCompatibleRequest() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://example.test/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer secret"))
                .andExpect(header("product", "PC1"))
                .andExpect(header("Cookie", "session=value"))
                .andExpect(jsonPath("$.model").value("test-model"))
                .andExpect(jsonPath("$.messages[0].role").value("user"))
                .andExpect(jsonPath("$.messages[0].content").value("OK?"))
                .andRespond(withSuccess("{\"choices\":[{\"message\":{\"content\":\"hello\"}}]}",
                        org.springframework.http.MediaType.APPLICATION_JSON));

        HttpLLMGateway gateway = new HttpLLMGateway(builder.build(), "https://example.test", "secret",
                "test-model", "PC1", "session=value");

        assertThat(gateway.complete("OK?")).isEqualTo("hello");
        server.verify();
    }

    @Test
    void convertsNon2xxAndMalformedResponsesToAdvisorException() {
        RestClient.Builder errorBuilder = RestClient.builder();
        MockRestServiceServer errorServer = MockRestServiceServer.bindTo(errorBuilder).build();
        errorServer.expect(requestTo("https://example.test/v1/chat/completions"))
                .andRespond(withBadRequest());
        HttpLLMGateway errorGateway = new HttpLLMGateway(errorBuilder.build(), "https://example.test", "secret",
                "test-model", "PC1", "");
        assertThatThrownBy(() -> errorGateway.complete("OK?")).isInstanceOf(AdvisorException.class);

        RestClient.Builder malformedBuilder = RestClient.builder();
        MockRestServiceServer malformedServer = MockRestServiceServer.bindTo(malformedBuilder).build();
        malformedServer.expect(requestTo("https://example.test/v1/chat/completions"))
                .andRespond(withSuccess("{\"choices\":[]}", org.springframework.http.MediaType.APPLICATION_JSON));
        HttpLLMGateway malformedGateway = new HttpLLMGateway(malformedBuilder.build(), "https://example.test", "secret",
                "test-model", "PC1", "");
        assertThatThrownBy(() -> malformedGateway.complete("OK?")).isInstanceOf(AdvisorException.class);
    }
}