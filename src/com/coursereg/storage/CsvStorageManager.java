package com.coursereg.storage;

import com.coursereg.model.Course;
import com.coursereg.model.Student;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Storage manager responsible for CSV-based file persistence and initial seeding.
 * Operates purely with java.io (BufferedReader, BufferedWriter) and standard collections,
 * avoiding any third-party libraries or database drivers.
 */
public class CsvStorageManager {
    private final String dataDirectoryPath;
    private final File coursesFile;
    private final File studentsFile;
    private final File enrollmentsFile;

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Immutable record representing a persistent enrollment association.
     */
    public static class EnrollmentRecord {
        private final String studentId;
        private final String courseCode;
        private final String timestamp;

        public EnrollmentRecord(String studentId, String courseCode, String timestamp) {
            this.studentId = studentId.trim().toUpperCase();
            this.courseCode = courseCode.trim().toUpperCase();
            this.timestamp = (timestamp == null || timestamp.trim().isEmpty())
                    ? LocalDateTime.now().format(TIMESTAMP_FORMATTER)
                    : timestamp.trim();
        }

        public String getStudentId() {
            return studentId;
        }

        public String getCourseCode() {
            return courseCode;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public String toCsv() {
            return String.format("%s,%s,%s", studentId, courseCode, timestamp);
        }

        public static EnrollmentRecord fromCsv(String csvLine) {
            String[] parts = csvLine.split(",", -1);
            if (parts.length < 2) {
                throw new IllegalArgumentException("Malformed enrollment CSV line: " + csvLine);
            }
            String studentId = parts[0].trim();
            String courseCode = parts[1].trim();
            String timestamp = parts.length >= 3 ? parts[2].trim() : LocalDateTime.now().format(TIMESTAMP_FORMATTER);
            return new EnrollmentRecord(studentId, courseCode, timestamp);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EnrollmentRecord that = (EnrollmentRecord) o;
            return Objects.equals(studentId, that.studentId) &&
                   Objects.equals(courseCode, that.courseCode);
        }

        @Override
        public int hashCode() {
            return Objects.hash(studentId, courseCode);
        }

        @Override
        public String toString() {
            return String.format("%s enrolled in %s at %s", studentId, courseCode, timestamp);
        }
    }

    /**
     * Default constructor targeting standard relative "data" directory.
     */
    public CsvStorageManager() {
        this("data");
    }

    /**
     * Constructor allowing configurable storage root directory (helpful for testing).
     *
     * @param dataDirectoryPath Path to directory holding CSV data files
     */
    public CsvStorageManager(String dataDirectoryPath) {
        this.dataDirectoryPath = dataDirectoryPath;
        File dir = new File(dataDirectoryPath);
        this.coursesFile = new File(dir, "courses.csv");
        this.studentsFile = new File(dir, "students.csv");
        this.enrollmentsFile = new File(dir, "enrollments.csv");
        initializeStorage();
    }

    /**
     * Ensures the data directory exists and pre-seeds initial academic datasets
     * if the files are not already present.
     */
    public final void initializeStorage() {
        File dir = new File(dataDirectoryPath);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created && !dir.exists()) {
                System.err.println("Warning: Unable to create storage directory: " + dir.getAbsolutePath());
            }
        }

