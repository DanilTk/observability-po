package pl.home.serviceb.service;

import com.example.common.event.CostItem;
import com.example.common.event.CostSheet;
import com.example.common.event.CostSheetMetadata;
import com.example.common.event.Quotation;
import com.example.common.event.QuotationAttributes;
import com.example.common.event.QuotationAttributesMetadata;
import com.example.common.event.QuotationEvent;
import com.example.common.event.QuotationMetadata;
import com.example.common.event.Status;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventAggregator {
	private final Map<String, List<QuotationEvent>> quotationBuffer = new ConcurrentHashMap<>();
	private final Map<String, Long> quotationTimestamps = new ConcurrentHashMap<>();
	private final ScheduledExecutorService cleanupScheduler = Executors.newScheduledThreadPool(1);
	private static final long CHUNK_TIMEOUT_MS = 30000;

//	@Value("${backend.kafka.quotation-event-topic}")
//	private String quotationEventsTopic;

//	public void QuotationAggregator() {
//		cleanupScheduler.scheduleAtFixedRate(this::cleanupExpiredQuotations, 30, 30, TimeUnit.SECONDS);
//	}

	@KafkaListener(topics = "${backend.kafka.quotation-event-topic}", groupId = "quotation-consumer-group",
		containerFactory = "kafkaListenerContainerFactory")
	public void consumeQuotationChunk(ConsumerRecord<String, QuotationEvent> record) {
		QuotationEvent event = record.value();
		String chunkedEventKey = record.key(); // 🔥 Use Kafka key instead of quotationId

		System.out.println("📥 Received chunk " + event.getChunkIndex() + "/" + event.getTotalChunks() + " for Key: " + chunkedEventKey);

		quotationBuffer.computeIfAbsent(chunkedEventKey, k -> new ArrayList<>()).add(event);
		quotationTimestamps.put(chunkedEventKey, System.currentTimeMillis());

		// If all chunks are received, process the full quotation
		if (quotationBuffer.get(chunkedEventKey).size() == event.getTotalChunks()) {
			Quotation fullQuotation = assembleQuotation(quotationBuffer.get(chunkedEventKey));
			System.out.println("✅ Reconstructed full Quotation for Key: " + chunkedEventKey);
			quotationBuffer.remove(chunkedEventKey);
			quotationTimestamps.remove(chunkedEventKey);
		}
	}

	private void cleanupExpiredQuotations() {
		long now = System.currentTimeMillis();
		quotationTimestamps.forEach((chunkedEventKey, timestamp) -> {
			if (now - timestamp > CHUNK_TIMEOUT_MS) {
				System.err.println("⚠ Incomplete Quotation discarded for Key: " + chunkedEventKey);
				quotationBuffer.remove(chunkedEventKey);
				quotationTimestamps.remove(chunkedEventKey);
			}
		});
	}

	private Quotation assembleQuotation(List<QuotationEvent> events) {
		validateChunks(events);

		events.sort(Comparator.comparingInt(QuotationEvent::getChunkIndex));

		QuotationEvent metadataEvent = events.get(0);
		QuotationMetadata metadata = metadataEvent.getQuotationMetadata();
		QuotationAttributesMetadata attributesMetadata = metadataEvent.getQuotationAttributesMetadata();
		CostSheetMetadata costSheetMetadata = metadataEvent.getCostSheetMetadata();

		List<CostItem> oldCostItems = new ArrayList<>();
		List<CostItem> newCostItems = new ArrayList<>();
		for (QuotationEvent event : events) {
			oldCostItems.addAll(event.getOldCostItems());
			newCostItems.addAll(event.getNewCostItems());
		}

		QuotationAttributes quotationAttributes = QuotationAttributes.newBuilder()
			.setCurrency(attributesMetadata.getCurrency())
			.setDiscountRate(attributesMetadata.getDiscountRate())
			.setSalesRepId(attributesMetadata.getSalesRepId())
			.build();

		CostSheet costSheet = CostSheet.newBuilder()
			.setTotalCost(costSheetMetadata.getTotalCost())
			.setDiscountedCost(costSheetMetadata.getDiscountedCost())
			.setCostItems(newCostItems)
			.build();

		return Quotation.newBuilder()
			.setId(metadata.getId())
			.setName(metadata.getName())
			.setCustomerId(metadata.getCustomerId())
			.setCreatedAt(metadata.getCreatedAt())
			.setValidUntil(metadata.getValidUntil())
			.setStatus(Status.valueOf(metadata.getStatus().toString()))
			.setQuotationAttributes(quotationAttributes) // Use separate variable
			.setCostSheet(costSheet) // Use separate variable
			.build();
	}

	private void validateChunks(List<QuotationEvent> events) {
		int totalChunks = events.get(0).getTotalChunks();
		Set<Integer> receivedChunkIndexes = events.stream()
			.map(QuotationEvent::getChunkIndex)
			.collect(Collectors.toSet());

		for (int expectedIndex = 0; expectedIndex < totalChunks; expectedIndex++) {
			if (!receivedChunkIndexes.contains(expectedIndex + 1)) {
				throw new IllegalStateException("❌ Missing chunk " + expectedIndex + " out of " + totalChunks);
			}
		}
	}

}
