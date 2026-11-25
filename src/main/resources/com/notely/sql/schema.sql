-- ============================================================
-- Notely Database Schema
-- Run once against MySQL to create both databases.
-- ============================================================

-- =================== PRODUCTION DATABASE ===================

CREATE DATABASE IF NOT EXISTS notely
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE notely;

-- Folders group related notes.
CREATE TABLE IF NOT EXISTS folders (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    created_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- Notes are the core entity.
CREATE TABLE IF NOT EXISTS notes (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    title      VARCHAR(200) NOT NULL,
    content    TEXT,
    is_pinned  BOOLEAN      DEFAULT FALSE,
    folder_id  INT,
    created_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (folder_id) REFERENCES folders(id) ON DELETE SET NULL
);

-- Tags are lightweight labels.
CREATE TABLE IF NOT EXISTS tags (
    id   INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

-- Join table: many-to-many between notes and tags.
CREATE TABLE IF NOT EXISTS note_tags (
    note_id INT NOT NULL,
    tag_id  INT NOT NULL,
    PRIMARY KEY (note_id, tag_id),
    FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id)  REFERENCES tags(id)  ON DELETE CASCADE
);

-- ===================== TEST DATABASE =======================
-- Used exclusively by JUnit integration tests.
-- NEVER point production code at this database.

CREATE DATABASE IF NOT EXISTS notely_test
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE notely_test;

CREATE TABLE IF NOT EXISTS folders (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    created_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS notes (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    title      VARCHAR(200) NOT NULL,
    content    TEXT,
    is_pinned  BOOLEAN      DEFAULT FALSE,
    folder_id  INT,
    created_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (folder_id) REFERENCES folders(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS tags (
    id   INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS note_tags (
    note_id INT NOT NULL,
    tag_id  INT NOT NULL,
    PRIMARY KEY (note_id, tag_id),
    FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id)  REFERENCES tags(id)  ON DELETE CASCADE
);
