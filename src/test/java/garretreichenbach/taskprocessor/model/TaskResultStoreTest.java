package garretreichenbach.taskprocessor.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class TaskResultStoreTest {

	private TaskResultStore resultStore;
	private static final int MAX_RESULTS = 5;

	@BeforeEach
	void setUp() {
		resultStore = new TaskResultStore(MAX_RESULTS);
	}

	@Test
	void testStoreAndRetrieveResult() {
		// Given
		String taskId = UUID.randomUUID().toString();
		TaskResult result = TaskResult.success(taskId, new HashMap<>());
		result.setCompletedAt(System.currentTimeMillis());

		// When
		resultStore.storeResult(result);
		TaskResult retrieved = resultStore.getResult(taskId);

		// Then
		assertNotNull(retrieved);
		assertEquals(taskId, retrieved.getTaskId());
	}

	@Test
	void testGetRecentResults() {
		// Given
		for(int i = 0; i < 10; i++) {
			String taskId = UUID.randomUUID().toString();
			TaskResult result = TaskResult.success(taskId, new HashMap<>());
			result.setCompletedAt(System.currentTimeMillis() + i * 1000); // Ensure different timestamps
			resultStore.storeResult(result);
		}

		// When
		ConcurrentHashMap<String, TaskResult> recentResults = resultStore.getRecentResults(3);

		// Then
		assertEquals(3, recentResults.size());
	}

	@Test
	void testMaxResultsLimit() {
		// Given
		Map<String, TaskResult> allResults = new HashMap<>();
		for(int i = 0; i < MAX_RESULTS + 5; i++) {
			String taskId = UUID.randomUUID().toString();
			TaskResult result = TaskResult.success(taskId, new HashMap<>());
			result.setCompletedAt(System.currentTimeMillis() + i * 1000);
			allResults.put(taskId, result);
			resultStore.storeResult(result);
		}

		// When
		ConcurrentHashMap<String, TaskResult> storedResults = resultStore.getAllResults();

		// Then
		assertTrue(storedResults.size() <= MAX_RESULTS, "Store should respect max results limit");
	}
}