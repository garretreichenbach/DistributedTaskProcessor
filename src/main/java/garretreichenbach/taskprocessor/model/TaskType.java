package garretreichenbach.taskprocessor.model;

import garretreichenbach.taskprocessor.processor.CustomTaskProcessor;
import garretreichenbach.taskprocessor.processor.ImageProcessor;
import garretreichenbach.taskprocessor.processor.TaskProcessor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

@Slf4j
public enum TaskType {
	//Image Processing Tasks
//	IMAGE_MANIPULATION,
	IMAGE_COMPRESSION("compress", ImageProcessor.class, Map.of(
			"data", byte[].class,
			"width", int.class,
			"height", int.class
	), () -> {
		try {
			//Generate random image data
			int width = (int) (Math.random() * 1000);
			int height = (int) (Math.random() * 1000);
			byte[] data = new byte[width * height * 3];
			for(int i = 0; i < data.length; i++) data[i] = (byte) (Math.random() * 256);
			return new Task("compress", Map.of(
					"data", data,
					"width", width,
					"height", height
			));
		} catch(OutOfMemoryError | Exception error) {
			log.error(error.getMessage(), error);
			return null;
		}
	}),
	IMAGE_DECOMPRESSION("decompress", ImageProcessor.class, Map.of(
			"data", byte[].class,
			"width", int.class,
			"height", int.class
	), null),
	IMAGE_SCALING("scale", ImageProcessor.class, Map.of(
			"data", byte[].class,
			"width", int.class,
			"height", int.class,
			"scale", double.class
	), () -> {
		try {
			//Generate random image data
			int width = (int) (Math.random() * 1000);
			int height = (int) (Math.random() * 1000);
			double scale = Math.random();
			byte[] data = new byte[width * height * 3];
			for(int i = 0; i < data.length; i++) data[i] = (byte) (Math.random() * 256);
			return new Task("scale", Map.of(
					"data", data,
					"width", width,
					"height", height,
					"scale", scale
			));
		} catch(OutOfMemoryError | Exception error) {
			log.error(error.getMessage(), error);
			return null;
		}
	}),
//	IMAGE_CONVERSION,

	//Data Processing Tasks
//	DATA_ANALYSIS,
//	DATA_CLEANING,

	//Numerical Tasks
//	MATHEMATICAL_COMPUTATION,

	//Text Processing Tasks
//	TEXT_ANALYSIS,
//	TEXT_EXTRACTION,

	//Audio Processing Tasks
//	AUDIO_MANIPULATION,
//	AUDIO_COMPRESSION,
//	AUDIO_DECOMPRESSION,
//	AUDIO_CONVERSION,

	//Video Processing Tasks
//	VIDEO_MANIPULATION,
//	VIDEO_COMPRESSION,
//	VIDEO_DECOMPRESSION,
//	VIDEO_CONVERSION,

	//Misc.
	CUSTOM_TASK("custom", CustomTaskProcessor.class, Map.of("script", String.class, "task_id", String.class), null);

	private final String name;
	private final Class<? extends TaskProcessor> processor;
	private final Map<String, Class<?>> parameters;
	private final RandomTaskGenerator randomTaskGenerator;

	/**
	 * Constructor for TaskType enum.
	 *
	 * @param processor   The class of the processor associated with this task type.
	 * @param parameters  A map of parameters required for the task type.
	 */
	TaskType(String name, Class<? extends TaskProcessor> processor, Map<String, Class<?>> parameters, RandomTaskGenerator randomTaskGenerator) {
		this.name = name;
		this.processor = processor;
		this.parameters = parameters;
		this.randomTaskGenerator = randomTaskGenerator;
	}

	public static TaskType getRandom() {
		TaskType[] values = values();
		int randomIndex = (int) (Math.random() * values.length);
		TaskType random = values[randomIndex];
		if(random.randomTaskGenerator == null) return getRandom();
		return random;
	}

	@Override
	public String toString() {
		return name == null ? super.toString() : name;
	}

	public static TaskType fromString(String name) {
		for(TaskType type : values()) {
			if(type.toString().equalsIgnoreCase(name)) return type;
		}
		throw new IllegalArgumentException("No TaskType found for name: " + name);
	}

	/**
	 * Creates a new instance of the processor associated with this task type.
	 * @param task The task for which the processor is created.
	 * @return An instance of the processor associated with this task type.
	 */
	public TaskProcessor createProcessor(Task task) {
		try {
			//Check if the parameters match
			Map<String, Object> taskParameters = task.getParameters();
			taskParameters.put("task_type", toString());
			for(String key : parameters.keySet()) {
				if(!taskParameters.containsKey(key)) throw new IllegalArgumentException("Missing parameter: " + key);
				if(!parameters.get(key).isInstance(taskParameters.get(key))) throw new IllegalArgumentException("Invalid type for parameter: " + key);
			}
			return processor.getDeclaredConstructor().newInstance();
		} catch(NoSuchMethodException exception) {
			throw new RuntimeException("No default constructor found for processor: " + processor, exception);
		} catch(InstantiationException | IllegalAccessException exception) {
			throw new RuntimeException("Failed to instantiate processor: " + processor, exception);
		} catch(InvocationTargetException exception) {
			throw new RuntimeException("Failed to invoke constructor for processor: " + processor, exception);
		} catch(Exception exception) {
			throw new RuntimeException("Failed to create processor for task type: " + this, exception);
		}
	}

	/**
	 * Generates a random task of this type.
	 * @return A Task object representing the generated task.
	 */
	public Task generateRandom() {
		if(randomTaskGenerator == null) throw new UnsupportedOperationException("Random task generation is not supported for this task type.");
		return randomTaskGenerator.generate();
	}
}