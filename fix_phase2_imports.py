#!/usr/bin/env python3
import re
import sys

files = [
    "src/main/java/com/heronix/scheduler/controller/TeacherAvailabilityDialogController.java",
    "src/main/java/com/heronix/scheduler/controller/ui/dialogs/OddEvenDayAssignmentDialogController.java",
    "src/main/java/com/heronix/scheduler/controller/ui/DragDropScheduleEditorController.java",
    "src/main/java/com/heronix/scheduler/controller/ui/DutyRosterController.java",
    "src/main/java/com/heronix/scheduler/controller/ui/ScheduleViewController.java",
    "src/main/java/com/heronix/scheduler/controller/ui/SpecialDutyRosterController.java",
    "src/main/java/com/heronix/scheduler/controller/ui/TeacherLoadHeatmapController.java",
    "src/main/java/com/heronix/scheduler/service/analysis/ResourceCapacityAnalyzer.java",
    "src/main/java/com/heronix/scheduler/service/impl/ComplianceValidationService.java",
    "src/main/java/com/heronix/scheduler/service/impl/ConflictMatrixServiceImpl.java",
    "src/main/java/com/heronix/scheduler/service/impl/DutyRosterServiceImpl.java",
    "src/main/java/com/heronix/scheduler/service/impl/OptimizationServiceImpl.java",
    "src/main/java/com/heronix/scheduler/service/impl/SmartCourseAssignmentService.java",
    "src/main/java/com/heronix/scheduler/service/IntelligentTeacherAssignmentService.java",
    "src/main/java/com/heronix/scheduler/service/RotationScheduleService.java",
    "src/main/java/com/heronix/scheduler/service/SmartRoomAssignmentService.java",
    "src/main/java/com/heronix/scheduler/service/SmartTeacherAssignmentService.java",
]

for filepath in files:
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Add SISDataService import if not present
        if 'import com.heronix.scheduler.service.data.SISDataService;' not in content:
            # Find the last import statement
            import_pattern = r'(import [^;]+;)\n(?!import)'
            match = re.search(import_pattern, content)
            if match:
                insert_pos = match.end()
                content = content[:insert_pos] + '\nimport com.heronix.scheduler.service.data.SISDataService;' + content[insert_pos:]
        
        # Replace repository field declarations
        # Pattern: @Autowired\n    private TeacherRepository teacherRepository;
        content = re.sub(
            r'@Autowired\s+private\s+TeacherRepository\s+teacherRepository;',
            '',
            content
        )
        content = re.sub(
            r'@Autowired\s+private\s+CourseRepository\s+courseRepository;',
            '',
            content
        )
        content = re.sub(
            r'@Autowired\s+private\s+StudentRepository\s+studentRepository;',
            '',
            content
        )
        
        # Add sisDataService field if repository fields were present
        if 'private SISDataService sisDataService;' not in content:
            # Find first @Autowired after class declaration
            class_pattern = r'(public class \w+[^{]*\{)\s*\n'
            match = re.search(class_pattern, content)
            if match:
                insert_pos = match.end()
                content = content[:insert_pos] + '\n    @Autowired\n    private SISDataService sisDataService;\n' + content[insert_pos:]
        
        # Remove consecutive blank lines
        content = re.sub(r'\n\s*\n\s*\n', '\n\n', content)
        
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        
        print(f"OK Fixed: {filepath}")
    except Exception as e:
        print(f"ERROR Error fixing {filepath}: {e}")

print("\nDone!")
