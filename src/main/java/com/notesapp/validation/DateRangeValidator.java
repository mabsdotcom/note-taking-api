package com.notesapp.validation;

import java.lang.reflect.Method;
import java.time.LocalDate;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DateRangeValidator implements ConstraintValidator<ValidDateRange, Object> {

    private String fromField;
    private String toField;

    @Override
    public void initialize(ValidDateRange annotation) {
        this.fromField = annotation.from();
        this.toField = annotation.to();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        LocalDate from = read(value, fromField);
        LocalDate to = read(value, toField);
        if (from == null || to == null || !from.isAfter(to)) {
            return true;
        }
        // Report the error on the "to" field so the client sees which parameter is wrong
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode(toField)
                .addConstraintViolation();
        return false;
    }

    /** Reads a record component (name()) or a JavaBean property (getName()). */
    private static LocalDate read(Object target, String name) {
        try {
            Method accessor;
            try {
                accessor = target.getClass().getMethod(name);
            } catch (NoSuchMethodException e) {
                accessor = target.getClass().getMethod("get" + Character.toUpperCase(name.charAt(0)) + name.substring(1));
            }
            return (LocalDate) accessor.invoke(target);
        } catch (ReflectiveOperationException | ClassCastException e) {
            throw new IllegalStateException("Cannot read date field '" + name + "' on " + target.getClass(), e);
        }
    }
}
