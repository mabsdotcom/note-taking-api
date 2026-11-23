-- PostgreSQL schema generated from the JPA entities (for reference / manual setup).
-- With the default config Hibernate creates it automatically (ddl-auto).

CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    email       VARCHAR(100) NOT NULL UNIQUE,
    password    VARCHAR(100) NOT NULL,              -- BCrypt hash
    role        VARCHAR(20)  NOT NULL,              -- USER | ADMIN
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_by  VARCHAR(50),
    updated_by  VARCHAR(50)
);

CREATE TABLE categories (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL,
    description VARCHAR(255),
    color       VARCHAR(7),
    owner_id    BIGINT NOT NULL REFERENCES users(id),
    created_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_by  VARCHAR(50),
    updated_by  VARCHAR(50),
    CONSTRAINT uk_categories_owner_name UNIQUE (owner_id, name)
);
CREATE INDEX idx_categories_owner ON categories(owner_id);

CREATE TABLE tags (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(50) NOT NULL,
    owner_id    BIGINT NOT NULL REFERENCES users(id),
    created_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_by  VARCHAR(50),
    updated_by  VARCHAR(50),
    CONSTRAINT uk_tags_owner_name UNIQUE (owner_id, name)
);
CREATE INDEX idx_tags_owner ON tags(owner_id);

CREATE TABLE notes (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(200) NOT NULL,
    content     VARCHAR(200000),                    -- sanitized HTML
    plain_text  VARCHAR(100000),                    -- text used for search
    pinned      BOOLEAN NOT NULL,
    archived    BOOLEAN NOT NULL,
    deleted     BOOLEAN NOT NULL,                   -- soft delete flag
    deleted_at  TIMESTAMP(6) WITH TIME ZONE,
    owner_id    BIGINT NOT NULL REFERENCES users(id),
    category_id BIGINT REFERENCES categories(id),
    created_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_by  VARCHAR(50),
    updated_by  VARCHAR(50)
);
CREATE INDEX idx_notes_owner_deleted ON notes(owner_id, deleted);
CREATE INDEX idx_notes_title         ON notes(title);
CREATE INDEX idx_notes_updated_at    ON notes(updated_at);
CREATE INDEX idx_notes_category      ON notes(category_id);
CREATE INDEX idx_notes_deleted_at    ON notes(deleted_at);

CREATE TABLE note_tags (                            -- Many-to-Many notes <-> tags
    note_id BIGINT NOT NULL REFERENCES notes(id),
    tag_id  BIGINT NOT NULL REFERENCES tags(id),
    PRIMARY KEY (note_id, tag_id)
);

CREATE TABLE note_history (                         -- audit trail
    id             BIGSERIAL PRIMARY KEY,
    note_id        BIGINT NOT NULL REFERENCES notes(id),
    action         VARCHAR(20) NOT NULL,            -- CREATED | UPDATED | DELETED | RESTORED
    title_snapshot VARCHAR(200),
    performed_by   VARCHAR(50),
    performed_at   TIMESTAMP(6) WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_note_history_note ON note_history(note_id);

CREATE TABLE refresh_tokens (
    id         BIGSERIAL PRIMARY KEY,
    token      VARCHAR(100) NOT NULL UNIQUE,
    user_id    BIGINT NOT NULL REFERENCES users(id),
    expires_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    revoked    BOOLEAN NOT NULL
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);

-- Entity relationships
--   users 1 ── * notes, categories, tags, refresh_tokens
--   categories 1 ── * notes          (optional; deleting a category un-sets it on notes)
--   notes * ── * tags                (join table note_tags)
--   notes 1 ── * note_history        (cascade ALL + orphanRemoval)
