package garretreichenbach.taskprocessor.processor;

import garretreichenbach.taskprocessor.lua.LuaEnvironment;
import garretreichenbach.taskprocessor.model.Task;
import garretreichenbach.taskprocessor.model.TaskResult;
import lombok.extern.slf4j.Slf4j;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A custom implementation of the TaskProcessor interface that allows for user-defined task processing
 * using Lua scripts. This class processes tasks by executing Lua scripts provided in the task parameters.
 */
@Slf4j
public class CustomTaskProcessor implements TaskProcessor {

	/**
	 * Processes a given task by executing a Lua script provided in the task's parameters.
	 *
	 * @param task The task to be processed.
	 * @return A TaskResult object containing the result of the task processing.
	 */
	@Override
	public TaskResult process(Task task) {
		// Allows for user-defined task processing via Lua scripts
		Map<String, Object> parameters = task.getParameters();
		String script = (String) parameters.get("script");
		try {
			LuaValue value = LuaEnvironment.create(script);
			if(value == null || value.isnil()) return TaskResult.error(task.getId(), new Exception("Script returned nil"));
			if(value.isfunction()) {
				LuaValue result = value.call(LuaValue.tableOf(), LuaValue.valueOf(task.getId()));
				Map<String, Object> output = new HashMap<>();
				if(result.istable()) {
					LuaTable resultTable = result.checktable();
					for(LuaValue key : resultTable.keys()) {
						LuaValue valueResult = resultTable.get(key);
						if(valueResult.isstring()) output.put(key.tojstring(), valueResult.tojstring());
						else if(valueResult.isnumber()) output.put(key.tojstring(), valueResult.todouble());
						else if(valueResult.isboolean()) output.put(key.tojstring(), valueResult.toboolean());
						else output.put(key.tojstring(), valueResult);
					}
				} else output.put("result", result);
				if(output.containsKey("error")) {
					Exception exception = getExceptionType((String) output.get("error"), (String) output.get("description"));
					return TaskResult.error(task.getId(), Objects.requireNonNullElseGet(exception, () -> new Exception("Unknown Error: " + output.get("error"))));
				} else if(output.containsKey("timeout")) {
					return TaskResult.timeout(task.getId());
				} else {
					return TaskResult.success(task.getId(), output);
				}
			} else {
				return TaskResult.error(task.getId(), new Exception("Script did not return a function"));
			}
		} catch(Exception exception) {
			String id = task.getId();
			log.error("[CustomTaskProcessor({}):process] Error processing task: {}", id, exception.getMessage());
			return TaskResult.error(id, exception);
		}
	}

	/**
	 * Determines the type of exception to be thrown based on the error type and description.
	 *
	 * @param errorType The type of error (e.g., "null", "index", "io").
	 * @param description A description of the error.
	 * @return An Exception object corresponding to the error type, or null if the error type is unknown.
	 */
	private Exception getExceptionType(String errorType, String description) {
		return switch(errorType.toLowerCase()) {
			case "null", "none", "nil" -> new NullPointerException("Null Pointer Exception: " + description);
			case "index" -> new IndexOutOfBoundsException("Index Out Of Bounds Exception: " + description);
			case "io" -> new IOException("IO Exception: " + description);
			case "class" -> new ClassCastException("Class Cast Exception: " + description);
			case "illegal" -> new IllegalArgumentException("Illegal Argument Exception: " + description);
			case "timeout" -> new InterruptedException("Timeout Exception: " + description);
			case "type" -> new ClassCastException("Type Exception: " + description);
			case "number" -> new NumberFormatException("Number Format Exception: " + description);
			default -> null;
		};
	}
}