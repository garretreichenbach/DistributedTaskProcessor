package garretreichenbach.taskprocessor.processor;

import garretreichenbach.taskprocessor.model.Task;
import garretreichenbach.taskprocessor.model.TaskResult;
import garretreichenbach.taskprocessor.model.TaskType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class CustomTaskProcessorTest {

	private CustomTaskProcessor customTaskProcessor;
	private Map<String, Object> parameters;

	@BeforeEach
	void setUp() {
		customTaskProcessor = new CustomTaskProcessor();
		parameters = new HashMap<>();
	}

	@Test
	void testSuccessfulScript() {
		// Given
		String script = getScript("successful_script.lua");

		parameters.put("script", script);
		Task task = new Task("success_test_id", TaskType.CUSTOM_TASK, parameters, 10);

		// When
		TaskResult result = customTaskProcessor.process(task);

		// Then
		assertEquals(TaskResult.ResultStatus.SUCCESS, result.getStatus());

		Map<String, Object> output = result.getOutput();
		assertEquals("Hello from Lua!", output.get("message"));
		assertEquals("42", output.get("computed"));
	}

	@Test
	void testErrorInScript() {
		// Given
		String script = getScript("error_in_script.lua");

		parameters.put("script", script);
		Task task = new Task("error_test_id", TaskType.CUSTOM_TASK, parameters, 10);

		// When
		TaskResult result = customTaskProcessor.process(task);

		// Then
		assertEquals(TaskResult.ResultStatus.FAILURE, result.getStatus());
		assertInstanceOf(NullPointerException.class, result.getOutput().get("error"));
	}

	@Test
	void testTimeoutScript() {
		// Given
		String script = getScript("timeout_script.lua");

		parameters.put("script", script);
		Task task = new Task("timeout_test_id", TaskType.CUSTOM_TASK, parameters, 10);

		// When
		TaskResult result = customTaskProcessor.process(task);

		// Then
		assertEquals(TaskResult.ResultStatus.TIMEOUT, result.getStatus());
	}

	@Test
	void testInvalidScript() {
		// Given
		String script = "This is not valid Lua code";

		parameters.put("script", script);
		Task task = new Task("invalid_script_test_id", TaskType.CUSTOM_TASK, parameters, 10);

		// When
		TaskResult result = customTaskProcessor.process(task);

		// Then
		assertEquals(TaskResult.ResultStatus.FAILURE, result.getStatus());
		assertInstanceOf(Exception.class, result.getOutput().get("error"));
	}

	private String getScript(String scriptName) {
		try {
			return new String(Objects.requireNonNull(getClass().getResourceAsStream("/scripts/" + scriptName)).readAllBytes(), StandardCharsets.UTF_8);
		} catch(Exception exception) {
			exception.printStackTrace();
			return null;
		}
	}
}