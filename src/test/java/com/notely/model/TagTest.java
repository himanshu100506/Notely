package com.notely.model;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Tag} — constructors, getters/setters,
 * equals/hashCode contract, and HashSet behaviour.
 */
class TagTest {

    // ───────────────── Constructor tests ─────────────────

    @Test
    void noArgConstructorCreatesEmptyTag() {
        Tag tag = new Tag();
        assertEquals(0, tag.getId());
        assertNull(tag.getName());
    }

    @Test
    void nameOnlyConstructorSetsName() {
        Tag tag = new Tag("java");
        assertEquals(0, tag.getId(), "ID should default to 0 before DB insert");
        assertEquals("java", tag.getName());
    }

    @Test
    void fullConstructorSetsAllFields() {
        Tag tag = new Tag(5, "urgent");
        assertEquals(5, tag.getId());
        assertEquals("urgent", tag.getName());
    }

    // ───────────────── Getter / Setter tests ─────────────

    @Test
    void settersUpdateFields() {
        Tag tag = new Tag();
        tag.setId(10);
        tag.setName("recipes");

        assertEquals(10, tag.getId());
        assertEquals("recipes", tag.getName());
    }

    // ───────────────── equals() and hashCode() ───────────

    @Test
    void tagsWithSameIdAreEqual() {
        Tag a = new Tag(1, "java");
        Tag b = new Tag(1, "Java");          // different name, same id
        assertEquals(a, b, "Tags with the same id must be equal");
    }

    @Test
    void tagsWithDifferentIdsAreNotEqual() {
        Tag a = new Tag(1, "java");
        Tag b = new Tag(2, "java");          // same name, different id
        assertNotEquals(a, b);
    }

    @Test
    void equalsReturnsFalseForNull() {
        Tag tag = new Tag(1, "java");
        assertNotEquals(null, tag);
    }

    @Test
    void equalsReturnsFalseForDifferentType() {
        Tag tag = new Tag(1, "java");
        assertNotEquals("java", tag);
    }

    @Test
    void hashCodeIsConsistentWithEquals() {
        Tag a = new Tag(1, "java");
        Tag b = new Tag(1, "Java");
        assertEquals(a.hashCode(), b.hashCode(),
                "Equal objects must have the same hashCode");
    }

    // ───────── HashSet behaviour (depends on equals/hashCode) ────────

    @Test
    void hashSetPreventsTagDuplicatesBySameId() {
        Set<Tag> tags = new HashSet<>();
        tags.add(new Tag(1, "java"));
        tags.add(new Tag(1, "java"));        // duplicate id
        assertEquals(1, tags.size(), "HashSet should contain only one Tag with id 1");
    }

    @Test
    void hashSetKeepsTagsWithDifferentIds() {
        Set<Tag> tags = new HashSet<>();
        tags.add(new Tag(1, "java"));
        tags.add(new Tag(2, "python"));
        assertEquals(2, tags.size());
    }

    // ───────────────── toString ──────────────────────────

    @Test
    void toStringContainsIdAndName() {
        Tag tag = new Tag(3, "design");
        String result = tag.toString();
        assertTrue(result.contains("3"));
        assertTrue(result.contains("design"));
    }
}
