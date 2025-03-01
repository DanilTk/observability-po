package pl.home.serviceb.config;

import org.apache.kafka.clients.consumer.ConsumerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static pl.home.serviceb.config.CorrelationIdFilter.CORRELATION_ID_HEADER;

public class KafkaCorrelationConsumerInterceptor implements ConsumerInterceptor<Object, Object> {

	@Override
	public ConsumerRecords<Object, Object> onConsume(ConsumerRecords<Object, Object> records) {
		records.forEach(record -> {
			String correlationId = null;

			if (record.headers().lastHeader(CORRELATION_ID_HEADER) != null) {
				correlationId = new String(record.headers().lastHeader(CORRELATION_ID_HEADER).value(),
					StandardCharsets.UTF_8);
			}

			if (correlationId == null) {
				correlationId = UUID.randomUUID().toString();
			}

			MDC.put(CORRELATION_ID_HEADER, correlationId);
		});

		return records;
	}

	@Override
	public void configure(Map<String, ?> configs) {
	}

	@Override
	public void close() {
	}

	@Override
	public void onCommit(Map offsets) {
	}

}
