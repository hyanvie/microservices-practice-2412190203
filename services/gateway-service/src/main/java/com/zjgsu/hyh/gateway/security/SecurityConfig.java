package com.zjgsu.hyh.gateway.security;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    SecretKey jwtSecretKey(@Value("${JWT_SECRET}") String secret) {
        byte[] key = secret.getBytes(StandardCharsets.UTF_8);
        if (key.length < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 UTF-8 bytes");
        }
        return new SecretKeySpec(key, "HmacSHA256");
    }

    @Bean
    ReactiveJwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        return NimbusReactiveJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, ReactiveJwtDecoder jwtDecoder) {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);

        return http.csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .authorizeExchange(auth -> auth
                        .pathMatchers("/actuator/health", "/api/users/login", "/api/users/*/exists").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/items/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/items/*/reserve").hasRole("BUYER")
                        .pathMatchers("/api/users/admin/**").hasRole("ADMIN")
                        .pathMatchers("/api/users/seller/**").hasRole("SELLER")
                        .pathMatchers("/api/users/buyer/**").hasRole("BUYER")
                        .pathMatchers(HttpMethod.POST, "/api/orders").hasRole("BUYER")
                        .anyExchange().authenticated())
                .oauth2ResourceServer(resource -> resource
                        .authenticationEntryPoint((exchange, ex) ->
                                writeError(exchange, HttpStatus.UNAUTHORIZED, "未登录或 Token 无效"))
                        .accessDeniedHandler((exchange, ex) ->
                                writeError(exchange, HttpStatus.FORBIDDEN, "当前角色无权访问该接口"))
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(
                                new ReactiveJwtAuthenticationConverterAdapter(converter))))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((exchange, ex) ->
                                writeError(exchange, HttpStatus.UNAUTHORIZED, "未登录或 Token 无效"))
                        .accessDeniedHandler((exchange, ex) ->
                                writeError(exchange, HttpStatus.FORBIDDEN, "当前角色无权访问该接口")))
                .build();
    }

    private Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"status\":" + status.value()
                + ",\"error\":\"" + status.getReasonPhrase()
                + "\",\"message\":\"" + message
                + "\",\"path\":\"" + exchange.getRequest().getURI().getRawPath() + "\"}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
