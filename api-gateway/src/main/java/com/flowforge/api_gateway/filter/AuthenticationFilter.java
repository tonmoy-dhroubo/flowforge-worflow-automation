package com.flowforge.api_gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final RouterValidator validator;
    private final JwtDecoder jwtDecoder;

    public AuthenticationFilter(RouterValidator validator, JwtDecoder jwtDecoder) {
        super(Config.class);
        this.validator = validator;
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            if (validator.isSecured.test(exchange.getRequest())) {
                // Check for auth header
                if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                    return onError(exchange, "Missing authorization header", HttpStatus.UNAUTHORIZED);
                }

                String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    return onError(exchange, "Invalid authorization header", HttpStatus.UNAUTHORIZED);
                }

                String token = authHeader.substring(7);

                try {
                    // Decode and validate the JWT
                    Jwt jwt = jwtDecoder.decode(token);
                    
                    // Extract user_id claim and add it as a header
                    String userId = jwt.getClaimAsString("user_id");
                    if (userId == null) {
                        return onError(exchange, "JWT token is missing user_id claim", HttpStatus.UNAUTHORIZED);
                    }

                    // Mutate the request to add the new header
                    ServerWebExchange modifiedExchange = exchange.mutate()
                            .request(request -> request.header("X-User-Id", userId))
                            .build();
                    
                    return chain.filter(modifiedExchange);

                } catch (JwtException e) {
                    log.error("JWT validation error: {}", e.getMessage());
                    return onError(exchange, "Unauthorized: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
                }
            }
            // If the endpoint is public, just pass through
            return chain.filter(exchange);
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        exchange.getResponse().setStatusCode(httpStatus);
        return exchange.getResponse().setComplete();
    }

    public static class Config {
        // Put any configuration properties for the filter here
    }
}