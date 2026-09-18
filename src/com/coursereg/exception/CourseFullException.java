package com.coursereg.exception;

/**
 * Checked exception thrown when a student attempts to enroll in a course
 * that has already reached its maximum seating capacity.
 */
public class CourseFullException extends Exception {
    private final String courseCode;
    private final int capacity;
    private final int currentEnrollment;

    /**
     * Constructs a new CourseFullException with relevant course metrics.
     *
     * @param courseCode        The code of the full course
     * @param currentEnrollment Active enrollment count
     * @param capacity          Maximum allowed capacity
     */
    public CourseFullException(String courseCode, int currentEnrollment, int capacity) {
        super(String.format("Registration rejected: Course %s is full (%d/%d seats occupied).",
                courseCode, currentEnrollment, capacity));
        this.courseCode = courseCode;
        this.currentEnrollment = currentEnrollment;
        this.capacity = capacity;
    }

    /**
     * Constructs a CourseFullException with a custom message.
     *
     * @param message Explanatory message
     */
    public CourseFullException(String message) {
        super(message);
        this.courseCode = "UNKNOWN";
        this.capacity = 0;
        this.currentEnrollment = 0;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getCurrentEnrollment() {
        return currentEnrollment;
    }
}
