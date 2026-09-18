package com.coursereg.model;

import java.util.Objects;

/**
 * Abstract base class representing a person within the university domain.
 * Provides core identification and contact attributes common to all university members.
 */
public abstract class Person {
    protected String id;
    protected String name;
    protected String email;

    /**
     * Constructs a new Person with required identity attributes.
     *
     * @param id    Unique institutional identifier (e.g., STU1001)
     * @param name  Full legal name of the person
     * @param email Official university email address
     */
    public Person(String id, String name, String email) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Person ID cannot be null or empty.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Person name cannot be null or empty.");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Person email cannot be null or empty.");
        }
        this.id = id.trim();
        this.name = name.trim();
        this.email = email.trim();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Person ID cannot be null or empty.");
        }
        this.id = id.trim();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Person name cannot be null or empty.");
        }
        this.name = name.trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Person email cannot be null or empty.");
        }
        this.email = email.trim();
    }

    /**
     * Returns the institutional role associated with the concrete subclass.
     *
     * @return String representation of the person's role (e.g., "Student", "Faculty")
     */
    public abstract String getRole();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Person person = (Person) o;
        return Objects.equals(id.toUpperCase(), person.id.toUpperCase());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id.toUpperCase());
    }

    @Override
    public String toString() {
        return String.format("[%s] ID: %s, Name: %s, Email: %s", getRole(), id, name, email);
    }
}
