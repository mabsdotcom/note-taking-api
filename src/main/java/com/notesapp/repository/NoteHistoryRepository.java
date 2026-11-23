package com.notesapp.repository;

import java.util.List;

import com.notesapp.entity.NoteHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteHistoryRepository extends JpaRepository<NoteHistory, Long> {

    List<NoteHistory> findByNoteIdOrderByPerformedAtDescIdDesc(Long noteId);
}
