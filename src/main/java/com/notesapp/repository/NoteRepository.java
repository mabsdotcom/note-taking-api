package com.notesapp.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.notesapp.entity.Note;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Soft delete is applied explicitly: every "normal" query includes {@code DeletedFalse}
 * (or the notDeleted() specification), while trash and admin queries ask for {@code DeletedTrue}.
 */
public interface NoteRepository extends JpaRepository<Note, Long>, JpaSpecificationExecutor<Note> {

    Optional<Note> findByIdAndOwnerIdAndDeletedFalse(Long id, Long ownerId);

    Optional<Note> findByIdAndOwnerIdAndDeletedTrue(Long id, Long ownerId);

    /** Any state (active or in trash) - used for permanent delete and history. */
    Optional<Note> findByIdAndOwnerId(Long id, Long ownerId);

    Page<Note> findByOwnerIdAndDeletedTrue(Long ownerId, Pageable pageable);

    Page<Note> findByDeletedTrue(Pageable pageable);

    Optional<Note> findByIdAndDeletedTrue(Long id);

    List<Note> findByDeletedTrueAndDeletedAtBefore(Instant cutoff);

    long countByCategoryIdAndDeletedFalse(Long categoryId);

    /** Detaches all notes (including trashed ones) from a category that is about to be deleted. */
    @Modifying(flushAutomatically = true)
    @Query("update Note n set n.category = null where n.category.id = :categoryId")
    int clearCategory(@Param("categoryId") Long categoryId);
}
