package com.easybuy.api_gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.GatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${jwt.secret-key:eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9/eyJzdWIiOiI4ZjQxYzY0Yi0yYjY0LTQ2NWEtYjM4Ny0zYjQ5YjQ5MjA1YzAiLCJuYW1lIjoiVGVzdCBVc2VyIiwiaWF0IjoxNzgyMTQ1NjAwfQ=v4sV1v6x4z9H7N4uK8cQmJ5Wf2YtR1nP3sE6dL0aB9Q}")
    private String secretKey;

    public AuthenticationFilter() {
        super(Config.class);
    }
    //string key --> Secret key re presentation
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }


    @Override
    public GatewayFilter apply(Config config) {
        logger.info("Started apply method inside AuthenticationFilter");
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();
            String method = request.getMethod().name();

            logger.info("Authentication Filter Request Path: {}", path);
            logger.info("Authentication Filter Method: {}", method);

            // Check 1 : If public endpoint
            if(isPublicEndpoint(path, method)){
                String clientIp = request.getRemoteAddress() != null
                        ? request.getRemoteAddress().getAddress().getHostAddress()
                        : "anonymous";
                logger.info("This was a public endpoint, added user-id as : {}", clientIp);

                ServerHttpRequest mutatedRequest = prepareHeader(request, clientIp, null, null);
                return chain.filter(exchange.mutate().request(mutatedRequest).build());
            }

            String authorizationHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if(authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")){
                return onError(exchange, "Missing or Invalid Authentication Header", HttpStatus.UNAUTHORIZED);
            }

            String token = authorizationHeader.substring(7);

            // Check 2 : Now you have to validate the jwt token
            try{

                // Get the claims form token
                Claims claims = Jwts.parser()
                        .verifyWith(getSigningKey())
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                String tokenUserId = claims.get("userId", String.class);
                String tokenRole = claims.get("role", String.class);
                Date expiration = claims.getExpiration();
                String type = claims.get("token-type", String.class);

                // Check if this is RefreshToken
                if(!type.equalsIgnoreCase("access-token")) return onError(exchange, "Refresh token in not acceptable.", HttpStatus.UNAUTHORIZED);

                // Check if token is already expired
                if(expiration.before(new Date())) return onError(exchange, "JWT Token Expired", HttpStatus.UNAUTHORIZED);
                
                // Check if user has a valid Role
                if(!isValidRole(tokenRole)) return onError(exchange, "Invalid role " + tokenRole, HttpStatus.UNAUTHORIZED);

                // Check if user trying to access admin only paths without admin role
                if(isAdminOnlyEndpoint(path, method) && !tokenRole.equalsIgnoreCase("ADMIN")) return onError(exchange, "Invalid role, this is only for admin", HttpStatus.UNAUTHORIZED);

                // Now check if user is not trying to access details of other user
                if(isUserOrGuest(tokenRole)){
                    String targetUserId = extractUserIdFromPath(path);

                    if(targetUserId != null && !targetUserId.equalsIgnoreCase(tokenUserId))
                        return onError(exchange, "Forbidden : You cannot access another user's data.", HttpStatus.UNAUTHORIZED);
                }

                // STEP 7: Propagate verified user details as headers to downstream microservices
                ServerHttpRequest mutatedRequest = prepareHeader(request, tokenUserId, claims, tokenRole);

                return chain.filter(exchange.mutate().request(mutatedRequest).build());

            }catch (Exception e){
                logger.error("Authentication Filter Exception", e);
                return onError(exchange, "Unauthorized: Invalid or expired token", HttpStatus.UNAUTHORIZED);
            }
        };
    }

    private static ServerHttpRequest prepareHeader(ServerHttpRequest request, String tokenUserId, Claims claims, String tokenRole) {
        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-User-Id", tokenUserId)
                .header("X-User-Email", claims == null ? null : claims.getSubject())
                .header("X-User-Role", tokenRole)
                .build();
        return mutatedRequest;
    }

    /**
     * Helper to verify if an endpoint is public (can be accessed without a token).
     */
    //api/users/login--POST
    private boolean isPublicEndpoint(String path, String method) {
        return path.contains("/public/") ||
                path.contains("/api/users/login") ||
                path.contains("/api/users/refresh") ||
                (path.contains("/api/users") && "POST".equalsIgnoreCase(method)) || // User registration
                (path.contains("/product") && "GET".equalsIgnoreCase(method)) || // View products
                (path.contains("/category") && "GET".equalsIgnoreCase(method)) || // View categories
                (path.contains("/review") && "GET".equalsIgnoreCase(method)); // View reviews
        //public mentions
    }

    /**
     * Helper to check if a role is a valid role supported by easybuy.
     */
    private boolean isValidRole(String role) {
        return "ADMIN".equalsIgnoreCase(role) ||
                "USER".equalsIgnoreCase(role) ||
                "GUEST".equalsIgnoreCase(role);
    }

    /**
     * Helper to check if a user belongs to non-admin customer roles.
     */
    private boolean isUserOrGuest(String role) {
        return "USER".equalsIgnoreCase(role) || "GUEST".equalsIgnoreCase(role);
    }

    /**
     * Helper to check if the route requires Admin permissions.
     */
    private boolean isAdminOnlyEndpoint(String path, String method) {
        // 1. Updating role mapping
        if (path.contains("/api/users/change-role")) return true;

        // 2. Querying list of all users (exclude single profile path `/api/users/123-uuid`)
        if (path.contains("/api/users") && "GET".equalsIgnoreCase(method) && !path.matches(".*/api/users/[a-fA-F0-9-]+"))
            return true;

        // 3. Modifying catalog (POST/PUT/DELETE products, categories, reviews)
        if ((path.contains("/product") || path.contains("/category") || path.contains("/review")) && !"GET".equalsIgnoreCase(method))
            return true;

        // 4. Modifying inventory details
        if (path.contains("/api/inventories") && (method.equalsIgnoreCase("POST") || method.equalsIgnoreCase("PUT") || method.equalsIgnoreCase("DELETE") || method.equalsIgnoreCase("PATCH")))
            return true;

        return false;
    }

    /**
     * Safely extracts the target userId from paths of different microservices.
     * Supports:
     * - Carts: /api/carts/{userId}/**
     * - Orders: /api/orders/user/{userId}/** and /api/orders/{userId}/checkout
     * - Users: /api/users/{userId}
     */
    private String extractUserIdFromPath(String path) {
        String[] prefixes = {"/cart/", "/order/user/", "/order/", "/api/users/"};

        for (String prefix : prefixes) {
            int index = path.indexOf(prefix);
            if (index != -1) {
                String sub = path.substring(index + prefix.length());

                // If it ends with '/checkout' (e.g. /api/orders/{userId}/checkout) remove it
                if (sub.endsWith("/checkout")) {
                    sub = sub.replace("/checkout", "");
                }

                // Extract segment before next slash if nested (e.g. /api/carts/{userId}/items)
                int slashIndex = sub.indexOf("/");
                String extractedId = (slashIndex != -1) ? sub.substring(0, slashIndex) : sub;

                // Avoid returning static endpoints as userIds
                if (extractedId.equals("login") || extractedId.equals("refresh") || extractedId.equals("change-role")) {
                    continue;
                }
                return extractedId;
            }
        }
        return null;
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        logger.info("Error Occured : {}", err);
        ServerHttpResponse response = exchange.getResponse();
        response.writeAndFlushWith(body-> Mono.just("Internal Server Error: " + err));
        response.setStatusCode(httpStatus);
        return response.setComplete();
    }


    public static class Config{

    }
}
