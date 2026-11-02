package br.edu.ifro.ji.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenSeguroServiceTest {

    private final TokenSeguroService service = new TokenSeguroService();

    @Test
    void geraTokensAleatoriosEArmazenaHashSha256() {
        String primeiro = service.gerar();
        String segundo = service.gerar();

        assertThat(primeiro).isNotBlank().isNotEqualTo(segundo);
        assertThat(service.hash(primeiro)).hasSize(64)
                .matches("[0-9a-f]{64}")
                .isEqualTo(service.hash(primeiro));
        assertThat(service.hash(segundo)).isNotEqualTo(service.hash(primeiro));
    }
}
