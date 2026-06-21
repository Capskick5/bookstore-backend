package edu.fpt.sba301.bookstore.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class RagClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final boolean enabled;

    public RagClient(
            @Value("${app.rag.base-url:http://localhost:8000}") String baseUrl,
            @Value("${app.rag.timeout-ms:15000}") int timeoutMs,
            @Value("${app.rag.enabled:true}") boolean enabled) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.enabled = enabled;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        this.restTemplate = new RestTemplate(factory);
    }

    public Optional<RagQueryResult> query(String query) {
        if (!enabled) {
            return Optional.empty();
        }
        try {
            RagQueryResponse response = restTemplate.postForObject(
                    baseUrl + "/query",
                    new RagQueryRequest(query, 5),
                    RagQueryResponse.class);
            if (response == null || response.answer() == null) {
                return Optional.empty();
            }
            return Optional.of(new RagQueryResult(response.answer(), response.sources()));
        } catch (ResourceAccessException ex) {
            log.warn("RAG query timed out: {}", ex.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.GATEWAY_TIMEOUT,
                    "The assistant is temporarily unavailable.");
        } catch (RestClientException ex) {
            log.warn("RAG query failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public boolean triggerIngest() {
        if (!enabled) {
            return false;
        }
        try {
            restTemplate.postForObject(baseUrl + "/ingest", "{}", Object.class);
            return true;
        } catch (RestClientException ex) {
            log.warn("RAG ingest failed: {}", ex.getMessage());
            return false;
        }
    }

    public record RagQueryResult(String answer, List<RagSource> sources) {
    }

    private record RagQueryRequest(String query, int top_k) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RagQueryResponse(String answer, List<RagSource> sources) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RagSource(
            String document_name,
            String file_name,
            Integer page,
            Double score,
            String text) {
    }
}
