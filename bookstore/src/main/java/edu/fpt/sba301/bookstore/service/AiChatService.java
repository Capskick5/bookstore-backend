package edu.fpt.sba301.bookstore.service;

import edu.fpt.sba301.bookstore.dto.request.ChatRequest;
import edu.fpt.sba301.bookstore.dto.response.ChatResponse;
import edu.fpt.sba301.bookstore.entity.User;

public interface AiChatService {
    ChatResponse chat(User user, ChatRequest request);
}
