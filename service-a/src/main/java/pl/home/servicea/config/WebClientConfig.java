package pl.home.servicea.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import pl.home.servicea.client.ServiceBWebClient;

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
			.build();
	}
}
