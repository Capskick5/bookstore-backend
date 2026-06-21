package edu.fpt.sba301.bookstore.controller;

import edu.fpt.sba301.bookstore.dto.response.ApiResponse;
import edu.fpt.sba301.bookstore.service.RagReindexService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/ai")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminAiController {

    private final RagReindexService ragReindexService;

    @PostMapping("/reindex")
    public ResponseEntity<ApiResponse<Void>> triggerReindex() {
        ragReindexService.reindexAsync();

        ApiResponse<Void> response = new ApiResponse<>();
        response.setCode(202);
        response.setMessage("Accepted");
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
