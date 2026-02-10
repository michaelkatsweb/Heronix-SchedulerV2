# Heronix AI Scheduling Engine

**Version:** 2.0.0
**Port:** 8090
**AI Engine:** OptaPlanner 9.40.0
**Status:** Production Ready

---

## Overview

Heronix AI Scheduling Engine is a sophisticated school schedule optimization system powered by OptaPlanner constraint solving. It generates optimal schedules by balancing 40+ hard and soft constraints while considering teacher qualifications, room availability, student needs, and administrative requirements.

### Key Features

✅ **AI-Powered Optimization** - OptaPlanner constraint solver with 40+ constraints
✅ **Smart Teacher Assignment** - Considers qualifications, certifications, preferences
✅ **Room Optimization** - Lab requirements, capacity, equipment matching
✅ **Conflict Detection** - Detects and resolves scheduling conflicts
✅ **Lunch Wave Management** - Multiple rotating lunch periods
✅ **Special Education Support** - IEP and 504 plan accommodations
✅ **Schedule Export** - PDF, CSV, Excel, iCalendar formats
✅ **Real-time Metrics** - Optimization quality scores and statistics
✅ **REST API** - Integration with SIS and other systems
✅ **Manual Overrides** - Override AI decisions when needed

---

## Architecture

### Domain Model (24 Entities)

- **Core Scheduling (3):** Schedule, ScheduleSlot, TimeSlot
- **OptaPlanner Planning (2):** SchedulingSolution, PlanningEntityConfig
- **Rooms & Resources (3):** Room, RoomEquipment, RoomZone
- **Constraints & Overrides (5):** Conflict, ConflictResolution, ScheduleOverride, SpecialEvent, ScheduleConstraint
- **Optimization Config (6):** ScheduleParameters, OptimizationResult, ScheduleMetrics, ScheduleHealth, ScheduleVersion, ScheduleTemplate
- **Lunch Management (3):** LunchWave, LunchPeriod, LunchCapacity
- **Supporting (2):** PeriodTimer, RotationSchedule

### Technology Stack

| Component | Technology | Version |
|-----------|------------|---------|
| Framework | Spring Boot | 3.2.0 |
| Language | Java | 21 |
| AI Engine | OptaPlanner | 9.40.0 |
| UI | JavaFX | 21 |
| Database | PostgreSQL / H2 | Latest |
| Build Tool | Maven | 3.8+ |
| API Docs | Springdoc OpenAPI | 2.3.0 |

---

## How It Works

### 1. Data Collection

Scheduler fetches data from Heronix-SIS via REST API:

```
GET /api/students     → All students
GET /api/teachers     → All teachers
GET /api/courses      → All courses
GET /api/enrollments  → Student enrollments
GET /api/lunch-assignments → Lunch wave assignments
```

### 2. Constraint Modeling

OptaPlanner evaluates 40+ constraints:

**Hard Constraints (Must be satisfied):**
- Teacher cannot teach 2 classes at same time
- Room cannot be used by 2 classes simultaneously
- Class enrollment ≤ room capacity
- Lab courses must get lab-type rooms
- Students attend their assigned lunch wave
- Teachers free during their planning period
- Max 3 consecutive periods for teachers
- Max 6 teaching periods per day

**Soft Constraints (Optimization goals):**
- Minimize unassigned slots
- Balance teacher workload
- Prefer qualified/certified teachers
- Minimize teacher gaps (consecutive scheduling)
- Prefer teacher's home room
- Keep same subjects in same rooms
- Minimize student building transitions

### 3. Optimization

OptaPlanner uses local search algorithms:

```
┌──────────────────────────────────────┐
│  Construction Heuristic              │
│  (FIRST_FIT - initial solution)      │
└──────────────────────────────────────┘
              ↓
┌──────────────────────────────────────┐
│  Local Search                        │
│  • Change moves                      │
│  • Swap moves                        │
│  • Pillar swap moves                 │
│  • Late Acceptance (size 400)        │
└──────────────────────────────────────┘
              ↓
┌──────────────────────────────────────┐
│  Best Solution                       │
│  Score: 0hard/-50soft                │
│  (0 violations, optimized)           │
└──────────────────────────────────────┘
```

### 4. Score Calculation

Scores indicate solution quality:

- `0hard/0soft` - **Perfect** (all constraints satisfied)
- `0hard/-50soft` - **Good** (valid, but could optimize)
- `-3hard/-20soft` - **Invalid** (3 hard violations)

---

## Getting Started

### Prerequisites

- Java 21 or higher
- Maven 3.8+
- Heronix-SIS running on port 8080
- PostgreSQL 13+ (production) or H2 (development)

### Installation

1. **Clone the repository:**
   ```bash
   cd Heronix-SchedulerV2
   ```

2. **Build the project:**
   ```bash
   mvn clean install
   ```

