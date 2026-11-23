package com.notesapp.validation;

import java.util.regex.Pattern;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class HexColorValidator implements ConstraintValidator<HexColor, String> {

    private static final Pattern HEX = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || HEX.matcher(value).matches();
    }
}
