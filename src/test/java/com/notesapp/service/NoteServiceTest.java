package com.notesapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.notesapp.TestData;
import com.notesapp.dto.common.PageResponse;
import com.notesapp.dto.note.NoteRequest;
import com.notesapp.dto.note.NoteResponse;
import com.notesapp.dto.note.NoteSummaryResponse;
import com.notesapp.entity.Category;
import com.notesapp.entity.Note;
import com.notesapp.entity.NoteAction;
import com.notesapp.entity.Tag;
import com.notesapp.entity.User;
import com.notesapp.exception.BadRequestException;
import com.notesapp.exception.ResourceNotFoundException;
import com.notesapp.repository.CategoryRepository;
import com.notesapp.repository.NoteHistoryRepository;
import com.notesapp.repository.NoteRepository;
import com.notesapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @Mock private NoteRepository noteRepository;
    @Mock private UserRepository userRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private NoteHistoryRepository historyRepository;
    @Mock private TagService tagService;

    private NoteService noteService;
    private User owner;

    @BeforeEach
    void setUp() {
        noteService = new NoteService(noteRepository, userRepository, categoryRepository, historyRepository,
                tagService, new HtmlSanitizer(), new CsvExportService());
        owner = TestData.user(1L, "alice");
    }

    private static NoteRequest request(String title, String html, Long categoryId, List<String> tags,
                                       Boolean pinned, Boolean archived) {
        return new NoteRequest(title, html, categoryId, tags, pinned, archived);
    }

    @Test
    void create_sanitizesHtml_setsCategoryAndTags_andRecordsHistory() {
        Category work = TestData.category(5L, "Work", owner);
        Tag tag = TestData.tag(9L, "meeting", owner);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(categoryRepository.findByIdAndOwnerId(5L, 1L)).thenReturn(Optional.of(work));
        when(tagService.findOrCreate(eq(owner), anyList())).thenReturn(Set.of(tag));
        when(noteRepository.save(any(Note.class))).thenAnswer(inv -> inv.getArgument(0));

        NoteResponse response = noteService.create(1L, request("  Standup ",
                "<p>Hello <b>team</b></p><script>alert('x')</script>", 5L, List.of("meeting"), true, false));

        assertThat(response.title()).isEqualTo("Standup");
        assertThat(response.content()).contains("<b>team</b>").doesNotContain("script");
        assertThat(response.category().name()).isEqualTo("Work");
        assertThat(response.tags()).containsExactly("meeting");
        assertThat(response.pinned()).isTrue();

        ArgumentCaptor<Note> saved = ArgumentCaptor.forClass(Note.class);
        verify(noteRepository).save(saved.capture());
        assertThat(saved.getValue().getPlainText()).isEqualTo("Hello team");
        assertThat(saved.getValue().getOwner()).isSameAs(owner);
        assertThat(saved.getValue().getHistory()).extracting("action").containsExactly(NoteAction.CREATED);
    }

    @Test
    void create_withCategoryOfAnotherUser_throwsNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(categoryRepository.findByIdAndOwnerId(77L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.create(1L, request("T", "", 77L, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category");
        verify(noteRepository, never()).save(any());
    }

    @Test
    void create_pinnedAndArchived_isRejected() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> noteService.create(1L, request("T", "", null, null, true, true)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("archived");
    }

    @Test
    void getById_ofMissingOrForeignNote_throwsNotFound() {
        when(noteRepository.findByIdAndOwnerIdAndDeletedFalse(3L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.getById(1L, 3L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_replacesFieldsAndRecordsHistory() {
        Note note = TestData.note(3L, "Old", owner);
        when(noteRepository.findByIdAndOwnerIdAndDeletedFalse(3L, 1L)).thenReturn(Optional.of(note));
        when(tagService.findOrCreate(eq(owner), any())).thenReturn(Set.of());
        when(noteRepository.saveAndFlush(note)).thenReturn(note);

        NoteResponse response = noteService.update(1L, 3L, request("New", "<p>body</p>", null, null, false, true));

        assertThat(response.title()).isEqualTo("New");
        assertThat(response.archived()).isTrue();
        assertThat(response.category()).isNull();
        assertThat(note.getHistory()).extracting("action").containsExactly(NoteAction.UPDATED);
    }

    @Test
    void delete_isSoftDelete() {
        Note note = TestData.note(3L, "To trash", owner);
        when(noteRepository.findByIdAndOwnerIdAndDeletedFalse(3L, 1L)).thenReturn(Optional.of(note));

        noteService.delete(1L, 3L);

        assertThat(note.isDeleted()).isTrue();
        assertThat(note.getDeletedAt()).isNotNull();
        verify(noteRepository, never()).delete(any(Note.class));
    }

    @Test
    void restore_bringsNoteBackFromTrash() {
        Note note = TestData.note(3L, "Back", owner);
        note.softDelete();
        when(noteRepository.findByIdAndOwnerIdAndDeletedTrue(3L, 1L)).thenReturn(Optional.of(note));
        when(noteRepository.saveAndFlush(note)).thenReturn(note);

        noteService.restore(1L, 3L);

        assertThat(note.isDeleted()).isFalse();
        assertThat(note.getDeletedAt()).isNull();
        assertThat(note.getHistory()).extracting("action")
                .containsExactly(NoteAction.DELETED, NoteAction.RESTORED);
    }

    @Test
    void restore_ofNoteNotInTrash_throwsNotFound() {
        when(noteRepository.findByIdAndOwnerIdAndDeletedTrue(3L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.restore(1L, 3L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deletePermanently_removesNote() {
        Note note = TestData.note(3L, "Gone", owner);
        when(noteRepository.findByIdAndOwnerId(3L, 1L)).thenReturn(Optional.of(note));

        noteService.deletePermanently(1L, 3L);

        verify(noteRepository).delete(note);
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_returnsPagedSummaries() {
        Note note = TestData.note(3L, "Found", owner);
        Pageable pageable = PageRequest.of(0, 10, Sort.by("title"));
        when(noteRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(note), pageable, 1));

        PageResponse<NoteSummaryResponse> page = noteService.search(1L, null, pageable);

        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content()).extracting(NoteSummaryResponse::title).containsExactly("Found");
        assertThat(page.content().get(0).owner()).isEqualTo("alice");
    }

    @Test
    void search_withUnknownSortField_isRejected() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("password"));

        assertThatThrownBy(() -> noteService.search(1L, null, pageable))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("password");
    }

    @Test
    @SuppressWarnings("unchecked")
    void exportCsv_containsHeaderAndRows() {
        Note note = TestData.note(3L, "Report, Q1", owner);
        when(noteRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(note));

        String csv = noteService.exportCsv(1L, null);

        assertThat(csv).startsWith("id,title,");
        assertThat(csv).contains("\"Report, Q1\"");
    }

    @Test
    void history_ofForeignNote_throwsNotFound() {
        when(noteRepository.findByIdAndOwnerId(3L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.history(1L, 3L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void adminRestore_restoresAnyUsersNote() {
        Note note = TestData.note(3L, "Someone else's", TestData.user(2L, "bob"));
        note.softDelete();
        when(noteRepository.findByIdAndDeletedTrue(3L)).thenReturn(Optional.of(note));
        when(noteRepository.saveAndFlush(note)).thenReturn(note);

        noteService.adminRestore(3L);

        assertThat(note.isDeleted()).isFalse();
    }

    @Test
    void purgeDeletedBefore_deletesExpiredTrash() {
        Note old = TestData.note(3L, "Old", owner);
        Instant cutoff = Instant.now();
        when(noteRepository.findByDeletedTrueAndDeletedAtBefore(cutoff)).thenReturn(List.of(old));

        int purged = noteService.purgeDeletedBefore(cutoff);

        assertThat(purged).isEqualTo(1);
        verify(noteRepository).deleteAll(List.of(old));
    }
}