3. **Ensure SIS is running:**
   ```bash
   # SIS must be running on port 8080
   curl http://localhost:9590/actuator/health
   ```

4. **Run the application:**
   ```bash
   mvn spring-boot:run
   ```

   Or run the JAR:
   ```bash
   java -jar target/heronix-scheduler-2.0.0.jar
   ```

5. **Access the application:**
   - REST API: http://localhost:8090/api
   - Swagger UI: http://localhost:8090/swagger-ui.html
   - H2 Console: http://localhost:8090/h2-console (dev only)
   - Actuator: http://localhost:8090/actuator/health

---

## Configuration

### SIS Integration

Configure connection to Heronix-SIS:

```yaml
heronix:
  scheduler:
    sis:
      api-url: http://localhost:9590/api
      timeout: 30000
      retry-attempts: 3
```

### OptaPlanner Configuration

```yaml
optaplanner:
  solver:
    termination:
      spent-limit: 120s          # 2 minutes max
      unimproved-spent-limit: 30s # Stop if no improvement
      best-score-limit: 0hard/0soft # Perfect score
    move-thread-count: AUTO
    environment-mode: FAST_ASSERT
```

### Constraint Weights

Adjust soft constraint weights:

```yaml
heronix:
  scheduler:
    scheduling:
      constraints:
        teacher-qualification-weight: -10
        teacher-preference-weight: -5
        minimize-gaps-weight: -2
```

---

## REST API Endpoints

### Schedule Generation

```
POST   /api/schedule/generate     - Generate new schedule
GET    /api/schedule/{id}         - Get schedule details
GET    /api/schedule/{id}/slots   - Get all schedule slots
DELETE /api/schedule/{id}         - Delete schedule
GET    /api/schedule/latest       - Get latest schedule
```

### Conflict Management

```
GET    /api/conflicts             - Get all conflicts
GET    /api/conflicts/{id}        - Get conflict details
POST   /api/conflicts/{id}/resolve - Resolve conflict
GET    /api/schedule/{id}/conflicts - Get schedule conflicts
```

### Optimization

```
POST   /api/optimization/start    - Start optimization
POST   /api/optimization/stop     - Stop optimization
GET    /api/optimization/metrics  - Get optimization metrics
GET    /api/optimization/status   - Get optimization status
```

### Schedule Export

```
GET    /api/schedule/{id}/export/pdf   - Export as PDF
GET    /api/schedule/{id}/export/csv   - Export as CSV
GET    /api/schedule/{id}/export/excel - Export as Excel
GET    /api/schedule/{id}/export/ical  - Export as iCalendar
```

---

## Schedule Generation Workflow

### Step 1: Prepare Data

```java
// Fetch data from SIS
List<StudentDTO> students = sisApiClient.getAllStudents();
List<TeacherDTO> teachers = sisApiClient.getAllTeachers();
List<CourseDTO> courses = sisApiClient.getAllCourses();
List<EnrollmentDTO> enrollments = sisApiClient.getAllEnrollments();
```

### Step 2: Build Planning Problem

```java
SchedulingSolution solution = new SchedulingSolution();
solution.setStudents(students);
solution.setTeachers(teachers);
solution.setCourses(courses);
solution.setRooms(rooms);
solution.setTimeSlots(timeSlots);
solution.setScheduleSlots(scheduleSlots); // Unassigned initially
```

### Step 3: Solve with OptaPlanner

```java
SolverFactory<SchedulingSolution> solverFactory =
    SolverFactory.create(new SolverConfig()
        .withSolutionClass(SchedulingSolution.class)
        .withEntityClasses(ScheduleSlot.class)
        .withTerminationSpentLimit(Duration.ofSeconds(120)));

Solver<SchedulingSolution> solver = solverFactory.buildSolver();
SchedulingSolution solvedSolution = solver.solve(solution);
```

### Step 4: Extract Results

```java
HardSoftScore score = solvedSolution.getScore();
List<ScheduleSlot> assignedSlots = solvedSolution.getScheduleSlots();

// Save to database
for (ScheduleSlot slot : assignedSlots) {
    scheduleSlotRepository.save(slot);
}
```

---

## Constraints

### Hard Constraints (17 total)

1. `teacherConflict` - No teacher teaches 2 classes at same time
2. `roomConflict` - No 2 classes use same room simultaneously
3. `roomCapacity` - Class enrollment ≤ room capacity
4. `labRequirement` - Lab courses get lab-type rooms
5. `roomTypeMatching` - Subject matched to room type
6. `studentLunchWaveAssignment` - Students attend assigned lunch
7. `teacherLunchWaveAssignment` - Teachers attend assigned lunch
8. `studentFreeDuringLunchWave` - No classes during lunch
9. `teacherFreeDuringLunchWave` - Teachers free during lunch
10. `teacherAvailability` - Teachers only assigned when available
11. `teacherRoomRestrictions` - Respect room restrictions
12. `multiRoomAvailability` - All required rooms available
13. `maxConsecutivePeriodsTeacher` - Max 3 consecutive periods
14. `maxDailyPeriodsTeacher` - Max 6 teaching periods/day
15. `minPlanningPeriods` - Minimum planning time
16. `teacherPlanningPeriodReserved` - No teaching during planning
17. `honorIepAccommodations` - IEP requirements respected

