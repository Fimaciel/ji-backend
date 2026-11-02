package br.edu.ifro.ji.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = SenhaValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface Senha {

    String message() default "A senha deve ter no mínimo 8 caracteres e no máximo 72 bytes UTF-8.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
