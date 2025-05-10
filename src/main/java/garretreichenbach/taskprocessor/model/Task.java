package garretreichenbach.taskprocessor.model;

import garretreichenbach.taskprocessor.util.JSONSerializable;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a task to be processed by the system.
 * This class is serializable and comparable, and it contains information about the task's ID, type, parameters, priority, and creation time.
 */
@Slf4j
public class Task implements Serializable, JSONSerializable, Comparable<Task> {

	/**
	 * The unique identifier of the task.
	 */
	@Getter
	private String id;

	/**
	 * The type of the task (e.g., IMAGE_PROCESSING, DATA_ANALYSIS).
	 */
	@Getter
	private TaskType type;

	/**
	 * A map containing the parameters required for processing the task.
	 */
	@Getter
	private Map<String, Object> parameters;

	/**
	 * The priority of the task. Higher values indicate higher priority.
	 */
	@Getter @Setter
	private int priority;

	/**
	 * The timestamp (in milliseconds) when the task was created.
	 */
	@Getter
	private long createdAt;

	/**
	 * Compares this task with another task based on their priority.
	 *
	 * @param o The other task to compare to.
	 * @return A negative integer, zero, or a positive integer as this task's priority
	 *         is less than, equal to, or greater than the other task's priority.
	 */
	@Override
	public int compareTo(Task o) {
		return Integer.compare(priority, o.priority);
	}

	@Override
	public JSONObject toJSON() {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("id", id);
		jsonObject.put("type", type.name());
		jsonObject.put("priority", priority);
		jsonObject.put("createdAt", createdAt);
		serializeParameters(jsonObject);
		return jsonObject;
	}

	@Override
	public void fromJSON(JSONObject jsonObject) {
		id = jsonObject.getString("id");
		type = TaskType.valueOf(jsonObject.getString("type"));
		priority = jsonObject.getInt("priority");
		createdAt = jsonObject.getLong("createdAt");
		parameters = deserializeParameters(jsonObject);
	}

	@Override
	public String toString() {
		return toJSON().toString();
	}

	/**
	 * Serializes the parameters of the task into a JSON object.
	 *
	 * @param jsonObject The JSON object to populate with parameters.
	 */
	public void serializeParameters(JSONObject jsonObject) {
		JSONObject params = new JSONObject();
		for(Map.Entry<String, Object> entry : parameters.entrySet()) {
			try {
				if(entry.getValue() instanceof JSONSerializable jsonSerializable) params.put(entry.getKey(), jsonSerializable.toJSON());
				else params.put(entry.getKey(), entry.getValue());
			} catch(Exception exception) {
				log.error(exception.getMessage(), exception);
			}
		}
		jsonObject.put("parameters", params);
	}

	/**
	 * Deserializes the parameters from a JSON object into a map.
	 *
	 * @param jsonObject The JSON object containing the parameters.
	 * @return A map of parameters.
	 */
	public Map<String, Object> deserializeParameters(JSONObject jsonObject) {
		JSONObject params = jsonObject.getJSONObject("parameters");
		for(String key : params.keySet()) {
			Object value = params.get(key);
			if(value instanceof JSONObject jsonObject1) {
				JSONSerializable jsonSerializable = new JSONSerializable() {
					@Override
					public JSONObject toJSON() {
						return jsonObject1;
					}

					@Override
					public void fromJSON(JSONObject jsonObject) {
						// Implement deserialization logic if needed
					}
				};
				parameters.put(key, jsonSerializable);
			} else parameters.put(key, value);
		}
		return parameters;
	}

	/**
	 * Constructs a new Task with the specified ID, type, parameters, and priority.
	 * The creation time is set to the current system time.
	 *
	 * @param id The unique identifier of the task.
	 * @param type The type of the task.
	 * @param parameters A map of parameters required for the task.
	 * @param priority The priority of the task.
	 */
	public Task(String id, TaskType type, Map<String, Object> parameters, int priority) {
		this.id = id;
		this.type = type;
		this.parameters = parameters;
		this.priority = priority;
		createdAt = System.currentTimeMillis();
	}

	public Task(String type, Map<String, Object> parameters) {
		id = UUID.randomUUID().toString();
		this.type = TaskType.fromString(type);
		this.parameters = parameters;
		priority = 1;
	}
}