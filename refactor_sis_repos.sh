#!/bin/bash

# List of files to refactor
files=(
  "src/main/java/com/heronix/scheduler/controller/TeacherAvailabilityDialogController.java"
  "src/main/java/com/heronix/scheduler/controller/ui/dialogs/OddEvenDayAssignmentDialogController.java"
  "src/main/java/com/heronix/scheduler/controller/ui/DragDropScheduleEditorController.java"
  "src/main/java/com/heronix/scheduler/controller/ui/DutyRosterController.java"
  "src/main/java/com/heronix/scheduler/controller/ui/ScheduleViewController.java"
  "src/main/java/com/heronix/scheduler/controller/ui/SpecialDutyRosterController.java"
  "src/main/java/com/heronix/scheduler/controller/ui/TeacherLoadHeatmapController.java"
  "src/main/java/com/heronix/scheduler/service/analysis/ResourceCapacityAnalyzer.java"
  "src/main/java/com/heronix/scheduler/service/impl/ComplianceValidationService.java"
  "src/main/java/com/heronix/scheduler/service/impl/ConflictMatrixServiceImpl.java"
  "src/main/java/com/heronix/scheduler/service/impl/DutyRosterServiceImpl.java"
  "src/main/java/com/heronix/scheduler/service/impl/OptimizationServiceImpl.java"
  "src/main/java/com/heronix/scheduler/service/impl/SmartCourseAssignmentService.java"
  "src/main/java/com/heronix/scheduler/service/IntelligentCourseAssignmentService.java"
  "src/main/java/com/heronix/scheduler/service/IntelligentTeacherAssignmentService.java"
  "src/main/java/com/heronix/scheduler/service/RotationScheduleService.java"
  "src/main/java/com/heronix/scheduler/service/SmartRoomAssignmentService.java"
  "src/main/java/com/heronix/scheduler/service/SmartTeacherAssignmentService.java"
)

for file in "${files[@]}"; do
  if [ -f "$file" ]; then
    echo "Processing: $file"
    
    # Replace common repository method calls
    sed -i 's/teacherRepository\.findAll()/sisDataService.getAllTeachers()/g' "$file"
    sed -i 's/teacherRepository\.findAllActive()/sisDataService.getAllTeachers()/g' "$file"
    sed -i 's/teacherRepository\.findById(\([^)]*\))/sisDataService.getTeacherById(\1)/g' "$file"
    
    sed -i 's/courseRepository\.findAll()/sisDataService.getAllCourses()/g' "$file"
    sed -i 's/courseRepository\.findByActiveTrue()/sisDataService.getAllCourses().stream().filter(Course::getActive).toList()/g' "$file"
    sed -i 's/courseRepository\.findById(\([^)]*\))/sisDataService.getCourseById(\1)/g' "$file"
    
    sed -i 's/studentRepository\.findAll()/sisDataService.getAllStudents()/g' "$file"
    sed -i 's/studentRepository\.findById(\([^)]*\))/sisDataService.getStudentById(\1)/g' "$file"
    
  fi
done

echo "Refactoring complete!"
