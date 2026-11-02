package br.edu.ifro.ji.config;

import br.edu.ifro.ji.service.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtConfigTest {

    private static final Instant AGORA = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    private final JwtConfig config = new JwtConfig();

    @Test
    void emiteEValidaJwtHs256ComClaimsDefinidos() {
        JwtProperties properties = properties("chave-com-32-bytes-para-testes!!");
        var chave = config.jwtSecretKey(properties);
        var encoder = config.jwtEncoder(chave);
        JwtDecoder decoder = config.jwtDecoder(chave, properties);
        JwtTokenService service = new JwtTokenService(
                encoder, properties, Clock.fixed(AGORA, ZoneOffset.UTC));

        var emitido = service.emitir(42L);
        var jwt = decoder.decode(emitido.valor());

        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getId()).isEqualTo(emitido.jti().toString());
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("ji-back");
        assertThat(jwt.getAudience()).containsExactly("ji-front");
        assertThat(jwt.getIssuedAt()).isEqualTo(AGORA);
        assertThat(jwt.getExpiresAt()).isEqualTo(AGORA.plus(Duration.ofHours(8)));
        assertThat(jwt.getClaims()).doesNotContainKeys("email", "cpf", "senha");
    }

    @Test
    void rejeitaJwtExpiradoOuComAssinaturaAlterada() {
        JwtProperties properties = properties("chave-com-32-bytes-para-testes!!");
        var chave = config.jwtSecretKey(properties);
        var encoder = config.jwtEncoder(chave);
        JwtDecoder decoder = config.jwtDecoder(chave, properties);
        JwtClaimsSet expirado = JwtClaimsSet.builder()
                .issuer("ji-back")
                .audience(java.util.List.of("ji-front"))
                .subject("42")
                .id(java.util.UUID.randomUUID().toString())
                .issuedAt(AGORA.minus(Duration.ofHours(2)))
                .expiresAt(AGORA.minus(Duration.ofHours(1)))
                .build();
        String tokenExpirado = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), expirado)).getTokenValue();

        assertThatThrownBy(() -> decoder.decode(tokenExpirado))
                .isInstanceOf(JwtException.class);

        var valido = new JwtTokenService(
                encoder, properties, Clock.fixed(AGORA, ZoneOffset.UTC)).emitir(42L).valor();
        int inicioAssinatura = valido.lastIndexOf('.') + 1;
        char substituto = valido.charAt(inicioAssinatura) == 'A' ? 'B' : 'A';
        String adulterado = valido.substring(0, inicioAssinatura) + substituto
                + valido.substring(inicioAssinatura + 1);
        assertThatThrownBy(() -> decoder.decode(adulterado))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejeitaSegredoAusenteInvalidoOuMenorQue256Bits() {
        assertThatThrownBy(() -> config.jwtSecretKey(
                new JwtProperties(null, "ji-back", "ji-front", Duration.ofHours(8))))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> config.jwtSecretKey(
                new JwtProperties("%%%", "ji-back", "ji-front", Duration.ofHours(8))))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> config.jwtSecretKey(properties("curta")))
                .isInstanceOf(IllegalStateException.class);
    }

    private JwtProperties properties(String segredo) {
        return new JwtProperties(
                Base64.getEncoder().encodeToString(segredo.getBytes(StandardCharsets.UTF_8)),
                "ji-back",
                "ji-front",
                Duration.ofHours(8)
        );
    }
}
