package com.notesapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.notesapp.TestData;
import com.notesapp.dto.category.CategoryRequest;
import com.notesapp.dto.category.CategoryResponse;
import com.notesapp.entity.Category;
import com.notesapp.entity.User;
import com.notesapp.exception.DuplicateResourceException;
import com.notesapp.exception.ResourceNotFoundException;
import com.notesapp.repository.CategoryRepository;
import com.notesapp.repository.NoteRepository;
import com.notesapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private NoteRepository noteRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private CategoryService categoryService;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = TestData.user(1L, "alice");
    }

    @Test
    void create_savesTrimmedNameAndUppercaseColor() {
        when(categoryRepository.existsByOwnerIdAndNameIgnoreCase(1L, "Work")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoryResponse response = categoryService.create(1L, new CategoryRequest(" Work ", "  ", "#1e88e5"));

        assertThat(response.name()).isEqualTo("Work");
        assertThat(response.description()).isNull();
        assertThat(response.color()).isEqualTo("#1E88E5");
        assertThat(response.noteCount()).isZero();
    }

    @Test
    void create_duplicateName_throwsConflict() {
        when(categoryRepository.existsByOwnerIdAndNameIgnoreCase(1L, "Work")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(1L, new CategoryRequest("Work", null, null)))
                .isInstanceOf(DuplicateResourceException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void update_toNameUsedByAnotherCategory_throwsConflict() {
        Category category = TestData.category(5L, "Work", owner);
        when(categoryRepository.findByIdAndOwnerId(5L, 1L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByOwnerIdAndNameIgnoreCaseAndIdNot(1L, "Home", 5L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.update(1L, 5L, new CategoryRequest("Home", null, null)))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void update_changesFields() {
        Category category = TestData.category(5L, "Work", owner);
        when(categoryRepository.findByIdAndOwnerId(5L, 1L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByOwnerIdAndNameIgnoreCaseAndIdNot(1L, "Office", 5L)).thenReturn(false);
        when(categoryRepository.saveAndFlush(category)).thenReturn(category);
        when(noteRepository.countByCategoryIdAndDeletedFalse(5L)).thenReturn(4L);

        CategoryResponse response = categoryService.update(1L, 5L, new CategoryRequest("Office", "Desk", null));

        assertThat(response.name()).isEqualTo("Office");
        assertThat(response.description()).isEqualTo("Desk");
        assertThat(response.noteCount()).isEqualTo(4);
    }

    @Test
    void delete_detachesNotesThenDeletesCategory() {
        Category category = TestData.category(5L, "Work", owner);
        when(categoryRepository.findByIdAndOwnerId(5L, 1L)).thenReturn(Optional.of(category));

        categoryService.delete(1L, 5L);

        verify(noteRepository).clearCategory(5L);
        verify(categoryRepository).delete(category);
    }

    @Test
    void get_foreignCategory_throwsNotFound() {
        when(categoryRepository.findByIdAndOwnerId(5L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.get(1L, 5L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void list_includesNoteCounts() {
        Category a = TestData.category(5L, "A", owner);
        when(categoryRepository.findByOwnerIdOrderByNameAsc(1L)).thenReturn(List.of(a));
        when(noteRepository.countByCategoryIdAndDeletedFalse(5L)).thenReturn(2L);

        assertThat(categoryService.list(1L)).singleElement()
                .satisfies(c -> assertThat(c.noteCount()).isEqualTo(2));
    }
}
