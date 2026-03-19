package com.nithingodugu.ecommerce.apigateway.filter;

import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.UUID;

@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);

    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String UPSTREAM_HEADER = "X-Upstream-Service";


    @Override
    public int getOrder() {

        //on top of all but before micrometer mdc injection which uses order(Ordered.HIGHEST_PRECEDENCE)
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        long startTime = System.currentTimeMillis();

        String requestId = request.getHeaders().getFirst(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()){
            requestId = "req-" + UUID.randomUUID().toString().replace("-","").substring(0,8);
        }

        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        String upstreamService = resolveUpstreamService(route);

        String finalRequestId = requestId;

        ServerHttpRequest mutatedRequest = request.mutate()
                .headers(headers -> {
                    headers.set(REQUEST_ID_HEADER, finalRequestId);
                    headers.set(UPSTREAM_HEADER, upstreamService);
                })
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();

        log.info("Incoming request",
                StructuredArguments.kv("method",     request.getMethod().name()),
                StructuredArguments.kv("path",       request.getPath().value()),
                StructuredArguments.kv("requestId",  finalRequestId),
                StructuredArguments.kv("upstream",   upstreamService),
                StructuredArguments.kv("clientIp",   getClientIp(request))
        );

        mutatedExchange.getResponse().getHeaders().add(REQUEST_ID_HEADER, requestId);

        return chain.filter(mutatedExchange)
                .doOnSuccess(v -> logResponse(mutatedExchange, finalRequestId, upstreamService, startTime))
                .doOnError(error -> logError(mutatedExchange, finalRequestId, upstreamService, startTime, error));
    }

    private void logResponse(ServerWebExchange exchange,
                             String requestId,
                             String upstreamService,
                             long startTime) {

        ServerHttpResponse response  = exchange.getResponse();
        int    status   = response.getStatusCode() != null ? response.getStatusCode().value() : 0;
        long   duration = System.currentTimeMillis() - startTime;
        String method   = exchange.getRequest().getMethod().name();
        String path     = exchange.getRequest().getPath().value();

        Object[] fields = {
                StructuredArguments.kv("method",      method),
                StructuredArguments.kv("path",        path),
                StructuredArguments.kv("status",      status),
                StructuredArguments.kv("durationMs", duration),
                StructuredArguments.kv("requestId",   requestId),
                StructuredArguments.kv("upstream",    upstreamService)
        };

        if (status >= 500) {
            log.error("Request completed", fields);
        } else if (status >= 400) {
            log.warn("Request completed",  fields);
        } else {
            log.info("Request completed",  fields);
        }

        if (duration > 2000) {
            log.warn("Slow request detected",
                    StructuredArguments.kv("method",      method),
                    StructuredArguments.kv("path",        path),
                    StructuredArguments.kv("durationMs", duration),
                    StructuredArguments.kv("requestId",   requestId)
            );
        }
    }


    private void logError(ServerWebExchange exchange,
                          String requestId,
                          String upstreamService,
                          long startTime,
                          Throwable error) {

        long duration = System.currentTimeMillis() - startTime;

        log.error("Request failed",
                StructuredArguments.kv("method",      exchange.getRequest().getMethod().name()),
                StructuredArguments.kv("path",        exchange.getRequest().getPath().value()),
                StructuredArguments.kv("durationMs", duration),
                StructuredArguments.kv("requestId",   requestId),
                StructuredArguments.kv("upstream",    upstreamService),
                StructuredArguments.kv("error",       error.getMessage()),
                error
        );
    }




    private String resolveUpstreamService(Route route){
        if (route != null){
            URI uri = route.getUri();
            return uri.getHost().toUpperCase();
        }

        return "UNKNOWN";
    }

    private String getClientIp(ServerHttpRequest request) {
        String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // X-Forwarded-For can be a comma-separated list: "clientIp, proxy1, proxy2"
            // The first entry is the actual client
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddress() != null
                ? request.getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
    }

}
