package garretreichenbach.taskprocessor.service;

import garretreichenbach.taskprocessor.model.Task;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TaskQueueService is responsible for managing the task queues.
 * It uses a PriorityBlockingQueue for backlog and LinkedBlockingQueues for high, normal, and low priority tasks.
 * The service ensures that tasks are added to the appropriate queue based on their priority.
 * It also provides methods to take tasks from the queues and check the status of the queues.
 */
@Slf4j
public class TaskQueueService {

	private final int HIGH_PRIORITY_THRESHOLD = 10; //Tasks >= 10 are high priority
	private final int NORMAL_PRIORITY_THRESHOLD = 5; //Tasks >= 5 and < 10 are normal priority
	private final int LOW_PRIORITY_THRESHOLD = 0; //Tasks < 5 are low priority
	private final int TASK_LEVEL_DOWN_THRESHOLD = 3; //If a task is within 3 of a lower level, it will be moved to that level

	private static final PriorityBlockingQueue<Task> backlogQueue = new PriorityBlockingQueue<>();
	private static LinkedBlockingQueue<Task> highPriorityQueue;
	private static LinkedBlockingQueue<Task> normalPriorityQueue;
	private static LinkedBlockingQueue<Task> lowPriorityQueue;
	private final AtomicInteger totalPendingTasks;
	private final int maxQueueSize;

	public enum QueueType {
		HIGH(highPriorityQueue),
		NORMAL(normalPriorityQueue),
		LOW(lowPriorityQueue),
		BACKLOG(backlogQueue);

		private final BlockingQueue<Task> queue;

		QueueType(BlockingQueue<Task> queue) {
			this.queue = queue;
		}

		public BlockingQueue<Task> getQueue() {
			return queue;
		}
	}

	public TaskQueueService(int maxQueueSize) {
		this.maxQueueSize = maxQueueSize;
		highPriorityQueue = new LinkedBlockingQueue<>(maxQueueSize);
		normalPriorityQueue = new LinkedBlockingQueue<>(maxQueueSize);
		lowPriorityQueue = new LinkedBlockingQueue<>(maxQueueSize);
		totalPendingTasks = new AtomicInteger(0);
	}

	@Override
	public String toString() {
		return getQueueStatus();
	}

	/**
	 * Returns the status of the queues.
	 * @return a string representation of the queue status
	 */
	public String getQueueStatus() {
		return String.format("High Priority Queue: %d, Normal Priority Queue: %d, Low Priority Queue: %d, Backlog Queue: %d", highPriorityQueue.size(), normalPriorityQueue.size(), lowPriorityQueue.size(), backlogQueue.size());
	}

	/**
	 * Returns the status of a specific queue.
	 * @param queue the queue to check
	 * @return a string representation of the queue status
	 */
	public String getQueueStatus(QueueType queue) {
		return String.format("Queue %s: %d", queue.name(), queue.getQueue().size());
	}

	/**
	 * Submits a task to the appropriate queue based on its priority.
	 * @param task the task to be submitted
	 */
	public QueueType submitTask(Task task) {
		int priority = task.getPriority();
		if(priority >= HIGH_PRIORITY_THRESHOLD) {
			if(!highPriorityQueue.offer(task)) {
				if(canLevelDown(task)) {
					log.warn("Task {} moved to normal priority queue as the high priority queue is full.", task.getId());
					normalPriorityQueue.offer(task);
					return QueueType.NORMAL;
				} else {
					log.warn("Task {} could not be added to either the high or normal priority queue and must be backlogged.", task.getId());
					backlogQueue.offer(task);
					return QueueType.BACKLOG;
				}
			} else {
				log.info("Task {} added to high priority queue.", task.getId());
				return QueueType.HIGH;
			}
		} else if(priority >= NORMAL_PRIORITY_THRESHOLD) {
			if(!normalPriorityQueue.offer(task)) {
				if(canLevelDown(task)) {
					log.warn("Task {} moved to low priority queue as the normal priority queue is full.", task.getId());
					lowPriorityQueue.offer(task);
					return QueueType.LOW;
				} else {
					log.warn("Task {} could not be added to either the normal or low priority queue and must be backlogged.", task.getId());
					backlogQueue.offer(task);
					return QueueType.BACKLOG;
				}
			} else {
				log.info("Task {} added to normal priority queue.", task.getId());
				return QueueType.NORMAL;
			}
		} else if(priority >= LOW_PRIORITY_THRESHOLD) {
			if(!lowPriorityQueue.offer(task)) {
				log.warn("Task {} moved to backlog as the low priority queue is full.", task.getId());
				backlogQueue.add(task);
				return QueueType.BACKLOG;
			} else {
				log.info("Task {} added to low priority queue.", task.getId());
				return QueueType.LOW;
			}
		} else {
			log.warn("Task {} has a negative priority and has been backlogged.", task.getId());
			backlogQueue.offer(task);
			return QueueType.BACKLOG;
		}
	}

	/**
	 * Takes the highest priority task available from the queues.
	 */
	public Task takeTask() {
		Task task = null;
		if(!highPriorityQueue.isEmpty()) {
			task = highPriorityQueue.poll();
			log.info("Task {} taken from high priority queue.", task.getId());
		} else if(!normalPriorityQueue.isEmpty()) {
			task = normalPriorityQueue.poll();
			log.info("Task {} taken from normal priority queue.", task.getId());
		} else if(!lowPriorityQueue.isEmpty()) {
			task = lowPriorityQueue.poll();
			log.info("Task {} taken from low priority queue.", task.getId());
		} else if(!backlogQueue.isEmpty()) {
			task = backlogQueue.poll();
			log.info("Task {} taken from backlog queue.", task.getId());
		}
		if(task != null) {
			totalPendingTasks.decrementAndGet();
			log.info("Total pending tasks: {}", totalPendingTasks.get());
		}
		return task;
	}

	/**
	 * Checks if the task can be moved up to a lower priority queue.
	 * </br>In the case that the desired queue is full, we can move the task to a lower priority queue if it is within 3 of the lower level's threshold.
	 * @param task the task to check
	 * @return true if the task can be moved up, false otherwise
	 */
	private boolean canLevelDown(Task task) {
		int priority = task.getPriority();
		if(priority >= HIGH_PRIORITY_THRESHOLD) {
			return false;
		} else if(priority >= NORMAL_PRIORITY_THRESHOLD) {
			return highPriorityQueue.size() < maxQueueSize && (highPriorityQueue.size() - priority) <= TASK_LEVEL_DOWN_THRESHOLD;
		} else if(priority >= LOW_PRIORITY_THRESHOLD) {
			return normalPriorityQueue.size() < maxQueueSize && (normalPriorityQueue.size() - priority) <= TASK_LEVEL_DOWN_THRESHOLD;
		} else {
			return lowPriorityQueue.size() < maxQueueSize && (lowPriorityQueue.size() - priority) <= TASK_LEVEL_DOWN_THRESHOLD;
		}
	}
}