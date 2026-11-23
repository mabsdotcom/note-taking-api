package com.notesapp.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;

import com.notesapp.dto.auth.RegisterRequest;
import com.notesapp.dto.category.CategoryRequest;
import com.notesapp.dto.note.NoteSearchCriteria;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ValidatorsTest {

    private static Validator validator;

    @BeforeAll
    static void init() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @ParameterizedTest
    @ValueSource(strings = {"short1!", "alllowercase1!", "ALLUPPER1!", "NoDigits!!", "NoSpecial123", "Has space1A"})
    void weakPasswords_areRejected(String password) {
        Set<ConstraintViolation<RegisterRequest>> violations =
                validator.validate(new RegisterRequest("jane", "jane@example.com", password));
        assertThat(violations).extracting(v -> v.getPropertyPath().toString()).contains("password");
    }

    @Test
    void strongPassword_isAccepted() {
        assertThat(validator.validate(new RegisterRequest("jane", "jane@example.com", "Secret@123"))).isEmpty();
    }

    @Test
    void hexColor_validation() {
        assertThat(validator.validate(new CategoryRequest("Work", null, "#A1B2C3"))).isEmpty();
        assertThat(validator.validate(new CategoryRequest("Work", null, null))).isEmpty();
        assertThat(validator.validate(new CategoryRequest("Work", null, "blue")))
                .extracting(v -> v.getPropertyPath().toString()).containsExactly("color");
    }

    @Test
    void dateRange_crossFieldValidation() {
        NoteSearchCriteria ok = new NoteSearchCriteria(null, null, null, null, null,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), null, null);
        NoteSearchCriteria reversed = new NoteSearchCriteria(null, null, null, null, null,
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1), null, null);

        assertThat(validator.validate(ok)).isEmpty();
        assertThat(validator.validate(reversed))
                .extracting(v -> v.getPropertyPath().toString()).containsExactly("createdTo");
    }
}
