package com.notesapp.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** One entry of a note's audit trail: who did what, and when. */
@Entity
@Table(name = "note_history", indexes = @Index(name = "idx_note_history_note", columnList = "note_id"))
@EntityListeners(AuditingEntityListener.class)
public class NoteHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "note_id", nullable = false)
    private Note note;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private NoteAction action;

    /** Title at the time of the change, so renames are visible in the history. */
    @Column(name = "title_snapshot", length = 200)
    private String titleSnapshot;

    @CreatedBy
    @Column(name = "performed_by", length = 50, updatable = false)
    private String performedBy;

    @CreatedDate
    @Column(name = "performed_at", nullable = false, updatable = false)
    private Instant performedAt;

    protected NoteHistory() {
        // for JPA
    }

    public NoteHistory(Note note, NoteAction action, String titleSnapshot) {
        this.note = note;
        this.action = action;
        this.titleSnapshot = titleSnapshot;
    }

    public Long getId() {
        return id;
    }

    public Note getNote() {
        return note;
    }

    public NoteAction getAction() {
        return action;
    }

    public String getTitleSnapshot() {
        return titleSnapshot;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public Instant getPerformedAt() {
        return performedAt;
    }
}
