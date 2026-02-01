package org.example.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class ForwardedHeadersGatewayFilterFactory extends AbstractGatewayFilterFactory<ForwardedHeadersGatewayFilterFactory.Config> {

    public ForwardedHeadersGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String host = request.getHeaders().getFirst("Host");
            if (host == null) {
                host = "localhost:8080";
            }
            String proto = request.getHeaders().getFirst("X-Forwarded-Proto");
            if (proto == null) {
                proto = "http";
            }
            ServerHttpRequest mutated = request.mutate()
                    .header("X-Forwarded-Host", host)
                    .header("X-Forwarded-Proto", proto)
                    .build();
            return chain.filter(exchange.mutate().request(mutated).build());
        };
    }

    public static class Config {
    }
}
