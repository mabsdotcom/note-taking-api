package com.notesapp.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/** Value must be null or a 6-digit hex colour such as #1E88E5. */
@Documented
@Constraint(validatedBy = HexColorValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface HexColor {

    String message() default "Color must be a hex value like #1E88E5";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
