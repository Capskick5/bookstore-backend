package edu.fpt.sba301.bookstore.controller;

import edu.fpt.sba301.bookstore.dto.response.ApiResponse;
import edu.fpt.sba301.bookstore.dto.response.ReindexTaskResponse;
import edu.fpt.sba301.bookstore.service.RagReindexService;
import edu.fpt.sba301.bookstore.service.ReindexStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

import static edu.fpt.sba301.bookstore.config.SwaggerConfig.BEARER_AUTH;

@RestController
@RequestMapping("/api/admin/ai")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin AI", description = "RAG reindex management")
@SecurityRequirement(name = BEARER_AUTH)
public class AdminAiController {

    private final RagReindexService ragReindexService;
    private final ReindexStatusService reindexStatusService;

    @Operation(summary = "Trigger an asynchronous RAG reindex")
    @PostMapping("/reindex")
    public ResponseEntity<ApiResponse<Void>> triggerReindex() {
        ragReindexService.reindexAsync();

        ApiResponse<Void> response = new ApiResponse<>();
        response.setCode(202);
        response.setMessage("Accepted");
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @Operation(summary = "List recent RAG reindex tasks and their status")
    @GetMapping("/reindex/status")
    public ResponseEntity<ApiResponse<List<ReindexTaskResponse>>> reindexStatus() {
        List<ReindexTaskResponse> data = reindexStatusService.listTasks();

        ApiResponse<List<ReindexTaskResponse>> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("OK");
        response.setData(data);
        return ResponseEntity.ok(response);
    }
}
