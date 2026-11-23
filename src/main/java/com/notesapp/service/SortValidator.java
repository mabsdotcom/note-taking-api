package com.notesapp.service;

import java.util.Set;

import com.notesapp.exception.BadRequestException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Only allow-listed fields can be used in ?sort=..., so clients cannot sort on internal columns. */
final class SortValidator {

    private SortValidator() {
    }

    static void validate(Pageable pageable, Set<String> allowed) {
        for (Sort.Order order : pageable.getSort()) {
            if (!allowed.contains(order.getProperty())) {
                throw new BadRequestException("Cannot sort by '" + order.getProperty()
                        + "'. Allowed: " + String.join(", ", allowed));
            }
        }
    }
}
