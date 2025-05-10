package garretreichenbach.taskprocessor.service;

import garretreichenbach.taskprocessor.model.Task;
import garretreichenbach.taskprocessor.model.TaskType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TaskQueueServiceTest {

	private TaskQueueService queueService;
	private static final int MAX_QUEUE_SIZE = 10;

	@BeforeEach
	void setUp() {
		queueService = new TaskQueueService(MAX_QUEUE_SIZE);
	}

	@Test
	void testSubmitHighPriorityTask() {
		// Given
		Task highPriorityTask = new Task(UUID.randomUUID().toString(), TaskType.IMAGE_SCALING, new HashMap<>(), 15);

		// When
		TaskQueueService.QueueType queueType = queueService.submitTask(highPriorityTask);

		// Then
		assertEquals(TaskQueueService.QueueType.HIGH, queueType);
	}

	@Test
	void testSubmitNormalPriorityTask() {
		// Given
		Task normalPriorityTask = new Task(UUID.randomUUID().toString(), TaskType.IMAGE_SCALING, new HashMap<>(), 7);

		// When
		TaskQueueService.QueueType queueType = queueService.submitTask(normalPriorityTask);

		// Then
		assertEquals(TaskQueueService.QueueType.NORMAL, queueType);
	}

	@Test
	void testSubmitLowPriorityTask() {
		// Given
		Task lowPriorityTask = new Task(UUID.randomUUID().toString(), TaskType.IMAGE_SCALING, new HashMap<>(), 3);

		// When
		TaskQueueService.QueueType queueType = queueService.submitTask(lowPriorityTask);

		// Then
		assertEquals(TaskQueueService.QueueType.LOW, queueType);
	}

	@Test
	void testQueuePriorities() {
		// Given
		Task highPriorityTask = new Task(UUID.randomUUID().toString(), TaskType.IMAGE_SCALING, new HashMap<>(), 15);
		Task normalPriorityTask = new Task(UUID.randomUUID().toString(), TaskType.IMAGE_SCALING, new HashMap<>(), 7);
		Task lowPriorityTask = new Task(UUID.randomUUID().toString(), TaskType.IMAGE_SCALING, new HashMap<>(), 3);

		// When
		queueService.submitTask(lowPriorityTask);
		queueService.submitTask(normalPriorityTask);
		queueService.submitTask(highPriorityTask);

		// Then
		// High priority task should be taken first
		Task takenTask1 = queueService.takeTask();
		assertNotNull(takenTask1);
		assertEquals(highPriorityTask.getId(), takenTask1.getId());

		// Normal priority task should be taken second
		Task takenTask2 = queueService.takeTask();
		assertNotNull(takenTask2);
		assertEquals(normalPriorityTask.getId(), takenTask2.getId());

		// Low priority task should be taken last
		Task takenTask3 = queueService.takeTask();
		assertNotNull(takenTask3);
		assertEquals(lowPriorityTask.getId(), takenTask3.getId());
	}

	@Test
	void testQueueCapacityLimits() {
		// Given
		List<Task> highPriorityTasks = new ArrayList<>();
		for (int i = 0; i < MAX_QUEUE_SIZE + 5; i++) {
			Task task = new Task(UUID.randomUUID().toString(), TaskType.IMAGE_SCALING, new HashMap<>(), 15);
			highPriorityTasks.add(task);
		}

		// When/Then
		// First MAX_QUEUE_SIZE tasks should go to HIGH queue
		for (int i = 0; i < MAX_QUEUE_SIZE; i++) {
			assertEquals(TaskQueueService.QueueType.HIGH, queueService.submitTask(highPriorityTasks.get(i)));
		}

		// Additional tasks should go to BACKLOG
		for (int i = MAX_QUEUE_SIZE; i < highPriorityTasks.size(); i++) {
			assertEquals(TaskQueueService.QueueType.BACKLOG, queueService.submitTask(highPriorityTasks.get(i)));
		}
	}

	@Test
	void testConcurrentSubmission() throws InterruptedException {
		// Given
		final int numThreads = 10;
		final int tasksPerThread = 10;
		CountDownLatch latch = new CountDownLatch(numThreads);
		AtomicInteger successfulSubmissions = new AtomicInteger(0);

		// When
		ExecutorService executor = Executors.newFixedThreadPool(numThreads);
		for (int i = 0; i < numThreads; i++) {
			executor.submit(() -> {
				try {
					for (int j = 0; j < tasksPerThread; j++) {
						Task task = new Task(UUID.randomUUID().toString(), TaskType.IMAGE_SCALING, new HashMap<>(), 15);
						TaskQueueService.QueueType queueType = queueService.submitTask(task);
						if (queueType != null) {
							successfulSubmissions.incrementAndGet();
						}
					}
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();
		executor.shutdown();

		// Then
		// Some submissions may go to backlog when queue capacities are reached
		assertTrue(successfulSubmissions.get() > 0);

		// Take tasks until none are left
		int tasksTaken = 0;
		while (queueService.takeTask() != null) {
			tasksTaken++;
		}

		// The number of tasks taken should match the number of successful submissions
		assertEquals(successfulSubmissions.get(), tasksTaken);
	}
}