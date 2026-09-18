package com.coursereg.exception;

/**
 * Checked exception thrown when an enrollment attempt would cause a student's
 * total registered semester credits to exceed their authorized maximum credit limit.
 */
public class CreditLimitExceededException extends Exception {
    private final String studentId;
    private final int currentCredits;
    private final int attemptedCredits;
    private final int maxCredits;

    /**
     * Constructs a CreditLimitExceededException with quantitative credit audit values.
     *
     * @param studentId        ID of the student attempting registration
     * @param currentCredits   Currently enrolled semester credits
     * @param attemptedCredits Credits associated with candidate course
     * @param maxCredits       Maximum authorized semester credit limit
     */
    public CreditLimitExceededException(String studentId, int currentCredits, int attemptedCredits, int maxCredits) {
        super(String.format(
                "Registration rejected: Enrolling in course with %d credits would bring student %s to %d credits, exceeding the maximum limit of %d credits.",
                attemptedCredits, studentId, (currentCredits + attemptedCredits), maxCredits));
        this.studentId = studentId;
        this.currentCredits = currentCredits;
        this.attemptedCredits = attemptedCredits;
        this.maxCredits = maxCredits;
    }

    /**
     * Constructs a CreditLimitExceededException with a custom message.
     *
     * @param message Explanatory message
     */
    public CreditLimitExceededException(String message) {
        super(message);
        this.studentId = "UNKNOWN";
        this.currentCredits = 0;
        this.attemptedCredits = 0;
        this.maxCredits = 0;
    }

    public String getStudentId() {
        return studentId;
    }

    public int getCurrentCredits() {
        return currentCredits;
    }

    public int getAttemptedCredits() {
        return attemptedCredits;
    }

    public int getMaxCredits() {
        return maxCredits;
    }

    public int getProjectedCredits() {
        return currentCredits + attemptedCredits;
    }
}
