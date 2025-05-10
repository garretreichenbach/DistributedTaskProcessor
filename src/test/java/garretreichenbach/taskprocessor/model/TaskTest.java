package garretreichenbach.taskprocessor.model;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TaskTest {

	@Test
	void testTaskCreation() {
		// Given
		String id = UUID.randomUUID().toString();
		TaskType type = TaskType.IMAGE_SCALING;
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("width", 100);
		parameters.put("height", 200);
		parameters.put("data", new byte[1000]);
		parameters.put("scale", 0.5);
		int priority = 8;

		// When
		Task task = new Task(id, type, parameters, priority);

		// Then
		assertEquals(id, task.getId());
		assertEquals(type, task.getType());
		assertEquals(parameters, task.getParameters());
		assertEquals(priority, task.getPriority());
		assertTrue(task.getCreatedAt() > 0);
	}

	@Test
	void testTaskComparison() {
		// Given
		Task highPriorityTask = new Task(UUID.randomUUID().toString(), TaskType.IMAGE_SCALING, new HashMap<>(), 10);
		Task lowPriorityTask = new Task(UUID.randomUUID().toString(), TaskType.IMAGE_SCALING, new HashMap<>(), 5);

		// Then
		assertTrue(highPriorityTask.compareTo(lowPriorityTask) > 0);
		assertTrue(lowPriorityTask.compareTo(highPriorityTask) < 0);
		assertEquals(0, highPriorityTask.compareTo(highPriorityTask));
	}

	@Test
	void testJsonSerialization() {
		// Given
		String id = UUID.randomUUID().toString();
		TaskType type = TaskType.IMAGE_COMPRESSION;
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("width", 100);
		parameters.put("height", 200);
		parameters.put("data", new byte[100]);
		int priority = 8;

		Task task = new Task(id, type, parameters, priority);

		// When
		JSONObject json = task.toJSON();

		// Then
		assertEquals(id, json.getString("id"));
		assertEquals(type.name(), json.getString("type"));
		assertEquals(priority, json.getInt("priority"));
		assertTrue(json.has("createdAt"));
		assertTrue(json.has("parameters"));
	}

	@Test
	void testTaskFromString() {
		// Given
		String taskType = "scale";
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("width", 100);
		parameters.put("height", 200);

		// When
		Task task = new Task(taskType, parameters);

		// Then
		assertNotNull(task.getId());
		assertEquals(TaskType.IMAGE_SCALING, task.getType());
		assertEquals(parameters, task.getParameters());
		assertEquals(1, task.getPriority());
	}
}