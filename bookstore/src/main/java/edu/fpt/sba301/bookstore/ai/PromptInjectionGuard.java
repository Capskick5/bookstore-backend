package edu.fpt.sba301.bookstore.ai;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class PromptInjectionGuard {

    private static final List<String> BLOCKED_PATTERNS = List.of(
            "ignore prior instructions",
            "ignore previous instructions",
            "bỏ qua hướng dẫn",
            "system prompt",
            "jailbreak",
            "dan mode");

    public boolean isBlocked(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String normalized = message.toLowerCase(Locale.ROOT);
        return BLOCKED_PATTERNS.stream().anyMatch(normalized::contains);
    }
}
