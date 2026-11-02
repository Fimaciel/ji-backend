package br.edu.ifro.ji.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SenhaValidatorTest {

    private final SenhaValidator validator = new SenhaValidator();

    @Test
    void aceitaSenhaEntreOitoCaracteresESetentaEDoisBytes() {
        assertThat(validator.isValid("12345678", null)).isTrue();
        assertThat(validator.isValid("ábcdef12", null)).isTrue();
        assertThat(validator.isValid("a".repeat(72), null)).isTrue();
    }

    @Test
    void rejeitaSenhaCurtaOuQueUltrapassaLimiteDoBcryptEmUtf8() {
        assertThat(validator.isValid("1234567", null)).isFalse();
        assertThat(validator.isValid("🔐".repeat(19), null)).isFalse();
        assertThat(validator.isValid("a".repeat(73), null)).isFalse();
    }

    @Test
    void deixaNulosEBrancosParaNotBlank() {
        assertThat(validator.isValid(null, null)).isTrue();
        assertThat(validator.isValid("   ", null)).isTrue();
    }
}
