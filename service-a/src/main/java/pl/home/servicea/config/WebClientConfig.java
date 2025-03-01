package pl.home.servicea.config;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import pl.home.servicea.client.ServiceBWebClient;

import java.util.UUID;

import static pl.home.servicea.config.CorrelationIdFilter.CORRELATION_ID_HEADER;

@Configuration
public class WebClientConfig {

	@Value("${backend.client-url.b-service}")
	private String bServiceUrl;

	@Bean
	public ServiceBWebClient serviceBWebClient() {
		WebClient webClient = getWebClient();
		HttpServiceProxyFactory factory = HttpServiceProxyFactory.builder()
			.exchangeAdapter(WebClientAdapter.create(webClient))
			.build();
		return factory.createClient(ServiceBWebClient.class);
	}

	public WebClient getWebClient() {
		return WebClient.builder()
			.baseUrl(bServiceUrl)
			.filter(correlationIdFilter())
			.build();
	}

	private ExchangeFilterFunction correlationIdFilter() {
		return (clientRequest, next) -> {
			String correlationId = MDC.get(CORRELATION_ID_HEADER);

			if (correlationId == null) {
				correlationId = UUID.randomUUID().toString();
			}

			ClientRequest newRequest = ClientRequest.from(clientRequest)
				.header(CORRELATION_ID_HEADER, correlationId)
				.build();

			return next.exchange(newRequest);
		};
	}
}
