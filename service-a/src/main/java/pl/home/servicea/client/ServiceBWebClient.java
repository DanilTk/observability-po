package pl.home.servicea.client;

import org.springframework.stereotype.Component;
import org.springframework.web.service.annotation.GetExchange;

@Component
public interface ServiceBWebClient {

	@GetExchange("/hello")
	String getHelloMessage();

}
