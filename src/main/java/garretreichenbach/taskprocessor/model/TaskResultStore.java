package garretreichenbach.taskprocessor.model;

import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A thread-safe store for task results.
 */
public class TaskResultStore {

	private final ConcurrentHashMap<String, TaskResult> results;
	private final int maxResultsToKeep;

	/**
	 * Creates a new TaskResultStore with the specified maximum number of results to keep.
	 * @param maxResultsToKeep the maximum number of results to keep in the store
	 */
	public TaskResultStore(int maxResultsToKeep) {
		this.maxResultsToKeep = maxResultsToKeep;
		results = new ConcurrentHashMap<>();
	}

	/**
	 * Stores a task result in the store. If the store exceeds the maximum number of results to keep, it will remove the oldest results.
	 * @param result the task result to store
	 */
	public void storeResult(TaskResult result) {
		if(results.size() >= maxResultsToKeep) {
			results.entrySet().stream().sorted(Comparator.comparingLong(e -> e.getValue().getCompletedAt())).limit(results.size() - maxResultsToKeep + 1).forEach(entry -> results.remove(entry.getKey()));
		}
		results.put(result.getTaskId(), result);
	}

	/**
	 * Retrieves a task result from the store by its task ID.
	 * @param taskId the ID of the task whose result to retrieve
	 * @return the task result, or null if not found
	 */
	public TaskResult getResult(String taskId) {
		return results.get(taskId);
	}

	/**
	 * Retrieves the most recent task results from the store, sorted by completion time.
	 * @param limit the maximum number of results to retrieve
	 * @return a map of the most recent task results
	 */
	public ConcurrentHashMap<String, TaskResult> getRecentResults(int limit) {
		return results.entrySet().stream().sorted((e1, e2) -> Long.compare(e2.getValue().getCompletedAt(), e1.getValue().getCompletedAt())).limit(limit).collect(ConcurrentHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), ConcurrentHashMap::putAll);
	}

	/**
	 * Retrieves all task results from the store.
	 * @return a map of all task results
	 */
	public ConcurrentHashMap<String, TaskResult> getAllResults() {
		return new ConcurrentHashMap<>(results);
	}
}