        if (!coursesFile.exists() || coursesFile.length() == 0) {
            seedDefaultCourses();
        }
        if (!studentsFile.exists() || studentsFile.length() == 0) {
            seedDefaultStudents();
        }
        if (!enrollmentsFile.exists() || enrollmentsFile.length() == 0) {
            seedDefaultEnrollments();
        }
    }

    private void seedDefaultCourses() {
        List<Course> defaultCourses = new ArrayList<>();
        // Rule 2 test scenario: CS101 is at full capacity (3/3)
        defaultCourses.add(new Course("CS101", "Introduction to Computer Science", 4, 3, 3, Course.NO_PREREQUISITE));
        // Rule 1 test scenario: CS201 requires CS101, currently has 1 active enrollment (Alex Johnson)
        defaultCourses.add(new Course("CS201", "Data Structures and Algorithms", 4, 30, 1, "CS101"));
        defaultCourses.add(new Course("CS202", "Database Systems", 3, 25, 0, "CS101"));
        // Rule 1 test scenario: CS301 requires CS201
        defaultCourses.add(new Course("CS301", "Operating Systems", 4, 20, 0, "CS201"));
        // Rule 1 test scenario: CS401 requires CS201
        defaultCourses.add(new Course("CS401", "Advanced Artificial Intelligence", 3, 15, 0, "CS201"));
        // General electives with no prerequisite
        defaultCourses.add(new Course("MATH101", "Calculus I", 4, 40, 0, Course.NO_PREREQUISITE));
        defaultCourses.add(new Course("PHYS101", "General Physics", 4, 30, 0, Course.NO_PREREQUISITE));
        defaultCourses.add(new Course("HIST101", "World History", 3, 30, 0, Course.NO_PREREQUISITE));
        defaultCourses.add(new Course("ENG101", "Technical Communication", 2, 30, 0, Course.NO_PREREQUISITE));

        saveCoursesList(defaultCourses);
    }

    private void seedDefaultStudents() {
        List<Student> defaultStudents = new ArrayList<>();
        // Primary evaluation student: Alex Johnson, max 16 credits, pre-completed CS101
        List<String> alexCompleted = new ArrayList<>();
        alexCompleted.add("CS101");
        defaultStudents.add(new Student("STU1001", "Alex Johnson", "alex.johnson@university.edu", 16, alexCompleted));

        // Secondary student: Maria Garcia, max 18 credits, completed CS101 and MATH101
        List<String> mariaCompleted = new ArrayList<>();
        mariaCompleted.add("CS101");
        mariaCompleted.add("MATH101");
        defaultStudents.add(new Student("STU1002", "Maria Garcia", "maria.garcia@university.edu", 18, mariaCompleted));

        // Freshman student: Liam Patel, max 12 credits, no completed courses
        defaultStudents.add(new Student("STU1003", "Liam Patel", "liam.patel@university.edu", 12, new ArrayList<>()));

        saveStudentsList(defaultStudents);
    }

    private void seedDefaultEnrollments() {
        List<EnrollmentRecord> defaultEnrollments = new ArrayList<>();
        // Three enrollments fill CS101 to capacity (3/3)
        defaultEnrollments.add(new EnrollmentRecord("STU9001", "CS101", "2026-09-01 09:00:00"));
        defaultEnrollments.add(new EnrollmentRecord("STU9002", "CS101", "2026-09-01 09:15:00"));
        defaultEnrollments.add(new EnrollmentRecord("STU9003", "CS101", "2026-09-01 09:30:00"));

        // Active enrollment for Alex Johnson in CS201 (4 credits)
        defaultEnrollments.add(new EnrollmentRecord("STU1001", "CS201", "2026-09-05 10:00:00"));

        saveEnrollmentsList(defaultEnrollments);
    }

    /**
     * Loads all course records from courses.csv into an ordered map.
     *
     * @return Map of upper-case course code to Course instance
     */
    public Map<String, Course> loadCourses() {
        Map<String, Course> courses = new LinkedHashMap<>();
        if (!coursesFile.exists()) {
            return courses;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(coursesFile))) {
            String line;
            boolean isHeader = true;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (isHeader) {
                    isHeader = false;
                    // Skip header row if matches column name
                    if (line.toLowerCase().startsWith("coursecode")) {
                        continue;
                    }
                }
                try {
                    Course course = Course.fromCsv(line);
                    courses.put(course.getCourseCode().toUpperCase(), course);
                } catch (IllegalArgumentException e) {
                    System.err.println("Skipping malformed course row: " + line + " (" + e.getMessage() + ")");
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading courses file: " + e.getMessage());
        }
        return courses;
    }

    /**
     * Loads all student records from students.csv into an ordered map.
     *
     * @return Map of upper-case student ID to Student instance
     */
    public Map<String, Student> loadStudents() {
        Map<String, Student> students = new LinkedHashMap<>();
        if (!studentsFile.exists()) {
            return students;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(studentsFile))) {
            String line;
            boolean isHeader = true;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (isHeader) {
                    isHeader = false;
                    if (line.toLowerCase().startsWith("id")) {
                        continue;
                    }
                }
                try {
                    Student student = Student.fromCsv(line);
                    students.put(student.getId().toUpperCase(), student);
                } catch (IllegalArgumentException e) {
                    System.err.println("Skipping malformed student row: " + line + " (" + e.getMessage() + ")");
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading students file: " + e.getMessage());
        }
        return students;
    }

    /**
     * Loads all active enrollment records from enrollments.csv.
     *
     * @return List of EnrollmentRecord instances
     */
    public List<EnrollmentRecord> loadEnrollments() {
        List<EnrollmentRecord> enrollments = new ArrayList<>();
        if (!enrollmentsFile.exists()) {
            return enrollments;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(enrollmentsFile))) {
            String line;
            boolean isHeader = true;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (isHeader) {
                    isHeader = false;
                    if (line.toLowerCase().startsWith("studentid")) {
                        continue;
                    }
                }
                try {
                    EnrollmentRecord record = EnrollmentRecord.fromCsv(line);
                    enrollments.add(record);
                } catch (IllegalArgumentException e) {
                    System.err.println("Skipping malformed enrollment row: " + line + " (" + e.getMessage() + ")");
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading enrollments file: " + e.getMessage());
        }
        return enrollments;
    }

    /**
     * Persists the given course mappings back to courses.csv.
     *
     * @param courses Map of course codes to Course models
     */
    public synchronized void saveCourses(Map<String, Course> courses) {
        saveCoursesList(new ArrayList<>(courses.values()));
    }

    private synchronized void saveCoursesList(List<Course> courses) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(coursesFile))) {
            writer.write("courseCode,title,credits,capacity,enrolledCount,prerequisiteCode");
            writer.newLine();
            for (Course course : courses) {
                writer.write(course.toCsv());
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error writing courses file: " + e.getMessage());
        }
    }

    /**
     * Persists the given student mappings back to students.csv.
     *
     * @param students Map of student IDs to Student models
     */
    public synchronized void saveStudents(Map<String, Student> students) {
        saveStudentsList(new ArrayList<>(students.values()));
    }

    private synchronized void saveStudentsList(List<Student> students) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(studentsFile))) {
            writer.write("id,name,email,maxCredits,completedCourses");
            writer.newLine();
            for (Student student : students) {
                writer.write(student.toCsv());
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error writing students file: " + e.getMessage());
        }
    }

    /**
     * Persists the enrollment records back to enrollments.csv.
     *
     * @param enrollments List of active enrollment records
     */
    public synchronized void saveEnrollmentsList(List<EnrollmentRecord> enrollments) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(enrollmentsFile))) {
            writer.write("studentId,courseCode,enrollmentTimestamp");
            writer.newLine();
            for (EnrollmentRecord record : enrollments) {
                writer.write(record.toCsv());
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error writing enrollments file: " + e.getMessage());
        }
    }

    /**
     * Atomically saves all data tables in one coordinated flush.
     *
     * @param courses     Map of courses
     * @param students    Map of students
     * @param enrollments List of enrollments
     */
    public synchronized void saveAll(Map<String, Course> courses, Map<String, Student> students, List<EnrollmentRecord> enrollments) {
        saveCourses(courses);
        saveStudents(students);
        saveEnrollmentsList(enrollments);
    }
}
