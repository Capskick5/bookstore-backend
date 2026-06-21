package edu.fpt.sba301.bookstore.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatRateLimiter {

    private final int limitPerMinute;
    private final Map<Long, Deque<Long>> requestTimestamps = new ConcurrentHashMap<>();

    public ChatRateLimiter(@Value("${app.ai.rate-limit-per-minute:20}") int limitPerMinute) {
        this.limitPerMinute = limitPerMinute;
    }

    public void checkAllowed(Long userId) {
        long now = System.currentTimeMillis();
        long windowStart = now - 60_000L;

        Deque<Long> timestamps = requestTimestamps.computeIfAbsent(userId, id -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= limitPerMinute) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Chat rate limit exceeded");
            }
            timestamps.addLast(now);
        }
    }
}
