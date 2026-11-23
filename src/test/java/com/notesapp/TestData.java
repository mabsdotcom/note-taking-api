package com.notesapp;

import com.notesapp.entity.Category;
import com.notesapp.entity.Note;
import com.notesapp.entity.Role;
import com.notesapp.entity.Tag;
import com.notesapp.entity.User;
import org.springframework.test.util.ReflectionTestUtils;

/** Builders for entities with ids, for unit tests that do not touch the database. */
public final class TestData {

    private TestData() {
    }

    public static User user(long id, String username) {
        User u = new User(username, username + "@test.local", "hashed", Role.USER);
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    public static Category category(long id, String name, User owner) {
        Category c = new Category(name, null, null, owner);
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }

    public static Tag tag(long id, String name, User owner) {
        Tag t = new Tag(name, owner);
        ReflectionTestUtils.setField(t, "id", id);
        return t;
    }

    public static Note note(long id, String title, User owner) {
        Note n = new Note();
        ReflectionTestUtils.setField(n, "id", id);
        n.setTitle(title);
        n.setContent("<p>" + title + "</p>");
        n.setPlainText(title);
        n.setOwner(owner);
        return n;
    }
}
