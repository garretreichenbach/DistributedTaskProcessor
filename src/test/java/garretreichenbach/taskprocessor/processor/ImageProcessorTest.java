package garretreichenbach.taskprocessor.processor;

import garretreichenbach.taskprocessor.model.Task;
import garretreichenbach.taskprocessor.model.TaskResult;
import garretreichenbach.taskprocessor.model.TaskType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ImageProcessorTest {

	private ImageProcessor imageProcessor;
	private Map<String, Object> parameters;
	private String taskId;

	@BeforeEach
	void setUp() {
		imageProcessor = new ImageProcessor();
		parameters = new HashMap<>();
		taskId = UUID.randomUUID().toString();
	}

	@Test
	void testScaleImage() {
		// Given
		int width = 100;
		int height = 100;
		double scale = 0.5;
		byte[] data = new byte[width * height * 3];

		parameters.put("task_type", "scale");
		parameters.put("width", width);
		parameters.put("height", height);
		parameters.put("scale", scale);
		parameters.put("data", data);

		Task task = new Task(taskId, TaskType.IMAGE_SCALING, parameters, 10);

		// When
		TaskResult result = imageProcessor.process(task);

		// Then
		assertEquals(TaskResult.ResultStatus.SUCCESS, result.getStatus());
		assertEquals(taskId, result.getTaskId());

		Map<String, Object> output = result.getOutput();
		assertNotNull(output.get("scaled_image"));
		assertEquals((int)(width * scale), output.get("width"));
		assertEquals((int)(height * scale), output.get("height"));
	}

	@Test
	void testUnknownTaskType() {
		// Given
		parameters.put("task_type", "unknown");
		parameters.put("width", 100);
		parameters.put("height", 100);
		parameters.put("data", new byte[100 * 100 * 3]);

		Task task = new Task(taskId, TaskType.IMAGE_SCALING, parameters, 10);

		// When
		TaskResult result = imageProcessor.process(task);

		// Then
		assertEquals(TaskResult.ResultStatus.FAILURE, result.getStatus());
		assertInstanceOf(Exception.class, result.getOutput().get("error"));
		assertTrue(((Exception)result.getOutput().get("error")).getMessage().contains("Unknown task type"));
	}
}