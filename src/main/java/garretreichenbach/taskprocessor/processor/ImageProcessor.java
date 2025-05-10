package garretreichenbach.taskprocessor.processor;

import garretreichenbach.taskprocessor.model.Task;
import garretreichenbach.taskprocessor.model.TaskResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.compressors.deflate.DeflateCompressorInputStream;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ImageProcessor implements TaskProcessor {

	@Override
	public TaskResult process(Task task) {
		Map<String, Object> parameters = task.getParameters();
		Map<String, Object> outputs = new HashMap<>();
		String taskType = (String) parameters.get("task_type");
		byte[] data = (byte[]) parameters.get("data");
		int width = (int) parameters.get("width");
		int height = (int) parameters.get("height");
		try {
			switch(taskType.toLowerCase().trim()) {
				case "scale" -> {
					double scale = (double) parameters.get("scale");
					outputs = scaleImage(data, width, height, scale);
					return TaskResult.success(task.getId(), outputs);
				}
				case "compress" -> {
					String algorithm = (String) parameters.get("algorithm");
					outputs = compressImage(algorithm, data, width, height);
					return TaskResult.success(task.getId(), outputs);
				}
				case "decompress" -> {
					outputs = decompressImage(data, width, height);
					return TaskResult.success(task.getId(), outputs);
				}
				default -> {
					log.error("Task type not supported: {}", taskType);
					return TaskResult.error(task.getId(), new Exception("Unknown task type: " + taskType));
				}
			}
		} catch(Exception exception) {
			log.error(exception.getMessage(), exception);
			return TaskResult.error(task.getId(), exception);
		}
	}

	/**
	 * Scales an image using the specified parameters.
	 * Uses parallel streams to process chunks of an image.
	 * @param data The image data to be scaled.
	 * @param width The width of the image.
	 * @param height The height of the image.
	 * @param scale The scaling factor.
	 */
	public Map<String, Object> scaleImage(byte[] data, int width, int height, double scale) {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		image.getRaster().setDataElements(0, 0, width, height, data);
		int newWidth = (int) (width * scale);
		int newHeight = (int) (height * scale);
		BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
		scaledImage.getGraphics().drawImage(image, 0, 0, newWidth, newHeight, null);
		byte[] scaledData = new byte[newWidth * newHeight * 3];
		scaledImage.getRaster().getDataElements(0, 0, newWidth, newHeight, scaledData);
		return Map.of("scaled_image", scaledData, "width", newWidth, "height", newHeight);
	}

	/**
	 * Compresses an image using the specified algorithm and parameters.
	 * @param algorithm The compression algorithm to be used (e.g., "gzip", "bzip2").
	 * @param data The image data to be compressed.
	 * @param width The width of the image.
	 * @param height The height of the image.
	 * @return A map containing the compressed image data and other parameters.
	 */
	private Map<String, Object> compressImage(String algorithm, byte[] data, int width, int height) {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		image.getRaster().setDataElements(0, 0, width, height, data);
		if(algorithm == null) algorithm = "gz"; // Default to gzip
		try {
			CompressorInputStream cis = new CompressorStreamFactory().createCompressorInputStream(algorithm, new ByteArrayInputStream(data));
			byte[] compressedData = new byte[width * height * 3];
			int bytesRead = cis.read(compressedData);
			if(bytesRead == -1) {
				log.error("Failed to read compressed data");
				return Map.of("error", "Failed to read compressed data");
			}
			cis.close();
			return Map.of("compressed_image", compressedData, "width", width, "height", height);
		} catch(Exception exception) {
			log.error(exception.getMessage(), exception);
			return Map.of("error", exception.getMessage());
		}
	}

	/**
	 * Decompresses an image using the specified algorithm and parameters.
	 * @param data The image data to be decompressed.
	 * @param width The width of the image.
	 * @param height The height of the image.
	 * @return A map containing the decompressed image data and other parameters.
	 */
	private Map<String, Object> decompressImage(byte[] data, int width, int height) {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		image.getRaster().setDataElements(0, 0, width, height, data);
		byte[] decompressedData = new byte[width * height * 3];
		int bytesRead = 0;
		try {
			DeflateCompressorInputStream dis = new DeflateCompressorInputStream(new ByteArrayInputStream(data));
			bytesRead = dis.read(decompressedData);
			if(bytesRead == -1) {
				log.error("Failed to read decompressed data");
				return Map.of("error", "Failed to read decompressed data");
			}
			dis.close();
			BufferedImage decompressedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			decompressedImage.getRaster().setDataElements(0, 0, width, height, decompressedData);
			byte[] finalData = new byte[width * height * 3];
			decompressedImage.getRaster().getDataElements(0, 0, width, height, finalData);
			return Map.of("decompressed_image", finalData, "width", width, "height", height);
		} catch(Exception exception) {
			log.error(exception.getMessage(), exception);
			return Map.of("error", exception.getMessage());
		}
	}
}
