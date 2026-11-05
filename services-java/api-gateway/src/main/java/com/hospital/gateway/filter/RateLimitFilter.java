package com.hospital.gateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory fixed-window rate limiter. For production this is where a
 * Redis-backed limiter (Bucket4j) would slot in, but the behaviour is the same.
 */
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    private final int limitPerMinute;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimitFilter(@Value("${hospital.rate-limit.requests-per-minute:240}") int limitPerMinute) {
        this.limitPerMinute = limitPerMinute;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (path.startsWith("/actuator")) {
            return chain.filter(exchange);
        }
        String client = clientKey(exchange);
        long minute = Instant.now().getEpochSecond() / 60;
        Window window = windows.compute(client, (key, current) -> {
            if (current == null || current.minute() != minute) {
                return new Window(minute, new AtomicInteger(1));
            }
            current.counter().incrementAndGet();
            return current;
        });
        if (windows.size() > 10_000) {
            windows.entrySet().removeIf(e -> e.getValue().minute() < minute - 1);
        }
        if (window.counter().get() > limitPerMinute) {
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            String body = "{\"status\":429,\"code\":\"RATE_LIMITED\",\"message\":\"Too many requests\"}";
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
            return exchange.getResponse().writeWith(Mono.just(buffer));
        }
        return chain.filter(exchange);
    }

    private String clientKey(ServerWebExchange exchange) {
        InetSocketAddress remote = exchange.getRequest().getRemoteAddress();
        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return remote == null ? "unknown" : remote.getAddress().getHostAddress();
    }

    @Override
    public int getOrder() {
        return -200;
    }

    private record Window(long minute, AtomicInteger counter) {
    }
}
