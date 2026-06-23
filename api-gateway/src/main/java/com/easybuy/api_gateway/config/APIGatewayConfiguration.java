package com.easybuy.api_gateway.config;

import com.easybuy.api_gateway.filter.AuthenticationFilter;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Configuration
public class APIGatewayConfiguration {

    public APIGatewayConfiguration(AuthenticationFilter authenticationFilter) {
        this.authenticationFilter = authenticationFilter;
    }

    private final AuthenticationFilter authenticationFilter;


    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder
                .routes()
                .route("Product-Category-service",
                        predicateSpec -> predicateSpec.path("/product-category-service/**")
                                .filters(filter -> filter
                                        .filter(authenticationFilter.apply(new AuthenticationFilter.Config()))
                                        .rewritePath("/product-category-service/?(?<remaining>.*)", "/${remaining}")
                                        .requestRateLimiter(rateLimiter ->
                                                rateLimiter
                                                        .setKeyResolver(userIdKeyResolver())
                                                        .setRateLimiter(redisRateLimiter())

                                        )
                                        .circuitBreaker(cb -> {
                                            cb
                                                    .setName("product-category-service-circuit-breaker")
                                                    .setFallbackUri("forward:/product-category-service-fallback");

                                        })
                                        .retry(retryConfig -> {
                                            retryConfig
                                                    .setRetries(3)
                                                    .setMethods(HttpMethod.GET, HttpMethod.POST) // remember this retry will only work for get and post methods
                                                    .setBackoff(
                                                            Duration.ofMillis(100),   // First retry after 100ms
                                                            Duration.ofMillis(1000),  // Do not retry for more than 1000ms
                                                            2,                        // Multiplying factor for retry time gaps
                                                            true                      // States use multiplying factor
                                                    );
                                        })
                                )
                                .uri("lb://PRODUCT-CATEGORY-SERVICE")
                )

                .route("Cart-Order-Service",
                        predicateSpec -> predicateSpec.path("/cart-order-service/**")
                                .filters(filter -> filter
                                        .filter(authenticationFilter.apply(new AuthenticationFilter.Config()))
                                        .rewritePath("/cart-order-service/?(?<remaining>.*)", "/${remaining}")
                                        .requestRateLimiter(rateLimiter ->
                                                rateLimiter
                                                        .setKeyResolver(userIdKeyResolver())
                                                        .setRateLimiter(redisRateLimiter())

                                        )
                                        .circuitBreaker(cb -> {
                                            cb
                                                    .setName("cart-order-service-circuit-breaker")
                                                    .setFallbackUri("forward:/cart-order-service-fallback");

                                        })
                                        .retry(retryConfig -> {
                                            retryConfig
                                                    .setRetries(3)
                                                    .setMethods(HttpMethod.GET, HttpMethod.POST) // remember this retry will only work for get and post methods
                                                    .setBackoff(
                                                            Duration.ofMillis(100),   // First retry after 100ms
                                                            Duration.ofMillis(1000),  // Do not retry for more than 1000ms
                                                            2,                        // Multiplying factor for retry time gaps
                                                            true                      // States use multiplying factor
                                                    );
                                        })
                                )
                                .uri("lb://CART-ORDER-SERVICE")
                )

                .route("User-Service-Route",
                        predicateSpec -> predicateSpec.path("/user-service/**")
                                .filters(filter-> filter
                                        .filter(authenticationFilter.apply(new AuthenticationFilter.Config()))
                                            .rewritePath("/user-service/?(?<remaining>.*)", "/${remaining}")
                                            .requestRateLimiter(rateLimiter ->
                                                        rateLimiter
                                                                .setKeyResolver(userIdKeyResolver())
                                                                .setRateLimiter(redisRateLimiter())
                                            )
                                            .circuitBreaker(circuitBreaker -> circuitBreaker
                                                    .setName("user-service-circuit-breaker")
                                                    .setFallbackUri("forward:/user-service-fallback")
                                            )
                                            .retry(retryConfig -> retryConfig
                                                    .setRetries(3)
                                                    .setMethods(HttpMethod.GET, HttpMethod.DELETE)
                                                    .setBackoff(
                                                            Duration.ofMillis(100),
                                                            Duration.ofMillis(1000),
                                                            2,
                                                            true
                                                    )
                                            )

                                )
                                .uri("lb://USER-SERVICE")
                )
                .route("inventory-service-route",
                        predicateSpec -> predicateSpec.path("/inventory-service/**")
                                .filters(filter -> filter
                                        .filter(authenticationFilter.apply(new AuthenticationFilter.Config()))
                                        .rewritePath("/inventory-service/?(?<remaining>.*)", "/${remaining}")
                                        .requestRateLimiter(rateLimiter ->
                                                rateLimiter
                                                        .setKeyResolver(userIdKeyResolver())
                                                        .setRateLimiter(redisRateLimiter())
                                        )
                                        .circuitBreaker(cb -> cb
                                                        .setName("inventory-service-circuit-breaker")
                                                        .setFallbackUri("forward:/inventory-service-fallback")
                                        )
                                        .retry(retryConfig -> retryConfig
                                                        .setRetries(3)
                                                        .setMethods(HttpMethod.GET, HttpMethod.DELETE)
                                                        .setBackoff(
                                                                Duration.ofMillis(100),
                                                                Duration.ofMillis(1000),
                                                                2,
                                                                true
                                                        )

                                                )

                                )
                                .uri("lb://INVENTORY-SERVICE")
                )


                .build();
    }

    // Resolve the user by user id ---> This is important to identify a user by a unique id
    @Bean
    public KeyResolver userIdKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest()
                    .getHeaders()
                    .getFirst("X-USER-ID");

            return Mono.justOrEmpty(userId);
        };
    }

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(
                10, // replenishRate  ---> 10 tokens are added every second to the bucket.
                15, // burstCapacity  ---> The bucket can hold a maximum of 10 tokens.
                5    // requested token  ---> Each request consumes 5 tokens.
        );
    }

}
