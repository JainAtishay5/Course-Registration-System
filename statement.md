# Academic Project Statement: University Course Registration System

## 1. Executive Summary

The University Course Registration System is a modular, production-ready, command-line academic management platform developed strictly in pure Core Java (JDK 17+) without external dependencies, build plugins, or external database drivers. The system enforces strict institutional validation rules—including course prerequisite verification, real-time section seat capacity checking, and student credit limit ceilings—while maintaining state across sessions using a robust CSV-based file storage mechanism.

---

## 2. Problem Statement

University course enrollment is a critical administrative phase characterized by rigorous academic regulations and resource constraints. In manual or poorly coordinated systems, institutions face several failure modes:
1. **Prerequisite Violations**: Students enroll in advanced coursework without completing prerequisite foundational courses, leading to high drop-out and failure rates.
2. **Class Over-Enrollment**: Inadequate concurrency and capacity management leads to lecture halls and lab sections exceeding fire-code and pedagogical limits.
3. **Credit Limit Violations**: Students over-register beyond their semester credit limit allowance, risking burnout and violating university accreditation guidelines.
4. **Lack of Transparent Credit Analytics**: Students often struggle to calculate their active course load and remaining allowable credits during peak registration.
5. **System Fragility & Dependency Bloat**: Heavy enterprise systems frequently suffer from brittle dependencies, cumbersome database drivers, and complex deployment pipelines that make local evaluation and auditing difficult.

This project addresses these challenges by delivering an auditable, lightweight, and mathematically verified course registration engine built entirely on the Java Standard Library (`java.io`, `java.util`).

---

## 3. Scope of the System

### 3.1 In-Scope (Functional Deliverables)
- **Course Offering Catalog**: Browsing scheduled courses with real-time seat availability, credit values, and prerequisite requirements.
- **Rule-Based Enrollment Engine**:
  - **Prerequisite Enforcement (Rule 1)**: Checked exception (`PrerequisiteNotMetException`) triggered if prerequisites are missing.
  - **Seat Capacity Enforcement (Rule 2)**: Checked exception (`CourseFullException`) triggered if enrolled count reaches capacity.
  - **Semester Credit Ceiling (Rule 3)**: Checked exception (`CreditLimitExceededException`) triggered if course credits exceed allowed limit.
- **Terminal Visual Analytics**: Real-time 20-character ASCII gauge displaying credit utilization percentage against the student's maximum semester credit allowance.
- **Course Drop & Seat Reallocation**: Dynamic dropping of courses with automatic seat count restoration and credit recalculation.
- **Multi-Profile Academic Switching**: Ability to simulate registration scenarios across multiple student profiles with varying credit limits and transcripts.
- **Relational CSV File Persistence**: Idempotent auto-seeding and persistent synchronization across `courses.csv`, `students.csv`, and `enrollments.csv`.
- **Integrated Automated Diagnostics**: Headless self-test runner to verify exception rules and data invariants directly from the CLI.

### 3.2 Non-Functional Requirements
- **Standard Library Compliance**: Zero third-party dependencies (no Maven, Gradle, Guava, Apache Commons, or JDBC).
- **Execution Portability**: Direct compilation via `javac` and execution via `java` on any standard JDK 17+ environment (Windows, Linux, macOS).
- **Data Integrity & Consistency**: Synchronized read/write operations ensuring that seat counts and student enrollment records remain perfectly balanced across files.
- **Defensive CLI UX**: Scanner buffer flush handling, prevention of newline skipping bugs, and NumberFormatException trapping.

---

## 4. Target Users & Stakeholders

| User Role | Responsibilities & Value Delivered |
| :--- | :--- |
| **Undergraduate / Graduate Students** | Self-serve course discovery, immediate feedback on prerequisite eligibility, active credit visualization, and schedule adjustments. |
| **Academic Advisors** | Oversight of student credit limits, verification that academic progression complies with institutional prerequisite trees. |
| **University Registrars** | Enforcement of strict class capacity ceilings, auditing of enrollment timestamps, and validation of course rosters. |
| **Academic Evaluators / Instructors** | Direct inspection of object-oriented design patterns, checked exception hierarchies, and clean standard library I/O. |

---

## 5. High-Level Features

1. **Catalog Exploration**: Tabular view of course code, title, credits, occupied/total seats, prerequisites, and dynamic status (`OPEN`, `FULL`, `ENROLLED`).
2. **Prerequisite Tree Verification**: Strict verification against student completed academic history before permitting enrollment.
3. **Capacity & Waitlist Defense**: Atomic seat count increments and decrements, preventing registration when capacity is saturated.
4. **Credit Allocation & Overload Defense**: Accurate aggregation of current semester credits against individual student limits (`maxCredits`).
5. **ASCII Terminal Analytics**: 20-character visual progress bar (`[#####               ]  25.0% (4/16 credits)`) with instant recalculation upon registration or drop.
6. **Robust File Storage Subsystem**: Automatic directory creation (`data/`), auto-seeding of missing files, and standardized CSV serialization/deserialization.
7. **Automated Diagnostic Suite**: Built-in verification option that automatically exercises each checked exception and asserts expected outcomes.

---

## 6. Architecture & Package Structure

```
src/
└── com/
    └── coursereg/
        ├── model/
        │   ├── Person.java                  # Abstract base class (id, name, email, abstract getRole())
        │   ├── Student.java                 # Extends Person (maxCredits, registeredCourses, completedCourses, CSV serialization)
        │   └── Course.java                  # Course offering (code, title, credits, capacity, enrolledCount, prereq)
        ├── exception/
        │   ├── CourseFullException.java     # Checked Exception for capacity saturation
        │   ├── CreditLimitExceededException.java # Checked Exception for semester credit ceiling violations
        │   └── PrerequisiteNotMetException.java  # Checked Exception for unfulfilled prerequisite requirements
        ├── storage/
        │   └── CsvStorageManager.java       # Pure java.io storage engine (courses.csv, students.csv, enrollments.csv)
        ├── service/
        │   └── RegistrationService.java     # Core business logic, rule validation, and ASCII gauge computation
        └── Main.java                        # Interactive CLI loop, Scanner handling, NumberFormatException trapping
```

---

## 7. Business Rules & Validation Invariants

- **Invariant 1 (Prerequisite Met)**:
  $$\forall s \in \text{Students}, c \in \text{Courses}: \text{Enroll}(s, c) \implies (c.\text{prereq} = \text{"NONE"} \lor c.\text{prereq} \in s.\text{completedCourses})$$
  *Violation*: Throws `PrerequisiteNotMetException`.

- **Invariant 2 (Capacity Maintained)**:
  $$\forall c \in \text{Courses}: c.\text{enrolledCount} < c.\text{capacity}$$
  *Violation*: Throws `CourseFullException`.

- **Invariant 3 (Credit Ceiling Respected)**:
  $$\forall s \in \text{Students}, c \in \text{Courses}: \text{TotalCredits}(s) + c.\text{credits} \le s.\text{maxCredits}$$
  *Violation*: Throws `CreditLimitExceededException`.

- **Invariant 4 (No Duplicate Active Registration)**:
  $$c.\text{courseCode} \notin s.\text{registeredCourses}$$

- **Invariant 5 (No Retake of Completed Credits)**:
  $$c.\text{courseCode} \notin s.\text{completedCourses}$$
