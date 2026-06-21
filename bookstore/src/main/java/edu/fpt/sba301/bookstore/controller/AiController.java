package edu.fpt.sba301.bookstore.controller;

import edu.fpt.sba301.bookstore.dto.request.ChatRequest;
import edu.fpt.sba301.bookstore.dto.response.ApiResponse;
import edu.fpt.sba301.bookstore.dto.response.ChatResponse;
import edu.fpt.sba301.bookstore.dto.response.ConversationResponse;
import edu.fpt.sba301.bookstore.dto.response.MessageResponse;
import edu.fpt.sba301.bookstore.dto.response.PageResponse;
import edu.fpt.sba301.bookstore.entity.User;
import edu.fpt.sba301.bookstore.service.AiChatService;
import edu.fpt.sba301.bookstore.support.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.security.Principal;

import static edu.fpt.sba301.bookstore.config.SwaggerConfig.BEARER_AUTH;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Validated
@Tag(name = "AI Chat", description = "RAG chatbot conversations")
@SecurityRequirement(name = BEARER_AUTH)
public class AiController {

    private final AiChatService aiChatService;
    private final CurrentUserService currentUserService;

    @Operation(summary = "Send a chat message and receive an AI answer")
    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<ChatResponse>> chat(
            @Valid @RequestBody ChatRequest request,
            Principal principal) {
        User user = currentUserService.requireUser(principal);
        ChatResponse data = aiChatService.chat(user, request);

        ApiResponse<ChatResponse> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("OK");
        response.setData(data);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<PageResponse<ConversationResponse>>> listConversations(
            Principal principal,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        User user = currentUserService.requireUser(principal);
        PageResponse<ConversationResponse> data = aiChatService.listConversations(user, page, size);

        ApiResponse<PageResponse<ConversationResponse>> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("OK");
        response.setData(data);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<ApiResponse<PageResponse<MessageResponse>>> listMessages(
            @PathVariable Long id,
            Principal principal,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        User user = currentUserService.requireUser(principal);
        PageResponse<MessageResponse> data = aiChatService.listMessages(user, id, page, size);

        ApiResponse<PageResponse<MessageResponse>> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("OK");
        response.setData(data);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/conversations/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteConversation(
            @PathVariable Long id,
            Principal principal) {
        User user = currentUserService.requireUser(principal);
        aiChatService.deleteConversation(user, id);

        ApiResponse<Void> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("Deleted");
        return ResponseEntity.ok(response);
    }
}
