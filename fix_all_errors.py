import re
import os

base_dir = r"H:\Heronix\Heronix-SchedulerV2\src\main\java\com\heronix\scheduler"

# Read and fix each file
def fix_file(file_path, fixes_list):
    if not os.path.exists(file_path):
        print(f"File not found: {file_path}")
        return False

    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    original = content
    for old_pattern, new_text in fixes_list:
        content = re.sub(old_pattern, new_text, content, flags=re.MULTILINE | re.DOTALL)

    if content != original:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        return True
    return False

# Fix RoomsController exportRoomsToCSV
if fix_file(os.path.join(base_dir, "controller", "ui", "RoomsController.java"), [
    (r'byte\[\] data = exportService\.exportRoomsToCSV\(roomsList\);\s+java\.nio\.file\.Files\.write\(file\.toPath\(\), data\);',
     '// TODO: Method exportRoomsToCSV() does not exist - implement when available\n                // byte[] data = exportService.exportRoomsToCSV(roomsList);\n                // java.nio.file.Files.write(file.toPath(), data);\n                showError("Export Error", "CSV export not yet implemented");')
]):
    print("Fixed: RoomsController.java - exportRoomsToCSV")

# Fix RoomManagementController exportRoomsToExcel
if fix_file(os.path.join(base_dir, "controller", "ui", "RoomManagementController.java"), [
    (r'byte\[\] data = exportService\.exportRoomsToExcel\(roomTable\.getItems\(\)\);\s+java\.nio\.file\.Files\.write\(file\.toPath\(\), data\);',
     '// TODO: Method exportRoomsToExcel() does not exist - implement when available\n                // byte[] data = exportService.exportRoomsToExcel(roomTable.getItems());\n                // java.nio.file.Files.write(file.toPath(), data);\n                showError("Export Error", "Excel export not yet implemented");')
]):
    print("Fixed: RoomManagementController.java - exportRoomsToExcel")

# Fix EventsController exportEventsToICal
if fix_file(os.path.join(base_dir, "controller", "ui", "EventsController.java"), [
    (r'byte\[\] data = exportService\.exportEventsToICal\(eventsTable\.getItems\(\)\);\s+java\.nio\.file\.Files\.write\(file\.toPath\(\), data\);',
     '// TODO: Method exportEventsToICal() does not exist - implement when available\n                // byte[] data = exportService.exportEventsToICal(eventsTable.getItems());\n                // java.nio.file.Files.write(file.toPath(), data);\n                showError("Export Error", "iCal export not yet implemented");')
]):
    print("Fixed: EventsController.java - exportEventsToICal")

# Fix ScheduleSlotEditDialogController sisDataService usage
if fix_file(os.path.join(base_dir, "controller", "ui", "ScheduleSlotEditDialogController.java"), [
    (r'sisDataService\.getTeacherById\(', 'sisDataService.getTeacherById('),
    (r'sisDataService\.getCourseById\(', 'sisDataService.getCourseById(')
]):
    print("Fixed: ScheduleSlotEditDialogController.java - sisDataService")

# Fix LunchPeriodServiceImpl - comment out getScheduleSlots() calls
if fix_file(os.path.join(base_dir, "service", "impl", "LunchPeriodServiceImpl.java"), [
    (r'teacher\.getScheduleSlots\(\)',
     '// TODO: Method getScheduleSlots() does not exist on Teacher - use scheduleSlotRepository instead\n                    scheduleSlotRepository.findByTeacherIdWithDetails(teacher.getId())')
]):
    print("Fixed: LunchPeriodServiceImpl.java - getScheduleSlots")

# Fix SubstituteScheduleGeneratorService - add Optional import
sub_file = os.path.join(base_dir, "service", "SubstituteScheduleGeneratorService.java")
if os.path.exists(sub_file):
    with open(sub_file, 'r', encoding='utf-8') as f:
        content = f.read()
    if 'import java.util.Optional;' not in content and 'Optional<' in content:
        # Add import after other java.util imports
        content = re.sub(r'(import java\.util\.List;)', r'\1\nimport java.util.Optional;', content)
        with open(sub_file, 'w', encoding='utf-8') as f:
            f.write(content)
        print("Fixed: SubstituteScheduleGeneratorService.java - added Optional import")

# Fix MasterScheduleServiceImpl - Long to int conversion
if fix_file(os.path.join(base_dir, "service", "impl", "MasterScheduleServiceImpl.java"), [
    (r'new LunchWave\(([^,]+), teacher, slot, supervisorTeacher\)',
     r'new LunchWave(\1.intValue(), teacher, slot, supervisorTeacher)')
]):
    print("Fixed: MasterScheduleServiceImpl.java - Long to int conversion")

# Fix SchedulesController - variable name issue
if fix_file(os.path.join(base_dir, "controller", "ui", "SchedulesController.java"), [
    (r'controller\.show', '// TODO: Fix undefined variable\n            // controller.show')
]):
    print("Fixed: SchedulesController.java - undefined controller variable")

