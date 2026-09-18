package com.coursereg.model;

import java.util.Objects;

/**
 * Model class representing a university course offering.
 * Encapsulates course code, title, credit weight, maximum student capacity,
 * current enrollment count, and prerequisite course requirements.
 */
public class Course {
    public static final String NO_PREREQUISITE = "NONE";

    private String courseCode;
    private String title;
    private int credits;
    private int capacity;
    private int enrolledCount;
    private String prerequisiteCode;

    /**
     * Constructs a Course instance with full specifications.
     *
     * @param courseCode       Unique catalog code (e.g., CS101, MATH201)
     * @param title            Descriptive title of the course
     * @param credits          Academic credits awarded (positive integer)
     * @param capacity         Maximum seating capacity (positive integer)
     * @param enrolledCount    Number of actively enrolled students (>= 0 and <= capacity)
     * @param prerequisiteCode Course code of prerequisite or "NONE"
     */
    public Course(String courseCode, String title, int credits, int capacity, int enrolledCount, String prerequisiteCode) {
        if (courseCode == null || courseCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Course code cannot be null or empty.");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Course title cannot be null or empty.");
        }
        if (credits <= 0) {
            throw new IllegalArgumentException("Course credits must be strictly positive.");
        }
        if (capacity <= 0) {
            throw new IllegalArgumentException("Course capacity must be strictly positive.");
        }
        if (enrolledCount < 0) {
            throw new IllegalArgumentException("Enrolled count cannot be negative.");
        }
        if (enrolledCount > capacity) {
            throw new IllegalArgumentException("Enrolled count cannot exceed total course capacity.");
        }

        this.courseCode = courseCode.trim().toUpperCase();
        this.title = title.trim();
        this.credits = credits;
        this.capacity = capacity;
        this.enrolledCount = enrolledCount;
        this.prerequisiteCode = (prerequisiteCode == null || prerequisiteCode.trim().isEmpty())
                ? NO_PREREQUISITE
                : prerequisiteCode.trim().toUpperCase();
    }

    /**
     * Overloaded constructor for newly scheduled courses with 0 initial enrollments.
     */
    public Course(String courseCode, String title, int credits, int capacity, String prerequisiteCode) {
        this(courseCode, title, credits, capacity, 0, prerequisiteCode);
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        if (courseCode == null || courseCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Course code cannot be null or empty.");
        }
        this.courseCode = courseCode.trim().toUpperCase();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Course title cannot be null or empty.");
        }
        this.title = title.trim();
    }

    public int getCredits() {
        return credits;
    }

    public void setCredits(int credits) {
        if (credits <= 0) {
            throw new IllegalArgumentException("Course credits must be strictly positive.");
        }
        this.credits = credits;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Course capacity must be strictly positive.");
        }
        if (this.enrolledCount > capacity) {
            throw new IllegalArgumentException("Capacity cannot be reduced below current enrollment count.");
        }
        this.capacity = capacity;
    }

    public int getEnrolledCount() {
        return enrolledCount;
    }

    public void setEnrolledCount(int enrolledCount) {
        if (enrolledCount < 0 || enrolledCount > capacity) {
            throw new IllegalArgumentException("Enrolled count must be between 0 and course capacity (" + capacity + ").");
        }
        this.enrolledCount = enrolledCount;
    }

    public String getPrerequisiteCode() {
        return prerequisiteCode;
    }

    public void setPrerequisiteCode(String prerequisiteCode) {
        this.prerequisiteCode = (prerequisiteCode == null || prerequisiteCode.trim().isEmpty())
                ? NO_PREREQUISITE
                : prerequisiteCode.trim().toUpperCase();
    }

    /**
     * Checks whether the course has reached or exceeded maximum enrollment capacity.
     *
     * @return true if enrolledCount >= capacity, false otherwise
     */
    public boolean isFull() {
        return enrolledCount >= capacity;
    }

    /**
     * Checks if this course enforces a prerequisite.
     *
     * @return true if a valid prerequisite course code is configured, false if "NONE"
     */
    public boolean hasPrerequisite() {
        return prerequisiteCode != null && !NO_PREREQUISITE.equalsIgnoreCase(prerequisiteCode);
    }

    /**
     * Returns remaining available seats in this course.
     *
     * @return non-negative integer indicating open seats
     */
    public int getAvailableSeats() {
        return Math.max(0, capacity - enrolledCount);
    }

    /**
     * Increments the enrolled student count by 1.
     *
     * @throws IllegalStateException if course is already at full capacity
     */
    public void incrementEnrollment() {
        if (isFull()) {
            throw new IllegalStateException("Cannot increment enrollment: Course " + courseCode + " is already full.");
        }
        this.enrolledCount++;
    }

    /**
     * Decrements the enrolled student count by 1.
     *
     * @throws IllegalStateException if enrollment count is already 0
     */
    public void decrementEnrollment() {
        if (this.enrolledCount <= 0) {
            throw new IllegalStateException("Cannot decrement enrollment: Course " + courseCode + " has zero enrollments.");
        }
        this.enrolledCount--;
    }

    /**
     * Serializes this Course into a standard CSV format line.
     * Format: courseCode,title,credits,capacity,enrolledCount,prerequisiteCode
     *
     * @return CSV formatted string
     */
    public String toCsv() {
        return String.format("%s,%s,%d,%d,%d,%s",
                courseCode,
                escapeCsv(title),
                credits,
                capacity,
                enrolledCount,
                prerequisiteCode);
    }

    /**
     * Parses a CSV formatted line into a Course object.
     *
     * @param csvLine CSV line containing course record
     * @return Deserialized Course instance
     * @throws IllegalArgumentException if line is malformed or fields cannot be parsed
     */
    public static Course fromCsv(String csvLine) {
        if (csvLine == null || csvLine.trim().isEmpty()) {
            throw new IllegalArgumentException("CSV line cannot be null or empty.");
        }
        String[] parts = csvLine.split(",", -1);
        if (parts.length < 6) {
            throw new IllegalArgumentException("Malformed Course CSV line (expected 6 columns): " + csvLine);
        }

        try {
            String code = parts[0].trim();
            String title = unescapeCsv(parts[1].trim());
            int credits = Integer.parseInt(parts[2].trim());
            int capacity = Integer.parseInt(parts[3].trim());
            int enrolled = Integer.parseInt(parts[4].trim());
            String prereq = parts[5].trim();

            return new Course(code, title, credits, capacity, enrolled, prereq);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Failed to parse numeric attributes in Course CSV: " + csvLine, e);
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Course course = (Course) o;
        return Objects.equals(courseCode, course.courseCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(courseCode);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s | %d Credits | Seats: %d/%d | Prereq: %s",
                courseCode, title, credits, enrolledCount, capacity, prerequisiteCode);
    }
}
