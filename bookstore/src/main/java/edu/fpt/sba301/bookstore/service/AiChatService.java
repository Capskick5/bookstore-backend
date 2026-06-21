package edu.fpt.sba301.bookstore.service;

import edu.fpt.sba301.bookstore.dto.request.ChatRequest;
import edu.fpt.sba301.bookstore.dto.response.ChatResponse;
import edu.fpt.sba301.bookstore.dto.response.ConversationResponse;
import edu.fpt.sba301.bookstore.dto.response.MessageResponse;
import edu.fpt.sba301.bookstore.dto.response.PageResponse;
import edu.fpt.sba301.bookstore.entity.User;

public interface AiChatService {
    ChatResponse chat(User user, ChatRequest request);

    PageResponse<ConversationResponse> listConversations(User user, int page, int size);

    PageResponse<MessageResponse> listMessages(User user, Long conversationId, int page, int size);

    void deleteConversation(User user, Long conversationId);
}
