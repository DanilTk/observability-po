package pl.home.serviceb.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter implements Filter {
	public static final String CORRELATION_ID_HEADER = "correlation-id";

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
		ServletException {

		if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
			HttpServletRequest httpRequest = (HttpServletRequest) request;
			HttpServletResponse httpResponse = (HttpServletResponse) response;

			String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);
			if (correlationId == null || correlationId.isBlank()) {
				correlationId = UUID.randomUUID().toString();
			}

			MDC.put(CORRELATION_ID_HEADER, correlationId);
			httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId);
		}

		try {
			chain.doFilter(request, response);
		} finally {
			MDC.clear();
		}
	}
}
