package pl.home.servicea.controller;

import com.example.common.event.CostItem;
import com.example.common.event.Quotation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.home.servicea.ObjectUtil;
import pl.home.servicea.service.AService;
import pl.home.servicea.service.EventSplitter;

import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class AppController {
	private final AService aService;
	private final EventSplitter eventSplitter;

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

	@GetMapping("/hello/kafka-splitter")
	public void callKafkaSplitter() {
		List<CostItem> newCostItems = ObjectUtil.generateCostItems(1000);
		Quotation quotation = ObjectUtil.generateTestQuotation(1000);
		eventSplitter.processEvent(quotation, newCostItems);
	}

}
