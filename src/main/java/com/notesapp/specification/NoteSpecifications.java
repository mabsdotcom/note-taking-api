package com.notesapp.specification;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.notesapp.dto.note.NoteSearchCriteria;
import com.notesapp.entity.Note;
import com.notesapp.entity.Tag;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

/**
 * Builds the dynamic search query for notes. Every criterion is optional; only the
 * ones that are present are AND-ed together. Ownership and "not deleted" are always applied.
 */
public final class NoteSpecifications {

    private NoteSpecifications() {
    }

    public static Specification<Note> fromCriteria(Long ownerId, NoteSearchCriteria c) {
        List<Specification<Note>> specs = new ArrayList<>();
        specs.add(ownedBy(ownerId));
        specs.add(notDeleted());

        if (c != null) {
            if (hasText(c.q())) {
                specs.add(textContains(c.q()));
            }
            if (c.categoryId() != null) {
                specs.add(inCategory(c.categoryId()));
            }
            if (c.tags() != null && !c.tags().isEmpty()) {
                specs.add(hasAnyTag(c.tags()));
            }
            if (c.pinned() != null) {
                specs.add(pinned(c.pinned()));
            }
            // Archived notes are hidden unless explicitly requested
            specs.add(archived(c.archived() != null && c.archived()));
            if (c.createdFrom() != null || c.createdTo() != null) {
                specs.add(dateBetween("createdAt", c.createdFrom(), c.createdTo()));
            }
            if (c.updatedFrom() != null || c.updatedTo() != null) {
                specs.add(dateBetween("updatedAt", c.updatedFrom(), c.updatedTo()));
            }
        } else {
            specs.add(archived(false));
        }
        return Specification.allOf(specs);
    }

    public static Specification<Note> ownedBy(Long ownerId) {
        return (root, query, cb) -> cb.equal(root.get("owner").get("id"), ownerId);
    }

    public static Specification<Note> notDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("deleted"));
    }

    /** Case-insensitive match on the title or the plain-text body. */
    public static Specification<Note> textContains(String text) {
        String pattern = "%" + escapeLike(text.trim().toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern, '\\'),
                cb.like(cb.lower(root.get("plainText")), pattern, '\\'));
    }

    public static Specification<Note> inCategory(Long categoryId) {
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    /** Notes having at least one of the given tag names (case-insensitive). */
    public static Specification<Note> hasAnyTag(List<String> tagNames) {
        List<String> lowered = tagNames.stream()
                .filter(NoteSpecifications::hasText)
                .map(t -> t.trim().toLowerCase(Locale.ROOT))
                .toList();
        return (root, query, cb) -> {
            if (lowered.isEmpty()) {
                return cb.conjunction();
            }
            // The join can produce duplicates when a note matches several tags
            query.distinct(true);
            Join<Note, Tag> tags = root.join("tags");
            return cb.lower(tags.get("name")).in(lowered);
        };
    }

    public static Specification<Note> pinned(boolean pinned) {
        return (root, query, cb) -> cb.equal(root.get("pinned"), pinned);
    }

    public static Specification<Note> archived(boolean archived) {
        return (root, query, cb) -> cb.equal(root.get("archived"), archived);
    }

    /** Inclusive date range on an Instant attribute; dates are interpreted as UTC days. */
    public static Specification<Note> dateBetween(String attribute, LocalDate from, LocalDate to) {
        Instant start = from == null ? null : from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endExclusive = to == null ? null : to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return (root, query, cb) -> {
            if (start != null && endExclusive != null) {
                return cb.and(
                        cb.greaterThanOrEqualTo(root.<Instant>get(attribute), start),
                        cb.lessThan(root.<Instant>get(attribute), endExclusive));
            }
            if (start != null) {
                return cb.greaterThanOrEqualTo(root.<Instant>get(attribute), start);
            }
            return cb.lessThan(root.<Instant>get(attribute), endExclusive);
        };
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    /** Escapes LIKE wildcards so a search for "50%" matches literally. */
    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
