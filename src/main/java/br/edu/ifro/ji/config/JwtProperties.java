package br.edu.ifro.ji.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("ji.security.jwt")
public record JwtProperties(
        String secretBase64,
        String issuer,
        String audience,
        Duration duration
) {
}
