package com.notesapp.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.notesapp.dto.tag.TagRequest;
import com.notesapp.dto.tag.TagResponse;
import com.notesapp.entity.Note;
import com.notesapp.entity.Tag;
import com.notesapp.entity.User;
import com.notesapp.exception.DuplicateResourceException;
import com.notesapp.exception.ResourceNotFoundException;
import com.notesapp.mapper.TagMapper;
import com.notesapp.repository.TagRepository;
import com.notesapp.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Tags are private to their owner; names are unique per user (case-insensitive). */
@Service
@Transactional
public class TagService {

    private final TagRepository tagRepository;
    private final UserRepository userRepository;

    public TagService(TagRepository tagRepository, UserRepository userRepository) {
        this.tagRepository = tagRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<TagResponse> list(Long userId) {
        return tagRepository.findByOwnerIdOrderByNameAsc(userId).stream().map(TagMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TagResponse get(Long userId, Long tagId) {
        return TagMapper.toResponse(findOwned(userId, tagId));
    }

    public TagResponse create(Long userId, TagRequest request) {
        String name = request.name().trim();
        if (tagRepository.findByOwnerIdAndNameIgnoreCase(userId, name).isPresent()) {
            throw new DuplicateResourceException("Tag '" + name + "' already exists");
        }
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return TagMapper.toResponse(tagRepository.save(new Tag(name, owner)));
    }

    public TagResponse rename(Long userId, Long tagId, TagRequest request) {
        Tag tag = findOwned(userId, tagId);
        String name = request.name().trim();
        if (tagRepository.existsByOwnerIdAndNameIgnoreCaseAndIdNot(userId, name, tagId)) {
            throw new DuplicateResourceException("Tag '" + name + "' already exists");
        }
        tag.setName(name);
        return TagMapper.toResponse(tag);
    }

    /** Removes the tag from every note first (the Note side owns the join table). */
    public void delete(Long userId, Long tagId) {
        Tag tag = findOwned(userId, tagId);
        for (Note note : new ArrayList<>(tag.getNotes())) {
            note.getTags().remove(tag);
        }
        tag.getNotes().clear();
        tagRepository.delete(tag);
    }

    /**
     * Resolves tag names to the owner's Tag entities, creating the missing ones.
     * Blank names are skipped and duplicates are merged case-insensitively ("Work" == "work").
     */
    public Set<Tag> findOrCreate(User owner, List<String> names) {
        Set<Tag> result = new HashSet<>();
        if (names == null || names.isEmpty()) {
            return result;
        }
        Map<String, String> unique = new LinkedHashMap<>();
        for (String raw : names) {
            if (raw != null && !raw.isBlank()) {
                String trimmed = raw.trim();
                unique.putIfAbsent(trimmed.toLowerCase(Locale.ROOT), trimmed);
            }
        }
        for (String name : unique.values()) {
            Tag tag = tagRepository.findByOwnerIdAndNameIgnoreCase(owner.getId(), name)
                    .orElseGet(() -> tagRepository.save(new Tag(name, owner)));
            result.add(tag);
        }
        return result;
    }

    private Tag findOwned(Long userId, Long tagId) {
        return tagRepository.findByIdAndOwnerId(tagId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag", tagId));
    }
}
