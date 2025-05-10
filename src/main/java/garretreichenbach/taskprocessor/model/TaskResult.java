package garretreichenbach.taskprocessor.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the result of a task processed by the system.
 * This class is serializable and contains information about the task's ID, status, output,
 * completion time, and the processor that handled it.
 */
public class TaskResult implements Serializable {

	public static TaskResult success(String taskId, Map<String, Object> output) {
		TaskResult result = new TaskResult(taskId, ResultStatus.SUCCESS);
		result.output.putAll(output);
		return result;
	}

	public static TaskResult error(String taskId, Exception exception) {
		TaskResult result = new TaskResult(taskId, ResultStatus.FAILURE);
		result.output.put("error", exception);
		return result;
	}

	public static TaskResult timeout(String taskId) {
		return new TaskResult(taskId, ResultStatus.TIMEOUT);
	}

	/**
	 * The unique identifier of the task.
	 */
	@Getter
	private final String taskId;

	/**
	 * The status of the task result (e.g., SUCCESS, FAILURE, TIMEOUT).
	 */
	@Getter @Setter
	private ResultStatus status;

	/**
	 * A map containing the output data of the task.
	 */
	@Getter
	private final Map<String, Object> output = new HashMap<>();

	/**
	 * The timestamp (in milliseconds) when the task was completed.
	 */
	@Getter @Setter
	private long completedAt;

	/**
	 * The identifier of the processor that handled the task.
	 */
	@Getter @Setter
	private String processorId;

	/**
	 * Enum representing the possible statuses of a task result.
	 */
	public enum ResultStatus {
		/**
		 * Indicates the task completed successfully.
		 */
		SUCCESS,
		/**
		 * Indicates the task failed.
		 */
		FAILURE,
		/**
		 * Indicates the task timed out.
		 */
		TIMEOUT
	}

	/**
	 * Constructs a new TaskResult with the specified task ID and status.
	 *
	 * @param taskId The unique identifier of the task.
	 * @param status The status of the task result.
	 */
	private TaskResult(String taskId, ResultStatus status) {
		this.taskId = taskId;
		this.status = status;
	}
}