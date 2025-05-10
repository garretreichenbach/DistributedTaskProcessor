package garretreichenbach.taskprocessor.model;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TaskResultTest {

	@Test
	void testSuccessResult() {
		// Given
		String taskId = UUID.randomUUID().toString();
		Map<String, Object> output = new HashMap<>();
		output.put("processed", true);
		output.put("processingTimeMs", 1500L);

		// When
		TaskResult result = TaskResult.success(taskId, output);

		// Then
		assertEquals(taskId, result.getTaskId());
		assertEquals(TaskResult.ResultStatus.SUCCESS, result.getStatus());
		assertEquals(output.get("processed"), result.getOutput().get("processed"));
		assertEquals(output.get("processingTimeMs"), result.getOutput().get("processingTimeMs"));
	}

	@Test
	void testErrorResult() {
		// Given
		String taskId = UUID.randomUUID().toString();
		Exception exception = new RuntimeException("Test exception");

		// When
		TaskResult result = TaskResult.error(taskId, exception);

		// Then
		assertEquals(taskId, result.getTaskId());
		assertEquals(TaskResult.ResultStatus.FAILURE, result.getStatus());
		assertEquals(exception, result.getOutput().get("error"));
	}

	@Test
	void testTimeoutResult() {
		// Given
		String taskId = UUID.randomUUID().toString();

		// When
		TaskResult result = TaskResult.timeout(taskId);

		// Then
		assertEquals(taskId, result.getTaskId());
		assertEquals(TaskResult.ResultStatus.TIMEOUT, result.getStatus());
	}

	@Test
	void testCompletedAtAndProcessorId() {
		// Given
		String taskId = UUID.randomUUID().toString();
		TaskResult result = TaskResult.success(taskId, new HashMap<>());
		long completedAt = System.currentTimeMillis();
		String processorId = "processor-1";

		// When
		result.setCompletedAt(completedAt);
		result.setProcessorId(processorId);

		// Then
		assertEquals(completedAt, result.getCompletedAt());
		assertEquals(processorId, result.getProcessorId());
	}
}