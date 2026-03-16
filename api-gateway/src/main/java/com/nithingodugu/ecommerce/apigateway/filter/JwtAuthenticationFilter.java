package com.nithingodugu.ecommerce.apigateway.filter;

import com.nithingodugu.ecommerce.apigateway.security.jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class JwtAuthenticationFilter implements GlobalFilter {

    private final JwtUtil jwtUtil;
    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);


    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Value("${security.public-endpoints}")
    private List<String> publicEndpoints;

    private boolean isPublicPath(String path){
        return publicEndpoints.stream().anyMatch(path::startsWith);
    }


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();
        String requestId = exchange.getRequest().getHeaders().getFirst("X-Request-ID");
        
        if(isPublicPath(path)){
            return chain.filter(exchange);
        }


        String authHeader = exchange
                .getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if(authHeader == null || !authHeader.startsWith("Bearer")){
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try{
            String token = authHeader.substring(7);
            Claims claims = jwtUtil.validateToken(token);
            String userId = claims.getSubject();
            String userRole = claims.get("role").toString();

            // Check if it is an admin route, if it is then check role
            if (path.startsWith("/admin") && !userRole.equalsIgnoreCase("ADMIN")){
                log.warn(
                        "Unauthorized admin access attempt",
                        StructuredArguments.kv("requestId", requestId),
                        StructuredArguments.kv("userId", userId),
                        StructuredArguments.kv("role", userRole)

                        );
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            ServerHttpRequest mutatedRequest = exchange.getRequest()
                    .mutate()
                    .header("X-USER-ID", userId)
                    .header("X-USER-ROLE", userRole)

                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        }catch (Exception ex){
            log.error(
                    "JWT validation failed",
                    StructuredArguments.kv("requestId", requestId),
                    StructuredArguments.kv("error", ex.getMessage())
            );
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

    }
}
