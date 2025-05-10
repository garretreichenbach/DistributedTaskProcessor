package garretreichenbach.taskprocessor.controller;

import garretreichenbach.taskprocessor.model.Task;
import garretreichenbach.taskprocessor.model.TaskResult;
import garretreichenbach.taskprocessor.model.TaskResultStore;
import garretreichenbach.taskprocessor.model.TaskType;
import garretreichenbach.taskprocessor.service.TaskQueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class TaskControllerTest {

	private TaskQueueService mockQueueService;
	private TaskResultStore mockResultStore;
	private TaskController taskController;

	@BeforeEach
	void setUp() {
		mockQueueService = Mockito.mock(TaskQueueService.class);
		mockResultStore = Mockito.mock(TaskResultStore.class);
		taskController = new TaskController(mockQueueService, mockResultStore);
	}

	@Test
	void testSubmitTask() {
		// Given
		Task task = new Task(
				UUID.randomUUID().toString(),
				TaskType.IMAGE_SCALING,
				new HashMap<>(),
				10
		);

		when(mockQueueService.submitTask(any(Task.class)))
				.thenReturn(TaskQueueService.QueueType.HIGH);

		// When
		ResponseEntity<Task> response = taskController.submitTask(task);

		// Then
		assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
		assertEquals(task, response.getBody());
	}

	@Test
	void testSubmitTaskWhenQueueFull() {
		// Given
		Task task = new Task(
				UUID.randomUUID().toString(),
				TaskType.IMAGE_SCALING,
				new HashMap<>(),
				10
		);

		when(mockQueueService.submitTask(any(Task.class)))
				.thenReturn(null);

		// When
		ResponseEntity<Task> response = taskController.submitTask(task);

		// Then
		assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
		assertNull(response.getBody());
	}

	@Test
	void testGetTaskResult() {
		// Given
		String taskId = UUID.randomUUID().toString();
		TaskResult taskResult = TaskResult.success(taskId, new HashMap<>());

		when(mockResultStore.getResult(taskId)).thenReturn(taskResult);

		// When
		ResponseEntity<?> response = taskController.getTaskResult(taskId);

		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(taskResult, response.getBody());
	}

	@Test
	void testGetTaskResultNotFound() {
		// Given
		String taskId = UUID.randomUUID().toString();

		when(mockResultStore.getResult(taskId)).thenReturn(null);

		// When
		ResponseEntity<?> response = taskController.getTaskResult(taskId);

		// Then
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
	}

	@Test
	void testGetQueueStatus() {
		// Given
		String queueStatus = "High Priority Queue: 5, Normal Priority Queue: 3, Low Priority Queue: 1, Backlog Queue: 0";

		when(mockQueueService.getQueueStatus()).thenReturn(queueStatus);

		// When
		ResponseEntity<?> response = taskController.getAllQueueStatus();

		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(queueStatus, response.getBody());
	}

	@Test
	void testGetRecentResults() {
		// Given
		int limit = 5;
		ConcurrentHashMap<String, TaskResult> recentResults = new ConcurrentHashMap<>();

		for (int i = 0; i < limit; i++) {
			String taskId = UUID.randomUUID().toString();
			recentResults.put(taskId, TaskResult.success(taskId, new HashMap<>()));
		}

		when(mockResultStore.getRecentResults(limit)).thenReturn(recentResults);

		// When
		ResponseEntity<?> response = taskController.getRecentResults(limit);

		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(recentResults, response.getBody());
	}
}