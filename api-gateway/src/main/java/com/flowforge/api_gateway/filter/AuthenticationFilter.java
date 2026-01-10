package com.flowforge.api_gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    public AuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> ReactiveSecurityContextHolder.getContext()
                .flatMap(securityContext -> {
                    log.info("AuthenticationFilter is running for request to: {}", exchange.getRequest().getURI());
                    System.out.println("AuthenticationFilter is running for request to: " + exchange.getRequest().getURI());
                    if (securityContext.getAuthentication() != null && securityContext.getAuthentication().getPrincipal() instanceof Jwt) {
                        Jwt jwt = (Jwt) securityContext.getAuthentication().getPrincipal();
                        String userId = jwt.getClaimAsString("user_id");

                        if (userId == null) {
                            log.warn("JWT token is missing user_id claim");
                            return onError(exchange, "JWT token is missing user_id claim", HttpStatus.UNAUTHORIZED);
                        }

                        ServerWebExchange modifiedExchange = exchange.mutate()
                                .request(request -> request.header("X-User-Id", userId))
                                .build();

                        return chain.filter(modifiedExchange);
                    }
                    return chain.filter(exchange);
                });
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        exchange.getResponse().setStatusCode(httpStatus);
        return exchange.getResponse().setComplete();
    }

    public static class Config {
    }
}
