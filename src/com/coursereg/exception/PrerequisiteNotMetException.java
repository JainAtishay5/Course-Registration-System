package com.coursereg.exception;

/**
 * Checked exception thrown when a student attempts to register for an advanced course
 * without having completed the mandatory prerequisite course.
 */
public class PrerequisiteNotMetException extends Exception {
    private final String courseCode;
    private final String prerequisiteCode;
    private final String studentId;

    /**
     * Constructs a PrerequisiteNotMetException with explicit course requirement details.
     *
     * @param studentId        ID of the student lacking the prerequisite
     * @param courseCode       Target course the student is attempting to register for
     * @param prerequisiteCode Required prerequisite course code that has not been completed
     */
    public PrerequisiteNotMetException(String studentId, String courseCode, String prerequisiteCode) {
        super(String.format(
                "Registration rejected: Course %s requires prerequisite %s, which has not been completed by student %s.",
                courseCode, prerequisiteCode, studentId));
        this.studentId = studentId;
        this.courseCode = courseCode;
        this.prerequisiteCode = prerequisiteCode;
    }

    /**
     * Constructs a PrerequisiteNotMetException with a custom message.
     *
     * @param message Explanatory message
     */
    public PrerequisiteNotMetException(String message) {
        super(message);
        this.studentId = "UNKNOWN";
        this.courseCode = "UNKNOWN";
        this.prerequisiteCode = "UNKNOWN";
    }

    public String getStudentId() {
        return studentId;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public String getPrerequisiteCode() {
        return prerequisiteCode;
    }
}
