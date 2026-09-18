package com.coursereg.service;

import com.coursereg.exception.CourseFullException;
import com.coursereg.exception.CreditLimitExceededException;
import com.coursereg.exception.PrerequisiteNotMetException;
import com.coursereg.model.Course;
import com.coursereg.model.Student;
import com.coursereg.storage.CsvStorageManager;
import com.coursereg.storage.CsvStorageManager.EnrollmentRecord;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Core business logic service for university course registration.
 * Enforces strict academic validation rules including prerequisite fulfillment,
 * seat capacity constraints, and semester credit limit ceilings.
 * Also provides terminal ASCII analytics for student credit utilization.
 */
public class RegistrationService {
    private static final int ASCII_GAUGE_WIDTH = 20;
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CsvStorageManager storageManager;
    private final Map<String, Course> courses;
    private final Map<String, Student> students;
    private final List<EnrollmentRecord> enrollments;

    /**
     * Constructs a RegistrationService with a default CsvStorageManager.
     */
    public RegistrationService() {
        this(new CsvStorageManager());
    }

    /**
     * Constructs a RegistrationService with a designated CsvStorageManager.
     * Synchronizes in-memory models with persistent records from CSV files.
     *
     * @param storageManager Storage subsystem instance
     */
    public RegistrationService(CsvStorageManager storageManager) {
        if (storageManager == null) {
            throw new IllegalArgumentException("CsvStorageManager cannot be null.");
        }
        this.storageManager = storageManager;
        this.courses = new LinkedHashMap<>(storageManager.loadCourses());
        this.students = new LinkedHashMap<>(storageManager.loadStudents());
        this.enrollments = new ArrayList<>(storageManager.loadEnrollments());

        synchronizeStudentEnrollments();
    }

    /**
     * Populates each student's registeredCourses collection from the active enrollment records.
     */
    private void synchronizeStudentEnrollments() {
        // Clear transient in-memory registrations first
        for (Student student : students.values()) {
            student.clearRegisteredCourses();
        }

        // Attach persisted enrollments
        for (EnrollmentRecord record : enrollments) {
            Student student = students.get(record.getStudentId().toUpperCase());
            if (student != null && courses.containsKey(record.getCourseCode().toUpperCase())) {
                if (!student.isRegistered(record.getCourseCode())) {
                    student.addRegisteredCourse(record.getCourseCode());
                }
            }
        }
    }

    /**
     * Enrolls a student into a specified course, validating all university business rules.
     *
     * Rule 1 (Prerequisites): Throws PrerequisiteNotMetException if prerequisite course is not completed.
     * Rule 2 (Capacity): Throws CourseFullException if enrolledCount >= capacity.
     * Rule 3 (Credit Limit): Throws CreditLimitExceededException if current credits + course credits > maxCredits.
     *
     * @param studentId  Identifier of the enrolling student
     * @param courseCode Catalog code of the target course
     * @throws PrerequisiteNotMetException    if prerequisite course is incomplete
     * @throws CourseFullException            if course is full
     * @throws CreditLimitExceededException   if semester credit ceiling would be exceeded
     * @throws IllegalArgumentException       if student/course does not exist or duplicate registration
     * @throws IllegalStateException          if student has already completed this course
     */
    public synchronized void enrollStudent(String studentId, String courseCode)
            throws PrerequisiteNotMetException, CourseFullException, CreditLimitExceededException {

        if (studentId == null || studentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Student ID cannot be null or empty.");
        }
        if (courseCode == null || courseCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Course code cannot be null or empty.");
        }

        String normStudentId = studentId.trim().toUpperCase();
        String normCourseCode = courseCode.trim().toUpperCase();

        Student student = students.get(normStudentId);
        if (student == null) {
            throw new IllegalArgumentException("Student record not found for ID: " + normStudentId);
        }

        Course course = courses.get(normCourseCode);
        if (course == null) {
            throw new IllegalArgumentException("Course record not found for code: " + normCourseCode);
        }

        // Duplicate registration check
        if (student.isRegistered(normCourseCode)) {
            throw new IllegalArgumentException(String.format(
                    "Registration conflict: Student %s is already registered for %s.", normStudentId, normCourseCode));
        }

        // Academic history check: cannot retake already completed course
        if (student.hasCompleted(normCourseCode)) {
            throw new IllegalStateException(String.format(
                    "Academic policy: Student %s has already completed and received credit for course %s.",
                    normStudentId, normCourseCode));
        }

        // RULE 1: Prerequisite Verification
        if (course.hasPrerequisite()) {
            String requiredPrereq = course.getPrerequisiteCode();
            if (!student.hasCompleted(requiredPrereq)) {
                throw new PrerequisiteNotMetException(normStudentId, normCourseCode, requiredPrereq);
            }
        }

        // RULE 2: Course Capacity Check
        if (course.isFull()) {
            throw new CourseFullException(normCourseCode, course.getEnrolledCount(), course.getCapacity());
        }

        // RULE 3: Semester Credit Limit Calculation
        int currentCredits = calculateStudentCredits(normStudentId);
        int attemptedCredits = course.getCredits();
        int maxCredits = student.getMaxCredits();

        if (currentCredits + attemptedCredits > maxCredits) {
            throw new CreditLimitExceededException(normStudentId, currentCredits, attemptedCredits, maxCredits);
        }

        // Apply state changes atomically
        course.incrementEnrollment();
        student.addRegisteredCourse(normCourseCode);
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        enrollments.add(new EnrollmentRecord(normStudentId, normCourseCode, timestamp));

        // Persist updated records to disk
        storageManager.saveAll(courses, students, enrollments);
    }

