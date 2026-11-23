package com.notesapp.scheduler;

import java.time.Duration;
import java.time.Instant;

import com.notesapp.service.NoteService;
import com.notesapp.service.RefreshTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Background maintenance (runs only when scheduling is enabled, see SchedulingConfig):
 * empties the trash of notes deleted longer than the retention period, and removes
 * expired or revoked refresh tokens.
 */
@Component
public class CleanupJobs {

    private static final Logger log = LoggerFactory.getLogger(CleanupJobs.class);

    private final NoteService noteService;
    private final RefreshTokenService refreshTokenService;
    private final int trashRetentionDays;

    public CleanupJobs(NoteService noteService, RefreshTokenService refreshTokenService,
                       @Value("${app.trash.retention-days:30}") int trashRetentionDays) {
        this.noteService = noteService;
        this.refreshTokenService = refreshTokenService;
        this.trashRetentionDays = trashRetentionDays;
    }

    @Scheduled(cron = "${app.trash.cleanup-cron:0 0 3 * * *}")
    public void purgeOldTrash() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(trashRetentionDays));
        int purged = noteService.purgeDeletedBefore(cutoff);
        log.info("Trash cleanup: permanently deleted {} note(s) deleted before {}", purged, cutoff);
    }

    @Scheduled(cron = "${app.tokens.cleanup-cron:0 30 3 * * *}")
    public void purgeRefreshTokens() {
        int removed = refreshTokenService.deleteExpiredOrRevoked();
        log.info("Token cleanup: removed {} expired/revoked refresh token(s)", removed);
    }
}
