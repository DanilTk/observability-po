package pl.home.servicea;

import com.example.common.event.CostItem;
import com.example.common.event.CostSheet;
import com.example.common.event.Quotation;
import com.example.common.event.QuotationAttributes;
import com.example.common.event.Status;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ObjectUtil {

	public static Quotation generateTestQuotation(int oldCostItemCount) {
		QuotationAttributes quotationAttributes = QuotationAttributes.newBuilder()
			.setCurrency("USD")
			.setDiscountRate(10.5)
			.setSalesRepId("SR123")
			.build();

		List<CostItem> costItems = generateCostItems(oldCostItemCount);

		CostSheet costSheet = CostSheet.newBuilder()
			.setTotalCost(5000.00)
			.setDiscountedCost(4500.00)
			.setCostItems(costItems)
			.build();

		return Quotation.newBuilder()
			.setId("Q123")
			.setName("Sample Quotation")
			.setCustomerId("CUST001")
			.setCreatedAt(Instant.now())
			.setValidUntil(Instant.now())
			.setStatus(Status.DRAFT)
			.setQuotationAttributes(quotationAttributes)
			.setCostSheet(costSheet)
			.build();
	}

	public static List<CostItem> generateCostItems(int count) {
		List<CostItem> costItems = new ArrayList<>();

		for (int i = 1; i <= count; i++) {
			CostItem item = CostItem.newBuilder()
				.setId("CI" + i)
				.setProductCode("P" + (1000 + i))
				.setDescription("Test Product " + i)
				.setQuantity((i % 5) + 1)  // Quantity between 1 and 5
				.setUnitPrice(50.0 + (i % 10) * 5) // Prices between 50 and 100
				.setTotalPrice((50.0 + (i % 10) * 5) * ((i % 5) + 1)) // UnitPrice * Quantity
				.setCategory(i % 2 == 0 ? "Electronics" : "Clothing") // Alternating categories
				.build();

			costItems.add(item);
		}

		return costItems;
	}
}
