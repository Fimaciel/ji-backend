package br.edu.ifro.ji.service;

import br.edu.ifro.ji.config.JwtProperties;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class JwtTokenService {

    private final JwtEncoder encoder;
    private final JwtProperties properties;
    private final Clock clock;

    public JwtTokenService(JwtEncoder encoder, JwtProperties properties, Clock clock) {
        this.encoder = encoder;
        this.properties = properties;
        this.clock = clock;
    }

    public TokenEmitido emitir(Long usuarioId) {
        Instant emitidoEm = Instant.now(clock);
        Instant expiraEm = emitidoEm.plus(properties.duration());
        UUID jti = UUID.randomUUID();

        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256)
                .type("JWT")
                .build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .audience(List.of(properties.audience()))
                .subject(usuarioId.toString())
                .id(jti.toString())
                .issuedAt(emitidoEm)
                .expiresAt(expiraEm)
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(cabecalho, claims))
                .getTokenValue();
        return new TokenEmitido(token, jti, emitidoEm, expiraEm);
    }

    public record TokenEmitido(String valor, UUID jti, Instant emitidoEm, Instant expiraEm) {
    }
}
