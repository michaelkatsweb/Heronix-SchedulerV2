import re
import os

base_dir = r"H:\Heronix\Heronix-SchedulerV2\src\main\java\com\heronix\scheduler"

fixes = [
    # Fix TimePickerField constructor calls in SpecialDutyRosterController
    {
        "file": os.path.join(base_dir, "controller", "ui", "SpecialDutyRosterController.java"),
        "old": r"TimePickerField startTimeField = new TimePickerField\(LocalTime\.of\(7, 30\)\);",
        "new": "TimePickerField startTimeField = new TimePickerField();\n        startTimeField.setTime(LocalTime.of(7, 30));"
    },
    {
        "file": os.path.join(base_dir, "controller", "ui", "SpecialDutyRosterController.java"),
        "old": r"TimePickerField endTimeField = new TimePickerField\(LocalTime\.of\(8, 0\)\);",
        "new": "TimePickerField endTimeField = new TimePickerField();\n        endTimeField.setTime(LocalTime.of(8, 0));"
    },
    # Fix TimePickerField constructor calls in EventsController
    {
        "file": os.path.join(base_dir, "controller", "ui", "EventsController.java"),
        "old": r"TimePickerField startTimeField = new TimePickerField\(event\.getStartTime\(\)\);",
        "new": "TimePickerField startTimeField = new TimePickerField();\n        startTimeField.setTime(event.getStartTime());"
    },
    {
        "file": os.path.join(base_dir, "controller", "ui", "EventsController.java"),
        "old": r"TimePickerField endTimeField = new TimePickerField\(event\.getEndTime\(\)\);",
        "new": "TimePickerField endTimeField = new TimePickerField();\n        endTimeField.setTime(event.getEndTime());"
    },
    # Replace teacherRepository with sisDataService in SpecialDutyRosterController
    {
        "file": os.path.join(base_dir, "controller", "ui", "SpecialDutyRosterController.java"),
        "old": r"List<Teacher> teachers = teacherRepository\.findByActiveTrue\(\);",
        "new": "List<Teacher> teachers = sisDataService.getAllTeachers();"
    },
    # Replace teacherRepository with sisDataService in ScheduleSlotEditDialogController
    {
        "file": os.path.join(base_dir, "controller", "ui", "ScheduleSlotEditDialogController.java"),
        "old": r"teacherRepository\.findById\(",
        "new": "sisDataService.getTeacherById("
    },
    # Replace courseRepository with sisDataService in ScheduleSlotEditDialogController
    {
        "file": os.path.join(base_dir, "controller", "ui", "ScheduleSlotEditDialogController.java"),
        "old": r"courseRepository\.findById\(",
        "new": "sisDataService.getCourseById("
    },
    # Replace teacherRepository in DutyRosterController
    {
        "file": os.path.join(base_dir, "controller", "ui", "DutyRosterController.java"),
        "old": r"teacherRepository\.",
        "new": "sisDataService."
    },
    # Replace teacherRepository in ComplianceValidationService
    {
        "file": os.path.join(base_dir, "service", "impl", "ComplianceValidationService.java"),
        "old": r"teacherRepository\.",
        "new": "sisDataService."
    },
    # Replace teacherRepository and courseRepository in SmartCourseAssignmentService
    {
        "file": os.path.join(base_dir, "service", "impl", "SmartCourseAssignmentService.java"),
        "old": r"teacherRepository\.",
        "new": "sisDataService."
    },
    {
        "file": os.path.join(base_dir, "service", "impl", "SmartCourseAssignmentService.java"),
        "old": r"courseRepository\.",
        "new": "sisDataService."
    },
]

for fix in fixes:
    file_path = fix["file"]
    if os.path.exists(file_path):
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()

        original = content
        content = re.sub(fix["old"], fix["new"], content)

        if content != original:
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(content)
            print(f"Fixed: {os.path.basename(file_path)}")
        else:
            print(f"No match found in: {os.path.basename(file_path)}")
    else:
        print(f"File not found: {file_path}")
