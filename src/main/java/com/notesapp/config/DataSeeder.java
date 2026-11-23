package com.notesapp.config;

import java.util.List;
import java.util.Set;

import com.notesapp.entity.Category;
import com.notesapp.entity.Note;
import com.notesapp.entity.NoteAction;
import com.notesapp.entity.Role;
import com.notesapp.entity.Tag;
import com.notesapp.entity.User;
import com.notesapp.repository.CategoryRepository;
import com.notesapp.repository.NoteRepository;
import com.notesapp.repository.TagRepository;
import com.notesapp.repository.UserRepository;
import com.notesapp.service.HtmlSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inserts demo data on an empty database when app.seed.enabled=true (dev profile).
 * Credentials: admin / Admin@123 (ADMIN), demo / Demo@1234 (USER).
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final NoteRepository noteRepository;
    private final PasswordEncoder passwordEncoder;
    private final HtmlSanitizer htmlSanitizer;

    public DataSeeder(UserRepository userRepository, CategoryRepository categoryRepository,
                      TagRepository tagRepository, NoteRepository noteRepository,
                      PasswordEncoder passwordEncoder, HtmlSanitizer htmlSanitizer) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.noteRepository = noteRepository;
        this.passwordEncoder = passwordEncoder;
        this.htmlSanitizer = htmlSanitizer;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        userRepository.save(new User("admin", "admin@notes.local", passwordEncoder.encode("Admin@123"), Role.ADMIN));
        User demo = userRepository.save(new User("demo", "demo@notes.local", passwordEncoder.encode("Demo@1234"), Role.USER));

        Category work = categoryRepository.save(new Category("Work", "Meetings and projects", "#1E88E5", demo));
        Category personal = categoryRepository.save(new Category("Personal", "Life admin and ideas", "#43A047", demo));
        Category study = categoryRepository.save(new Category("Study", "Course notes", "#FB8C00", demo));

        Tag meeting = tagRepository.save(new Tag("meeting", demo));
        Tag urgent = tagRepository.save(new Tag("urgent", demo));
        Tag ideas = tagRepository.save(new Tag("ideas", demo));
        Tag java = tagRepository.save(new Tag("java", demo));
        Tag shopping = tagRepository.save(new Tag("shopping", demo));

        List<Note> notes = List.of(
                note(demo, "Sprint planning", "<h2>Agenda</h2><ul><li><b>Review</b> backlog</li>"
                        + "<li>Estimate stories</li></ul><p>Remember to invite the QA team.</p>", work,
                        Set.of(meeting, urgent), true),
                note(demo, "Retrospective notes", "<p>What went well: <i>pairing</i>. "
                        + "To improve: smaller pull requests.</p>", work, Set.of(meeting), false),
                note(demo, "Spring Security cheatsheet", "<h3>JWT flow</h3><ol><li>Login</li>"
                        + "<li>Send <code>Authorization: Bearer ...</code></li></ol>", study, Set.of(java), false),
                note(demo, "JPA relationships", "<p><b>@ManyToMany</b> needs an owning side with "
                        + "<code>@JoinTable</code>.</p>", study, Set.of(java), false),
                note(demo, "Weekend groceries", "<ul><li>Milk</li><li>Coffee</li><li>Apples</li></ul>",
                        personal, Set.of(shopping), false),
                note(demo, "App ideas", "<p>A habit tracker that <mark>syncs</mark> with notes.</p>",
                        personal, Set.of(ideas), false));
        noteRepository.saveAll(notes);
        log.info("Seeded demo data: users 'admin' (Admin@123) and 'demo' (Demo@1234), {} notes", notes.size());
    }

    private Note note(User owner, String title, String html, Category category, Set<Tag> tags, boolean pinned) {
        Note note = new Note();
        note.setOwner(owner);
        note.setTitle(title);
        String safe = htmlSanitizer.sanitize(html);
        note.setContent(safe);
        note.setPlainText(htmlSanitizer.toPlainText(safe));
        note.setCategory(category);
        note.getTags().addAll(tags);
        note.setPinned(pinned);
        note.recordHistory(NoteAction.CREATED);
        return note;
    }
}