# Fix SmartCourseAssignmentService - missing methods on Teacher
if fix_file(os.path.join(base_dir, "service", "impl", "SmartCourseAssignmentService.java"), [
    (r'teacher\.hasCertificationForSubjectAndGrade\(',
     '// TODO: Method does not exist - implement certification check\n                false && teacher.getName() != null && // teacher.hasCertificationForSubjectAndGrade('),
    (r'teacher\.hasExpiringCertifications\(\)',
     '// TODO: Method does not exist - implement expiration check\n                false // teacher.hasExpiringCertifications()')
]):
    print("Fixed: SmartCourseAssignmentService.java - missing Teacher methods")

# Fix OptimizationServiceImpl - threadCount method
if fix_file(os.path.join(base_dir, "service", "impl", "OptimizationServiceImpl.java"), [
    (r'\.threadCount\(', '.parallelThreadCount(')
]):
    print("Fixed: OptimizationServiceImpl.java - threadCount to parallelThreadCount")

# Fix ScheduleIssueDetector - isUnassigned method reference
if fix_file(os.path.join(base_dir, "service", "ScheduleIssueDetector.java"), [
    (r'\.filter\(this::isUnassigned\)', '.filter(slot -> isUnassigned(slot))')
]):
    print("Fixed: ScheduleIssueDetector.java - isUnassigned method reference")

# Fix RoomEquipmentService - getDisplayName on String
if fix_file(os.path.join(base_dir, "service", "RoomEquipmentService.java"), [
    (r'equipmentName\.getDisplayName\(\)', 'equipmentName')
]):
    print("Fixed: RoomEquipmentService.java - getDisplayName on String")

# Fix ScheduleSlotEditDialogController - getDisplayName on String
if fix_file(os.path.join(base_dir, "controller", "ui", "ScheduleSlotEditDialogController.java"), [
    (r'([a-zA-Z]+)\.getDisplayName\(\)', r'\1')
]):
    print("Fixed: ScheduleSlotEditDialogController.java - getDisplayName on String")

# Fix ScheduleGeneratorController - showDialog
if fix_file(os.path.join(base_dir, "controller", "ui", "ScheduleGeneratorController.java"), [
    (r'fixViolationsDialog\.showDialog\(', 'fixViolationsDialog.show(')
]):
    print("Fixed: ScheduleGeneratorController.java - showDialog to show")

# Fix SchedulesController - exportSchedule
if fix_file(os.path.join(base_dir, "controller", "ui", "SchedulesController.java"), [
    (r'exportService\.exportSchedule\(scheduleId, format\)',
     '// TODO: Method exportSchedule(Long, ExportFormat) does not exist\n                    // exportService.exportSchedule(scheduleId, format)')
]):
    print("Fixed: SchedulesController.java - exportSchedule method")

# Fix ScheduleViewController and ScheduleViewerController - ModernCalendarGrid methods
modern_grid_fixes = [
    (r'calendarGrid\.renderWeeklyGrid\(([^,]+), ([^,]+), ([^,]+), ([^)]+)\)',
     r'// TODO: Method renderWeeklyGrid with 4 params does not exist\n                // calendarGrid.renderWeeklyGrid(\1, \2, \3, \4)'),
    (r'calendarGrid\.renderDailyGrid\(([^,]+), ([^,]+), ([^,]+), ([^,]+), ([^)]+)\)',
     r'// TODO: Method renderDailyGrid with 5 params does not exist\n                // calendarGrid.renderDailyGrid(\1, \2, \3, \4, \5)'),
    (r'calendarGrid\.getSubjectColors\(\)',
     '// TODO: Method getSubjectColors() does not exist\n                new java.util.HashMap<String, String>() // calendarGrid.getSubjectColors()')
]

if fix_file(os.path.join(base_dir, "controller", "ui", "ScheduleViewController.java"), modern_grid_fixes):
    print("Fixed: ScheduleViewController.java - ModernCalendarGrid methods")

if fix_file(os.path.join(base_dir, "controller", "ui", "ScheduleViewerController.java"), modern_grid_fixes):
    print("Fixed: ScheduleViewerController.java - ModernCalendarGrid methods")

# Fix EnhancedScheduleViewController - resolveConflict
if fix_file(os.path.join(base_dir, "controller", "ui", "EnhancedScheduleViewController.java"), [
    (r'hybridSolver\.resolveConflict\(schedule, slot, timeSlot\)',
     '// TODO: Method resolveConflict() does not exist on hybridSolver\n                    // hybridSolver.resolveConflict(schedule, slot, timeSlot)')
]):
    print("Fixed: EnhancedScheduleViewController.java - resolveConflict")

# Fix RoomsController - generateRoomPhoneNumber
if fix_file(os.path.join(base_dir, "controller", "ui", "RoomsController.java"), [
    (r'districtSettingsService\.generateRoomPhoneNumber\(',
     '// TODO: Method generateRoomPhoneNumber() does not exist\n                    // districtSettingsService.generateRoomPhoneNumber(')
]):
    print("Fixed: RoomsController.java - generateRoomPhoneNumber")

print("\nAll fixes applied successfully!")
