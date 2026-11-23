package com.notesapp.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * A rich text note.
 * <ul>
 *   <li>{@code content} holds sanitized HTML (rich text).</li>
 *   <li>{@code plainText} is the same content with tags stripped; it is what text search runs against.</li>
 *   <li>Deleting a note is a soft delete ({@code deleted = true}); notes stay in the trash until
 *       restored, permanently deleted, or purged by the scheduled cleanup job.</li>
 * </ul>
 */
@Entity
@Table(name = "notes", indexes = {
        @Index(name = "idx_notes_owner_deleted", columnList = "owner_id, deleted"),
        @Index(name = "idx_notes_title", columnList = "title"),
        @Index(name = "idx_notes_updated_at", columnList = "updated_at"),
        @Index(name = "idx_notes_category", columnList = "category_id"),
        @Index(name = "idx_notes_deleted_at", columnList = "deleted_at")
})
public class Note extends BaseEntity {

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", length = 200_000)
    private String content;

    @Column(name = "plain_text", length = 100_000)
    private String plainText;

    @Column(name = "pinned", nullable = false)
    private boolean pinned;

    @Column(name = "archived", nullable = false)
    private boolean archived;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToMany
    @JoinTable(name = "note_tags",
            joinColumns = @JoinColumn(name = "note_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();

    /** Change history is owned by the note: saved with it and removed with it. */
    @OneToMany(mappedBy = "note", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("performedAt DESC")
    private List<NoteHistory> history = new ArrayList<>();

    public Note() {
        // for JPA and services
    }

    /** Adds an entry to the change history; it is persisted together with the note. */
    public void recordHistory(NoteAction action) {
        history.add(new NoteHistory(this, action, title));
    }

    public void softDelete() {
        this.deleted = true;
        this.deletedAt = Instant.now();
        recordHistory(NoteAction.DELETED);
    }

    public void restore() {
        this.deleted = false;
        this.deletedAt = null;
        recordHistory(NoteAction.RESTORED);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getPlainText() {
        return plainText;
    }

    public void setPlainText(String plainText) {
        this.plainText = plainText;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Set<Tag> getTags() {
        return tags;
    }

    public List<NoteHistory> getHistory() {
        return history;
    }
}
