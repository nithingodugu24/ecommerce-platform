package com.nithingodugu.ecommerce.orderservice.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Slf4j
@Component
public class LoggingFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = "req-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        }

        MDC.put("requestId", requestId);

        response.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            log.debug("Incoming request",
                    kv("method", request.getMethod()),
                    kv("path", request.getRequestURI())
            );

            filterChain.doFilter(request, response);

        } finally {

            long duration = System.currentTimeMillis() - startTime;
            int  status   = response.getStatus();

            if (duration > 2000) {
                log.warn("Slow request detected",
                        kv("method", request.getMethod()),
                        kv("path", request.getRequestURI()),
                        kv("durationMs", duration));
            }else if (status >= 500) {
                log.error("Request completed",
                        kv("method", request.getMethod()),
                        kv("path", request.getRequestURI()),
                        kv("status", status),
                        kv("durationMs", duration));
            } else if (status >= 400) {
                log.warn("Request completed",
                        kv("method", request.getMethod()),
                        kv("path", request.getRequestURI()),
                        kv("status", status),
                        kv("durationMs", duration));
            } else {
                log.info("Request completed",
                        kv("method", request.getMethod()),
                        kv("path", request.getRequestURI()),
                        kv("status", status),
                        kv("durationMs", duration));
            }


            MDC.clear();
        }
    }
}
