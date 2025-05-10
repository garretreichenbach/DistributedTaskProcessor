package garretreichenbach.taskprocessor.service;

import garretreichenbach.taskprocessor.model.Task;
import garretreichenbach.taskprocessor.model.TaskResult;
import garretreichenbach.taskprocessor.model.TaskResultStore;
import garretreichenbach.taskprocessor.processor.TaskProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TaskThreadPoolTest {

	private TaskQueueService mockQueueService;
	private TaskResultStore mockResultStore;
	private TaskThreadPool taskThreadPool;
	private final String workerId = UUID.randomUUID().toString();
	private final int numThreads = 2;

	@BeforeEach
	void setUp() {
		mockQueueService = mock(TaskQueueService.class);
		mockResultStore = mock(TaskResultStore.class);
		taskThreadPool = new TaskThreadPool(workerId, mockQueueService, mockResultStore, numThreads);
	}

	@Test
	void testStartAndStop() throws InterruptedException {
		// Given
		Task mockTask = mock(Task.class);
		TaskProcessor mockProcessor = mock(TaskProcessor.class);

		when(mockQueueService.takeTask()).thenReturn(mockTask).thenReturn(null);
		when(mockProcessor.process(any())).thenReturn(mock(TaskResult.class));

		// When
		taskThreadPool.start();

		// Give threads time to process
		Thread.sleep(200);

		taskThreadPool.stop();

		// Then
		verify(mockQueueService, atLeastOnce()).takeTask();
	}

	@Test
	void testProcessorExecution() throws InterruptedException {
		// Given
		CountDownLatch latch = new CountDownLatch(1);

		// Create a task that is also a TaskProcessor
		Task mockTask = mock(Task.class, withSettings().extraInterfaces(TaskProcessor.class));
		when(((TaskProcessor)mockTask).process(any())).thenAnswer(invocation -> {
			latch.countDown();
			return TaskResult.success(mockTask.getId(), new HashMap<>());
		});

		// Make the queue service return our test processor-task
		when(mockQueueService.takeTask()).thenReturn(mockTask).thenReturn(null);
		when(mockTask.getId()).thenReturn(UUID.randomUUID().toString());

		// When
		taskThreadPool.start();

		// Wait for processing to complete or timeout
		boolean processed = latch.await(1, TimeUnit.SECONDS);

		taskThreadPool.stop();

		// Then
		assertTrue(processed, "Task should have been processed");
		verify(mockResultStore, times(1)).storeResult(any(TaskResult.class));
	}
}
