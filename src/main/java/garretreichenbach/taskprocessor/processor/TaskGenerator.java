package garretreichenbach.taskprocessor.processor;

import garretreichenbach.taskprocessor.model.Task;
import garretreichenbach.taskprocessor.model.TaskType;

import java.net.http.HttpClient;
import java.time.Duration;

public class TaskGenerator {

	private final String apiEndpoint;
	private final HttpClient httpClient;

	public TaskGenerator(String apiEndpoint) {
		this.apiEndpoint = apiEndpoint;
		httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
	}

	/**
	 * Generates a random task.
	 * @return A JSON string representing the task.
	 */
	public String generateRandomTask() {
		TaskType taskType = TaskType.getRandom();
		Task task = taskType.generateRandom();
		submitTask(task);
		return task.toString();
	}

	/**
	 * Submits a task to the API endpoint.
	 * @param task The task to submit.
	 */
	public void submitTask(Task task) {
		String jsonTask = task.toString();

	}
}