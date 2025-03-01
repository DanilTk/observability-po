package pl.home.servicea.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import pl.home.servicea.client.ServiceBWebClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class AService {
	private final ServiceBWebClient serviceBWebClient;
	private final KafkaTemplate<String, String> kafkaTemplate;

	@Value("${backend.kafka.ping-topic}")
	private String topic;

	public String callExternalWithWebClient() {
		return serviceBWebClient.getHelloMessage();
	}

	public void callKafka() {
		kafkaTemplate.send(topic, "world");
		log.info("Kafka called");
	}

}
