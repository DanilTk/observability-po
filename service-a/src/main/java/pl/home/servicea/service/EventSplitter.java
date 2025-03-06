package pl.home.servicea.service;

import com.example.common.event.CostItem;
import com.example.common.event.CostSheetMetadata;
import com.example.common.event.Quotation;
import com.example.common.event.QuotationAttributesMetadata;
import com.example.common.event.QuotationEvent;
import com.example.common.event.QuotationMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventSplitter {
	private final Producer<String, QuotationEvent> kafkaProducer;

	@Value("${backend.kafka.quotation-event-topic}")
	private String quotationEventsTopic;

	@Value("${kafka.max.message.size:900000}")
	private int maxKafkaMessageSize;

	public void processEvent(Quotation quotation, List<CostItem> newCostItems) {
		splitAndSend(quotation, newCostItems);
	}

	private void splitAndSend(Quotation quotation, List<CostItem> newCostItems) {
		String key = UUID.randomUUID().toString();
		List<CostItem> oldCostItems = quotation.getCostSheet().getCostItems();
		int totalChunks = calculateTotalChunks(oldCostItems, newCostItems);
		sendFirstChunk(quotation, totalChunks, key);

		// Step 2: Split Cost Items into Chunks
		List<List<CostItem>> oldItemChunks = chunkList(oldCostItems);
		List<List<CostItem>> newItemChunks = chunkList(newCostItems);

		int chunkIndex = 1;
		for (int i = 0; i < totalChunks; i++) {
			List<CostItem> oldChunk = i < oldItemChunks.size() ? oldItemChunks.get(i) : new ArrayList<>();
			List<CostItem> newChunk = i < newItemChunks.size() ? newItemChunks.get(i) : new ArrayList<>();

			QuotationEvent metadataEvent = QuotationEvent.newBuilder()
				.setQuotationId(quotation.getId())
				.setQuotationMetadata(null)
				.setQuotationAttributesMetadata(null)
				.setCostSheetMetadata(null)
				.setOldCostItems(oldChunk)
				.setNewCostItems(newChunk)
				.setChunkIndex(chunkIndex + 1)
				.setTotalChunks(totalChunks + 1)
				.build();

			ProducerRecord<String, QuotationEvent> record = new ProducerRecord<>(quotationEventsTopic, key,
				metadataEvent);
			kafkaProducer.send(record);

			chunkIndex++;
		}
	}

	private void sendFirstChunk(Quotation quotation, int totalChunks, String key) {
		QuotationMetadata quotationMetadata = QuotationMetadata.newBuilder()
			.setId(quotation.getId())
			.setName(quotation.getName())
			.setCustomerId(quotation.getCustomerId())
			.setCreatedAt(quotation.getCreatedAt())
			.setValidUntil(quotation.getValidUntil())
			.setStatus(quotation.getStatus().toString())
			.build();

		QuotationAttributesMetadata quotationAttributesMetadata = QuotationAttributesMetadata.newBuilder()
			.setCurrency(quotation.getQuotationAttributes().getCurrency())
			.setDiscountRate(quotation.getQuotationAttributes().getDiscountRate())
			.setSalesRepId(quotation.getQuotationAttributes().getSalesRepId())
			.build();

		CostSheetMetadata costSheetMetadata = CostSheetMetadata.newBuilder()
			.setTotalCost(quotation.getCostSheet().getTotalCost())
			.setDiscountedCost(quotation.getCostSheet().getDiscountedCost())
			.build();

		QuotationEvent metadataEvent = QuotationEvent.newBuilder()
			.setQuotationId(quotation.getId())
			.setQuotationMetadata(quotationMetadata)
			.setQuotationAttributesMetadata(quotationAttributesMetadata)
			.setCostSheetMetadata(costSheetMetadata)
			.setOldCostItems(List.of())
			.setNewCostItems(List.of())
			.setChunkIndex(1)
			.setTotalChunks(totalChunks + 1)
			.build();

		ProducerRecord<String, QuotationEvent> record = new ProducerRecord<>(quotationEventsTopic, key, metadataEvent);
		kafkaProducer.send(record);
	}

	private List<List<CostItem>> chunkList(List<CostItem> costItems) {
		int maxItemsPerChunk = 100; // Fixed item-based chunking
		List<List<CostItem>> chunkedList = new ArrayList<>();

		for (int i = 0; i < costItems.size(); i += maxItemsPerChunk) {
			int end = Math.min(costItems.size(), i + maxItemsPerChunk);
			chunkedList.add(new ArrayList<>(costItems.subList(i, end))); // Properly create sublists
		}

		return chunkedList;
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
		int maxItemsPerChunk = 100; // Cutoff threshold

		int oldChunks = (int) Math.ceil((double) oldCostItems.size() / maxItemsPerChunk);
		int newChunks = (int) Math.ceil((double) newCostItems.size() / maxItemsPerChunk);

		int totalChunks = Math.max(oldChunks, newChunks);

		return totalChunks;
	}

	private int estimateAvroSizeForList(List<CostItem> costItems) {
		int totalSize = 0;
		for (CostItem item : costItems) {
			totalSize += estimateAvroSize(item);
		}
		return totalSize;
	}

	private boolean isSplitRequired(Quotation quotation, List<CostItem> newCostItems) {
		// Convert Quotation (excluding cost items) to JSON for size estimation
		String quotationJson = quotation.toString();
		int quotationSize = quotationJson.getBytes().length;

		// Estimate the size of cost items using Avro serialization
//		int oldCostItemsSize = estimateAvroSizeForList(quotation.getCostSheet().getCostItems());
		int newCostItemsSize = estimateAvroSizeForList(newCostItems);

		// Calculate the total estimated size
		int totalSize = quotationSize + newCostItemsSize;

		// Determine if splitting is required
		boolean isRequired = totalSize > maxKafkaMessageSize;
		return true;
	}

}
