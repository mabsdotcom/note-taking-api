package com.notesapp.repository;

import java.util.List;
import java.util.Optional;

import com.notesapp.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, Long> {

    List<Tag> findByOwnerIdOrderByNameAsc(Long ownerId);

    Optional<Tag> findByIdAndOwnerId(Long id, Long ownerId);

    Optional<Tag> findByOwnerIdAndNameIgnoreCase(Long ownerId, String name);

    boolean existsByOwnerIdAndNameIgnoreCaseAndIdNot(Long ownerId, String name, Long id);
}
