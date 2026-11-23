package com.notesapp.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.notesapp.config.JpaAuditingConfig;
import com.notesapp.dto.note.NoteSearchCriteria;
import com.notesapp.entity.Category;
import com.notesapp.entity.Note;
import com.notesapp.entity.Role;
import com.notesapp.entity.Tag;
import com.notesapp.entity.User;
import com.notesapp.specification.NoteSpecifications;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class NoteRepositoryTest {

    @Autowired private NoteRepository noteRepository;
    @Autowired private TestEntityManager em;

    private User alice;
    private User bob;
    private Category work;
    private Tag urgent;

    @BeforeEach
    void setUp() {
        alice = em.persist(new User("alice", "alice@test.local", "x", Role.USER));
        bob = em.persist(new User("bob", "bob@test.local", "x", Role.USER));
        work = em.persist(new Category("Work", null, null, alice));
        urgent = em.persist(new Tag("urgent", alice));

        persistNote(alice, "Quarterly report", "numbers and charts", work, urgent, false, false);
        persistNote(alice, "Shopping list", "bread, report paper", null, null, false, false);
        persistNote(alice, "Old idea", "archived thought", null, null, true, false);
        persistNote(alice, "Trashed report", "gone", null, null, false, true);
        persistNote(bob, "Bob's report", "not alice's", null, null, false, false);
        em.flush();
    }

    private void persistNote(User owner, String title, String text, Category category, Tag tag,
                             boolean archived, boolean deleted) {
        Note n = new Note();
        n.setOwner(owner);
        n.setTitle(title);
        n.setContent("<p>" + text + "</p>");
        n.setPlainText(text);
        n.setCategory(category);
        n.setArchived(archived);
        if (tag != null) {
            n.getTags().add(tag);
        }
        if (deleted) {
            n.softDelete();
        }
        em.persist(n);
    }

    private List<String> search(User owner, NoteSearchCriteria criteria) {
        Page<Note> page = noteRepository.findAll(NoteSpecifications.fromCriteria(owner.getId(), criteria),
                PageRequest.of(0, 20));
        return page.getContent().stream().map(Note::getTitle).toList();
    }

    private static NoteSearchCriteria criteria(String q, Long categoryId, List<String> tags, Boolean archived) {
        return new NoteSearchCriteria(q, categoryId, tags, null, archived, null, null, null, null);
    }

    @Test
    void textSearch_matchesTitleOrBody_onlyOwnActiveNotes() {
        assertThat(search(alice, criteria("REPORT", null, null, null)))
                .containsExactlyInAnyOrder("Quarterly report", "Shopping list");
    }

    @Test
    void filterByCategoryAndTag() {
        assertThat(search(alice, criteria(null, work.getId(), null, null))).containsExactly("Quarterly report");
        assertThat(search(alice, criteria(null, null, List.of("URGENT", "missing"), null)))
                .containsExactly("Quarterly report");
    }

    @Test
    void archivedNotes_onlyWhenRequested() {
        assertThat(search(alice, criteria(null, null, null, null))).doesNotContain("Old idea");
        assertThat(search(alice, criteria(null, null, null, true))).containsExactly("Old idea");
    }

    @Test
    void softDeletedNotes_areExcludedFromSearch_butListedInTrash() {
        assertThat(search(alice, null)).doesNotContain("Trashed report");
        assertThat(noteRepository.findByOwnerIdAndDeletedTrue(alice.getId(), PageRequest.of(0, 10)))
                .extracting(Note::getTitle).containsExactly("Trashed report");
    }

    @Test
    void auditingFillsTimestampsAndAuthor() {
        Note any = noteRepository.findAll().get(0);
        assertThat(any.getCreatedAt()).isNotNull();
        assertThat(any.getCreatedBy()).isEqualTo(JpaAuditingConfig.SYSTEM_AUDITOR);
    }
}
