package garretreichenbach.taskprocessor.service;

import garretreichenbach.taskprocessor.model.Task;
import garretreichenbach.taskprocessor.model.TaskResult;
import garretreichenbach.taskprocessor.model.TaskResultStore;
import garretreichenbach.taskprocessor.processor.TaskProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class TaskThreadPool {

	private final TaskQueueService workQueue;
	private final List<Thread> workerThreads;
	private final AtomicBoolean isRunning;
	private final String workerId;
	private final TaskResultStore resultStore;
	private final int numThreads;

	public TaskThreadPool(String workerId, TaskQueueService workQueue, TaskResultStore resultStore, int numThreads) {
		this.workerId = workerId;
		this.workQueue = workQueue;
		this.resultStore = resultStore;
		this.numThreads = numThreads;
		workerThreads = new ArrayList<>(numThreads);
		isRunning = new AtomicBoolean(false);
	}

	public void start() {
		isRunning.set(true);
		for(int i = 0; i < numThreads; i++) {
			Thread workerThread = new Thread(createWorkerRunnable());
			workerThread.start();
			workerThreads.add(workerThread);
		}
	}

	public void stop() {
		isRunning.set(false);
		for(Thread workerThread : workerThreads) {
			try {
				workerThread.join();
			} catch(InterruptedException exception) {
				Thread.currentThread().interrupt();
			}
		}
	}

	private Runnable createWorkerRunnable() {
		return () -> {
			while(isRunning.get()) {
				Task task = workQueue.takeTask();
				if(task instanceof TaskProcessor processor) {
					TaskResult result = processor.process(task);
					result.setProcessorId(workerId);
					result.setCompletedAt(System.currentTimeMillis());
					resultStore.storeResult(result);
				}
			}
		};
	}

	// Implement methods:
	// 1. start() - Start the worker threads
	// 2. stop() - Gracefully stop the worker threads
	// 3. createWorkerRunnable() - Create a Runnable that processes tasks
}