### Soft Constraints (23 total)

1. `minimizeUnassignedSlots` - Assign all slots
2. `balanceTeacherWorkload` - Equalize workload
3. `preferMorningForComplexCourses` - Complex courses morning
4. `minimizeTeacherGaps` - Consecutive scheduling
5. `maximizeRoomUtilization` - Use available rooms
6. `teacherQualification` - Prefer certified teachers (0/10/100)
7. `teacherCoursePreference` - Prefer pre-assigned teachers
8. `teacherCourseLoadBalance` - 3-4 courses per teacher
9. `roomSubjectAffinity` - Keep subjects in same rooms
10. `preferTeacherHomeRoom` - Use teacher's home room
11. `studentPeriodDistribution` - Spread student courses
12. `avoidBackToBackSameSubject` - Variety for students
13. `balanceClassSizes` - Even class distributions
14. `minimizeBuildingTransitions` - Reduce student travel
15. `teacherRoomPreferences` - Honor room preferences
16. `roomEquipmentCompatibility` - Room has required equipment
17. `departmentZonePreference` - Rooms in dept zone
18. `minimizeTeacherTravel` - Minimize building transitions
19. `multiRoomProximity` - Multi-room courses nearby
20. `roomActivityMatching` - PE activities in right rooms
21-23. (Additional optimization constraints)

---

## Development

### Build and Test

```bash
# Build
mvn clean install

# Run tests
mvn test

# Run OptaPlanner tests
mvn test -Dtest=SchedulingConstraintProviderTest

# Benchmark solver
mvn test -Dtest=SolverBenchmarkTest
```

### Constraint Development

Create new constraint in `SchedulingConstraintProvider.java`:

```java
private Constraint myNewConstraint(ConstraintFactory constraintFactory) {
    return constraintFactory
        .forEach(ScheduleSlot.class)
        .filter(slot -> /* condition */)
        .penalize(HardSoftScore.ONE_SOFT)
        .asConstraint("My new constraint");
}
```

---

## Deployment

### Production Deployment

1. **Configure production settings:**
   ```bash
   export DB_USERNAME=scheduler_user
   export DB_PASSWORD=secure_password
   export SIS_API_URL=https://sis.school.edu/api
   ```

2. **Build production JAR:**
   ```bash
   mvn clean package -Pprod
   ```

3. **Run with production profile:**
   ```bash
   java -jar target/heronix-scheduler-2.0.0.jar --spring.profiles.active=prod
   ```

### Docker Deployment

```dockerfile
FROM openjdk:21-jdk-slim
COPY target/heronix-scheduler-2.0.0.jar app.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

```bash
docker build -t heronix-scheduler:2.0.0 .
docker run -p 8090:8090 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SIS_API_URL=http://sis:8080/api \
  heronix-scheduler:2.0.0
```

---

## Monitoring

### OptaPlanner Metrics

```bash
curl http://localhost:8090/actuator/optaplanner
```

### Solver Status

```bash
curl http://localhost:8090/api/optimization/status
```

### Performance Metrics

```bash
curl http://localhost:8090/actuator/metrics/optaplanner.solver.solve.duration
```

---

## Troubleshooting

### Common Issues

**Issue: Cannot connect to SIS**
- Verify SIS is running: `curl http://localhost:9590/actuator/health`
- Check network connectivity
- Review firewall rules
- Check `heronix.scheduler.sis.api-url` configuration

**Issue: Schedule generation timeout**
- Increase `optaplanner.solver.termination.spent-limit`
- Reduce problem size (fewer students/courses)
- Enable incremental solving
- Use faster environment mode

**Issue: Too many conflicts**
- Review hard constraint violations
- Check teacher availability
- Verify room capacity
- Review lunch wave assignments
- Check for data quality issues in SIS

**Issue: Poor schedule quality**
- Adjust soft constraint weights
- Increase solve time
- Enable nearby selection
- Use multi-threading

---

## Support

- **Documentation:** See `/docs` folder
- **API Docs:** http://localhost:8090/swagger-ui.html
- **OptaPlanner Docs:** https://www.optaplanner.org/docs/
- **Issues:** Report to Heronix support

---

## License

© 2025 Heronix Educational Systems LLC. All rights reserved.

---

**Heronix AI Scheduler - Intelligent Scheduling Through Constraint Solving** 🤖📅
