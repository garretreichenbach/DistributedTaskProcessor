package garretreichenbach.taskprocessor.processor;

import garretreichenbach.taskprocessor.model.Task;
import garretreichenbach.taskprocessor.model.TaskResult;

public interface TaskProcessor {

	TaskResult process(Task task);
}