    /**
     * Drops an active course registration for a student.
     * Decrements the course enrollment count and frees student credit load.
     *
     * @param studentId  Identifier of the student
     * @param courseCode Catalog code of the course to drop
     * @throws IllegalArgumentException if student or course is invalid, or student is not enrolled
     */
    public synchronized void dropCourse(String studentId, String courseCode) {
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Student ID cannot be null or empty.");
        }
        if (courseCode == null || courseCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Course code cannot be null or empty.");
        }

        String normStudentId = studentId.trim().toUpperCase();
        String normCourseCode = courseCode.trim().toUpperCase();

        Student student = students.get(normStudentId);
        if (student == null) {
            throw new IllegalArgumentException("Student record not found for ID: " + normStudentId);
        }

        Course course = courses.get(normCourseCode);
        if (course == null) {
            throw new IllegalArgumentException("Course record not found for code: " + normCourseCode);
        }

        if (!student.isRegistered(normCourseCode)) {
            throw new IllegalArgumentException(String.format(
                    "Drop failed: Student %s is not registered for course %s.", normStudentId, normCourseCode));
        }

        // Apply changes
        student.removeRegisteredCourse(normCourseCode);
        course.decrementEnrollment();

        // Remove from persistent enrollment records
        enrollments.removeIf(record ->
                record.getStudentId().equalsIgnoreCase(normStudentId) &&
                record.getCourseCode().equalsIgnoreCase(normCourseCode));

        // Persist
        storageManager.saveAll(courses, students, enrollments);
    }

    /**
     * Calculates the total sum of credit hours for all courses currently registered by a student.
     *
     * @param studentId Identifier of the student
     * @return Sum of credit hours
     */
    public int calculateStudentCredits(String studentId) {
        Student student = students.get(studentId.trim().toUpperCase());
        if (student == null) {
            return 0;
        }

        int totalCredits = 0;
        for (String courseCode : student.getRegisteredCourses()) {
            Course c = courses.get(courseCode.toUpperCase());
            if (c != null) {
                totalCredits += c.getCredits();
            }
        }
        return totalCredits;
    }

    /**
     * Renders a custom 20-character ASCII gauge showing credit utilization percentage
     * against the maximum semester credit allowance.
     *
     * Format example: [######              ] 37.5% (6/16 credits)
     *
     * @param studentId Identifier of the student
     * @return ASCII gauge string
     */
    public String renderCreditUtilizationGauge(String studentId) {
        return renderCreditUtilizationGauge(studentId, ASCII_GAUGE_WIDTH);
    }

    /**
     * Renders an ASCII gauge with configurable total character width.
     *
     * @param studentId  Identifier of the student
     * @param gaugeWidth Total character width of the inner gauge bar
     * @return Formatted ASCII gauge string with percentage and numeric fraction
     */
    public String renderCreditUtilizationGauge(String studentId, int gaugeWidth) {
        Student student = students.get(studentId.trim().toUpperCase());
        if (student == null) {
            return "[Invalid Student ID]";
        }

        int usedCredits = calculateStudentCredits(studentId);
        int maxCredits = student.getMaxCredits();
        if (maxCredits <= 0) {
            maxCredits = 1;
        }

        double ratio = (double) usedCredits / (double) maxCredits;
        double percentage = ratio * 100.0;

        int filledChars = (int) Math.round(ratio * gaugeWidth);
        if (filledChars > gaugeWidth) {
            filledChars = gaugeWidth;
        }
        if (filledChars < 0) {
            filledChars = 0;
        }
        int emptyChars = gaugeWidth - filledChars;

        StringBuilder bar = new StringBuilder();
        bar.append("[");
        for (int i = 0; i < filledChars; i++) {
            bar.append("#");
        }
        for (int i = 0; i < emptyChars; i++) {
            bar.append(" ");
        }
        bar.append("] ");
        bar.append(String.format(Locale.US, "%5.1f%% (%d/%d credits)", percentage, usedCredits, maxCredits));

        return bar.toString();
    }

    /**
     * Returns an unmodifiable list of all courses in the catalog.
     */
    public List<Course> getAllCourses() {
        return Collections.unmodifiableList(new ArrayList<>(courses.values()));
    }

    /**
     * Finds a course by code.
     */
    public Course getCourse(String courseCode) {
        if (courseCode == null) return null;
        return courses.get(courseCode.trim().toUpperCase());
    }

    /**
     * Returns an unmodifiable list of all registered students.
     */
    public List<Student> getAllStudents() {
        return Collections.unmodifiableList(new ArrayList<>(students.values()));
    }

    /**
     * Finds a student by identifier.
     */
    public Student getStudent(String studentId) {
        if (studentId == null) return null;
        return students.get(studentId.trim().toUpperCase());
    }

    /**
     * Returns a list of Course objects for which the student is currently registered.
     */
    public List<Course> getRegisteredCoursesForStudent(String studentId) {
        Student student = getStudent(studentId);
        if (student == null) {
            return Collections.emptyList();
        }

        List<Course> result = new ArrayList<>();
        for (String code : student.getRegisteredCourses()) {
            Course c = courses.get(code.toUpperCase());
            if (c != null) {
                result.add(c);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Diagnostic helper to reload state from storage if needed.
     */
    public synchronized void reloadFromStorage() {
        courses.clear();
        courses.putAll(storageManager.loadCourses());
        students.clear();
        students.putAll(storageManager.loadStudents());
        enrollments.clear();
        enrollments.addAll(storageManager.loadEnrollments());
        synchronizeStudentEnrollments();
    }
}
