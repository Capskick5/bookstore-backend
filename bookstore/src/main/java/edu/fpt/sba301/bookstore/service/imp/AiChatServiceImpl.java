package edu.fpt.sba301.bookstore.service.imp;

import edu.fpt.sba301.bookstore.ai.ChatOutputFilter;
import edu.fpt.sba301.bookstore.ai.ChatRateLimiter;
import edu.fpt.sba301.bookstore.ai.PromptInjectionGuard;
import edu.fpt.sba301.bookstore.ai.RagClient;
import edu.fpt.sba301.bookstore.dto.request.ChatRequest;
import edu.fpt.sba301.bookstore.dto.response.BookRecommendationResponse;
import edu.fpt.sba301.bookstore.dto.response.ChatResponse;
import edu.fpt.sba301.bookstore.dto.response.ConversationResponse;
import edu.fpt.sba301.bookstore.dto.response.MessageResponse;
import edu.fpt.sba301.bookstore.dto.response.PageResponse;
import edu.fpt.sba301.bookstore.dto.response.SourceResponse;
import edu.fpt.sba301.bookstore.entity.Book;
import edu.fpt.sba301.bookstore.entity.Conversation;
import edu.fpt.sba301.bookstore.entity.Message;
import edu.fpt.sba301.bookstore.entity.User;
import edu.fpt.sba301.bookstore.repository.BookRepository;
import edu.fpt.sba301.bookstore.repository.ConversationRepository;
import edu.fpt.sba301.bookstore.repository.MessageRepository;
import edu.fpt.sba301.bookstore.service.AiChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final BookRepository bookRepository;
    private final RagClient ragClient;
    private final PromptInjectionGuard promptInjectionGuard;
    private final ChatRateLimiter chatRateLimiter;
    private final ChatOutputFilter chatOutputFilter;

    @Override
    @Transactional
    public ChatResponse chat(User user, ChatRequest request) {
        chatRateLimiter.checkAllowed(user.getId());

        if (promptInjectionGuard.isBlocked(request.message())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Your message was rejected for safety reasons.");
        }

        Conversation conversation = resolveConversation(user, request.conversationId(), request.message());
        saveMessage(conversation, "user", request.message(), null);

        String answer;
        List<SourceResponse> sources;
        List<RagClient.RagSource> ragSources = List.of();
        var ragResult = ragClient.query(buildContextualQuery(conversation.getId(), request.message()));
        if (ragResult.isPresent()) {
            answer = ragResult.get().answer();
            ragSources = ragResult.get().sources() != null ? ragResult.get().sources() : List.of();
            sources = ragSources.stream()
                    .map(src -> new SourceResponse(
                            src.document_name(),
                            src.document_name(),
                            src.page(),
                            src.score()))
                    .toList();
        } else {
            answer = buildFallbackAnswer(request.message());
            sources = List.of();
        }

        answer = chatOutputFilter.filter(answer);
        List<BookRecommendationResponse> recommendations =
                findRecommendations(request.message(), answer, ragSources);
        Message assistantMessage = saveMessage(conversation, "assistant", answer, sources);

        return new ChatResponse(
                assistantMessage.getId(),
                conversation.getId(),
                answer,
                sources,
                recommendations);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ConversationResponse> listConversations(User user, int page, int size) {
        Page<ConversationResponse> conversations = conversationRepository
                .findByUserOrderByCreatedAtDesc(user, PageRequest.of(page, size))
                .map(c -> new ConversationResponse(c.getId(), c.getTitle(), c.getCreatedAt()));
        return PageResponse.from(conversations);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MessageResponse> listMessages(User user, Long conversationId, int page, int size) {
        Conversation conversation = requireOwnedConversation(user, conversationId);
        Page<MessageResponse> messages = messageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversation.getId(), PageRequest.of(page, size))
                .map(this::mapMessage);
        return PageResponse.from(messages);
    }

    @Override
    @Transactional
    public void deleteConversation(User user, Long conversationId) {
        Conversation conversation = requireOwnedConversation(user, conversationId);
        conversationRepository.delete(conversation);
    }

    private Conversation requireOwnedConversation(User user, Long conversationId) {
        return conversationRepository.findById(conversationId)
                .filter(c -> c.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
    }

    private MessageResponse mapMessage(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getRole(),
                message.getContent(),
                extractSources(message.getSourcesJson()),
                message.getCreatedAt());
    }

    @SuppressWarnings("unchecked")
    private List<SourceResponse> extractSources(Map<String, Object> sourcesJson) {
        if (sourcesJson == null || !sourcesJson.containsKey("sources")) {
            return List.of();
        }
        Object raw = sourcesJson.get("sources");
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<SourceResponse> sources = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                sources.add(new SourceResponse(
                        stringValue(map.get("title")),
                        stringValue(map.get("documentName")),
                        intValue(map.get("page")),
                        doubleValue(map.get("score"))));
            }
        }
        return sources;
    }

    private String stringValue(Object value) {
        return value != null ? value.toString() : null;
    }

    private Integer intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private Double doubleValue(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return null;
    }

    private Conversation resolveConversation(User user, Long conversationId, String firstMessage) {
        if (conversationId != null) {
            return conversationRepository.findById(conversationId)
                    .filter(conversation -> conversation.getUser().getId().equals(user.getId()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
        }

        Conversation conversation = new Conversation();
        conversation.setUser(user);
        conversation.setTitle(truncate(firstMessage, 255));
        conversation.setCreatedAt(OffsetDateTime.now());
        return conversationRepository.save(conversation);
    }

    private String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

    private Message saveMessage(
            Conversation conversation,
            String role,
            String content,
            List<SourceResponse> sources) {
        Message message = new Message();
        message.setConversation(conversation);
        message.setRole(role);
        message.setContent(content);
        if (sources != null && !sources.isEmpty()) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("sources", sources.stream()
                    .map(source -> Map.<String, Object>of(
                            "title", source.title(),
                            "documentName", source.documentName(),
                            "page", source.page() != null ? source.page() : 0,
                            "score", source.score() != null ? source.score() : 0.0))
                    .toList());
            message.setSourcesJson(payload);
        }
        message.setCreatedAt(OffsetDateTime.now());
        return messageRepository.save(message);
    }

    private String buildContextualQuery(Long conversationId, String latestMessage) {
        List<Message> recent = new ArrayList<>(messageRepository.findTop10ByConversationIdOrderByCreatedAtDesc(conversationId));
        Collections.reverse(recent);

        StringBuilder builder = new StringBuilder();
        for (Message message : recent) {
            if ("user".equals(message.getRole()) || "assistant".equals(message.getRole())) {
                builder.append(message.getRole()).append(": ").append(message.getContent()).append('\n');
            }
        }
        if (builder.isEmpty()) {
            return latestMessage;
        }
        return builder.toString();
    }

    private String buildFallbackAnswer(String message) {
        String normalized = message.toLowerCase(Locale.ROOT);
        if (normalized.contains("giao hàng") || normalized.contains("ship")) {
            return "BookVerse áp dụng phí giao hàng 30.000 VND. Đơn từ 300.000 VND sau giảm giá được miễn phí ship.";
        }
        if (normalized.contains("điểm") || normalized.contains("loyalty")) {
            return "Cứ 10.000 VND chi tiêu được 1 điểm. 1 điểm = 100 VND khi thanh toán, tối đa 20% giá trị đơn.";
        }
        if (normalized.contains("voucher") || normalized.contains("khuyến mãi")) {
            return "Mỗi đơn chỉ áp dụng một voucher. Voucher có thể giảm tiền, phần trăm hoặc miễn phí ship tùy loại.";
        }
        return "Tôi chưa kết nối được dịch vụ RAG hoặc chưa tìm thấy tài liệu phù hợp. Bạn có thể hỏi về sách, giao hàng, điểm thưởng hoặc voucher.";
    }

    private List<BookRecommendationResponse> findRecommendations(
            String query,
            String answer,
            List<RagClient.RagSource> ragSources) {
        if (ragSources != null && !ragSources.isEmpty()) {
            List<BookRecommendationResponse> fromSources = recommendationsFromSources(ragSources);
            if (!fromSources.isEmpty()) {
                return fromSources;
            }
        }
        return findRecommendationsByKeywords(query, answer);
    }

    private List<BookRecommendationResponse> recommendationsFromSources(List<RagClient.RagSource> sources) {
        Set<BookRecommendationResponse> results = new LinkedHashSet<>();
        for (RagClient.RagSource source : sources) {
            if (source.document_name() == null || source.document_name().isBlank()) {
                continue;
            }
            bookRepository.searchActiveInStock(source.document_name(), PageRequest.of(0, 3)).stream()
                    .map(this::toRecommendation)
                    .forEach(results::add);
            if (results.size() >= 5) {
                break;
            }
        }
        return results.stream().limit(5).toList();
    }

    private List<BookRecommendationResponse> findRecommendationsByKeywords(String query, String answer) {
        Set<BookRecommendationResponse> results = new LinkedHashSet<>();
        for (String keyword : extractKeywords(query + " " + answer)) {
            bookRepository.searchActiveInStock(keyword, PageRequest.of(0, 3)).stream()
                    .map(this::toRecommendation)
                    .forEach(results::add);
            if (results.size() >= 5) {
                break;
            }
        }
        return results.stream().limit(5).toList();
    }

    private BookRecommendationResponse toRecommendation(Book book) {
        return new BookRecommendationResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getPrice(),
                book.getStock());
    }

    private List<String> extractKeywords(String text) {
        if (text == null || text.isBlank()) {
            return List.of("sách");
        }
        String[] tokens = text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+");
        List<String> keywords = new ArrayList<>();
        for (String token : tokens) {
            if (token.length() >= 3 && !isStopWord(token)) {
                keywords.add(token);
            }
        }
        return keywords.isEmpty() ? List.of("sách") : keywords.stream().distinct().limit(3).toList();
    }

    private boolean isStopWord(String token) {
        return Set.of("the", "and", "for", "with", "book", "sach", "ban", "toi", "cho", "cua").contains(token);
    }
}
