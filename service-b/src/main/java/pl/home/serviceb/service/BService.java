package pl.home.serviceb.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import static pl.home.serviceb.config.CorrelationIdFilter.CORRELATION_ID_HEADER;

@Slf4j
@Service
public class BService {

	@KafkaListener(topics = "${backend.kafka.ping-topic}")
	public void ping(@Header(name = CORRELATION_ID_HEADER, required = false) String correlationId,
					 ConsumerRecord<String, String> record) {
		log.info("Message received: {} with correlation id: {}", record.value(), correlationId);
	}

}
