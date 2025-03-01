package pl.home.servicea.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.home.servicea.service.AService;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class AppController {
	private final AService aService;

	@GetMapping("hello")
	public String hello() {
		return "Hello From Service A";
	}

	@GetMapping("/hello/b")
	public String helloB() {
		return aService.callExternalWithWebClient();
	}

	@GetMapping("/hello/kafka")
	public void callKafka() {
		aService.callKafka();
	}
}
