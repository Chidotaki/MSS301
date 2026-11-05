package com.mss301.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

// [TODO-7.3, TODO-7.4, TODO-7.5]
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final String USER_ID_HEADER =
            "X-User-Id";

    private static final String USER_ROLE_HEADER =
            "X-User-Role";

    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * Public với mọi HTTP method.
     */
    private static final List<String> PUBLIC_PREFIXES =
            List.of(
                    "/api/auth/login",
                    "/api/auth/refresh",
                    "/fallback"
            );

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain
    ) {

        ServerHttpRequest request =
                exchange.getRequest();

        String path =
                request.getURI()
                        .getPath();

        // =====================================================
        // 1. XÓA HEADER GIẢ MẠO DO CLIENT GỬI
        // =====================================================

        ServerHttpRequest sanitized =
                request.mutate()
                        .headers(headers -> {
                            headers.remove(USER_ID_HEADER);
                            headers.remove(USER_ROLE_HEADER);
                        })
                        .build();

        // =====================================================
        // 2. PUBLIC ENDPOINTS
        // =====================================================

        boolean isPublic =
                PUBLIC_PREFIXES
                        .stream()
                        .anyMatch(path::startsWith)

                        ||

                        (
                                HttpMethod.GET.equals(
                                        request.getMethod()
                                )
                                        &&
                                path.equals(
                                        "/api/watches"
                                )
                        );

        if (isPublic) {

            return chain.filter(
                    exchange.mutate()
                            .request(sanitized)
                            .build()
            );
        }

        // =====================================================
        // 3. KIỂM TRA BEARER TOKEN
        // =====================================================

        String authHeader =
                request.getHeaders()
                        .getFirst(
                                HttpHeaders.AUTHORIZATION
                        );

        if (
                authHeader == null
                        ||
                !authHeader.startsWith("Bearer ")
        ) {

            exchange
                    .getResponse()
                    .setStatusCode(
                            HttpStatus.UNAUTHORIZED
                    );

            return exchange
                    .getResponse()
                    .setComplete();
        }

        // =====================================================
        // 4. VERIFY JWT
        // =====================================================

        try {

            Claims claims =
                    Jwts.parser()
                            .verifyWith(
                                    Keys.hmacShaKeyFor(
                                            jwtSecret
                                                    .getBytes(
                                                            StandardCharsets.UTF_8
                                                    )
                                    )
                            )
                            .build()
                            .parseSignedClaims(
                                    authHeader.substring(7)
                            )
                            .getPayload();

            // Refresh token KHÔNG được dùng gọi API
            if (
                    !"access".equals(
                            claims.get(
                                    "type",
                                    String.class
                            )
                    )
            ) {

                exchange
                        .getResponse()
                        .setStatusCode(
                                HttpStatus.UNAUTHORIZED
                        );

                return exchange
                        .getResponse()
                        .setComplete();
            }

            // =================================================
            // 5. GATEWAY TỰ GẮN USER INFO
            // =================================================

            ServerHttpRequest authenticated =
                    sanitized.mutate()
                            .header(
                                    USER_ID_HEADER,
                                    claims.getSubject()
                            )
                            .header(
                                    USER_ROLE_HEADER,
                                    String.valueOf(
                                            claims.get(
                                                    "role",
                                                    Integer.class
                                            )
                                    )
                            )
                            .build();

            return chain.filter(
                    exchange.mutate()
                            .request(authenticated)
                            .build()
            );

        } catch (Exception ex) {

            exchange
                    .getResponse()
                    .setStatusCode(
                            HttpStatus.UNAUTHORIZED
                    );

            return exchange
                    .getResponse()
                    .setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }
}