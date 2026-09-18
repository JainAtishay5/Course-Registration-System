package com.coursereg.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Model class representing an undergraduate or graduate student enrolled in the university.
 * Extends the abstract Person base class and encapsulates academic credit limits,
 * completed prerequisite course history, and active course enrollments.
 */
public class Student extends Person {
    public static final String NO_COURSES = "NONE";

    private int maxCredits;
    private final List<String> registeredCourses;
    private final List<String> completedCourses;

    /**
     * Constructs a new Student instance.
     *
     * @param id               Unique student identifier (e.g. STU1001)
     * @param name             Student's full legal name
     * @param email            University student email address
     * @param maxCredits       Maximum semester credit allowance (positive integer)
     * @param completedCourses Initial list of completed prerequisite course codes
     */
    public Student(String id, String name, String email, int maxCredits, List<String> completedCourses) {
        super(id, name, email);
        if (maxCredits <= 0) {
            throw new IllegalArgumentException("Max credits limit must be strictly positive.");
        }
        this.maxCredits = maxCredits;
        this.registeredCourses = new ArrayList<>();
        this.completedCourses = new ArrayList<>();

        if (completedCourses != null) {
            for (String code : completedCourses) {
                if (code != null && !code.trim().isEmpty() && !NO_COURSES.equalsIgnoreCase(code.trim())) {
                    this.completedCourses.add(code.trim().toUpperCase());
                }
            }
        }
    }

    /**
     * Secondary constructor for students with no initial completed courses.
     */
    public Student(String id, String name, String email, int maxCredits) {
        this(id, name, email, maxCredits, new ArrayList<>());
    }

    @Override
    public String getRole() {
        return "Student";
    }

    public int getMaxCredits() {
        return maxCredits;
    }

    public void setMaxCredits(int maxCredits) {
        if (maxCredits <= 0) {
            throw new IllegalArgumentException("Max credits limit must be strictly positive.");
        }
        this.maxCredits = maxCredits;
    }

    public List<String> getRegisteredCourses() {
        return Collections.unmodifiableList(registeredCourses);
    }

    public List<String> getCompletedCourses() {
        return Collections.unmodifiableList(completedCourses);
    }

    /**
     * Determines whether the student has completed a given course code.
     *
     * @param courseCode The course code to check
     * @return true if course code is in completedCourses (case-insensitive), false otherwise
     */
    public boolean hasCompleted(String courseCode) {
        if (courseCode == null || courseCode.trim().isEmpty()) {
            return false;
        }
        String normalized = courseCode.trim().toUpperCase();
        for (String c : completedCourses) {
            if (c.equalsIgnoreCase(normalized)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines whether the student is actively registered for a course code this semester.
     *
     * @param courseCode The course code to verify
     * @return true if currently registered, false otherwise
     */
    public boolean isRegistered(String courseCode) {
        if (courseCode == null || courseCode.trim().isEmpty()) {
            return false;
        }
        String normalized = courseCode.trim().toUpperCase();
        for (String c : registeredCourses) {
            if (c.equalsIgnoreCase(normalized)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds a course code to the active registration list.
     *
     * @param courseCode Course code to enroll into
     * @throws IllegalArgumentException if course code is already registered
     */
    public void addRegisteredCourse(String courseCode) {
        if (courseCode == null || courseCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Course code cannot be null or empty.");
        }
        String normalized = courseCode.trim().toUpperCase();
        if (isRegistered(normalized)) {
            throw new IllegalArgumentException("Student " + getId() + " is already registered for course " + normalized);
        }
        registeredCourses.add(normalized);
    }

    /**
     * Removes a course code from the active registration list.
     *
     * @param courseCode Course code to drop
     * @return true if removed, false if not found
     */
    public boolean removeRegisteredCourse(String courseCode) {
        if (courseCode == null || courseCode.trim().isEmpty()) {
            return false;
        }
        String normalized = courseCode.trim().toUpperCase();
        return registeredCourses.removeIf(c -> c.equalsIgnoreCase(normalized));
    }

    /**
     * Clears all active course registrations.
     */
    public void clearRegisteredCourses() {
        registeredCourses.clear();
    }

    /**
     * Adds a course code to the student's completed academic history.
     *
     * @param courseCode Course code to mark as completed
     */
    public void addCompletedCourse(String courseCode) {
        if (courseCode == null || courseCode.trim().isEmpty()) {
            return;
        }
        String normalized = courseCode.trim().toUpperCase();
        if (!hasCompleted(normalized)) {
            completedCourses.add(normalized);
        }
    }

    /**
     * Serializes student profile to CSV line.
     * Format: id,name,email,maxCredits,completedCourses(semicolon-separated or NONE)
     *
     * @return CSV formatted record string
     */
    public String toCsv() {
        String completedStr = completedCourses.isEmpty()
                ? NO_COURSES
                : String.join(";", completedCourses);

        return String.format("%s,%s,%s,%d,%s",
                getId(),
                escapeCsv(getName()),
                escapeCsv(getEmail()),
                maxCredits,
                completedStr);
    }

    /**
     * Deserializes a student record from a CSV line.
     *
     * @param csvLine CSV line containing student attributes
     * @return Student instance
     * @throws IllegalArgumentException if format is invalid
     */
    public static Student fromCsv(String csvLine) {
        if (csvLine == null || csvLine.trim().isEmpty()) {
            throw new IllegalArgumentException("CSV line cannot be null or empty.");
        }
        String[] parts = csvLine.split(",", -1);
        if (parts.length < 5) {
            throw new IllegalArgumentException("Malformed Student CSV line (expected 5 columns): " + csvLine);
        }

        try {
            String id = parts[0].trim();
            String name = unescapeCsv(parts[1].trim());
            String email = unescapeCsv(parts[2].trim());
            int maxCredits = Integer.parseInt(parts[3].trim());
            String completedRaw = parts[4].trim();

            List<String> completedList = new ArrayList<>();
            if (!completedRaw.isEmpty() && !NO_COURSES.equalsIgnoreCase(completedRaw)) {
                String[] items = completedRaw.split(";");
                for (String item : items) {
                    if (!item.trim().isEmpty() && !NO_COURSES.equalsIgnoreCase(item.trim())) {
                        completedList.add(item.trim().toUpperCase());
                    }
                }
            }

            return new Student(id, name, email, maxCredits, completedList);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Failed to parse numeric attributes in Student CSV: " + csvLine, e);
        }
    }

    private static String escapeCsv(String text) {
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    private static String unescapeCsv(String text) {
        if (text.startsWith("\"") && text.endsWith("\"") && text.length() >= 2) {
            String inner = text.substring(1, text.length() - 1);
            return inner.replace("\"\"", "\"");
        }
        return text;
    }

    @Override
    public String toString() {
        return String.format("[Student] %s - %s (%s) | Max Credits: %d | Registered: %d | Completed: %s",
                getId(), getName(), getEmail(), maxCredits, registeredCourses.size(),
                completedCourses.isEmpty() ? "None" : String.join(", ", completedCourses));
    }
}
