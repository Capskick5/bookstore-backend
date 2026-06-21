package edu.fpt.sba301.bookstore.ai;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class ChatOutputFilter {

    private static final String FILTERED_MESSAGE =
            "I'm sorry, I can't share that information.";

    private static final List<String> BLOCKED_PATTERNS = List.of(
            "system prompt",
            "developer mode",
            "dan mode",
            "api key",
            "api_key",
            "secret key",
            "jwt secret",
            "ignore prior instructions",
            "ignore previous instructions",
            "bỏ qua hướng dẫn");

    public String filter(String content) {
        if (content == null || content.isBlank()) {
            return content;
        }
        String normalized = content.toLowerCase(Locale.ROOT);
        if (BLOCKED_PATTERNS.stream().anyMatch(normalized::contains)) {
            return FILTERED_MESSAGE;
        }
        return content;
    }
}
