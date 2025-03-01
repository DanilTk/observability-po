package pl.home.serviceb.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BService {

	@KafkaListener(topics = "${backend.kafka.ping-topic}")
	public void ping(ConsumerRecord<String, String> record) {
		log.info("Message received: " + record.value());
	}

}
