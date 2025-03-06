package pl.home.serviceb.config;

import com.example.common.event.QuotationEvent;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

	@Value("${spring.kafka.bootstrap-servers}")
	private String bootstrapServers;

	@Value("${spring.kafka.properties.schema.registry.url}")
	private String schemaRegistryUrl;

	@Bean
	public Map<String, Object> consumerConfigs() {
		Map<String, Object> props = new HashMap<>();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers); // Kafka Broker
		props.put(ConsumerConfig.GROUP_ID_CONFIG, "quotation-consumer-group"); // Consumer Group ID
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"); // Start from earliest if no offset is stored
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
		props.put("schema.registry.url", schemaRegistryUrl); // Schema Registry URL
		props.put("specific.avro.reader", true); // Use Specific Avro Record

		return props;
	}

	@Bean
	public ConsumerFactory<String, QuotationEvent> consumerFactory() {
		return new DefaultKafkaConsumerFactory<>(consumerConfigs());
	}

	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, QuotationEvent> kafkaListenerContainerFactory() {
		ConcurrentKafkaListenerContainerFactory<String, QuotationEvent> factory =
			new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(consumerFactory());
		return factory;
	}

}
