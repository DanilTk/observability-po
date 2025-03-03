package pl.home.servicea;

import com.example.common.event.CostItem;
import com.example.common.event.Quotation;
import com.example.common.event.QuotationEvent;
import lombok.RequiredArgsConstructor;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.Producer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class EventSplitter {
	private final Producer<String, QuotationEvent> kafkaProducer;

	@Value("${kafka.quotation-event-topic}")
	private String quotationEventsTopic;

	@Value("${kafka.max.message.size:900000}")
	private int maxKafkaMessageSize;

	public void processEvent(Quotation quotation) {
		if (isSplitRequired(quotation)) {
			splitAndSend(quotation);
		} else {
			send(quotation);
		}
	}

	private void send(Quotation quotation) {
		//empty
	}

	private void splitAndSend(Quotation quotation) {
		int totalChunks = calculateTotalChunks(List.of(), List.of());
		//send metadata first

		// Step 2: Split Cost Items into Chunks
		List<List<CostItem>> oldItemChunks = chunkList(List.of());
		List<List<CostItem>> newItemChunks = chunkList(List.of());

		int chunkIndex = 1;
		for (int i = 0; i < totalChunks; i++) {
			List<CostItem> oldChunk = i < oldItemChunks.size() ? oldItemChunks.get(i) : new ArrayList<>();
			List<CostItem> newChunk = i < newItemChunks.size() ? newItemChunks.get(i) : new ArrayList<>();
			QuotationEvent chunkEvent = new QuotationEvent(
				quotation.getId(), null,
				null, null,
				oldChunk, newChunk,
				chunkIndex, totalChunks);
			//send
			chunkIndex++;
		}
	}

	private List<List<CostItem>> chunkList(List<CostItem> costItems) {
		List<List<CostItem>> chunks = new ArrayList<>();
		List<CostItem> currentChunk = new ArrayList<>();
		int currentSize = 0;

		for (CostItem item : costItems) {
			int itemSize = estimateAvroSize(item);

			if (currentSize + itemSize > maxKafkaMessageSize && !currentChunk.isEmpty()) {
				chunks.add(new ArrayList<>(currentChunk));
				currentChunk.clear();
				currentSize = 0;
			}

			currentChunk.add(item);
			currentSize += itemSize;
		}

		if (!currentChunk.isEmpty()) {
			chunks.add(currentChunk);
		}

		return chunks;
	}

	private boolean isSplitRequired(Quotation quotation) {
		String jsonRepresentation = quotation.toString();
		return jsonRepresentation.getBytes().length > maxKafkaMessageSize;
	}

	public static <T extends SpecificRecord> int estimateAvroSize(T avroObject) {
		if (avroObject == null) {
			return 0;
		}

		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(outputStream, null);

			DatumWriter<T> writer;
			if (avroObject instanceof SpecificRecord) {
				writer = new SpecificDatumWriter<>(avroObject.getSchema());
			} else {
				writer = new ReflectDatumWriter<>(avroObject.getSchema());
			}

			writer.write(avroObject, encoder);
			encoder.flush();
			return outputStream.size(); // Correct way to get byte length
		} catch (IOException e) {
			throw new RuntimeException("Error estimating Avro size", e);
		}
	}

	private int calculateTotalChunks(List<CostItem> oldCostItems, List<CostItem> newCostItems) {
		int maxSizePerMessage = 900000; // 900KB Kafka message limit

		int totalEstimatedSize = estimateAvroSizeForList(oldCostItems) + estimateAvroSizeForList(newCostItems);

		// Calculate total chunks needed to fit all items within Kafka's size limit
		return (int) Math.ceil((double) totalEstimatedSize / maxSizePerMessage);
	}

	private int estimateAvroSizeForList(List<CostItem> costItems) {
		int totalSize = 0;
		for (CostItem item : costItems) {
			totalSize += estimateAvroSize(item);
		}
		return totalSize;
	}

	private int estimateAvroSizeForListSampled(List<CostItem> costItems) {
		if (costItems.isEmpty()) {
			return 0;
		}

		int sampleSize = Math.min(5, costItems.size());  // Take up to 5 samples
		int totalSampleSize = 0;

		for (int i = 0; i < sampleSize; i++) {
			totalSampleSize += estimateAvroSize(costItems.get(i));
		}

		int avgSizePerItem = totalSampleSize / sampleSize;  // Calculate average size
		return avgSizePerItem * costItems.size();  // Approximate total size
	}
}
