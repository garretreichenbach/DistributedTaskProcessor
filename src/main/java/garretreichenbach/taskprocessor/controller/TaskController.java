package garretreichenbach.taskprocessor.controller;

import garretreichenbach.taskprocessor.model.Task;
import garretreichenbach.taskprocessor.model.TaskResult;
import garretreichenbach.taskprocessor.model.TaskResultStore;
import garretreichenbach.taskprocessor.service.TaskQueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/tasks")
public class TaskController {

	private final TaskQueueService queueService;
	private final TaskResultStore resultStore;

	@Autowired
	public TaskController(TaskQueueService queueService, TaskResultStore resultStore) {
		this.queueService = queueService;
		this.resultStore = resultStore;
	}

	@PostMapping
	public ResponseEntity<Task> submitTask(@RequestBody Task task) {
		TaskQueueService.QueueType addedTo = queueService.submitTask(task);
		if(addedTo == null) return ResponseEntity.status(503).body(null);
		return ResponseEntity.status(202).body(task);
	}

	@GetMapping("/{taskId}/result")
	public ResponseEntity<?> getTaskResult(@PathVariable String taskId) {
		TaskResult result = resultStore.getResult(taskId);
		if(result == null) return ResponseEntity.notFound().build();
		return ResponseEntity.ok(result);
	}

	@GetMapping("/status")
	public ResponseEntity<?> getAllQueueStatus() {
		return ResponseEntity.ok(queueService.getQueueStatus());
	}

	@GetMapping("/status/{queueName}")
	public ResponseEntity<?> getQueueStatus(@PathVariable String queueName) {
		return switch(queueName.toLowerCase()) {
			case "high" -> ResponseEntity.ok(queueService.getQueueStatus(TaskQueueService.QueueType.HIGH));
			case "normal" -> ResponseEntity.ok(queueService.getQueueStatus(TaskQueueService.QueueType.NORMAL));
			case "low" -> ResponseEntity.ok(queueService.getQueueStatus(TaskQueueService.QueueType.LOW));
			default -> ResponseEntity.badRequest().body("Invalid queue name");
		};
	}

	@GetMapping("/results")
	public ResponseEntity<?> getAllResults() {
		return ResponseEntity.ok(resultStore.getAllResults());
	}

	@GetMapping("/results/recent")
	public ResponseEntity<?> getRecentResults(@RequestParam(defaultValue = "10") int limit) {
		return ResponseEntity.ok(resultStore.getRecentResults(limit));
	}

	@GetMapping("/results/{taskId}")
	public ResponseEntity<?> getResultById(@PathVariable String taskId) {
		TaskResult result = resultStore.getResult(taskId);
		if(result == null) return ResponseEntity.notFound().build();
		return ResponseEntity.ok(result);
	}

	@GetMapping("/results/{queueName}/recent")
	public ResponseEntity<?> getRecentResultsByQueue(@PathVariable String queueName, @RequestParam(defaultValue = "10") int limit) {
		return switch(queueName.toLowerCase()) {
			case "high", "normal", "low" -> ResponseEntity.ok(resultStore.getRecentResults(limit));
			default -> ResponseEntity.badRequest().body("Invalid queue name");
		};
	}

	@GetMapping("/results/{queueName}")
	public ResponseEntity<?> getResultsByQueue(@PathVariable String queueName) {
		return switch(queueName.toLowerCase()) {
			case "high", "normal", "low" -> ResponseEntity.ok(resultStore.getAllResults());
			default -> ResponseEntity.badRequest().body("Invalid queue name");
		};
	}
}