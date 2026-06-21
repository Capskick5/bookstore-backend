package edu.fpt.sba301.bookstore.service.imp;

import edu.fpt.sba301.bookstore.dto.response.ReindexTaskResponse;
import edu.fpt.sba301.bookstore.service.ReindexStatusService;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class ReindexStatusServiceImpl implements ReindexStatusService {

    private static final int MAX_TASKS = 20;

    private final Deque<ReindexTaskResponse> tasks = new ConcurrentLinkedDeque<>();

    @Override
    public String startTask(String source) {
        String taskId = UUID.randomUUID().toString();
        OffsetDateTime now = OffsetDateTime.now();
        tasks.addFirst(new ReindexTaskResponse(
                taskId,
                source,
                "PENDING",
                now,
                null,
                null));
        trimTasks();
        return taskId;
    }

    @Override
    public void markRunning(String taskId) {
        updateTask(taskId, "RUNNING", null, null);
    }

    @Override
    public void markCompleted(String taskId) {
        OffsetDateTime now = OffsetDateTime.now();
        updateTask(taskId, "COMPLETED", now, null);
    }

    @Override
    public void markFailed(String taskId, String message) {
        updateTask(taskId, "FAILED", null, message);
    }

    @Override
    public List<ReindexTaskResponse> listTasks() {
        return new ArrayList<>(tasks);
    }

    private void updateTask(
            String taskId,
            String status,
            OffsetDateTime lastIndexedAt,
            String message) {
        List<ReindexTaskResponse> updated = new ArrayList<>(tasks.size());
        for (ReindexTaskResponse task : tasks) {
            if (task.id().equals(taskId)) {
                updated.add(new ReindexTaskResponse(
                        task.id(),
                        task.source(),
                        status,
                        task.startedAt(),
                        lastIndexedAt != null ? lastIndexedAt : task.lastIndexedAt(),
                        message != null ? message : task.message()));
            } else {
                updated.add(task);
            }
        }
        tasks.clear();
        tasks.addAll(updated);
    }

    private void trimTasks() {
        while (tasks.size() > MAX_TASKS) {
            tasks.removeLast();
        }
    }
}
