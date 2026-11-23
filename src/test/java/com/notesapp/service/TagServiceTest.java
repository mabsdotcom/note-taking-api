package com.notesapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.notesapp.TestData;
import com.notesapp.dto.tag.TagRequest;
import com.notesapp.entity.Note;
import com.notesapp.entity.Tag;
import com.notesapp.entity.User;
import com.notesapp.exception.DuplicateResourceException;
import com.notesapp.exception.ResourceNotFoundException;
import com.notesapp.repository.TagRepository;
import com.notesapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock private TagRepository tagRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private TagService tagService;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = TestData.user(1L, "alice");
    }

    @Test
    void findOrCreate_reusesExisting_createsMissing_andMergesDuplicates() {
        Tag existing = TestData.tag(10L, "work", owner);
        when(tagRepository.findByOwnerIdAndNameIgnoreCase(1L, "Work")).thenReturn(Optional.of(existing));
        when(tagRepository.findByOwnerIdAndNameIgnoreCase(1L, "new")).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenAnswer(inv -> inv.getArgument(0));

        Set<Tag> tags = tagService.findOrCreate(owner, Arrays.asList("Work", "work ", "new", " ", null));

        assertThat(tags).extracting(Tag::getName).containsExactlyInAnyOrder("work", "new");
        verify(tagRepository, times(1)).save(any(Tag.class));
    }

    @Test
    void findOrCreate_withNoNames_returnsEmptySet() {
        assertThat(tagService.findOrCreate(owner, null)).isEmpty();
        assertThat(tagService.findOrCreate(owner, List.of())).isEmpty();
    }

    @Test
    void get_returnsOwnedTag() {
        when(tagRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(TestData.tag(10L, "work", owner)));

        assertThat(tagService.get(1L, 10L).name()).isEqualTo("work");
    }

    @Test
    void get_tagOfAnotherUser_throwsNotFound() {
        when(tagRepository.findByIdAndOwnerId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tagService.get(1L, 99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_duplicate_throwsConflict() {
        when(tagRepository.findByOwnerIdAndNameIgnoreCase(1L, "work"))
                .thenReturn(Optional.of(TestData.tag(10L, "Work", owner)));

        assertThatThrownBy(() -> tagService.create(1L, new TagRequest("work")))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void rename_changesName() {
        Tag tag = TestData.tag(10L, "wrk", owner);
        when(tagRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(tag));
        when(tagRepository.existsByOwnerIdAndNameIgnoreCaseAndIdNot(1L, "work", 10L)).thenReturn(false);

        assertThat(tagService.rename(1L, 10L, new TagRequest(" work ")).name()).isEqualTo("work");
    }

    @Test
    void delete_removesTagFromNotes() {
        Tag tag = TestData.tag(10L, "work", owner);
        Note note = TestData.note(3L, "N", owner);
        note.getTags().add(tag);
        tag.getNotes().add(note);
        when(tagRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(tag));

        tagService.delete(1L, 10L);

        assertThat(note.getTags()).isEmpty();
        verify(tagRepository).delete(tag);
    }
}
