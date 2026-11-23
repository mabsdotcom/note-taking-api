package com.notesapp.service;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import com.notesapp.dto.common.PageResponse;
import com.notesapp.dto.note.NoteHistoryResponse;
import com.notesapp.dto.note.NoteRequest;
import com.notesapp.dto.note.NoteResponse;
import com.notesapp.dto.note.NoteSearchCriteria;
import com.notesapp.dto.note.NoteSummaryResponse;
import com.notesapp.entity.Category;
import com.notesapp.entity.Note;
import com.notesapp.entity.NoteAction;
import com.notesapp.entity.User;
import com.notesapp.exception.BadRequestException;
import com.notesapp.exception.ResourceNotFoundException;
import com.notesapp.mapper.NoteMapper;
import com.notesapp.repository.CategoryRepository;
import com.notesapp.repository.NoteHistoryRepository;
import com.notesapp.repository.NoteRepository;
import com.notesapp.repository.UserRepository;
import com.notesapp.specification.NoteSpecifications;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for notes. Every method takes the id of the authenticated user, and
 * notes of other users are reported as "not found" so their existence is not revealed.
 */
@Service
@Transactional
public class NoteService {

    static final Set<String> SORTABLE_FIELDS = Set.of("title", "createdAt", "updatedAt", "pinned");

    private final NoteRepository noteRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final NoteHistoryRepository historyRepository;
    private final TagService tagService;
    private final HtmlSanitizer htmlSanitizer;
    private final CsvExportService csvExportService;

    public NoteService(NoteRepository noteRepository, UserRepository userRepository,
                       CategoryRepository categoryRepository, NoteHistoryRepository historyRepository,
                       TagService tagService, HtmlSanitizer htmlSanitizer, CsvExportService csvExportService) {
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.historyRepository = historyRepository;
        this.tagService = tagService;
        this.htmlSanitizer = htmlSanitizer;
        this.csvExportService = csvExportService;
    }

    // ---------------------------------------------------------------- CRUD

    public NoteResponse create(Long userId, NoteRequest request) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Note note = new Note();
        note.setOwner(owner);
        applyRequest(note, request, userId);
        note.recordHistory(NoteAction.CREATED);
        return NoteMapper.toResponse(noteRepository.save(note));
    }

    @Transactional(readOnly = true)
    public NoteResponse getById(Long userId, Long noteId) {
        return NoteMapper.toResponse(findActive(userId, noteId));
    }

    public NoteResponse update(Long userId, Long noteId, NoteRequest request) {
        Note note = findActive(userId, noteId);
        applyRequest(note, request, userId);
        note.recordHistory(NoteAction.UPDATED);
        // flush so the response contains the new updatedAt/updatedBy audit values
        return NoteMapper.toResponse(noteRepository.saveAndFlush(note));
    }

    /** Soft delete: the note moves to the trash and can be restored. */
    public void delete(Long userId, Long noteId) {
        findActive(userId, noteId).softDelete();
    }

    // ---------------------------------------------------------------- search

    @Transactional(readOnly = true)
    public PageResponse<NoteSummaryResponse> search(Long userId, NoteSearchCriteria criteria, Pageable pageable) {
        SortValidator.validate(pageable, SORTABLE_FIELDS);
        Specification<Note> spec = NoteSpecifications.fromCriteria(userId, criteria);
        return PageResponse.from(noteRepository.findAll(spec, pageable).map(NoteMapper::toSummary));
    }

    @Transactional(readOnly = true)
    public String exportCsv(Long userId, NoteSearchCriteria criteria) {
        Specification<Note> spec = NoteSpecifications.fromCriteria(userId, criteria);
        List<Note> notes = noteRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "updatedAt"));
        return csvExportService.toCsv(notes);
    }

    // ---------------------------------------------------------------- trash & history

    @Transactional(readOnly = true)
    public PageResponse<NoteSummaryResponse> trash(Long userId, Pageable pageable) {
        SortValidator.validate(pageable, Set.of("deletedAt", "title", "updatedAt"));
        return PageResponse.from(noteRepository.findByOwnerIdAndDeletedTrue(userId, pageable)
                .map(NoteMapper::toSummary));
    }

    public NoteResponse restore(Long userId, Long noteId) {
        Note note = noteRepository.findByIdAndOwnerIdAndDeletedTrue(noteId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Deleted note not found with id " + noteId));
        note.restore();
        return NoteMapper.toResponse(noteRepository.saveAndFlush(note));
    }

    /** Hard delete; history and tag links are removed with the note (cascade / join table). */
    public void deletePermanently(Long userId, Long noteId) {
        Note note = noteRepository.findByIdAndOwnerId(noteId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Note", noteId));
        noteRepository.delete(note);
    }

    @Transactional(readOnly = true)
    public List<NoteHistoryResponse> history(Long userId, Long noteId) {
        noteRepository.findByIdAndOwnerId(noteId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Note", noteId));
        return historyRepository.findByNoteIdOrderByPerformedAtDescIdDesc(noteId).stream()
                .map(NoteMapper::toHistory)
                .toList();
    }

    // ---------------------------------------------------------------- admin

    @Transactional(readOnly = true)
    public PageResponse<NoteSummaryResponse> listAllDeleted(Pageable pageable) {
        SortValidator.validate(pageable, Set.of("deletedAt", "title", "updatedAt"));
        return PageResponse.from(noteRepository.findByDeletedTrue(pageable).map(NoteMapper::toSummary));
    }

    public NoteResponse adminRestore(Long noteId) {
        Note note = noteRepository.findByIdAndDeletedTrue(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("Deleted note not found with id " + noteId));
        note.restore();
        return NoteMapper.toResponse(noteRepository.saveAndFlush(note));
    }

    /** Used by the scheduled cleanup job. Returns the number of purged notes. */
    public int purgeDeletedBefore(Instant cutoff) {
        List<Note> expired = noteRepository.findByDeletedTrueAndDeletedAtBefore(cutoff);
        noteRepository.deleteAll(expired);
        return expired.size();
    }

    // ---------------------------------------------------------------- helpers

    private Note findActive(Long userId, Long noteId) {
        return noteRepository.findByIdAndOwnerIdAndDeletedFalse(noteId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Note", noteId));
    }

    private void applyRequest(Note note, NoteRequest request, Long userId) {
        boolean pinned = Boolean.TRUE.equals(request.pinned());
        boolean archived = Boolean.TRUE.equals(request.archived());
        if (pinned && archived) {
            throw new BadRequestException("An archived note cannot be pinned");
        }

        String safeHtml = htmlSanitizer.sanitize(request.content());
        note.setTitle(request.title().trim());
        note.setContent(safeHtml);
        note.setPlainText(htmlSanitizer.toPlainText(safeHtml));
        note.setPinned(pinned);
        note.setArchived(archived);
        note.setCategory(resolveCategory(userId, request.categoryId()));

        note.getTags().clear();
        note.getTags().addAll(tagService.findOrCreate(note.getOwner(), request.tags()));
    }

    private Category resolveCategory(Long userId, Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findByIdAndOwnerId(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
    }
}
