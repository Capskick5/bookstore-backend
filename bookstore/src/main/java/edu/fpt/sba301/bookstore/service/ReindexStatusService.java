package edu.fpt.sba301.bookstore.service;

import edu.fpt.sba301.bookstore.dto.response.ReindexTaskResponse;

import java.util.List;

public interface ReindexStatusService {

    String startTask(String source);

    void markRunning(String taskId);

    void markCompleted(String taskId);

    void markFailed(String taskId, String message);

    List<ReindexTaskResponse> listTasks();
}
