package pl.discountapp.infrastructure.adapter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import pl.discountapp.infrastructure.adapter.web.dto.ApiError;

import java.io.IOException;
import java.time.Instant;

@Component
@Order(1)
@RequiredArgsConstructor
public class RateLimitFilter implements Filter {

    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        var httpReq = (HttpServletRequest) request;
        var clientIp = extractClientIp(httpReq);

        if (!rateLimitService.tryConsume(clientIp)) {
            var httpRes = (HttpServletResponse) response;
            httpRes.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            httpRes.setContentType(MediaType.APPLICATION_JSON_VALUE);
            var error = new ApiError(
                    Instant.now(),
                    HttpStatus.TOO_MANY_REQUESTS.value(),
                    ErrorCode.RATE_LIMIT_EXCEEDED,
                    "Rate limit exceeded",
                    MDC.get("correlationId")
            );
            objectMapper.writeValue(httpRes.getWriter(), error);
            return;
        }
        chain.doFilter(request, response);
    }

    private String extractClientIp(HttpServletRequest request) {
        var xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
