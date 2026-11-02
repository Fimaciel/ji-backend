package br.edu.ifro.ji.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;

public class SenhaValidator implements ConstraintValidator<Senha, String> {

    @Override
    public boolean isValid(String senha, ConstraintValidatorContext context) {
        if (senha == null || senha.isBlank()) {
            return true;
        }
        int caracteres = senha.codePointCount(0, senha.length());
        int bytes = senha.getBytes(StandardCharsets.UTF_8).length;
        return caracteres >= 8 && bytes <= 72;
    }
}
