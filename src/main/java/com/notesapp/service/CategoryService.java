package com.notesapp.service;

import java.util.List;

import com.notesapp.dto.category.CategoryRequest;
import com.notesapp.dto.category.CategoryResponse;
import com.notesapp.entity.Category;
import com.notesapp.entity.User;
import com.notesapp.exception.DuplicateResourceException;
import com.notesapp.exception.ResourceNotFoundException;
import com.notesapp.mapper.CategoryMapper;
import com.notesapp.repository.CategoryRepository;
import com.notesapp.repository.NoteRepository;
import com.notesapp.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Categories are private to their owner; names are unique per user (case-insensitive). */
@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final NoteRepository noteRepository;
    private final UserRepository userRepository;

    public CategoryService(CategoryRepository categoryRepository, NoteRepository noteRepository,
                           UserRepository userRepository) {
        this.categoryRepository = categoryRepository;
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> list(Long userId) {
        return categoryRepository.findByOwnerIdOrderByNameAsc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse get(Long userId, Long categoryId) {
        return toResponse(findOwned(userId, categoryId));
    }

    public CategoryResponse create(Long userId, CategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.existsByOwnerIdAndNameIgnoreCase(userId, name)) {
            throw new DuplicateResourceException("Category '" + name + "' already exists");
        }
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Category category = new Category(name, trimToNull(request.description()), normalizeColor(request.color()), owner);
        return CategoryMapper.toResponse(categoryRepository.save(category), 0);
    }

    public CategoryResponse update(Long userId, Long categoryId, CategoryRequest request) {
        Category category = findOwned(userId, categoryId);
        String name = request.name().trim();
        if (categoryRepository.existsByOwnerIdAndNameIgnoreCaseAndIdNot(userId, name, categoryId)) {
            throw new DuplicateResourceException("Category '" + name + "' already exists");
        }
        category.setName(name);
        category.setDescription(trimToNull(request.description()));
        category.setColor(normalizeColor(request.color()));
        return toResponse(categoryRepository.saveAndFlush(category));
    }

    /** Notes in the category are kept; they simply become uncategorised. */
    public void delete(Long userId, Long categoryId) {
        Category category = findOwned(userId, categoryId);
        noteRepository.clearCategory(category.getId());
        categoryRepository.delete(category);
    }

    private Category findOwned(Long userId, Long categoryId) {
        return categoryRepository.findByIdAndOwnerId(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
    }

    private CategoryResponse toResponse(Category c) {
        return CategoryMapper.toResponse(c, noteRepository.countByCategoryIdAndDeletedFalse(c.getId()));
    }

    private static String trimToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String normalizeColor(String color) {
        return color == null ? null : color.toUpperCase();
    }
}
